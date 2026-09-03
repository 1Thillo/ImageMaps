package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImageMapHelpCommand extends ImageMapSubCommand {
    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
        Map.entry("place", "<filename> [frameInvisible] [frameFixed] [frameGlowing] [size] - starts image placement"),
        Map.entry("download", "<filename> <sourceURL> - downloads an image"),
        Map.entry("delete", "<filename> - deletes an image"),
        Map.entry("info", "<filename> - displays image info"),
        Map.entry("reload", "<filename> - reloads an image from disk"),
        Map.entry("frame", "<invisible|fixed|rotatable> <true|false> - changes the frames of the image you look at"),
        Map.entry("bake", "[filename] - converts images to map colours ahead of time"),
        Map.entry("cleanup", " - removes invalid maps from the plugin"),
        Map.entry("debuginfo", " - prints debug output"),
        Map.entry("list", "[page] - lists all files in the images folder"),
        Map.entry("version", " - shows which version of the plugin is running"),
        Map.entry("help", "[command] - shows help"));

    private final Map<String, ImageMapSubCommand> commands;

    public ImageMapHelpCommand(ImageMaps plugin, Map<String, ImageMapSubCommand> commands) {
        super("imagemaps.help", plugin, true);
        this.commands = commands;
    }

    @Override
    protected void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 2) {
            ImageMapSubCommand subCommand = commands.get(args[1].toLowerCase(Locale.ROOT));

            if (subCommand == null || !sender.hasPermission(subCommand.getPermission())) {
                MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Unknown command.");
                return;
            }

            subCommand.help(sender);
            return;
        }

        help(sender);
    }

    @Override
    public void help(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageLevel.INFO, "## ImageMaps Commands ##");

        commands.forEach((name, command) -> {
            if (!sender.hasPermission(command.getPermission()))
                return;

            MessageUtil.sendMessage(sender, MessageLevel.NORMAL,
                                    Component.text("/imagemap " + name + " ", NamedTextColor.WHITE)
                                             .append(Component.text(DESCRIPTIONS.getOrDefault(name, ""),
                                                                    NamedTextColor.GRAY)));
        });
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length != 2)
            return Collections.emptyList();

        List<String> names = new ArrayList<>();

        commands.forEach((name, command) -> {
            if (sender.hasPermission(command.getPermission()))
                names.add(name);
        });

        return Utils.getMatches(args[1], names);
    }
}
