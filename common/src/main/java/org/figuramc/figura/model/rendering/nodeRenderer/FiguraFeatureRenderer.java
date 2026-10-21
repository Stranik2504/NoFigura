package org.figuramc.figura.model.rendering.nodeRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.figuramc.figura.ducks.NodeCollectorExtension;

import java.util.List;

public class FiguraFeatureRenderer {
    public void render(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource) {
        List<FiguraSubmission> figuraSubmissions = ((NodeCollectorExtension) submitNodeCollection).getFiguraSubmissions();

        for (int i = 0; i < figuraSubmissions.size(); i++) {
            FiguraSubmission figuraSubmission = figuraSubmissions.get(i);

            if (figuraSubmission.avatar() == null)
                continue;

            figuraSubmission.renderer().apply(
                    figuraSubmission.avatar(),
                    figuraSubmission.renderState(),
                    bufferSource
            );
        }
    }
}
