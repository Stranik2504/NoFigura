package org.figuramc.figura.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.figuramc.figura.utils.PlatformUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ClientLevel.class)
public class ClientLevelSkullMixin {
    @Unique
    private static final boolean figura$HAS_SODIUM = PlatformUtils.isModLoaded("sodium");

    @Shadow
    @Final
    private Set<BlockEntity> globallyRenderedBlockEntities;

    @Inject(method = "onBlockEntityAdded", at = @At("TAIL"))
    private void figura$addSkullToGlobal(BlockEntity blockEntity, CallbackInfo ci) {
        if (!figura$HAS_SODIUM && blockEntity instanceof SkullBlockEntity)
            globallyRenderedBlockEntities.add(blockEntity);
    }
}
