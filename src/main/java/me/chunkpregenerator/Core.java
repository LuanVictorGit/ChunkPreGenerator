package me.chunkpregenerator;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import lombok.Getter;
import me.chunkpregenerator.commands.CommandChunkPreGenerator;
import me.chunkpregenerator.objects.Manager;
import me.chunkpregenerator.utils.ConfigManager;
import me.chunkpregenerator.utils.Timer;
import me.chunkpregenerator.utils.tasks.TaskWorld;

@Getter
public class Core extends JavaPlugin {

	@Getter private static Core instance;
	private String tag, version = "§dv" + getDescription().getVersion();
	private Manager manager;
	private ConfigManager configManager;
	private BukkitTask task;
	private TaskWorld taskWorld;
	
	@Override
	public void onEnable() {
		instance = this;
		saveDefaultConfig();
		reloadPlugin();
		
		new CommandChunkPreGenerator();
		
		sendConsole(" ");
		sendConsole(tag + " &aChunkPreGenerator loaded. &6[Author lHawk_] " + version);
		sendConsole(" ");
	}
	
	@Override
	public void onDisable() {
		if (task != null) task.cancel();
		HandlerList.unregisterAll(this);
		sendConsole(" ");
		sendConsole(tag + " &cChunkPreGenerator unload. &6[Author lHawk_] " + version);
		sendConsole(" ");
	}
	
	public void reloadPlugin() {
		reloadConfig();
		tag = getConfig().getString("Config.tag").replace("&", "§");
		manager = new Manager();
		configManager = new ConfigManager();
		
		taskWorld = new TaskWorld();
		manager.getTasks().add(taskWorld);
		
		if (task != null) task.cancel();
		task = new Timer().runTaskTimerAsynchronously(this, 0, 1);
	}
	
	private void sendConsole(String msg) {Bukkit.getConsoleSender().sendMessage(msg.replace("&", "§"));}
	
}
