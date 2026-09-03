package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Prints which build of the plugin is running.
 * <p>
 * Deliberately available to everyone and separate from debuginfo: after a jar has been swapped out, this is the
 * question people actually need answered, and they should not need console access to answer it.
 */
public class ImageMapVersionCommand extends ImageMapSubCommand {

    public ImageMapVersionCommand(ImageMaps plugin) {
        super("imagemaps.version", plugin, true);
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        MessageUtil.sendMessage(sender, MessageLevel.INFO,
                                getPlugin().getPluginMeta().getName() + " " + getPlugin().getPluginMeta().getVersion());
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Server: " + getPlugin().getServer().getVersion());
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.NORMAL, "Shows which version of the plugin is running.");
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "Usage: /imagemap version");
    }
}
