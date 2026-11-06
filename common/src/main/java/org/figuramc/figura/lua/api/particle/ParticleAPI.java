package org.figuramc.figura.lua.api.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.ParticleEngineAccessor;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.world.WorldAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.VanillaCompat;
import org.figuramc.figura.utils.LuaUtils;
import org.luaj.vm2.LuaError;


@LuaWhitelist
@LuaTypeDoc(
        name = "ParticleAPI",
        value = "particles"
)
public class ParticleAPI {

    private final Avatar owner;

    public ParticleAPI(Avatar owner) {
        this.owner = owner;
    }

    public static ParticleEngineAccessor getParticleEngine() {
        return (ParticleEngineAccessor) Minecraft.getInstance().particleEngine;
    }

    private LuaParticle generate(String id, double x, double y, double z, double w, double t, double h) {
        try {
            // Some old avatars still pass "id {options}" (with a space). 26.3's
            // ParticleArgument expects "id{options}"; without this the options are
            // ignored and codecs like flash report "No key color in MapLike[{}]".
            id = VanillaCompat.Particles.normalizeOptions(id);
            id = convertOldToNewParticleFormat(id);
            ParticleOptions options = ParticleArgument.readParticle(new StringReader(id), WorldAPI.getCurrentWorld().registryAccess());
            Particle p = getParticleEngine().figura$makeParticle(options, x, y, z, w, t, h);
            if (p == null) throw new LuaError("Could not parse particle \"" + id + "\"");
            return new LuaParticle(id, p, owner);
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    // package-private for unit tests
    static String convertOldToNewParticleFormat(String id) {
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
            case "block" -> VanillaCompat.Blocks.legacyBlockParticle("block", args);
            case "block_marker" -> VanillaCompat.Blocks.legacyBlockParticle("block_marker", args);
            case "falling_dust" -> VanillaCompat.Blocks.legacyBlockParticle("falling_dust", args);
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
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {String.class, FiguraVec3.class},
                            argumentNames = {"name", "pos"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, FiguraVec3.class, FiguraVec3.class},
                            argumentNames = {"name", "pos", "vel"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Double.class, Double.class, Double.class},
                            argumentNames = {"name", "posX", "posY", "posZ"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, FiguraVec3.class, Double.class, Double.class, Double.class},
                            argumentNames = {"name", "pos", "velX", "velY", "velZ"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Double.class, Double.class, Double.class, FiguraVec3.class},
                            argumentNames = {"name", "posX", "posY", "posZ", "vel"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"name", "posX", "posY", "posZ", "velX", "velY", "velZ"}
                    )
            },
            value = "particles.new_particle"
    )
    public LuaParticle newParticle(@LuaNotNil String id, Object x, Object y, Double z, Object w, Double t, Double h) {
        FiguraVec3 pos, vel;

        // Parse pos and vel
        Pair<FiguraVec3, FiguraVec3> pair = LuaUtils.parse2Vec3("newParticle", x, y, z, w, t, h, 2);
        pos = pair.getFirst();
        vel = pair.getSecond();

        LuaParticle particle = generate(id, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
        particle.spawn();
        return particle;
    }

    @LuaWhitelist
    @LuaMethodDoc("particles.remove_particles")
    public ParticleAPI removeParticles() {
        getParticleEngine().figura$clearParticles(owner.owner);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "id"
            ),
            value = "particles.is_present"
    )
    public boolean isPresent(String id) {
        try {
            id = VanillaCompat.Particles.normalizeOptions(id);
            id = convertOldToNewParticleFormat(id);
            ParticleOptions options = ParticleArgument.readParticle(new StringReader(id), WorldAPI.getCurrentWorld().registryAccess());
            return getParticleEngine().figura$makeParticle(options, 0, 0, 0, 0, 0, 0) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    @LuaWhitelist
    public LuaParticle __index(String id) {
        return generate(id, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public String toString() {
        return "ParticleAPI";
    }
}
