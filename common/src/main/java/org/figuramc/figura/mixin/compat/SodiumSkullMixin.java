package org.figuramc.figura.mixin.compat;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo$Builder", remap = false)
public class SodiumSkullMixin {
    @Shadow
    @Final
    private List<BlockEntity> globalBlockEntities;

    @Inject(method = "addBlockEntity", at = @At("TAIL"))
    private void figura$addSkullToBoth(BlockEntity entity, boolean cull, CallbackInfo ci) {
        if (cull && entity instanceof SkullBlockEntity)
            globalBlockEntities.add(entity);
    }
}
