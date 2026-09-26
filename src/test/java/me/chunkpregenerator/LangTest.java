package me.chunkpregenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;

public class LangTest extends TestCase {
	static final Pattern SPEC = Pattern.compile("%[,.0-9]*[a-zA-Z%]");
	static List<String> specs(String s) { List<String> r = new ArrayList<>(); Matcher m = SPEC.matcher(s); while (m.find()) if (!m.group().equals("%%")) r.add(m.group()); return r; }
	public void testTexts() {
		for (Lang.T t : Lang.T.values()) {
			assertEquals(t.name(), Lang.values().length, t.v.length); List<String> ref = specs(t.v[0]); List<Object> args = new ArrayList<>();
			for (String s : ref) args.add(s.endsWith("s") ? "x" : s.endsWith("f") ? (Object) 1.5 : (Object) 1234L);
			for (Lang g : Lang.values()) {
				String v = t.v[g.ordinal()]; assertFalse(t.name(), v.trim().isEmpty()); assertEquals(t.name(), ref, specs(v)); assertNotNull(g.t(t, args.toArray()));
				if (t.name().startsWith("TI_")) assertTrue(t.name(), v.length() <= 32);
			}
		}
	}
	public void testDetect() {
		assertEquals(Lang.PT, Lang.of("pt_br")); assertEquals(Lang.PT, Lang.of("pt_PT")); assertEquals(Lang.PT, Lang.of("pt_BR")); assertEquals(Lang.EN, Lang.of("en_us"));
		assertEquals(Lang.EN, Lang.of("de_de")); assertEquals(Lang.EN, Lang.of("")); assertEquals(Lang.EN, Lang.of((String) null));
	}
	public void testFormat() {
		assertEquals("00:01:02", Lang.time(62)); assertEquals("1d 01:00:00", Lang.time(90000)); assertEquals("00:00:00", Lang.time(0));
		assertEquals("1.234.567", Lang.PT.num(1234567)); assertEquals("1,234,567", Lang.EN.num(1234567)); assertEquals("§aProgresso: §f45,5%", Lang.PT.t(Lang.T.IT_BAR, 45.5));
	}
}
