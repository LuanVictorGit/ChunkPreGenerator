package me.chunkpregenerator;

import junit.framework.TestCase;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CompatTest extends TestCase {
	public void testLegacyMaterials() {
		for (String s : new String[] {Menu.PANE, Menu.YES, Menu.NO, Menu.CLOCK, Menu.AUTO, Menu.ARROW, "NETHER_STAR", "BEACON", "COMPASS", "EMERALD_BLOCK", "BOOK", "BARRIER", "PAPER", "IRON_BARS|IRON_FENCE", "RED_STAINED_GLASS_PANE|STAINED_GLASS_PANE:14", "LIME_STAINED_GLASS_PANE|STAINED_GLASS_PANE:5", "EXPERIENCE_BOTTLE|EXP_BOTTLE", "FEATHER", "REDSTONE", "YELLOW_WOOL|WOOL:4", "NETHERRACK", "END_STONE|ENDER_STONE", "GRASS_BLOCK|GRASS"}) assertNotSame(s, Material.STONE, Compat.parse(s).getType());
		assertEquals(Material.STAINED_GLASS_PANE, Compat.parse(Menu.PANE).getType()); assertEquals(7, Compat.parse(Menu.PANE).getDurability()); assertEquals(Material.STONE, Compat.parse("NOPE|NADA:3").getType());
	}
	public void testLegacyLocale() {
		Player p = EngineTest.proxy(Player.class, (x, m, a) -> m.getName().equals("spigot") ? new Player.Spigot() { public String getLocale() { return "pt_BR"; } } : EngineTest.def(m));
		assertNotNull(Compat.LOCALE); assertEquals("pt_BR", Compat.locale(p)); assertEquals(Lang.PT, Lang.of(p)); assertNull(Compat.MSPT); assertEquals(-1.0, Compat.mspt());
		assertNull(Compat.PAPER); assertNull(Compat.NMS); assertFalse(Compat.ASYNC); Compat.release(null, 0, 0);
	}
}
