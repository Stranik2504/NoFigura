package org.figuramc.figura.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.mixin.gui.GuiGraphicsExtractorAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class FiguraGui {

    public static void onRender(GuiGraphicsExtractor guiGraphics, float tickDelta, CallbackInfo ci) {
        if (AvatarManager.panic)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);

        FiguraMod.pushProfiler("popupMenu");
        PopupMenu.render(guiGraphics);
        FiguraMod.popProfiler();

        Entity entity = Minecraft.getInstance().getCameraEntity();
        Avatar avatar = entity == null ? null : AvatarManager.getAvatar(entity);

        if (avatar != null) {
            int width = guiGraphics.guiWidth();
            int height = guiGraphics.guiHeight();

            FiguraHudRenderState state = new FiguraHudRenderState(avatar, entity, tickDelta, 0, 0, width, height, 1f, null);
            ((GuiGraphicsExtractorAccessor) guiGraphics).figura$getRenderState().addPicturesInPictureState(state);

            if (avatar.luaRuntime != null && !avatar.luaRuntime.renderer.renderHUD) {
                renderOverlays(guiGraphics);
                ci.cancel();
            }
        }

        FiguraMod.popProfiler();
    }

    public static void renderOverlays(GuiGraphicsExtractor guiGraphics) {
        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler("paperdoll");
        PaperDoll.render(guiGraphics, false);
        FiguraMod.popPushProfiler("actionWheel");
        ActionWheel.render(guiGraphics);
        FiguraMod.popProfiler(2);
    }
}