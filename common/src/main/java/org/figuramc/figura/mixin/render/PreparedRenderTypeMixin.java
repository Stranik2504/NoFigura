package org.figuramc.figura.mixin.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.utils.HudRenderContext;
import org.figuramc.figura.utils.HudRenderPipelines;
import org.figuramc.figura.utils.RenderPipelineUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PreparedRenderType.class)
public class PreparedRenderTypeMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, index = 2)
    private static RenderPipeline figura$hudTextNoDepth(RenderPipeline pipeline) {
        boolean text = HudRenderContext.isTextPass();
        boolean emissiveOverlay = HudRenderContext.isHudPass() && RenderPipelineUtils.isEmissive(pipeline);

        if (!text && !emissiveOverlay)
            return pipeline;

        try {
            return text ? HudRenderPipelines.noDepth(pipeline) : HudRenderPipelines.noCull(pipeline);
        } catch (Throwable throwable) {
            FiguraMod.LOGGER.error("[HUD] failed to create overlay pipeline, falling back", throwable);
            return pipeline;
        }
    }
}
