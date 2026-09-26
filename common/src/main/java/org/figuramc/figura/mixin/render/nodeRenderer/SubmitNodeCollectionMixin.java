package org.figuramc.figura.mixin.render.nodeRenderer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.world.item.ItemDisplayContext;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FlameSubmitExtension;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {
    @WrapOperation(method = "submitModel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"))
    private <S, Submit extends SubmitNode> void figura$transferTranslucent(FeatureRenderPhase<? super TranslucentSubmit> instance, Submit submit, Operation<Void> original, @Local(argsOnly = true) Model<? super S> model) {
        figura$transfer(model, submit);
        original.call(instance, submit);
    }

    @WrapOperation(method = "submitModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"))
    private <S> void figura$onSubmitModel(SimpleFeatureRenderPhase instance, SubmitNode submit, Operation<Void> original, @Local(argsOnly = true) Model<? super S> model) {
        figura$transfer(model, submit);
        original.call(instance, submit);
    }

    @Unique
    private void figura$transfer(Model<?> model, Object submitObj) {
        FiguraSubmitCallBackExtension modelExt = (FiguraSubmitCallBackExtension) model;
        FiguraSubmitCallBackExtension submitExt = (FiguraSubmitCallBackExtension) submitObj;

        for (var cb : modelExt.figura$getPreRenderingCallbacks())
            submitExt.figura$addPreRenderingCallback(cb);
        for (var cb : modelExt.figura$getPostRenderingCallbacks())
            submitExt.figura$addPostRenderingCallback(cb);

        submitExt.figura$setPreventAnimSetup(modelExt.figura$getPreventAnimSetup());
        modelExt.figura$setPreventAnimSetup(false);
        modelExt.figura$getPreRenderingCallbacks().clear();
        modelExt.figura$getPostRenderingCallbacks().clear();
    }

    @WrapOperation(method = "submitItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"))
    private void figura$onSubmitItemSimple(SimpleFeatureRenderPhase instance, SubmitNode submit, Operation<Void> original,
                                           @Local(argsOnly = true) ItemDisplayContext displayContext) {
        figura$transferItem(displayContext, submit);
        original.call(instance, submit);
    }

    @WrapOperation(method = "submitItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"))
    private <Submit extends SubmitNode> void figura$onSubmitItemTranslucent(FeatureRenderPhase<? super TranslucentSubmit> instance, Submit submit, Operation<Void> original, @Local(argsOnly = true) ItemDisplayContext displayContext) {
        figura$transferItem(displayContext, submit);
        original.call(instance, submit);
    }

    @Unique
    private void figura$transferItem(ItemDisplayContext displayContext, Object submitObj) {
        FiguraSubmitCallBackExtension displayExt = (FiguraSubmitCallBackExtension) (Object) displayContext;
        FiguraSubmitCallBackExtension submitExt = (FiguraSubmitCallBackExtension) submitObj;

        for (var cb : displayExt.figura$getPreRenderingCallbacks())
            submitExt.figura$addPreRenderingCallback(cb);
        for (var cb : displayExt.figura$getPostRenderingCallbacks())
            submitExt.figura$addPostRenderingCallback(cb);

        submitExt.figura$setPreventAnimSetup(displayExt.figura$getPreventAnimSetup());
        displayExt.figura$setPreventAnimSetup(false);
        displayExt.figura$getPreRenderingCallbacks().clear();
        displayExt.figura$getPostRenderingCallbacks().clear();
    }

    @WrapOperation(method = "submitFlame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"))
    private void figura$onSubmitFlame(SimpleFeatureRenderPhase instance, SubmitNode submit, Operation<Void> original) {
        var flameSubmit = (FlameFeatureRenderer.Submit) submit;

        FlameSubmitExtension itemSubmissionExtension = (FlameSubmitExtension) (Object) flameSubmit;
        Avatar avatar = AvatarManager.getAvatar(flameSubmit.entityRenderState());
        itemSubmissionExtension.figura$setAvatar(avatar);

        original.call(instance, submit);
    }
}
