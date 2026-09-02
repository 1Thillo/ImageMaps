package net.craftcitizen.imagemaps;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * An on-disk cache of map sized colour index tiles.
 * <p>
 * Converting a source image is expensive, but its result never changes as long as the file does not. Previous
 * versions therefore kept every source image decoded in memory for the lifetime of the server, which is what made
 * the plugin scale with the total resolution of all placed images. Instead the conversion result is stored next to
 * the images as one 16 KiB file per map. Nothing but the tile currently being handed to a renderer is held in
 * memory.
 * <p>
 * All methods are safe to call asynchronously.
 */
class MapTileCache {
    private static final int MAGIC = 0x494D4331;
    private static final String CACHE_DIR = "cache";
    private static final String SUFFIX = ".mapcolor";

    private final ImageMaps plugin;
    private final ConcurrentMap<String, Object> bakeLocks = new ConcurrentHashMap<>();

    MapTileCache(ImageMaps plugin) {
        this.plugin = plugin;
    }

    /**
     * The colour indices of a single map, converting the source image first if needed.
     *
     * @return the colour indices of the tile, or null if the image is gone or the tile lies outside of it
     */
    byte[] getTile(String filename, int x, int y, double scale) {
        File source = plugin.getImageFile(filename);

        if (source == null || !source.isFile())
            return null;

        byte[] cached = read(filename, x, y, scale, source);

        if (cached != null)
            return cached;

        // Only one thread may decode a given image, otherwise every tile of it decodes its own copy.
        Object lock = bakeLocks.computeIfAbsent(bakeKey(filename, scale), a -> new Object());

        synchronized (lock) {
            cached = read(filename, x, y, scale, source);

            if (cached != null)
                return cached;

            bake(filename, scale, source);
            return read(filename, x, y, scale, source);
        }
    }

    /**
     * Converts every tile of an image and writes the result to the cache.
     *
     * @return the number of tiles written, or -1 if the image could not be read
     */
    int bake(String filename, double scale) {
        File source = plugin.getImageFile(filename);

        if (source == null || !source.isFile())
            return -1;

        Object lock = bakeLocks.computeIfAbsent(bakeKey(filename, scale), a -> new Object());

        synchronized (lock) {
            return bake(filename, scale, source);
        }
    }

    private int bake(String filename, double scale, File source) {
        BufferedImage image = null;

        try {
            image = ImageIO.read(source);
        }
        catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
                                   String.format("Error while trying to read image %s.", source.getName()), e);
        }

        if (image == null) {
            plugin.getLogger().log(Level.WARNING,
                                   () -> String.format("Failed to read file as image %s.", source.getName()));
            return -1;
        }

        int written = 0;

        try {
            for (int x = 0; ImageTiles.extractTile(image, x, 0, scale) != null; x++)
                for (int y = 0;; y++) {
                    BufferedImage tile = ImageTiles.extractTile(image, x, y, scale);

                    if (tile == null)
                        break;

                    if (write(filename, x, y, scale, source, ImageTiles.toMapColors(tile)))
                        written++;
                }
        }
        finally {
            // The decoded image is deliberately not kept anywhere, it can be collected as soon as we return.
            image.flush();
        }

        return written;
    }

    /**
     * Drops every cached tile of an image so the next access converts it again. To be called after the file changed.
     */
    void invalidate(String filename) {
        File[] files = getCacheDir().listFiles();

        if (files == null)
            return;

        String prefix = hash(filename) + "-";

        for (File file : files)
            if (file.getName().startsWith(prefix) && file.getName().endsWith(SUFFIX) && !file.delete())
                plugin.getLogger().warning(() -> "Failed to delete cache file " + file.getName());
    }

    private byte[] read(String filename, int x, int y, double scale, File source) {
        File file = getCacheFile(filename, x, y, scale);

        if (!file.isFile())
            return null;

        try (InputStream in = Files.newInputStream(file.toPath());
             DataInputStream data = new DataInputStream(in)) {
            if (data.readInt() != MAGIC || data.readLong() != source.lastModified()
                || data.readLong() != source.length())
                return null;

            byte[] colors = new byte[ImageTiles.TILE_BYTES];
            data.readFully(colors);
            return colors;
        }
        catch (IOException e) {
            // A truncated or unreadable cache file is not fatal, it gets rebuilt from the source image.
            return null;
        }
    }

    private boolean write(String filename, int x, int y, double scale, File source, byte[] colors) {
        File file = getCacheFile(filename, x, y, scale);
        Path temp = file.toPath().resolveSibling(file.getName() + ".tmp");

        try {
            Files.createDirectories(file.getParentFile().toPath());

            try (OutputStream out = Files.newOutputStream(temp);
                 DataOutputStream data = new DataOutputStream(out)) {
                data.writeInt(MAGIC);
                data.writeLong(source.lastModified());
                data.writeLong(source.length());
                data.write(colors);
            }

            Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write map colour cache for " + filename, e);
            return false;
        }
    }

    private File getCacheDir() {
        return new File(plugin.getDataFolder(), CACHE_DIR);
    }

    private File getCacheFile(String filename, int x, int y, double scale) {
        return new File(getCacheDir(), String.format(Locale.ROOT, "%s-%s-%d-%d%s", hash(filename),
                                                     Long.toHexString(Double.doubleToLongBits(scale)), x, y, SUFFIX));
    }

    private static String bakeKey(String filename, double scale) {
        return filename.toLowerCase(Locale.ROOT) + '|' + scale;
    }

    /**
     * A file system safe, stable identifier for an image name. Image names may contain spaces, unicode, or
     * characters that differ only in case, none of which make for good file names.
     */
    private static String hash(String filename) {
        byte[] digest;

        try {
            digest = MessageDigest.getInstance("SHA-1")
                                  .digest(filename.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is required to be available", e);
        }

        StringBuilder builder = new StringBuilder(digest.length * 2);

        for (byte value : digest)
            builder.append(Character.forDigit((value >> 4) & 0xF, 16)).append(Character.forDigit(value & 0xF, 16));

        return builder.toString();
    }
}
