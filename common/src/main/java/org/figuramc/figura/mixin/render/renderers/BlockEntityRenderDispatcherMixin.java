package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraSkullAvatarAssociationExtension;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "tryExtractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRenderOffScreen()Z"))
    private <E extends BlockEntity, S extends BlockEntityRenderState> void tryExtractRenderState(E blockEntity, float partialTicks, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, boolean isGloballyRendered, CallbackInfoReturnable<S> cir, @Local BlockEntityRenderer<E, S> renderer) {
        if (!(blockEntity instanceof SkullBlockEntity skullBlockEntity)) return;

        ResolvableProfile profile = skullBlockEntity.getOwnerProfile();
        if (profile == null) return;

        var avatar = AvatarManager.getAvatarForPlayer(profile.partialProfile().id());
        if (!(renderer instanceof FiguraSkullAvatarAssociationExtension figuraSkullAvatarAssociationExtension)) return;

        figuraSkullAvatarAssociationExtension.figura$setAvatar(avatar);
    }
}