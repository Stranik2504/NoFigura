package org.figuramc.figura.compat.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.figuramc.figura.FiguraMod;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Data-driven 1.21.x -> 26.3 texture path mappings. */
public final class TextureMappings {

    private static final Map<String, String> EXACT = new HashMap<>();
    private static final Map<String, String> NAMES = new HashMap<>();
    private static final List<PatternRule> PATTERNS = new ArrayList<>();

    static {
        load();
    }

    private TextureMappings() {
    }

    private static void load() {
        try (InputStream in = TextureMappings.class.getResourceAsStream("/assets/figura/compat/legacy_textures.json")) {
            if (in == null) {
                FiguraMod.LOGGER.error("Missing /assets/figura/compat/legacy_textures.json");
                return;
            }

            JsonObject root = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject exact = root.getAsJsonObject("exact");
            if (exact != null) {
                for (Map.Entry<String, JsonElement> entry : exact.entrySet())
                    EXACT.put(entry.getKey(), entry.getValue().getAsString());
            }

            JsonObject names = root.getAsJsonObject("names");
            if (names != null) {
                for (Map.Entry<String, JsonElement> entry : names.entrySet())
                    NAMES.put(entry.getKey(), entry.getValue().getAsString());
            }

            JsonArray patterns = root.getAsJsonArray("patterns");
            if (patterns != null) {
                for (JsonElement element : patterns) {
                    JsonArray pair = element.getAsJsonArray();
                    PATTERNS.add(new PatternRule(Pattern.compile(pair.get(0).getAsString()), pair.get(1).getAsString()));
                }
            }
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to load legacy texture mappings", e);
        }
    }

    /** 26.3 resource path for a legacy for_modeling texture name, or null. */
    public static String modelTexturePath(String name) {
        String n = name.endsWith(".png") ? name.substring(0, name.length() - 4) : name;
        if (n.equals("white"))
            return null;
        return NAMES.get(n);
    }

    /** Exact or pattern-mapped path for an old RESOURCE path, or null. */
    public static String mapPath(String path) {
        String exact = EXACT.get(path);
        if (exact != null)
            return exact;

        for (PatternRule rule : PATTERNS) {
            if (rule.pattern().matcher(path).matches())
                return rule.pattern().matcher(path).replaceFirst(rule.replacement());
        }
        return null;
    }

    private record PatternRule(Pattern pattern, String replacement) {
    }
}
