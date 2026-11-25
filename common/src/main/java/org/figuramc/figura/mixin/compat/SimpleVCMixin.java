package org.figuramc.figura.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.utils.NameplateUtils;
import org.figuramc.figura.utils.TextUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Pseudo
@Mixin(targets = "de.maxhenkel.voicechat.voice.client.RenderEvents")
public class SimpleVCMixin {
    // Simple VC will hide your icon, this makes it so it is rendered if your nameplate is rendered
    @WrapOperation(method = "onRenderName",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;equals(Ljava/lang/Object;)Z"))
    private boolean figura$selfCheck(Entity entity, Object player, Operation<Boolean> original) {
        return !Configs.SELF_NAMEPLATE.value && original.call(entity, player);
    }

    @Inject(method = "shouldShowIcons", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void renderSelfNameplate(CallbackInfoReturnable<Boolean> callbackInfoReturnable){
        callbackInfoReturnable.setReturnValue(true);
    }

    @WrapOperation(method = "renderPlayerIcon",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"))
    private int figura$nameWidth(Font font, FormattedText nameTag, Operation<Integer> original,
                                 @Local(argsOnly = true) UUID id) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(id);
        Component name = (Component) nameTag;
        Component result = NameplateUtils.buildEntityName(name, name, avatar, id);

        // по строкам имя делится только при активной кастомизации
        if (NameplateUtils.getCustom(avatar) != null) {
            List<Component> lines = TextUtils.splitText(result, "\n");
            if (!lines.isEmpty())
                result = lines.get(lines.size() - 1);
        }

        return original.call(font, result);
    }
}