package org.figuramc.figura.mixin.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.mixin.render.PoseStackAccessor;
import org.figuramc.figura.utils.RenderUtils;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixinFabric {
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitFeatures(Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;Z)V"))
    private void renderLevelFirstPerson(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.mainCamera();
        DeltaTracker dt = minecraft.getDeltaTracker();
        if (camera.isDetached())
            return;

        float tickDelta = dt.getGameTimeDeltaPartialTick(false);
        Entity e = camera.entity();
        Avatar avatar = AvatarManager.getAvatar(e);

        if (avatar == null || !(e instanceof LivingEntity livingEntity))
            return;

        PoseStack stack = new PoseStack();

        if (RenderUtils.vanillaModelAndScript(avatar)) {
            FiguraMat3 normal = avatar.luaRuntime.renderer.cameraNormal;
            if (normal != null)
                stack.last().normal().set(normal.toMatrix3f());
        }

        @SuppressWarnings("unchecked")
        EntityRenderer<LivingEntity, LivingEntityRenderState> entityRenderer =
                (EntityRenderer<LivingEntity, LivingEntityRenderState>) this.entityRenderDispatcher.getRenderer(livingEntity);

        LivingEntityRenderState state = entityRenderer.createRenderState(livingEntity,
                dt.getGameTimeDeltaPartialTick(minecraft.level.tickRateManager().isEntityFrozen(e)));

        avatar.firstPersonWorldRender(e, stack, this.submitNodeStorage, camera, tickDelta);

        if (Configs.FIRST_PERSON_MATRICES.value) {
            Avatar.firstPerson = true;

            int lastIndex = ((PoseStackAccessor) stack).getLastIndex();
            stack.pushPose();
            Vec3 offset = entityRenderer.getRenderOffset(state);
            Vec3 cam = camera.position();
            stack.translate(
                    Mth.lerp(tickDelta, livingEntity.xOld, livingEntity.getX()) - cam.x() + offset.x(),
                    Mth.lerp(tickDelta, livingEntity.yOld, livingEntity.getY()) - cam.y() + offset.y(),
                    Mth.lerp(tickDelta, livingEntity.zOld, livingEntity.getZ()) - cam.z() + offset.z()
            );

            entityRenderer.submit(state, stack, this.submitNodeStorage, cameraState);
            do {
                stack.popPose();
            } while (((PoseStackAccessor) stack).getLastIndex() > lastIndex);

            Avatar.firstPerson = false;
        }

        /*Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(cameraState.viewRotationMatrix);

        featureRenderDispatcher.renderAllFeatures(this.submitNodeStorage);

        modelViewStack.popMatrix();
        Avatar.firstPerson = false;*/
    }
}
