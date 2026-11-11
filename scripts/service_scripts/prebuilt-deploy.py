# This is a private project. All rights reserved.
"""Build on the operator's Docker backend; transfer immutable images over SSH."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import shlex
import subprocess
import sys
import tarfile
import uuid

ROOT = Path(__file__).resolve().parents[2]
TARGETS = {"server": "server", "worker": "worker", "cli": "cli", "ws": "ws",
           "filesystem": "filesystem-service", "lucene": "lucene-service",
           "app-wasm": "app-wasm", "panel-wasm": "panel-wasm"}
PROFILES = {"pg", "server", "worker", "cli", "filesystem", "lucene", "app"}
EXTERNAL = {"pg": "postgres:17", "adminer": "adminer:latest",
            "pg-exporter": "quay.io/prometheuscommunity/postgres-exporter:latest",
            "signin-redis": "redis:7.4-alpine"}
RUNTIME_FILES = ["scripts/service_scripts/service-env.sh",
                 "scripts/tool_scripts/named-options.sh",
                 "scripts/service_scripts/compose-service.sh",
                 "scripts/service_scripts/prebuilt-deploy.py",
                 "deploy/docker-compose/stage-api.nginx.conf.template",
                 "deploy/docker-compose/app-wasm.nginx.conf"] + [
    f"deploy/docker-compose/docker-compose{suffix}.yml"
    for suffix in ("", ".pg", ".server", ".worker", ".cli", ".filesystem", ".lucene", ".app")]


def run(args, **kwargs):
    return subprocess.run([str(x) for x in args], check=True, **kwargs)


def capture(args, **kwargs):
    return run(args, stdout=subprocess.PIPE, text=True, **kwargs).stdout.strip()


def digest(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def read_env(path):
    values = {}
    for line in Path(path).read_text(encoding="utf-8-sig").splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            key, separator, value = line.partition("=")
            if not separator or not re.fullmatch(r"[A-Z][A-Z0-9_]*", key):
                raise ValueError("Environment must contain simple KEY=value lines")
            values[key] = value.strip().strip("\"'")
    return values


def validate_stage(env, project):
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]*-stage", project):
        raise ValueError("Stage project must end in -stage")
    if set(env.get("COMPOSE_FILE_LIST", "").split(",")) != PROFILES:
        raise ValueError("Stage requires profiles: " + ",".join(sorted(PROFILES)))
    if env.get("DEPLOYMENT_MODE") != "stage":
        raise ValueError("Use a dedicated DEPLOYMENT_MODE=stage environment")
    for flag in ("INIT_ENABLE", "CLI_INIT_ENABLE"):
        if env.get(flag) != "false":
            raise ValueError(f"Stage requires {flag}=false")
    # A stage env is intentionally small: production credentials/providers cannot leak in.
    allowed = set("DEPLOYMENT_MODE COMPOSE_FILE_LIST COMPOSE_PROJECT_NAME BUILD_ON FLAVOR BUILD_TYPE "
                  "SERVER_URL WS_SERVER_URL WS_RPC_URL FILESYSTEM_PUBLIC_URL PRESET_PATH "
                  "DATABASE_URI DATABASE_PORT DATABASE_DB DATABASE_DRIVER DATABASE_USER DATABASE_PASS "
                  "DATABASE_FRONT_PORT SERVER_EXPOSE_PORT WS_SERVER_EXPOSE_PORT FILESYSTEM_EXPOSE_PORT "
                  "APP_WASM_PORT PANEL_WASM_PORT SESSION_SECRET INIT_ENABLE CLI_INIT_ENABLE "
                  "CLI_READY_PORT MEDIA_SERVICE SEARCH_SERVICE".split())
    if set(env) - allowed:
        raise ValueError("Stage environment has unsupported settings")
    if env.get("DATABASE_URI") != "r2dbc:postgresql://pg:5432/stage":
        raise ValueError("Stage database must be the isolated pg service")
    for key, value in env.items():
        if key.endswith("URL") and not re.match(r"^(https?|wss?)://(127\.0\.0\.1|localhost|ws)(:|/|$)", value):
            raise ValueError(f"Stage {key} must use a local/test origin")
    browser_ports(env)


def browser_ports(env):
    ports = tuple(int(env.get(key, default)) for key, default in
                  (("APP_WASM_PORT", "8080"), ("PANEL_WASM_PORT", "8081")))
    if any(port < 1 or port > 65535 for port in ports) or ports[0] == ports[1]:
        raise ValueError("Stage browser ports must be distinct numbers between 1 and 65535")
    return ports


def stage_origins(env):
    return [f"http://{host}:{port}" for port in browser_ports(env)
            for host in ("127.0.0.1", "localhost")]


def stage_proxy_config(env):
    app_port, panel_port = browser_ports(env)
    template = (ROOT / "deploy/docker-compose/stage-api.nginx.conf.template").read_text(encoding="utf-8")
    return template.replace("__APP_PORT__", str(app_port)).replace("__PANEL_PORT__", str(panel_port))


def render_runtime(env_path, project, manifest):
    env = read_env(env_path)
    validate_stage(env, project)
    raw = capture(["bash", ROOT / "scripts/service_scripts/compose-service.sh",
                   "--env", env_path, "--command", "config --format json"],
                  cwd=ROOT, env=os.environ | {"BUILD_ON": "docker"})
    model = json.loads(raw[raw.index("{"):])
    if set(model["services"]) != set(TARGETS) | set(EXTERNAL):
        raise ValueError("Runtime services differ from the image manifest")
    model["name"] = project
    model.pop("networks", None)
    for name, volume in model.get("volumes", {}).items():
        volume.clear()
        volume["name"] = f"{project}_{name}"
    for name, service in model["services"].items():
        service.pop("build", None)
        service.pop("container_name", None)
        service.pop("networks", None)
        service["image"] = manifest["images"][name]["ref"]
        service["pull_policy"] = "never"
        if name in ("server", "ws"):
            service.setdefault("environment", {}).update(
                SIGNIN_ALLOWED_ORIGINS=",".join(stage_origins(env)), BUILD_TYPE="dev")
        for port in service.get("ports", []):
            # Let Docker allocate unused loopback ports, independent of production listeners.
            port["host_ip"] = "127.0.0.1"
            port["published"] = "0"
        for mount in service.get("volumes", []):
            if mount["type"] == "bind" and mount["target"] != "/etc/nginx/conf.d/default.conf":
                mount["source"] = str(ROOT / "stage-data" / name / mount["target"].strip("/"))
                Path(mount["source"]).mkdir(parents=True, exist_ok=True)
                os.chown(mount["source"], 1000, 1000)
        if name == "pg":
            # PostgreSQL 17 otherwise creates an anonymous data volume below this mount.
            service["volumes"][0]["target"] = "/var/lib/postgresql/data"
    # Production delegates CORS to its ingress. Stage has no shared ingress and
    # needs the same contract, restricted to the operator's loopback browser ports.
    model["services"]["stage-api"] = {
        "image": manifest["images"]["app-wasm"]["ref"], "pull_policy": "never",
        "restart": "unless-stopped", "depends_on": {"server": {"condition": "service_started"}},
        "ports": [{"target": 80, "published": "0", "host_ip": "127.0.0.1", "protocol": "tcp"}],
        "volumes": [{"type": "bind", "source": str(ROOT / "stage-api.nginx.conf"),
                     "target": "/etc/nginx/conf.d/default.conf", "read_only": True}],
        "healthcheck": {"test": ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1/ping"],
                        "interval": "5s", "timeout": "3s", "retries": 24}}
    return model


def runtime_apply(args):
    import fcntl
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]*-stage", args.project):
        raise ValueError("Stage project must end in -stage")
    with (ROOT.parent / f".{args.project}.lock").open("w") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        apply_verified(args)


def apply_verified(args):
    manifest = json.loads((ROOT / "manifest.json").read_text())
    for record in manifest["images"].values():
        actual = capture(["docker", "image", "inspect", "--format", "{{.Id}}", record["ref"]])
        if actual != record["id"]:
            raise ValueError("Imported image ID mismatch")
    model = render_runtime(args.env, args.project, manifest)
    (ROOT / "stage-api.nginx.conf").write_text(stage_proxy_config(read_env(args.env)), encoding="utf-8")
    config = ROOT / "runtime.json"
    config.write_text(json.dumps(model), encoding="utf-8")
    config.chmod(0o600)
    command = ["docker", "compose", "-p", args.project, "-f", config]
    try:
        run(command + ["up", "-d", "--no-build", "--pull", "never", "--wait", "--wait-timeout", "300"])
    except subprocess.CalledProcessError:
        run(command + ["ps", "-a"])
        print("Deployment failed; containers and private logs retained for diagnosis.", flush=True)
        raise
    run(command + ["ps"])
    print("Stage containers ready; application acceptance and data migration are separate.", flush=True)


def safe_remote(args):
    if getattr(args, "ssh_config", None) and not args.ssh_config.is_file():
        raise ValueError("SSH configuration file does not exist")
    if not re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_.@-]*", args.ssh):
        raise ValueError("--ssh must be an SSH config alias or user@host")
    for value in (args.remote_dir, args.remote_env):
        if not re.fullmatch(r"/[A-Za-z0-9_./-]+", value) or ".." in PurePosixPath(value).parts:
            raise ValueError("Remote paths must be absolute, without spaces or parent traversal")
    if args.remote_dir == "/" or args.remote_env.startswith(args.remote_dir.rstrip("/") + "/"):
        raise ValueError("Runtime environment must be outside the release directory")


def ssh_config_options(args):
    config = getattr(args, "ssh_config", None)
    # Forward slashes also survive OpenSSH's shell-quoted ProxyJump command on Windows.
    return ["-F", config.resolve().as_posix()] if config else []


def ssh(args, command, **kwargs):
    return run(["ssh", *ssh_config_options(args), "-o", "BatchMode=yes", "-o", "ConnectTimeout=15",
                "-o", "ServerAliveInterval=15", "-o", "ServerAliveCountMax=3",
                args.ssh, shlex.join([str(x) for x in command])], **kwargs)


def verify_checkout(commit):
    if (capture(["git", "status", "--porcelain"], cwd=ROOT)
            or capture(["git", "rev-parse", "HEAD"], cwd=ROOT) != commit):
        raise ValueError("Checkout changed during build; no release will be transferred")


def deploy(args):
    safe_remote(args)
    env = read_env(args.env)
    validate_stage(env, args.project)
    if capture(["git", "status", "--porcelain"], cwd=ROOT):
        raise ValueError("Commit reviewed changes before building a traceable release")
    commit = capture(["git", "rev-parse", "HEAD"], cwd=ROOT)
    release = f"{commit[:12]}-{uuid.uuid4().hex[:8]}"
    remote = f"{args.remote_dir.rstrip('/')}/{release}"
    # Fail before costly builds if the target/configuration is unavailable.
    ssh(args, ["sh", "-c", f"test -f {shlex.quote(args.remote_env)} && command -v python3 && docker info >/dev/null && docker compose version"])
    source = Path(__file__).read_text(encoding="utf-8")
    ssh(args, ["python3", "-c", f"import sys; __file__={str(remote + '/scripts/service_scripts/prebuilt-deploy.py')!r}; exec(compile(sys.stdin.read(), __file__, 'exec'))",
               "--check-env", "--env", args.remote_env, "--project", args.project],
        input=source, text=True)
    output = ROOT / "build/prebuilt-deploy" / release
    output.mkdir(parents=True, mode=0o700)
    manifest = {"commit": commit, "runtime_commit": commit,
                "project": args.project, "images": {}, "files": {}}
    for service, target in TARGETS.items():
        verify_checkout(commit)
        ref = f"a-{service}:{release}"
        print(f"Building {service} ({commit[:12]})", flush=True)
        command = ["docker", "build", "--platform", "linux/amd64", "--target", target,
                   "-t", ref, "--label", f"org.opencontainers.image.revision={commit}"]
        if service.endswith("-wasm"):
            command += ["-f", "wasm.Dockerfile"]
            for key in ("FLAVOR", "BUILD_TYPE", "SERVER_URL", "WS_SERVER_URL"):
                if not env.get(key):
                    raise ValueError(f"Missing build setting {key}")
                command += ["--build-arg", f"{key}={env[key]}"]
        else:
            command += ["--build-arg", "BUILD_ON=docker"]
        run(command + ["."], cwd=ROOT)
        manifest["images"][service] = {"ref": ref, "id": capture([
            "docker", "image", "inspect", "--format", "{{.Id}}", ref])}
    for service, source in EXTERNAL.items():
        run(["docker", "pull", "--platform", "linux/amd64", source])
        ref = f"a-dependency-{service}:{release}"
        run(["docker", "tag", source, ref])
        manifest["images"][service] = {"ref": ref, "source": source, "id": capture([
            "docker", "image", "inspect", "--format", "{{.Id}}", ref])}
    images = output / "images.tar"
    verify_checkout(commit)
    # Stream stdout: works with local engines and SSH-backed Docker CLI implementations.
    with images.open("wb") as stream:
        run(["docker", "save", *[r["ref"] for r in manifest["images"].values()]], stdout=stream)
    manifest["archive_sha256"] = digest(images)
    payload = output / "runtime.tar"
    with tarfile.open(payload, "w") as archive:
        for relative in RUNTIME_FILES:
            data = (ROOT / relative).read_bytes().replace(b"\r\n", b"\n")
            manifest["files"][relative] = hashlib.sha256(data).hexdigest()
            info = tarfile.TarInfo(relative)
            info.size = len(data)
            tracked = capture(["git", "ls-files", "--stage", "--", relative], cwd=ROOT)
            info.mode = int(tracked.split()[0], 8) & 0o777
            archive.addfile(info, io.BytesIO(data))
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    hashes = output / "SHA256SUMS"
    hashes.write_text("".join(f"{digest(output / name)}  {name}\n"
                             for name in ("images.tar", "runtime.tar", "manifest.json")), encoding="ascii")
    ssh(args, ["mkdir", "-p", remote])
    run(["scp", *ssh_config_options(args), "-o", "BatchMode=yes", *[(output / name).as_posix() for name in
         ("images.tar", "runtime.tar", "manifest.json", "SHA256SUMS")], f"{args.ssh}:{remote}/"])
    print("Verifying and loading remote release", flush=True)
    ssh(args, ["sh", "-c", f"cd {shlex.quote(remote)} && sha256sum -c SHA256SUMS && "
               "tar -xf runtime.tar && docker load -i images.tar && " + shlex.join([
                   "python3", "scripts/service_scripts/prebuilt-deploy.py", "--apply",
                   "--env", args.remote_env, "--project", args.project])])
    print(f"Release ready: {release}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help=argparse.SUPPRESS)
    parser.add_argument("--check-env", action="store_true", help=argparse.SUPPRESS)
    parser.add_argument("--ssh", help="SSH config alias or user@host")
    parser.add_argument("--ssh-config", type=Path, help="Optional private OpenSSH configuration file")
    parser.add_argument("--env", required=True, type=Path)
    parser.add_argument("--remote-env", help="Existing isolated stage env on target")
    parser.add_argument("--remote-dir", help="Release parent on target")
    parser.add_argument("--project", required=True)
    parser.add_argument("--mode", choices=["stage"], default="stage")
    args = parser.parse_args()
    if not (args.apply or args.check_env) and not all((args.ssh, args.remote_env, args.remote_dir)):
        parser.error("--ssh, --remote-env and --remote-dir are required")
    try:
        if args.check_env:
            validate_stage(read_env(args.env), args.project)
        elif args.apply:
            runtime_apply(args)
        else:
            deploy(args)
    except (ValueError, subprocess.CalledProcessError) as error:
        print(f"Deployment stopped: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
