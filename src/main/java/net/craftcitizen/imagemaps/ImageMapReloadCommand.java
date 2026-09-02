package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ImageMapReloadCommand extends ImageMapSubCommand {

    public ImageMapReloadCommand(ImageMaps plugin) {
        super("imagemaps.reload", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "You must specify a file name.");
            return;
        }

        if (getPlugin().reloadImage(args[1]))
            MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Image reloaded.");
        else
            MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                    "Image couldn't be reloaded (does it exist?).");
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Reloads an image from disk, to be used when the file changed.");
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Avoid resolution changes, since they won't be scaled.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Usage: /imagemap reload <filename>");
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2)
            return Utils.getMatches(args[1], getPlugin().listImages());

        return Collections.emptyList();
    }
}
