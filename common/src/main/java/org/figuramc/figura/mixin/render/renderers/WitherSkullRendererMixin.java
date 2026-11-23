package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.client.renderer.entity.state.WitherSkullRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherSkullRenderer.class)
public class WitherSkullRendererMixin {

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/WitherSkullRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;III)V"
            )
    )
    private void figura$renderCustomWitherSkull(SubmitNodeCollector collector, Model<?> model, Object modelState,
                                                PoseStack poseStack, Identifier texture, int light, int overlay,
                                                int outlineColor, Operation<Void> original,
                                                @Local(argsOnly = true) WitherSkullRenderState state) {
        Entity entity = RenderUtils.getEntity(state);
        if (!(entity instanceof WitherSkull skull)) {
            original.call(collector, model, modelState, poseStack, texture, light, overlay, outlineColor);
            return;
        }

        Avatar avatar = RenderUtils.getRenderAvatar(skull.getOwner());
        if (avatar == null) {
            original.call(collector, model, modelState, poseStack, texture, light, overlay, outlineColor);
            return;
        }

        float tickDelta = RenderUtils.getTickDelta(state);
        boolean cancelled = avatar.projectileRenderEvent("WITHER_SKULL_RENDER", tickDelta, EntityAPI.wrap(entity));
        boolean rendered = avatar.renderSpecialPart(PartFilterScheme.WITHER_SKULL, poseStack, collector, tickDelta,
                state.lightCoords, state.outlineColor);

        if (!(cancelled || rendered))
            original.call(collector, model, modelState, poseStack, texture, light, overlay, outlineColor);
    }
}
