package org.figuramc.figura.gui;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.gui.widgets.permissions.PlayerPermPackElement;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public class FiguraPortraitRenderer extends PictureInPictureRenderer<FiguraPortraitRenderState> {

    private static final int COLOR_USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
    private static final int DEPTH_USAGE = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_RENDER_ATTACHMENT;

    private final ProjectionMatrixBuffer avatarProjectionMatrixBuffer = new ProjectionMatrixBuffer(
            "Portrait-PIP - " + this.getClass().getSimpleName()
    );
    private final Projection avatarProjection = new Projection();
    private final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
    Map<Avatar, TextureEntry> avatarToTexture = new HashMap<>();
    private boolean renderSkin;

    @Override
    public @NonNull Class<FiguraPortraitRenderState> getRenderStateClass() {
        return FiguraPortraitRenderState.class;
    }

    @Override
    protected void renderToTexture(FiguraPortraitRenderState portraitState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);

        Avatar avatar = portraitState.avatar();
        if (avatar != null) {
            renderSkin = !avatar.renderHeadForPortrait(submitNodeCollector, poseStack, LightCoordsUtil.FULL_BRIGHT, portraitState.modelScale(), portraitState.upsideDown());
        } else {
            renderSkin = true;
        }
    }

    @Override
    public void prepare(FiguraPortraitRenderState state, GuiRenderState guiRenderState, FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
        int width = (state.x1() - state.x0()) * guiScale;
        int height = (state.y1() - state.y0()) * guiScale;

        if (state.avatar() != null) {
            prepareTexturesAndProjectionForAvatar(state.avatar(), width, height);
            TextureEntry textureEntry = avatarToTexture.get(state.avatar());

            PoseStack poseStack = new PoseStack();
            poseStack.translate(width / 2.0F, this.getTranslateY(height, guiScale), 0.0F);
            float f = guiScale * state.scale();
            poseStack.scale(f, f, -f);

            this.renderToTexture(state, poseStack, this.submitNodeStorage);

            try (
                    FeatureRenderDispatcher.PreparedFrame frame = featureRenderDispatcher.prepareFrame(this.submitNodeStorage);
                    RenderPass pass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "FiguraPortrait", textureEntry.textureView, Optional.empty(), textureEntry.depthTextureView, OptionalDouble.empty())
            ) {
                RenderSystem.bindDefaultUniforms(pass);
                FeatureRenderDispatcher.renderAllFeatures(pass, frame);
            }

            this.blitTexture(state, guiRenderState);
        } else {
            super.prepare(state, guiRenderState, featureRenderDispatcher, guiScale);
        }
    }

    private void prepareTexturesAndProjectionForAvatar(Avatar avatar, int i, int j) {
        if (avatar == null)
            return;

        TextureEntry entry = avatarToTexture.computeIfAbsent(avatar, k -> new TextureEntry());

        boolean bl = entry.texture == null || entry.texture.getWidth(0) != i || entry.texture.getHeight(0) != j;
        if (entry.texture != null && bl) {
            entry.texture.close();
            entry.texture = null;
            entry.textureView.close();
            entry.textureView = null;
            entry.depthTexture.close();
            entry.depthTexture = null;
            entry.depthTextureView.close();
        }

        GpuDevice gpuDevice = RenderSystem.getDevice();
        if (entry.texture == null) {
            entry.texture = gpuDevice.createTexture(() -> "UI " + this.getTextureLabel() + " texture " + avatar.name, COLOR_USAGE, GpuFormat.RGBA8_UNORM, i, j, 1, 1);
            entry.textureView = gpuDevice.createTextureView(entry.texture);
            entry.depthTexture = gpuDevice.createTexture(() -> "UI " + this.getTextureLabel() + " depth texture " + avatar.name, DEPTH_USAGE, GpuFormat.D32_FLOAT, i, j, 1, 1);
            entry.depthTextureView = gpuDevice.createTextureView(entry.depthTexture);
        }

        gpuDevice.createCommandEncoder().clearColorAndDepthTextures(entry.texture, GuiRenderer.CLEAR_COLOR, entry.depthTexture, 0.0);
        // Use Minecraft's projection so clip-space depth matches the active RenderPearl backend.
        avatarProjection.setupOrtho(-1000.0F, 1000.0F, i, j, true);
        RenderSystem.setProjectionMatrix(avatarProjectionMatrixBuffer.getBuffer(avatarProjection), ProjectionType.ORTHOGRAPHIC);
    }

    @Override
    protected void blitTexture(FiguraPortraitRenderState pictureInPictureRenderState, GuiRenderState guiRenderState) {
        if (!renderSkin){
            TextureEntry entry = avatarToTexture.get(pictureInPictureRenderState.avatar());
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(entry.textureView, sampler),
                            pictureInPictureRenderState.pose(),
                            pictureInPictureRenderState.x0(),
                            pictureInPictureRenderState.y0(),
                            pictureInPictureRenderState.x1(),
                            pictureInPictureRenderState.y1(),
                            0.0F,
                            1.0F,
                            1.0F,
                            0.0F,
                            -1,
                            pictureInPictureRenderState.scissorArea(),
                            null
                    )
            );
            return;
        }

        if (pictureInPictureRenderState.fallbackSkin() != null) {
            Identifier texture = pictureInPictureRenderState.fallbackSkin();
            // render skin
            GpuTextureView gpuTextureView = Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView();
            GpuSampler sampler = Minecraft.getInstance().getTextureManager().getTexture(texture).getSampler();

            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(gpuTextureView, sampler),
                            pictureInPictureRenderState.pose(),
                            pictureInPictureRenderState.x0(),
                            pictureInPictureRenderState.y0(),
                            pictureInPictureRenderState.x1(),
                            pictureInPictureRenderState.y1(),
                            8/64F,
                            16/64.0F,
                            8/64F,
                            16/64.0F,
                            -1,
                            pictureInPictureRenderState.scissorArea(),
                            null
                    )
            );

            // hat
            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(gpuTextureView, sampler),
                            pictureInPictureRenderState.pose(),
                            pictureInPictureRenderState.x0(),
                            pictureInPictureRenderState.y0(),
                            pictureInPictureRenderState.x1(),
                            pictureInPictureRenderState.y1(),
                            40/64F,
                            48/64.0F,
                            8/64F,
                            16/64.0F,
                            -1,
                            pictureInPictureRenderState.scissorArea(),
                            null
                    )
            );
        } else {
            GpuTextureView gpuTextureView = Minecraft.getInstance().getTextureManager().getTexture(PlayerPermPackElement.UNKNOWN).getTextureView();
            GpuSampler sampler = Minecraft.getInstance().getTextureManager().getTexture(PlayerPermPackElement.UNKNOWN).getSampler();
            guiRenderState.addBlitToCurrentLayer(
                    new BlitRenderState(
                            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                            TextureSetup.singleTexture(gpuTextureView, sampler),
                            pictureInPictureRenderState.pose(),
                            pictureInPictureRenderState.x0(),
                            pictureInPictureRenderState.y0(),
                            pictureInPictureRenderState.x1(),
                            pictureInPictureRenderState.y1(),
                            0,
                            1,
                            0,
                            1,
                            -1,
                            pictureInPictureRenderState.scissorArea(),
                            null
                    )
            );
        }
    }

    @Override
    protected String getTextureLabel() {
        return "figura-portrait";
    }

    @Override
    public void close() {
        super.close();
        for (Map.Entry<Avatar, TextureEntry> entry : avatarToTexture.entrySet()) {
            entry.getValue().texture.close();
            entry.getValue().textureView.close();
            entry.getValue().depthTexture.close();
            entry.getValue().depthTextureView.close();
        }
        avatarProjectionMatrixBuffer.close();
    }

    private static final class TextureEntry {
        private GpuTexture texture;
        private GpuTextureView textureView;
        private GpuTexture depthTexture;
        private GpuTextureView depthTextureView;

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (TextureEntry) obj;
            return Objects.equals(this.texture, that.texture) &&
                    Objects.equals(this.textureView, that.textureView) &&
                    Objects.equals(this.depthTexture, that.depthTexture) &&
                    Objects.equals(this.depthTextureView, that.depthTextureView);
        }

        @Override
        public int hashCode() {
            return Objects.hash(texture, textureView, depthTexture, depthTextureView);
        }

        @Override
        public String toString() {
            return "TextureEntry[" +
                    "texture=" + texture + ", " +
                    "textureView=" + textureView + ", " +
                    "depthTexture=" + depthTexture + ", " +
                    "depthTextureView=" + depthTextureView + ']';
        }


    }

}
