package org.figuramc.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelPart;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, float attackValue, HumanoidArm arm, PlayerRenderState playerState);

    @Unique Avatar avatar;

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void onSubmitHandsWithItems(float partialTicks, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        avatar = player == null ? null : AvatarManager.getAvatarForPlayer(player.getUUID());
        if (avatar == null)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(avatar);
        FiguraMod.pushProfiler("renderEvent");
        avatar.renderMode = EntityRenderMode.FIRST_PERSON;
        avatar.renderEvent(partialTicks, new FiguraMat4().set(poseStack.last().pose()));
        FiguraMod.popProfiler(3);
    }

    @Inject(method = "submitHandsWithItems", at = @At("RETURN"))
    private void afterSubmitHandsWithItems(float partialTicks, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        if (avatar == null)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(avatar);
        FiguraMod.pushProfiler("postRenderEvent");
        avatar.postRenderEvent(partialTicks, new FiguraMat4().set(poseStack.last().pose()));
        avatar = null;
        FiguraMod.popProfiler(3);
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void submitArmWithItem(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, float partialTicks, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (state.isScoping || avatar == null || avatar.luaRuntime == null)
            return;

        AvatarRenderState avatarRenderState = playerState.avatarRenderState;
        if (avatarRenderState == null)
            return;

        boolean main = hand == InteractionHand.MAIN_HAND;
        HumanoidArm arm = main ? avatarRenderState.mainArm : avatarRenderState.mainArm.getOpposite();
        Boolean armVisible = arm == HumanoidArm.LEFT ? avatar.luaRuntime.renderer.renderLeftArm : avatar.luaRuntime.renderer.renderRightArm;

        boolean willRenderItem = !itemStack.isEmpty();
        boolean willRenderArm = (!willRenderItem && main) || itemStack.is(Items.FILLED_MAP) || (!willRenderItem && state.mainHandItem.is(Items.FILLED_MAP));

        // hide arm
        if (willRenderArm && !willRenderItem && armVisible != null && !armVisible) {
            ci.cancel();
            return;
        }
        // render arm
        if (!willRenderArm && !avatarRenderState.isInvisible && armVisible != null && armVisible) {
            poseStack.pushPose();
            this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, inverseArmHeight, attack, arm, playerState);
            poseStack.popPose();
        }

        // hide item
        VanillaModelPart part = arm == HumanoidArm.LEFT ? avatar.luaRuntime.vanilla_model.LEFT_ITEM : avatar.luaRuntime.vanilla_model.RIGHT_ITEM;
        if (willRenderItem && !part.checkVisible()) {
            ci.cancel();
        }
    }
}
