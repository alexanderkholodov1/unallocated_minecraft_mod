package org.unallocated.story;

import com.google.gson.*;
import java.util.*;
import static org.unallocated.story.StoryData.*;

/** Pure language/placeholder selection; receipt/local clock are supplied only by the client. */
public final class StoryTexts {
    private StoryTexts() {}
    public static String language(JsonObject envelope, String locale) {
        String fallback = string(envelope, "default_language", "es");
        if (string(envelope, "language_select", "fixed").equals("fixed")) return fallback;
        String base = locale.toLowerCase(Locale.ROOT).split("[_-]", 2)[0];
        return array(envelope, "languages").asList().stream().anyMatch(e -> e.getAsString().equals(base)) ? base : fallback;
    }
    public static String render(JsonObject text, JsonObject envelope, String locale, Map<String, String> local) {
        String lang = language(envelope, locale);
        String value = string(text, lang, "");
        Map<String, String> values = new HashMap<>(local);
        values.put("elapsed_min", String.valueOf((int) number(envelope, "elapsed_min", 0)));
        values.put("deaths", String.valueOf((int) number(envelope, "deaths", 0)));
        if (!values.containsKey("agreement")) values.put("agreement", string(object(envelope, "agreement_fallback"), lang, ""));
        for (var counter : object(envelope, "counters").entrySet()) values.put("counter:" + counter.getKey(), counter.getValue().getAsString());
        var matcher = java.util.regex.Pattern.compile("\\{([^{}]+)\\}").matcher(value); StringBuilder output = new StringBuilder();
        while (matcher.find()) matcher.appendReplacement(output, java.util.regex.Matcher.quoteReplacement(values.getOrDefault(matcher.group(1), matcher.group(1).startsWith("counter:") ? "0" : "")));
        matcher.appendTail(output); return output.toString();
    }
}
