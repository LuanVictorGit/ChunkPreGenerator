package me.chunkpregenerator;

import static me.chunkpregenerator.EngineTest.*;
import static me.chunkpregenerator.Lang.T.*;
import static org.bukkit.event.inventory.ClickType.*;
import java.io.File;
import java.util.List;
import junit.framework.TestCase;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;

public class MenuTest extends TestCase {
	static String name(int s) { return open.getItem(s).getItemMeta().getDisplayName(); }
	static String lore(int s) { List<String> l = open.getItem(s).getItemMeta().getLore(); return l == null ? "" : String.join("\n", l); }
	static void at(Lang.T t) { assertNotNull(open); assertEquals(Lang.PT.t(t), open.getTitle()); }
	static void click(int slot, ClickType c) {
		Inventory top = open; InventoryClickEvent e = new InventoryClickEvent(new InventoryView() { public Inventory getTopInventory() { return top; } public Inventory getBottomInventory() { return null; } public HumanEntity getPlayer() { return PLAYER; } public InventoryType getType() { return InventoryType.CHEST; } }, InventoryType.SlotType.CONTAINER, slot, c, InventoryAction.PICKUP_ALL);
		Menu.click(e); assertTrue(e.isCancelled());
	}
	public void testMenus() throws Exception {
		Engine.JOBS.clear(); Menu.OPEN.clear(); Menu.SEL.clear(); Engine.file = File.createTempFile("menu", ".dat"); present = true;
		Menu.home(PLAYER); at(TI_MAIN); assertTrue(name(4).contains("Informações do servidor") && lore(4).contains("Núcleos de CPU")); assertTrue(lore(15).contains("Nenhuma geração")); assertTrue(lore(16).contains("Legado 1.8-1.12"));
		assertTrue(lore(12).contains("Spawn §7(100, -100)")); click(12, LEFT); assertTrue(lore(12).contains("Sua posição §7(-500, 900)")); click(12, RIGHT); assertTrue(lore(12).contains("Círculo")); click(12, LEFT); click(12, LEFT); assertTrue(lore(12).contains("Spawn"));
		Inventory home = open; click(40, LEFT); click(4, LEFT); assertSame(home, open);
		click(11, LEFT); at(TI_RADIUS); Menu.Sel s = Menu.sel(PLAYER); click(15, LEFT); assertEquals(3000, s.radius); click(10, LEFT); assertEquals(16, s.radius); click(25, LEFT); assertEquals(Job.MAX, s.radius); click(19, LEFT); assertEquals(500, s.radius);
		click(31, LEFT); at(TI_MAIN); assertTrue(name(11).contains("500"));
		click(10, LEFT); at(TI_WORLDS); click(0, LEFT); at(TI_MAIN); assertEquals("world", s.world);
		click(13, LEFT); at(TI_CONFIRM); assertTrue(lore(13).contains("500 blocos") && lore(13).contains("Círculo")); click(15, LEFT); at(TI_MAIN); assertTrue(Engine.JOBS.isEmpty());
		click(13, LEFT); click(11, LEFT); at(TI_JOB); Job j = Engine.find("world"); assertNotNull(j); assertEquals(Job.RUN, j.state); assertTrue(j.circle); assertEquals(500, j.radius);
		String before = lore(13); j.done += 7; Menu.refresh(); assertFalse(before.equals(lore(13))); j.done -= 7;
		click(38, LEFT); assertEquals(Job.PAUSED, j.state); click(38, LEFT); assertEquals(Job.RUN, j.state);
		click(40, LEFT); at(TI_JOBS); assertTrue(name(0).contains("world")); click(0, LEFT); at(TI_JOB);
		click(40, LEFT); click(49, LEFT); at(TI_MAIN); assertTrue(lore(15).contains("GERANDO")); click(15, LEFT); at(TI_JOB);
		click(42, LEFT); at(TI_CONFIRM); click(15, LEFT); at(TI_JOB); assertEquals(1, Engine.JOBS.size());
		click(42, LEFT); click(11, LEFT); at(TI_JOBS); assertTrue(Engine.JOBS.isEmpty()); assertTrue(name(22).contains("Nenhuma geração"));
		click(49, LEFT); at(TI_MAIN); click(22, LEFT); assertNull(open); Menu.refresh(); assertTrue(Menu.OPEN.isEmpty());
		Menu.home(PLAYER); Menu.closeAll(); assertNull(open); assertTrue(Menu.OPEN.isEmpty() && Menu.SEL.isEmpty()); Engine.stop();
	}
}
