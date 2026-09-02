package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImageMapCommandHandler implements CommandExecutor, TabCompleter {
    private final Map<String, ImageMapSubCommand> commands = new LinkedHashMap<>();
    private final Map<String, String> aliases = new HashMap<>();

    public ImageMapCommandHandler(ImageMaps plugin) {
        register("download", new ImageMapDownloadCommand(plugin));
        register("delete", new ImageMapDeleteCommand(plugin));
        register("place", new ImageMapPlaceCommand(plugin));
        register("info", new ImageMapInfoCommand(plugin));
        register("list", new ImageMapListCommand(plugin));
        register("reload", new ImageMapReloadCommand(plugin));
        register("bake", new ImageMapBakeCommand(plugin));
        register("cleanup", new ImageMapCleanupCommand(plugin));
        register("debuginfo", new ImageMapDebugInfoCommand(plugin));
        register("help", new ImageMapHelpCommand(plugin, commands), "?");
    }

    private void register(String name, ImageMapSubCommand command, String... commandAliases) {
        commands.put(name, command);

        for (String alias : commandAliases)
            aliases.put(alias, name);
    }

    public Map<String, ImageMapSubCommand> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            commands.get("help").execute(sender, cmd, label, args);
            return true;
        }

        ImageMapSubCommand subCommand = get(args[0]);

        if (subCommand == null) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "Unknown command, try /imagemap help.");
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING,
                                    "You do not have permission to use this command.");
            return true;
        }

        if (!subCommand.checkSender(sender)) {
            MessageUtil.sendMessage(sender, MessageLevel.WARNING, "You can't run this command.");
            return true;
        }

        subCommand.execute(sender, cmd, label, mergeQuoted(args));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length <= 1) {
            List<String> names = new ArrayList<>();

            commands.forEach((name, command) -> {
                if (sender.hasPermission(command.getPermission()))
                    names.add(name);
            });

            return Utils.getMatches(args.length == 0 ? "" : args[0], names);
        }

        ImageMapSubCommand subCommand = get(args[0]);

        if (subCommand == null || !sender.hasPermission(subCommand.getPermission()))
            return Collections.emptyList();

        return subCommand.onTabComplete(sender, args);
    }

    private ImageMapSubCommand get(String name) {
        String key = name.toLowerCase(Locale.ROOT);

        return commands.get(aliases.getOrDefault(key, key));
    }

    /**
     * Joins arguments wrapped in double quotes back into a single argument and strips the quotes.
     * <p>
     * The list and info output offer clickable commands that quote the file name, which is the only way images
     * with a space in their name can be addressed at all.
     */
    static String[] mergeQuoted(String[] args) {
        List<String> merged = new ArrayList<>(args.length);
        StringBuilder open = null;

        for (String arg : args) {
            if (open != null) {
                open.append(' ').append(arg);

                if (arg.endsWith("\"")) {
                    merged.add(strip(open.toString()));
                    open = null;
                }
            }
            else if (arg.length() > 1 && arg.startsWith("\"") && !arg.endsWith("\""))
                open = new StringBuilder(arg);
            else
                merged.add(strip(arg));
        }

        if (open != null)
            merged.add(strip(open.toString()));

        return merged.toArray(new String[0]);
    }

    private static String strip(String argument) {
        if (argument.length() > 1 && argument.startsWith("\"") && argument.endsWith("\""))
            return argument.substring(1, argument.length() - 1);

        return argument;
    }
}
