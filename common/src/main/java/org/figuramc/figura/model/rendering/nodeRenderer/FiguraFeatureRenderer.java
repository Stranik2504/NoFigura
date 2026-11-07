package org.figuramc.figura.model.rendering.nodeRenderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
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
            drawBuffered(submit.primaryBuffers());
            drawBuffered(submit.secondaryBuffers());

            if (submit.outlineColor() != 0) {
                drawOutline(submit.primaryBuffers(), submit.outlineColor());
                drawOutline(submit.secondaryBuffers(), submit.outlineColor());
            }

            for (FiguraSubmission.QueuedRenderTask qt : submit.renderTasks()) {
                boolean hudText = HudRenderContext.isHudPass() && qt.task() instanceof TextTask;

                if (hudText)
                    HudRenderContext.beginTextPass();

                try {
                    qt.task().render(qt.poseStack(), this::getVertexBuilder, qt.light(), qt.overlay());
                }
                finally {
                    if (hudText) HudRenderContext.endTextPass();
                }
            }

            for (FiguraSubmission.QueuedPivotBox qb : submit.pivotBoxes()) {
                VertexConsumer vc = this.getVertexBuilder(RenderTypes.LINES);

                ImmediateFiguraRenderer.renderLineBox(qb.pose(), vc,
                        qb.x1(), qb.y1(), qb.z1(), qb.x2(), qb.y2(), qb.z2(),
                        qb.r(), qb.g(), qb.b(), qb.a());
            }
        }
    }

    private void drawBuffered(Map<RenderType, List<Consumer<VertexConsumer>>> buffers) {
        for (Map.Entry<RenderType, List<Consumer<VertexConsumer>>> entry : buffers.entrySet()) {
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
