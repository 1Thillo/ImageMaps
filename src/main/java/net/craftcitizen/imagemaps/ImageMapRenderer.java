package net.craftcitizen.imagemaps;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * Draws a single map of an image, exactly once.
 * <p>
 * The renderer holds no image data of its own. The first time the server asks it to render, it requests the
 * converted tile from the {@link MapTileCache} on an async thread and draws it on the next render call, after
 * which the reference is dropped again. A renderer therefore costs a handful of bytes no matter how large the
 * source image is.
 * <p>
 * Drawing happens inside {@link #render(MapView, MapCanvas, Player)} rather than from a scheduled task. Writing
 * to a canvas outside of a render call is what made images stay blank until a player rejoined.
 */
public class ImageMapRenderer extends MapRenderer {
    private enum State {
        /** Nothing requested yet. */
        NEW,
        /** Tile is being read/converted on an async thread. */
        LOADING,
        /** Tile is available and waiting to be drawn. */
        PENDING,
        /** Tile has been drawn, nothing left to do. */
        DRAWN,
        /** Tile could not be loaded, do not retry until the image is reloaded. */
        FAILED
    }

    private final ImageMaps plugin;
    private final String filename;
    private final int x;
    private final int y;
    private final double scale;

    private volatile byte[] colors;
    private volatile State state = State.NEW;

    public ImageMapRenderer(ImageMaps plugin, String filename, int x, int y, double scale) {
        this.plugin = plugin;
        this.filename = filename;
        this.x = x;
        this.y = y;
        this.scale = scale;
    }

    public String getFilename() {
        return filename;
    }

    /**
     * Discards the drawn state, so the tile is loaded and drawn again on the next render. To be called after the
     * source image changed.
     */
    public void invalidate() {
        colors = null;
        state = State.NEW;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        switch (state) {
            case NEW:
                state = State.LOADING;
                plugin.getServer().getAsyncScheduler().runNow(plugin, task -> load());
                return;
            case PENDING:
                draw(canvas);
                return;
            default:
                // LOADING, DRAWN and FAILED have nothing to do, the canvas keeps what was drawn before.
        }
    }

    private void load() {
        byte[] loaded = plugin.getTileCache().getTile(filename, x, y, scale);

        if (loaded == null) {
            plugin.getLogger().warning(() -> String.format("Could not load tile %d/%d of image %s.", x, y, filename));
            state = State.FAILED;
            return;
        }

        colors = loaded;
        state = State.PENDING;
    }

    @SuppressWarnings("deprecation") // MapCanvas#setPixel takes a colour index, which is exactly what we cached
    private void draw(MapCanvas canvas) {
        byte[] data = colors;

        colors = null;
        state = State.DRAWN;

        if (data == null)
            return;

        // setPixel only marks changed pixels dirty, so redrawing an unchanged tile sends no update packets.
        for (int i = 0; i < data.length; i++)
            canvas.setPixel(i % ImageMaps.MAP_WIDTH, i / ImageMaps.MAP_WIDTH, data[i]);
    }
}
