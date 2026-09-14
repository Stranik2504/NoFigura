package org.figuramc.figura.mixin.render.feature;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.CameraRenderStateExtension;
import org.figuramc.figura.lua.api.nameplate.EntityNameplateCustomization;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.TextUtils;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionSubmitRedirectMixin {

    // i literally have to inject a new submission list for outlined text, *screams into void*
    @Unique private Avatar figura$avatar;
    @Unique private EntityNameplateCustomization figura$custom;
    @Unique private boolean figura$hasCustomNameplate;
    @Unique private boolean figura$enabled;

    @Inject(method = "submitNameTag", at = @At("HEAD"))
    private void figura$setupAvatar(PoseStack poseStack, Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera, CallbackInfo ci) {
        figura$avatar = ((CameraRenderStateExtension) camera).figura$getAvatar();
        ((CameraRenderStateExtension) camera).figura$setAvatar(null);

        if (figura$avatar == null) {
            figura$custom = null;
            figura$hasCustomNameplate = false;
            figura$enabled = false;
            return;
        }

        figura$custom = figura$avatar.luaRuntime == null ? null : figura$avatar.luaRuntime.nameplate.ENTITY;
        figura$hasCustomNameplate = figura$custom != null && figura$avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1;
        figura$enabled = Configs.ENTITY_NAMEPLATE.value > 0 && !AvatarManager.panic && figura$hasCustomNameplate;
    }

    @Inject(method = "submitNameTag", at = @At("TAIL"))
    private void figura$clearAvatar(CallbackInfo ci) {
        figura$avatar = null;
        figura$custom = null;
        figura$hasCustomNameplate = false;
        figura$enabled = false;
    }

    // Push pivot transformations when the nametag is being pivoted (set to entity height in vanilla)
    @WrapOperation(method = "submitNameTag",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    private void figura$modifyPivot(PoseStack instance, double x, double y, double z, Operation<Void> original) {
        FiguraVec3 pivot = FiguraVec3.of(x, y, z);

        if (figura$enabled && figura$hasCustomNameplate && figura$custom.getPivot() != null) {
            FiguraMod.pushProfiler("pivot");
            pivot = figura$custom.getPivot();
        }

        original.call(instance, pivot.x, pivot.y, pivot.z);
    }


    // Push position transformations after the nametag has been rotated to face the camera
    @Inject(method = "submitNameTag",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotate(Lorg/joml/Quaternionfc;)V", shift = At.Shift.AFTER))
    private void figura$modifyPos(PoseStack poseStack, Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera, CallbackInfo ci) {
        if (figura$enabled && figura$hasCustomNameplate && figura$custom.getPos() != null) {
            FiguraMod.popPushProfiler("position");
            FiguraVec3 pos = figura$custom.getPos();
            poseStack.translate(pos.x, pos.y, pos.z);
        }
    }

    @WrapOperation(method = "submitNameTag",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void figura$modifyScale(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        FiguraVec3 scaleVec = FiguraVec3.of(x, y, z);

        if (figura$enabled && figura$hasCustomNameplate && figura$custom.getScale() != null) {
            FiguraMod.popPushProfiler("scale");
            scaleVec.multiply(figura$custom.getScale());
        }

        original.call(instance, (float) scaleVec.x, (float) scaleVec.y, (float) scaleVec.z);
    }

    @WrapOperation(method = "submitNameTag",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;submitNameTagPart(Lnet/minecraft/client/renderer/feature/TextFeatureRenderer$Submit;)V"))
    private void figura$redirectSolid(SubmitNodeCollection instance, TextFeatureRenderer.Submit submit, Operation<Void> original,
                                      @Local(argsOnly = true) Component name) {
        figura$handle(submit, name, s -> original.call(instance, s));
    }

    // see-through ветка — перехватываем прямой вызов seeThrough.submit(...)
    @WrapOperation(method = "submitNameTag",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/TranslucentSubmit;)V"))
    private void figura$redirectSeeThrough(TranslucentFeatureRenderPhase instance, TranslucentSubmit submit, Operation<Void> original,
                                           @Local(argsOnly = true) Component name) {
        figura$handle((TextFeatureRenderer.Submit) submit, name, s -> original.call(instance, s));
    }

    private void figura$handle(TextFeatureRenderer.Submit submit, Component originalName, Consumer<TextFeatureRenderer.Submit> submitFn) {
        if (!figura$enabled) {
            submitFn.accept(submit);
            return;
        }

        TextFeatureRenderer.Content.Text content = (TextFeatureRenderer.Content.Text) submit.content();

        Font font = Minecraft.getInstance().font;
        int light = figura$custom.light != null ? figura$custom.light : submit.lightCoords();
        int backgroundColor = figura$custom.background != null ? figura$custom.background : content.backgroundColor();
        int outlineColor = content.outlineColor();
        if (figura$custom.outline)
            outlineColor = figura$custom.outlineColor != null ? figura$custom.outlineColor : 0x202020;

        boolean deadmau = originalName.getString().equals("deadmau5");
        List<Component> lines = TextUtils.splitText(originalName, "\n");

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (line.getString().isEmpty())
                continue;

            int lineOffset = i - lines.size() + 1;
            float x = -font.width(line) / 2f;
            float y = (deadmau ? -10f : 0f) + (font.lineHeight + 1) * lineOffset;

            TextFeatureRenderer.Content.Text lineContent = new TextFeatureRenderer.Content.Text(
                    x, y, line.getVisualOrderText(), content.dropShadow(), content.color(), backgroundColor, outlineColor
            );

            submitFn.accept(new TextFeatureRenderer.Submit(submit.pose(), submit.displayMode(), light, lineContent));
        }
    }
}
