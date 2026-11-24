package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraEntityRenderStateExtension;
import org.figuramc.figura.permissions.Permissions;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHookRenderer.class)
public class FishingHookRendererMixin {

    /*
     * FishingHookRenderer.submit() submits two custom geometries:
     *   1. the bobber itself (scale 0.5 + camera-facing)
     *   2. the fishing line (RenderTypes.lines())
     *
     * Only replace the first one here. The second one is intentionally left
     * untouched so the vanilla fishing line keeps working.
     */
    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V",
                    ordinal = 0
            )
    )
    private void figura$renderFishingBobber(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
                                            SubmitNodeCollector.CustomGeometryRenderer geometry, Operation<Void> original,
                                            @Local(argsOnly = true) FishingHookRenderState state,
                                            @Local(argsOnly = true) CameraRenderState cameraRenderState) {
        Integer entityId = ((FiguraEntityRenderStateExtension) state).figura$getEntityId();
        if (entityId == null) {
            original.call(collector, poseStack, renderType, geometry);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            original.call(collector, poseStack, renderType, geometry);
            return;
        }

        Entity entity = AvatarManager.ENTITY_CACHE.computeIfAbsent(entityId, id -> minecraft.level.getEntity(id));
        if (!(entity instanceof FishingHook hook)) {
            original.call(collector, poseStack, renderType, geometry);
            return;
        }

        Avatar avatar = AvatarManager.getAvatar(hook.getPlayerOwner());
        if (avatar == null || avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 0) {
            original.call(collector, poseStack, renderType, geometry);
            return;
        }

        float tickDelta = ((FiguraEntityRenderStateExtension) state).figura$getTickDelta();

        /*
         * The vanilla bobber is a camera-facing quad. The pose at this point is
         * already scale(0.5) + camera rotation, which is correct for that quad
         * but NOT for an arbitrary custom model (e.g. a cube). Undo only the
         * camera rotation for the custom part, so it stays in entity/world
         * space by default. Authors who want billboarding can use the Camera
         * parent type on the part (or its children).
         */
        boolean cancelled = avatar.projectileRenderEvent("FISHING_BOBBER_RENDER", tickDelta, EntityAPI.wrap(hook));

        poseStack.pushPose();
        boolean rendered;
        try {
            poseStack.rotate(new Quaternionf(cameraRenderState.orientation).conjugate());
            rendered = avatar.renderFishingBobber(poseStack, collector, tickDelta, state.lightCoords, state.outlineColor);
        } finally {
            poseStack.popPose();
        }

        if (!(cancelled || rendered))
            original.call(collector, poseStack, renderType, geometry);
    }
}
