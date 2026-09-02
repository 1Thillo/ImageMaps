package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ImageMapCleanupCommand extends ImageMapSubCommand {

    public ImageMapCleanupCommand(ImageMaps plugin) {
        super("imagemaps.admin", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        int removedMaps = getPlugin().cleanupMaps();
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Removed " + removedMaps + " invalid images/maps.");
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                "Removes maps with invalid IDs or missing image files.");
        MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                "This action is not reverseable. It is recommended to create a backup of your maps.yml first!");
        MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                "It also asks the server for every single map, which loads all of them into memory.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Usage: /imagemap cleanup");
    }
}
