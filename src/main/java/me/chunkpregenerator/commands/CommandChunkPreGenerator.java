package me.chunkpregenerator.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

import me.chunkpregenerator.Core;
import me.chunkpregenerator.utils.tasks.TaskWorld;

public class CommandChunkPreGenerator implements CommandExecutor, TabCompleter {

    public CommandChunkPreGenerator() {
        PluginCommand command = Core.getInstance().getCommand("chunkpregenerator");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String tag = Core.getInstance().getTag();

        if (!sender.hasPermission("chunkpregenerator.adm")) {
            sender.sendMessage(tag + " §cYou do not have permission to do that!");
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            // Comando help
            sender.sendMessage(tag + " §e--- ChunkPreGenerator Help ---");
            sender.sendMessage("§e/chunkpregenerator reload §7- Reload the plugin.");
            sender.sendMessage("§e/chunkpregenerator generate <world> §7- Set a world to pregenerate chunks.");
            sender.sendMessage("§e/chunkpregenerator help §7- Show this help message.");
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                Core.getInstance().reloadPlugin();
                tag = Core.getInstance().getTag();
                sender.sendMessage(tag + " §aPlugin reloaded successfully.");
                return true;
            }
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("generate")) {
                World world = Bukkit.getWorld(args[1]);
                if (world != null) {
                    TaskWorld.setWorld(world);
                    sender.sendMessage(tag + " §aYou set the world " + world.getName() + " to pregenerate, please wait.");
                    return true;
                } else {
                    sender.sendMessage(tag + " §cWorld not found.");
                    return false;
                }
            }
        }

        // Caso nenhum comando seja reconhecido
        sender.sendMessage(tag + " §cUnknown command! Use /chunkpregenerator help.");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("chunkpregenerator.adm")) {
            return null; // sem permissões, não mostra sugestões
        }

        if (args.length == 1) {
            return List.of("help", "reload", "generate");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("generate")) {
                return Bukkit.getWorlds().stream()
                        .map(World::getName)
                        .toList();
            }
        }

        return null;
    }
}
