package org.figuramc.figura.model.rendering.nodeRenderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.figuramc.figura.model.rendering.ImmediateFiguraRenderer;
import org.figuramc.figura.model.rendertasks.TextTask;
import org.figuramc.figura.utils.HudRenderContext;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FiguraFeatureRenderer extends RenderTypeFeatureRenderer<FiguraSubmission> {
    public static final FeatureRendererType<FiguraSubmission> TYPE = FiguraSubmission.TYPE;

    @Override
    protected void buildGroup(@NonNull FeatureFrameContext context, List<FiguraSubmission> submits) {
        for (FiguraSubmission submit : submits) {
            FiguraSubmission.Pass pass = submit.pass();

            if (pass == FiguraSubmission.Pass.OUTLINE) {
                drawOutline(submit.primaryBuffers(), submit.outlineColor());
                drawOutline(submit.secondaryBuffers(), submit.outlineColor());
                continue;
            }

            drawBuffered(submit.primaryBuffers(), pass);
            drawBuffered(submit.secondaryBuffers(), pass);

            for (FiguraSubmission.QueuedRenderTask qt : submit.renderTasks()) {
                boolean hudText = HudRenderContext.isHudPass() && qt.task() instanceof TextTask;

                if (hudText)
                    HudRenderContext.beginTextPass();

                qt.poseStack().pushPose();
                try {
                    qt.task().render(qt.poseStack(), type -> builderFor(type, pass), qt.light(), qt.overlay());
                }
                finally {
                    qt.poseStack().popPose();
                    if (hudText) HudRenderContext.endTextPass();
                }
            }

            if (passOf(RenderTypes.LINES) != pass) continue;

            for (FiguraSubmission.QueuedPivotBox qb : submit.pivotBoxes()) {
                VertexConsumer vc = this.getVertexBuilder(RenderTypes.LINES);

                ImmediateFiguraRenderer.renderLineBox(qb.pose(), vc,
                        qb.x1(), qb.y1(), qb.z1(), qb.x2(), qb.y2(), qb.z2(),
                        qb.r(), qb.g(), qb.b(), qb.a());
            }
        }
    }

    private static FiguraSubmission.Pass passOf(RenderType type) {
        var pipeline = type.pipeline();
        if (pipeline == RenderPipelines.TEXT_SEE_THROUGH || pipeline == RenderPipelines.TEXT_GRAYSCALE_SEE_THROUGH)
            return FiguraSubmission.Pass.SEE_THROUGH;

        if (type.forceSolidModelPhase() || !type.hasBlending())
            return FiguraSubmission.Pass.SOLID;

        return FiguraSubmission.Pass.TRANSLUCENT;
    }

    private VertexConsumer builderFor(RenderType type, FiguraSubmission.Pass pass) {
        return passOf(type) == pass ? this.getVertexBuilder(type) : NoopVertexConsumer.INSTANCE;
    }

    private void drawBuffered(Map<RenderType, List<Consumer<VertexConsumer>>> buffers, FiguraSubmission.Pass pass) {
        for (Map.Entry<RenderType, List<Consumer<VertexConsumer>>> entry : buffers.entrySet()) {
            if (passOf(entry.getKey()) != pass) continue;

            VertexConsumer vc = this.getVertexBuilder(entry.getKey());

            for (Consumer<VertexConsumer> action : entry.getValue())
                action.accept(vc);
        }
    }

    private void drawOutline(Map<RenderType, List<Consumer<VertexConsumer>>> buffers, int color) {
        for (var entry : buffers.entrySet()) {
            var outlineType = entry.getKey().outline();
            if (outlineType.isEmpty()) continue;
            VertexConsumer real = this.getVertexBuilder(outlineType.get());
            VertexConsumer wrapped = new ConstantColorVertexConsumer(real, color);
            for (var action : entry.getValue())
                action.accept(wrapped);
        }
    }
}
