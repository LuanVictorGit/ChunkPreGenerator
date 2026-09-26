package me.chunkpregenerator;

import static me.chunkpregenerator.Lang.T.*;
import java.util.*;
import java.util.function.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

final class Menu implements InventoryHolder {
	static final List<Menu> OPEN = new ArrayList<>(); static final Map<UUID, Sel> SEL = new HashMap<>(); static final Lang.T[] CENTERS = {C_SPAWN, C_PLAYER, C_BORDER};
	static final String PANE = "GRAY_STAINED_GLASS_PANE|STAINED_GLASS_PANE:7", ARROW = "ARROW", YES = "LIME_WOOL|WOOL:5", NO = "RED_WOOL|WOOL:14", CLOCK = "CLOCK|WATCH", AUTO = "COMPARATOR|REDSTONE_COMPARATOR";
	final Player p; final Lang g; final Inventory inv; final boolean live; final Map<Integer, Consumer<ClickType>> acts = new HashMap<>(); Runnable draw;
	Menu(Player p, int rows, Lang.T title, boolean live) { this.p = p; g = Lang.of(p); inv = Bukkit.createInventory(this, rows * 9, g.t(title)); this.live = live; }
	public Inventory getInventory() { return inv; }
	void set(int s, String m, String n, String l, Consumer<ClickType> a) { inv.setItem(s, Compat.item(m, n, l == null ? null : Arrays.asList(l.split("\n")))); if (a != null) acts.put(s, a); }
	void show() { acts.clear(); ItemStack f = Compat.item(PANE, " ", null); for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, f); draw.run(); }
	void open(Runnable d) { draw = d; show(); p.openInventory(inv); OPEN.add(this); }
	void page(int page, int size, IntConsumer each, IntConsumer go) {
		for (int i = page * 45; i < Math.min(size, page * 45 + 45); i++) each.accept(i);
		if (page > 0) set(45, ARROW, g.t(IT_PREV), null, c -> go.accept(page - 1));
		if (size > page * 45 + 45) set(53, ARROW, g.t(IT_NEXT), null, c -> go.accept(page + 1));
	}
	static Menu of(Inventory i) { for (Menu m : OPEN) if (m.inv.equals(i)) return m; return null; }
	static void click(InventoryClickEvent e) { Menu m = of(e.getInventory()); if (m == null) return; e.setCancelled(true); Consumer<ClickType> a = m.acts.get(e.getRawSlot()); ClickType c = e.getClick(); if (a != null && m.p.hasPermission(Core.PERM)) Bukkit.getScheduler().runTask(Core.plugin, () -> { if (OPEN.contains(m)) a.accept(c); }); }
	static void refresh() { OPEN.removeIf(m -> m.inv.getViewers().isEmpty()); for (Menu m : new ArrayList<>(OPEN)) if (m.live) m.show(); }
	static void closeAll() { for (Menu m : new ArrayList<>(OPEN)) if (m.inv.getViewers().contains(m.p)) m.p.closeInventory(); OPEN.clear(); SEL.clear(); }
	static Sel sel(Player p) { Sel s = SEL.computeIfAbsent(p.getUniqueId(), u -> new Sel()); if (s.world == null || Bukkit.getWorld(s.world) == null) s.world = p.getWorld().getName(); return s; }
	static String icon(String world) { World w = Bukkit.getWorld(world); String e = w == null ? "" : w.getEnvironment().name(); return e.equals("NETHER") ? "NETHERRACK" : e.equals("THE_END") ? "END_STONE|ENDER_STONE" : "GRASS_BLOCK|GRASS"; }
	static String shape(Lang g, Job j) { return g.t(j.circle ? SH_CIRCLE : SH_SQUARE); }
	static String jobLore(Lang g, Job j) { return g.t(L_JOB, j.status(g), j.percent(), j.eta(g)); }
	static String summary(Lang g, Job j, int center) { return g.t(L_SUMMARY, j.world, g.t(CENTERS[center]), j.cx, j.cz, j.radius, shape(g, j), j.total) + (j.total > 1000000 ? "\n" + g.t(L_HUGE) : ""); }
	static String bar(double pct) { int n = (int) Math.min(20, pct / 5); return "§a" + new String(new char[n]).replace('\0', '█') + "§8" + new String(new char[20 - n]).replace('\0', '█'); }
	static void home(Player p) {
		Menu m = new Menu(p, 3, TI_MAIN, true);
		m.open(() -> {
			Lang g = m.g; Sel s = sel(p); Job j = s.job(p); List<Job> run = Engine.running();
			m.set(4, "NETHER_STAR", g.t(IT_SERVER), Engine.server(g), null);
			m.set(10, icon(s.world), g.t(IT_WORLD, s.world), g.t(L_SELECT), c -> worlds(p, 0));
			m.set(11, "BEACON", g.t(IT_RADIUS, s.radius), g.t(L_RADIUS, s.radius * 2L, s.radius * 2L, j.total), c -> radius(p));
			m.set(12, "COMPASS", g.t(IT_AREA), g.t(L_AREA, g.t(CENTERS[s.center]), j.cx, j.cz, shape(g, j)), c -> { if (c.isRightClick()) s.circle = !s.circle; else s.center = (s.center + 1) % 3; m.show(); });
			m.set(13, "EMERALD_BLOCK", g.t(IT_START), g.t(L_START, s.world, j.total), c -> start(p, s));
			m.set(14, "BOOK", g.t(IT_MANAGE), g.t(L_MANAGE, Engine.JOBS.size()), c -> jobs(p, 0));
			m.set(15, CLOCK, g.t(IT_PROGRESS), run.isEmpty() ? g.t(L_NO_ACTIVE) : jobLore(g, run.get(0)) + "\n" + g.t(L_DETAILS), c -> { List<Job> r = Engine.running(); if (r.size() == 1) job(p, r.get(0)); else jobs(p, 0); });
			m.set(16, AUTO, g.t(IT_AUTO), Engine.auto(g), null);
			m.set(22, "BARRIER", g.t(IT_CLOSE), null, c -> p.closeInventory());
		});
	}
	static void start(Player p, Sel s) {
		Job j = s.job(p), e = Engine.find(j.world); Lang g = Lang.of(p); s.job = null;
		if (e != null && e.state != Job.DONE) { Engine.msg(p, EXISTS, j.world); job(p, e); }
		else confirm(p, g.t(IT_SUMMARY), summary(g, j, s.center), () -> { if (Engine.start(p, j)) job(p, j); else home(p); }, () -> home(p));
	}
	static void worlds(Player p, int page) {
		Menu m = new Menu(p, 6, TI_WORLDS, false); List<World> ws = Bukkit.getWorlds();
		m.open(() -> { m.page(page, ws.size(), i -> { String w = ws.get(i).getName(); m.set(i % 45, icon(w), "§a" + w, m.g.t(L_SELECT), c -> { sel(p).world = w; home(p); }); }, n -> worlds(p, n)); m.set(49, ARROW, m.g.t(IT_BACK), null, c -> home(p)); });
	}
	static void radius(Player p) {
		Menu m = new Menu(p, 4, TI_RADIUS, false);
		m.open(() -> {
			Lang g = m.g; Sel s = sel(p); World w = Bukkit.getWorld(s.world); int[] d = {-10000, -1000, -100, 100, 1000, 10000}, v = {500, 1000, 2500, 5000, 10000, 25000, w == null ? 16 : (int) Math.min(Job.MAX, w.getWorldBorder().getSize() / 2)};
			m.set(4, "BEACON", g.t(IT_RADIUS, s.radius), g.t(L_RADIUS, s.radius * 2L, s.radius * 2L, s.job(p).total), null);
			for (int i = 0; i < 6; i++) { int x = d[i]; m.set(i < 3 ? 10 + i : 11 + i, x < 0 ? "RED_STAINED_GLASS_PANE|STAINED_GLASS_PANE:14" : "LIME_STAINED_GLASS_PANE|STAINED_GLASS_PANE:5", g.t(IT_DELTA, (x < 0 ? "§c" : "§a+") + g.num(x)), null, c -> { s.radius(s.radius + x); m.show(); }); }
			for (int i = 0; i < 7; i++) { int x = v[i]; m.set(19 + i, i < 6 ? "PAPER" : "IRON_BARS|IRON_FENCE", g.t(i < 6 ? IT_PRESET : IT_BORDER, x), null, c -> { s.radius(x); m.show(); }); }
			m.set(31, ARROW, g.t(IT_BACK), null, c -> home(p));
		});
	}
	static void confirm(Player p, String name, String lore, Runnable yes, Runnable no) {
		Menu m = new Menu(p, 3, TI_CONFIRM, false);
		m.open(() -> { m.set(13, "PAPER", name, lore, null); m.set(11, YES, m.g.t(IT_CONFIRM), null, c -> yes.run()); m.set(15, NO, m.g.t(IT_CANCEL), null, c -> no.run()); });
	}
	static void jobs(Player p, int page) {
		Menu m = new Menu(p, 6, TI_JOBS, true);
		m.open(() -> {
			List<Job> js = Engine.JOBS; if (js.isEmpty()) m.set(22, "BARRIER", m.g.t(NONE), null, null);
			m.page(page, js.size(), i -> { Job j = js.get(i); m.set(i % 45, icon(j.world), "§a" + j.world, jobLore(m.g, j) + "\n" + m.g.t(L_DETAILS), c -> job(p, j)); }, n -> jobs(p, n));
			m.set(49, ARROW, m.g.t(IT_BACK), null, c -> home(p));
		});
	}
	static void job(Player p, Job j) {
		Menu m = new Menu(p, 5, TI_JOB, true);
		m.open(() -> {
			Lang g = m.g; m.set(40, ARROW, g.t(IT_BACK), null, c -> jobs(p, 0));
			if (!Engine.JOBS.contains(j)) return;
			m.set(4, icon(j.world), "§a" + j.world, g.t(L_STATUS, j.status(g)), null);
			m.set(13, "EXPERIENCE_BOTTLE|EXP_BOTTLE", g.t(IT_BAR, j.percent()), g.t(L_BAR, bar(j.percent()), j.done, j.total, j.total - j.done), null);
			m.set(20, "FEATHER", g.t(IT_SPEED), g.t(L_SPEED, j.speed(g), Compat.ASYNC ? j.inflight : j.state == Job.RUN ? Engine.batch : 0), null);
			m.set(21, CLOCK, g.t(IT_TIME), g.t(L_TIME, Lang.time(j.elapsed / 1000), j.eta(g)), null);
			m.set(22, "COMPASS", g.t(IT_AREA), g.t(L_AREA_INFO, j.cx, j.cz, j.radius, shape(g, j)), null);
			m.set(23, "REDSTONE", g.t(IT_PERF), Engine.perf(g), null);
			m.set(24, AUTO, g.t(IT_AUTO), Engine.auto(g), null);
			if (j.state == Job.DONE) { m.set(38, NO, g.t(IT_REMOVE), null, c -> { Engine.remove(j); jobs(p, 0); }); return; }
			m.set(38, j.state == Job.RUN ? "YELLOW_WOOL|WOOL:4" : YES, g.t(j.state == Job.RUN ? IT_PAUSE : IT_RESUME), null, c -> { if (j.state == Job.RUN) Engine.pause(p, j); else Engine.resume(p, j); m.show(); });
			m.set(42, NO, g.t(IT_CANCEL), null, c -> confirm(p, g.t(IT_CANCEL_ASK, j.world), g.t(L_CANCEL_ASK), () -> { Engine.cancel(p, j); jobs(p, 0); }, () -> job(p, j)));
		});
	}
	static final class Sel {
		String world; int center, radius = 2000; boolean circle; Job job;
		void radius(int v) { radius = Math.max(16, Math.min(Job.MAX, v)); }
		Job job(Player p) {
			World w = Bukkit.getWorld(world); Location l = w == null || center == 1 ? p.getLocation() : center == 2 ? w.getWorldBorder().getCenter() : w.getSpawnLocation(); int x = l.getBlockX(), z = l.getBlockZ();
			if (job == null || !job.world.equals(world) || job.cx != x || job.cz != z || job.radius != radius || job.circle != circle) job = new Job(world, x, z, radius, circle);
			return job;
		}
	}
}
