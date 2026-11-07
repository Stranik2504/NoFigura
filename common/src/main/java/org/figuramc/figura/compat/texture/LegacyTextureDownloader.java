package org.figuramc.figura.compat.texture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.figuramc.figura.FiguraMod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads the official 1.21.4 client jar once and extracts only the legacy
 * texture paths listed in legacy_textures.json.
 *
 * <p>No Minecraft assets are redistributed by the mod. The jar is verified
 * against Mojang's published SHA-1, is never executed, and only PNG entries
 * from the allow-list are extracted.</p>
 */
public final class LegacyTextureDownloader {

    private static final String VERSION = LegacyTexturePack.VERSION;
    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final long MAX_CLIENT_JAR_SIZE = 100L * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private LegacyTextureDownloader() {
    }

    public static CompletableFuture<Void> ensure() {
        if (isCacheValid())
            return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            try {
                downloadAndInstall();
            } catch (Throwable e) {
                FiguraMod.LOGGER.warn("Failed to install 1.21.4 legacy textures; using safe texture fallback", e);
            }
        });
    }

    private static boolean isCacheValid() {
        Path manifest = LegacyTexturePack.manifestPath();
        if (!Files.isRegularFile(manifest))
            return false;

        try (InputStream in = Files.newInputStream(manifest)) {
            JsonObject root = JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if (!VERSION.equals(root.get("version").getAsString()))
                return false;

            JsonObject files = root.getAsJsonObject("files");
            List<String> legacy = TextureMappings.legacyPaths();
            if (files == null || files.size() != legacy.size())
                return false;

            for (String oldPath : legacy) {
                JsonElement hash = files.get(oldPath);
                if (hash == null || !Files.isRegularFile(targetFor(oldPath)))
                    return false;
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void downloadAndInstall() throws Exception {
        List<String> legacy = TextureMappings.legacyPaths();
        if (legacy.isEmpty())
            throw new IOException("No legacy texture paths loaded");

        JsonObject versionJson = fetchVersionJson();
        JsonObject client = versionJson.getAsJsonObject("downloads").getAsJsonObject("client");
        String url = client.get("url").getAsString();
        String sha1 = client.get("sha1").getAsString();
        long size = client.get("size").getAsLong();
        if (size <= 0 || size > MAX_CLIENT_JAR_SIZE)
            throw new IOException("Unexpected 1.21.4 client jar size: " + size);

        Path temp = LegacyTexturePack.root().resolve("legacy").resolve(".tmp-" + UUID.randomUUID());
        Path jar = temp.resolve("client.jar");
        Path staging = temp.resolve("staging");
        try {
            Files.createDirectories(temp);
            download(URI.create(url), jar, MAX_CLIENT_JAR_SIZE);
            if (Files.size(jar) != size)
                throw new IOException("1.21.4 client jar size mismatch");
            if (!sha1.equalsIgnoreCase(sha1(jar)))
                throw new IOException("1.21.4 client jar SHA-1 mismatch");

            JsonObject files = new JsonObject();
            int extracted = extract(jar, staging, legacy, files);
            if (extracted == 0)
                throw new IOException("No legacy textures extracted");

            copy(staging.resolve("assets"), LegacyTexturePack.root().resolve("assets"));

            JsonObject manifest = new JsonObject();
            manifest.addProperty("version", VERSION);
            manifest.addProperty("sourceSha1", sha1);
            manifest.addProperty("sourceUrl", url);
            manifest.add("files", files);
            Path manifestTarget = LegacyTexturePack.manifestPath();
            Files.createDirectories(manifestTarget.getParent());
            Path manifestTemp = manifestTarget.resolveSibling("manifest.json.tmp");
            Files.writeString(manifestTemp, manifest.toString());
            try {
                Files.move(manifestTemp, manifestTarget, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(manifestTemp, manifestTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            FiguraMod.LOGGER.info("Installed {} legacy 1.21.4 textures", extracted);
            LegacyTexturePack.installed();
        } finally {
            deleteTree(temp);
        }
    }

    private static JsonObject fetchVersionJson() throws Exception {
        JsonObject manifest = JsonParser.parseString(fetchString(URI.create(VERSION_MANIFEST))).getAsJsonObject();
        for (JsonElement element : manifest.getAsJsonArray("versions")) {
            JsonObject version = element.getAsJsonObject();
            if (VERSION.equals(version.get("id").getAsString()))
                return JsonParser.parseString(fetchString(URI.create(version.get("url").getAsString()))).getAsJsonObject();
        }
        throw new IOException("Could not find " + VERSION + " in Mojang version manifest");
    }

    private static String fetchString(URI uri) throws Exception {
        checkHost(uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .header("Accept", "application/json")
                .header("User-Agent", "NoFigura legacy texture installer")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IOException("HTTP " + response.statusCode() + " from " + uri);
        return response.body();
    }

    private static void download(URI uri, Path target, long maxSize) throws Exception {
        checkHost(uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "NoFigura legacy texture installer")
                .GET()
                .build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200)
            throw new IOException("HTTP " + response.statusCode() + " from " + uri);

        Files.createDirectories(target.getParent());
        long total = 0;
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxSize)
                    throw new IOException("Download exceeded size limit");
                out.write(buffer, 0, read);
            }
        }
    }

    private static int extract(Path jar, Path staging, List<String> legacy, JsonObject files) throws Exception {
        Path minecraft = staging.resolve("assets/minecraft");
        int extracted = 0;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (String oldPath : legacy) {
                if (!oldPath.startsWith("textures/") || oldPath.contains("..") || !oldPath.endsWith(".png"))
                    continue;

                ZipEntry entry = zip.getEntry("assets/minecraft/" + oldPath);
                if (entry == null)
                    continue;

                byte[] bytes;
                try (InputStream in = zip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }
                if (bytes.length < PNG_SIGNATURE.length || !hasPngSignature(bytes))
                    continue;

                Path target = minecraft.resolve(oldPath).normalize();
                if (!target.startsWith(minecraft))
                    continue;

                Files.createDirectories(target.getParent());
                Files.write(target, bytes);
                files.addProperty(oldPath, sha256(bytes));
                extracted++;
            }
        }
        return extracted;
    }

    private static void copy(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source))
            return;
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                if (!Files.isRegularFile(path))
                    continue;
                Path destination = target.resolve(source.relativize(path)).normalize();
                if (!destination.startsWith(target))
                    throw new IOException("Refusing path outside resource root: " + destination);
                Files.createDirectories(destination.getParent());
                Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static Path targetFor(String oldPath) {
        return LegacyTexturePack.textureRoot().resolve(oldPath).normalize();
    }

    private static void checkHost(URI uri) throws IOException {
        if (!"https".equalsIgnoreCase(uri.getScheme()))
            throw new IOException("Refusing non-HTTPS URL: " + uri);
        String host = uri.getHost();
        if (host == null || !(host.equals("piston-meta.mojang.com") || host.equals("piston-data.mojang.com")))
            throw new IOException("Refusing non-Mojang URL: " + uri);
    }

    private static boolean hasPngSignature(byte[] bytes) {
        for (int i = 0; i < PNG_SIGNATURE.length; i++)
            if (bytes[i] != PNG_SIGNATURE[i])
                return false;
        return true;
    }

    private static String sha1(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1)
                digest.update(buffer, 0, read);
        }
        return hex(digest.digest());
    }

    private static String sha256(byte[] bytes) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            out.append(String.format(Locale.ROOT, "%02x", b));
        return out.toString();
    }

    private static void deleteTree(Path root) {
        try {
            if (!Files.exists(root))
                return;
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
        }
    }
}
