package org.figuramc.figura.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.figuramc.figura.model.FiguraVertexConsumerProvider;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraSubmission;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FiguraSubmitUtils {
    public static void submit(SubmitNodeCollector collector, FiguraSubmission submission) {
        if (submission == null) return;
        if (collector instanceof SubmitNodeStorage storage) {
            SubmitNodeCollection collection = storage.order(0);
            collection.solid.submit(submission.withPass(FiguraSubmission.Pass.SOLID));
            collection.translucentCustomGeometry.submit(submission.withPass(FiguraSubmission.Pass.TRANSLUCENT));

            if (!submission.renderTasks().isEmpty())
                storage.seeThrough().submit(submission.withPass(FiguraSubmission.Pass.SEE_THROUGH));

            if (submission.outlineColor() != 0)
                collection.outline.submit(submission.withPass(FiguraSubmission.Pass.OUTLINE));
        }
    }

    public static void drawImmediate(FiguraVertexConsumerProvider provider, Map<RenderType, List<Consumer<VertexConsumer>>> buffers) {
        for (var entry : buffers.entrySet()) {
            VertexConsumer vc = provider.getBuffer(entry.getKey());

            for (var action : entry.getValue())
                action.accept(vc);
        }
    }
}
