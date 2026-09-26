package org.figuramc.figura.mixin.render.nodeRenderer;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OrderedSubmitNodeCollector.class)
public interface OrderedSubmitNodeCollectorMixin {
    @Inject(method = "submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/UvMapping;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"))
    default void figura$transferModelPartCallbacks(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, UvMapping uvMapping, int tintedColor, int outlineColor, CallbackInfo ci, @Local Model.Simple model) {
        FiguraSubmitCallBackExtension partExt = (FiguraSubmitCallBackExtension) (Object) modelPart;
        FiguraSubmitCallBackExtension modelExt = (FiguraSubmitCallBackExtension) (Object) model;

        for (var cb : partExt.figura$getPreRenderingCallbacks())
            modelExt.figura$addPreRenderingCallback(cb);
        for (var cb : partExt.figura$getPostRenderingCallbacks())
            modelExt.figura$addPostRenderingCallback(cb);

        modelExt.figura$setPreventAnimSetup(partExt.figura$getPreventAnimSetup());
        partExt.figura$setPreventAnimSetup(false);
        partExt.figura$getPreRenderingCallbacks().clear();
        partExt.figura$getPostRenderingCallbacks().clear();
    }

}
