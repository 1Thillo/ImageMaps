package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ImageMapDeleteCommand extends ImageMapSubCommand {

    public ImageMapDeleteCommand(ImageMaps plugin) {
        super("imagemaps.delete", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "You must specify a file name.");
            return;
        }

        String filename = args[1];

        if (!getPlugin().hasImage(filename)) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "No image with this name exists.");
            return;
        }

        if (getPlugin().deleteImage(filename))
            MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "File deleted.");
        else
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Failed to delete file.");
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Deletes an image.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Usage: /imagemap delete <filename>");
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2)
            return Utils.getMatches(args[1], getPlugin().listImages());

        return Collections.emptyList();
    }
}
