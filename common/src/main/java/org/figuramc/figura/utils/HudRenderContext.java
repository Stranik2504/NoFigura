package org.figuramc.figura.utils;

/**
 * Per-thread context for the HUD PIP pass.
 *
 * <p>The legacy HUD overlay threshold is expressed in model units, but the
 * vertices reaching {@code ImmediateFiguraRenderer.pushToBuffer} have already
 * been multiplied by {@code guiScale * state.scale()}. Store that factor here
 * so the threshold does not shrink (and eventually clip normal UI parts) on
 * high-GUI-scale displays.</p>
 */
public final class HudRenderContext {

    private static final ThreadLocal<Double> SCALE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> HUD_PASS = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> TEXT_PASS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private HudRenderContext() {
    }

    public static void setScale(double scale) {
        SCALE.set(scale);
    }

    public static double scale() {
        Double scale = SCALE.get();
        return scale == null ? 1.0 : scale;
    }

    public static void clear() {
        SCALE.remove();
    }

    public static void beginHudPass() {
        HUD_PASS.set(Boolean.TRUE);
    }

    public static void endHudPass() {
        HUD_PASS.set(Boolean.FALSE);
    }

    public static boolean isHudPass() {
        return HUD_PASS.get();
    }

    public static void beginTextPass() {
        TEXT_PASS.set(Boolean.TRUE);
    }

    public static void endTextPass() {
        TEXT_PASS.set(Boolean.FALSE);
    }

    public static boolean isTextPass() {
        return TEXT_PASS.get();
    }
}
