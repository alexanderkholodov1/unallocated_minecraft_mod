package org.unallocated.story;

import com.google.gson.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import org.unallocated.core.Features;
import org.unallocated.map.MapLayout;
import static org.unallocated.story.StoryData.*;

/** Shared rule IDs/pointers with the offline checker. Parsers receive this validation context. */
public final class StoryValidator {
    public record Finding(String file, String pointer, String rule, String message, boolean warning) {
        public String marker() { return "UNALLOCATED story " + (warning ? "warning " : "invalid ") + file + "#" + pointer + ": [" + rule + "] " + message; }
    }
    public final class Context {
        public final String file, pointer;
        public final JsonObject beat;
        Context(String file, String pointer, JsonObject beat) { this.file = file; this.pointer = pointer; this.beat = beat; }
        public Context child(String suffix) { return context(file, pointer + suffix, beat); }
        public void error(String suffix, String rule, String message) { StoryValidator.this.error(file, pointer + suffix, rule, message); }
        public void keys(JsonObject o, String allowed, String required) { StoryValidator.this.keys(o, file, pointer, allowed, required); }
        public void position(JsonElement e, String key) { StoryValidator.this.position(e, file, pointer + "/" + key, false); }
        public void box(JsonElement e, String key) { StoryValidator.this.position(e, file, pointer + "/" + key, true); }
        public void action(JsonObject o) { validateAction(o, this); }
        public void trigger(JsonObject o) { validateTrigger(o, this); }
    }
    private record Ref(String id, String file, String pointer) {}
    private final List<Finding> findings = new ArrayList<>();
    private final List<Ref> beatRefs = new ArrayList<>(), flagRefs = new ArrayList<>();
    private final Set<String> beats = new HashSet<>(), flags = new HashSet<>(), ends = new HashSet<>(), captures = new HashSet<>();
    private final MinecraftServer server;
    private final Predicate<String> functionExists;
    private final boolean options, screamer;
    private JsonObject root;
    private List<String> languages = List.of();
    private int budget, screams;
    private boolean agreement;
    private static final String ID = "[a-z][a-z0-9_.]{0,47}", RESOURCE = "[a-z0-9_.-]+:[a-z0-9_./-]+";
    public StoryValidator(MinecraftServer server) {
        this(server, id -> server != null && server.getFunctions().get(Identifier.parse(id)).isPresent(),
            Features.load().enabled("effects.options"), Features.load().enabled("effects.screamer"));
    }
    public StoryValidator(MinecraftServer server, Predicate<String> functionExists, boolean options, boolean screamer) {
        this.server = server; this.functionExists = functionExists; this.options = options; this.screamer = screamer;
    }
    void bind(StoryData data) {
        root = data.root();
        languages = array(root, "languages").asList().stream().filter(StoryValidator::isString).map(JsonElement::getAsString).toList();
    }
    public List<Finding> findings() { return findings; }
    public void error(String file, String pointer, String rule, String message) { findings.add(new Finding(file, pointer, rule, message, false)); }
    private void warning(String file, String pointer, String rule, String message) { findings.add(new Finding(file, pointer, rule, message, true)); }
    public Context context(String file, String pointer, JsonObject beat) { return new Context(file, pointer, beat); }
    private static Set<String> words(String s) { return s.isBlank() ? Set.of() : Set.of(s.split(" +")); }
    private void keys(JsonObject o, String f, String p, String allowed, String required) {
        Set<String> allow = words(allowed);
        for (String k : o.keySet()) if (!allow.contains(k)) error(f, p + "/" + k, "unknown-key", "Unknown key " + k);
        for (String k : words(required)) if (!o.has(k)) error(f, p + "/" + k, "bad-value", "Required key " + k);
    }
    private void id(JsonElement e, String regex, String f, String p) {
        if (!isString(e) || !e.getAsString().matches(regex)) error(f, p, "bad-id", "Invalid identifier");
    }
    private static boolean isString(JsonElement e) { return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString(); }
    private static boolean isNumber(JsonElement e) { return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() && Double.isFinite(e.getAsDouble()); }
    private void num(JsonObject o, String key, double min, double max, boolean integer, String rule, String f, String p) {
        if (!o.has(key)) return;
        JsonElement e = o.get(key);
        if (!isNumber(e) || integer && e.getAsDouble() != Math.rint(e.getAsDouble())) error(f, p + "/" + key, "bad-value", "Expected finite " + (integer ? "integer" : "number"));
        else if (e.getAsDouble() < min || e.getAsDouble() > max) error(f, p + "/" + key, rule, "Out of range");
    }
    private void enumeration(JsonObject o, String key, String choices, String f, String p) {
        if (o.has(key) && (!isString(o.get(key)) || !words(choices).contains(o.get(key).getAsString()))) error(f, p + "/" + key, "bad-value", "Expected " + choices);
    }
    private void vector(JsonElement e, int n, String f, String p) {
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() != n) { error(f, p, "bad-value", "Expected " + n + " coordinates"); return; }
        for (JsonElement v : e.getAsJsonArray()) if (!isNumber(v)) { error(f, p, "bad-value", "Coordinates must be finite numbers"); break; }
    }
    private void position(JsonElement e, String f, String p, boolean box) {
        if (isString(e)) {
            String name = e.getAsString();
            boolean found = box ? MapLayout.triggers().containsKey(name) || object(root, "zones").has(name)
                : MapLayout.checkpoints().containsKey(name) || MapLayout.landmarks().containsKey(name) || object(root, "anchors").has(name);
            if (!found) error(f, p, box ? "unknown-zone" : "unknown-anchor", "Unknown name " + name);
        } else if (e != null && e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (!box && o.has("anchor")) {
                keys(o, f, p, "anchor offset", "anchor"); position(o.get("anchor"), f, p + "/anchor", false);
                if (o.has("offset")) vector(o.get("offset"), 3, f, p + "/offset");
            } else if (box && o.has("section")) {
                keys(o, f, p, "section", "section");
                if (!MapLayout.sections().containsKey(string(o, "section", ""))) error(f, p + "/section", "unknown-zone", "Unknown section");
            } else {
                String key = box ? "box" : "pos";
                keys(o, f, p, box ? "box dimension" : "pos dimension yaw pitch", key);
                vector(o.get(key), box ? 6 : 3, f, p + "/" + key);
                enumeration(o, "dimension", "overworld pocket", f, p);
                num(o, "yaw", -Double.MAX_VALUE, Double.MAX_VALUE, false, "bad-value", f, p);
                num(o, "pitch", -90, 90, false, "bad-value", f, p);
                if (server != null && string(o, "dimension", "overworld").equals("pocket")
                        && server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse("unallocated:pocket"))) == null)
                    error(f, p + "/dimension", "bad-value", "Dimension does not exist");
            }
        } else error(f, p, "bad-value", "Expected " + (box ? "zone" : "position"));
    }
    private void text(JsonElement e, String f, String p, boolean multiline, boolean title) {
        if (e == null || !e.isJsonObject()) { error(f, p, "bad-value", "Expected text object"); return; }
        JsonObject o = e.getAsJsonObject();
        keys(o, f, p, String.join(" ", languages) + " color bold italic obfuscated", "");
        for (String lang : languages) {
            if (!o.has(lang)) { error(f, p + "/" + lang, "missing-language", "Missing translation"); continue; }
            if (!isString(o.get(lang))) { error(f, p + "/" + lang, "bad-value", "Expected text string"); continue; }
            String value = o.get(lang).getAsString();
            if (!multiline && value.contains("\n")) error(f, p + "/" + lang, "bad-value", "Newline only allowed in chat/farewell lines");
            String stripped = value.replaceAll("\\{(?:player|agreement|local_time|world|elapsed_min|deaths|counter:[a-z][a-z0-9_.]{0,47})\\}", "");
            if (stripped.contains("{") || stripped.contains("}")) error(f, p + "/" + lang, "unknown-placeholder", "Unknown or malformed placeholder");
            agreement |= value.contains("{agreement}");
            if (title && value.length() > 40) warning(f, p + "/" + lang, "long-title", "Title exceeds 40 characters");
        }
        if (o.has("color") && (!isString(o.get("color")) || !o.get("color").getAsString().matches("#[0-9a-fA-F]{6}"))) error(f, p + "/color", "bad-value", "Invalid colour");
        for (String key : List.of("bold", "italic", "obfuscated")) boolType(o, key, f, p);
    }
    private void boolType(JsonObject o, String key, String f, String p) {
        if (o.has(key) && !(o.get(key).isJsonPrimitive() && o.getAsJsonPrimitive(key).isBoolean())) error(f, p + "/" + key, "bad-value", "Expected boolean");
    }
    public void command(JsonElement e, String f, String p) {
        if (!isString(e)) { error(f, p, "bad-value", "Expected command string"); return; }
        String cmd = e.getAsString();
        String first = cmd.strip().split(" +")[0];
        if (cmd.isBlank() || cmd.startsWith("/") || words("stop op deop kick ban ban-ip pardon pardon-ip whitelist save-all save-off save-on reload datapack publish debug perf jfr transfer tick").contains(first)) {
            error(f, p, "bad-command", "Empty, slash-prefixed or forbidden command"); return;
        }
        if (server != null) {
            var parsed = server.getCommands().getDispatcher().parse(cmd, server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER));
            if (parsed.getReader().canRead() || !parsed.getExceptions().isEmpty() || parsed.getContext().getCommand() == null)
                error(f, p, "bad-command", "Server dispatcher rejected command");
            // Parsing accepts symbolic names. Resolve their real argument types without executing.
            for (var context = parsed.getContext(); context != null; context = context.getChild()) {
                var built = context.build(cmd);
                for (var parsedNode : context.getNodes()) {
                    if (!(parsedNode.getNode() instanceof com.mojang.brigadier.tree.ArgumentCommandNode<?, ?> argument)) continue;
                    try {
                        if (argument.getType() instanceof net.minecraft.commands.arguments.DimensionArgument)
                            net.minecraft.commands.arguments.DimensionArgument.getDimension(built, argument.getName());
                        else if (argument.getType() instanceof net.minecraft.commands.arguments.item.FunctionArgument)
                            net.minecraft.commands.arguments.item.FunctionArgument.getFunctions(built, argument.getName());
                        else if (argument.getName().equals("structure") && argument.getType() instanceof net.minecraft.commands.arguments.ResourceKeyArgument<?>)
                            net.minecraft.commands.arguments.ResourceKeyArgument.getStructure(built, argument.getName());
                        else if (argument.getName().equals("template") && built.getArgument(argument.getName(), Object.class) instanceof Identifier template
                                && server.getStructureTemplateManager().get(template).isEmpty())
                            error(f, p, "bad-value", "Structure template does not exist: " + template);
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException | IllegalArgumentException missing) {
                        error(f, p, argument.getType() instanceof net.minecraft.commands.arguments.item.FunctionArgument ? "unknown-function" : "bad-value", missing.getMessage());
                    }
                }
            }
        }
    }
    public void validate(StoryData data) {
        root = data.root(); String f = "story.json";
        keys(root, f, "", "format languages default_language language_select agreement_fallback chapters anchors zones endings fallback_ending build autoplay", "format languages default_language language_select chapters endings fallback_ending autoplay");
        if (!isNumber(root.get("format")) || number(root, "format", -1) != 1) error(f, "/format", "bad-value", "Only format 1 is supported");
        if (!root.has("languages") || !root.get("languages").isJsonArray()) error(f, "/languages", "bad-value", "Expected language list");
        for (String key : List.of("anchors", "zones", "endings", "build", "autoplay")) if (root.has(key) && !root.get(key).isJsonObject()) error(f, "/" + key, "bad-value", "Expected object");
        languages = array(root, "languages").asList().stream().filter(StoryValidator::isString).map(JsonElement::getAsString).toList();
        for (int i = 0; i < array(root, "languages").size(); i++) if (!isString(array(root, "languages").get(i))) error(f, "/languages/" + i, "bad-value", "Expected language string");
        if (languages.isEmpty() || new HashSet<>(languages).size() != languages.size() || languages.stream().anyMatch(l -> !Set.of("es", "en").contains(l))) error(f, "/languages", "bad-value", "Nonempty unique subset of es/en required");
        if (!languages.contains(string(root, "default_language", ""))) error(f, "/default_language", "missing-language", "Default must be in languages");
        enumeration(root, "language_select", "fixed client", f, "");
        if (root.has("agreement_fallback")) text(root.get("agreement_fallback"), f, "/agreement_fallback", false, false);
        Set<String> names = new HashSet<>(); names.addAll(MapLayout.checkpoints().keySet()); names.addAll(MapLayout.landmarks().keySet()); names.addAll(MapLayout.triggers().keySet()); names.addAll(MapLayout.sections().keySet());
        for (String key : List.of("anchors", "zones")) for (var entry : object(root, key).entrySet()) {
            String p = "/" + key + "/" + entry.getKey(); id(new JsonPrimitive(entry.getKey()), ID, f, p);
            if (!names.add(entry.getKey())) error(f, p, "duplicate-id", "Duplicate map/story name");
            if (entry.getValue().isJsonObject()) keys(entry.getValue().getAsJsonObject(), f, p, key.equals("zones") ? "box dimension" : "pos dimension yaw pitch", key.equals("zones") ? "box" : "pos");
            position(entry.getValue(), f, p, key.equals("zones"));
        }
        JsonObject endings = object(root, "endings");
        if (endings.isEmpty()) error(f, "/endings", "bad-value", "Endings required");
        for (var entry : endings.entrySet()) {
            String p = "/endings/" + entry.getKey(); id(new JsonPrimitive(entry.getKey()), "[A-Z][A-Z0-9_]{0,31}", f, p);
            if (!entry.getValue().isJsonObject()) { error(f, p, "bad-value", "Expected ending object"); continue; }
            JsonObject o = entry.getValue().getAsJsonObject();
            if (bool(o, "builtin", false)) {
                keys(o, f, p, "builtin", "builtin");
                if (!entry.getKey().equals("SERVER_CLOSED")) error(f, p, "bad-value", "Only SERVER_CLOSED is builtin");
            } else {
                keys(o, f, p, "title lines splash quote_agreement", "title lines splash quote_agreement");
                text(o.get("title"), f, p + "/title", false, true); text(o.get("splash"), f, p + "/splash", false, false);
                JsonArray lines = list(o, "lines", f, p);
                for (int i = 0; i < lines.size(); i++) text(lines.get(i), f, p + "/lines/" + i, true, false);
                boolType(o, "quote_agreement", f, p);
            }
        }
        if (!endings.has(string(root, "fallback_ending", ""))) error(f, "/fallback_ending", "unknown-ending", "Unknown fallback ending");
        JsonArray chapters = list(root, "chapters", f, "");
        if (chapters.isEmpty()) error(f, "/chapters", "bad-value", "Chapters required");
        if (chapters.size() > 5) error(f, "/chapters", "too-many-chapters", "At most five chapters");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < chapters.size(); i++) {
            id(chapters.get(i), "[a-z][a-z0-9_]{0,23}", f, "/chapters/" + i);
            if (isString(chapters.get(i)) && !seen.add(chapters.get(i).getAsString())) error(f, "/chapters/" + i, "duplicate-id", "Duplicate chapter");
        }
        for (int i = 0; i < data.chapters().size(); i++) {
            JsonObject chapter = data.chapters().get(i);
            String expected = i < chapters.size() && isString(chapters.get(i)) ? chapters.get(i).getAsString() : "";
            chapter(chapter, "chapters/" + expected + ".json", expected);
        }
        JsonObject build = object(root, "build"); keys(build, f, "/build", "forceload run", "");
        for (String key : List.of("run", "forceload")) if (build.has(key) && !build.get(key).isJsonArray()) error(f, "/build/" + key, "bad-value", "Expected list");
        JsonArray commands = array(build, "run");
        for (int i = 0; i < commands.size(); i++) command(commands.get(i), f, "/build/run/" + i);
        JsonArray loads = array(build, "forceload");
        for (int i = 0; i < loads.size(); i++) {
            JsonElement e = loads.get(i); String p = "/build/forceload/" + i;
            if (!e.isJsonArray() || e.getAsJsonArray().size() != 5) error(f, p, "bad-value", "Expected [x1,z1,x2,z2,dimension]");
            else {
                for (int j = 0; j < 4; j++) if (!isNumber(e.getAsJsonArray().get(j))) error(f, p + "/" + j, "bad-value", "Expected coordinate");
                if (!Set.of("overworld", "pocket").contains(e.getAsJsonArray().get(4).getAsString())) error(f, p + "/4", "bad-value", "Unknown dimension");
            }
        }
        JsonObject autoplay = object(root, "autoplay"); keys(autoplay, f, "/autoplay", "time_scale runs", "time_scale runs");
        num(autoplay, "time_scale", Double.MIN_VALUE, Double.MAX_VALUE, false, "bad-value", f, "/autoplay");
        Set<String> covered = new HashSet<>(); JsonArray runs = list(autoplay, "runs", f, "/autoplay");
        for (int i = 0; i < runs.size(); i++) {
            if (!runs.get(i).isJsonObject()) { error(f, "/autoplay/runs/" + i, "bad-value", "Expected run"); continue; }
            JsonObject run = runs.get(i).getAsJsonObject(); String p = "/autoplay/runs/" + i;
            keys(run, f, p, "ending skip", "ending");
            String end = string(run, "ending", "");
            if (!endings.has(end) || !covered.add(end)) error(f, p + "/ending", "autoplay-ending-coverage", "Unknown or duplicate ending");
            if (run.has("skip") && !run.get("skip").isJsonArray()) error(f, p + "/skip", "bad-value", "Expected beat list");
            refs(array(run, "skip"), beatRefs, f, p + "/skip");
        }
        if (!covered.equals(endings.keySet())) error(f, "/autoplay/runs", "autoplay-ending-coverage", "Every ending must have exactly one run");
        for (Ref ref : beatRefs) if (!beats.contains(ref.id)) error(ref.file, ref.pointer, "unknown-beat-ref", ref.id);
        for (Ref ref : flagRefs) if (!flags.contains(ref.id)) error(ref.file, ref.pointer, "flag-never-set", ref.id);
        for (String end : endings.keySet()) if (!ends.contains(end) && !end.equals(string(root, "fallback_ending", ""))) error(f, "/endings/" + end, "ending-unreachable", "No end action reaches " + end);
        if (agreement && !root.has("agreement_fallback")) error(f, "/agreement_fallback", "missing-language", "Agreement fallback required");
        if (budget < 600 || budget > 900) warning(f, "/chapters", "session-length", "Budget outside 600–900 seconds");
    }
    private JsonArray list(JsonObject o, String k, String f, String p) {
        if (!o.has(k) || !o.get(k).isJsonArray()) error(f, p + "/" + k, "bad-value", "Expected list");
        return array(o, k);
    }
    private void refs(JsonArray list, List<Ref> refs, String f, String p) {
        for (int i = 0; i < list.size(); i++) if (isString(list.get(i))) refs.add(new Ref(list.get(i).getAsString(), f, p + "/" + i)); else error(f, p + "/" + i, "bad-value", "Expected identifier");
    }
    private void chapter(JsonObject o, String f, String expected) {
        keys(o, f, "", "id budget_seconds checkpoint respawn beats on_enter on_resume escape", "id budget_seconds checkpoint beats");
        if (!string(o, "id", "").equals(expected)) error(f, "/id", "chapter-id-mismatch", "Must match filename/list");
        num(o, "budget_seconds", 30, 1200, false, "bad-time", f, ""); budget += (int) number(o, "budget_seconds", 0);
        position(o.get("checkpoint"), f, "/checkpoint", false);
        if (o.has("respawn")) position(o.get("respawn"), f, "/respawn", false);
        for (String key : List.of("on_enter", "on_resume", "escape")) actions(o, key, f, "/" + key, new JsonObject());
        JsonArray list = list(o, "beats", f, "");
        for (int i = 0; i < list.size(); i++) {
            String p = "/beats/" + i;
            if (!list.get(i).isJsonObject()) { error(f, p, "bad-value", "Expected beat"); continue; }
            JsonObject beat = list.get(i).getAsJsonObject();
            keys(beat, f, p, "id when if repeat actions capture autoplay", "id when actions");
            id(beat.get("id"), Pattern.quote(expected) + "\\.[a-z0-9_]{1,32}", f, p + "/id");
            if (!beats.add(string(beat, "id", ""))) error(f, p + "/id", "duplicate-id", "Duplicate beat");
            for (String key : List.of("if", "repeat", "capture", "autoplay")) if (beat.has(key) && !beat.get(key).isJsonObject()) error(f, p + "/" + key, "bad-value", "Expected object");
            JsonObject guards = object(beat, "if"); keys(guards, f, p + "/if", "flags not_flags beats not_beats", "");
            for (String key : List.of("flags", "not_flags", "beats", "not_beats")) if (guards.has(key) && !guards.get(key).isJsonArray()) error(f, p + "/if/" + key, "bad-value", "Expected list");
            for (String key : List.of("flags", "not_flags")) refs(array(guards, key), flagRefs, f, p + "/if/" + key);
            for (String key : List.of("beats", "not_beats")) refs(array(guards, key), beatRefs, f, p + "/if/" + key);
            JsonObject repeat = object(beat, "repeat"); keys(repeat, f, p + "/repeat", "max cooldown_seconds", beat.has("repeat") ? "max cooldown_seconds" : "");
            num(repeat, "max", 1, Integer.MAX_VALUE, true, "bad-value", f, p + "/repeat"); num(repeat, "cooldown_seconds", 0, Double.MAX_VALUE, false, "bad-time", f, p + "/repeat");
            JsonObject autoplay = object(beat, "autoplay"); keys(autoplay, f, p + "/autoplay", "input wait_seconds", ""); num(autoplay, "wait_seconds", 0, Double.MAX_VALUE, false, "bad-time", f, p + "/autoplay");
            if (autoplay.has("input") && !isString(autoplay.get("input"))) error(f, p + "/autoplay/input", "bad-value", "Expected input string");
            if (beat.has("capture")) {
                JsonObject capture = object(beat, "capture"); keys(capture, f, p + "/capture", "name at", "name at");
                id(capture.get("name"), "[a-z0-9][a-z0-9-]{0,39}", f, p + "/capture/name");
                if (!captures.add(string(capture, "name", ""))) error(f, p + "/capture/name", "duplicate-capture", "Duplicate capture");
                num(capture, "at", 0, 600, false, "bad-time", f, p + "/capture");
            }
            if (!beat.has("when") || !beat.get("when").isJsonObject()) error(f, p + "/when", "bad-value", "Expected trigger");
            else {
                JsonObject when = beat.getAsJsonObject("when"); var parser = StoryTriggers.parser(string(when, "type", ""));
                if (parser == null) error(f, p + "/when/type", "unknown-trigger", "Unknown trigger");
                else try { parser.parse(when, context(f, p + "/when", beat)); } catch (RuntimeException e) { error(f, p + "/when", "bad-value", e.toString()); }
            }
            actions(beat, "actions", f, p + "/actions", beat);
        }
    }
    private void actions(JsonObject o, String key, String f, String p, JsonObject beat) {
        if (o.has(key) && !o.get(key).isJsonArray()) { error(f, p, "bad-value", "Expected action list"); return; }
        JsonArray list = array(o, key);
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isJsonObject()) { error(f, p + "/" + i, "bad-value", "Expected action"); continue; }
            JsonObject action = list.get(i).getAsJsonObject(); var parser = StoryActions.parser(string(action, "do", ""));
            if (parser == null) error(f, p + "/" + i + "/do", "unknown-action", "Unknown action");
            else try { parser.parse(action, context(f, p + "/" + i, beat)); } catch (RuntimeException e) { error(f, p + "/" + i, "bad-value", e.toString()); }
        }
    }
    private void validateAction(JsonObject o, Context c) {
        String f = c.file, p = c.pointer, name = string(o, "do", "");
        String spec = switch (name) {
            case "flag" -> "set clear"; case "counter" -> "id add set"; case "end" -> "ending"; case "cancel" -> "beat";
            case "title" -> "title subtitle fade_in stay fade_out"; case "actionbar" -> "text"; case "chat" -> "text from";
            case "blackout" -> "fade_in hold fade_out color"; case "interface" -> "effect seconds generation strength leak seed sign advancement text";
            case "audio" -> "cue where"; case "teleport" -> "to blackout"; case "bait" -> string(o, "op", "").equals("place") ? "op lure distances scare appear_delay fallback_after max_lifetime seed" : "op";
            case "command" -> "run"; case "function" -> "id"; default -> "";
        };
        String required = switch (name) { case "counter", "function" -> "id"; case "end" -> "ending"; case "cancel" -> "beat"; case "title" -> "title"; case "actionbar", "chat" -> "text"; case "interface" -> "effect seconds"; case "audio" -> "cue"; case "teleport" -> "to"; case "bait" -> "op"; case "command" -> "run"; default -> ""; };
        keys(o, f, p, "do at " + spec, required);
        num(o, "at", 0, 600, false, "bad-time", f, p);
        for (String key : List.of("seconds", "fade_in", "hold", "fade_out", "stay", "appear_delay", "fallback_after", "max_lifetime")) num(o, key, 0, Double.MAX_VALUE, false, "bad-time", f, p);
        for (String key : List.of("text", "title", "subtitle", "from")) if (o.has(key)) text(o.get(key), f, p + "/" + key, name.equals("chat"), key.equals("title"));
        for (String key : List.of("to", "where", "sign", "lure")) if (o.has(key)) position(o.get(key), f, p + "/" + key, false);
        for (String key : List.of("blackout", "scare")) boolType(o, key, f, p);
        num(o, "seed", Long.MIN_VALUE, Long.MAX_VALUE, true, "bad-value", f, p);
        if (o.has("color") && (!isString(o.get("color")) || !o.get("color").getAsString().matches("#[a-fA-F0-9]{6}"))) error(f, p + "/color", "bad-value", "Invalid colour");
        switch (name) {
            case "flag" -> {
                if (o.has("set") == o.has("clear")) error(f, p, "bad-value", "Exactly one of set/clear");
                for (String key : List.of("set", "clear")) if (o.has(key)) id(o.get(key), ID, f, p + "/" + key);
                if (isString(o.get("set"))) flags.add(o.get("set").getAsString());
            }
            case "counter" -> {
                id(o.get("id"), ID, f, p + "/id");
                if (o.has("set") == o.has("add")) error(f, p, "bad-value", "Exactly one of set/add");
                num(o, "set", Integer.MIN_VALUE, Integer.MAX_VALUE, true, "bad-value", f, p); num(o, "add", Integer.MIN_VALUE, Integer.MAX_VALUE, true, "bad-value", f, p);
            }
            case "end" -> {
                String end = string(o, "ending", ""); ends.add(end);
                if (!object(root, "endings").has(end)) error(f, p + "/ending", "unknown-ending", end);
            }
            case "cancel" -> beatRefs.add(new Ref(string(o, "beat", ""), f, p + "/beat"));
            case "command" -> command(o.get("run"), f, p + "/run");
            case "function" -> {
                String nameId = string(o, "id", "");
                if (!nameId.matches("unallocated:[a-z0-9_./-]+")) error(f, p + "/id", "bad-value", "Invalid function ID");
                else if (!functionExists.test(nameId)) error(f, p + "/id", "unknown-function", nameId);
                else command(new JsonPrimitive("function " + nameId), f, p + "/id");
            }
            case "audio" -> {
                enumeration(o, "cue", "ambient threads muffle screamer stop", f, p);
                if (string(o, "cue", "").equals("screamer") && ++screams > 2) warning(f, p, "screamer-count", "More than two screamer cues");
            }
            case "interface" -> {
                String effect = string(o, "effect", "");
                enumeration(o, "effect", "F3 DEATH SPLASH LOADING ADVANCEMENT SENSITIVITY FOV GAMMA SIGN", f, p);
                enumeration(o, "leak", "es en mixed", f, p);
                num(o, "generation", 0, 5, true, "bad-value", f, p); num(o, "strength", -1, 1, false, "bad-value", f, p);
                double max = effect.equals("F3") ? 3 : Set.of("DEATH", "SPLASH", "LOADING").contains(effect) ? 30 : 5;
                num(o, "seconds", .05, max, false, "bad-time", f, p);
                if (effect.equals("SIGN") && !o.has("sign")) error(f, p + "/sign", "bad-value", "SIGN requires position");
                if (!options && Set.of("SENSITIVITY", "FOV", "GAMMA").contains(effect)) error(f, p + "/effect", "feature-off", "effects.options is false");
                if (o.has("advancement") && (!isString(o.get("advancement")) || !o.get("advancement").getAsString().matches(RESOURCE))) error(f, p + "/advancement", "bad-value", "Invalid resource ID");
            }
            case "bait" -> {
                enumeration(o, "op", "place step scare clear", f, p);
                if (!screamer && (bool(o, "scare", false) || string(o, "op", "").equals("scare"))) error(f, p + "/scare", "feature-off", "effects.screamer is false");
                if (o.has("distances")) {
                    double previous = 161; JsonArray ds = list(o, "distances", f, p);
                    if (ds.isEmpty() || ds.size() > 12) error(f, p + "/distances", "bad-value", "1–12 distances required");
                    for (JsonElement d : ds) {
                        if (!isNumber(d) || d.getAsDouble() < 4 || d.getAsDouble() > 160 || d.getAsDouble() >= previous) error(f, p + "/distances", "bad-value", "Distances must decrease within 4–160");
                        else previous = d.getAsDouble();
                    }
                }
            }
            default -> { }
        }
    }
    private void validateTrigger(JsonObject o, Context c) {
        String f = c.file, p = c.pointer, type = string(o, "type", "");
        String spec = switch (type) {
            case "after" -> "seconds beat"; case "flag" -> "flag"; case "zone" -> "zone seconds";
            case "look" -> "at degrees seconds max_distance spyglass"; case "interact" -> "at";
            case "use_item" -> "item"; case "has_item" -> "item count"; case "break" -> "block zone";
            case "chat" -> "pattern hide"; case "still", "sprint", "sneak" -> "seconds"; case "bait" -> "event"; case "any" -> "of"; default -> "";
        };
        String required = switch (type) { case "after", "still", "sprint", "sneak" -> "seconds"; case "flag" -> "flag"; case "zone" -> "zone"; case "look", "interact" -> "at"; case "use_item", "has_item" -> "item"; case "break" -> "block"; case "chat" -> "pattern"; case "bait" -> "event"; case "any" -> "of"; default -> ""; };
        keys(o, f, p, "type " + spec, required);
        num(o, "seconds", 0, Double.MAX_VALUE, false, "bad-time", f, p); num(o, "degrees", 0, 180, false, "bad-value", f, p);
        num(o, "max_distance", .01, Double.MAX_VALUE, false, "bad-value", f, p); num(o, "count", 1, Integer.MAX_VALUE, true, "bad-value", f, p);
        boolType(o, "spyglass", f, p); boolType(o, "hide", f, p);
        if (o.has("at")) position(o.get("at"), f, p + "/at", false);
        if (o.has("zone")) position(o.get("zone"), f, p + "/zone", true);
        for (String key : List.of("block", "item")) if (o.has(key) && (!isString(o.get(key)) || !o.get(key).getAsString().matches(RESOURCE))) error(f, p + "/" + key, "bad-value", "Invalid resource ID");
        if (o.has("beat")) beatRefs.add(new Ref(string(o, "beat", ""), f, p + "/beat"));
        if (type.equals("flag")) { id(o.get("flag"), ID, f, p + "/flag"); flagRefs.add(new Ref(string(o, "flag", ""), f, p + "/flag")); }
        if (type.equals("bait")) enumeration(o, "event", "step screamer gone", f, p);
        if (type.equals("chat")) try {
            var pattern = Pattern.compile(string(o, "pattern", ""));
            String input = string(object(c.beat, "autoplay"), "input", "");
            if (!object(c.beat, "autoplay").has("input")) error(f, p, "chat-without-input", "autoplay.input required");
            else if (!pattern.matcher(input).find()) error(f, p + "/pattern", "chat-without-input", "Input must match pattern");
        } catch (RuntimeException e) { error(f, p + "/pattern", "chat-without-input", "Invalid regex"); }
        if (type.equals("any")) {
            JsonArray of = list(o, "of", f, p);
            if (of.isEmpty()) error(f, p + "/of", "bad-value", "Nonempty trigger list required");
            for (int i = 0; i < of.size(); i++) {
                if (!of.get(i).isJsonObject()) { error(f, p + "/of/" + i, "bad-value", "Expected trigger"); continue; }
                JsonObject sub = of.get(i).getAsJsonObject(); var parser = StoryTriggers.parser(string(sub, "type", ""));
                if (parser == null) error(f, p + "/of/" + i + "/type", "unknown-trigger", "Unknown trigger");
                // The built-in any parser compiles/validates each child through its registered parser.
            }
        }
    }
}
