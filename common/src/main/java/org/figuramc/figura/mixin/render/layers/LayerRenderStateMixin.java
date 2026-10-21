package org.figuramc.figura.mixin.render.layers;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class LayerRenderStateMixin {
    @Shadow(aliases = "this$0") @Final private ItemStackRenderState outer;

    @Inject(method = "setupSpecialModel", at = @At("TAIL"))
    private <T> void figura$forceAnimatedForHeads(SpecialModelRenderer<T> renderer, T argument, CallbackInfo ci) {
        if (renderer instanceof PlayerHeadSpecialRenderer) {
            this.outer.setAnimated();
        }
    }
}