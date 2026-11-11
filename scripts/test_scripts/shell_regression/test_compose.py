# This is a private project. All rights reserved.
import re
import shlex

from support import ROOT, ShellCase, cases, parameterize, shell_path

COMPOSE = "scripts/service_scripts/compose-service.sh"
START = "scripts/service_scripts/start-service-in-docker.sh"
PATCH = "deploy/docker-compose/docker-compose.generated-patch.yml"


class ComposeFixture(ShellCase):
    def setUp(self):
        super().setUp()
        for path in (COMPOSE, START, "scripts/service_scripts/service-env.sh", "scripts/tool_scripts/named-options.sh"):
            self.copy(path)
        (self.root / "deploy/docker-compose").mkdir(parents=True)
        self.recorder("gradlew")
        self.recorder("docker", '''for arg in "$@"; do
  if [[ $arg == build ]]; then exit "${MOCK_DOCKER_BUILD_EXIT:-0}"; fi
done
''')
        self.env["GRADLEW"] = "gradlew"

    def write_env(self, profiles, relative="deploy/sample.env"):
        return self.write(relative, f"""BUILD_TYPE=dev
FLAVOR=sample
BUILD_ON=docker
COMPOSE_PROJECT_NAME=a-shell-test
COMPOSE_FILE_LIST={profiles}
SERVER_PORT=8811
SERVER_EXPOSE_PORT=8811
CLI_INIT_ENABLE=false
CLI_READY_PORT=8081
PRESET_PATH=../dev-data
SERVER_URL=http://localhost:8811
WS_SERVER_URL=ws://localhost:8813
""")

    def configure(self, profiles, build_on=None):
        path = self.write_env(profiles)
        self.run_script(COMPOSE, "--env", "sample", "--command", "config",
                        env={} if build_on is None else {"BUILD_ON": build_on})
        self.assert_compose_calls(profiles, path, [["config"]])
        return self.yaml(PATCH)

    def assert_compose_calls(self, profiles, path, commands):
        prefix = ["compose", "--env-file", shell_path(path),
                  "-f", "./deploy/docker-compose/docker-compose.yml"]
        for profile in profiles.split(","):
            prefix += ["-f", f"./deploy/docker-compose/docker-compose.{profile}.yml"]
        prefix += ["-f", "./" + PATCH]
        calls = self.calls("docker")
        self.assertEqual([call[0] for call in calls], [prefix + command for command in commands])
        self.assertEqual([call[1] for call in calls], [shell_path(path)] * len(commands))
        self.assertEqual(self.calls("gradlew"), [])
        # Only the documented generated patch is owned by compose-service.
        self.assertEqual({p.name for p in (self.root / "deploy/docker-compose").iterdir()},
                         {"docker-compose.generated-patch.yml"})


@parameterize
class ComposeTests(ComposeFixture):
    def test_dependency_profiles_have_empty_override(self):
        self.assertEqual(self.configure("pg,elastic,minio,certs_bind"), {"services": {}})

    def test_server_cli_dependencies_and_certificates(self):
        generated = self.configure("pg,elastic,minio,certs_bind,cli,server", "local")
        self.assertEqual(set(generated["services"]), {"cli", "server", "ws"})
        for name, service in generated["services"].items():
            dependencies = {"pg", "minio", "es01"} | (set() if name == "cli" else {"cli"})
            self.assertEqual(service["depends_on"],
                             {key: {"condition": "service_healthy"} for key in dependencies})
            self.assertEqual(service["volumes"], ["certs:/app/deploy/es_ca"])
        self.assertEqual(self.calls("docker")[0][2], "local")

    @cases({"without_pg": (False,), "with_pg": (True,)})
    def test_rpc_dependencies_without_cli(self, postgres):
        profiles = ("pg," if postgres else "") + "filesystem,lucene,server,worker"
        services = self.configure(profiles)["services"]
        self.assertEqual(set(services), {"server", "worker", "ws"})
        for service in services.values():
            self.assertEqual(service["environment"], {
                "MEDIA_SERVICE": "filesystem", "FILESYSTEM_RPC_URL": "ws://filesystem:8820/rpc",
                "FILESYSTEM_PUBLIC_URL": "${FILESYSTEM_PUBLIC_URL:?set the externally reachable filesystem HTTP origin}",
                "SEARCH_SERVICE": "lucene", "LUCENE_RPC_URL": "ws://lucene:8821/rpc"})
            dependencies = {"filesystem", "lucene"} | ({"pg"} if postgres else set())
            self.assertEqual(service["depends_on"],
                             {key: {"condition": "service_healthy"} for key in dependencies})

    def test_build_mode_comes_from_env(self):
        self.configure("server")
        self.assertEqual(self.calls("docker")[0][2], "docker")

    def test_app_profile_does_not_build_host_wasm(self):
        self.configure("pg,server,app")

    def test_local_backend_dependencies_with_cli(self):
        services = self.configure("pg,filesystem,lucene,cli,server,worker")["services"]
        self.assertEqual(set(services), {"cli", "server", "worker", "ws"})
        for name, service in services.items():
            dependencies = {"pg", "filesystem", "lucene"} | (set() if name == "cli" else {"cli"})
            self.assertEqual(service["depends_on"],
                             {key: {"condition": "service_healthy"} for key in dependencies})
            self.assertNotIn("volumes", service)

    def test_bunker_env_and_network_membership(self):
        profiles = "pg,elastic,minio,certs_bind,grafana,dicebear,etcd,cli,server,worker,bunker"
        path = self.write_env(profiles)
        bunker = self.copy("deploy/bunker.env.example")
        self.run_script(COMPOSE, "--env", shell_path(path), "--command", "config",
                        env={"BUNKER_ENV_FILE": shell_path(bunker), "BUILD_ON": "local"})
        self.assert_compose_calls(profiles, path, [["config"]])
        generated = self.yaml(PATCH)
        self.assertEqual(generated["networks"], {"bw-services": {"name": "bw-services"}})
        services = generated["services"]
        self.assertEqual(set(services), {"bunkerweb", "pg", "adminer", "pg-exporter", "minio",
                                       "signin-redis", "grafana", "prometheus", "loki", "promtail",
                                       "setup", "es01", "kibana", "dicebear", "etcd", "cli",
                                       "server", "ws", "worker"})
        self.assertEqual(services.pop("bunkerweb"),
                         {"env_file": [{"path": shell_path(bunker), "format": "raw"}]})
        for service in services.values():
            self.assertEqual(service["networks"], ["bw-services"])

    @cases({"backends": ("pg,filesystem,lucene,cli,server,worker",
                         ["filesystem", "lucene", "cli", "worker", "server", "ws"]),
            "app": ("pg,server,app", ["server", "ws", "app-wasm", "panel-wasm"]),
            "without_app": ("pg,server", ["server", "ws"])})
    def test_deployment_builds_in_docker_before_start(self, profiles, targets):
        path = self.write_env(profiles)
        self.run_script(START, shell_path(path), env={"BUILD_ON": "local"})
        self.assert_compose_calls(profiles, path, [["build", *targets], ["up", "-d", "--no-build"]])
        self.assertEqual([call[2] for call in self.calls("docker")], ["docker", "docker"])

    def test_failed_build_preserves_status_and_prevents_deployment(self):
        path = self.write_env("pg,server")
        self.run_script(START, shell_path(path), expected=7, env={"MOCK_DOCKER_BUILD_EXIT": "7"})
        self.assert_compose_calls("pg,server", path, [["build", "server", "ws"]])

    @cases({"absolute": (False, False), "relative": (True, False), "deployment": (False, True)})
    def test_external_env_path_and_relative_bunker_env(self, relative, deploy):
        profiles = "filesystem,lucene,cli,server,worker,app,bunker"
        path = self.write_env(profiles, "external config/alpha.env")
        with path.open("a", encoding="utf-8") as stream:
            stream.write("BUNKER_ENV_FILE=bunker.env\n")
        bunker = self.write("external config/bunker.env", (ROOT / "deploy/bunker.env.example").read_text())
        selector = "alpha.env" if relative else shell_path(path)
        commands = [["config"]]
        if deploy:
            commands = [["build", "filesystem", "lucene", "cli", "worker", "server", "ws",
                         "app-wasm", "panel-wasm"], ["up", "-d", "--no-build"],
                        ["exec", "-T", "bunkerweb", "nginx", "-s", "reload"]]
        self.run_script(START if deploy else COMPOSE,
                        *([selector] if deploy else ["--env", selector, "--command", "config"]),
                        cwd=path.parent if relative else None)
        self.assert_compose_calls(profiles, path, commands)
        self.assertEqual(self.yaml(PATCH)["services"]["bunkerweb"]["env_file"],
                         [{"path": shell_path(bunker), "format": "raw"}])

    def test_missing_explicit_env_path_is_rejected(self):
        self.run_script(COMPOSE, "--env", shell_path(self.root / "external config/missing.env"),
                        "--command", "config", expected=1)
        self.assertEqual(self.calls("docker"), [])
        self.assertFalse((self.root / PATCH).exists())


@parameterize
class DeploymentConfigurationTests(ShellCase):
    def load(self, relative):
        self.copy(relative)
        return self.yaml(relative)

    @cases({"filesystem": ("filesystem",), "lucene": ("lucene",)})
    def test_rpc_service_logs_and_business_independence(self, backend):
        services = self.load(f"deploy/docker-compose/docker-compose.{backend}.yml")["services"]
        self.assertEqual(set(services), {backend})
        self.assertEqual(services[backend]["environment"]["LOG_PATH"], f"/home/app/logs/{backend}")
        self.assertIn("service-logs:/home/app/logs", services[backend]["volumes"])
        configs = self.load("deploy/promtail-config.yaml")["scrape_configs"]
        labels = [item["labels"] for config in configs for item in config["static_configs"]]
        self.assertIn({"job": backend, "__path__": f"/var/log/A/{backend}/*.log"}, labels)

    def test_server_ports(self):
        services = self.load("deploy/docker-compose/docker-compose.server.yml")["services"]
        self.assertEqual(services["server"]["environment"]["SERVER_PORT"], 80)
        self.assertEqual(services["ws"]["environment"]["WS_SERVER_PORT"], 80)

    def test_bunker_uses_raw_env_and_adminer_regex(self):
        example = (ROOT / "deploy/bunker.env.example").read_text()
        properties = dict(line.split("=", 1) for line in example.splitlines()
                          if line and not line.startswith("#") and "=" in line)
        self.assertEqual(properties["adminer.example.com_REVERSE_PROXY_URL"], "~ ^/")
        self.assertNotIn("${BUNKER_", (ROOT / "deploy/docker-compose/docker-compose.bunker.yml").read_text())

    def test_wasm_build_targets_urls_and_nginx_mount(self):
        services = self.load("deploy/docker-compose/docker-compose.app.yml")["services"]
        self.assertEqual(set(services), {"app-wasm", "panel-wasm"})
        for name, service in services.items():
            self.assertEqual(service["build"], {
                "context": "../../", "dockerfile": "wasm.Dockerfile", "target": name,
                "args": {"FLAVOR": "${FLAVOR}", "BUILD_TYPE": "${BUILD_TYPE}",
                         "SERVER_URL": "${SERVER_URL}", "WS_SERVER_URL": "${WS_SERVER_URL}"}})
            self.assertEqual(service["volumes"],
                             ["./app-wasm.nginx.conf:/etc/nginx/conf.d/default.conf:ro"])

    @cases({"server": ("server", ["server", "ws"]), "worker": ("worker", ["worker"]),
            "cli": ("cli", ["cli"]), "minio": ("minio", ["minio"])})
    def test_backend_build_targets(self, profile, names):
        services = self.load(f"deploy/docker-compose/docker-compose.{profile}.yml")["services"]
        for name in names:
            build = services[name]["build"]
            if name == "minio":
                self.assertEqual(build["dockerfile"], "minio.Dockerfile")
            else:
                self.assertEqual(build["target"], name)
                self.assertEqual(build.get("dockerfile", "Dockerfile"), "Dockerfile")

    def test_dockerfile_service_stages(self):
        stages = re.findall(r"^FROM\s+.+\s+AS\s+(\S+)", (ROOT / "Dockerfile").read_text(), re.M | re.I)
        self.assertTrue({"server", "worker", "ws", "cli", "filesystem-service", "lucene-service"} <= set(stages))

    def test_image_builder_uses_shared_dockerfile(self):
        script = (ROOT / "scripts/build_scripts/build-service-images.sh").read_text()
        for obsolete in ("worker.Dockerfile", "ws.Dockerfile", "cli.Dockerfile"):
            self.assertNotIn(obsolete, script)

    def test_minio_installs_healthcheck_tools(self):
        dockerfile = (ROOT / "minio.Dockerfile").read_text().replace("\\\n", " ")
        commands = [shlex.split(line[4:]) for line in dockerfile.splitlines() if line.startswith("RUN ")]
        self.assertTrue(any(command[:5] == ["apk", "add", "--no-cache", "ca-certificates", "curl"]
                            for command in commands))

    @cases({"parallelism": ("org.gradle.parallel", "true"),
            "detekt_worker_api": ("detekt.use.worker.api", "false")})
    def test_gradle_policy(self, key, expected):
        properties = dict(tuple(part.strip() for part in line.split("=", 1))
                          for line in (ROOT / "gradle.properties").read_text().splitlines()
                          if "=" in line and not line.startswith("#"))
        self.assertEqual(properties[key], expected)

    @cases({"backend": ("Dockerfile",), "wasm": ("wasm.Dockerfile",)})
    def test_docker_build_preserves_parallelism(self, filename):
        content = (ROOT / filename).read_text()
        self.assertNotIn("org.gradle.parallel=false", content)
        self.assertNotIn("--no-parallel", content)

    def test_wasm_image_keeps_runtime_configuration_external(self):
        content = (ROOT / "wasm.Dockerfile").read_text()
        for obsolete in ("flavor_env", "deploy/${FLAVOR}.env",
                         "COPY deploy/docker-compose/app-wasm.nginx.conf"):
            self.assertNotIn(obsolete, content)
