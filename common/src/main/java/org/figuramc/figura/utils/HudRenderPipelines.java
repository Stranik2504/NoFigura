package org.figuramc.figura.utils;

import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
  * Pipeline helpers for HUD overlay draws that must not be occluded by the HUD
  * depth buffer. Only used for text in the compatibility pass; cloning every
  * pipeline (as an earlier experiment did) is too expensive.
  */
public final class HudRenderPipelines {

    private static final Map<RenderPipeline, RenderPipeline> NO_DEPTH_CACHE = new ConcurrentHashMap<>();
    private static final Map<RenderPipeline, RenderPipeline> NO_CULL_CACHE = new ConcurrentHashMap<>();

    private HudRenderPipelines() {
    }

    public static RenderPipeline noDepth(RenderPipeline base) {
        if (base == null)
                return null;

        DepthStencilState depth = base.getDepthStencilState();
        if (depth == null || (depth.depthTest() == CompareOp.ALWAYS_PASS && !depth.writeDepth()))
            return base;

        return NO_DEPTH_CACHE.computeIfAbsent(base, HudRenderPipelines::createNoDepth);
    }

    public static RenderPipeline noCull(RenderPipeline base) {
        if (base == null)
            return null;

        if (!base.isCull())
            return base;

        return NO_CULL_CACHE.computeIfAbsent(base, pipeline -> create(pipeline, false));
    }

    private static RenderPipeline createNoDepth(RenderPipeline base) {
        return create(base, true);
    }

    private static RenderPipeline create(RenderPipeline base, boolean disableDepth) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation("figura/hud_" + (disableDepth ? "no_depth_" : "no_cull_") + base.getSortKey())
            .withPolygonMode(base.getPolygonMode())
            .withCull(false)
            .withPushConstantSize(base.pushConstantSize())
            .withPrimitiveTopology(base.getPrimitiveTopology());

        if (disableDepth)
            builder.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false));
        else if (base.getDepthStencilState() != null)
            builder.withDepthStencilState(base.getDepthStencilState());

        for (Map.Entry<ShaderType, net.minecraft.resources.Identifier> shader : base.getShaders().entrySet()) {
            if (shader.getKey() == ShaderType.FRAGMENT)
                builder.withFragmentShader(shader.getValue());
            else if (shader.getKey() == ShaderType.VERTEX)
                builder.withVertexShader(shader.getValue());
        }

        for (Map.Entry<String, String> define : base.getShaderDefines().values().entrySet()) {
            // Shader define values may be ints or floats (e.g. ALPHA_CUTOUT 0.1).
            String value = define.getValue();

            if (value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0)
                builder.withShaderDefine(define.getKey(), Float.parseFloat(value));
            else
                builder.withShaderDefine(define.getKey(), Integer.parseInt(value));
        }

        for (String flag : base.getShaderDefines().flags())
            builder.withShaderDefine(flag);

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