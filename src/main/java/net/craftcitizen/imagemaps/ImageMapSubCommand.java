package net.craftcitizen.imagemaps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * A single {@code /imagemap} sub command.
 * <p>
 * Replaces the CLCore command framework of previous versions. CLCore was only available from a GitHub Packages
 * repository that requires authentication, which made the project impossible to build for anyone but its author.
 */
public abstract class ImageMapSubCommand {
    protected final ImageMaps plugin;

    private final String permission;
    private final boolean console;

    protected ImageMapSubCommand(String permission, ImageMaps plugin, boolean console) {
        this.permission = permission;
        this.plugin = plugin;
        this.console = console;
    }

    public ImageMaps getPlugin() {
        return plugin;
    }

    public String getPermission() {
        return permission;
    }

    /**
     * Whether this command may be run by anything that is not a player.
     */
    public boolean isConsoleAllowed() {
        return console;
    }

    protected boolean checkSender(CommandSender sender) {
        return console || sender instanceof Player;
    }

    protected abstract void execute(CommandSender sender, Command cmd, String label, String[] args);

    public abstract void help(CommandSender sender);

    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
