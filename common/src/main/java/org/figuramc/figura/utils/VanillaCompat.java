package org.figuramc.figura.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    /** Legacy vanilla texture paths used by older avatars. */
    public static final class Textures {

        private static final byte[] WHITE_64X32 = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAEAAAAAgCAYAAACinX6EAAAAPElEQVR42u3QAQEAAAgCIP+f1h8FE0ifiwABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIEHDfACHE4dJh9HFRAAAAAElFTkSuQmCC");

        private static final Map<Identifier, Identifier> RESOURCE_CACHE = new ConcurrentHashMap<>();
        private static volatile ResourceManager cachedManager;

        private Textures() {
        }

        /** 26.3 resource path for a legacy for_modeling texture name, or null. */
        public static String modelTexturePath(String name) {
            String n = name.endsWith(".png") ? name.substring(0, name.length() - 4) : name;
            return switch (n) {
                case "leather_layer_1" -> "textures/entity/equipment/humanoid/leather.png";
                case "leather_layer_1_overlay" -> "textures/entity/equipment/humanoid/leather_overlay.png";
                case "leather_layer_2" -> "textures/entity/equipment/humanoid_leggings/leather.png";
                case "leather_layer_2_overlay" -> "textures/entity/equipment/humanoid_leggings/leather_overlay.png";
                case "oak_log" -> "textures/block/oak_log.png";
                case "oak_log_top" -> "textures/block/oak_log_top.png";
                // Approximate only: the original for_modeling/oak.png may be a
                // custom 64x32 atlas, not a vanilla 16x16 block texture.
                case "oak" -> "textures/block/oak_planks.png";
                case "water_flow" -> "textures/block/water_flow.png";
                case "water_still" -> "textures/block/water_still.png";
                case "beacon_beam" -> "textures/entity/beacon/beacon_beam.png";
                case "snow_fox" -> "textures/entity/fox/snow.png";
                case "sunflower_front" -> "textures/block/sunflower_front.png";
                default -> null;
            };
        }

        /** Bytes for a legacy model texture, or null if there is no fallback. */
        public static byte[] modelTexture(String name) {
            String n = name.endsWith(".png") ? name.substring(0, name.length() - 4) : name;
            // Approximate only: the original white.png may be a custom atlas.
            if (n.equals("white"))
                return WHITE_64X32;

            String path = modelTexturePath(name);
            if (path == null)
                return null;

            try {
                return ResourceUtils.getResource(
                        Minecraft.getInstance().getResourceManager(),
                        Identifier.withDefaultNamespace(path)
                );
            } catch (Throwable ignored) {
                return null;
            }
        }

        /** Maps a runtime RESOURCE id to a 26.3 vanilla path when the old one is absent. */
        public static Identifier resolveResource(Identifier id) {
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            if (manager == null)
                return id;

            // Resource reloads replace the manager; do not keep aliases from the
            // previous generation, otherwise a pack that adds/removes a texture
            // would keep resolving through stale state.
            if (manager != cachedManager) {
                synchronized (Textures.class) {
                    if (manager != cachedManager) {
                        RESOURCE_CACHE.clear();
                        cachedManager = manager;
                    }
                }
            }

            return RESOURCE_CACHE.computeIfAbsent(id, key -> resolveUncached(key, manager));
        }

        private static Identifier resolveUncached(Identifier id, ResourceManager manager) {
            try {
                if (manager.getResource(id).isPresent())
                    return id;

                String alias = legacyResourcePath(id.getPath());
                if (alias != null) {
                    Identifier candidate = Identifier.fromNamespaceAndPath(id.getNamespace(), alias);
                    if (manager.getResource(candidate).isPresent())
                        return candidate;
                }
            } catch (Throwable ignored) {
            }
            return id;
        }

        // package-private for unit tests
        static String legacyResourcePath(String path) {
            // Rabbit textures gained a rabbit_ prefix in 26.3.
            if (path.startsWith("textures/entity/rabbit/") && path.endsWith(".png")) {
                String file = path.substring("textures/entity/rabbit/".length());
                if (!file.startsWith("rabbit_"))
                    return "textures/entity/rabbit/rabbit_" + file;
            }

            // Hanging-sign entity textures became block textures.
            if (path.startsWith("textures/entity/signs/hanging/") && path.endsWith(".png")) {
                String wood = path.substring("textures/entity/signs/hanging/".length(), path.length() - 4);
                return "textures/block/" + wood + "_hanging_sign.png";
            }

            // Armor layers moved under entity/equipment in 1.21.2+.
            if (path.startsWith("textures/models/armor/") && path.endsWith(".png")) {
                String file = path.substring("textures/models/armor/".length(), path.length() - 4);
                boolean overlay = file.endsWith("_overlay");
                if (overlay)
                    file = file.substring(0, file.length() - "_overlay".length());

                String material;
                String folder;
                if (file.endsWith("_layer_1")) {
                    material = file.substring(0, file.length() - "_layer_1".length());
                    folder = "humanoid";
                } else if (file.endsWith("_layer_2")) {
                    material = file.substring(0, file.length() - "_layer_2".length());
                    folder = "humanoid_leggings";
                } else {
                    return null;
                }

                return "textures/entity/equipment/" + folder + "/" + material + (overlay ? "_overlay" : "") + ".png";
            }

            // The beacon beam moved into the beacon subfolder.
            if (path.equals("textures/entity/beacon_beam.png"))
                return "textures/entity/beacon/beacon_beam.png";

            return null;
        }
    }

    /** Client-side entity compatibility for EntityTask. */
    public static final class Entities {

        private static final AtomicInteger FAKE_ENTITY_ID = new AtomicInteger(-1);

        private Entities() {
        }

        /** EntityTask entities are not in a level and need a non-zero ID on 26.3. */
        public static int nextFakeEntityId() {
            return FAKE_ENTITY_ID.getAndDecrement();
        }

        /** Adds the 26.3 block-attached fields when they are missing. */
        public static void addBlockAttachedDefaults(CompoundTag nbt) {
            if (!nbt.contains("block_pos"))
                nbt.put("block_pos", new IntArrayTag(new int[]{0, 0, 0}));
            if (!nbt.contains("facing"))
                nbt.putByte("facing", (byte) Direction.SOUTH.get2DDataValue());
        }
    }
}
