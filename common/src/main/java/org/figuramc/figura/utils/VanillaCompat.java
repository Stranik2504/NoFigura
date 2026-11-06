package org.figuramc.figura.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Central compatibility layer for vanilla format/path changes in 26.3.
 *
 * <p>This keeps all mapping-based workarounds in one place:
 * block-state NBT, legacy particle options, legacy vanilla texture paths and
 * client-side EntityTask entities.</p>
 */
public final class VanillaCompat {

    private VanillaCompat() {
    }

    /** 26.3 block-state and particle block-state format. */
    public static final class Blocks {

        private Blocks() {
        }

        /** Returns the state's properties, accepting 26.3 and legacy key names. */
        public static CompoundTag properties(BlockState state) {
            CompoundTag tag = NbtUtils.writeBlockState(state);
            Tag properties = tag.get("properties");
            if (properties == null)
                properties = tag.get("Properties");
            return properties instanceof CompoundTag compound ? compound : new CompoundTag();
        }

        /**
         * Converts a legacy block-particle argument (e.g. {@code minecraft:snow}
         * or {@code minecraft:stone[type=top]}) to the 26.3 block_state form.
         */
        public static String legacyBlockParticle(String type, String blockPart) {
            blockPart = blockPart.trim();

            String blockName = blockPart;
            String properties = "";
            int start = blockPart.indexOf('[');
            int end = start >= 0 ? blockPart.indexOf(']', start) : -1;
            if (start >= 0 && end > start) {
                blockName = blockPart.substring(0, start).trim();
                properties = blockPart.substring(start + 1, end);
            }

            StringBuilder out = new StringBuilder(type)
                    .append("{block_state:{id:\"")
                    .append(blockName)
                    .append('"');

            if (!properties.isBlank()) {
                out.append(",properties:{");
                boolean first = true;
                for (String property : splitProperties(properties)) {
                    int equals = property.indexOf('=');
                    if (equals <= 0)
                        continue;
                    if (!first)
                        out.append(',');
                    first = false;
                    out.append('"')
                            .append(escapeJson(property.substring(0, equals).trim()))
                            .append("\":\"")
                            .append(escapeJson(unquote(property.substring(equals + 1).trim())))
                            .append('"');
                }
                out.append('}');
            }

            return out.append("}}").toString();
        }

        /** Splits command-style property pairs, respecting quoted values. */
        private static List<String> splitProperties(String properties) {
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean quoted = false;
            boolean escaped = false;

            for (int i = 0; i < properties.length(); i++) {
                char c = properties.charAt(i);
                if (escaped) {
                    current.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    current.append(c);
                    escaped = true;
                } else if (c == '"') {
                    quoted = !quoted;
                    current.append(c);
                } else if (c == ',' && !quoted) {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }

            result.add(current.toString());
            return result;
        }

        private static String unquote(String value) {
            if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
                return value.substring(1, value.length() - 1);
            return value;
        }

        private static String escapeJson(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    /** 26.3 particle argument format. */
    public static final class Particles {

        private Particles() {
        }

        /** Normalizes legacy "id {options}" to 26.3's "id{options}". */
        public static String normalizeOptions(String id) {
            id = id.trim();
            return id.replaceFirst("^([^\\s{]+)\\s+\\{", "$1{");
        }
    }

}
