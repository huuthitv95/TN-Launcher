#!/usr/bin/env python3
"""API 29 emulator-only TN Launcher journeys; JSON to stdout, no report files.

Preinstall app-debug.apk and app-debug-androidTest.apk, then run:
  python tools/emulator-e2e.py --adb <adb.exe> --serial emulator-5554 \
      --fixture-apk <app-debug-androidTest.apk> --wallpaper <test.png>

Changes only the explicitly selected emulator: selects TN Home, pins the test
fixture and selects Dark. Optional fixture replacement removes only the known
test package. Screenshots (at most two) go outside the repository. This script
does not validate Wincavn hardware, camera switching, or automotive safety.
"""

import argparse
import json
import os
from pathlib import Path
import re
import statistics
import struct
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
import zipfile


APP = "vn.tnlauncher.app"
HOME = APP + "/.ui.HomeActivity"
FIXTURE_PACKAGE = APP + ".test"
FIXTURE = FIXTURE_PACKAGE + "/vn.tnlauncher.app.FixtureActivity"
FIXTURE_LABEL = "Ứng dụng mẫu A"


class Failure(RuntimeError):
    pass


def apk_package(path):
    """Read only the root manifest package from an APK's compiled XML.

    No APK code is loaded or executed. Bound the sole ZIP entry read, validate
    chunk lengths, and fail closed if the manifest format is unsupported.
    """
    with zipfile.ZipFile(path) as archive:
        entry = archive.getinfo("AndroidManifest.xml")
        require(0 < entry.file_size <= 4 * 1024 * 1024, "APK manifest exceeds inspection bound")
        data = archive.read(entry)
    if data.lstrip().startswith(b"<"):
        root = ET.fromstring(data)
        require(root.tag == "manifest", "APK root element is not manifest")
        return root.get("package")

    def uint16(offset):
        return struct.unpack_from("<H", data, offset)[0]

    def uint32(offset):
        return struct.unpack_from("<I", data, offset)[0]

    require(len(data) >= 8 and uint16(0) == 0x0003, "Unsupported APK manifest format")
    document_end = uint32(4)
    require(8 <= document_end <= len(data), "Invalid APK manifest length")
    strings = None
    offset = uint16(2)
    while offset + 8 <= document_end:
        kind, header_size, size = struct.unpack_from("<HHI", data, offset)
        end = offset + size
        require(8 <= header_size <= size and end <= document_end, "Invalid manifest chunk")
        if kind == 0x0001:  # ResStringPool_header
            require(header_size >= 28, "Invalid manifest string pool header")
            count, flags, string_start = uint32(offset + 8), uint32(offset + 16), uint32(offset + 20)
            require(count <= 100000 and header_size + count * 4 <= size, "Invalid manifest string count")
            pool = []
            for index in range(count):
                cursor = offset + string_start + uint32(offset + header_size + index * 4)
                require(offset + string_start <= cursor < end, "Manifest string outside pool")
                if flags & 0x100:  # UTF-8 pool: UTF-16 length followed by byte length.
                    def length8(position):
                        require(position < end, "Truncated manifest string length")
                        first = data[position]
                        if first & 0x80:
                            require(position + 1 < end, "Truncated manifest string length")
                            return ((first & 0x7f) << 8) | data[position + 1], position + 2
                        return first, position + 1
                    _, cursor = length8(cursor)
                    length, cursor = length8(cursor)
                    require(cursor + length < end, "Truncated UTF-8 manifest string")
                    pool.append(data[cursor:cursor + length].decode("utf-8"))
                else:
                    require(cursor + 2 <= end, "Truncated manifest string length")
                    length = uint16(cursor)
                    cursor += 2
                    if length & 0x8000:
                        require(cursor + 2 <= end, "Truncated manifest string length")
                        length = ((length & 0x7fff) << 16) | uint16(cursor)
                        cursor += 2
                    require(cursor + length * 2 + 2 <= end, "Truncated UTF-16 manifest string")
                    pool.append(data[cursor:cursor + length * 2].decode("utf-16-le"))
            strings = pool
        elif kind == 0x0102:  # ResXMLTree_attrExt, after the XML node header.
            require(strings is not None and header_size >= 16 and header_size + 20 <= size,
                    "Manifest element missing string pool or attributes")
            extension = offset + header_size
            name = uint32(extension + 4)
            require(name < len(strings) and strings[name] == "manifest", "APK root element is not manifest")
            start, attribute_size, count = struct.unpack_from("<HHH", data, extension + 8)
            require(start >= 20 and attribute_size >= 20 and extension + start + count * attribute_size <= end,
                    "Invalid manifest root attributes")
            for index in range(count):
                attribute = extension + start + index * attribute_size
                namespace, name, raw = struct.unpack_from("<III", data, attribute)
                require(name < len(strings), "Invalid manifest attribute name")
                if namespace == 0xffffffff and strings[name] == "package":
                    value = raw if raw != 0xffffffff else uint32(attribute + 16)
                    require(raw != 0xffffffff or data[attribute + 15] == 3,
                            "Manifest package is not a string")
                    require(value < len(strings), "Invalid manifest package value")
                    return strings[value]
            raise Failure("APK manifest package missing")
        offset = end
    raise Failure("APK manifest root missing")


class Emulator:
    def __init__(self, adb, serial):
        self.adb = adb
        self.serial = serial
        self.dump_path = "/sdcard/tn-launcher-e2e-%s.xml" % os.getpid()
        self.crash_started_epoch = None
        self.folder_label = None

    def command(self, *args, binary=False, timeout=30, check=True):
        result = subprocess.run([self.adb, "-s", self.serial, *map(str, args)],
                                capture_output=True, timeout=timeout)
        if check and result.returncode:
            raise Failure("adb %s: %s" % (" ".join(map(str, args[:4])),
                          result.stderr.decode("utf-8", errors="replace")[-1200:]))
        if binary:
            return result.stdout
        return result.stdout.decode("utf-8", errors="replace").strip()

    def shell(self, *args, **kwargs):
        return self.command("shell", *args, **kwargs)

    def guard(self):
        if not self.serial.startswith("emulator-"):
            raise Failure("Only an explicit emulator-* serial is accepted")
        if self.command("get-state") != "device":
            raise Failure("Selected emulator is not online")
        sdk = self.shell("getprop", "ro.build.version.sdk")
        qemu = self.shell("getprop", "ro.kernel.qemu")
        if sdk != "29" or qemu != "1":
            raise Failure("Requires ro.kernel.qemu=1 and API 29; got %s/%s" % (qemu, sdk))
        for package in (APP, FIXTURE_PACKAGE):
            if not self.shell("pm", "path", package).startswith("package:"):
                raise Failure("Required preinstalled package missing: " + package)
        # wm size can be clamped or rotated by the emulator. Read the actual
        # rendered framebuffer dimensions instead of relabeling requested sizes.
        pixels = self.command("exec-out", "screencap", "-p", binary=True)
        require(len(pixels) >= 24 and pixels[:8] == b"\x89PNG\r\n\x1a\n" and pixels[12:16] == b"IHDR",
                "Cannot establish actual emulator viewport from framebuffer PNG")
        width, height = struct.unpack(">II", pixels[16:24])
        return {"serial": self.serial, "sdk": sdk, "qemu": qemu,
                "display": self.shell("wm", "size"), "density": self.shell("wm", "density"),
                "font_scale": self.shell("settings", "get", "system", "font_scale"),
                "animation_scales": {key: self.shell("settings", "get", "global", key) for key in
                                     ("window_animation_scale", "transition_animation_scale", "animator_duration_scale")},
                "actual_viewport": {"width_px": width, "height_px": height,
                                    "source": "Current screencap PNG IHDR; not a requested display size"}}

    def tree(self):
        last = None
        for _ in range(3):
            try:
                self.shell("uiautomator", "dump", self.dump_path, timeout=20)
                raw = self.command("exec-out", "cat", self.dump_path)
                return ET.fromstring(raw[raw.index("<?xml"):])
            except (ValueError, ET.ParseError, Failure, subprocess.TimeoutExpired) as error:
                last = error
                time.sleep(0.3)
        raise Failure("UI hierarchy unavailable: " + str(last))

    @staticmethod
    def find(tree, resource=None, text=None, description=None, within=None, contains=None):
        if within:
            tree = next((n for n in tree.iter("node")
                         if n.get("resource-id") == APP + ":id/" + within), None)
            if tree is None:
                return None
        for node in tree.iter("node"):
            if resource and node.get("resource-id") != APP + ":id/" + resource:
                continue
            if text is not None and node.get("text") != text:
                continue
            if description is not None and node.get("content-desc") != description:
                continue
            if contains is not None and contains not in node.get("text", "") and contains not in node.get("content-desc", ""):
                continue
            bounds = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
            if bounds and int(bounds[3]) > int(bounds[1]) and int(bounds[4]) > int(bounds[2]):
                return node
        return None

    def wait_node(self, timeout=12, **selector):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            node = self.find(self.tree(), **selector)
            if node is not None:
                return node
            time.sleep(0.25)
        raise Failure("UI element missing: " + repr(selector))

    @staticmethod
    def bounds(node):
        numbers = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
        if len(numbers) != 4:
            raise Failure("Invalid UI bounds")
        return numbers

    def tap_node(self, node, long=False):
        left, top, right, bottom = self.bounds(node)
        x, y = (left + right) // 2, (top + bottom) // 2
        if long:
            self.shell("input", "swipe", x, y, x, y, 850)
        else:
            self.shell("input", "tap", x, y)

    def tap(self, **selector):
        self.tap_node(self.wait_node(**selector))

    def home_item(self, description):
        # A denser display has fewer dp and can put a restored item on page two.
        for _ in range(128):
            tree = self.tree()
            item = self.find(tree, description=description)
            if item is not None:
                return item
            next_page = self.find(tree, resource="next")
            if next_page is None or next_page.get("enabled") != "true":
                break
            self.tap_node(next_page)
        raise Failure("Home item missing across its actual pages: " + description)

    def scroll_to(self, text):
        # Settings can retain its scroll offset when returning from another app.
        for downward in (True, False):
            for _ in range(8):
                tree = self.tree()
                node = self.find(tree, text=text)
                if node is not None:
                    return node
                scroll = next((n for n in tree.iter("node") if n.get("scrollable") == "true"), None)
                if scroll is None:
                    break
                left, top, right, bottom = self.bounds(scroll)
                x = (left + right) // 2
                upper, lower = top + (bottom - top) // 5, bottom - (bottom - top) // 5
                self.shell("input", "swipe", x, lower if downward else upper,
                           x, upper if downward else lower, 350)
        raise Failure("Scrollable UI element missing: " + text)

    def foreground(self):
        component = self.resumed_component()
        return component.split("/", 1)[0] if component else None

    def resumed_component(self):
        output = self.shell("dumpsys", "activity", "activities")
        match = re.search(r"(?:mResumedActivity|topResumedActivity).*?\s([A-Za-z][\w.]*/[\w.$]+)", output)
        return match[1] if match else None

    def wait_recents(self, timeout=12):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            component = self.resumed_component()
            if component and component.split("/", 1)[0] not in (APP, FIXTURE_PACKAGE) and \
                    component.rsplit(".", 1)[-1] == "RecentsActivity":
                return {"resumed_component": component}
            for node in self.tree().iter("node"):
                package = node.get("package")
                resource = node.get("resource-id", "").rsplit("/", 1)[-1]
                if package in ("com.android.launcher3", "com.google.android.apps.nexuslauncher", "com.android.systemui") and \
                        resource in ("overview_panel", "recents_view", "recents_root", "task_view"):
                    left, top, right, bottom = self.bounds(node)
                    if right > left and bottom > top:
                        return {"ui_package": package, "resource_id": node.get("resource-id")}
            time.sleep(0.25)
        raise Failure("Recents was not observed in resumed activity or visible system overview UI")

    def wait_foreground(self, package, timeout=12):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if self.foreground() == package:
                return
            time.sleep(0.25)
        raise Failure("Foreground is %r; expected %r" % (self.foreground(), package))

    def home(self):
        self.shell("input", "keyevent", "KEYCODE_HOME")
        self.wait_foreground(APP)
        self.wait_node(resource="grid")

    def start(self, component=HOME):
        output = self.shell("am", "start", "-W", "-n", component)
        if "Error:" in output or "Exception" in output:
            raise Failure(output[-1200:])
        return output

    def search_fixture(self):
        self.home()
        self.tap(resource="apps_button")
        self.tap(resource="search")
        self.shell("input", "text", "ung%sdung%smau%sa")
        self.shell("input", "keyevent", "KEYCODE_BACK")
        return self.wait_node(text=FIXTURE_LABEL, within="grid")

    def preferences(self):
        return self.command("exec-out", "run-as", APP, "cat", "files/datastore/launcher.preferences_pb", binary=True)

    def wallpaper_names(self):
        names = self.shell("run-as", APP, "ls", "files").splitlines()
        return sorted(n for n in names if re.fullmatch(r"wallpaper-[A-Za-z0-9.-]+", n))

    def select_theme_and_wait_commit(self, label, value, timeout=12):
        self.tap(text=label)
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            node = self.find(self.tree(), text=label)
            if node is not None and node.get("checked") == "true" and value.encode() in self.preferences():
                return
            time.sleep(0.2)
        raise Failure("Theme was not checked and committed: " + value)

    def check_run_crashes(self):
        require(self.crash_started_epoch is not None, "Crash observation start time missing")
        # Read all retained crash events, not an arbitrary last-N line sample.
        output = self.shell("logcat", "-d", "-b", "crash", "-v", "epoch")
        relevant = []
        for line in output.splitlines():
            timestamp = re.match(r"\s*(\d{9,}\.\d+)\s", line)
            if timestamp and float(timestamp[1]) >= self.crash_started_epoch and \
                    re.search(r"(?:Process:\s*|Cmdline:\s*|>>>\s*)" + re.escape(APP) + r"(?:,|\s|$)", line):
                relevant.append(line)
        require(not relevant, "Launcher crash observed during this run")
        events = self.shell("logcat", "-d", "-b", "events", "-v", "epoch")
        for line in events.splitlines():
            timestamp = re.match(r"\s*(\d{9,}\.\d+)\s", line)
            if timestamp and float(timestamp[1]) >= self.crash_started_epoch and "am_anr" in line:
                require(not re.search(r"(?<![\w.])" + re.escape(APP) + r"(?![\w.])", line),
                        "Launcher ANR observed during this run")
        return {"launcher_crashes_observed": False, "launcher_anrs_observed": False,
                "from_device_epoch_second": self.crash_started_epoch,
                "method": "Retained epoch crash buffer and am_anr events since emulator start marker (one-second precision)",
                "limit": "Android log buffers are finite; this is observation, not proof against every crash or ANR"}


def require(condition, message):
    if not condition:
        raise Failure(message)


def search_and_back(e):
    e.home()
    e.tap(resource="apps_button")
    e.tap(resource="search")
    e.shell("input", "text", "__no_installed_app__")
    e.shell("input", "keyevent", "KEYCODE_BACK")
    e.wait_node(text="Không tìm thấy ứng dụng phù hợp.")
    # Back clears the query before the next Back returns to Home.
    e.shell("input", "keyevent", "KEYCODE_BACK")
    node = e.wait_node(resource="search")
    # UIAutomator may expose an empty EditText's hint as its text on API 29.
    require(node.get("text", "") in ("", "Tìm ứng dụng…"), "Back did not clear the search query")
    e.shell("input", "keyevent", "KEYCODE_BACK")
    tree = e.tree()
    require(e.find(tree, resource="search") is None, "Search remains visible after Back to Home")
    require(e.find(tree, resource="grid") is not None, "Home grid missing")


def pin_fixture(e):
    e.tap_node(e.search_fixture(), long=True)
    tree = e.tree()
    pin = e.find(tree, text="Ghim vào Dock")
    if pin is not None:
        e.tap_node(pin)
    else:
        require(e.find(tree, text="Bỏ ghim khỏi Dock") is not None,
                "Fixture menu has neither pin nor unpin; Dock may already contain six other apps")
        e.shell("input", "keyevent", "KEYCODE_BACK")
    e.home()
    e.wait_node(text=FIXTURE_LABEL, within="favorites")
    require(FIXTURE.encode() in e.preferences(), "Fixture favorite was not persisted")


def dark_persistence(e):
    e.home()
    e.tap(resource="settings_button")
    e.select_theme_and_wait_commit("Sáng", "LIGHT")
    e.select_theme_and_wait_commit("Tối", "DARK")
    e.shell("am", "force-stop", APP)
    e.start()
    e.wait_foreground(APP)
    e.tap(resource="settings_button")
    node = e.wait_node(text="Tối")
    require(node.get("checked") == "true", "Dark theme was not retained after force-stop/start")
    e.home()
    e.wait_node(text=FIXTURE_LABEL, within="favorites")
    return {"theme": "DARK", "transition": "LIGHT committed, then DARK committed", "favorite_retained": True}


def app_launch_and_foreground(e):
    e.tap_node(e.search_fixture())
    e.wait_foreground(FIXTURE_PACKAGE)
    time.sleep(2)
    require(e.foreground() == FIXTURE_PACKAGE, "Home reclaimed foreground from fixture")
    e.shell("input", "keyevent", "KEYCODE_APP_SWITCH")
    recents = e.wait_recents()
    e.home()
    return {"launched_via_ui": FIXTURE, "foreground_hold_seconds": 2, "recents_observed": recents, "recents_return": True}


def folder_create(e):
    e.home()
    e.tap(resource="settings_button")
    e.tap_node(e.scroll_to("Tạo thư mục"))
    field = next(n for n in e.tree().iter("node") if n.get("class") == "android.widget.EditText")
    e.tap_node(field)
    e.shell("input", "keyevent", "KEYCODE_MOVE_END")
    e.shell("input", "keyevent", *(["KEYCODE_DEL"] * 20))
    e.folder_label = "TN-" + str(time.time_ns())[-9:]
    e.shell("input", "text", e.folder_label)
    ime = e.shell("dumpsys", "input_method")
    if re.search(r"mInputShown=true|mIsInputViewShown=true", ime):
        e.shell("input", "keyevent", "KEYCODE_BACK")
    e.tap_node(e.scroll_to(FIXTURE_LABEL))
    save = next(n for n in e.tree().iter("node") if n.get("resource-id") == "android:id/button1")
    e.tap_node(save)
    e.home()
    e.tap_node(e.home_item(e.folder_label))
    e.wait_node(text=FIXTURE_LABEL)
    e.home()
    return {"folder_created_via_ui": True, "real_fixture_member": FIXTURE,
            "follow_up": "The following process-death and package-change journeys also validate this folder"}


def process_death(e):
    e.home()
    before_probe = set(e.shell("pidof", APP).split())
    require(bool(before_probe), "Launcher has no process before process-death probe")

    def current_pids():
        return set(e.shell("pidof", APP, check=False).split())

    # Android may restart the target before starting instrumentation. The probe
    # reports the actual process it backgrounds and terminates from within its
    # own process; a pre-instrumentation PID change alone proves nothing.
    output = e.shell("am", "instrument", "-w", "-r", "-e", "confirm", "emulator-api29-only",
                     FIXTURE_PACKAGE + "/vn.tnlauncher.app.LauncherTestRunner", check=False, timeout=45)
    reported = set(re.findall(r"^INSTRUMENTATION_STATUS: probe_pid=(\d+)\s*$", output, re.MULTILINE))
    require(len(reported) == 1, "Process-death probe did not report one target PID: " + output[-1200:])
    require("INSTRUMENTATION_STATUS: probe_phase=background_before_self_termination" in output and
            "INSTRUMENTATION_STATUS: probe_expected_termination=true" in output,
            "Probe did not confirm background state and expected self-termination")
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline and reported & current_pids():
        time.sleep(0.25)
    require(not reported & current_pids(), "The probe's background Home process was not terminated")
    e.home()
    after = current_pids()
    require(bool(after) and not reported & after, "Home process was not recreated with new PIDs")
    e.wait_node(text=FIXTURE_LABEL, within="favorites")
    require(b"DARK" in e.preferences(), "Theme lost during process recreation")
    e.tap(resource="settings_button")
    dark = e.wait_node(text="Tối")
    require(dark.get("checked") == "true", "Restored Home did not load Dark theme into Settings UI")
    e.home()
    if e.folder_label:
        e.tap_node(e.home_item(e.folder_label))
        e.wait_node(text=FIXTURE_LABEL)
        e.home()
    termination_report = [line for line in output.splitlines() if
                          re.match(r"INSTRUMENTATION_(?:FAILED|RESULT|CODE|STATUS_CODE)", line)]
    return {"pid_changed": True, "before_instrumentation_pids": sorted(before_probe),
            "terminated_probe_pids": sorted(reported), "restored_pids": sorted(after),
            "method": "Test-APK instrumentation backgrounds Home and kills only its own Process.myPid()",
            "probe_result_kind": "Intentional instrumentation termination; separate from AndroidJUnitRunner tests",
            "instrumentation_failure_expected": True, "instrumentation_termination_report": termination_report,
            "favorite_retained": True, "theme_retained": True, "restored_dark_ui_checked": True,
            "folder_and_member_restored": bool(e.folder_label)}


def replace_fixture(e, apk):
    require(apk_package(apk) == FIXTURE_PACKAGE, "Replacement APK must have package " + FIXTURE_PACKAGE)
    e.home()
    removed = False
    try:
        e.tap_node(e.search_fixture(), long=True)
        e.tap(text="Gỡ ứng dụng…")
        confirm = None
        deadline = time.monotonic() + 12
        while time.monotonic() < deadline:
            confirm = next((n for n in e.tree().iter("node") if n.get("resource-id") == "android:id/button1"
                            and "packageinstaller" in n.get("package", "")), None)
            if confirm is not None:
                break
            time.sleep(.25)
        require(confirm is not None and "packageinstaller" in (e.foreground() or ""),
                "Uninstall confirmation is not owned by Android")
        e.tap_node(confirm)
        removed = True
        deadline = time.monotonic() + 12
        while time.monotonic() < deadline and e.shell("pm", "path", FIXTURE_PACKAGE, check=False).startswith("package:"):
            time.sleep(.25)
        require(not e.shell("pm", "path", FIXTURE_PACKAGE, check=False).startswith("package:"),
                "System-confirmed fixture uninstall did not complete")
        e.home()
        e.wait_node(text="Không có trên máy", within="favorites")
        require(FIXTURE.encode() in e.preferences(), "Unavailable favorite was erased")
        if e.folder_label:
            e.tap_node(e.home_item(e.folder_label))
            e.wait_node(text="Ứng dụng không có trên máy")
            e.home()
        e.tap(resource="apps_button")
        e.tap(resource="search")
        e.shell("input", "text", "ung%sdung%smau%sa")
        e.shell("input", "keyevent", "KEYCODE_BACK")
        e.wait_node(text="Không tìm thấy ứng dụng phù hợp.")
    finally:
        if removed:
            output = e.command("install", "-r", str(apk), timeout=90)
            require("Success" in output, "Could not restore test-owned fixture APK: " + output[-500:])
    e.home()
    e.wait_node(text=FIXTURE_LABEL, within="favorites")
    e.search_fixture()
    e.home()
    # A package replacement must also refresh discovery while retaining Dock.
    output = e.command("install", "-r", str(apk), timeout=90)
    require("Success" in output, "Fixture update failed")
    e.wait_node(text=FIXTURE_LABEL, within="favorites")
    if e.folder_label:
        e.tap_node(e.home_item(e.folder_label))
        e.wait_node(text=FIXTURE_LABEL)
        e.home()
        e.tap_node(e.home_item(e.folder_label), long=True)
        e.tap(text="Xóa thư mục · đưa ứng dụng về Home")
        deadline = time.monotonic() + 8
        while time.monotonic() < deadline and e.find(e.tree(), description=e.folder_label) is not None:
            time.sleep(.25)
        require(e.find(e.tree(), description=e.folder_label) is None, "Test folder deletion did not restore Home")
    return {"uninstall_missing_discovery": True, "uninstalled_via_system_confirmation": True, "missing_favorite_retained": True,
            "reinstall_and_update_discovered": True, "folder_membership_retained_and_folder_removed": bool(e.folder_label)}


def wallpaper_picker(e, source):
    remote_name = "tn-launcher-e2e-%s.png" % time.time_ns()
    remote = "/sdcard/Download/" + remote_name
    before = e.wallpaper_names()
    try:
        e.command("push", str(source), remote)
        e.shell("am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
                "-d", "file://" + remote)
        e.home()
        e.tap(resource="settings_button")
        e.tap_node(e.scroll_to("Chọn hình nền riêng"))
        deadline = time.monotonic() + 12
        while time.monotonic() < deadline and "documentsui" not in (e.foreground() or ""):
            time.sleep(0.25)
        require("documentsui" in (e.foreground() or ""), "System document picker did not open")
        tree = e.tree()
        download = next((n for n in tree.iter("node") if n.get("text") in ("Downloads", "Tải xuống")), None)
        if download is None:
            roots = next((n for n in tree.iter("node") if n.get("content-desc") in
                          ("Show roots", "Hiển thị gốc", "Open navigation drawer")), None)
            if roots is None:
                roots = next((n for n in tree.iter("node") if n.get("class") == "android.widget.ImageButton"
                              and n.get("clickable") == "true"), None)
            require(roots is not None, "Document picker root menu missing")
            e.tap_node(roots)
            tree = e.tree()
            download = next((n for n in tree.iter("node") if n.get("text") in ("Downloads", "Tải xuống")), None)
        require(download is not None, "Downloads root unavailable")
        e.tap_node(download)
        e.tap(contains=remote_name.rsplit(".", 1)[0])
        e.wait_foreground(APP)
        deadline = time.monotonic() + 10
        after = before
        imported = []
        committed = False
        while time.monotonic() < deadline:
            after = e.wallpaper_names()
            imported = list(set(after) - set(before))
            committed = bool(imported) and any(name.encode() in e.preferences() for name in imported)
            if committed:
                break
            time.sleep(0.25)
        require(bool(imported), "SAF selection did not produce app-private wallpaper")
        require(committed, "Imported wallpaper was not committed")
        e.home()
        return {"system_picker": True, "private_wallpaper_committed": True}
    finally:
        e.shell("rm", "-f", remote, check=False)


def performance(e):
    cold = []
    for _ in range(3):
        # Keep another app foreground so Android does not immediately recreate
        # its default Home between force-stop and the timed explicit start.
        e.start(FIXTURE)
        e.wait_foreground(FIXTURE_PACKAGE)
        previous_pids = set(e.shell("pidof", APP, check=False).split())
        e.shell("am", "force-stop", APP)
        require(not e.shell("pidof", APP, check=False), "Home restarted before cold-start timing; refusing a warm sample")
        output = e.start()
        e.wait_foreground(APP)
        e.wait_node(resource="grid")
        require(not previous_pids.intersection(e.shell("pidof", APP).split()), "Cold-start process was not replaced")
        cold.append({key: int(value) for key, value in
                     re.findall(r"^(ThisTime|TotalTime|WaitTime):\s*(\d+)", output, re.MULTILINE)})
    require(all("TotalTime" in sample for sample in cold), "Cold startup timing missing")
    pids = e.shell("pidof", APP).split()
    require(bool(pids), "Launcher process missing before CPU sample")
    pid = pids[0]
    ticks_per_second = e.shell("getconf", "CLK_TCK")
    require(ticks_per_second.isdigit(), "Clock tick rate unavailable for idle CPU measurement")

    def process_ticks():
        # run-as uses the debug app's own UID, including API 29 /proc restrictions.
        stat = e.shell("run-as", APP, "cat", "/proc/" + pid + "/stat")
        fields = stat.rsplit(")", 1)[1].split()
        return int(fields[11]) + int(fields[12])  # utime/stime, kernel fields 14/15

    initial_ticks = process_ticks()
    idle_started = time.monotonic()
    time.sleep(3)
    idle_ticks = process_ticks() - initial_ticks
    idle_seconds = time.monotonic() - idle_started
    idle_cpu = 100 * idle_ticks / int(ticks_per_second) / idle_seconds
    memory = e.shell("dumpsys", "meminfo", APP)
    pss = re.search(r"TOTAL PSS:\s*(\d+)", memory)
    if pss is None:
        pss = re.search(r"^\s*TOTAL\s+(\d+)", memory, re.MULTILINE)
    cpu = e.shell("dumpsys", "cpuinfo")
    cpu_lines = [line.strip() for line in cpu.splitlines() if re.search(r"/" + re.escape(APP) + r":", line)]
    e.shell("dumpsys", "gfxinfo", APP, "reset")
    for _ in range(20):
        e.start(FIXTURE)
        e.wait_foreground(FIXTURE_PACKAGE)
        e.shell("input", "keyevent", "KEYCODE_HOME")
        e.wait_foreground(APP)
    e.wait_node(resource="grid")
    frames = e.shell("dumpsys", "gfxinfo", APP, "framestats")
    after_memory = e.shell("dumpsys", "meminfo", APP)
    after_pss = re.search(r"TOTAL PSS:\s*(\d+)", after_memory) or re.search(r"^\s*TOTAL\s+(\d+)", after_memory, re.MULTILINE)
    require(after_pss is not None, "Process memory after 20 switches unavailable")
    crash_observation = e.check_run_crashes()
    aggregate = {}
    for key in ("Total frames rendered", "Janky frames", "50th percentile", "90th percentile", "95th percentile", "99th percentile"):
        found = re.search(r"^\s*" + re.escape(key) + r":\s*(.+)", frames, re.MULTILINE)
        if found:
            aggregate[key] = found[1].strip()
    require(bool(aggregate), "Frame timing summary unavailable")
    require(pss is not None, "Process memory summary unavailable")
    return {"cold_start_ms": cold, "cold_total_median_ms": statistics.median(x["TotalTime"] for x in cold),
            "cold_start_method": "Fixture foreground; force-stop Home; require no Home PID; am start -W; require new PID",
            "idle_total_pss_kb": int(pss[1]), "idle_cpu_percent_of_one_core": round(idle_cpu, 3),
            "after_20_switches_total_pss_kb": int(after_pss[1]),
            "crash_observation": crash_observation,
            "idle_cpu_sample_seconds": round(idle_seconds, 3), "idle_cpu_rolling_snapshot": cpu_lines,
            "idle_cpu_method": "App UID /proc/pid/stat utime+stime delta, getconf CLK_TCK, host monotonic interval",
            "switch_cycles": 20, "frame_summary": aggregate,
            "measurement_limit": "Emulator observations, not target vehicle performance"}


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--fixture-apk", type=Path, help="Optional known app-debug-androidTest.apk for package replacement")
    parser.add_argument("--wallpaper", type=Path, help="Optional known PNG for the real system document-picker journey")
    parser.add_argument("--out", type=Path, help="Screenshot directory outside repository; default a system TEMP directory")
    args = parser.parse_args()
    repo = Path(__file__).resolve().parent.parent
    if args.out:
        destination = args.out.resolve()
        if destination == repo or repo in destination.parents:
            parser.error("--out must be outside the repository")
    else:
        destination = None
    for source in (args.fixture_apk, args.wallpaper):
        if source and not source.is_file():
            parser.error("Input does not exist: " + str(source))
    if args.wallpaper:
        with args.wallpaper.open("rb") as image_input:
            if image_input.read(8) != b"\x89PNG\r\n\x1a\n":
                parser.error("--wallpaper must be a PNG")
    emulator = Emulator(args.adb, args.serial)
    report = {"journey": "TN Launcher API 29 emulator E2E", "status": "FAIL", "checks": [],
              "screenshots": [], "automotive_verified": False,
              "intended_emulator_changes": "Select TN Home; pin fixture; create/remove test folder; select Dark; optionally import wallpaper"}
    steps = [("search_empty_and_back_home", lambda: search_and_back(emulator)),
             ("pin_fixture", lambda: pin_fixture(emulator)),
             ("theme_and_favorite_persist_after_force_stop", lambda: dark_persistence(emulator)),
             ("real_app_launch_foreground_recents_home", lambda: app_launch_and_foreground(emulator)),
             ("folder_create_real_member", lambda: folder_create(emulator)),
             ("background_process_death_restore", lambda: process_death(emulator)),
             ("fixture_package_remove_reinstall_update", lambda: replace_fixture(emulator, args.fixture_apk) if args.fixture_apk else None),
             ("wallpaper_system_document_picker", lambda: wallpaper_picker(emulator, args.wallpaper) if args.wallpaper else None),
             ("startup_idle_and_switch_performance", lambda: performance(emulator))]
    started = time.monotonic()
    guarded = False
    try:
        report["environment"] = emulator.guard()
        guarded = True
        if args.fixture_apk:
            package = apk_package(args.fixture_apk)
            require(package == FIXTURE_PACKAGE, "Replacement APK package is %r; expected %s" % (package, FIXTURE_PACKAGE))
            report["fixture_apk_package"] = package
        device_epoch = emulator.shell("date", "+%s")
        require(device_epoch.isdigit(), "Cannot establish emulator clock for crash observation")
        emulator.crash_started_epoch = int(device_epoch)
        emulator.shell("cmd", "package", "set-home-activity", HOME)
        report["home_selection_requested"] = True
        emulator.start()
        destination = destination or Path(tempfile.mkdtemp(prefix="tn-launcher-e2e-"))
        destination.mkdir(parents=True, exist_ok=True)
        for index, (name, action) in enumerate(steps):
            check_started = time.monotonic()
            try:
                optional_missing = (name.startswith("fixture_package") and not args.fixture_apk) or (
                    name.startswith("wallpaper_system") and not args.wallpaper)
                if optional_missing:
                    report["checks"].append({"name": name, "status": "NOT_RUN", "reason": "Optional input not supplied"})
                    continue
                detail = action()
                report["checks"].append({"name": name, "status": "PASS", "detail": detail,
                                         "elapsed_seconds": round(time.monotonic() - check_started, 2)})
                if name in ("theme_and_favorite_persist_after_force_stop", "startup_idle_and_switch_performance"):
                    screenshot = destination / (name + ".png")
                    screenshot.write_bytes(emulator.command("exec-out", "screencap", "-p", binary=True))
                    report["screenshots"].append(str(screenshot))
            except Exception as error:
                report["checks"].append({"name": name, "status": "FAIL", "reason": str(error)[:1800]})
                report["checks"].extend({"name": pending, "status": "NOT_RUN", "reason": "Previous journey failed"}
                                         for pending, _ in steps[index + 1:])
                raise
        report["status"] = "PASS" if all(check["status"] == "PASS" for check in report["checks"]) else "PARTIAL"
    except Exception as error:
        report["error"] = str(error)[:1800]
    finally:
        if guarded:
            try:
                emulator.shell("rm", "-f", emulator.dump_path, check=False)
            except (OSError, subprocess.TimeoutExpired):
                report["cleanup_note"] = "Could not remove the task-owned UI dump from emulator"
        report["elapsed_seconds"] = round(time.monotonic() - started, 2)
        print(json.dumps(report, ensure_ascii=False, indent=2))
    return 1 if report["status"] == "FAIL" else 0


if __name__ == "__main__":
    sys.exit(main())
