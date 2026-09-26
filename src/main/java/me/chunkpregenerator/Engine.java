package me.chunkpregenerator;

import static me.chunkpregenerator.Lang.T.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class Engine {
	static final List<Job> JOBS = new ArrayList<>(); static final long[] USED = new long[5];
	static final int CORES = Runtime.getRuntime().availableProcessors(), MAX = Math.min(128, Math.max(16, CORES * 8)), START = Math.max(2, CORES), BUDGET = 10000;
	static int limit = START, budget = BUDGET, health, rr, ticks, sec, batch; static long tokens, last, lastSec, seq, written; static double tick = 50, mspt = -1; static boolean dirty; static World world; static File file;
	static void tick() {
		long now = System.nanoTime(); if (last != 0) tick += ((now - last) / 1e6 - tick) * 0.05; last = now;
		if (ticks++ % 20 == 0) second();
		batch = 0; if (health > 1) return;
		if (Compat.ASYNC != null) for (int i = 0, n = inflight(); i < 4096 && n < limit; i++) { Job j = pick(); if (j == null) break; if (async(j)) n++; }
		else { tokens = Math.min(tokens + budget * 1000L, budget * 2000L); for (; batch < 4096 && tokens > 0; batch++) { Job j = pick(); if (j == null) break; long t = System.nanoTime(); sync(j); tokens -= System.nanoTime() - t; } }
		world = null;
	}
	static Job pick() {
		for (int i = 0, n = JOBS.size(); i < n; i++) {
			Job j = JOBS.get(rr = (rr + 1) % n); if (j.state != Job.RUN || j.drain) continue;
			if ((world = Bukkit.getWorld(j.world)) == null) { j.reset(); j.state = Job.PAUSED; dirty = true; broadcast(WORLD_MISSING, j.world); } else if (j.next()) return j;
		}
		return null;
	}
	static boolean async(Job j) {
		World w = world; int x = j.x, z = j.z, e = j.epoch; if (w.isChunkLoaded(x, z)) { j.done++; return false; }
		j.inflight++; try { Compat.async(w, x, z).whenComplete((c, t) -> done(j, e, w, x, z, t)); } catch (Throwable t) { done(j, e, w, x, z, t); }
		return true;
	}
	static void done(Job j, int e, World w, int x, int z, Throwable t) {
		if (Core.plugin == null || !Core.plugin.isEnabled() || e != j.epoch) return;
		if (!Bukkit.isPrimaryThread()) { Bukkit.getScheduler().runTask(Core.plugin, () -> done(j, e, w, x, z, t)); return; }
		j.inflight--; j.done++; if (t != null) j.error(x, z, t); if (j.inflight == 0 && j.drain && j.checkpoint()) dirty = true;
		try { if (w.isChunkLoaded(x, z)) w.unloadChunkRequest(x, z); } catch (Throwable ignored) {}
	}
	static void sync(Job j) {
		World w = world; int x = j.x, z = j.z;
		try {
			if (Compat.LEGACY) { for (int i = 0; i < 4; i++) w.loadChunk(x + (i & 1), z + (i >> 1), true); for (int i = 0; i < 4; i++) if (i == 0 || !j.future(x + (i & 1), z + (i >> 1))) w.unloadChunkRequest(x + (i & 1), z + (i >> 1)); }
			else if (!w.isChunkLoaded(x, z)) { w.loadChunk(x, z, true); w.unloadChunkRequest(x, z); }
		} catch (Throwable t) { j.error(x, z, t); }
		j.done++;
	}
	static void second() {
		Runtime r = Runtime.getRuntime(); long now = System.nanoTime(), dt = lastSec == 0 ? 0 : (now - lastSec) / 1000000, min = Long.MAX_VALUE; lastSec = now; USED[sec % 5] = r.totalMemory() - r.freeMemory();
		for (long u : USED) if (u > 0) min = Math.min(min, u);
		double mem = (double) min / r.maxMemory(); mspt = Compat.mspt(); boolean run = !running().isEmpty();
		int h = mem > 0.9 || tick > 60 ? 2 : mem > 0.8 || tick > 52.5 || mspt > 45 ? 1 : 0; boolean grow = h == 0 && (mspt < 0 ? tick < 51 : mspt < 38);
		if (run && (h > 1) != (health > 1)) log(h > 1 ? CRITICAL_MSG : RECOVERED_MSG);
		health = h; limit = run ? adapt(limit, h, grow, 1, MAX) : START; budget = run ? adapt(budget, h, grow, 1000, 40000) : BUDGET;
		for (Job j : new ArrayList<>(JOBS)) {
			if (j.state == Job.RUN) { j.elapsed += dt; j.sample(now); if (sec % 30 == 0 && j.inflight > 0) j.drain = true; if (j.cursor >= j.slots && j.inflight == 0) finish(j); }
			if (j.inflight == 0 && j.checkpoint()) dirty = true;
		}
		if (dirty && sec % 5 == 0) save(true);
		sec++; Menu.refresh();
	}
	static int adapt(int v, int h, boolean grow, int min, int max) { return h > 0 ? Math.max(min, v / 2) : grow ? Math.min(max, v + Math.max(1, v / 10)) : v; }
	static int inflight() { int n = 0; for (Job j : JOBS) n += j.inflight; return n; }
	static List<Job> running() { List<Job> r = new ArrayList<>(); for (Job j : JOBS) if (j.state == Job.RUN) r.add(j); return r; }
	static Job find(String w) { for (Job j : JOBS) if (j.world.equalsIgnoreCase(w)) return j; return null; }
	static Lang.T mode() { return Compat.ASYNC != null ? M_ASYNC : Compat.LEGACY ? M_LEGACY : M_SYNC; }
	static Lang.T health() { return health > 1 ? H_CRIT : health > 0 ? H_HIGH : H_OK; }
	static void finish(Job j) { j.state = Job.DONE; dirty = true; broadcast(COMPLETED_MSG, j.world, j.total, Lang.time(j.elapsed / 1000)); }
	static boolean start(CommandSender s, Job j) {
		Job e = find(j.world);
		if (Bukkit.getWorld(j.world) == null) msg(s, WORLD_MISSING, j.world); else if (e != null && e.state != Job.DONE) msg(s, EXISTS, j.world);
		else { JOBS.remove(e); JOBS.add(j); j.state = Job.RUN; j.hn = 0; dirty = true; msg(s, STARTED, j.world, j.total); return true; }
		return false;
	}
	static void pause(CommandSender s, Job j) { if (j.state == Job.RUN) { j.state = Job.PAUSED; dirty = true; msg(s, PAUSED_MSG, j.world); } }
	static void resume(CommandSender s, Job j) { if (j.state != Job.PAUSED) return; if (Bukkit.getWorld(j.world) == null) { msg(s, WORLD_MISSING, j.world); return; } j.state = Job.RUN; j.hn = 0; dirty = true; msg(s, RESUMED_MSG, j.world); }
	static void cancel(CommandSender s, Job j) { if (JOBS.remove(j)) { j.epoch++; dirty = true; msg(s, CANCELLED_MSG, j.world); } }
	static void remove(Job j) { if (j.state == Job.DONE) JOBS.remove(j); }
	static void msg(CommandSender s, Lang.T k, Object... a) { Lang g = Lang.of(s); s.sendMessage(g.t(PREFIX) + g.t(k, a)); }
	static void log(Lang.T k, Object... a) { msg(Bukkit.getConsoleSender(), k, a); }
	static void broadcast(Lang.T k, Object... a) { log(k, a); for (Player p : Bukkit.getOnlinePlayers()) if (p.hasPermission(Core.PERM)) msg(p, k, a); }
	static String perf(Lang g) { Runtime r = Runtime.getRuntime(); return g.t(L_PERF, g.t(health()), Math.min(20, 1000 / tick), mspt < 0 ? g.t(NA) : String.format(g.loc, "%.1f ms", mspt), (r.totalMemory() - r.freeMemory()) >> 20, r.maxMemory() >> 20); }
	static String server(Lang g) { return g.t(L_SERVER, Bukkit.getName() + " " + Bukkit.getBukkitVersion().split("-")[0], perf(g), CORES, running().size()); }
	static String auto(Lang g) { return g.t(L_AUTO, g.t(mode()), Compat.ASYNC != null ? g.t(L_LIMIT, inflight(), limit) : g.t(L_BUDGET, budget / 1000.0), g.t(health())); }
	static void console(CommandSender s, String[] a) {
		Lang g = Lang.of(s); String c = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "";
		if (c.isEmpty()) { if (JOBS.isEmpty()) msg(s, NONE); for (Job j : JOBS) msg(s, STATUS_LINE, j.world, j.status(g), j.percent(), j.done, j.total, j.speed(g), j.eta(g)); msg(s, USAGE); return; }
		if (c.equals("start")) {
			World w = a.length > 2 ? Bukkit.getWorld(a[1]) : null; int r; try { r = a.length > 2 ? Integer.parseInt(a[2]) : 0; } catch (NumberFormatException e) { r = 0; }
			if (a.length < 3) msg(s, USAGE); else if (w == null) msg(s, WORLD_MISSING, a[1]); else if (r < 16 || r > Job.MAX) msg(s, BAD_RADIUS);
			else { Location l = w.getSpawnLocation(); Job j = new Job(w.getName(), l.getBlockX(), l.getBlockZ(), r, false); if (a.length > 3 && a[3].equalsIgnoreCase("confirm")) start(s, j); else { s.sendMessage(g.t(IT_SUMMARY) + "\n" + Menu.summary(g, j, 0)); msg(s, CONFIRM_HINT); } }
			return;
		}
		Job j = a.length > 1 ? find(a[1]) : null;
		if (j == null) { if (a.length > 1) msg(s, NOT_FOUND, a[1]); else msg(s, USAGE); return; }
		switch (c) { case "pause": pause(s, j); break; case "resume": resume(s, j); break; case "cancel": cancel(s, j); break; default: msg(s, USAGE); }
	}
	static void load() {
		try { if (file.exists()) for (String l : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) { Job j = Job.parse(l); if (j != null && find(j.world) == null) JOBS.add(j); } } catch (Exception e) { log(FILE_ERROR, e); }
		if (!JOBS.isEmpty()) log(RESTORED, JOBS.size());
	}
	static void stop() { for (Job j : JOBS) if (j.inflight == 0) j.checkpoint(); save(false); JOBS.clear(); }
	static void save(boolean async) {
		StringBuilder b = new StringBuilder(); for (Job j : JOBS) if (j.state != Job.DONE) b.append(j.line()).append('\n');
		String d = b.toString(); long n = ++seq; File f = file; dirty = false;
		if (async) Bukkit.getScheduler().runTaskAsynchronously(Core.plugin, () -> write(f, d, n)); else write(f, d, n);
	}
	static synchronized void write(File f, String d, long n) {
		if (n < written) return; written = n;
		try {
			Path p = f.toPath(), t = new File(f.getPath() + ".tmp").toPath(); if (d.isEmpty()) { Files.deleteIfExists(p); return; }
			Files.createDirectories(p.getParent()); Files.write(t, d.getBytes(StandardCharsets.UTF_8));
			try { Files.move(t, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); } catch (AtomicMoveNotSupportedException e) { Files.move(t, p, StandardCopyOption.REPLACE_EXISTING); }
		} catch (Exception e) { log(FILE_ERROR, e); }
	}
}
