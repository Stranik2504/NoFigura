package org.figuramc.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.EntityRendererAccessor;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.lua.api.nameplate.EntityNameplateCustomization;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BiFunction;

@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, AvatarRenderState, PlayerModel> {

    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel entityModel, float shadowRadius) {
        super(context, entityModel, shadowRadius);
    }

    @Unique
    private Avatar avatar;


    @Inject(at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/UvMapping;)V"), method = "renderHand")
    private void onRenderHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, Identifier resourceLocation, ModelPart modelPart, boolean bl, CallbackInfo ci) {
        avatar = AvatarManager.getAvatarForPlayer(Minecraft.getInstance().player.getUUID());


        PlayerModel model = this.getModel();
        Map<ModelPart, PartPose> modelState = RenderUtils.captureModelState(model);

        Avatar localAvatar = avatar;
        BiFunction<VertexConsumer, PoseStack, Boolean> lambda = (bufferSource, stack) -> {
            if (localAvatar != null && localAvatar.luaRuntime != null) {
                VanillaPart part = localAvatar.luaRuntime.vanilla_model.PLAYER;
                RenderUtils.restoreModelPoseState(model, modelState);
                part.save(model);

                if (localAvatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1) {
                    part.preTransform(model);
                    part.posTransform(model);
                }
            }

            return true;
        };

        ((FiguraSubmitCallBackExtension)(Object)modelPart).figura$addPreRenderingCallback(lambda);
        ((FiguraSubmitCallBackExtension)(Object)modelPart).figura$addPostRenderingCallback(() -> {
            if (localAvatar != null && localAvatar.luaRuntime != null)
                localAvatar.luaRuntime.vanilla_model.PLAYER.restore(model);
            }
        );
    }

    @Inject(at = @At("RETURN"), method = "renderHand")
    private void postRenderHand(PoseStack stack, SubmitNodeCollector submitNodeCollector, int light, Identifier resourceLocation, ModelPart arm, boolean bl, CallbackInfo ci) {
        if (avatar == null)
            return;

        float delta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);

        PoseStack copy = new PoseStack();
        copy.pushPose(); // save the current stack
        copy.last().set(stack.last());

        PlayerModel playerModel = getModel();

        Map<ModelPart, PartPose> modelState = RenderUtils.captureModelState(playerModel);
        RenderUtils.restoreModelPoseState(playerModel, modelState);

        if (avatar != null && avatar.luaRuntime != null) {
            VanillaPart part = avatar.luaRuntime.vanilla_model.PLAYER;
            PlayerModel model = this.getModel();

            part.save(model);

            if (avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1) {
                part.preTransform(model);
                part.posTransform(model);
            }
        }

        avatar.firstPersonRender(copy, submitNodeCollector, Minecraft.getInstance().player, playerModel, arm, light, delta);

        if (avatar.luaRuntime != null)
            avatar.luaRuntime.vanilla_model.PLAYER.restore(playerModel);

        avatar = null;
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("HEAD"), cancellable = true)
    private void setupRotations(AvatarRenderState playerRenderState, PoseStack matrices, float f, float g, CallbackInfo cir) {
        Avatar avatar = AvatarManager.getAvatar(playerRenderState);
        if (RenderUtils.vanillaModelAndScript(avatar) && !avatar.luaRuntime.renderer.getRootRotationAllowed()) {
            cir.cancel();
        }
    }
}
