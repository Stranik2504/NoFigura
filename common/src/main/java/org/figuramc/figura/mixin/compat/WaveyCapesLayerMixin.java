package org.figuramc.figura.mixin.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.tr7zw.waveycapes.renderlayers.CustomCapeRenderLayer", remap = false)
public class WaveyCapesLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void figura$hideCape(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState renderState, float f, float g, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(renderState);

        if (!RenderUtils.vanillaModelAndScript(avatar))
            return;

        if (!avatar.luaRuntime.vanilla_model.CAPE_MODEL.checkVisible() || !avatar.luaRuntime.vanilla_model.CAPE.checkVisible())
            ci.cancel();
    }
}
