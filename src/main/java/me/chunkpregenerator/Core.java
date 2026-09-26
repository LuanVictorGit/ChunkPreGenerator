package me.chunkpregenerator;

import static me.chunkpregenerator.Lang.T.*;
import java.io.File;
import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Core extends JavaPlugin implements Listener {
	static final String PERM = "chunkpregenerator.admin"; static Core plugin;
	public void onEnable() {
		plugin = this; PluginManager m = getServer().getPluginManager();
		m.registerEvent(InventoryClickEvent.class, this, EventPriority.HIGHEST, (l, e) -> Menu.click((InventoryClickEvent) e), this);
		m.registerEvent(InventoryDragEvent.class, this, EventPriority.HIGHEST, (l, e) -> { if (Menu.of(((InventoryDragEvent) e).getInventory()) != null) ((InventoryDragEvent) e).setCancelled(true); }, this);
		m.registerEvent(InventoryCloseEvent.class, this, EventPriority.MONITOR, (l, e) -> Menu.OPEN.remove(Menu.of(((InventoryCloseEvent) e).getInventory())), this);
		m.registerEvent(PlayerQuitEvent.class, this, EventPriority.MONITOR, (l, e) -> Menu.SEL.remove(((PlayerQuitEvent) e).getPlayer().getUniqueId()), this);
		Engine.file = new File(getDataFolder(), "generations.dat"); Engine.load(); getServer().getScheduler().runTaskTimer(this, Engine::tick, 1, 1);
		Engine.log(ENABLED, getDescription().getVersion(), Lang.of(getServer().getConsoleSender()).t(Engine.mode()));
	}
	public void onDisable() { getServer().getScheduler().cancelTasks(this); Menu.closeAll(); Engine.stop(); plugin = null; }
	public boolean onCommand(CommandSender s, Command c, String l, String[] a) { if (!s.hasPermission(PERM)) Engine.msg(s, NO_PERM); else if (s instanceof Player) Menu.home((Player) s); else Engine.console(s, a); return true; }
	public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
		List<String> r = new ArrayList<>(); if (s instanceof Player || !s.hasPermission(PERM) || a.length == 0) return r;
		if (a.length == 1) r.addAll(Arrays.asList("start", "pause", "resume", "cancel")); else if (a.length == 2) for (World w : Bukkit.getWorlds()) r.add(w.getName()); else if (a.length == 4 && a[0].equalsIgnoreCase("start")) r.add("confirm");
		String p = a[a.length - 1].toLowerCase(Locale.ROOT); r.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(p)); return r;
	}
}
