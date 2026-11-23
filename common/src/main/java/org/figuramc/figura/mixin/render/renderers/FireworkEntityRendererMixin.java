package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FireworkEntityRenderer;
import net.minecraft.client.renderer.entity.state.FireworkRocketRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.utils.RenderUtils;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkEntityRenderer.class)
public class FireworkEntityRendererMixin {

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/FireworkRocketRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void figura$renderCustomFirework(ItemStackRenderState itemState, PoseStack poseStack,
                                             SubmitNodeCollector collector, int light, int overlay, int outlineColor,
                                             Operation<Void> original,
                                             @Local(argsOnly = true) FireworkRocketRenderState state,
                                             @Local(argsOnly = true) CameraRenderState cameraRenderState) {
        Entity entity = RenderUtils.getEntity(state);
        if (!(entity instanceof FireworkRocketEntity firework)) {
            original.call(itemState, poseStack, collector, light, overlay, outlineColor);
            return;
        }

        Avatar avatar = RenderUtils.getRenderAvatar(firework.getOwner());
        if (avatar == null) {
            original.call(itemState, poseStack, collector, light, overlay, outlineColor);
            return;
        }

        float tickDelta = RenderUtils.getTickDelta(state);
        boolean cancelled = avatar.projectileRenderEvent("FIREWORK_ROCKET_RENDER", tickDelta, EntityAPI.wrap(entity));

        poseStack.pushPose();
        boolean rendered;
        try {
            poseStack.rotate(new Quaternionf(cameraRenderState.orientation).conjugate());
            rendered = avatar.renderSpecialPart(PartFilterScheme.FIREWORK_ROCKET, poseStack, collector,
                    tickDelta, state.lightCoords, state.outlineColor);
        } finally {
            poseStack.popPose();
        }

        if (!(cancelled || rendered))
            original.call(itemState, poseStack, collector, light, overlay, outlineColor);
    }
}
