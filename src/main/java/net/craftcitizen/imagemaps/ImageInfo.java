package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.Tuple;

import java.io.File;

/**
 * What the plugin needs to know about an image without decoding it: its resolution, plus enough of a stamp to
 * notice that the file changed.
 */
record ImageInfo(int width, int height, long lastModified, long length) {

    boolean matches(File file) {
        return lastModified == file.lastModified() && length == file.length();
    }

    /**
     * The factor the image has to be scaled by to fit into a requested size in maps, keeping its aspect ratio.
     * A requested size of null, or of -1 in both directions, leaves the image unscaled.
     */
    double getScale(Tuple<Integer, Integer> size) {
        if (size == null)
            return 1.0D;

        double scaleX = size.getKey() > 0 ? (double) (size.getKey() * ImageMaps.MAP_WIDTH) / width : Double.MAX_VALUE;
        double scaleY = size.getValue() > 0 ? (double) (size.getValue() * ImageMaps.MAP_HEIGHT) / height
                                            : Double.MAX_VALUE;

        double scale = Math.min(scaleX, scaleY);

        return scale >= Double.MAX_VALUE ? 1.0D : scale;
    }
}
