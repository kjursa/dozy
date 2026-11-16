#!/usr/bin/env python3

import contextlib
import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request


ROOT = Path(__file__).resolve().parent.parent
WRAPPER = ROOT / "backend" / "gradlew"
LAUNCHER = ROOT / "backend" / "build" / "install" / "dozy-backend" / "bin" / "dozy-backend"
REPORT = ROOT / "e2e" / "build" / "reports" / "tests" / "test" / "index.html"


def stop_process(process):
    try:
        os.killpg(process.pid, signal.SIGTERM)
    except ProcessLookupError:
        return
    try:
        process.wait(timeout=10)
    except subprocess.TimeoutExpired:
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.wait()


@contextlib.contextmanager
def managed_process(command, **options):
    process = subprocess.Popen(command, start_new_session=True, **options)
    try:
        yield process
    finally:
        stop_process(process)


def gradle(project, *tasks, env=None):
    command = [str(WRAPPER), "-p", str(ROOT / project), "--no-daemon", "--console=plain", *tasks]
    with managed_process(command, cwd=ROOT, env=env) as process:
        return process.wait()


def available_port():
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        return listener.getsockname()[1]


def backend_environment(directory, port):
    env = os.environ.copy()
    env.update(
        HOST="127.0.0.1",
        PORT=str(port),
        DOZY_DATABASE_PATH=str(directory / "dozy.db"),
        DOZY_DATABASE_BUSY_TIMEOUT_MS="5000",
        DOZY_AVATAR_STORAGE_PATH=str(directory / "assets"),
        DOZY_AVATAR_MODE="SUCCESS",
        DOZY_AVATAR_DELAY_MS="0",
        DOZY_AVATAR_CONCURRENCY="1",
        DOZY_ASSET_PUBLIC_BASE_URL=f"http://127.0.0.1:{port}",
        DOZY_ASSET_ALLOW_LOCAL_HTTP="true",
        DOZY_ASSET_BASE_URL=f"http://127.0.0.1:{port}",
        DOZY_ASSET_ALLOW_HTTP="true",
        DOZY_STORY_TEXT_MODE="SUCCESS",
        DOZY_STORY_COVER_MODE="SUCCESS",
        DOZY_STORY_DELAY_MS="0",
        DOZY_STORY_CONCURRENCY="1",
    )
    return env


def wait_until_ready(process, log, base_url):
    deadline = time.monotonic() + 60
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError("Backend exited before becoming ready")
        if f"Responding at {base_url}" in log.read_text(errors="replace"):
            try:
                with opener.open(f"{base_url}/hello", timeout=1) as response:
                    if json.loads(response.read(4096)).get("message") == "Hello world from Dozy":
                        if process.poll() is None:
                            return
            except (urllib.error.URLError, TimeoutError, ValueError):
                pass
        time.sleep(0.2)
    raise TimeoutError("Backend did not become ready within 60 seconds")


def print_backend_log(log):
    print("\nBackend log (last 80 lines):", file=sys.stderr, flush=True)
    print("\n".join(log.read_text(errors="replace").splitlines()[-80:]), file=sys.stderr, flush=True)


def run_places():
    print("Building the standalone backend distribution...", flush=True)
    result = gradle("backend", "installDist")
    if result != 0:
        return result
    with tempfile.TemporaryDirectory(prefix="dozy-e2e-") as temporary:
        directory = Path(temporary)
        port = available_port()
        base_url = f"http://127.0.0.1:{port}"
        log = directory / "backend.log"
        print(f"Starting isolated backend at {base_url}...", flush=True)
        with log.open("w") as output:
            with managed_process([str(LAUNCHER)], cwd=directory, env=backend_environment(directory, port), stdout=output, stderr=subprocess.STDOUT) as server:
                try:
                    wait_until_ready(server, log, base_url)
                    env = os.environ.copy()
                    env["DOZY_E2E_BASE_URL"] = base_url
                    result = gradle("e2e", "test", "--tests", "com.dozy.e2e.PlacesTest", env=env)
                    if result != 0:
                        print_backend_log(log)
                    return result
                except (Exception, KeyboardInterrupt):
                    print_backend_log(log)
                    raise


def interrupted(signum, frame):
    raise KeyboardInterrupt


def main():
    signal.signal(signal.SIGTERM, interrupted)
    try:
        result = run_places()
    except KeyboardInterrupt:
        print("\nPlacesTest: INTERRUPTED (owned processes stopped)", file=sys.stderr, flush=True)
        return 130
    except Exception as failure:
        print(f"\nPlacesTest: FAILED ({failure})", file=sys.stderr, flush=True)
        return 1
    print(f"\nPlacesTest: {'PASSED' if result == 0 else 'FAILED'}", flush=True)
    print(f"Report: {REPORT}", flush=True)
    return result if result >= 0 else 1


if __name__ == "__main__":
    sys.exit(main())
