package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Tuple;
import net.craftcitizen.imagemaps.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ImageMapInfoCommand extends ImageMapSubCommand {

    public ImageMapInfoCommand(ImageMaps plugin) {
        super("imagemaps.info", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "You must specify a file name.");
            return;
        }

        String filename = args[1];
        ImageInfo info = getPlugin().getImageInfo(filename);

        if (info == null) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "No image with this name exists.");
            return;
        }

        Tuple<Integer, Integer> size = getPlugin().getImageSize(filename, null);

        Component actions = Component.text("Action:")
                                     .append(ImageMapListCommand.action(" [Reload]", NamedTextColor.GOLD, "reload",
                                                                        filename))
                                     .append(ImageMapListCommand.action(" [Place]", NamedTextColor.GOLD, "place",
                                                                        filename))
                                     .append(ImageMapListCommand.action(" [Delete]", NamedTextColor.RED, "delete",
                                                                        filename));

        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Image Information: ");
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, String.format("File Name: %s", filename));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                String.format("Resolution: %dx%d", info.width(), info.height()));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                String.format("Ingame Size: %dx%d", size.getKey(), size.getValue()));
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, actions);
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Displays information about an image.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Usage: /imagemap info <filename>");
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2)
            return Utils.getMatches(args[1], getPlugin().listImages());

        return Collections.emptyList();
    }
}
