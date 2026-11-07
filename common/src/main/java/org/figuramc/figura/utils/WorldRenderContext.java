package org.figuramc.figura.utils;

/**
  * Per-thread marker for the level entity submission pass.
  *
  * <p>26.3 builds the avatar model submission inside
  * {@code LevelRenderer.submitEntities}; the GUI/HUD/PIP paths run outside it.
  * Used to scope world-only material decisions (e.g. fullbright materials).</p>
  */
public final class WorldRenderContext {

    private static final ThreadLocal<Boolean> WORLD_ENTITY_PASS = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> SKULL_BLOCK_RENDER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private WorldRenderContext() {
    }

    public static void beginWorldEntityPass() {
        WORLD_ENTITY_PASS.set(Boolean.TRUE);
    }

    public static void endWorldEntityPass() {
        WORLD_ENTITY_PASS.set(Boolean.FALSE);
    }

    public static boolean isWorldEntityPass() {
        return WORLD_ENTITY_PASS.get();
    }

    /**
      * Set while a placed player-head block ({@code SkullRenderMode.BLOCK}) renders
      * its Figura model. Placed skulls are block entities and are not part of the
      * level entity submission, so they need their own scope for world-only material
      * decisions. GUI/item/head-item skull renders are intentionally excluded.
      */
    public static void beginSkullBlockRender() {
        SKULL_BLOCK_RENDER.set(Boolean.TRUE);
    }

    public static void endSkullBlockRender() {
        SKULL_BLOCK_RENDER.set(Boolean.FALSE);
    }

    public static boolean isSkullBlockRender() {
        return SKULL_BLOCK_RENDER.get();
    }
}