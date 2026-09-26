package me.chunkpregenerator;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

public class EngineTest extends TestCase {
	static final Set<Long> LOADED = new HashSet<>(); static final List<String> MSGS = new ArrayList<>(); static boolean present = true; static int loads; static Inventory open;
	static final World WORLD = proxy(World.class, (p, m, a) -> {
		switch (m.getName()) {
			case "getName": return "world";
			case "isChunkLoaded": return LOADED.contains(JobTest.key((int) a[0], (int) a[1]));
			case "loadChunk": if (LOADED.add(JobTest.key((int) a[0], (int) a[1]))) loads++; return true;
			case "unloadChunkRequest": LOADED.remove(JobTest.key((int) a[0], (int) a[1])); return true;
			case "getSpawnLocation": return new Location(EngineTest.WORLD, 100, 64, -100);
			case "getWorldBorder": return proxy(WorldBorder.class, (b, n, c) -> n.getName().equals("getSize") ? (Object) 59999968.0 : n.getName().equals("getCenter") ? new Location(EngineTest.WORLD, 0, 0, 0) : def(n));
			default: return def(m);
		}
	});
	static final ConsoleCommandSender CONSOLE = proxy(ConsoleCommandSender.class, (p, m, a) -> { if (m.getName().equals("sendMessage") && a[0] instanceof String) MSGS.add((String) a[0]); return m.getName().equals("hasPermission") ? (Object) true : def(m); });
	static final Player PLAYER = proxy(Player.class, (p, m, a) -> {
		switch (m.getName()) {
			case "spigot": return new Player.Spigot() { public String getLocale() { return "pt_BR"; } };
			case "getUniqueId": return new UUID(0, 1);
			case "getWorld": return WORLD;
			case "getLocation": return new Location(WORLD, -500, 70, 900);
			case "hasPermission": return true;
			case "openInventory": open = (Inventory) a[0]; return null;
			case "closeInventory": open = null; return null;
			case "sendMessage": if (a[0] instanceof String) MSGS.add((String) a[0]); return null;
			case "equals": return p == a[0];
			case "hashCode": return 1;
			default: return def(m);
		}
	});
	static final BukkitScheduler SCHEDULER = proxy(BukkitScheduler.class, (p, m, a) -> { if (m.getName().startsWith("runTask")) ((Runnable) a[1]).run(); return def(m); });
	static final ItemFactory FACTORY = proxy(ItemFactory.class, (p, m, a) -> m.getName().equals("getItemMeta") ? meta(new HashMap<>()) : m.getName().equals("asMetaFor") ? a[0] : m.getName().equals("isApplicable") ? (Object) true : def(m));
	static {
		Bukkit.setServer(proxy(Server.class, (p, m, a) -> {
			switch (m.getName()) {
				case "getWorld": return present && "world".equals(a[0]) ? WORLD : null;
				case "getWorlds": return Collections.singletonList(WORLD);
				case "getConsoleSender": return CONSOLE;
				case "getScheduler": return SCHEDULER;
				case "getItemFactory": return FACTORY;
				case "createInventory": return inventory((InventoryHolder) a[0], (int) a[1], (String) a[2]);
				case "isPrimaryThread": return true;
				default: return def(m);
			}
		}));
	}
	static Object def(Method m) { Class<?> r = m.getReturnType(); return r == boolean.class ? (Object) false : r == int.class ? (Object) 0 : r == long.class ? (Object) 0L : r == double.class ? (Object) 0.0 : r == String.class ? "test" : r == Logger.class ? Logger.getLogger("test") : r.isEnum() ? r.getEnumConstants()[0] : Collection.class.isAssignableFrom(r) ? Collections.emptyList() : null; }
	static <T> T proxy(Class<T> c, InvocationHandler h) { return c.cast(Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] {c}, h)); }
	static ItemMeta meta(Map<String, Object> d) { return proxy(ItemMeta.class, (p, m, a) -> { String n = m.getName(), k = n.substring(Math.min(3, n.length())); if (n.equals("clone")) return meta(new HashMap<>(d)); if (n.startsWith("set")) { d.put(k, a[0]); return null; } return n.startsWith("get") && d.containsKey(k) ? d.get(k) : def(m); }); }
	static Inventory inventory(InventoryHolder h, int size, String title) {
		ItemStack[] items = new ItemStack[size];
		return proxy(Inventory.class, (p, m, a) -> {
			switch (m.getName()) {
				case "getSize": return size;
				case "getTitle": return title;
				case "getHolder": return h;
				case "setItem": items[(int) a[0]] = (ItemStack) a[1]; return null;
				case "getItem": return items[(int) a[0]];
				case "getViewers": return p == open ? Collections.singletonList(PLAYER) : Collections.emptyList();
				case "equals": return p == a[0];
				case "hashCode": return System.identityHashCode(p);
				default: return def(m);
			}
		});
	}
	static void run(int ticks) { for (int i = 0; i < ticks; i++) Engine.tick(); }
	public void testAdapt() {
		assertEquals(11, Engine.adapt(10, 0, true, 1, 128)); assertEquals(2, Engine.adapt(1, 0, true, 1, 128)); assertEquals(128, Engine.adapt(128, 0, true, 1, 128)); assertEquals(128, Engine.adapt(120, 0, true, 1, 128));
		assertEquals(10, Engine.adapt(10, 0, false, 1, 128)); assertEquals(5, Engine.adapt(10, 1, true, 1, 128)); assertEquals(5, Engine.adapt(10, 2, false, 1, 128)); assertEquals(1, Engine.adapt(1, 1, false, 1, 128)); assertEquals(1000, Engine.adapt(1500, 2, false, 1000, 40000));
		assertTrue(Engine.START >= 2 && Engine.START <= Engine.CORES + 2); assertTrue(Engine.MAX >= 16 && Engine.MAX <= 128);
	}
	public void testAsyncBookkeeping() {
		Engine.world = WORLD; Job j = new Job("world", 0, 0, 100, false); j.state = Job.RUN; long[] s = new long[3]; int[][] c = new int[3][];
		for (int i = 0; i < 3; i++) { j.next(); c[i] = new int[] {j.x, j.z}; s[i] = j.push(); j.inflight++; }
		Engine.done(j, j.epoch, s[2], WORLD, c[2][0], c[2][1], null); Engine.done(j, j.epoch, s[1], WORLD, c[1][0], c[1][1], new RuntimeException("x")); assertEquals(2, j.done); assertEquals(0, j.ckDone); assertEquals(1, j.errors);
		Engine.done(j, j.epoch - 1, s[0], WORLD, c[0][0], c[0][1], null); assertEquals(2, j.done); Engine.done(j, j.epoch, s[0], WORLD, c[0][0], c[0][1], null); assertEquals(3, j.ckDone); assertEquals(j.cursor, j.ckCursor); assertEquals(0, j.inflight);
		j.next(); LOADED.add(JobTest.key(j.x, j.z)); assertFalse(Engine.async(j)); assertEquals(4, j.ckDone); LOADED.clear(); Engine.world = null;
	}
	public void testGenerationLifecycle() throws Exception {
		assertTrue(Compat.LEGACY); assertFalse(Compat.ASYNC); assertEquals(Lang.T.M_LEGACY, Engine.mode()); Engine.JOBS.clear(); Engine.file = File.createTempFile("generations", ".dat"); assertTrue(Engine.file.delete());
		Engine.console(CONSOLE, new String[] {"start", "world", "700"}); assertTrue(Engine.JOBS.isEmpty());
		Engine.console(CONSOLE, new String[] {"start", "world", "5"}); Engine.console(CONSOLE, new String[] {"start", "nope", "700", "confirm"}); assertTrue(Engine.JOBS.isEmpty());
		Engine.console(CONSOLE, new String[] {"start", "world", "700", "confirm"}); Job j = Engine.find("world"); assertNotNull(j); assertEquals(Job.RUN, j.state); assertEquals(100, j.cx); assertEquals(-100, j.cz);
		Engine.console(CONSOLE, new String[] {"start", "world", "900", "confirm"}); assertEquals(1, Engine.JOBS.size()); assertSame(j, Engine.find("world"));
		Engine.budget = 1; Engine.tokens = 0; run(3); assertTrue(j.done > 0 && j.done < j.total); assertEquals(j.done, j.ckDone);
		Engine.console(CONSOLE, new String[] {"pause", "world"}); assertEquals(Job.PAUSED, j.state); long done = j.done; run(25); assertEquals(done, j.done);
		Engine.stop(); assertTrue(Engine.file.exists()); assertTrue(Engine.JOBS.isEmpty());
		Engine.load(); Job r = Engine.find("world"); assertNotNull(r); assertNotSame(j, r); assertEquals(Job.PAUSED, r.state); assertEquals(done, r.done); assertEquals(j.total, r.total);
		present = false; Engine.console(CONSOLE, new String[] {"resume", "world"}); assertEquals(Job.PAUSED, r.state); present = true;
		Engine.console(CONSOLE, new String[] {"resume", "world"}); assertEquals(Job.RUN, r.state);
		for (int i = 0; i < 20000 && r.state == Job.RUN; i++) Engine.tick();
		assertEquals(Job.DONE, r.state); assertEquals(r.total, r.done); assertEquals(0, r.inflight); assertTrue(LOADED.isEmpty()); assertTrue(loads >= r.total); assertEquals(100.0, r.percent());
		Engine.stop(); assertFalse(Engine.file.exists());
		Engine.console(CONSOLE, new String[] {"start", "world", "300", "confirm"}); Job c = Engine.find("world"); Engine.budget = 1; Engine.tokens = 0; run(2); present = false; Engine.budget = Engine.BUDGET; run(21); assertEquals(Job.PAUSED, c.state); assertEquals(c.ckDone, c.done); present = true;
		Engine.console(CONSOLE, new String[] {"cancel", "world"}); assertTrue(Engine.JOBS.isEmpty()); Engine.console(CONSOLE, new String[] {"pause", "world"}); Engine.console(CONSOLE, new String[0]);
		assertFalse(MSGS.isEmpty()); Engine.stop(); assertFalse(Engine.file.exists());
	}
}
