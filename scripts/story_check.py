#!/usr/bin/env python3
"""Story validator for UNALLOCATED story format v1."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import json
import math
from pathlib import Path
import re
import sys
from typing import Any, Optional, Union

RE_CHAPTER_ID = re.compile(r"^[a-z][a-z0-9_]{0,23}$")
RE_BEAT_NAME = re.compile(r"^[a-z0-9_]{1,32}$")
RE_FLAG_COUNTER_ID = re.compile(r"^[a-z][a-z0-9_.]{0,47}$")
RE_ENDING_ID = re.compile(r"^[A-Z][A-Z0-9_]{0,31}$")
RE_CAPTURE_ID = re.compile(r"^[a-z0-9][a-z0-9-]{0,39}$")
RE_RESOURCE_ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
RE_COLOR = re.compile(r"^#[0-9a-fA-F]{6}$")

FORBIDDEN_COMMAND_ROOTS = {
    "stop", "op", "deop", "kick", "ban", "ban-ip", "pardon", "pardon-ip",
    "whitelist", "save-all", "save-off", "save-on", "reload", "datapack",
    "publish", "debug", "perf", "jfr", "transfer", "tick"
}

INTERFACE_EFFECT_MAX_SECONDS = {
    "F3": 3,
    "DEATH": 30,
    "SPLASH": 30,
    "LOADING": 30,
    "ADVANCEMENT": 5,
    "SENSITIVITY": 5,
    "FOV": 5,
    "GAMMA": 5,
    "SIGN": 5,
}

VALID_PLACEHOLDERS = {
    "player", "agreement", "local_time", "world", "elapsed_min", "deaths"
}


def is_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate JSON key '{key}'")
        result[key] = value
    return result


def strict_json(content):
    return json.loads(content, object_pairs_hook=unique_object,
                      parse_constant=lambda value: (_ for _ in ()).throw(ValueError('Non-finite JSON number: ' + value)))


@dataclass
class Finding:
    file: str
    pointer: str
    level: str  # "error" | "warning"
    rule: str
    message: str


class Validator:
    def __init__(
        self,
        story_dir: Path,
        anchors_path: Path,
        features_path: Path,
        functions_dir: Path,
    ) -> None:
        self.story_dir = story_dir
        self.anchors_path = anchors_path
        self.features_path = features_path
        self.functions_dir = functions_dir

        self.findings: list[Finding] = []

        # Map data
        self.map_checkpoints: set[str] = set()
        self.map_landmarks: set[str] = set()
        self.map_triggers: set[str] = set()
        self.map_sections: set[str] = set()

        # Features
        self.effects_options = False
        self.effects_screamer = False

        # Story data
        self.languages: list[str] = []
        self.story_anchors: set[str] = set()
        self.story_zones: set[str] = set()
        self.story_endings: set[str] = set()
        self.story_fallback_ending: Optional[str] = None
        self.has_agreement_fallback = False

        # Cross-file tracking
        self.all_beat_ids: set[str] = set()
        self.all_beat_id_sources: dict[str, tuple[str, str]] = {}
        self.all_flags_set: set[str] = set()
        self.all_flags_tested: list[tuple[str, str, str]] = []  # (flag, file, pointer)
        self.all_used_endings: set[str] = set()
        self.all_capture_names: dict[str, tuple[str, str]] = {}  # name -> (file, pointer)
        self.all_beat_refs: list[tuple[str, str, str]] = []  # (beat_ref, file, pointer)
        self.total_budget_seconds = 0
        self.screamer_actions: list[tuple[str, str]] = []  # (file, pointer)
        self.used_agreement = False

    def add_finding(self, file: str, pointer: str, level: str, rule: str, message: str) -> None:
        self.findings.append(Finding(file=file, pointer=pointer, level=level, rule=rule, message=message))

    def load_anchors(self) -> None:
        if not self.anchors_path.is_file():
            return
        try:
            data = json.loads(self.anchors_path.read_text(encoding="utf-8"))
            self.map_checkpoints = set(data.get("checkpoints", []))
            self.map_landmarks = set(data.get("landmarks", []))
            self.map_triggers = set(data.get("triggers", []))
            self.map_sections = set(data.get("sections", []))
        except Exception:
            pass

    def load_features(self) -> None:
        if not self.features_path.is_file():
            return
        try:
            data = json.loads(self.features_path.read_text(encoding="utf-8"))
            effects = data.get("effects", {})
            self.effects_options = bool(effects.get("options", False))
            self.effects_screamer = bool(effects.get("screamer", False))
        except Exception:
            pass

    def validate_text(
        self,
        text_obj: Any,
        file: str,
        pointer: str,
        is_title: bool = False,
        is_chat_or_farewell: bool = False,
    ) -> None:
        if not isinstance(text_obj, dict):
            self.add_finding(file, pointer, "error", "bad-value", "Text must be a JSON object")
            return

        allowed_keys = set(self.languages) | {"color", "bold", "italic", "obfuscated"}
        for k in text_obj:
            if k not in allowed_keys:
                self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in text")

        for lang in self.languages:
            if lang not in text_obj:
                self.add_finding(file, f"{pointer}/{lang}", "error", "missing-language", f"Missing language '{lang}' in text")
            else:
                val = text_obj[lang]
                if not isinstance(val, str):
                    self.add_finding(file, f"{pointer}/{lang}", "error", "bad-value", f"Text for '{lang}' must be a string")
                else:
                    if not is_chat_or_farewell and "\n" in val:
                        self.add_finding(file, f"{pointer}/{lang}", "error", "bad-value", "\\n is allowed in chat and farewell lines only")
                    self.validate_placeholders(val, file, f"{pointer}/{lang}")
                    if is_title and len(val) > 40:
                        self.add_finding(file, f"{pointer}/{lang}", "warning", "long-title", f"Title text in '{lang}' exceeds 40 characters ({len(val)})")

        if "color" in text_obj:
            c = text_obj["color"]
            if not isinstance(c, str) or not RE_COLOR.match(c):
                self.add_finding(file, f"{pointer}/color", "error", "bad-value", f"Invalid color '{c}', expected #rrggbb")
        for bool_key in ("bold", "italic", "obfuscated"):
            if bool_key in text_obj and not isinstance(text_obj[bool_key], bool):
                self.add_finding(file, f"{pointer}/{bool_key}", "error", "bad-value", f"Key '{bool_key}' must be boolean")

    def validate_placeholders(self, text: str, file: str, pointer: str) -> None:
        # Check braces and placeholders
        idx = 0
        n = len(text)
        has_error = False
        while idx < n:
            if text[idx] == "}":
                if not has_error:
                    self.add_finding(file, pointer, "error", "unknown-placeholder", "Stray '}' in text")
                    has_error = True
                idx += 1
            elif text[idx] == "{":
                end = text.find("}", idx + 1)
                if end == -1:
                    if not has_error:
                        self.add_finding(file, pointer, "error", "unknown-placeholder", "Unclosed '{' in text")
                        has_error = True
                    break
                inner = text[idx + 1:end]
                if "{" in inner:
                    if not has_error:
                        self.add_finding(file, pointer, "error", "unknown-placeholder", "Nested or stray '{' in text")
                        has_error = True
                else:
                    if inner == "agreement":
                        self.used_agreement = True
                    elif inner in VALID_PLACEHOLDERS:
                        pass
                    elif inner.startswith("counter:"):
                        cid = inner[len("counter:"):]
                        if not RE_FLAG_COUNTER_ID.match(cid):
                            if not has_error:
                                self.add_finding(file, pointer, "error", "unknown-placeholder", f"Invalid counter id in placeholder '{{{inner}}}'")
                                has_error = True
                    else:
                        if not has_error:
                            self.add_finding(file, pointer, "error", "unknown-placeholder", f"Unknown placeholder '{{{inner}}}'")
                            has_error = True
                idx = end + 1
            else:
                idx += 1

    def validate_position(self, pos: Any, file: str, pointer: str) -> None:
        if isinstance(pos, str):
            if (
                pos not in self.map_checkpoints
                and pos not in self.map_landmarks
                and pos not in self.story_anchors
            ):
                self.add_finding(file, pointer, "error", "unknown-anchor", f"Unknown anchor or checkpoint '{pos}'")
        elif isinstance(pos, dict):
            if "anchor" in pos:
                allowed = {"anchor", "offset"}
                for k in pos:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in position")
                anc = pos["anchor"]
                if not isinstance(anc, str) or (
                    anc not in self.map_checkpoints
                    and anc not in self.map_landmarks
                    and anc not in self.story_anchors
                ):
                    self.add_finding(file, f"{pointer}/anchor", "error", "unknown-anchor", f"Unknown anchor '{anc}'")
                offset = pos.get("offset")
                if not isinstance(offset, list) or len(offset) != 3 or not all(is_number(x) for x in offset):
                    self.add_finding(file, f"{pointer}/offset", "error", "bad-value", "Position offset must be [dx, dy, dz]")
            elif "pos" in pos:
                allowed = {"pos", "dimension", "yaw", "pitch"}
                for k in pos:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in position")
                p = pos["pos"]
                if not isinstance(p, list) or len(p) != 3 or not all(is_number(x) for x in p):
                    self.add_finding(file, f"{pointer}/pos", "error", "bad-value", "Position pos must be [x, y, z]")
                if "dimension" in pos and pos["dimension"] not in ("overworld", "pocket"):
                    self.add_finding(file, f"{pointer}/dimension", "error", "bad-value", "Dimension must be 'overworld' or 'pocket'")
                if "yaw" in pos and not isinstance(pos["yaw"], (int, float)):
                    self.add_finding(file, f"{pointer}/yaw", "error", "bad-value", "Yaw must be a number")
                if "pitch" in pos and not isinstance(pos["pitch"], (int, float)):
                    self.add_finding(file, f"{pointer}/pitch", "error", "bad-value", "Pitch must be a number")
            else:
                self.add_finding(file, pointer, "error", "bad-value", "Position object must have 'anchor' or 'pos'")
        else:
            self.add_finding(file, pointer, "error", "bad-value", "Position must be a name string or position object")

    def validate_zone(self, zone: Any, file: str, pointer: str) -> None:
        if isinstance(zone, str):
            if zone not in self.map_triggers and zone not in self.story_zones:
                self.add_finding(file, pointer, "error", "unknown-zone", f"Unknown zone '{zone}'")
        elif isinstance(zone, dict):
            if "box" in zone:
                allowed = {"box", "dimension"}
                for k in zone:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in zone")
                box = zone["box"]
                if not isinstance(box, list) or len(box) != 6 or not all(is_number(x) for x in box):
                    self.add_finding(file, f"{pointer}/box", "error", "bad-value", "Zone box must be [x1, y1, z1, x2, y2, z2]")
                if "dimension" in zone and zone["dimension"] not in ("overworld", "pocket"):
                    self.add_finding(file, f"{pointer}/dimension", "error", "bad-value", "Dimension must be 'overworld' or 'pocket'")
            elif "section" in zone:
                allowed = {"section"}
                for k in zone:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in zone")
                sec = zone["section"]
                if not isinstance(sec, str) or sec not in self.map_sections:
                    self.add_finding(file, f"{pointer}/section", "error", "unknown-zone", f"Unknown section '{sec}'")
            else:
                self.add_finding(file, pointer, "error", "bad-value", "Zone object must have 'box' or 'section'")
        else:
            self.add_finding(file, pointer, "error", "bad-value", "Zone must be a name string or zone object")

    def validate_command(self, cmd: Any, file: str, pointer: str) -> None:
        if not isinstance(cmd, str):
            self.add_finding(file, pointer, "error", "bad-value", "Command must be a string")
            return
        if not cmd or not cmd.strip():
            self.add_finding(file, pointer, "error", "bad-command", "Command cannot be empty")
            return
        if cmd.startswith("/"):
            self.add_finding(file, pointer, "error", "bad-command", "Command cannot have leading '/'")
            return
        parts = cmd.strip().split()
        if parts and parts[0] in FORBIDDEN_COMMAND_ROOTS:
            self.add_finding(file, pointer, "error", "bad-command", f"Forbidden command root '{parts[0]}'")

    def validate_trigger(self, trigger: Any, file: str, pointer: str, beat_autoplay_input: Optional[str] = None) -> None:
        if not isinstance(trigger, dict):
            self.add_finding(file, pointer, "error", "bad-value", "Trigger must be an object")
            return

        if "type" not in trigger:
            self.add_finding(file, f"{pointer}/type", "error", "bad-value", "Missing required key 'type' in trigger")
            return

        ttype = trigger["type"]
        valid_types = {
            "chapter_start", "after", "flag", "zone", "look", "interact",
            "use_item", "has_item", "break", "chat", "still", "sprint",
            "sneak", "death", "respawn", "bait", "creature", "any"
        }
        if ttype not in valid_types:
            self.add_finding(file, f"{pointer}/type", "error", "unknown-trigger", f"Unknown trigger type '{ttype}'")
            return

        if ttype == "chapter_start":
            allowed = {"type"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")

        elif ttype == "after":
            allowed = {"type", "seconds", "beat"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "seconds" not in trigger:
                self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Missing 'seconds' in after trigger")
            else:
                s = trigger["seconds"]
                if not is_number(s):
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Trigger seconds must be a number")
                elif s < 0:
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-time", "Trigger seconds cannot be negative")
            if "beat" in trigger:
                b = trigger["beat"]
                if not isinstance(b, str):
                    self.add_finding(file, f"{pointer}/beat", "error", "bad-value", "Trigger beat must be a string")
                else:
                    self.all_beat_refs.append((b, file, f"{pointer}/beat"))

        elif ttype == "flag":
            allowed = {"type", "flag"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "flag" not in trigger:
                self.add_finding(file, f"{pointer}/flag", "error", "bad-value", "Missing 'flag' in flag trigger")
            else:
                fl = trigger["flag"]
                if not isinstance(fl, str) or not RE_FLAG_COUNTER_ID.match(fl):
                    self.add_finding(file, f"{pointer}/flag", "error", "bad-id", f"Invalid flag id '{fl}'")
                else:
                    self.all_flags_tested.append((fl, file, f"{pointer}/flag"))

        elif ttype == "zone":
            allowed = {"type", "zone", "seconds"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "zone" not in trigger:
                self.add_finding(file, f"{pointer}/zone", "error", "bad-value", "Missing 'zone' in zone trigger")
            else:
                self.validate_zone(trigger["zone"], file, f"{pointer}/zone")
            if "seconds" in trigger:
                s = trigger["seconds"]
                if not is_number(s):
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Seconds must be a number")
                elif s < 0:
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-time", "Seconds cannot be negative")

        elif ttype == "look":
            allowed = {"type", "at", "degrees", "seconds", "max_distance", "spyglass"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "at" not in trigger:
                self.add_finding(file, f"{pointer}/at", "error", "bad-value", "Missing 'at' in look trigger")
            else:
                self.validate_position(trigger["at"], file, f"{pointer}/at")
            if "degrees" in trigger and not isinstance(trigger["degrees"], (int, float)):
                self.add_finding(file, f"{pointer}/degrees", "error", "bad-value", "Degrees must be a number")
            if "seconds" in trigger:
                s = trigger["seconds"]
                if not is_number(s):
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Seconds must be a number")
                elif s < 0:
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-time", "Seconds cannot be negative")
            if "max_distance" in trigger and not isinstance(trigger["max_distance"], (int, float)):
                self.add_finding(file, f"{pointer}/max_distance", "error", "bad-value", "max_distance must be a number")
            if "spyglass" in trigger and not isinstance(trigger["spyglass"], bool):
                self.add_finding(file, f"{pointer}/spyglass", "error", "bad-value", "Spyglass must be boolean")

        elif ttype == "interact":
            allowed = {"type", "at"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "at" not in trigger:
                self.add_finding(file, f"{pointer}/at", "error", "bad-value", "Missing 'at' in interact trigger")
            else:
                self.validate_position(trigger["at"], file, f"{pointer}/at")

        elif ttype == "use_item":
            allowed = {"type", "item"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "item" not in trigger:
                self.add_finding(file, f"{pointer}/item", "error", "bad-value", "Missing 'item' in use_item trigger")
            else:
                it = trigger["item"]
                if not isinstance(it, str) or not RE_RESOURCE_ID.match(it):
                    self.add_finding(file, f"{pointer}/item", "error", "bad-value", f"Invalid item id '{it}'")

        elif ttype == "has_item":
            allowed = {"type", "item", "count"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "item" not in trigger:
                self.add_finding(file, f"{pointer}/item", "error", "bad-value", "Missing 'item' in has_item trigger")
            else:
                it = trigger["item"]
                if not isinstance(it, str) or not RE_RESOURCE_ID.match(it):
                    self.add_finding(file, f"{pointer}/item", "error", "bad-value", f"Invalid item id '{it}'")
            if "count" in trigger:
                c = trigger["count"]
                if not isinstance(c, int) or c < 1:
                    self.add_finding(file, f"{pointer}/count", "error", "bad-value", "Count must be integer >= 1")

        elif ttype == "break":
            allowed = {"type", "block", "zone"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "block" not in trigger:
                self.add_finding(file, f"{pointer}/block", "error", "bad-value", "Missing 'block' in break trigger")
            else:
                bl = trigger["block"]
                if not isinstance(bl, str) or not RE_RESOURCE_ID.match(bl):
                    self.add_finding(file, f"{pointer}/block", "error", "bad-value", f"Invalid block id '{bl}'")
            if "zone" in trigger:
                self.validate_zone(trigger["zone"], file, f"{pointer}/zone")

        elif ttype == "chat":
            allowed = {"type", "pattern", "hide"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "pattern" not in trigger:
                self.add_finding(file, f"{pointer}/pattern", "error", "bad-value", "Missing 'pattern' in chat trigger")
            else:
                pat = trigger["pattern"]
                if not isinstance(pat, str):
                    self.add_finding(file, f"{pointer}/pattern", "error", "bad-value", "Chat pattern must be a string")
                else:
                    try:
                        compiled = re.compile(pat)
                    except re.error as e:
                        self.add_finding(file, f"{pointer}/pattern", "error", "chat-without-input", f"Invalid regex pattern '{pat}': {e}")
                        compiled = None

                    if compiled is not None:
                        if beat_autoplay_input is None:
                            self.add_finding(file, pointer, "error", "chat-without-input", "Beat with chat trigger lacks autoplay.input")
                        elif not compiled.search(beat_autoplay_input):
                            self.add_finding(file, f"{pointer}/pattern", "error", "chat-without-input", f"Chat input '{beat_autoplay_input}' does not match pattern '{pat}'")

            if "hide" in trigger and not isinstance(trigger["hide"], bool):
                self.add_finding(file, f"{pointer}/hide", "error", "bad-value", "Hide must be boolean")

        elif ttype in ("still", "sprint", "sneak"):
            allowed = {"type", "seconds"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "seconds" not in trigger:
                self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", f"Missing 'seconds' in {ttype} trigger")
            else:
                s = trigger["seconds"]
                if not is_number(s):
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Seconds must be a number")
                elif s < 0:
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-time", "Seconds cannot be negative")

        elif ttype in ("death", "respawn"):
            allowed = {"type"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")

        elif ttype == "bait":
            allowed = {"type", "event"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "event" not in trigger:
                self.add_finding(file, f"{pointer}/event", "error", "bad-value", "Missing 'event' in bait trigger")
            else:
                ev = trigger["event"]
                if ev not in ("step", "screamer", "gone"):
                    self.add_finding(file, f"{pointer}/event", "error", "bad-value", f"Invalid bait event '{ev}'")

        elif ttype == "creature":
            allowed = {"type", "creature", "event", "distance", "seconds"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "creature" not in trigger:
                self.add_finding(file, f"{pointer}/creature", "error", "bad-value", "Missing 'creature' in creature trigger")
            if "event" not in trigger:
                self.add_finding(file, f"{pointer}/event", "error", "bad-value", "Missing 'event' in creature trigger")
            else:
                ev = trigger["event"]
                if ev not in ("near", "seen", "caught"):
                    self.add_finding(file, f"{pointer}/event", "error", "bad-value", f"Invalid creature event '{ev}'")
            if "distance" in trigger and not isinstance(trigger["distance"], (int, float)):
                self.add_finding(file, f"{pointer}/distance", "error", "bad-value", "Distance must be a number")
            if "seconds" in trigger:
                s = trigger["seconds"]
                if not is_number(s):
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Seconds must be a number")
                elif s < 0:
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-time", "Seconds cannot be negative")

        elif ttype == "any":
            allowed = {"type", "of"}
            for k in trigger:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in trigger")
            if "of" not in trigger or not isinstance(trigger["of"], list) or len(trigger["of"]) < 1:
                self.add_finding(file, f"{pointer}/of", "error", "bad-value", "Trigger 'any' requires non-empty list 'of'")
            else:
                for idx, sub_trig in enumerate(trigger["of"]):
                    self.validate_trigger(sub_trig, file, f"{pointer}/of/{idx}", beat_autoplay_input)

    def validate_action(self, action: Any, file: str, pointer: str) -> None:
        if not isinstance(action, dict):
            self.add_finding(file, pointer, "error", "bad-value", "Action must be an object")
            return

        if "do" not in action:
            self.add_finding(file, f"{pointer}/do", "error", "bad-value", "Missing required key 'do' in action")
            return

        do = action["do"]
        valid_actions = {
            "flag", "counter", "next_chapter", "end", "cancel", "title",
            "actionbar", "chat", "blackout", "interface", "audio",
            "teleport", "bait", "command", "function",
            "copy", "corrupt", "creature"
        }
        if do not in valid_actions:
            self.add_finding(file, f"{pointer}/do", "error", "unknown-action", f"Unknown action '{do}'")
            return

        # Check 'at' time
        if "at" in action:
            at_val = action["at"]
            if not is_number(at_val):
                self.add_finding(file, f"{pointer}/at", "error", "bad-value", "Action 'at' must be a number")
            elif at_val < 0 or at_val > 600:
                self.add_finding(file, f"{pointer}/at", "error", "bad-time", f"Action 'at' ({at_val}) must be between 0 and 600")

        if do == "flag":
            allowed = {"do", "at", "set", "clear"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in flag action")
            has_set = "set" in action
            has_clear = "clear" in action
            if (has_set and has_clear) or (not has_set and not has_clear):
                self.add_finding(file, pointer, "error", "bad-value", "Flag action requires exactly one of 'set' or 'clear'")
            if has_set:
                fl = action["set"]
                if not isinstance(fl, str) or not RE_FLAG_COUNTER_ID.match(fl):
                    self.add_finding(file, f"{pointer}/set", "error", "bad-id", f"Invalid flag id '{fl}'")
                else:
                    self.all_flags_set.add(fl)
            if has_clear:
                fl = action["clear"]
                if not isinstance(fl, str) or not RE_FLAG_COUNTER_ID.match(fl):
                    self.add_finding(file, f"{pointer}/clear", "error", "bad-id", f"Invalid flag id '{fl}'")

        elif do == "counter":
            allowed = {"do", "at", "id", "add", "set"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in counter action")
            if "id" not in action:
                self.add_finding(file, f"{pointer}/id", "error", "bad-value", "Missing 'id' in counter action")
            else:
                cid = action["id"]
                if not isinstance(cid, str) or not RE_FLAG_COUNTER_ID.match(cid):
                    self.add_finding(file, f"{pointer}/id", "error", "bad-id", f"Invalid counter id '{cid}'")
            has_add = "add" in action
            has_set = "set" in action
            if (has_add and has_set) or (not has_add and not has_set):
                self.add_finding(file, pointer, "error", "bad-value", "Counter action requires exactly one of 'add' or 'set'")
            if has_add and not (isinstance(action["add"], int) and not isinstance(action["add"], bool)):
                self.add_finding(file, f"{pointer}/add", "error", "bad-value", "Counter 'add' must be an integer")
            if has_set and not (isinstance(action["set"], int) and not isinstance(action["set"], bool)):
                self.add_finding(file, f"{pointer}/set", "error", "bad-value", "Counter 'set' must be an integer")

        elif do == "next_chapter":
            allowed = {"do", "at"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in next_chapter action")

        elif do == "end":
            allowed = {"do", "at", "ending"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in end action")
            if "ending" not in action:
                self.add_finding(file, f"{pointer}/ending", "error", "bad-value", "Missing 'ending' in end action")
            else:
                eid = action["ending"]
                if not isinstance(eid, str) or not RE_ENDING_ID.match(eid):
                    self.add_finding(file, f"{pointer}/ending", "error", "bad-id", f"Invalid ending id '{eid}'")
                elif eid not in self.story_endings:
                    self.add_finding(file, f"{pointer}/ending", "error", "unknown-ending", f"Unknown ending '{eid}'")
                else:
                    self.all_used_endings.add(eid)

        elif do == "cancel":
            allowed = {"do", "at", "beat"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in cancel action")
            if "beat" not in action:
                self.add_finding(file, f"{pointer}/beat", "error", "bad-value", "Missing 'beat' in cancel action")
            else:
                b = action["beat"]
                if not isinstance(b, str):
                    self.add_finding(file, f"{pointer}/beat", "error", "bad-value", "Cancel beat must be a string")
                else:
                    self.all_beat_refs.append((b, file, f"{pointer}/beat"))

        elif do == "title":
            allowed = {"do", "at", "title", "subtitle", "fade_in", "stay", "fade_out"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in title action")
            if "title" not in action:
                self.add_finding(file, f"{pointer}/title", "error", "bad-value", "Missing 'title' in title action")
            else:
                self.validate_text(action["title"], file, f"{pointer}/title", is_title=True)
            if "subtitle" in action:
                self.validate_text(action["subtitle"], file, f"{pointer}/subtitle")
            for t_k in ("fade_in", "stay", "fade_out"):
                if t_k in action:
                    v = action[t_k]
                    if not is_number(v):
                        self.add_finding(file, f"{pointer}/{t_k}", "error", "bad-value", f"{t_k} must be a number")
                    elif v < 0:
                        self.add_finding(file, f"{pointer}/{t_k}", "error", "bad-time", f"{t_k} cannot be negative")

        elif do == "actionbar":
            allowed = {"do", "at", "text"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in actionbar action")
            if "text" not in action:
                self.add_finding(file, f"{pointer}/text", "error", "bad-value", "Missing 'text' in actionbar action")
            else:
                self.validate_text(action["text"], file, f"{pointer}/text")

        elif do == "chat":
            allowed = {"do", "at", "text", "from"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in chat action")
            if "text" not in action:
                self.add_finding(file, f"{pointer}/text", "error", "bad-value", "Missing 'text' in chat action")
            else:
                self.validate_text(action["text"], file, f"{pointer}/text", is_chat_or_farewell=True)
            if "from" in action:
                self.validate_text(action["from"], file, f"{pointer}/from", is_chat_or_farewell=True)

        elif do == "blackout":
            allowed = {"do", "at", "fade_in", "hold", "fade_out", "color"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in blackout action")
            for t_k in ("fade_in", "hold", "fade_out"):
                if t_k in action:
                    v = action[t_k]
                    if not is_number(v):
                        self.add_finding(file, f"{pointer}/{t_k}", "error", "bad-value", f"{t_k} must be a number")
                    elif v < 0:
                        self.add_finding(file, f"{pointer}/{t_k}", "error", "bad-time", f"{t_k} cannot be negative")
            if "color" in action:
                c = action["color"]
                if not isinstance(c, str) or not RE_COLOR.match(c):
                    self.add_finding(file, f"{pointer}/color", "error", "bad-value", f"Invalid color '{c}', expected #rrggbb")

        elif do == "interface":
            allowed = {"do", "at", "effect", "seconds", "generation", "strength", "leak", "seed", "sign", "advancement", "text"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in interface action")
            if "effect" not in action:
                self.add_finding(file, f"{pointer}/effect", "error", "bad-value", "Missing 'effect' in interface action")
                effect = None
            else:
                effect = action["effect"]
                if effect not in INTERFACE_EFFECT_MAX_SECONDS:
                    self.add_finding(file, f"{pointer}/effect", "error", "bad-value", f"Unknown interface effect '{effect}'")
                elif effect in ("SENSITIVITY", "FOV", "GAMMA") and not self.effects_options:
                    self.add_finding(file, f"{pointer}/effect", "error", "feature-off", f"Interface effect '{effect}' disabled by features (effects.options is false)")

            if "seconds" not in action:
                self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Missing 'seconds' in interface action")
            else:
                s = action["seconds"]
                if not is_number(s):
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Seconds must be a number")
                elif s < 0 or (effect in INTERFACE_EFFECT_MAX_SECONDS and s > INTERFACE_EFFECT_MAX_SECONDS[effect]):
                    max_s = INTERFACE_EFFECT_MAX_SECONDS.get(effect, "max")
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-time", f"Interface seconds ({s}) exceeds maximum ({max_s}) or is negative")

            if "generation" in action:
                g = action["generation"]
                if not (isinstance(g, int) and not isinstance(g, bool)) or g < 0 or g > 5:
                    self.add_finding(file, f"{pointer}/generation", "error", "bad-value", "Generation must be integer 0-5")
            if "strength" in action:
                st = action["strength"]
                if not is_number(st) or st < -1.0 or st > 1.0:
                    self.add_finding(file, f"{pointer}/strength", "error", "bad-value", "Strength must be number -1...1")
            if "leak" in action:
                lk = action["leak"]
                if lk not in ("es", "en", "mixed"):
                    self.add_finding(file, f"{pointer}/leak", "error", "bad-value", "Leak must be 'es', 'en', or 'mixed'")
            if "seed" in action:
                sd = action["seed"]
                if not (isinstance(sd, int) and not isinstance(sd, bool)):
                    self.add_finding(file, f"{pointer}/seed", "error", "bad-value", "Seed must be integer")

            if effect == "SIGN":
                if "sign" not in action:
                    self.add_finding(file, f"{pointer}/sign", "error", "bad-value", "SIGN interface effect requires 'sign' position")
                else:
                    self.validate_position(action["sign"], file, f"{pointer}/sign")
            else:
                if "sign" in action:
                    self.add_finding(file, f"{pointer}/sign", "error", "bad-value", "'sign' position allowed only for SIGN effect")

            if "advancement" in action:
                adv = action["advancement"]
                if not isinstance(adv, str) or not RE_RESOURCE_ID.match(adv):
                    self.add_finding(file, f"{pointer}/advancement", "error", "bad-value", f"Invalid advancement id '{adv}'")
            if "text" in action:
                self.validate_text(action["text"], file, f"{pointer}/text")

        elif do == "audio":
            allowed = {"do", "at", "cue", "where"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in audio action")
            if "cue" not in action:
                self.add_finding(file, f"{pointer}/cue", "error", "bad-value", "Missing 'cue' in audio action")
            else:
                cue = action["cue"]
                if cue not in ("ambient", "threads", "muffle", "screamer", "stop"):
                    self.add_finding(file, f"{pointer}/cue", "error", "bad-value", f"Invalid audio cue '{cue}'")
                elif cue == "screamer":
                    self.screamer_actions.append((file, f"{pointer}/cue"))

            if "where" in action:
                self.validate_position(action["where"], file, f"{pointer}/where")

        elif do == "post_effect":
            allowed = {"do", "at", "id", "seconds"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in post_effect action")
            if "id" not in action:
                self.add_finding(file, f"{pointer}/id", "error", "bad-value", "Missing 'id' in post_effect action")
            else:
                pid = action["id"]
                if not isinstance(pid, str) or not RE_RESOURCE_ID.match(pid):
                    self.add_finding(file, f"{pointer}/id", "error", "bad-value", f"Invalid post_effect id '{pid}'")
            if "seconds" not in action:
                self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Missing 'seconds' in post_effect action")
            else:
                s = action["seconds"]
                if not is_number(s):
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-value", "Seconds must be a number")
                elif s < 0:
                    self.add_finding(file, f"{pointer}/seconds", "error", "bad-time", "Seconds cannot be negative")

        elif do == "teleport":
            allowed = {"do", "at", "to", "blackout"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in teleport action")
            if "to" not in action:
                self.add_finding(file, f"{pointer}/to", "error", "bad-value", "Missing 'to' in teleport action")
            else:
                self.validate_position(action["to"], file, f"{pointer}/to")
            if "blackout" in action and not isinstance(action["blackout"], bool):
                self.add_finding(file, f"{pointer}/blackout", "error", "bad-value", "Blackout must be boolean")

        elif do == "bait":
            if "op" not in action:
                self.add_finding(file, f"{pointer}/op", "error", "bad-value", "Missing 'op' in bait action")
                return
            op = action["op"]
            if op not in ("place", "step", "scare", "clear"):
                self.add_finding(file, f"{pointer}/op", "error", "bad-value", f"Invalid bait op '{op}'")
                return

            if op == "place":
                allowed = {"do", "at", "op", "lure", "distances", "scare", "appear_delay", "fallback_after", "max_lifetime", "seed"}
                for k in action:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in bait place action")
                if "lure" in action:
                    self.validate_position(action["lure"], file, f"{pointer}/lure")
                if "distances" in action:
                    dist = action["distances"]
                    if not isinstance(dist, list) or not all(is_number(x) and x > 0 for x in dist):
                        self.add_finding(file, f"{pointer}/distances", "error", "bad-value", "Distances must be list of positive numbers")
                    else:
                        # strictly decreasing
                        if not all(dist[i] > dist[i + 1] for i in range(len(dist) - 1)):
                            self.add_finding(file, f"{pointer}/distances", "error", "bad-value", "Distances must be strictly decreasing")
                if "scare" in action:
                    sc = action["scare"]
                    if not isinstance(sc, bool):
                        self.add_finding(file, f"{pointer}/scare", "error", "bad-value", "Scare must be boolean")
                    elif sc and not self.effects_screamer:
                        self.add_finding(file, f"{pointer}/scare", "error", "feature-off", "Bait scare disabled by features (effects.screamer is false)")
                for t_k in ("appear_delay", "fallback_after", "max_lifetime"):
                    if t_k in action:
                        v = action[t_k]
                        if not is_number(v):
                            self.add_finding(file, f"{pointer}/{t_k}", "error", "bad-value", f"{t_k} must be a number")
                        elif v < 0:
                            self.add_finding(file, f"{pointer}/{t_k}", "error", "bad-time", f"{t_k} cannot be negative")
                if "seed" in action:
                    sd = action["seed"]
                    if not (isinstance(sd, int) and not isinstance(sd, bool)):
                        self.add_finding(file, f"{pointer}/seed", "error", "bad-value", "Seed must be integer")
            else:
                if op == "scare" and not self.effects_screamer:
                    self.add_finding(file, f"{pointer}/scare", "error", "feature-off", "Bait scare disabled by features")
                allowed = {"do", "at", "op"}
                for k in action:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in bait {op} action")

        elif do == "command":
            allowed = {"do", "at", "run"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in command action")
            if "run" not in action:
                self.add_finding(file, f"{pointer}/run", "error", "bad-value", "Missing 'run' in command action")
            else:
                self.validate_command(action["run"], file, f"{pointer}/run")

        elif do == "function":
            allowed = {"do", "at", "id"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in function action")
            if "id" not in action:
                self.add_finding(file, f"{pointer}/id", "error", "bad-value", "Missing 'id' in function action")
            else:
                fid = action["id"]
                if not isinstance(fid, str) or not RE_RESOURCE_ID.match(fid):
                    self.add_finding(file, f"{pointer}/id", "error", "bad-value", f"Invalid function id '{fid}'")
                elif not fid.startswith("unallocated:"):
                    self.add_finding(file, f"{pointer}/id", "error", "unknown-function", f"Function namespace must be 'unallocated:', got '{fid}'")
                else:
                    path_part = fid[len("unallocated:"):]
                    func_file = self.functions_dir / f"{path_part}.mcfunction"
                    if not func_file.is_file():
                        self.add_finding(file, f"{pointer}/id", "error", "unknown-function", f"Function file not found: {func_file}")

        elif do == "copy":
            allowed = {"do", "at", "from", "to"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in copy action")
            if "from" not in action:
                self.add_finding(file, f"{pointer}/from", "error", "bad-value", "Missing 'from' in copy action")
            else:
                self.validate_zone(action["from"], file, f"{pointer}/from")
            if "to" not in action:
                self.add_finding(file, f"{pointer}/to", "error", "bad-value", "Missing 'to' in copy action")
            else:
                self.validate_position(action["to"], file, f"{pointer}/to")

        elif do == "corrupt":
            allowed = {"do", "at", "area", "rules", "seed", "over_seconds"}
            for k in action:
                if k not in allowed:
                    self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in corrupt action")
            if "area" not in action:
                self.add_finding(file, f"{pointer}/area", "error", "bad-value", "Missing 'area' in corrupt action")
            else:
                self.validate_zone(action["area"], file, f"{pointer}/area")
            if "rules" not in action or not isinstance(action["rules"], list):
                self.add_finding(file, f"{pointer}/rules", "error", "bad-value", "Corrupt action requires list of 'rules'")
            else:
                for idx, r in enumerate(action["rules"]):
                    if not isinstance(r, dict):
                        self.add_finding(file, f"{pointer}/rules/{idx}", "error", "bad-value", "Corrupt rule must be an object")
                        continue
                    r_allowed = {"from", "to", "ratio"}
                    for r_k in r:
                        if r_k not in r_allowed:
                            self.add_finding(file, f"{pointer}/rules/{idx}/{r_k}", "error", "unknown-key", f"Unknown key '{r_k}' in corrupt rule")
                    if "from" not in r or not isinstance(r["from"], list) or not all(isinstance(b, str) and RE_RESOURCE_ID.match(b) for b in r["from"]):
                        self.add_finding(file, f"{pointer}/rules/{idx}/from", "error", "bad-value", "Corrupt rule 'from' must be list of block ids")
                    if "to" not in r or not isinstance(r["to"], list) or not all(isinstance(b, str) and RE_RESOURCE_ID.match(b) for b in r["to"]):
                        self.add_finding(file, f"{pointer}/rules/{idx}/to", "error", "bad-value", "Corrupt rule 'to' must be list of block ids")
                    if "ratio" not in r:
                        self.add_finding(file, f"{pointer}/rules/{idx}/ratio", "error", "bad-value", "Missing 'ratio' in corrupt rule")
                    else:
                        rat = r["ratio"]
                        if not is_number(rat) or rat < 0.0 or rat > 1.0:
                            self.add_finding(file, f"{pointer}/rules/{idx}/ratio", "error", "bad-value", "Corrupt rule ratio must be 0-1")
            if "seed" in action:
                sd = action["seed"]
                if not (isinstance(sd, int) and not isinstance(sd, bool)):
                    self.add_finding(file, f"{pointer}/seed", "error", "bad-value", "Seed must be integer")
            if "over_seconds" in action:
                s = action["over_seconds"]
                if not is_number(s):
                    self.add_finding(file, f"{pointer}/over_seconds", "error", "bad-value", "over_seconds must be a number")
                elif s < 0:
                    self.add_finding(file, f"{pointer}/over_seconds", "error", "bad-time", "over_seconds cannot be negative")

        elif do == "creature":
            if "op" not in action:
                self.add_finding(file, f"{pointer}/op", "error", "bad-value", "Missing 'op' in creature action")
                return
            op = action["op"]
            if op not in ("spawn", "stalk", "chase", "hold", "face_player", "despawn"):
                self.add_finding(file, f"{pointer}/op", "error", "bad-value", f"Invalid creature op '{op}'")
                return
            if "id" not in action:
                self.add_finding(file, f"{pointer}/id", "error", "bad-value", "Missing 'id' in creature action")
            else:
                cid = action["id"]
                if not isinstance(cid, str) or not RE_FLAG_COUNTER_ID.match(cid):
                    self.add_finding(file, f"{pointer}/id", "error", "bad-id", f"Invalid creature id '{cid}'")

            if op == "spawn":
                allowed = {"do", "at", "op", "id", "body", "where", "facing"}
                for k in action:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in creature spawn action")
                if "body" not in action:
                    self.add_finding(file, f"{pointer}/body", "error", "bad-value", "Missing 'body' in creature spawn action")
                else:
                    body = action["body"]
                    if not isinstance(body, dict):
                        self.add_finding(file, f"{pointer}/body", "error", "bad-value", "Creature body must be an object")
                    elif "mannequin" in body:
                        man = body["mannequin"]
                        if not isinstance(man, dict):
                            self.add_finding(file, f"{pointer}/body/mannequin", "error", "bad-value", "Mannequin must be an object")
                        else:
                            man_allowed = {"profile", "texture", "scale"}
                            for mk in man:
                                if mk not in man_allowed:
                                    self.add_finding(file, f"{pointer}/body/mannequin/{mk}", "error", "unknown-key", f"Unknown key '{mk}' in mannequin")
                            if "profile" not in man:
                                self.add_finding(file, f"{pointer}/body/mannequin/profile", "error", "bad-value", "Missing profile in mannequin")
                            if "scale" in man and not isinstance(man["scale"], (int, float)):
                                self.add_finding(file, f"{pointer}/body/mannequin/scale", "error", "bad-value", "Scale must be a number")
                    elif "summon" in body:
                        if not isinstance(body["summon"], str):
                            self.add_finding(file, f"{pointer}/body/summon", "error", "bad-value", "Summon must be a string")
                    else:
                        self.add_finding(file, f"{pointer}/body", "error", "bad-value", "Creature body must have 'mannequin' or 'summon'")
                if "where" not in action:
                    self.add_finding(file, f"{pointer}/where", "error", "bad-value", "Missing 'where' in creature spawn action")
                else:
                    self.validate_position(action["where"], file, f"{pointer}/where")
                if "facing" in action:
                    f = action["facing"]
                    if f != "player" and not is_number(f):
                        self.add_finding(file, f"{pointer}/facing", "error", "bad-value", "Facing must be 'player' or number")

            elif op == "stalk":
                allowed = {"do", "at", "op", "id", "step", "min_distance"}
                for k in action:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in creature stalk action")
                if "step" in action and not isinstance(action["step"], (int, float)):
                    self.add_finding(file, f"{pointer}/step", "error", "bad-value", "Step must be a number")
                if "min_distance" in action and not isinstance(action["min_distance"], (int, float)):
                    self.add_finding(file, f"{pointer}/min_distance", "error", "bad-value", "min_distance must be a number")

            elif op == "chase":
                allowed = {"do", "at", "op", "id", "speed", "catch_distance"}
                for k in action:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in creature chase action")
                if "speed" in action and not isinstance(action["speed"], (int, float)):
                    self.add_finding(file, f"{pointer}/speed", "error", "bad-value", "Speed must be a number")
                if "catch_distance" in action and not isinstance(action["catch_distance"], (int, float)):
                    self.add_finding(file, f"{pointer}/catch_distance", "error", "bad-value", "catch_distance must be a number")

            else:  # hold, face_player, despawn
                allowed = {"do", "at", "op", "id"}
                for k in action:
                    if k not in allowed:
                        self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in creature {op} action")

    def validate_beat(self, beat: Any, file: str, pointer: str, chapter_id: str) -> None:
        if not isinstance(beat, dict):
            self.add_finding(file, pointer, "error", "bad-value", "Beat must be an object")
            return

        allowed = {"id", "when", "if", "repeat", "actions", "capture", "autoplay"}
        for k in beat:
            if k not in allowed:
                self.add_finding(file, f"{pointer}/{k}", "error", "unknown-key", f"Unknown key '{k}' in beat")

        # Validate beat id
        if "id" not in beat:
            self.add_finding(file, f"{pointer}/id", "error", "bad-value", "Missing 'id' in beat")
        else:
            bid = beat["id"]
            if not isinstance(bid, str):
                self.add_finding(file, f"{pointer}/id", "error", "bad-value", "Beat id must be a string")
            else:
                parts = bid.split(".", 1)
                if len(parts) != 2 or parts[0] != chapter_id or not RE_BEAT_NAME.match(parts[1]):
                    self.add_finding(file, f"{pointer}/id", "error", "bad-id", f"Invalid beat id '{bid}' for chapter '{chapter_id}'")
                elif bid in self.all_beat_ids:
                    self.add_finding(file, f"{pointer}/id", "error", "duplicate-id", f"Duplicate beat id '{bid}'")
                else:
                    self.all_beat_ids.add(bid)
                    self.all_beat_id_sources[bid] = (file, pointer)

        # Autoplay input
        beat_autoplay_input: Optional[str] = None
        if "autoplay" in beat:
            ap = beat["autoplay"]
            if not isinstance(ap, dict):
                self.add_finding(file, f"{pointer}/autoplay", "error", "bad-value", "Beat autoplay must be an object")
            else:
                ap_allowed = {"input", "wait_seconds"}
                for k in ap:
                    if k not in ap_allowed:
                        self.add_finding(file, f"{pointer}/autoplay/{k}", "error", "unknown-key", f"Unknown key '{k}' in beat autoplay")
                if "input" in ap:
                    if not isinstance(ap["input"], str):
                        self.add_finding(file, f"{pointer}/autoplay/input", "error", "bad-value", "Autoplay input must be a string")
                    else:
                        beat_autoplay_input = ap["input"]
                if "wait_seconds" in ap:
                    ws = ap["wait_seconds"]
                    if not is_number(ws):
                        self.add_finding(file, f"{pointer}/autoplay/wait_seconds", "error", "bad-value", "wait_seconds must be a number")
                    elif ws < 0:
                        self.add_finding(file, f"{pointer}/autoplay/wait_seconds", "error", "bad-time", "wait_seconds cannot be negative")

        # Validate when
        if "when" not in beat:
            self.add_finding(file, f"{pointer}/when", "error", "bad-value", "Missing 'when' in beat")
        else:
            self.validate_trigger(beat["when"], file, f"{pointer}/when", beat_autoplay_input)

        # Validate if
        if "if" in beat:
            cond = beat["if"]
            if not isinstance(cond, dict):
                self.add_finding(file, f"{pointer}/if", "error", "bad-value", "Beat 'if' must be an object")
            else:
                cond_allowed = {"flags", "not_flags", "beats", "not_beats"}
                for k in cond:
                    if k not in cond_allowed:
                        self.add_finding(file, f"{pointer}/if/{k}", "error", "unknown-key", f"Unknown key '{k}' in beat 'if'")
                for fl_k in ("flags", "not_flags"):
                    if fl_k in cond:
                        fl_list = cond[fl_k]
                        if not isinstance(fl_list, list):
                            self.add_finding(file, f"{pointer}/if/{fl_k}", "error", "bad-value", f"'{fl_k}' must be a list")
                        else:
                            for idx, fl in enumerate(fl_list):
                                if not isinstance(fl, str) or not RE_FLAG_COUNTER_ID.match(fl):
                                    self.add_finding(file, f"{pointer}/if/{fl_k}/{idx}", "error", "bad-id", f"Invalid flag id '{fl}'")
                                else:
                                    self.all_flags_tested.append((fl, file, f"{pointer}/if/{fl_k}/{idx}"))
                for b_k in ("beats", "not_beats"):
                    if b_k in cond:
                        b_list = cond[b_k]
                        if not isinstance(b_list, list):
                            self.add_finding(file, f"{pointer}/if/{b_k}", "error", "bad-value", f"'{b_k}' must be a list")
                        else:
                            for idx, b in enumerate(b_list):
                                if not isinstance(b, str):
                                    self.add_finding(file, f"{pointer}/if/{b_k}/{idx}", "error", "bad-value", "Beat reference must be a string")
                                else:
                                    self.all_beat_refs.append((b, file, f"{pointer}/if/{b_k}/{idx}"))

        # Validate repeat
        if "repeat" in beat:
            rep = beat["repeat"]
            if not isinstance(rep, dict):
                self.add_finding(file, f"{pointer}/repeat", "error", "bad-value", "Beat repeat must be an object")
            else:
                rep_allowed = {"max", "cooldown_seconds"}
                for k in rep:
                    if k not in rep_allowed:
                        self.add_finding(file, f"{pointer}/repeat/{k}", "error", "unknown-key", f"Unknown key '{k}' in beat repeat")
                if "max" not in rep or not (isinstance(rep["max"], int) and not isinstance(rep["max"], bool)) or rep["max"] < 1:
                    self.add_finding(file, f"{pointer}/repeat/max", "error", "bad-value", "Repeat max must be integer >= 1")
                if "cooldown_seconds" not in rep:
                    self.add_finding(file, f"{pointer}/repeat/cooldown_seconds", "error", "bad-value", "Missing cooldown_seconds in repeat")
                else:
                    cs = rep["cooldown_seconds"]
                    if not is_number(cs):
                        self.add_finding(file, f"{pointer}/repeat/cooldown_seconds", "error", "bad-value", "cooldown_seconds must be a number")
                    elif cs < 0:
                        self.add_finding(file, f"{pointer}/repeat/cooldown_seconds", "error", "bad-time", "cooldown_seconds cannot be negative")

        # Validate capture
        if "capture" in beat:
            cap = beat["capture"]
            if not isinstance(cap, dict):
                self.add_finding(file, f"{pointer}/capture", "error", "bad-value", "Capture must be an object")
            else:
                cap_allowed = {"name", "at"}
                for k in cap:
                    if k not in cap_allowed:
                        self.add_finding(file, f"{pointer}/capture/{k}", "error", "unknown-key", f"Unknown key '{k}' in capture")
                if "name" not in cap:
                    self.add_finding(file, f"{pointer}/capture/name", "error", "bad-value", "Missing 'name' in capture")
                else:
                    cname = cap["name"]
                    if not isinstance(cname, str) or not RE_CAPTURE_ID.match(cname):
                        self.add_finding(file, f"{pointer}/capture/name", "error", "bad-id", f"Invalid capture name '{cname}'")
                    elif cname in self.all_capture_names:
                        self.add_finding(file, f"{pointer}/capture/name", "error", "duplicate-capture", f"Duplicate capture name '{cname}'")
                    else:
                        self.all_capture_names[cname] = (file, f"{pointer}/capture/name")
                if "at" not in cap:
                    self.add_finding(file, f"{pointer}/capture/at", "error", "bad-value", "Missing 'at' in capture")
                else:
                    ca = cap["at"]
                    if not is_number(ca):
                        self.add_finding(file, f"{pointer}/capture/at", "error", "bad-value", "Capture 'at' must be a number")
                    elif ca < 0:
                        self.add_finding(file, f"{pointer}/capture/at", "error", "bad-time", "Capture 'at' cannot be negative")

        # Validate actions
        if "actions" not in beat or not isinstance(beat["actions"], list):
            self.add_finding(file, f"{pointer}/actions", "error", "bad-value", "Beat requires list of 'actions'")
        else:
            for idx, act in enumerate(beat["actions"]):
                self.validate_action(act, file, f"{pointer}/actions/{idx}")

    def validate_chapter_file(self, ch_id: str) -> None:
        file_rel = f"chapters/{ch_id}.json"
        ch_path = self.story_dir / file_rel
        if not ch_path.is_file():
            return

        try:
            content = ch_path.read_text(encoding="utf-8")
            data = strict_json(content)
        except Exception as e:
            self.add_finding(file_rel, "", "error", "bad-json", f"Malformed JSON: {e}")
            return

        if not isinstance(data, dict):
            self.add_finding(file_rel, "", "error", "bad-value", "Chapter file must contain a JSON object")
            return

        allowed = {"id", "budget_seconds", "checkpoint", "respawn", "on_enter", "on_resume", "escape", "beats"}
        for k in data:
            if k not in allowed:
                self.add_finding(file_rel, f"/{k}", "error", "unknown-key", f"Unknown key '{k}' in chapter")

        # id
        if "id" not in data:
            self.add_finding(file_rel, "/id", "error", "bad-value", "Missing required key 'id' in chapter")
        else:
            actual_id = data["id"]
            if actual_id != ch_id:
                self.add_finding(file_rel, "/id", "error", "chapter-id-mismatch", f"Chapter id '{actual_id}' does not match filename '{ch_id}'")
            elif not isinstance(actual_id, str) or not RE_CHAPTER_ID.match(actual_id):
                self.add_finding(file_rel, "/id", "error", "bad-id", f"Invalid chapter id '{actual_id}'")

        # budget_seconds
        if "budget_seconds" not in data:
            self.add_finding(file_rel, "/budget_seconds", "error", "bad-value", "Missing required key 'budget_seconds'")
        else:
            bs = data["budget_seconds"]
            if not is_number(bs):
                self.add_finding(file_rel, "/budget_seconds", "error", "bad-value", "budget_seconds must be a number")
            elif bs < 30 or bs > 1200:
                self.add_finding(file_rel, "/budget_seconds", "error", "bad-time", f"budget_seconds ({bs}) must be between 30 and 1200")
            else:
                self.total_budget_seconds += bs

        # checkpoint
        if "checkpoint" not in data:
            self.add_finding(file_rel, "/checkpoint", "error", "bad-value", "Missing required key 'checkpoint'")
        else:
            self.validate_position(data["checkpoint"], file_rel, "/checkpoint")

        # respawn
        if "respawn" in data:
            self.validate_position(data["respawn"], file_rel, "/respawn")

        # on_enter, on_resume, escape
        for act_list_name in ("on_enter", "on_resume", "escape"):
            if act_list_name in data:
                acts = data[act_list_name]
                if not isinstance(acts, list):
                    self.add_finding(file_rel, f"/{act_list_name}", "error", "bad-value", f"'{act_list_name}' must be a list of actions")
                else:
                    for idx, act in enumerate(acts):
                        self.validate_action(act, file_rel, f"/{act_list_name}/{idx}")

        # beats
        if "beats" not in data or not isinstance(data["beats"], list):
            self.add_finding(file_rel, "/beats", "error", "bad-value", "Missing required key 'beats' (list)")
        else:
            for idx, b in enumerate(data["beats"]):
                self.validate_beat(b, file_rel, f"/beats/{idx}", ch_id)

    def validate_story_json(self) -> Optional[dict[str, Any]]:
        story_file = "story.json"
        story_path = self.story_dir / story_file
        if not story_path.is_file():
            self.add_finding(story_file, "", "error", "bad-value", "story.json does not exist")
            return None

        try:
            content = story_path.read_text(encoding="utf-8")
            data = strict_json(content)
        except Exception as e:
            self.add_finding(story_file, "", "error", "bad-json", f"Malformed JSON: {e}")
            return None

        if not isinstance(data, dict):
            self.add_finding(story_file, "", "error", "bad-value", "story.json root must be a JSON object")
            return None

        allowed = {
            "format", "languages", "default_language", "language_select",
            "agreement_fallback", "chapters", "anchors", "zones", "endings",
            "fallback_ending", "build", "autoplay"
        }
        for k in data:
            if k not in allowed:
                self.add_finding(story_file, f"/{k}", "error", "unknown-key", f"Unknown key '{k}' in story.json")

        required = {
            "format", "languages", "default_language", "language_select",
            "chapters", "endings", "fallback_ending", "autoplay"
        }
        for req in required:
            if req not in data:
                self.add_finding(story_file, f"/{req}", "error", "bad-value", f"Missing required key '{req}' in story.json")

        # format
        if "format" in data:
            if isinstance(data["format"], bool) or data["format"] != 1:
                self.add_finding(story_file, "/format", "error", "bad-value", f"Format must be 1, got {data['format']}")

        # languages
        if "languages" in data:
            langs = data["languages"]
            if not isinstance(langs, list) or len(langs) == 0:
                self.add_finding(story_file, "/languages", "error", "bad-value", "Languages must be a non-empty list")
            else:
                for idx, l in enumerate(langs):
                    if l not in ("es", "en"):
                        self.add_finding(story_file, f"/languages/{idx}", "error", "bad-value", f"Invalid language '{l}', must be 'es' or 'en'")
                self.languages = [str(l) for l in langs]

        # default_language
        if "default_language" in data:
            dl = data["default_language"]
            if not isinstance(dl, str):
                self.add_finding(story_file, "/default_language", "error", "bad-value", "default_language must be a string")
            elif dl not in self.languages:
                self.add_finding(story_file, "/default_language", "error", "missing-language", f"default_language '{dl}' not in languages {self.languages}")

        # language_select
        if "language_select" in data:
            ls = data["language_select"]
            if ls not in ("fixed", "client"):
                self.add_finding(story_file, "/language_select", "error", "bad-value", "language_select must be 'fixed' or 'client'")

        # agreement_fallback
        if "agreement_fallback" in data:
            self.has_agreement_fallback = True
            self.validate_text(data["agreement_fallback"], story_file, "/agreement_fallback")

        # anchors
        if "anchors" in data:
            ancs = data["anchors"]
            if not isinstance(ancs, dict):
                self.add_finding(story_file, "/anchors", "error", "bad-value", "Anchors must be an object")
            else:
                for k, v in ancs.items():
                    if not RE_FLAG_COUNTER_ID.match(k):
                        self.add_finding(story_file, f"/anchors/{k}", "error", "bad-id", f"Invalid anchor name '{k}'")
                    if k in self.map_checkpoints or k in self.map_landmarks or k in self.map_triggers:
                        self.add_finding(story_file, f"/anchors/{k}", "error", "duplicate-id", f"Anchor '{k}' duplicates map name")
                    self.story_anchors.add(k)

                    if not isinstance(v, dict):
                        self.add_finding(story_file, f"/anchors/{k}", "error", "bad-value", "Anchor must be an object")
                    else:
                        anc_allowed = {"pos", "dimension", "yaw", "pitch"}
                        for ak in v:
                            if ak not in anc_allowed:
                                self.add_finding(story_file, f"/anchors/{k}/{ak}", "error", "unknown-key", f"Unknown key '{ak}' in anchor")
                        if "pos" not in v or not isinstance(v["pos"], list) or len(v["pos"]) != 3 or not all(is_number(x) for x in v["pos"]):
                            self.add_finding(story_file, f"/anchors/{k}/pos", "error", "bad-value", "Anchor pos must be [x, y, z]")
                        if "dimension" in v and v["dimension"] not in ("overworld", "pocket"):
                            self.add_finding(story_file, f"/anchors/{k}/dimension", "error", "bad-value", "Dimension must be 'overworld' or 'pocket'")
                        if "yaw" in v and not isinstance(v["yaw"], (int, float)):
                            self.add_finding(story_file, f"/anchors/{k}/yaw", "error", "bad-value", "Yaw must be a number")
                        if "pitch" in v and not isinstance(v["pitch"], (int, float)):
                            self.add_finding(story_file, f"/anchors/{k}/pitch", "error", "bad-value", "Pitch must be a number")

        # zones
        if "zones" in data:
            zns = data["zones"]
            if not isinstance(zns, dict):
                self.add_finding(story_file, "/zones", "error", "bad-value", "Zones must be an object")
            else:
                for k, v in zns.items():
                    if not RE_FLAG_COUNTER_ID.match(k):
                        self.add_finding(story_file, f"/zones/{k}", "error", "bad-id", f"Invalid zone name '{k}'")
                    if (
                        k in self.map_triggers
                        or k in self.map_checkpoints
                        or k in self.map_landmarks
                        or k in self.story_anchors
                    ):
                        self.add_finding(story_file, f"/zones/{k}", "error", "duplicate-id", f"Zone '{k}' duplicates map/anchor name")
                    self.story_zones.add(k)

                    if not isinstance(v, dict):
                        self.add_finding(story_file, f"/zones/{k}", "error", "bad-value", "Zone must be an object")
                    else:
                        zn_allowed = {"box", "dimension"}
                        for zk in v:
                            if zk not in zn_allowed:
                                self.add_finding(story_file, f"/zones/{k}/{zk}", "error", "unknown-key", f"Unknown key '{zk}' in zone")
                        if "box" not in v or not isinstance(v["box"], list) or len(v["box"]) != 6 or not all(is_number(x) for x in v["box"]):
                            self.add_finding(story_file, f"/zones/{k}/box", "error", "bad-value", "Zone box must be [x1, y1, z1, x2, y2, z2]")
                        if "dimension" in v and v["dimension"] not in ("overworld", "pocket"):
                            self.add_finding(story_file, f"/zones/{k}/dimension", "error", "bad-value", "Dimension must be 'overworld' or 'pocket'")

        # endings
        if "endings" in data:
            ends = data["endings"]
            if not isinstance(ends, dict) or len(ends) == 0:
                self.add_finding(story_file, "/endings", "error", "bad-value", "Endings must be a non-empty object")
            else:
                for k, v in ends.items():
                    if not RE_ENDING_ID.match(k):
                        self.add_finding(story_file, f"/endings/{k}", "error", "bad-id", f"Invalid ending id '{k}'")
                    self.story_endings.add(k)

                    if not isinstance(v, dict):
                        self.add_finding(story_file, f"/endings/{k}", "error", "bad-value", "Ending must be an object")
                    elif v.get("builtin") is True:
                        end_allowed = {"builtin"}
                        for ek in v:
                            if ek not in end_allowed:
                                self.add_finding(story_file, f"/endings/{k}/{ek}", "error", "unknown-key", f"Unknown key '{ek}' in builtin ending")
                    else:
                        end_allowed = {"title", "lines", "splash", "quote_agreement"}
                        for ek in v:
                            if ek not in end_allowed:
                                self.add_finding(story_file, f"/endings/{k}/{ek}", "error", "unknown-key", f"Unknown key '{ek}' in ending")
                        if "title" not in v:
                            self.add_finding(story_file, f"/endings/{k}/title", "error", "bad-value", "Missing 'title' in ending")
                        else:
                            self.validate_text(v["title"], story_file, f"/endings/{k}/title")
                        if "lines" not in v or not isinstance(v["lines"], list):
                            self.add_finding(story_file, f"/endings/{k}/lines", "error", "bad-value", "Ending requires list of 'lines'")
                        else:
                            for idx, line in enumerate(v["lines"]):
                                self.validate_text(line, story_file, f"/endings/{k}/lines/{idx}", is_chat_or_farewell=True)
                        if "splash" not in v:
                            self.add_finding(story_file, f"/endings/{k}/splash", "error", "bad-value", "Missing 'splash' in ending")
                        else:
                            self.validate_text(v["splash"], story_file, f"/endings/{k}/splash")
                        if "quote_agreement" not in v or not isinstance(v["quote_agreement"], bool):
                            self.add_finding(story_file, f"/endings/{k}/quote_agreement", "error", "bad-value", "Missing or invalid 'quote_agreement' in ending")

        # fallback_ending
        if "fallback_ending" in data:
            fe = data["fallback_ending"]
            if not isinstance(fe, str) or not RE_ENDING_ID.match(fe):
                self.add_finding(story_file, "/fallback_ending", "error", "bad-value", f"Invalid fallback_ending '{fe}'")
            elif fe not in self.story_endings:
                self.add_finding(story_file, "/fallback_ending", "error", "unknown-ending", f"Unknown fallback_ending '{fe}'")
            else:
                self.story_fallback_ending = fe

        # build
        if "build" in data:
            bld = data["build"]
            if not isinstance(bld, dict):
                self.add_finding(story_file, "/build", "error", "bad-value", "Build must be an object")
            else:
                bld_allowed = {"forceload", "run"}
                for bk in bld:
                    if bk not in bld_allowed:
                        self.add_finding(story_file, f"/build/{bk}", "error", "unknown-key", f"Unknown key '{bk}' in build")
                if "forceload" in bld:
                    fl = bld["forceload"]
                    if not isinstance(fl, list):
                        self.add_finding(story_file, "/build/forceload", "error", "bad-value", "forceload must be a list")
                if "run" in bld:
                    rn = bld["run"]
                    if not isinstance(rn, list):
                        self.add_finding(story_file, "/build/run", "error", "bad-value", "run must be a list")
                    else:
                        for idx, cmd in enumerate(rn):
                            self.validate_command(cmd, story_file, f"/build/run/{idx}")

        # autoplay
        if "autoplay" in data:
            ap = data["autoplay"]
            if not isinstance(ap, dict):
                self.add_finding(story_file, "/autoplay", "error", "bad-value", "Autoplay must be an object")
            else:
                ap_allowed = {"time_scale", "runs"}
                for ak in ap:
                    if ak not in ap_allowed:
                        self.add_finding(story_file, f"/autoplay/{ak}", "error", "unknown-key", f"Unknown key '{ak}' in autoplay")
                if "time_scale" not in ap or not isinstance(ap["time_scale"], (int, float)) or ap["time_scale"] <= 0:
                    self.add_finding(story_file, "/autoplay/time_scale", "error", "bad-value", "time_scale must be a number > 0")

                if "runs" not in ap or not isinstance(ap["runs"], list):
                    self.add_finding(story_file, "/autoplay/runs", "error", "bad-value", "autoplay.runs must be a list")
                else:
                    runs = ap["runs"]
                    run_endings: list[str] = []
                    for idx, r in enumerate(runs):
                        if not isinstance(r, dict):
                            self.add_finding(story_file, f"/autoplay/runs/{idx}", "error", "bad-value", "Run must be an object")
                            continue
                        r_allowed = {"ending", "skip"}
                        for rk in r:
                            if rk not in r_allowed:
                                self.add_finding(story_file, f"/autoplay/runs/{idx}/{rk}", "error", "unknown-key", f"Unknown key '{rk}' in run")
                        if "ending" not in r:
                            self.add_finding(story_file, f"/autoplay/runs/{idx}/ending", "error", "bad-value", "Missing 'ending' in run")
                        else:
                            re_id = r["ending"]
                            if re_id not in self.story_endings:
                                self.add_finding(story_file, f"/autoplay/runs/{idx}/ending", "error", "autoplay-ending-coverage", f"Unknown ending in run: '{re_id}'")
                            elif re_id in run_endings:
                                self.add_finding(story_file, f"/autoplay/runs/{idx}/ending", "error", "autoplay-ending-coverage", f"Ending '{re_id}' listed more than once in autoplay.runs")
                            else:
                                run_endings.append(re_id)

                        if "skip" in r:
                            sk = r["skip"]
                            if not isinstance(sk, list):
                                self.add_finding(story_file, f"/autoplay/runs/{idx}/skip", "error", "bad-value", "Run skip must be a list")
                            else:
                                for s_idx, beat_ref in enumerate(sk):
                                    if not isinstance(beat_ref, str):
                                        self.add_finding(story_file, f"/autoplay/runs/{idx}/skip/{s_idx}", "error", "bad-value", "Skip item must be a string")
                                    else:
                                        self.all_beat_refs.append((beat_ref, story_file, f"/autoplay/runs/{idx}/skip/{s_idx}"))

                    # Check that every ending appears in autoplay.runs
                    for se in self.story_endings:
                        if se not in run_endings:
                            self.add_finding(story_file, "/autoplay/runs", "error", "autoplay-ending-coverage", f"Ending '{se}' missing from autoplay.runs")

        # chapters
        if "chapters" in data:
            chs = data["chapters"]
            if not isinstance(chs, list):
                self.add_finding(story_file, "/chapters", "error", "bad-value", "Chapters must be a list")
            elif len(chs) == 0:
                self.add_finding(story_file, "/chapters", "error", "bad-value", "Chapters list cannot be empty")
            elif len(chs) > 5:
                self.add_finding(story_file, "/chapters", "error", "too-many-chapters", f"Too many chapters: {len(chs)} > 5")
            else:
                seen_chs = set()
                for idx, ch_id in enumerate(chs):
                    if not isinstance(ch_id, str) or not RE_CHAPTER_ID.match(ch_id):
                        self.add_finding(story_file, f"/chapters/{idx}", "error", "bad-id", f"Invalid chapter id '{ch_id}'")
                    elif ch_id in seen_chs:
                        self.add_finding(story_file, f"/chapters/{idx}", "error", "duplicate-id", f"Duplicate chapter id '{ch_id}' in chapters list")
                    else:
                        seen_chs.add(ch_id)

                    ch_file = self.story_dir / f"chapters/{ch_id}.json"
                    if not ch_file.is_file():
                        self.add_finding(story_file, f"/chapters/{idx}", "error", "missing-chapter-file", f"Missing chapter file 'chapters/{ch_id}.json'")

        return data

    def run(self) -> list[Finding]:
        self.load_anchors()
        self.load_features()

        story_data = self.validate_story_json()

        # If story.json loaded, process chapter files
        if story_data and "chapters" in story_data and isinstance(story_data["chapters"], list):
            for ch_id in story_data["chapters"]:
                if isinstance(ch_id, str):
                    self.validate_chapter_file(ch_id)

        # Cross-file validations:
        # 1. Unknown beat refs
        for b_ref, file, ptr in self.all_beat_refs:
            if b_ref not in self.all_beat_ids:
                self.add_finding(file, ptr, "error", "unknown-beat-ref", f"Unknown beat reference '{b_ref}'")

        # 2. Flag never set
        for fl, file, ptr in self.all_flags_tested:
            if fl not in self.all_flags_set:
                self.add_finding(file, ptr, "error", "flag-never-set", f"Flag '{fl}' is tested but never set")

        # 3. Ending unreachable
        for eid in self.story_endings:
            if eid not in self.all_used_endings and eid != self.story_fallback_ending:
                self.add_finding("story.json", f"/endings/{eid}", "error", "ending-unreachable", f"Ending '{eid}' is never reached and not fallback_ending")

        # 4. Agreement fallback check
        if self.used_agreement and not self.has_agreement_fallback:
            self.add_finding("story.json", "/agreement_fallback", "error", "missing-language", "Text uses {agreement} but agreement_fallback is missing in story.json")

        # Warnings:
        # 5. Session length
        if self.total_budget_seconds < 600 or self.total_budget_seconds > 900:
            self.add_finding("story.json", "/chapters", "warning", "session-length", f"Total session budget {self.total_budget_seconds}s outside 600-900s")

        # 6. Screamer count
        if len(self.screamer_actions) > 2:
            for file, ptr in self.screamer_actions[2:]:
                self.add_finding(file, ptr, "warning", "screamer-count", f"More than two screamer cues in story ({len(self.screamer_actions)})")

        # Sort findings by file then pointer
        self.findings.sort(key=lambda f: (f.file, f.pointer))
        return self.findings


def check(
    story_dir: Union[str, Path],
    anchors: Optional[Union[str, Path]] = None,
    features: Optional[Union[str, Path]] = None,
    functions: Optional[Union[str, Path]] = None,
) -> list[Finding]:
    repo_root = Path(__file__).resolve().parent.parent
    story_dir_path = Path(story_dir)

    anchors_path = Path(anchors) if anchors is not None else repo_root / "scripts/map_anchors.json"

    if (story_dir_path / "features.json").is_file():
        features_path = story_dir_path / "features.json"
    elif features is not None:
        features_path = Path(features)
    else:
        features_path = repo_root / "src/main/resources/assets/unallocated/features.json"

    functions_dir = Path(functions) if functions is not None else repo_root / "src/main/resources/data/unallocated/function"

    validator = Validator(story_dir_path, anchors_path, features_path, functions_dir)
    return validator.run()


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent

    parser = argparse.ArgumentParser(description="Validate UNALLOCATED story files.")
    parser.add_argument("story_dir", nargs="?", default=str(repo_root / "src/main/resources/data/unallocated/story"), help="Story directory")
    parser.add_argument("--anchors", default=None, help="Map anchors JSON file")
    parser.add_argument("--features", default=None, help="Features JSON file")
    parser.add_argument("--functions", default=None, help="Functions directory")

    args = parser.parse_args()

    findings = check(args.story_dir, anchors=args.anchors, features=args.features, functions=args.functions)

    errors = 0
    warnings = 0
    for f in findings:
        if f.level == "error":
            errors += 1
        elif f.level == "warning":
            warnings += 1
        print(f"{f.file}#{f.pointer}: {f.level}: [{f.rule}] {f.message}")

    print(f"story_check: {errors} errors, {warnings} warnings")
    return 1 if errors > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
