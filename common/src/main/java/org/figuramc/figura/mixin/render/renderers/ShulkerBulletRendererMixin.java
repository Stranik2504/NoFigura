package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ShulkerBulletRenderer;
import net.minecraft.client.renderer.entity.state.ShulkerBulletRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBulletRenderer.class)
public class ShulkerBulletRendererMixin {

    @Unique
    private boolean figura$shulkerBulletReplaced;

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ShulkerBulletRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void figura$resetShulkerBullet(ShulkerBulletRenderState state, PoseStack poseStack,
                                           SubmitNodeCollector collector, CameraRenderState cameraRenderState,
                                           CallbackInfo ci) {
        figura$shulkerBulletReplaced = false;
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ShulkerBulletRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;III)V"
            )
    )
    private void figura$renderCustomShulkerBullet(SubmitNodeCollector collector, Model<?> model, Object modelState,
                                                  PoseStack poseStack, Identifier texture, int light, int overlay,
                                                  int outlineColor, Operation<Void> original,
                                                  @Local(argsOnly = true) ShulkerBulletRenderState state) {
        Entity entity = RenderUtils.getEntity(state);
        if (!(entity instanceof ShulkerBullet bullet)) {
            original.call(collector, model, modelState, poseStack, texture, light, overlay, outlineColor);
            return;
        }

        Avatar avatar = RenderUtils.getRenderAvatar(bullet.getOwner());
        if (avatar == null) {
            original.call(collector, model, modelState, poseStack, texture, light, overlay, outlineColor);
            return;
        }

        float tickDelta = RenderUtils.getTickDelta(state);
        boolean cancelled = avatar.projectileRenderEvent("SHULKER_BULLET_RENDER", tickDelta, EntityAPI.wrap(entity));
        boolean rendered = avatar.renderSpecialPart(PartFilterScheme.SHULKER_BULLET, poseStack, collector,
                tickDelta, state.lightCoords, state.outlineColor);

        if (cancelled || rendered)
            figura$shulkerBulletReplaced = true;
        else
            original.call(collector, model, modelState, poseStack, texture, light, overlay, outlineColor);
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ShulkerBulletRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"
            )
    )
    private void figura$skipShulkerBulletGlow(OrderedSubmitNodeCollector collector, Model<?> model, Object modelState,
                                              PoseStack poseStack, RenderType renderType, int light, int overlay,
                                              int color, UvMapping uvMapping, int outlineColor, Operation<Void> original) {
        if (!figura$shulkerBulletReplaced)
            original.call(collector, model, modelState, poseStack, renderType, light, overlay, color, uvMapping, outlineColor);
    }
}
