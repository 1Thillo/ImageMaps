package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Converts images into map colours ahead of time.
 * <p>
 * Nothing needs this: a map converts itself the first time a player looks at it. Running it once after upgrading,
 * or after dropping new files into the images folder, just means that first look does not have to wait for it.
 */
public class ImageMapBakeCommand extends ImageMapSubCommand {

    public ImageMapBakeCommand(ImageMaps plugin) {
        super("imagemaps.admin", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        // The placement data lives on the main thread, so the work list is built here and only the conversion
        // itself is handed off.
        Map<String, Set<Double>> work = new LinkedHashMap<>();

        if (args.length >= 2) {
            String filename = args[1];

            if (!getPlugin().hasImage(filename)) {
                MessageUtil.sendMessage(sender, MessageLevel.WARNING, "No image with this name exists.");
                return;
            }

            work.put(filename, getPlugin().getUsedScales(filename));
        }
        else
            for (String filename : getPlugin().getPlacedImages())
                work.put(filename, getPlugin().getUsedScales(filename));

        if (work.isEmpty()) {
            MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "There is nothing to convert.");
            return;
        }

        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                String.format("Converting %d image(s) in the background...", work.size()));

        getPlugin().getServer().getAsyncScheduler().runNow(getPlugin(), task -> bake(sender, work));
    }

    private void bake(CommandSender sender, Map<String, Set<Double>> work) {
        int tiles = 0;
        List<String> failed = new ArrayList<>();

        for (Entry<String, Set<Double>> entry : work.entrySet()) {
            boolean ok = false;

            for (double scale : entry.getValue()) {
                int written = getPlugin().getTileCache().bake(entry.getKey(), scale);

                if (written >= 0) {
                    tiles += written;
                    ok = true;
                }
            }

            if (!ok)
                failed.add(entry.getKey());
        }

        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                String.format("Converted %d map(s) of %d image(s).", tiles,
                                              work.size() - failed.size()));

        if (!failed.isEmpty())
            MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                    "Could not read: " + String.join(", ", failed));
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Converts images to map colours ahead of time, so nothing has to be converted while players are looking.");
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Without a file name it converts every image that is currently placed.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Usage: /imagemap bake [filename]");
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2)
            return Utils.getMatches(args[1], getPlugin().listImages());

        return Collections.emptyList();
    }
}
