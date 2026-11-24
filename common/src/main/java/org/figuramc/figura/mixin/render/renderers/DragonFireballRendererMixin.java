package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DragonFireballRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.utils.RenderUtils;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonFireballRenderer.class)
public class DragonFireballRendererMixin {

    @Unique
    private boolean figura$dragonFireballReplaced;

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void figura$resetDragonFireball(EntityRenderState state, PoseStack poseStack,
                                            SubmitNodeCollector collector, CameraRenderState cameraRenderState,
                                            CallbackInfo ci) {
        figura$dragonFireballReplaced = false;
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V",
                    ordinal = 0
            )
    )
    private void figura$renderCustomDragonFireball(SubmitNodeCollector collector, PoseStack poseStack,
                                                   RenderType renderType,
                                                   SubmitNodeCollector.CustomGeometryRenderer geometry,
                                                   Operation<Void> original,
                                                   @Local(argsOnly = true) EntityRenderState state,
                                                   @Local(argsOnly = true) CameraRenderState cameraRenderState) {
        Entity entity = RenderUtils.getEntity(state);
        if (!(entity instanceof DragonFireball fireball)) {
            original.call(collector, poseStack, renderType, geometry);
            return;
        }

        Avatar avatar = RenderUtils.getRenderAvatar(fireball.getOwner());
        if (avatar == null) {
            original.call(collector, poseStack, renderType, geometry);
            return;
        }

        float tickDelta = RenderUtils.getTickDelta(state);
        boolean cancelled = avatar.projectileRenderEvent("DRAGON_FIREBALL_RENDER", tickDelta, EntityAPI.wrap(entity));

        poseStack.pushPose();
        boolean rendered;
        try {
            poseStack.rotate(new Quaternionf(cameraRenderState.orientation).conjugate());
            rendered = avatar.renderSpecialPart(PartFilterScheme.DRAGON_FIREBALL, poseStack, collector,
                    tickDelta, state.lightCoords, state.outlineColor);
        } finally {
            poseStack.popPose();
        }

        if (cancelled || rendered)
            figura$dragonFireballReplaced = true;
        else
            original.call(collector, poseStack, renderType, geometry);
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V",
                    ordinal = 1
            )
    )
    private void figura$skipDragonFireballOutline(SubmitNodeCollector collector, PoseStack poseStack,
                                                  RenderType renderType,
                                                  SubmitNodeCollector.CustomGeometryRenderer geometry,
                                                  Operation<Void> original) {
        if (!figura$dragonFireballReplaced)
            original.call(collector, poseStack, renderType, geometry);
    }
}
