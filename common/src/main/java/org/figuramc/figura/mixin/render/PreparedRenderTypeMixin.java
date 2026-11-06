package org.figuramc.figura.mixin.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.utils.HudRenderContext;
import org.figuramc.figura.utils.HudRenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PreparedRenderType.class)
public class PreparedRenderTypeMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, index = 2)
    private static RenderPipeline figura$hudTextNoDepth(RenderPipeline pipeline) {
        if (!HudRenderContext.isTextPass())
            return pipeline;

        String path = pipeline.getLocation().getPath();
        if (!path.contains("text"))
            return pipeline;

        try {
            return HudRenderPipelines.noDepth(pipeline);
        } catch (Throwable throwable) {
            FiguraMod.LOGGER.error("[HUD] failed to create no-depth text pipeline, falling back", throwable);
            return pipeline;
        }
    }
}
