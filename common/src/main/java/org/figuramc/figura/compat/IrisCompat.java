package org.figuramc.figura.compat;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.irisshaders.iris.pipeline.IrisPipelines;
import org.figuramc.figura.model.rendering.texture.FiguraRenderTypes;

public class IrisCompat {
    public static void assignPipelinesToIrisPrograms() {
        IrisApi.getInstance().assignPipeline(FiguraRenderTypes.FiguraRenderPipelines.FIGURA_SOLID, IrisProgram.ENTITIES);
    }

    public static void copyPipeline(RenderPipeline base, RenderPipeline variant) {
        IrisPipelines.copyPipeline(base, variant);
    }
}
