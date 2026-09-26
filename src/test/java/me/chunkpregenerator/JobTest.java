package me.chunkpregenerator;

import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class JobTest extends TestCase {
	static final int[][] CASES = {{0, 0, 16}, {7, -9, 100}, {-5000, 7000, 300}, {1000, -1000, 700}, {-31, -31, 16}, {12345, 54321, 1000}, {-100000, -100000, 2500}, {29999000, 0, 3000}};
	static long key(int x, int z) { return (long) x << 32 | (z & 0xffffffffL); }
	static Set<Long> run(Job j) { Set<Long> s = new HashSet<>(); while (j.next()) { assertTrue(j.in(j.x, j.z)); assertTrue(s.add(key(j.x, j.z))); j.done++; } return s; }
	public void testCoverage() {
		for (int[] c : CASES) for (boolean circle : new boolean[] {false, true}) {
			Job j = new Job("w", c[0], c[1], c[2], circle); Set<Long> s = run(j); long n = 0;
			assertEquals(j.total, s.size()); assertEquals(j.total, j.done); assertFalse(j.next());
			for (int x = j.ccx - j.rc - 3; x <= j.ccx + j.rc + 3; x++) for (int z = j.ccz - j.rc - 3; z <= j.ccz + j.rc + 3; z++) if (j.in(x, z)) { n++; assertTrue(s.contains(key(x, z))); }
			assertEquals(n, j.total);
		}
	}
	public void testSquareCount() { assertEquals(9, new Job("w", 0, 0, 16, false).total); assertEquals(17 * 17, new Job("w", 0, 0, 128, false).total); assertEquals(5, new Job("w", 0, 0, 16, true).total); }
	public void testSpiral() {
		Job j = new Job("w", 0, 0, 16, false);
		for (int r = 0; r < 8; r++) { Set<Long> s = new HashSet<>(); for (int k = 0; k < (2 * r + 1) * (2 * r + 1); k++) { j.spiral(k); assertTrue(Math.abs(j.rx) <= r && Math.abs(j.rz) <= r); assertTrue(s.add(key(j.rx, j.rz))); } }
	}
	public void testResume() {
		Job a = new Job("w", 100, -200, 900, true); Set<Long> all = run(new Job("w", 100, -200, 900, true)), s = new HashSet<>();
		for (int i = 0; i < 1234 && a.next(); i++) { s.add(key(a.x, a.z)); a.done++; }
		a.ckCursor = a.cursor; a.ckDone = a.done; a.elapsed = 5000; Job b = Job.parse(a.line());
		assertEquals(a.done, b.done); assertEquals(a.cursor, b.cursor); assertEquals(5000, b.elapsed); assertEquals(Job.PAUSED, b.state); assertEquals(a.total, b.total);
		while (b.next()) { assertTrue(s.add(key(b.x, b.z))); b.done++; }
		assertEquals(all, s); assertEquals(b.total, b.done); assertNull(Job.parse("broken"));
	}
	public void testReset() {
		Job j = new Job("w", 0, 0, 2000, false); for (int i = 0; i < 50; i++) j.next();
		j.ckCursor = j.cursor; j.ckDone = 50; for (int i = 0; i < 70; i++) j.next(); j.done = 120; j.inflight = 9; int e = j.epoch;
		j.reset(); assertEquals(j.ckCursor, j.cursor); assertEquals(50, j.done); assertEquals(0, j.inflight); assertEquals(e + 1, j.epoch);
		j.drain = true; assertFalse(j.checkpoint()); assertFalse(j.drain); j.next(); j.done++; assertTrue(j.checkpoint()); assertEquals(j.cursor, j.ckCursor); assertEquals(51, j.ckDone);
	}
	public void testLegacyFuture() {
		Job j = new Job("w", 40, -40, 600, false); Set<Long> all = run(new Job("w", 40, -40, 600, false)), seen = new HashSet<>();
		while (j.next()) {
			seen.add(key(j.x, j.z));
			for (int i = 1; i < 4; i++) { int a = j.x + (i & 1), b = j.z + (i >> 1); if (j.future(a, b)) { assertFalse(seen.contains(key(a, b))); assertTrue(all.contains(key(a, b))); } }
		}
	}
	public void testSpeedAndEta() {
		Job j = new Job("w", 0, 0, 5000, false); j.state = Job.RUN; Lang g = Lang.EN;
		for (int i = 0; i < 5; i++) { j.done = i * 100; j.sample(i * 1000000000L); }
		assertEquals(-1.0, j.speed()); assertEquals(-1, j.eta()); assertEquals("Calculating...", j.eta(g));
		for (int i = 5; i < 40; i++) { j.done = i * 100; j.sample(i * 1000000000L); }
		assertEquals(100.0, j.speed(), 1e-9); assertEquals((j.total - j.done) / 100, j.eta()); assertTrue(j.eta(g).startsWith("~"));
		j.state = Job.PAUSED; assertEquals("-", j.eta(g)); assertEquals("-", j.speed(g));
	}
}
