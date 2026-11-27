package org.figuramc.figura.model.rendering.nodeRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.BatchableSubmit;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.figuramc.figura.model.rendertasks.RenderTask;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public record FiguraSubmission(
    Map<RenderType, List<Consumer<VertexConsumer>>> primaryBuffers,
    Map<RenderType, List<Consumer<VertexConsumer>>> secondaryBuffers,
    List<QueuedRenderTask> renderTasks,
    List<QueuedPivotBox> pivotBoxes,
    int outlineColor,
    Pass pass
) implements SubmitNode, BatchableSubmit, TranslucentSubmit {
    public static final FeatureRendererType<FiguraSubmission> TYPE = FeatureRendererType.create("figura:avatar_submission");

    public enum Pass { SOLID, TRANSLUCENT, SEE_THROUGH, OUTLINE }

    public FiguraSubmission(Map<RenderType, List<Consumer<VertexConsumer>>> primaryBuffers,
                            Map<RenderType, List<Consumer<VertexConsumer>>> secondaryBuffers,
                            List<QueuedRenderTask> renderTasks,
                            List<QueuedPivotBox> pivotBoxes,
                            int outlineColor) {
        this(primaryBuffers, secondaryBuffers, renderTasks, pivotBoxes, outlineColor, Pass.SOLID);
    }

    public FiguraSubmission withPass(Pass pass) {
        return new FiguraSubmission(primaryBuffers, secondaryBuffers, renderTasks, pivotBoxes, outlineColor, pass);
    }

    @Override
    public float distanceToCameraSq() {
        return 0f;
    }

    @Override
    public Object batchKey() {
        return null;
    }

    @Override
    public @NonNull FeatureRendererType<FiguraSubmission> featureType() {
        return TYPE;
    }

    public record QueuedRenderTask(RenderTask task, PoseStack poseStack, int light, int overlay) {}

    public record QueuedPivotBox(PoseStack.Pose pose,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a) {}
}
