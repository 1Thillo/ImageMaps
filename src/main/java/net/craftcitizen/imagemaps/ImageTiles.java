package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.Tuple;
import org.bukkit.map.MapPalette;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

/**
 * Cuts source images into map sized tiles and converts them into Minecraft's map colour palette.
 * <p>
 * This is the only place that touches the deprecated {@link MapPalette} conversion methods. They are scheduled
 * for removal from the API, keeping the usage in a single class makes a future replacement a one file change.
 */
final class ImageTiles {
    /** Number of colour indices in a single map, one byte per pixel. */
    static final int TILE_BYTES = ImageMaps.MAP_WIDTH * ImageMaps.MAP_HEIGHT;

    private ImageTiles() {
    }

    /**
     * Builds the colour cache used by {@link MapPalette#matchColor(Color)} on the calling thread.
     * <p>
     * The cache is initialised lazily and its guard is not thread safe: two threads entering it at the same time
     * make one of them fail with "Cache is already build or is currently being build". Triggering it once from the
     * main thread during startup means no later conversion can race it.
     */
    @SuppressWarnings({ "deprecation", "removal" })
    static void warmColorCache() {
        MapPalette.matchColor(Color.WHITE);
    }

    /**
     * The sub-image backing the map at tile position (x|y), or null when the tile is outside of the image.
     */
    static BufferedImage extractTile(BufferedImage input, int x, int y, double scale) {
        if (x * ImageMaps.MAP_WIDTH > Math.round(input.getWidth() * scale)
            || y * ImageMaps.MAP_HEIGHT > Math.round(input.getHeight() * scale))
            return null;

        int x1 = (int) Math.floor(x * ImageMaps.MAP_WIDTH / scale);
        int y1 = (int) Math.floor(y * ImageMaps.MAP_HEIGHT / scale);

        int x2 = (int) Math.ceil(Math.min(input.getWidth(), ((x + 1) * ImageMaps.MAP_WIDTH / scale)));
        int y2 = (int) Math.ceil(Math.min(input.getHeight(), ((y + 1) * ImageMaps.MAP_HEIGHT / scale)));

        if (x2 - x1 <= 0 || y2 - y1 <= 0)
            return null;

        BufferedImage tile = input.getSubimage(x1, y1, x2 - x1, y2 - y1);

        if (scale != 1D) {
            BufferedImage resized = new BufferedImage(ImageMaps.MAP_WIDTH, ImageMaps.MAP_HEIGHT,
                                                      input.getType() == 0 ? tile.getType() : input.getType());
            AffineTransform at = new AffineTransform();
            at.scale(scale, scale);
            AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            tile = scaleOp.filter(tile, resized);
        }

        return tile;
    }

    /**
     * Converts a tile into a full map sized array of colour indices. Pixels not covered by the tile stay
     * transparent, which matches what MapCanvas#drawImage did in previous versions.
     * <p>
     * Safe to call asynchronously once {@link #warmColorCache()} has run.
     */
    @SuppressWarnings({ "deprecation", "removal" })
    static byte[] toMapColors(BufferedImage tile) {
        byte[] colors = new byte[TILE_BYTES];
        byte[] converted = MapPalette.imageToBytes(tile);

        int width = Math.min(tile.getWidth(), ImageMaps.MAP_WIDTH);
        int height = Math.min(tile.getHeight(), ImageMaps.MAP_HEIGHT);

        for (int row = 0; row < height; row++)
            System.arraycopy(converted, row * tile.getWidth(), colors, row * ImageMaps.MAP_WIDTH, width);

        return colors;
    }

    /**
     * The size of an image in maps, given a scale factor.
     */
    static Tuple<Integer, Integer> getTileCount(int width, int height, double scale) {
        int x = (int) ((ImageMaps.MAP_WIDTH - 1 + Math.ceil(width * scale)) / ImageMaps.MAP_WIDTH);
        int y = (int) ((ImageMaps.MAP_HEIGHT - 1 + Math.ceil(height * scale)) / ImageMaps.MAP_HEIGHT);

        return new Tuple<>(x, y);
    }
}
