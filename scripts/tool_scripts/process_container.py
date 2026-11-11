# This is a private project. All rights reserved.
"""Own a verification worker and all descendants until the container is empty."""
import ctypes
import argparse
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import threading
import time


class WindowsContainer:
    def __init__(self, command):
        from ctypes import wintypes as w

        class BasicLimits(ctypes.Structure):
            _fields_ = [("user", ctypes.c_int64), ("job_user", ctypes.c_int64),
                        ("flags", w.DWORD), ("minimum", ctypes.c_size_t),
                        ("maximum", ctypes.c_size_t), ("processes", w.DWORD),
                        ("affinity", ctypes.c_size_t), ("priority", w.DWORD),
                        ("scheduling", w.DWORD)]

        class IoCounters(ctypes.Structure):
            _fields_ = [(name, ctypes.c_uint64) for name in
                        ("read_ops", "write_ops", "other_ops", "read_bytes", "write_bytes", "other_bytes")]

        class ExtendedLimits(ctypes.Structure):
            _fields_ = [("basic", BasicLimits), ("io", IoCounters),
                        ("process_memory", ctypes.c_size_t), ("job_memory", ctypes.c_size_t),
                        ("peak_process", ctypes.c_size_t), ("peak_job", ctypes.c_size_t)]

        class Accounting(ctypes.Structure):
            _fields_ = [(name, ctypes.c_int64) for name in ("user", "kernel", "period_user", "period_kernel")] + [
                (name, w.DWORD) for name in ("faults", "total", "active", "terminated")]

        class Startup(ctypes.Structure):
            _fields_ = [("cb", w.DWORD), ("reserved", w.LPWSTR), ("desktop", w.LPWSTR), ("title", w.LPWSTR),
                        ("x", w.DWORD), ("y", w.DWORD), ("width", w.DWORD), ("height", w.DWORD),
                        ("chars_x", w.DWORD), ("chars_y", w.DWORD), ("fill", w.DWORD), ("flags", w.DWORD),
                        ("show", w.WORD), ("reserved_count", w.WORD), ("reserved_bytes", ctypes.POINTER(ctypes.c_byte)),
                        ("input", w.HANDLE), ("output", w.HANDLE), ("error", w.HANDLE)]

        class ProcessInfo(ctypes.Structure):
            _fields_ = [("process", w.HANDLE), ("thread", w.HANDLE), ("pid", w.DWORD), ("tid", w.DWORD)]

        self.api = ctypes.WinDLL("kernel32", use_last_error=True)
        signatures = {
            "CreateJobObjectW": ([ctypes.c_void_p, w.LPCWSTR], w.HANDLE),
            "SetInformationJobObject": ([w.HANDLE, ctypes.c_int, ctypes.c_void_p, w.DWORD], w.BOOL),
            "QueryInformationJobObject": ([w.HANDLE, ctypes.c_int, ctypes.c_void_p, w.DWORD, ctypes.c_void_p], w.BOOL),
            "AssignProcessToJobObject": ([w.HANDLE, w.HANDLE], w.BOOL),
            "TerminateJobObject": ([w.HANDLE, w.UINT], w.BOOL),
            "CreateProcessW": ([w.LPCWSTR, w.LPWSTR, ctypes.c_void_p, ctypes.c_void_p, w.BOOL,
                                w.DWORD, ctypes.c_void_p, w.LPCWSTR, ctypes.POINTER(Startup), ctypes.POINTER(ProcessInfo)], w.BOOL),
            "ResumeThread": ([w.HANDLE], w.DWORD),
            "TerminateProcess": ([w.HANDLE, w.UINT], w.BOOL),
            "WaitForSingleObject": ([w.HANDLE, w.DWORD], w.DWORD),
            "GetExitCodeProcess": ([w.HANDLE, ctypes.POINTER(w.DWORD)], w.BOOL),
            "CloseHandle": ([w.HANDLE], w.BOOL),
            "SetHandleInformation": ([w.HANDLE, w.DWORD, w.DWORD], w.BOOL),
        }
        for name, (arguments, result) in signatures.items():
            function = getattr(self.api, name)
            function.argtypes = arguments
            function.restype = result
        self.accounting = Accounting
        self.job = self.api.CreateJobObjectW(None, None)
        self.process = None
        if not self.job:
            raise ctypes.WinError(ctypes.get_last_error())
        try:
            limits = ExtendedLimits()
            limits.basic.flags = 0x2000  # JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE; no breakaway.
            self.check(self.api.SetInformationJobObject(self.job, 9, ctypes.byref(limits), ctypes.sizeof(limits)))
            import msvcrt
            startup = Startup()
            startup.cb = ctypes.sizeof(startup)
            startup.flags = 0x100  # STARTF_USESTDHANDLES
            with open(os.devnull, "rb") as null_input:
                startup.input = msvcrt.get_osfhandle(null_input.fileno())
                startup.output = msvcrt.get_osfhandle(sys.stdout.fileno())
                startup.error = msvcrt.get_osfhandle(sys.stderr.fileno())
                self.check(self.api.SetHandleInformation(msvcrt.get_osfhandle(sys.stdin.fileno()), 1, 0))
                for handle in (startup.input, startup.output, startup.error):
                    self.check(self.api.SetHandleInformation(handle, 1, 1))
                information = ProcessInfo()
                line = ctypes.create_unicode_buffer(subprocess.list2cmdline(command))
                self.check(self.api.CreateProcessW(None, line, None, None, True,
                           0x4 | 0x08000000, None, None, ctypes.byref(startup), ctypes.byref(information)))
            self.process = information.process
            self.pid = information.pid
            try:
                self.check(self.api.AssignProcessToJobObject(self.job, self.process))
                if self.api.ResumeThread(information.thread) == 0xFFFFFFFF:
                    raise ctypes.WinError(ctypes.get_last_error())
            except BaseException:
                self.api.TerminateProcess(self.process, 1)
                raise
            finally:
                self.api.CloseHandle(information.thread)
        except BaseException:
            self.close()
            raise

    @staticmethod
    def check(result):
        if not result:
            raise ctypes.WinError(ctypes.get_last_error())

    def root_code(self):
        from ctypes import wintypes as w
        if self.api.WaitForSingleObject(self.process, 0) != 0:
            return None
        code = w.DWORD()
        self.check(self.api.GetExitCodeProcess(self.process, ctypes.byref(code)))
        return code.value

    def active(self):
        values = self.accounting()
        self.check(self.api.QueryInformationJobObject(self.job, 1, ctypes.byref(values), ctypes.sizeof(values), None))
        return values.active

    def terminate(self):
        self.check(self.api.TerminateJobObject(self.job, 1))

    def close(self):
        if self.process:
            self.api.CloseHandle(self.process)
            self.process = None
        if self.job:
            self.api.CloseHandle(self.job)
            self.job = None


class LinuxContainer:
    def __init__(self, command):
        # Orphans from detached process groups are adopted here, not by PID 1.
        libc = ctypes.CDLL(None, use_errno=True)
        if libc.prctl(36, 1, 0, 0, 0) != 0:  # PR_SET_CHILD_SUBREAPER
            raise OSError(ctypes.get_errno(), "Cannot enable child subreaper")
        self.child = subprocess.Popen(command, stdin=subprocess.DEVNULL, start_new_session=True)
        self.pid = self.child.pid
        self.code = None

    def reap(self):
        while True:
            try:
                pid, status = os.waitpid(-1, os.WNOHANG)
            except ChildProcessError:
                return
            if pid == 0:
                return
            if pid == self.pid:
                self.code = os.waitstatus_to_exitcode(status)
                self.child.returncode = self.code

    def root_code(self):
        self.reap()
        return self.code

    def children(self):
        return [int(value) for value in Path(f"/proc/self/task/{os.getpid()}/children").read_text().split()]

    def active(self):
        self.reap()
        # Zero direct children proves there are no living descendants: detached
        # orphans are adopted before their former parent can be reaped.
        return len(self.children())

    def terminate(self):
        # Direct children cannot have their PIDs reused until this owner reaps
        # them. Killing successive adopted generations also covers setsid().
        for pid in self.children():
            try:
                os.kill(pid, signal.SIGKILL)
            except ProcessLookupError:
                pass

    def close(self):
        # Keep the subreaper alive on an exceptional path until its descendants
        # are gone. The supervisor stops the run if release is unconfirmed.
        while self.active():
            try:
                self.terminate()
            except PermissionError:
                pass
            time.sleep(0.05)


def main():
    if sys.version_info < (3, 10):
        raise RuntimeError("Verification process ownership requires Python 3.10+")
    if os.name != "nt" and sys.platform != "linux":
        raise RuntimeError("Verification containers support Windows and Linux")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--status-file', required=True, type=Path)
    parser.add_argument('--token', required=True)
    parser.add_argument('--timeout-ms', required=True, type=float)
    parser.add_argument('--parent-pid', required=True, type=int)
    parser.add_argument('--command', required=True, nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if not args.command:
        parser.error('--command requires an executable and its arguments')
    status_file = args.status_file
    token = args.token
    timeout = args.timeout_ms / 1000
    parent_pid = args.parent_pid
    command = args.command
    stopped = threading.Event()

    def control():
        pending = b""
        while True:
            data = os.read(sys.stdin.fileno(), 4096)
            if not data:
                stopped.set()  # Supervisor disappeared; release its container.
                return
            pending += data
            lines = pending.split(b"\n")
            pending = lines.pop()
            if any(line.strip() == b"terminate" for line in lines):
                stopped.set()
                return

    threading.Thread(target=control, daemon=True).start()
    if os.name == "nt":
        from ctypes import wintypes as w
        api = ctypes.WinDLL("kernel32", use_last_error=True)
        api.OpenProcess.argtypes = [w.DWORD, w.BOOL, w.DWORD]
        api.OpenProcess.restype = w.HANDLE
        api.WaitForSingleObject.argtypes = [w.HANDLE, w.DWORD]
        api.CloseHandle.argtypes = [w.HANDLE]
        parent = api.OpenProcess(0x00100000, False, parent_pid)  # SYNCHRONIZE
        if not parent:
            stopped.set()
        else:
            def watch_parent():
                api.WaitForSingleObject(parent, 0xFFFFFFFF)
                stopped.set()
                api.CloseHandle(parent)
            threading.Thread(target=watch_parent, daemon=True).start()
    else:
        signal.signal(signal.SIGTERM, lambda *_: stopped.set())
        signal.signal(signal.SIGINT, lambda *_: stopped.set())
        libc = ctypes.CDLL(None, use_errno=True)
        if libc.prctl(1, signal.SIGTERM, 0, 0, 0) != 0:  # PR_SET_PDEATHSIG
            raise OSError(ctypes.get_errno(), "Cannot watch supervisor lifetime")
        if os.getppid() != parent_pid:
            stopped.set()
    container = None
    status = {"token": token, "phase": "starting", "root_pid": None, "root_code": None, "owner_pid": os.getpid()}

    def publish():
        # Append complete records instead of replacing a file another process
        # may have open on Windows. Readers ignore an unfinished final line.
        with status_file.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(status) + "\n")
            stream.flush()

    try:
        if os.name == "nt":
            container = WindowsContainer(command)
        elif sys.platform == "linux":
            container = LinuxContainer(command)
        else:
            raise RuntimeError("Verification containers support Windows and Linux")
        status.update(phase="running", root_pid=container.pid)
        publish()
        deadline = None
        previous = None
        while True:
            active = container.active()
            code = container.root_code()
            status.update(root_code=code, active=active)
            if active == 0 and code is not None:
                status["phase"] = "released"
                publish()
                return 0
            if stopped.is_set():
                if deadline is None:
                    deadline = time.monotonic() + timeout
                    status["forced"] = True
                container.terminate()
                if time.monotonic() >= deadline:
                    raise TimeoutError("Cannot confirm that the owned container is empty")
            status["phase"] = "root-exited" if code is not None else "running"
            summary = (status["phase"], code, active, bool(status.get("forced")))
            if summary != previous:
                publish()
                previous = summary
            time.sleep(0.01)
    except BaseException as error:
        status.update(phase="error", error=f"{type(error).__name__}: {error}")
        publish()
        return 1
    finally:
        if container is not None:
            container.close()


if __name__ == "__main__":
    sys.exit(main())
