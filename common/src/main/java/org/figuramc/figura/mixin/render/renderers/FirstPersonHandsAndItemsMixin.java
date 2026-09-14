package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItems.class)
public class FirstPersonHandsAndItemsMixin {
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            ordinal = 0))
    private void figura$skullMainHand(
            LocalPlayer player, float partialTicks, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci,
            @Local(ordinal = 0) ItemDisplayContext mainHandDisplayContext) {
        figura$applySkullMode(player, state.mainHandItem, mainHandDisplayContext);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            ordinal = 1))
    private void figura$skullOffHand(
            LocalPlayer player, float partialTicks, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci,
            @Local(ordinal = 1) ItemDisplayContext offHandDisplayContext) {
        figura$applySkullMode(player, state.offHandItem, offHandDisplayContext);
    }

    @Unique
    private void figura$applySkullMode(LocalPlayer player, ItemStack stack, ItemDisplayContext displayContext) {
        if (stack.getItem() instanceof BlockItem bl && bl.getBlock() instanceof AbstractSkullBlock) {
            SkullBlockRendererAccessor.setEntity(player);
            SkullBlockRendererAccessor.setRenderMode(switch (displayContext) {
                case FIRST_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_LEFT_HAND;
                case FIRST_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_RIGHT_HAND;
                default -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_RIGHT_HAND;
            });
        }
    }
}
