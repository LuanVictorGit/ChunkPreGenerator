package me.chunkpregenerator;

import java.lang.invoke.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

final class Compat {
	static final boolean LEGACY = !exists("org.bukkit.block.data.BlockData");
	static final MethodHandle ASYNC = find(World.class, "getChunkAtAsync", CompletableFuture.class, int.class, int.class, boolean.class), MSPT = find(Server.class, "getAverageTickTime", double.class), LOCALE = locale();
	static final Map<String, ItemStack> ITEMS = new HashMap<>();
	static boolean exists(String c) { try { Class.forName(c); return true; } catch (Throwable t) { return false; } }
	static MethodHandle find(Class<?> c, String n, Class<?> r, Class<?>... p) { try { return MethodHandles.publicLookup().findVirtual(c, n, MethodType.methodType(r, p)); } catch (Throwable t) { return null; } }
	static MethodHandle locale() {
		MethodHandle h = find(Player.class, "getLocale", String.class);
		if (h == null) try { h = MethodHandles.filterArguments(find(Player.Spigot.class, "getLocale", String.class), 0, find(Player.class, "spigot", Player.Spigot.class)); } catch (Throwable t) { h = find(Player.class, "locale", Locale.class); }
		return h == null ? null : h.asType(MethodType.methodType(Object.class, Player.class));
	}
	static String locale(Player p) { try { return LOCALE == null ? "" : String.valueOf((Object) LOCALE.invokeExact(p)); } catch (Throwable t) { return ""; } }
	static double mspt() { try { return MSPT == null ? -1 : (double) MSPT.invokeExact(Bukkit.getServer()); } catch (Throwable t) { return -1; } }
	static CompletableFuture<?> async(World w, int x, int z) throws Throwable { return (CompletableFuture<?>) ASYNC.invokeExact(w, x, z, true); }
	static ItemStack item(String spec, String name, List<String> lore) {
		ItemStack i = ITEMS.computeIfAbsent(spec, Compat::parse).clone(); ItemMeta m = i.getItemMeta();
		if (m != null) { m.setDisplayName(name); m.setLore(lore); m.addItemFlags(ItemFlag.values()); i.setItemMeta(m); }
		return i;
	}
	static ItemStack parse(String spec) {
		for (String s : spec.split("\\|")) { String[] p = s.split(":"); Material m = Material.getMaterial(p[0]); if (m == null) continue; ItemStack i = new ItemStack(m); if (p.length > 1) i.setDurability(Short.parseShort(p[1])); return i; }
		return new ItemStack(Material.STONE);
	}
}
