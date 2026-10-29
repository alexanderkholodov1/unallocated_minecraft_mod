package org.unallocated.story;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Reads only named files inside the story root; no directory enumeration or external process. */
public final class StoryLoader {
    @FunctionalInterface public interface Source { String read(String name) throws IOException; }
    public record Result(StoryData data, List<StoryValidator.Finding> findings) {
        public boolean valid() { return findings.stream().noneMatch(f -> !f.warning()); }
    }
    private StoryLoader() {}
    public static Source directory(Path root) {
        Path base = root.toAbsolutePath().normalize();
        return name -> {
            Path path = base.resolve(name).normalize();
            if (!path.startsWith(base)) throw new IOException("Path outside story directory");
            return Files.readString(path);
        };
    }
    public static Source resources() {
        String override = System.getProperty("unallocated.story.dir");
        if (override != null) return directory(Path.of(override));
        return name -> {
            try (var in = StoryLoader.class.getResourceAsStream("/data/unallocated/story/" + name)) {
                if (in == null) throw new FileNotFoundException(name);
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        };
    }
    public static Result load(Source source, StoryValidator validator) {
        JsonObject root = read(source, "story.json", validator);
        List<JsonObject> chapters = new ArrayList<>();
        JsonArray names = StoryData.array(root, "chapters");
        for (int i = 0; i < names.size(); i++) {
            JsonElement name = names.get(i);
            if (!name.isJsonPrimitive() || !name.getAsJsonPrimitive().isString()
                    || !name.getAsString().matches("[a-z][a-z0-9_]{0,23}")) continue;
            String file = "chapters/" + name.getAsString() + ".json";
            try { chapters.add(parse(source.read(file))); }
            catch (FileNotFoundException | NoSuchFileException e) {
                validator.error("story.json", "/chapters/" + i, "missing-chapter-file", file);
            } catch (Exception e) { validator.error(file, "", "bad-json", e.getMessage()); }
        }
        StoryData data = new StoryData(root, chapters);
        try { validator.validate(data); }
        catch (RuntimeException malformed) { validator.error("story.json", "", "bad-value", "Malformed value: " + malformed.getClass().getSimpleName()); }
        return new Result(data, List.copyOf(validator.findings()));
    }
    private static JsonObject read(Source source, String file, StoryValidator validator) {
        try { return parse(source.read(file)); }
        catch (Exception e) { validator.error(file, "", "bad-json", String.valueOf(e.getMessage())); return new JsonObject(); }
    }
    /** Gson's object parser silently overwrites duplicate keys. Reject them before building objects. */
    public static JsonObject parse(String text) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(text));
        reader.setStrictness(Strictness.STRICT);
        JsonElement value = value(reader);
        if (reader.peek() != JsonToken.END_DOCUMENT || !value.isJsonObject()) throw new IOException("Expected one object");
        return value.getAsJsonObject();
    }
    private static JsonElement value(JsonReader reader) throws IOException {
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object = new JsonObject(); reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (object.has(key)) throw new IOException("Duplicate key at " + reader.getPath());
                    object.add(key, value(reader));
                }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                JsonArray array = new JsonArray(); reader.beginArray();
                while (reader.hasNext()) array.add(value(reader)); reader.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new java.math.BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IOException("Invalid JSON at " + reader.getPath());
        };
    }
}
