package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.Projectile;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.utils.RenderUtils;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


/**
 * Covers all item-model projectiles that use {@link ThrownItemRenderer}:
 * snowball, egg, ender pearl, experience bottle, splash/lingering potion,
 * large/small fireball.
 *
 * The vanilla pose contains the renderer's scale plus a camera-facing rotation.
 * The scale is intentionally preserved (it is part of the vanilla projectile's
 * look), but the camera rotation is undone so custom models do not billboard by
 * default. Authors can opt into billboarding with the Camera parent type.
 */
@Mixin(ThrownItemRenderer.class)
public class ThrownItemRendererMixin {

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ThrownItemRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void figura$renderCustomItemProjectile(ItemStackRenderState itemState, PoseStack poseStack,
                                                   SubmitNodeCollector collector, int light, int overlay, int outlineColor,
                                                   Operation<Void> original,
                                                   @Local(argsOnly = true) ThrownItemRenderState state,
                                                   @Local(argsOnly = true) CameraRenderState cameraRenderState) {
        Entity entity = RenderUtils.getEntity(state);
        if (!(entity instanceof Projectile projectile)) {
            original.call(itemState, poseStack, collector, light, overlay, outlineColor);
            return;
        }

        PartFilterScheme scheme = figura$scheme(entity.getType());
        if (scheme == null) {
            original.call(itemState, poseStack, collector, light, overlay, outlineColor);
            return;
        }

        Avatar avatar = RenderUtils.getRenderAvatar(projectile.getOwner());
        if (avatar == null) {
            original.call(itemState, poseStack, collector, light, overlay, outlineColor);
            return;
        }

        float tickDelta = RenderUtils.getTickDelta(state);
        String event = RenderUtils.projectileEvent(scheme);
        boolean cancelled = event != null && avatar.projectileRenderEvent(event, tickDelta, EntityAPI.wrap(entity));

        poseStack.pushPose();
        boolean rendered;
        try {
            poseStack.rotate(new Quaternionf(cameraRenderState.orientation).conjugate());
            rendered = avatar.renderSpecialPart(scheme, poseStack, collector, tickDelta,
                    state.lightCoords, state.outlineColor);
        } finally {
            poseStack.popPose();
        }

        if (!(cancelled || rendered))
            original.call(itemState, poseStack, collector, light, overlay, outlineColor);
    }

    private static PartFilterScheme figura$scheme(EntityType<?> type) {
        if (type == EntityTypes.SNOWBALL)
            return PartFilterScheme.SNOWBALL;
        if (type == EntityTypes.EGG)
            return PartFilterScheme.EGG;
        if (type == EntityTypes.ENDER_PEARL)
            return PartFilterScheme.ENDER_PEARL;
        if (type == EntityTypes.EXPERIENCE_BOTTLE)
            return PartFilterScheme.EXPERIENCE_BOTTLE;
        if (type == EntityTypes.SPLASH_POTION)
            return PartFilterScheme.SPLASH_POTION;
        if (type == EntityTypes.LINGERING_POTION)
            return PartFilterScheme.LINGERING_POTION;
        if (type == EntityTypes.FIREBALL)
            return PartFilterScheme.FIREBALL;
        if (type == EntityTypes.SMALL_FIREBALL)
            return PartFilterScheme.SMALL_FIREBALL;
        return null;
    }
}
