package org.figuramc.figura.gui;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.figuramc.figura.utils.HudRenderContext;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.OptionalDouble;

public class FiguraHudRenderer extends PictureInPictureRenderer<FiguraHudRenderState> {

    private static final int COLOR_USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
    private static final int DEPTH_USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_RENDER_ATTACHMENT;

    private final ProjectionMatrixBuffer hudProjectionMatrixBuffer = new ProjectionMatrixBuffer("Hud-PIP");
    private final Projection hudProjection = new Projection();
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();

    private GpuTexture texture, depthTexture;
    private GpuTextureView textureView, depthTextureView;

    @Override
    public @NonNull Class<FiguraHudRenderState> getRenderStateClass() {
        return FiguraHudRenderState.class;
    }

    @Override
    protected void renderToTexture(FiguraHudRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        if (state.avatar() == null)
            return;

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

        float scale = guiScale * state.scale();
        prepareTexturesAndProjection(width, height, scale);

        PoseStack poseStack = new PoseStack();
        poseStack.scale(scale, scale, scale);

        HudRenderContext.setScale(scale);

        try {
            this.renderToTexture(state, poseStack, this.submitNodeStorage);
        } finally {
            HudRenderContext.clear();
        }

        FeatureRenderDispatcher.PreparedFrame frame;

        HudRenderContext.beginHudPass();
        try {
            frame = featureRenderDispatcher.prepareFrame(this.submitNodeStorage);
        } finally {
            HudRenderContext.endHudPass();
        }
        try (
                frame;
                RenderPass pass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "FiguraHud", textureView, Optional.empty(), depthTextureView, OptionalDouble.empty())
        ) {
            RenderSystem.bindDefaultUniforms(pass);
            FeatureRenderDispatcher.renderAllFeatures(pass, frame);
        }

        this.blitTexture(state, guiRenderState);
    }

    private void prepareTexturesAndProjection(int width, int height, float scale) {
        boolean needsRecreate = texture == null || texture.getWidth(0) != width || texture.getHeight(0) != height;
        if (needsRecreate) {
            if (texture != null) {
                texture.close(); textureView.close();
                depthTexture.close(); depthTextureView.close();
            }
            GpuDevice gpuDevice = RenderSystem.getDevice();
            texture = gpuDevice.createTexture(() -> "UI figura-hud texture", COLOR_USAGE, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
            textureView = gpuDevice.createTextureView(texture);
            depthTexture = gpuDevice.createTexture(() -> "UI figura-hud depth texture", DEPTH_USAGE, GpuFormat.D32_FLOAT, width, height, 1, 1);
            depthTextureView = gpuDevice.createTextureView(depthTexture);
        }

        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(texture, GuiRenderer.CLEAR_COLOR, depthTexture, 0.0);
        hudProjection.setupOrtho(-10000.0F * scale, 10000.0F * scale, width, height, true);
        RenderSystem.setProjectionMatrix(hudProjectionMatrixBuffer.getBuffer(hudProjection), ProjectionType.ORTHOGRAPHIC);
    }

    @Override
    protected void blitTexture(FiguraHudRenderState state, GuiRenderState guiRenderState) {
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
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
        hudProjectionMatrixBuffer.close();
    }
}