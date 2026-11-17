# This is a private project. All rights reserved.
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

MODULE = Path(__file__).resolve().parents[2] / "service_scripts/prebuilt-deploy.py"
SPEC = importlib.util.spec_from_file_location("prebuilt_deploy", MODULE)
deploy = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(deploy)


def stage_env():
    return {"DEPLOYMENT_MODE": "stage", "COMPOSE_FILE_LIST": ",".join(sorted(deploy.PROFILES)),
            "INIT_ENABLE": "false", "CLI_INIT_ENABLE": "false",
            "DATABASE_URI": "r2dbc:postgresql://pg:5432/stage"}


class StageConfigTests(unittest.TestCase):
    def test_accepts_dedicated_stage_environment(self):
        deploy.validate_stage(stage_env(), "example-stage")

    def test_rejects_external_database(self):
        with self.assertRaisesRegex(ValueError, "database"):
            deploy.validate_stage(stage_env() | {"DATABASE_URI": "r2dbc:postgresql://production/db"}, "example-stage")

    def test_rejects_provider_credentials(self):
        with self.assertRaisesRegex(ValueError, "unsupported"):
            deploy.validate_stage(stage_env() | {"SMTP_PASSWORD": "fixture"}, "example-stage")

    def test_rejects_initialization(self):
        with self.assertRaisesRegex(ValueError, "INIT_ENABLE"):
            deploy.validate_stage(stage_env() | {"INIT_ENABLE": "true"}, "example-stage")

    def test_rejects_production_project_name(self):
        with self.assertRaisesRegex(ValueError, "end in"):
            deploy.validate_stage(stage_env(), "production")

    def test_rejects_public_websocket_endpoint(self):
        with self.assertRaisesRegex(ValueError, "origin"):
            deploy.validate_stage(stage_env() | {"WS_SERVER_URL": "wss://example.invalid"}, "example-stage")

    def test_rejects_bunker_profile(self):
        with self.assertRaisesRegex(ValueError, "profiles"):
            deploy.validate_stage(stage_env() | {"COMPOSE_FILE_LIST": "bunker,pg"}, "example-stage")

    def test_runtime_uses_only_imported_images_and_isolated_resources(self):
        services = {name: {"build": {"context": "fixture"}, "image": "mutable:latest",
                           "ports": [{"target": 80, "published": "80", "host_ip": "0.0.0.0"}],
                           "networks": {"shared": {}}}
                    for name in deploy.TARGETS | deploy.EXTERNAL}
        services["pg"]["volumes"] = [{"type": "volume", "source": "pg-data", "target": "/var/lib/postgresql"}]
        model = {"services": services, "volumes": {"pg-data": {"name": "production-data", "external": True}},
                 "networks": {"shared": {"external": True}}}
        manifest = {"images": {name: {"ref": f"fixture/{name}:revision"} for name in services}}
        with patch.object(deploy, "read_env", return_value=stage_env()), patch.object(
                deploy, "capture", return_value="prefix\n" + json.dumps(model)):
            result = deploy.render_runtime(Path("fixture.env"), "example-stage", manifest)
        self.assertEqual(result["name"], "example-stage")
        self.assertNotIn("networks", result)
        self.assertEqual(result["volumes"]["pg-data"], {"name": "example-stage_pg-data"})
        for name, service in result["services"].items():
            self.assertNotIn("build", service)
            self.assertNotIn("networks", service)
            self.assertEqual(service["pull_policy"], "never")
            image_name = "app-wasm" if name == "stage-api" else name
            self.assertEqual(service["image"], manifest["images"][image_name]["ref"])
            self.assertEqual(service["ports"][0]["host_ip"], "127.0.0.1")
            self.assertEqual(service["ports"][0]["published"], "0")
        self.assertEqual(result["services"]["pg"]["volumes"][0]["target"], "/var/lib/postgresql/data")
        for name in ("server", "ws"):
            self.assertEqual(result["services"][name]["environment"]["SIGNIN_ALLOWED_ORIGINS"],
                             "http://127.0.0.1:8080,http://localhost:8080,http://127.0.0.1:8081,http://localhost:8081")
            self.assertEqual(result["services"][name]["environment"]["BUILD_TYPE"], "dev")
        gateway = result["services"]["stage-api"]
        self.assertEqual(gateway["healthcheck"]["test"][1], "wget -q -O /dev/null http://127.0.0.1/ping")
        self.assertTrue(gateway["volumes"][0]["read_only"])

    def test_proxy_template_whitelists_only_configured_loopback_origins(self):
        config = deploy.stage_proxy_config({"APP_WASM_PORT": "18080", "PANEL_WASM_PORT": "18081"})
        self.assertIn('default "";', config)
        for host in ("127.0.0.1", "localhost"):
            for port in (18080, 18081):
                self.assertIn(f'"http://{host}:{port}" $http_origin;', config)
        self.assertNotIn("__APP_PORT__", config)
        self.assertNotIn('Allow-Origin "*"', config)
        self.assertIn("proxy_request_buffering off;", config)
        self.assertIn("resolver 127.0.0.11 valid=5s ipv6=off;", config)

    def test_proxy_rejects_same_browser_ports(self):
        with self.assertRaisesRegex(ValueError, "distinct"):
            deploy.browser_ports({"APP_WASM_PORT": "18080", "PANEL_WASM_PORT": "18080"})

    def test_proxy_rejects_invalid_browser_port(self):
        with self.assertRaises(ValueError):
            deploy.browser_ports({"APP_WASM_PORT": "18080;invalid"})

    def test_rejects_unexpected_runtime_service(self):
        with patch.object(deploy, "read_env", return_value=stage_env()), patch.object(
                deploy, "capture", return_value='{"services":{"unexpected":{}}}'):
            with self.assertRaisesRegex(ValueError, "differ"):
                deploy.render_runtime(Path("fixture.env"), "example-stage", {})

    def test_remote_image_mismatch_prevents_start(self):
        from argparse import Namespace
        with tempfile.TemporaryDirectory(dir=deploy.ROOT / "build") as directory:
            root = Path(directory)
            (root / "manifest.json").write_text(json.dumps({"images": {"server": {"ref": "fixture:rev", "id": "sha256:expected", "identity_sha256": "expected"}}}))
            with patch.object(deploy, "ROOT", root), patch.object(deploy, "image_record", return_value={"identity_sha256": "wrong"}), patch.object(
                    deploy, "run") as start, patch.dict(__import__("sys").modules, {"fcntl": __import__("types").SimpleNamespace(
                        flock=lambda *args: None, LOCK_EX=1, LOCK_NB=2)}):
                with self.assertRaisesRegex(ValueError, "mismatch"):
                    deploy.runtime_apply(Namespace(project="example-stage"))
                start.assert_not_called()
            (root.parent / ".example-stage.lock").unlink(missing_ok=True)

    def test_remote_paths_reject_command_injection(self):
        from argparse import Namespace
        with self.assertRaisesRegex(ValueError, "Remote paths"):
            deploy.safe_remote(Namespace(ssh="fixture", remote_dir="/tmp/x;touch", remote_env="/tmp/env"))

    def test_runtime_bundle_is_an_explicit_secret_free_allowlist(self):
        for relative in deploy.RUNTIME_FILES:
            self.assertTrue((deploy.ROOT / relative).is_file())
            self.assertFalse(relative.endswith(".env"))
            self.assertNotIn("secrets", relative)
            self.assertNotIn(".git", Path(relative).parts)


class TransferTests(unittest.TestCase):
    def fixture(self, directory):
        from argparse import Namespace
        root = Path(directory)
        for relative in deploy.RUNTIME_FILES:
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("fixture\n")
        (root / "connection.conf").write_text("# Synthetic private connection profile\n")
        return root, Namespace(env=root / "stage.env", ssh="fixture", project="fixture-stage",
                               remote_env="/private/stage.env", remote_dir="/opt/stage/releases",
                               ssh_config=root / "connection.conf")

    @staticmethod
    def output(command, **kwargs):
        if command[:3] == ["git", "status", "--porcelain"]:
            return ""
        if command[:2] == ["git", "rev-parse"]:
            return "a" * 40
        if command[:2] == ["git", "ls-files"]:
            return "100755 " + "a" * 40 + " 0\tfixture"
        if command[:3] == ["docker", "pussh", "--version"]:
            return "docker-pussh, version 0.4.3\nunregistry image: fixture"
        return json.dumps([{"Id": "sha256:fixture", "Config": {"User": "", "Env": ["FIXTURE=1"]},
                            "RootFS": {"Type": "layers", "Layers": ["sha256:layer"]},
                            "Os": "linux", "Architecture": "amd64"}])

    def test_pushes_all_images_and_transfers_only_verified_runtime(self):
        import tarfile
        with tempfile.TemporaryDirectory(dir=deploy.ROOT / "build") as directory:
            root, args = self.fixture(directory)

            with patch.object(deploy, "ROOT", root), patch.object(deploy, "read_env", return_value=stage_env() | {
                    "FLAVOR": "dev", "BUILD_TYPE": "release", "SERVER_URL": "http://localhost:8080",
                    "WS_SERVER_URL": "ws://localhost:8081"}), patch.object(deploy, "capture", side_effect=self.output), patch.object(
                    deploy, "run") as commands, patch.object(deploy, "ssh") as remote:
                deploy.deploy(args)
            release = next((root / "build/prebuilt-deploy").iterdir())
            manifest = json.loads((release / "manifest.json").read_text())
            self.assertEqual(set(manifest["images"]), set(deploy.TARGETS) | set(deploy.EXTERNAL))
            self.assertNotIn("archive_sha256", manifest)
            self.assertFalse((release / "images.tar").exists())
            pushes = [call.args[0] for call in commands.call_args_list if call.args[0][:2] == ["docker", "pussh"]]
            self.assertEqual([command[-2] for command in pushes],
                             [record["ref"] for record in manifest["images"].values()])
            self.assertTrue(all(command[-1] == args.ssh for command in pushes))
            self.assertTrue(all(command[2:4] == ["-F", args.ssh_config.resolve().as_posix()] for command in pushes))
            for call in commands.call_args_list:
                if call.args[0][:2] == ["docker", "pussh"]:
                    self.assertEqual(call.kwargs["env"]["UNREGISTRY_IMAGE"], deploy.UNREGISTRY_IMAGE)
                    self.assertEqual(call.kwargs["env"]["SSH_STRICT_HOST_KEY_CHECKING"], "yes")
            with tarfile.open(release / "runtime.tar") as archive:
                self.assertEqual(set(archive.getnames()), set(deploy.RUNTIME_FILES))
                self.assertEqual(archive.getmember("scripts/service_scripts/compose-service.sh").mode, 0o755)
            self.assertIn("sha256sum -c SHA256SUMS", remote.call_args.args[1][2])
            self.assertNotIn("docker load", remote.call_args.args[1][2])
            self.assertFalse(any(call.args[0][:2] == ["docker", "save"] for call in commands.call_args_list))
            transfer = next(call.args[0] for call in commands.call_args_list if call.args[0][0] == "scp")
            self.assertEqual(transfer[1:3], ["-F", args.ssh_config.resolve().as_posix()])
            self.assertEqual({Path(item).name for item in transfer[-4:-1]},
                             {"runtime.tar", "manifest.json", "SHA256SUMS"})

    def test_incompatible_target_prevents_build_and_transfer(self):
        import subprocess
        with tempfile.TemporaryDirectory(dir=deploy.ROOT / "build") as directory:
            root, args = self.fixture(directory)
            with patch.object(deploy, "ROOT", root), patch.object(deploy, "read_env", return_value=stage_env()), patch.object(
                    deploy, "capture", side_effect=self.output), patch.object(deploy, "ssh", side_effect=[
                        None, None, subprocess.CalledProcessError(1, ["docker", "info"])]) as remote, patch.object(
                    deploy, "run") as commands:
                with self.assertRaises(subprocess.CalledProcessError):
                    deploy.deploy(args)
            commands.assert_not_called()
            self.assertEqual(remote.call_count, 3)

    def test_failed_image_push_prevents_remote_apply(self):
        import subprocess
        with tempfile.TemporaryDirectory(dir=deploy.ROOT / "build") as directory:
            root, args = self.fixture(directory)

            def execute(command, **kwargs):
                if command[:2] == ["docker", "pussh"]:
                    raise subprocess.CalledProcessError(1, command)

            with patch.object(deploy, "ROOT", root), patch.object(deploy, "read_env", return_value=stage_env() | {
                    "FLAVOR": "dev", "BUILD_TYPE": "debug", "SERVER_URL": "http://localhost:8080",
                    "WS_SERVER_URL": "ws://localhost:8081"}), patch.object(deploy, "capture", side_effect=self.output), patch.object(
                    deploy, "run", side_effect=execute), patch.object(deploy, "ssh") as remote:
                with self.assertRaises(subprocess.CalledProcessError):
                    deploy.deploy(args)
            self.assertFalse(any("--apply" in str(call.args) for call in remote.call_args_list))

    def test_ssh_uses_same_private_connection_profile_as_scp(self):
        with tempfile.TemporaryDirectory(dir=deploy.ROOT / "build") as directory:
            _, args = self.fixture(directory)
            with patch.object(deploy, "run") as command:
                deploy.ssh(args, ["echo", "ready"])
            self.assertEqual(command.call_args.args[0][:3], ["ssh", "-F", args.ssh_config.resolve().as_posix()])

    def test_build_failure_prevents_image_transfer_and_remote_start(self):
        import subprocess
        with tempfile.TemporaryDirectory(dir=deploy.ROOT / "build") as directory:
            root, args = self.fixture(directory)
            with patch.object(deploy, "ROOT", root), patch.object(deploy, "read_env", return_value=stage_env()), patch.object(
                    deploy, "capture", side_effect=self.output), patch.object(deploy, "ssh") as remote, patch.object(
                    deploy, "run", side_effect=subprocess.CalledProcessError(7, ["docker", "build"])) as commands:
                with self.assertRaises(subprocess.CalledProcessError):
                    deploy.deploy(args)
            self.assertEqual(remote.call_count, 3)  # Tool, env and image-store preflight only.
            self.assertEqual(commands.call_count, 1)


class ImageIdentityTests(unittest.TestCase):
    @staticmethod
    def image():
        return {"Id": "sha256:config", "Config": {"User": "", "WorkingDir": "", "Cmd": None, "Env": ["EXAMPLE=1"]},
                "RootFS": {"Type": "layers", "Layers": ["sha256:base", "sha256:app"]},
                "Os": "linux", "Architecture": "amd64"}

    def identity(self, image):
        with patch.object(deploy, "capture", return_value=json.dumps([image])):
            return deploy.image_record("fixture:rev")["identity_sha256"]

    def test_classic_and_containerd_ids_share_effective_identity(self):
        classic = self.image()
        containerd = self.image() | {"Id": "sha256:manifest", "Variant": ""}
        containerd["Config"] = {"Env": ["EXAMPLE=1"]}
        self.assertEqual(self.identity(classic), self.identity(containerd))

    def test_configuration_layer_order_and_platform_changes_are_rejected(self):
        from copy import deepcopy
        original = self.image()
        changes = []
        for field, value in [("Env", ["EXAMPLE=2"]), ("User", "1000"), ("WorkingDir", "/other"), ("Cmd", ["other"])]:
            changed = deepcopy(original)
            changed["Config"][field] = value
            changes.append(changed)
        changes += [original | {"RootFS": {"Type": "layers", "Layers": ["sha256:app", "sha256:base"]}},
                    original | {"Architecture": "arm64"}, original | {"Variant": "v7"}]
        for changed in changes:
            with self.subTest(changed=changed):
                self.assertNotEqual(self.identity(original), self.identity(changed))


class PusshFailureTests(unittest.TestCase):
    def test_unsupported_plugin_fails_before_remote_preflight(self):
        with patch.object(deploy, "capture", return_value="docker-pussh, version 0.3.0"), patch.object(
                deploy, "ssh") as remote:
            with self.assertRaisesRegex(ValueError, "0.4.3"):
                deploy.check_image_transport(object())
            remote.assert_not_called()

    def test_push_failure_stops_before_remaining_images(self):
        import subprocess
        from argparse import Namespace
        with patch.object(deploy, "run", side_effect=subprocess.CalledProcessError(1, ["docker", "pussh"])) as run:
            with self.assertRaises(subprocess.CalledProcessError):
                deploy.push_images(Namespace(ssh="fixture"), {"a": {"ref": "a:rev"}, "b": {"ref": "b:rev"}})
            self.assertEqual(run.call_count, 1)
