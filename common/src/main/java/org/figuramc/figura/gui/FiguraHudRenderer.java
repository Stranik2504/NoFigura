package org.figuramc.figura.gui;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import org.figuramc.figura.avatar.Avatar;
import org.jspecify.annotations.NonNull;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.OptionalDouble;

public class FiguraHudRenderer extends PictureInPictureRenderer<FiguraHudRenderState> {

    private final ProjectionMatrixBuffer hudProjectionMatrixBuffer = new ProjectionMatrixBuffer("Hud-PIP");
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();

    private GpuTexture texture, depthTexture;
    private GpuTextureView textureView, depthTextureView;
    private GpuSampler sampler;

    @Override
    public @NonNull Class<FiguraHudRenderState> getRenderStateClass() {
        return FiguraHudRenderState.class;
    }

    @Override
    protected void renderToTexture(FiguraHudRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        if (state.avatar() == null)
            return;

        // сохраняем ровно ту трансформацию, что раньше делал Avatar.hudRender сам:
        poseStack.last().pose().scale(16, 16, -16);
        poseStack.last().normal().scale(1, 1, -1);

        state.avatar().hudRender(poseStack, submitNodeCollector, state.entity(), state.tickDelta());
    }

    @Override
    public void prepare(FiguraHudRenderState state, GuiRenderState guiRenderState, FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
        if (state.avatar() == null) {
            super.prepare(state, guiRenderState, featureRenderDispatcher, guiScale);
            return;
        }

        int width = (state.x1() - state.x0()) * guiScale;
        int height = (state.y1() - state.y0()) * guiScale;

        prepareTexturesAndProjection(width, height);

        PoseStack poseStack = new PoseStack();
        // центр экрана — как раньше делал matrix4fStack.translation(0,0,-11000) + stack.setIdentity()
        poseStack.translate(width / 2.0F, height / 2.0F, 0.0F);

        this.renderToTexture(state, poseStack, this.submitNodeStorage);

        try (
                FeatureRenderDispatcher.PreparedFrame frame = featureRenderDispatcher.prepareFrame(this.submitNodeStorage);
                RenderPass pass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "FiguraHud", textureView, Optional.empty(), depthTextureView, OptionalDouble.empty())
        ) {
            RenderSystem.bindDefaultUniforms(pass);
            FeatureRenderDispatcher.renderAllFeatures(pass, frame);
        }

        this.blitTexture(state, guiRenderState);
    }

    private void prepareTexturesAndProjection(int width, int height) {
        boolean needsRecreate = texture == null || texture.getWidth(0) != width || texture.getHeight(0) != height;
        if (needsRecreate) {
            if (texture != null) {
                texture.close(); textureView.close();
                depthTexture.close(); depthTextureView.close();
                if (sampler != null) sampler.close();
            }
            GpuDevice gpuDevice = RenderSystem.getDevice();
            texture = gpuDevice.createTexture(() -> "UI figura-hud texture", 13, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
            textureView = gpuDevice.createTextureView(texture);
            depthTexture = gpuDevice.createTexture(() -> "UI figura-hud depth texture", 9, GpuFormat.D32_FLOAT, width, height, 1, 1);
            depthTextureView = gpuDevice.createTextureView(depthTexture);
            sampler = gpuDevice.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(texture, GuiRenderer.CLEAR_COLOR, depthTexture, 0.0);
        RenderSystem.setProjectionMatrix(
                hudProjectionMatrixBuffer.getBuffer(new Matrix4f().setOrtho(0.0F, width, height, 0.0F, 1000.0F, 11000.0F)),
                ProjectionType.ORTHOGRAPHIC
        );
    }

    @Override
    protected void blitTexture(FiguraHudRenderState state, GuiRenderState guiRenderState) {
        guiRenderState.addBlitToCurrentLayer(
                new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        TextureSetup.singleTexture(textureView, sampler),
                        state.pose(),
                        state.x0(), state.y0(), state.x1(), state.y1(),
                        0.0F, 1.0F, 1.0F, 0.0F,
                        -1,
                        state.scissorArea(),
                        null
                )
        );
    }

    @Override
    protected String getTextureLabel() {
        return "figura-hud";
    }

    @Override
    public void close() {
        super.close();
        if (texture != null) {
            texture.close(); textureView.close();
            depthTexture.close(); depthTextureView.close();
        }
        if (sampler != null) sampler.close();
        hudProjectionMatrixBuffer.close();
    }
}