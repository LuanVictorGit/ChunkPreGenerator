package me.chunkpregenerator;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitScheduler;

public class EngineTest extends TestCase {
	static final Set<Long> LOADED = new HashSet<>(); static final List<String> MSGS = new ArrayList<>(); static boolean present = true; static int loads;
	static final World WORLD = proxy(World.class, (p, m, a) -> {
		switch (m.getName()) {
			case "getName": return "world";
			case "isChunkLoaded": return LOADED.contains(JobTest.key((int) a[0], (int) a[1]));
			case "loadChunk": if (LOADED.add(JobTest.key((int) a[0], (int) a[1]))) loads++; return true;
			case "unloadChunkRequest": LOADED.remove(JobTest.key((int) a[0], (int) a[1])); return true;
			case "getSpawnLocation": return new Location(EngineTest.WORLD, 100, 64, -100);
			default: return def(m);
		}
	});
	static final ConsoleCommandSender CONSOLE = proxy(ConsoleCommandSender.class, (p, m, a) -> { if (m.getName().equals("sendMessage") && a[0] instanceof String) MSGS.add((String) a[0]); return m.getName().equals("hasPermission") ? (Object) true : def(m); });
	static final BukkitScheduler SCHEDULER = proxy(BukkitScheduler.class, (p, m, a) -> { if (m.getName().startsWith("runTask")) ((Runnable) a[1]).run(); return def(m); });
	static {
		Bukkit.setServer(proxy(Server.class, (p, m, a) -> {
			switch (m.getName()) {
				case "getWorld": return present && "world".equals(a[0]) ? WORLD : null;
				case "getConsoleSender": return CONSOLE;
				case "getScheduler": return SCHEDULER;
				case "isPrimaryThread": return true;
				default: return def(m);
			}
		}));
	}
	static Object def(Method m) { Class<?> r = m.getReturnType(); return r == boolean.class ? (Object) false : r == int.class ? (Object) 0 : r == long.class ? (Object) 0L : r == double.class ? (Object) 0.0 : r == String.class ? "test" : r == Logger.class ? Logger.getLogger("test") : Collection.class.isAssignableFrom(r) ? Collections.emptyList() : null; }
	static <T> T proxy(Class<T> c, InvocationHandler h) { return c.cast(Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] {c}, h)); }
	static void run(int ticks) { for (int i = 0; i < ticks; i++) Engine.tick(); }
	public void testAdapt() {
		assertEquals(11, Engine.adapt(10, 0, true, 1, 128)); assertEquals(2, Engine.adapt(1, 0, true, 1, 128)); assertEquals(128, Engine.adapt(128, 0, true, 1, 128)); assertEquals(128, Engine.adapt(120, 0, true, 1, 128));
		assertEquals(10, Engine.adapt(10, 0, false, 1, 128)); assertEquals(5, Engine.adapt(10, 1, true, 1, 128)); assertEquals(5, Engine.adapt(10, 2, false, 1, 128)); assertEquals(1, Engine.adapt(1, 1, false, 1, 128)); assertEquals(1000, Engine.adapt(1500, 2, false, 1000, 40000));
		assertTrue(Engine.START >= 2 && Engine.START <= Engine.CORES + 2); assertTrue(Engine.MAX >= 16 && Engine.MAX <= 128);
	}
	public void testGenerationLifecycle() throws Exception {
		assertTrue(Compat.LEGACY); assertNull(Compat.ASYNC); Engine.JOBS.clear(); Engine.file = File.createTempFile("generations", ".dat"); assertTrue(Engine.file.delete());
		Engine.console(CONSOLE, new String[] {"start", "world", "700"}); assertTrue(Engine.JOBS.isEmpty());
		Engine.console(CONSOLE, new String[] {"start", "world", "5"}); Engine.console(CONSOLE, new String[] {"start", "nope", "700", "confirm"}); assertTrue(Engine.JOBS.isEmpty());
		Engine.console(CONSOLE, new String[] {"start", "world", "700", "confirm"}); Job j = Engine.find("world"); assertNotNull(j); assertEquals(Job.RUN, j.state); assertEquals(100, j.cx); assertEquals(-100, j.cz);
		Engine.console(CONSOLE, new String[] {"start", "world", "900", "confirm"}); assertEquals(1, Engine.JOBS.size()); assertSame(j, Engine.find("world"));
		Engine.budget = 1; Engine.tokens = 0; run(3); assertTrue(j.done > 0 && j.done < j.total);
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
