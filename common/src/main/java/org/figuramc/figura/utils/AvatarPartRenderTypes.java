package org.figuramc.figura.utils;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.figuramc.figura.model.PartCustomization;
import org.figuramc.figura.model.rendering.texture.FiguraRenderTypes;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World-only material variants for avatar parts.
 *
 * <p>Fullbright parts ({@code customization.light == FULL_BRIGHT}) are unlit, so
 * they use 26.3's {@code EMISSIVE} + {@code NO_CARDINAL_LIGHTING} pipeline variant
 * (see {@link WorldRenderPipelines#unlit}). Single-face flat parts (halo/ring)
 * that are not fullbright use {@link WorldRenderPipelines#withoutPerFaceLighting}:
 * only {@code PER_FACE_LIGHTING} is removed, so both sides use the same
 * normal-based vertex color while the lightmap/light is preserved. Three-dimensional
 * parts keep their original 26.3 per-face lighting.</p>
 *
 * <p>The opaque cutout variants keep the base pipeline's color target, depth
 * state, cull and {@code ALPHA_CUTOUT}, so clouds/background cannot show through
 * the ring and the depth/occlusion behavior is unchanged. Translucent variants
 * keep the base blend/sort/depth.</p>
 */
public final class AvatarPartRenderTypes {

    private static final Map<Identifier, RenderType> CUTOUT = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> CUTOUT_CULL = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> TRANSLUCENT = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> TRANSLUCENT_CULL = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> FLAT_CUTOUT = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> FLAT_CUTOUT_CULL = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> FLAT_TRANSLUCENT = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> FLAT_TRANSLUCENT_CULL = new ConcurrentHashMap<>();

    /** Identity set of every RenderType created here, used to keep them out of the emissive-last ordering. */
    private static final Set<RenderType> VARIANTS = Collections.newSetFromMap(new IdentityHashMap<>());

    private AvatarPartRenderTypes() {
    }

    public static boolean isVariant(RenderType type) {
        return type != null && VARIANTS.contains(type);
    }

    public static boolean isFullbright(PartCustomization customization) {
        return customization != null && customization.light != null
                && customization.light == LightCoordsUtil.FULL_BRIGHT;
    }

    public static RenderType get(FiguraRenderTypes type, Identifier texture) {
        if (type == FiguraRenderTypes.CUTOUT)
            return CUTOUT.computeIfAbsent(texture, AvatarPartRenderTypes::createCutout);
        if (type == FiguraRenderTypes.CUTOUT_CULL)
            return CUTOUT_CULL.computeIfAbsent(texture, AvatarPartRenderTypes::createCutoutCull);
        if (type == FiguraRenderTypes.TRANSLUCENT)
            return TRANSLUCENT.computeIfAbsent(texture, AvatarPartRenderTypes::createTranslucent);
        if (type == FiguraRenderTypes.TRANSLUCENT_CULL)
            return TRANSLUCENT_CULL.computeIfAbsent(texture, AvatarPartRenderTypes::createTranslucentCull);
        return null;
    }

    /** Variant that only removes PER_FACE_LIGHTING; used for single-face flat parts. */
    public static RenderType getFlat(FiguraRenderTypes type, Identifier texture) {
        if (type == FiguraRenderTypes.CUTOUT)
            return FLAT_CUTOUT.computeIfAbsent(texture, AvatarPartRenderTypes::createFlatCutout);
        if (type == FiguraRenderTypes.CUTOUT_CULL)
            return FLAT_CUTOUT_CULL.computeIfAbsent(texture, AvatarPartRenderTypes::createFlatCutoutCull);
        if (type == FiguraRenderTypes.TRANSLUCENT)
            return FLAT_TRANSLUCENT.computeIfAbsent(texture, AvatarPartRenderTypes::createFlatTranslucent);
        if (type == FiguraRenderTypes.TRANSLUCENT_CULL)
            return FLAT_TRANSLUCENT_CULL.computeIfAbsent(texture, AvatarPartRenderTypes::createFlatTranslucentCull);
        return null;
    }

    private static RenderType createFlatCutout(Identifier texture) {
        return createFlat("figura_flat_cutout", RenderPipelines.ENTITY_CUTOUT, texture, false);
    }

    private static RenderType createFlatCutoutCull(Identifier texture) {
        return createFlat("figura_flat_cutout_cull", RenderPipelines.ENTITY_CUTOUT_CULL, texture, false);
    }

    private static RenderType createFlatTranslucent(Identifier texture) {
        return createFlat("figura_flat_translucent", RenderPipelines.ENTITY_TRANSLUCENT, texture, true);
    }

    private static RenderType createFlatTranslucentCull(Identifier texture) {
        return createFlat("figura_flat_translucent_cull", RenderPipelines.ENTITY_TRANSLUCENT_CULL, texture, true);
    }

    private static RenderType createFlat(String name, RenderPipeline base, Identifier texture, boolean sort) {
        var builder = RenderSetup.builder(WorldRenderPipelines.withoutPerFaceLighting(base))
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        if (sort)
            builder.sortOnUpload();
        RenderType type = RenderType.create(name, builder.createRenderSetup());
        VARIANTS.add(type);
        return type;
    }

    private static RenderType createCutout(Identifier texture) {
        return create("figura_fullbright_cutout", RenderPipelines.ENTITY_CUTOUT, texture, false);
    }

    private static RenderType createCutoutCull(Identifier texture) {
        return create("figura_fullbright_cutout_cull", RenderPipelines.ENTITY_CUTOUT_CULL, texture, false);
    }

    private static RenderType createTranslucent(Identifier texture) {
        return create("figura_fullbright_translucent", RenderPipelines.ENTITY_TRANSLUCENT, texture, true);
    }

    private static RenderType createTranslucentCull(Identifier texture) {
        return create("figura_fullbright_translucent_cull", RenderPipelines.ENTITY_TRANSLUCENT_CULL, texture, true);
    }

    private static RenderType create(String name, RenderPipeline base, Identifier texture, boolean sort) {
        var builder = RenderSetup.builder(WorldRenderPipelines.unlit(base))
                .withTexture("Sampler0", texture)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        if (sort)
            builder.sortOnUpload();
        RenderType type = RenderType.create(name, builder.createRenderSetup());
        VARIANTS.add(type);
        return type;
    }
}
