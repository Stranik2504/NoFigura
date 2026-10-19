package org.figuramc.figura.mixin.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.FiguraGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws Figura overlays (paperdoll, action wheel) right after the HUD has been extracted.
 */
@Mixin(Gui.class)
public class GuiOverlayMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void figura$renderOverlays(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, @Local GuiGraphicsExtractor graphics) {
        if (!AvatarManager.panic)
            FiguraGui.renderOverlays(graphics);
    }
}