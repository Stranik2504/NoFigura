package org.figuramc.figura.compat.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.figuramc.figura.utils.ResourceUtils;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves legacy avatar texture paths against 26.3 resources. */
public final class TextureResolver {

    private static final byte[] WHITE_64X32 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAEAAAAAgCAYAAACinX6EAAAAPElEQVR42u3QAQEAAAgCIP+f1h8FE0ifiwABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIEHDfACHE4dJh9HFRAAAAAElFTkSuQmCC");

    private static final Map<Identifier, Identifier> RESOURCE_CACHE = new ConcurrentHashMap<>();
    private static volatile ResourceManager cachedManager;

    private TextureResolver() {
    }

    public static void clearCache() {
        synchronized (TextureResolver.class) {
            RESOURCE_CACHE.clear();
            cachedManager = null;
        }
    }

    /** Bytes for a legacy model texture, or null if there is no fallback. */
    public static byte[] modelTexture(String name) {
        String n = name.endsWith(".png") ? name.substring(0, name.length() - 4) : name;
        if (n.equals("white"))
            return WHITE_64X32;

        String path = TextureMappings.modelTexturePath(name);
        if (path == null || path.equals("__WHITE__"))
            return null;

        try {
            return ResourceUtils.getResource(
                    Minecraft.getInstance().getResourceManager(),
                    Identifier.withDefaultNamespace(path)
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Maps a runtime RESOURCE id to a 26.3 path when the old one is absent. */
    public static Identifier resolveResource(Identifier id) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager == null)
            return id;

        if (manager != cachedManager) {
            synchronized (TextureResolver.class) {
                if (manager != cachedManager) {
                    RESOURCE_CACHE.clear();
                    cachedManager = manager;
                }
            }
        }

        return RESOURCE_CACHE.computeIfAbsent(id, key -> resolveUncached(key, manager));
    }

    private static Identifier resolveUncached(Identifier id, ResourceManager manager) {
        try {
            if (manager.getResource(id).isPresent())
                return id;

            String alias = TextureMappings.mapPath(id.getPath());
            if (alias != null) {
                Identifier candidate = Identifier.fromNamespaceAndPath(id.getNamespace(), alias);
                if (manager.getResource(candidate).isPresent())
                    return candidate;
            }
        } catch (Throwable ignored) {
        }
        return id;
    }
}
