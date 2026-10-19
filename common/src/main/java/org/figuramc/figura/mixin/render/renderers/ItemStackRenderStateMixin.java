package org.figuramc.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraItemStackRenderStateExtension;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.RenderUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin implements FiguraItemStackRenderStateExtension, FiguraSubmitCallBackExtension {
    @Shadow
    ItemDisplayContext displayContext;
    @Shadow private ItemStackRenderState.LayerRenderState[] layers;
    @Unique
    ItemStack figura$itemStack;
    @Unique
    private final List<BiFunction<VertexConsumer, PoseStack, Boolean>> figura$preRenderingCallback = new ArrayList<>();
    @Unique
    private final List<Runnable> figura$postRenderingCallback = new ArrayList<>();

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V", at = @At("HEAD"), cancellable = true)
    private void figura$itemRenderEvent(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci) {
        if (figura$itemStack == null || figura$itemStack.isEmpty() || displayContext == null)
            return;

        switch (displayContext) {
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD -> { return; }
            default -> { }
        }

        if (AvatarManager.panic)
            return;

        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        if (avatar == null)
            return;

        ItemTransform transform = figura$getItemTransform();

        if (avatar.itemRenderEvent(ItemStackAPI.verify(figura$itemStack), displayContext.name(),
                FiguraVec3.fromVec3f(transform.translation()),
                FiguraVec3.of(transform.rotation().z(), transform.rotation().y(), transform.rotation().x()),
                FiguraVec3.fromVec3f(transform.scale()), displayContext.leftHand(),
                poseStack, submitNodeCollector, lightCoords, overlayCoords)
        )
            ci.cancel();
    }

    @Override
    public void figura$setItemStack(@Nullable ItemStack itemStack) {
        this.figura$itemStack = itemStack;
    }

    @Override
    public ItemStack figura$getItemStack() {
        return figura$itemStack;
    }

    @Override
    public boolean figura$isLeftHanded() {
        return displayContext.leftHand();
    }

    @Override
    public ItemDisplayContext figura$getDisplayContext() {
        return displayContext;
    }

    @Override
    public ItemTransform figura$getItemTransform() {
        for (ItemStackRenderState.LayerRenderState layerRenderState : layers) {
            if (((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).figura$getTransform() != null)
                return ((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).figura$getTransform();
        }
        return ItemTransform.NO_TRANSFORM;
    }

    @Override
    public List<BakedQuad> figura$getQuads() {
        for (ItemStackRenderState.LayerRenderState layerRenderState : layers) {
            if (((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).figura$getQuads() != null)
                return ((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).figura$getQuads();
        }
        return List.of();
    }

    @Override
    public void figura$addPreRenderingCallback(BiFunction<VertexConsumer, PoseStack, Boolean> callback) {
        this.figura$preRenderingCallback.add(callback);
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V", at = @At("HEAD"), cancellable = true)
    private void figura$firstPersonItemEvent(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outlineColor, CallbackInfo ci) {
        if (!RenderUtils.firstPersonItemPass || RenderUtils.firstPersonItemEventActive)
            return;

        if (displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return;

        var player = Minecraft.getInstance().player;
        Avatar avatar = player == null ? null : AvatarManager.getAvatarForPlayer(player.getUUID());

        if (avatar == null)
            return;

        ItemTransform transform = figura$getItemTransform();
        RenderUtils.firstPersonItemEventActive = true;

        try {
            if (avatar.itemRenderEvent(ItemStackAPI.verify(figura$itemStack), displayContext.name(),
                    FiguraVec3.fromVec3f(transform.translation()), FiguraVec3.of(transform.rotation().z(), transform.rotation().y(), transform.rotation().x()),
                    FiguraVec3.fromVec3f(transform.scale()), displayContext.leftHand(),
                    poseStack, collector, light, overlay)
            )
                ci.cancel();
        } finally {
            RenderUtils.firstPersonItemEventActive = false;
        }
    }

    @Override
    public void figura$addPostRenderingCallback(Runnable callback) {
        this.figura$postRenderingCallback.add(callback);
    }

    @Override
    public List<Runnable> figura$getPostRenderingCallbacks() {
        return figura$postRenderingCallback;
    }

    @Override
    public List<BiFunction<VertexConsumer, PoseStack, Boolean>> figura$getPreRenderingCallbacks() {
        return figura$preRenderingCallback;
    }
}
