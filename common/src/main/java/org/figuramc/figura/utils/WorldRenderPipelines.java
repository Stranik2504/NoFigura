package org.figuramc.figura.utils;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
  * Pipeline variants for fullbright world-avatar materials.
  *
  * <p>A fullbright part ({@code setLight(15, 15)}) is by definition unlit. 26.3's
  * entity pipelines enable {@code PER_FACE_LIGHTING}, which computes separate
  * {@code light}/{@code -light} vertex colors for front and back and makes the two
  * sides of a single flat quad differ. For an unlit surface that distinction is
  * meaningless and wrong.</p>
  *
  * <p>Clone the base pipeline with everything preserved (cull, depth, color target,
  * alpha cutout, bind groups, vertex format, shaders) but replace the lighting
  * defines with 26.3's {@code EMISSIVE}  {@code NO_CARDINAL_LIGHTING} and drop
  * {@code PER_FACE_LIGHTING}. Lit (non-fullbright) parts keep their original 26.3
  * per-face lighting pipeline.</p>
  */
public final class WorldRenderPipelines {

    private static final Map<RenderPipeline, RenderPipeline> UNLIT_CACHE = new ConcurrentHashMap<>();
    private static final Map<RenderPipeline, RenderPipeline> FLAT_CACHE = new ConcurrentHashMap<>();

    private WorldRenderPipelines() {
    }

    public static RenderPipeline unlit(RenderPipeline base) {
        return variant(base, true);
    }

    /**
      * Removes only PER_FACE_LIGHTING, keeping all lighting/lightmap/alpha/depth
      * state. Used for single-face flat parts: both sides then use the same
      * normal-based vertex color, matching 1.21.4, without forcing fullbright.
      */
    public static RenderPipeline withoutPerFaceLighting(RenderPipeline base) {
        return variant(base, false);
    }

    private static RenderPipeline variant(RenderPipeline base, boolean unlit) {
        if (base == null)
            return null;

        boolean hasPerFace = false;
        for (String flag : base.getShaderDefines().flags()) {
            if ("PER_FACE_LIGHTING".equals(flag)) {
                hasPerFace = true;
                break;
            }
        }

        if (!hasPerFace)
            return base;

        Map<RenderPipeline, RenderPipeline> cache = unlit ? UNLIT_CACHE : FLAT_CACHE;
        return cache.computeIfAbsent(base, pipeline -> create(pipeline, unlit));
    }

    private static RenderPipeline create(RenderPipeline base, boolean unlit) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation((unlit ? "figura/fullbright_" : "figura/flat_lighting_") + base.getSortKey())
                .withPolygonMode(base.getPolygonMode())
                .withCull(base.isCull())
                .withPushConstantSize(base.pushConstantSize())
                .withPrimitiveTopology(base.getPrimitiveTopology());

        if (base.getDepthStencilState() != null)
            builder.withDepthStencilState(base.getDepthStencilState());

        for (Map.Entry<ShaderType, net.minecraft.resources.Identifier> shader : base.getShaders().entrySet()) {
            if (shader.getKey() == ShaderType.FRAGMENT)
                builder.withFragmentShader(shader.getValue());
            else if (shader.getKey() == ShaderType.VERTEX)
                builder.withVertexShader(shader.getValue());
        }

        for (Map.Entry<String, String> define : base.getShaderDefines().values().entrySet()) {
            String value = define.getValue();

            if (value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0)
                builder.withShaderDefine(define.getKey(), Float.parseFloat(value));
            else
                builder.withShaderDefine(define.getKey(), Integer.parseInt(value));
        }

        for (String flag : base.getShaderDefines().flags()) {
            if (!"PER_FACE_LIGHTING".equals(flag))
                builder.withShaderDefine(flag);
        }

        if (unlit) {
            builder.withShaderDefine("EMISSIVE");
            builder.withShaderDefine("NO_CARDINAL_LIGHTING");
        }

        var colorTargets = base.getColorTargetStates();
        for (int i = 0; i < colorTargets.size(); i++)
            builder.withColorTargetState(i, colorTargets.get(i));

        for (var layout : base.getBindGroupLayouts())
            builder.withBindGroupLayout(layout);

        var vertexBindings = base.getVertexFormatBindings();
        for (int i = 0; i < vertexBindings.size(); i++) {
            var vertexFormat = vertexBindings.get(i);

            if (vertexFormat != null)
                builder.withVertexBinding(i, vertexFormat);
        }

        return builder.build();
    }
}
