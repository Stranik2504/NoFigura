package org.figuramc.figura.mixin.fabric;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalDouble;

@Mixin(LevelRenderer.class)
public class LevelRendererMixinFabric {
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

    @Inject(method = "render", at = @At("RETURN"))
    private void renderLevelFirstPerson(GraphicsResourceAllocator resourceAllocator, boolean renderOutline, CameraRenderState cameraState, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, boolean consistentDepthRequired, CallbackInfo ci) {
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
        }

        GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
        RenderTarget mainTarget = gameRenderer.mainRenderTarget();

        GpuTextureView depthTextureView = consistentDepthRequired
                ? gameRenderer.hud3DTarget.getDepthTextureView()
                : mainTarget.getDepthTextureView();

        try (
                FeatureRenderDispatcher.PreparedFrame frame = featureRenderDispatcher.prepareFrame(this.submitNodeStorage);
                RenderPass pass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "FiguraFirstPerson",
                                mainTarget.getColorTextureView(), Optional.empty(),
                                depthTextureView, OptionalDouble.empty());
        ) {
            RenderSystem.bindDefaultUniforms(pass);
            FeatureRenderDispatcher.renderAllFeatures(pass, frame);
        }

        Avatar.firstPerson = false;
    }
}
