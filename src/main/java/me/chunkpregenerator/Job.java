package me.chunkpregenerator;

import static me.chunkpregenerator.Lang.T.*;

final class Job {
	static final int RUN = 0, PAUSED = 1, DONE = 2, MAX = 29999984, LIMIT = 1875000;
	final String world; final int cx, cz, radius, rc, ccx, ccz, rcx, rcz; final boolean circle; final long slots, total; final long[] hd = new long[60], ht = new long[60], win = new long[512]; final boolean[] ok = new boolean[512];
	long cursor, done, elapsed, ckCursor, ckDone, rk = -1, head, tail; int state = PAUSED, inflight, epoch, errors, hn, x, z, rx, rz;
	Job(String world, int cx, int cz, int radius, boolean circle) {
		this.world = world; this.cx = cx; this.cz = cz; this.radius = radius; this.circle = circle; rc = (radius + 15) >> 4; ccx = cx >> 4; ccz = cz >> 4; rcx = ccx >> 5; rcz = ccz >> 5;
		long r = Math.max(Math.max(rcx - ((ccx - rc) >> 5), ((ccx + rc) >> 5) - rcx), Math.max(rcz - ((ccz - rc) >> 5), ((ccz + rc) >> 5) - rcz)), n = 0;
		for (long dz = -rc; dz <= rc; dz++) { long w = circle ? isqrt((long) rc * rc - dz * dz) : rc; if (Math.abs(ccz + dz) <= LIMIT) n += Math.max(0, Math.min(ccx + w, LIMIT) - Math.max(ccx - w, -LIMIT) + 1); }
		slots = (2 * r + 1) * (2 * r + 1) << 10; total = n;
	}
	static long isqrt(long v) { long r = (long) Math.sqrt(v); while (r * r > v) r--; while ((r + 1) * (r + 1) <= v) r++; return r; }
	boolean in(int x, int z) { long dx = x - ccx, dz = z - ccz; return Math.abs(x) <= LIMIT && Math.abs(z) <= LIMIT && Math.abs(dx) <= rc && Math.abs(dz) <= rc && (!circle || dx * dx + dz * dz <= (long) rc * rc); }
	boolean future(int a, int b) { return in(a, b) && a >> 5 == rx && b >> 5 == rz; }
	long slot(int x, int z) { long dx = (x >> 5) - rcx, dz = (z >> 5) - rcz, r = Math.max(Math.abs(dx), Math.abs(dz)); return (r == 0 ? 0 : (2 * r - 1) * (2 * r - 1) + (dx == r && dz > -r ? dz + r - 1 : dz == r ? 3 * r - 1 - dx : dx == -r ? 5 * r - 1 - dz : 7 * r - 1 + dx)) << 10 | (z & 31) << 5 | x & 31; }
	boolean next() {
		while (cursor < slots) {
			long s = cursor++, k = s >> 10; int l = (int) (s & 1023);
			if (k != rk) { rk = k; spiral(k); if (!in(Math.max(rx << 5, Math.min(ccx, (rx << 5) + 31)), Math.max(rz << 5, Math.min(ccz, (rz << 5) + 31)))) { cursor = (k + 1) << 10; continue; } }
			x = (rx << 5) + (l & 31); z = (rz << 5) + (l >> 5); if (in(x, z)) return true;
		}
		return false;
	}
	void spiral(long k) {
		if (k == 0) { rx = rcx; rz = rcz; return; }
		long r = (long) ((Math.sqrt(k) + 1) / 2); while ((2 * r - 1) * (2 * r - 1) > k) r--; while ((2 * r + 1) * (2 * r + 1) <= k) r++;
		long t = k - (2 * r - 1) * (2 * r - 1), s = t / (2 * r), o = t % (2 * r);
		rx = (int) (rcx + (s == 0 ? r : s == 1 ? r - 1 - o : s == 2 ? -r : o + 1 - r)); rz = (int) (rcz + (s == 0 ? o + 1 - r : s == 1 ? r : s == 2 ? r - 1 - o : -r));
	}
	void sample(long now) { hd[hn % hd.length] = done; ht[hn++ % hd.length] = now; }
	double speed() { int n = hd.length, k = Math.min(hn - 1, n - 1), a = (hn - 1) % n, b = (hn - 1 - k) % n; return k < 5 ? -1 : (hd[a] - hd[b]) * 1e9 / Math.max(1, ht[a] - ht[b]); }
	long eta() { double s = speed(); return s > 0 ? (long) ((total - done) / s) : -1; }
	double percent() { return total == 0 ? 100 : Math.min(100, done * 100.0 / total); }
	void reset() { cursor = ckCursor; done = ckDone; inflight = 0; epoch++; head = tail = 0; rk = -1; }
	boolean full() { return tail - head >= win.length; }
	long push() { long s = tail++; win[(int) (s & 511)] = cursor; ok[(int) (s & 511)] = false; return s; }
	boolean complete(long s) { ok[(int) (s & 511)] = true; boolean m = false; for (; head < tail && ok[(int) (head & 511)]; head++, ckDone++, m = true) ckCursor = win[(int) (head & 511)]; return m; }
	void error(int x, int z, Throwable t) { if (errors++ == 0) Engine.log(ERROR_MSG, x, z, world, t); }
	String status(Lang g) { return g.t(state == RUN ? S_RUNNING : state == PAUSED ? S_PAUSED : S_DONE); }
	String speed(Lang g) { double s = speed(); return state != RUN ? "-" : s < 0 ? g.t(CALC) : g.t(SPEED, s); }
	String eta(Lang g) { long e = eta(); return state != RUN ? "-" : e < 0 ? g.t(CALC) : "~" + Lang.time(e); }
	String line() { return world + "\t" + cx + "\t" + cz + "\t" + radius + "\t" + circle + "\t" + ckCursor + "\t" + ckDone + "\t" + elapsed; }
	static Job parse(String l) {
		try { String[] p = l.split("\t"); Job j = new Job(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Boolean.parseBoolean(p[4])); j.cursor = j.ckCursor = Long.parseLong(p[5]); j.done = j.ckDone = Long.parseLong(p[6]); j.elapsed = Long.parseLong(p[7]); return j; } catch (Exception e) { return null; }
	}
}
