# This is a private project. All rights reserved.
"""Execute a command through an owner that outlives its entire process tree."""
import json
from dataclasses import dataclass, field
from enum import Enum, auto
import os
from pathlib import Path
import subprocess
import sys
import time
import uuid

CONTAINER = Path(__file__).resolve().parents[3] / "scripts/tool_scripts/process_container.py"


class ProcessReleaseError(RuntimeError):
    """The owner did not confirm that its root and all descendants were released."""


class ProcessCleanupError(RuntimeError):
    """Retain every cleanup failure when more than one operation fails."""
    def __init__(self, errors):
        self.errors = tuple(errors)
        super().__init__("; ".join(f"{type(error).__name__}: {error}" for error in self.errors))
        self.__cause__ = self.errors[0]


def cleanup_failure(errors):
    return errors[0] if len(errors) == 1 else ProcessCleanupError(errors)


def finish_owner(owner, lease, stop):
    """Attempt all cleanup operations before deciding which exception to propagate."""
    errors = []
    if not lease.safe_to_remove:
        try:
            stop()
        except BaseException as error:
            errors.append(error)
    try:
        owner.stdin.close()
    except BaseException as error:
        errors.append(error)
    return errors


class ProcessPhase(Enum):
    NOT_STARTED = auto()
    OWNED = auto()
    RELEASED = auto()


@dataclass
class ProcessLease:
    """Track resource ownership independently of the exception returned to a caller."""
    phase: ProcessPhase = field(default=ProcessPhase.NOT_STARTED, init=False)

    @property
    def safe_to_remove(self):
        return self.phase != ProcessPhase.OWNED

    def begin(self):
        if self.phase != ProcessPhase.NOT_STARTED:
            raise RuntimeError("A process lease can only be started once")
        self.phase = ProcessPhase.OWNED

    def confirm_release(self):
        if self.phase == ProcessPhase.NOT_STARTED:
            raise RuntimeError("Only an owned process lease can be released")
        self.phase = ProcessPhase.RELEASED


def run_owned(command, *, cwd, env, output, timeout=45, cleanup_timeout=5, lease=None):
    """Return output only after empty-container acknowledgement and owner exit.

    stdout/stderr use files so inherited pipe handles cannot extend an execution
    deadline. The separate owner holds a Windows Job or Linux subreaper; killing
    the root alone is never taken as evidence that its descendants have stopped.
    """
    output = Path(output)
    lease = lease if lease is not None else ProcessLease()
    status_file = output.with_suffix(".container.jsonl")
    token = uuid.uuid4().hex
    with output.with_suffix(".stdout").open("wb") as stdout, output.with_suffix(".stderr").open("wb") as stderr:
        # Ownership is pending before launch, including interruption during startup.
        lease.begin()
        owner = subprocess.Popen(
            [sys.executable, str(CONTAINER), '--status-file', str(status_file), '--token', token,
             '--timeout-ms', str(int(cleanup_timeout * 1000)), '--parent-pid', str(os.getpid()),
             '--command', *command],
            cwd=cwd, env=env | {"PYTHONDONTWRITEBYTECODE": "1"},
            stdin=subprocess.PIPE, stdout=stdout, stderr=stderr,
        )

        def read_state():
            if not status_file.exists():
                return None
            data = status_file.read_text(encoding="utf-8")
            lines = data[:data.rfind("\n") + 1].splitlines()
            if not lines:
                return None
            state = json.loads(lines[-1])
            if state["token"] != token:
                raise ProcessReleaseError("Container acknowledgement has the wrong identity")
            return state

        def await_release(duration):
            deadline = time.monotonic() + duration
            while True:
                exited = owner.poll()
                state = read_state()
                if state and state["phase"] == "released":
                    if state.get("active") != 0 or state.get("root_code") is None:
                        raise ProcessReleaseError("Container release lacks root exit or empty-tree evidence")
                    try:
                        code = owner.wait(timeout=max(0, deadline - time.monotonic()))
                    except subprocess.TimeoutExpired as error:
                        raise ProcessReleaseError("Released container owner did not exit") from error
                    if code != 0:
                        raise ProcessReleaseError(f"Container owner exited {code} after release")
                    lease.confirm_release()
                    return state["root_code"]
                if state and state["phase"] == "error":
                    raise ProcessReleaseError(state["error"])
                if exited is not None:
                    raise ProcessReleaseError(f"Container owner exited {exited} without release acknowledgement")
                if time.monotonic() >= deadline:
                    raise TimeoutError("Container execution deadline expired")
                time.sleep(0.01)

        def stop():
            try:
                owner.stdin.write(b"terminate\n")
                owner.stdin.flush()
            except (BrokenPipeError, OSError):
                pass
            try:
                return await_release(cleanup_timeout + 1)
            except TimeoutError as error:
                raise ProcessReleaseError("Container did not confirm process-tree cleanup") from error

        failure = None
        try:
            code = await_release(timeout)
        except TimeoutError:
            failure = subprocess.TimeoutExpired(command, timeout)
        except BaseException as error:
            failure = error
        finally:
            errors = finish_owner(owner, lease, stop)
            if isinstance(failure, subprocess.TimeoutExpired):
                for attribute, suffix in (("output", ".stdout"), ("stderr", ".stderr")):
                    try:
                        setattr(failure, attribute, output.with_suffix(suffix).read_bytes())
                    except BaseException as error:
                        errors.append(error)
            if failure is not None:
                if errors:
                    raise failure from cleanup_failure(errors)
                raise failure
            if errors:
                raise cleanup_failure(errors)
    return subprocess.CompletedProcess(command, code,
        output.with_suffix(".stdout").read_bytes(), output.with_suffix(".stderr").read_bytes())
