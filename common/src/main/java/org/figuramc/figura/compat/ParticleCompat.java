package org.figuramc.figura.compat;

/** 26.3 particle-argument compatibility. */
public final class ParticleCompat {

    private ParticleCompat() {
    }

    /** Normalizes legacy "id {options}" to 26.3's "id{options}". */
    public static String normalizeOptions(String id) {
        id = id.trim();
        return id.replaceFirst("^([^\\s{]+)\\s+\\{", "$1{");
    }

    public static String convertLegacy(String id) {
        String trimmed = id.trim();

        // entity_effect with no options keeps its original special case.
        if (trimmed.contains("entity_effect") && !trimmed.contains("{"))
            return trimmed + "{color:[0.0,0.0,0.0,1.0]}";

        int space = trimmed.indexOf(' ');
        if (space <= 0)
            return id;

        String rawType = trimmed.substring(0, space);
        String type = rawType.startsWith("minecraft:") ? rawType.substring("minecraft:".length()) : rawType;
        String args = trimmed.substring(space + 1).trim();
        if (args.isEmpty())
            return id;

        return switch (type) {
            case "block" -> BlockStateCompat.legacyBlockParticle("block", args);
            case "block_marker" -> BlockStateCompat.legacyBlockParticle("block_marker", args);
            case "falling_dust" -> BlockStateCompat.legacyBlockParticle("falling_dust", args);
            case "dust" -> {
                String[] parts = args.split("\\s+");
                if (parts.length < 4)
                    yield id;
                yield "dust{color:[" + decimal(parts[0]) + "," + decimal(parts[1]) + "," + decimal(parts[2]) + "],scale:" + parts[3] + "}";
            }
            case "dust_color_transition" -> {
                String[] parts = args.split("\\s+");
                if (parts.length < 7)
                    yield id;
                yield "dust_color_transition{from_color:[" + decimal(parts[0]) + "," + decimal(parts[1]) + "," + decimal(parts[2]) + "],scale:" + parts[3] + ",to_color:[" + decimal(parts[4]) + "," + decimal(parts[5]) + "," + decimal(parts[6]) + "]}";
            }
            case "item" -> {
                String[] parts = args.split("\\s+");
                if (parts.length < 1)
                    yield id;
                yield "item{item:{id:" + parts[0] + "}}";
            }
            case "sculk_charge" -> {
                String[] parts = args.split("\\s+");
                if (parts.length < 1)
                    yield id;
                yield "sculk_charge{roll:" + parts[0] + "}";
            }
            case "shriek" -> {
                String[] parts = args.split("\\s+");
                if (parts.length < 1)
                    yield id;
                yield "shriek{delay:" + parts[0] + "}";
            }
            case "vibration" -> {
                String[] parts = args.split("\\s+");
                if (parts.length < 4)
                    yield id;
                yield "vibration{arrival_in_ticks:" + parts[3] + ",destination:{type:block,pos:[" + parts[0] + "," + parts[1] + "," + parts[2] + "]}}";
            }
            default -> id;
        };
    }

    private static String decimal(String value) {
        return value.contains(".") ? value : value + ".0";
    }}
