package org.figuramc.figura.mixin.render.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {
    @Inject(method = "prepareSubmit", at = @At(value = "HEAD"), cancellable = true)
    private void figura$preRender(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        FiguraSubmitCallBackExtension callBackExtension = (FiguraSubmitCallBackExtension) (Object) submit;
        var poseStack = figura$poseStackFromSubmit(submit);

        for (var callback : callBackExtension.figura$getPreRenderingCallbacks()) {
            if (!callback.apply(null, poseStack)) {
                ci.cancel();
            }
        }

        if (ci.isCancelled()) {
            for (var callback : callBackExtension.figura$getPostRenderingCallbacks())
                callback.run();

            callBackExtension.figura$getPostRenderingCallbacks().clear();
        }

        callBackExtension.figura$getPreRenderingCallbacks().clear();
    }

    @Inject(method = "prepareSubmit", at = @At(value = "RETURN"))
    private <S> void figura$postRender(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        FiguraSubmitCallBackExtension callBackExtension = (FiguraSubmitCallBackExtension) (Object) submit;

        for (var callback : callBackExtension.figura$getPostRenderingCallbacks())
            callback.run();

        callBackExtension.figura$getPostRenderingCallbacks().clear();
    }

    @Unique
    private PoseStack figura$poseStackFromSubmit(ItemFeatureRenderer.Submit submit) {
        var poseStack = new PoseStack();
        poseStack.last().set(submit.pose());
        return poseStack;
    }
}
