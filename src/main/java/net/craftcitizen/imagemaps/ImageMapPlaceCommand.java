package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Tuple;
import net.craftcitizen.imagemaps.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImageMapPlaceCommand extends ImageMapSubCommand {

    public ImageMapPlaceCommand(ImageMaps plugin) {
        super("imagemaps.place", plugin, false);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "You must specify a file name.");
            return;
        }

        String filename = args[1];
        boolean isInvisible = args.length >= 3 && Boolean.parseBoolean(args[2]);
        boolean isFixed = args.length >= 4 && Boolean.parseBoolean(args[3]);
        boolean isGlowing = args.length >= 5 && Boolean.parseBoolean(args[4]);
        Tuple<Integer, Integer> scale = args.length >= 6 ? parseScale(args[5]) : new Tuple<>(-1, -1);

        if (!getPlugin().hasImage(filename)) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "No image with this name exists.");
            return;
        }

        getPlugin().startPlacement((Player) sender,
                                   new PlacementData(filename, isInvisible, isFixed, isGlowing, scale));

        Tuple<Integer, Integer> size = getPlugin().getImageSize(filename, scale);
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                String.format("Started placing of %s. It needs a %d by %d area.", filename,
                                              size.getKey(), size.getValue()));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Right click on the block, that should be the upper left corner.");
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Starts placing an image.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO,
                                "Usage: /imagemap place <filename> [frameInvisible] [frameFixed] [frameGlowing] [size]");
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Size format: XxY -> 5x2, use -1 for default");
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "The plugin will scale the map to not be larger than the given size while maintaining the aspect ratio.");
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "It's recommended to avoid the size function in favor of using properly sized source images.");
    }

    private static Tuple<Integer, Integer> parseScale(String string) {
        String[] tmp = string.split("x");

        if (tmp.length < 2)
            return new Tuple<>(-1, -1);

        return new Tuple<>(Utils.parseIntegerOrDefault(tmp[0], -1), Utils.parseIntegerOrDefault(tmp[1], -1));
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 2:
                return Utils.getMatches(args[1], getPlugin().listImages());
            case 3:
                return Utils.getMatches(args[2], Arrays.asList("true", "false"));
            case 4:
                return Utils.getMatches(args[3], Arrays.asList("true", "false"));
            case 5:
                return Utils.getMatches(args[4], Arrays.asList("true", "false"));
            default:
                return Collections.emptyList();
        }
    }
}
