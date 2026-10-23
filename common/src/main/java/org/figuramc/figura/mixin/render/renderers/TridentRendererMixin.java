package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraEntityRenderStateExtension;
import org.figuramc.figura.ducks.FiguraProjectileRenderStateExtension;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.ducks.NodeCollectorExtension;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.permissions.Permissions;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTridentRenderer.class)
public abstract class TridentRendererMixin<T extends ThrownTrident, S extends ThrownTridentRenderState> extends EntityRenderer<T, S> {

    @Shadow
    @Final
    private TridentModel model;

    protected TridentRendererMixin(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Unique
    private boolean figura$hideVanilla;

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ThrownTridentRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private void figura$submitTrident(OrderedSubmitNodeCollector instance, Model<? super S> model, Object state, PoseStack poseStack, Identifier texture, int lightCoords, int overlayCoords, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, Operation<Void> original, @Local(argsOnly = true) ThrownTridentRenderState renderState) {
        figura$hideVanilla = false;

        Integer id = ((FiguraEntityRenderStateExtension) renderState).figura$getEntityId();
        Projectile trident = id == null ? null : (Projectile) AvatarManager.ENTITY_CACHE.computeIfAbsent(id, (id2) -> Minecraft.getInstance().level.getEntity(id2));
        Entity owner = trident == null ? null : trident.getOwner();
        Avatar avatar = owner == null ? null : AvatarManager.getAvatar(owner);

        if (avatar != null && avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) != 0) {

            float tickDelta = ((FiguraProjectileRenderStateExtension) renderState).figura$getTickDelta();

            FiguraMod.pushProfiler(FiguraMod.MOD_ID);
            FiguraMod.pushProfiler(avatar);
            FiguraMod.pushProfiler("tridentRender");

            FiguraMod.pushProfiler("event");
            boolean cancelled = avatar.tridentRenderEvent(tickDelta, EntityAPI.wrap(trident));
            FiguraMod.popProfiler(4);

            boolean customParts = !cancelled && avatar.hasTridentParts();

            if (customParts && instance instanceof NodeCollectorExtension extension) {
                PoseStack snapshot = new PoseStack();
                snapshot.last().set(poseStack.last());
                int light = renderState.lightCoords;

                extension.submitFiguraModel(avatar, renderState, (a, s, bufferSource) -> {
                    a.renderTrident(snapshot, bufferSource, tickDelta, light);
                    return null;
                });
            }

            if (cancelled || customParts) {
                figura$hideVanilla = true;
                return;
            }
        }

        original.call(instance, model, state, poseStack, texture, lightCoords, overlayCoords, outlineColor, crumblingOverlay);
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ThrownTridentRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private void figura$submitTridentFoil(OrderedSubmitNodeCollector instance, Model<? super S> model, Object state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, Operation<Void> original) {
        if (figura$hideVanilla) {
            figura$hideVanilla = false;
            return;
        }

        original.call(instance, model, state, poseStack, renderType, lightCoords, overlayCoords, outlineColor, crumblingOverlay);
    }

    @Inject(at = @At("HEAD"), method = "extractRenderState(Lnet/minecraft/world/entity/projectile/arrow/ThrownTrident;Lnet/minecraft/client/renderer/entity/state/ThrownTridentRenderState;F)V")
    void appendFiguraProperties(T trident, S tridentState, float f, CallbackInfo ci) {
        ((FiguraProjectileRenderStateExtension)tridentState).figura$setTickDelta(f);
    }
}
