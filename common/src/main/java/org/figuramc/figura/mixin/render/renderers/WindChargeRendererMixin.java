package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.WindChargeRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.model.rendering.PartFilterScheme;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WindChargeRenderer.class)
public class WindChargeRendererMixin {

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;III)V"
            )
    )
    private void figura$renderCustomWindCharge(SubmitNodeCollector collector, Model<?> model, Object modelState,
                                               PoseStack poseStack, RenderType renderType, int light, int overlay,
                                               int outlineColor, Operation<Void> original,
                                               @Local(argsOnly = true) EntityRenderState state) {
        Entity entity = RenderUtils.getEntity(state);
        if (!(entity instanceof AbstractWindCharge windCharge)) {
            original.call(collector, model, modelState, poseStack, renderType, light, overlay, outlineColor);
            return;
        }

        PartFilterScheme scheme = figura$scheme(entity.getType());
        Avatar avatar = scheme == null ? null : RenderUtils.getRenderAvatar(windCharge.getOwner());
        if (avatar == null) {
            original.call(collector, model, modelState, poseStack, renderType, light, overlay, outlineColor);
            return;
        }

        float tickDelta = RenderUtils.getTickDelta(state);
        String event = RenderUtils.projectileEvent(scheme);
        boolean cancelled = event != null && avatar.projectileRenderEvent(event, tickDelta, EntityAPI.wrap(entity));
        boolean rendered = avatar.renderSpecialPart(scheme, poseStack, collector, tickDelta,
                state.lightCoords, state.outlineColor);

        if (!(cancelled || rendered))
            original.call(collector, model, modelState, poseStack, renderType, light, overlay, outlineColor);
    }

    private static PartFilterScheme figura$scheme(EntityType<?> type) {
        if (type == EntityTypes.WIND_CHARGE)
            return PartFilterScheme.WIND_CHARGE;
        if (type == EntityTypes.BREEZE_WIND_CHARGE)
            return PartFilterScheme.BREEZE_WIND_CHARGE;
        return null;
    }
}
