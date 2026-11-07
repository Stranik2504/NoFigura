package org.figuramc.figura.compat.texture;

import net.minecraft.client.Minecraft;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.resources.FiguraRuntimeResources;
import org.figuramc.figura.utils.FiguraResourceListener;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Owns the 1.21.4 legacy texture cache and its lifecycle.
 *
 * <p>The cache lives inside Figura's existing runtime resource pack:
 * {@code .figura/cache/resources/assets/minecraft/...}.</p>
 */
public final class LegacyTexturePack {

    public static final String VERSION = "1.21.4";

    /** Set when any resource reload has completed, including the initial one. */
    private static volatile boolean initialReloadDone;

    public static final FiguraResourceListener RESOURCE_LISTENER =
            FiguraResourceListener.createResourceListener("legacy_textures", manager -> initialReloadDone = true);

    private static volatile CompletableFuture<Void> future;

    private LegacyTexturePack() {
    }

    public static Path root() {
        return FiguraRuntimeResources.getRootDirectory();
    }

    public static Path cacheDirectory() {
        return root().resolve("legacy").resolve(VERSION);
    }

    public static Path manifestPath() {
        return cacheDirectory().resolve("manifest.json");
    }

    public static Path textureRoot() {
        return root().resolve("assets/minecraft");
    }

    public static void init() {
        if (Configs.LEGACY_TEXTURES != null && !Configs.LEGACY_TEXTURES.value)
            return;

        future = LegacyTextureDownloader.ensure();
    }

    static void installed() {
        TextureResolver.clearCache();

        // If the initial resource reload has not happened yet, the assets are
        // already on disk and that reload will pick them up. Do not force a
        // second reload during startup.
        if (!initialReloadDone)
            return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null)
            return;

        minecraft.execute(() -> {
            try {
                minecraft.reloadResourcePacks();
            } catch (Throwable e) {
                FiguraMod.LOGGER.warn("Failed to reload resources after legacy textures were installed", e);
            }
        });
    }
}
