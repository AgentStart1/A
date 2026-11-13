# This is a private project. All rights reserved.
import subprocess
import sys

from support import ROOT, ShellCase, cases, parameterize


@parameterize
class NamedOptionsTests(ShellCase):
    def setUp(self):
        super().setUp()
        self.copy("scripts/tool_scripts/named-options.sh")

    @cases({"compose": ("scripts/service_scripts/compose-service.sh",),
            "avd": ("scripts/android_scripts/create-avd.sh",),
            "deep_link": ("scripts/android_scripts/open-deep-link.sh",),
            "ci_build": ("scripts/build_scripts/build-app-on-ci.sh",),
            "notification": ("scripts/tool_scripts/show-notification.sh",)})
    def test_rejects_old_positional_interface_before_side_effects(self, script):
        self.copy(script)
        if script.endswith("compose-service.sh"):
            self.copy("scripts/service_scripts/service-env.sh")
        for name in ("docker", "adb", "gradlew", "sdkmanager", "notify-send"):
            self.recorder(name)
        self.run_script(script, "first", "second", "third", expected=1)
        self.assertFalse((self.root / "calls").exists())
        self.assertFalse((self.home / ".gradle").exists())

    def test_deep_link_named_values_preserve_uri_arguments(self):
        script = "scripts/android_scripts/open-deep-link.sh"
        self.copy(script)
        self.recorder("adb")
        self.run_script(script, "--path", "/room/100?label=two words", "--host", "localhost",
                        "--scheme", "dev-debug")
        self.assertEqual(self.calls("adb")[0][0], ["shell", "am", "start", "-a",
            "android.intent.action.VIEW", "-d", "alphaheart-dev-debug://localhost/room/100?label=two words"])

    @cases({"missing_value": ("--title",), "unknown": ("--unknown", "value"),
            "missing_required": ("--title", "title"),
            "invalid_boolean": ("--title", "title", "--message", "body", "--success", "maybe")})
    def test_notification_rejects_invalid_options(self, *args):
        script = "scripts/tool_scripts/show-notification.sh"
        self.copy(script)
        self.run_script(script, *args, expected=1)

    def test_notification_named_values_preserve_spaces_and_shell_characters(self):
        script = "scripts/tool_scripts/show-notification.sh"
        self.copy(script)
        self.fake("uname", "printf 'Linux\\n'\n")
        self.recorder("notify-send")
        self.fake("paplay", "exit 0\n")
        self.run_script(script, "--title", "title with spaces", "--message", "literal $(touch injected)",
                        "--success", "true")
        self.assertEqual(self.calls("notify-send")[0][0], ["-u", "normal", "-i", "dialog-information",
                         "title with spaces", "literal $(touch injected)"])
        self.assertFalse((self.root / "injected").exists())

    def test_avd_named_values_reach_sdk_manager(self):
        script = "scripts/android_scripts/create-avd.sh"
        self.copy(script)
        self.recorder("sdkmanager", "printf 'system-images;android-36;google_apis;x86_64\\n'\n")
        self.recorder("avdmanager", 'if [[ $1 == list ]]; then echo "fixture-avd"; fi\n')
        self.fake("emulator", "exit 0\n")
        self.run_script(script, "--device", "pixel", "--name", "fixture-avd", "--system-image",
                        "system-images;android-36;google_apis;x86_64", env={"WINDIR": "", "OSTYPE": "linux"})
        self.assertEqual(self.calls("avdmanager")[0][0], ["list", "avd"])
        self.assertEqual(self.calls("sdkmanager")[0][0], ["--list_installed"])

    def test_ci_named_values_select_artifact_and_build_properties(self):
        script = "scripts/build_scripts/build-app-on-ci.sh"
        self.copy(script)
        self.write("gradlew", self.recorder("gradlew").read_text())
        self.fake("jq", "cat >/dev/null\n")
        self.write("app/desktopApp/build/compose/binaries/main-release/deb/fixture.deb", "artifact")
        (self.root / "deploy").mkdir()
        self.run_script(script, "--target", "desktop-deb", "--url", "api.example.invalid", "--flavor", "fixture",
                        env={"SECRETS_CONTEXT": "{}"})
        self.assertEqual(self.calls("gradlew")[0][0], ["app:desktopApp:packageReleaseDeb",
                         "-Pserver.flavor=fixture", "-Pserver.buildType=prod"])
        self.assertEqual((self.root / "build/outputs/pkg/release/app-fixture.deb").read_text(), "artifact")

    def test_process_owner_rejects_positional_protocol(self):
        result = subprocess.run([sys.executable, str(ROOT / "scripts/tool_scripts/process_container.py"),
                                 "journal", "token", "1000", "1", "echo"], capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn("--status-file", result.stderr)

    def test_shell_runner_rejects_positional_test_ids(self):
        result = subprocess.run([sys.executable, str(ROOT / "scripts/test_scripts/run-shell-tests.py"),
                                 "test_example.Fixture.contract"], capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn("unrecognized arguments", result.stderr)
