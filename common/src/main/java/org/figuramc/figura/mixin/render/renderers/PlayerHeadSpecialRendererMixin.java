package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraSkullAvatarAssociationExtension;
import org.figuramc.figura.ducks.SkullAvatarLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerHeadSpecialRenderer.class)
public abstract class PlayerHeadSpecialRendererMixin {
    @ModifyReturnValue(method = "extractArgument(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/renderer/PlayerSkinRenderCache$RenderInfo;", at = @At("TAIL"))
    public PlayerSkinRenderCache.RenderInfo setAvatar(PlayerSkinRenderCache.RenderInfo original, @Local(argsOnly = true) ItemStack itemStack) {
        if (original == null) return original;
        Avatar avatar = AvatarManager.getAvatarForPlayer(original.gameProfile().id());
        ((FiguraSkullAvatarAssociationExtension) (Object) original).figura$setAvatar(avatar);
        return original;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/PlayerSkinRenderCache$RenderInfo;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V", at = @At("HEAD"))
    private void captureAvatar(PlayerSkinRenderCache.RenderInfo playerHeadRenderInfo, ItemDisplayContext itemDisplayContext, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, boolean bl, int k, CallbackInfo ci) {
        if (playerHeadRenderInfo == null) {
            SkullAvatarLookup.setPendingSpecial(null);
            return;
        }
        // copy the avatar from the playerHeadRenderInfo into the inner renderType()
        // possibly redundant, see PlayerHeadRenderInfoMixin for implementation details
        Avatar avatar = ((FiguraSkullAvatarAssociationExtension)(Object)playerHeadRenderInfo).figura$getAvatar();
        ((FiguraSkullAvatarAssociationExtension)playerHeadRenderInfo.renderType()).figura$setAvatar(avatar);
        SkullAvatarLookup.setPendingSpecial(avatar);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/PlayerSkinRenderCache$RenderInfo;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V", at = @At("RETURN"))
    private void figura$clearPendingAvatar(PlayerSkinRenderCache.RenderInfo renderInfo, ItemDisplayContext itemDisplayContext, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, boolean bl, int k, CallbackInfo ci) {
        SkullAvatarLookup.clearPendingSpecial();
    }
}
