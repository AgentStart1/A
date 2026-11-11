# This is a private project. All rights reserved.
"""Run independent Shell regression contracts and write JUnit XML plus raw logs."""
import argparse
import os
from pathlib import Path
import sys
import tempfile
import time
import unittest
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True
TESTS = Path(__file__).resolve().parent / "shell_regression"
sys.path.insert(0, str(TESTS))
from reporting import write_junit

SUITES = {"build-and-test": "test_build_and_test", "images": "test_images",
          "cli": "test_cli", "compose": "test_compose", "desktop": "test_desktop",
          "infrastructure": "test_infrastructure", "prebuilt-deploy": "test_prebuilt_deploy",
          "named-options": "test_named_options"}


def flatten(suite):
    for test in suite:
        if isinstance(test, unittest.TestSuite):
            yield from flatten(test)
        else:
            yield test


class ReportResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.xml = ET.Element("testsuite", name="shell-regression")

    def startTest(self, test):
        super().startTest(test)
        self.started = time.monotonic()
        classname, name = test.id().rsplit(".", 1)
        self.current = ET.SubElement(self.xml, "testcase", classname=classname, name=name)

    def stopTest(self, test):
        self.current.set("time", f"{time.monotonic() - self.started:.3f}")
        super().stopTest(test)

    def addFailure(self, test, err):
        super().addFailure(test, err)
        ET.SubElement(self.current, "failure", message=str(err[1])).text = self._exc_info_to_string(err, test)

    def addError(self, test, err):
        super().addError(test, err)
        ET.SubElement(self.current, "error", message=str(err[1])).text = self._exc_info_to_string(err, test)

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        ET.SubElement(self.current, "skipped", message=reason)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tests", nargs="+", help="Dotted unittest IDs; default discovers every contract")
    parser.add_argument("--suite", choices=SUITES)
    parser.add_argument("--list", action="store_true", help="List selected test IDs without running them")
    args = parser.parse_args()
    loader = unittest.TestLoader()
    if args.tests:
        suite = loader.loadTestsFromNames(args.tests)
    else:
        pattern = SUITES[args.suite] + ".py" if args.suite else "test_*.py"
        suite = loader.discover(str(TESTS), pattern=pattern)
    ids = [test.id() for test in flatten(suite)]
    if not ids:
        parser.error("No tests discovered")
    if args.list:
        sys.stdout.write("\n".join(ids) + "\n")
        for error in loader.errors:
            sys.stderr.write(error + "\n")
        return 1 if loader.errors else 0
    parent = TESTS.parents[2] / "build/test-results/shell-regression"
    parent.mkdir(parents=True, exist_ok=True)
    output = Path(tempfile.mkdtemp(prefix="run-", dir=parent))
    os.environ["SHELL_TEST_OUTPUT"] = str(output)
    (output / "discovered.txt").write_text("\n".join(ids) + "\n", encoding="utf-8")
    sys.stdout.write(f"Shell regression reports: {output}\n")
    result = unittest.TextTestRunner(verbosity=2, resultclass=ReportResult).run(suite)
    result.xml.attrib.update(tests=str(result.testsRun), failures=str(len(result.failures)),
                             errors=str(len(result.errors)), skipped=str(len(result.skipped)))
    write_junit(result.xml, output / "junit.xml")
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main())
