package org.figuramc.figura.mixin.render.renderers;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.figuramc.figura.ducks.SkullAvatarLookup;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "tryExtractRenderState", at = @At("RETURN"))
    private <E extends BlockEntity, S extends BlockEntityRenderState> void figura$tagSkullState(E blockEntity, float partialTicks, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, CallbackInfoReturnable<S> cir) {
        S state = cir.getReturnValue();

        if (state == null || !(blockEntity instanceof SkullBlockEntity skullBlockEntity))
            return;

        SkullAvatarLookup.putBlock(state, SkullAvatarLookup.fromProfile(skullBlockEntity.getOwnerProfile()));
    }
}
