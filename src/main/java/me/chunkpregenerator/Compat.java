package me.chunkpregenerator;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class Compat {
	static final boolean LEGACY = !exists("org.bukkit.block.data.BlockData");
	static final MethodHandle PAPER = find(World.class, "getChunkAtAsync", CompletableFuture.class, int.class, int.class, boolean.class), MSPT = tickTime(), LOCALE = locale();
	static Object ticket; static final MethodHandle[] NMS = PAPER == null ? nms() : null; static final boolean ASYNC = PAPER != null || NMS != null;
	static final Map<String, ItemStack> ITEMS = new HashMap<>();
	static boolean exists(String c) { try { Class.forName(c); return true; } catch (Throwable t) { return false; } }
	static MethodHandle find(Class<?> c, String n, Class<?> r, Class<?>... p) { try { return MethodHandles.publicLookup().findVirtual(c, n, MethodType.methodType(r, p)); } catch (Throwable t) { return null; } }
	static MethodHandle locale() {
		MethodHandle h = find(Player.class, "getLocale", String.class);
		if (h == null) try { h = MethodHandles.filterArguments(find(Player.Spigot.class, "getLocale", String.class), 0, find(Player.class, "spigot", Player.Spigot.class)); } catch (Throwable t) { h = find(Player.class, "locale", Locale.class); }
		return h == null ? null : h.asType(MethodType.methodType(Object.class, Player.class));
	}
	static MethodHandle tickTime() {
		MethodHandle h = find(Server.class, "getAverageTickTime", double.class); if (h != null) return h;
		try {
			MethodHandles.Lookup l = MethodHandles.publicLookup(); Method s = Bukkit.getServer().getClass().getMethod("getServer"); MethodHandle n = l.unreflect(s.getReturnType().getMethod("getAverageTickTimeNanos"));
			return MethodHandles.filterReturnValue(MethodHandles.filterReturnValue(l.unreflect(s), n.asType(n.type().changeParameterType(0, s.getReturnType()))), MethodHandles.lookup().findStatic(Compat.class, "ms", MethodType.methodType(double.class, long.class))).asType(MethodType.methodType(double.class, Server.class));
		} catch (Throwable t) { return null; }
	}
	static double ms(long nanos) { return nanos / 1e6; }
	static MethodHandle[] nms() {
		try {
			Class<?> type = Class.forName("net.minecraft.server.level.TicketType"), pos = Class.forName("net.minecraft.world.level.ChunkPos"); MethodHandles.Lookup l = MethodHandles.publicLookup();
			Method handle = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".CraftWorld").getMethod("getHandle"), source = handle.getReturnType().getMethod("getChunkSource"); Class<?> cache = source.getReturnType(); ticket = type.getField("PLUGIN").get(null);
			return new MethodHandle[] {l.unreflect(handle), l.unreflect(source), l.unreflect(cache.getMethod("addTicketAndLoadWithRadius", type, pos, int.class)), l.unreflect(cache.getMethod("removeTicketWithRadius", type, pos, int.class)), l.unreflectConstructor(pos.getConstructor(int.class, int.class))};
		} catch (Throwable t) { return null; }
	}
	static String locale(Player p) { try { return LOCALE == null ? "" : String.valueOf((Object) LOCALE.invokeExact(p)); } catch (Throwable t) { return ""; } }
	static double mspt() { try { return MSPT == null ? -1 : (double) MSPT.invokeExact(Bukkit.getServer()); } catch (Throwable t) { return -1; } }
	static CompletableFuture<?> async(World w, int x, int z) throws Throwable { return PAPER != null ? (CompletableFuture<?>) PAPER.invokeExact(w, x, z, true) : (CompletableFuture<?>) NMS[2].invoke(NMS[1].invoke(NMS[0].invoke(w)), ticket, NMS[4].invoke(x, z), 0); }
	static void release(World w, int x, int z) { try { if (NMS != null) NMS[3].invoke(NMS[1].invoke(NMS[0].invoke(w)), ticket, NMS[4].invoke(x, z), 0); else if (w.isChunkLoaded(x, z)) w.unloadChunkRequest(x, z); } catch (Throwable ignored) {} }
	static ItemStack item(String spec, String name, List<String> lore) {
		ItemStack i = ITEMS.computeIfAbsent(spec, Compat::parse).clone(); ItemMeta m = i.getItemMeta();
		if (m != null) { m.setDisplayName(name); m.setLore(lore); i.setItemMeta(m); }
		return i;
	}
	static ItemStack parse(String spec) {
		for (String s : spec.split("\\|")) { String[] p = s.split(":"); Material m = Material.getMaterial(p[0]); if (m == null) continue; ItemStack i = new ItemStack(m); if (p.length > 1) i.setDurability(Short.parseShort(p[1])); return i; }
		return new ItemStack(Material.STONE);
	}
}
