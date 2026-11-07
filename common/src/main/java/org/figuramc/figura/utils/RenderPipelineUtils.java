package org.figuramc.figura.utils;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;

/**
  * Structural pipeline classification helpers.
  *
  * <p>Emissive/glow pipelines use either the entity shader with the
  * {@code EMISSIVE} define (eyes / entity_translucent_emissive) or the
  * dedicated beacon-beam shader pipelines (beacon beams and Figura's
  * cutout/emissive solid material). Both are identified without looking at
  * pipeline names or part names.</p>
  */
public final class RenderPipelineUtils {

    private RenderPipelineUtils() {
    }

    public static boolean isEmissive(RenderPipeline pipeline) {
        if (pipeline == null)
            return false;

        // Beacon-beam pipelines use their own shader program and do not carry the
        // entity EMISSIVE define; identify them by the vanilla pipeline identity.
        if (pipeline == RenderPipelines.BEACON_BEAM_OPAQUE
            || pipeline == RenderPipelines.BEACON_BEAM_TRANSLUCENT)
            return true;

        for (String flag : pipeline.getShaderDefines().flags()) {
            if ("EMISSIVE".equals(flag))
                return true;
        }

        return false;
    }
}