package me.chunkpregenerator;

import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

enum Lang {
	EN("en", Locale.US), PT("pt", Locale.forLanguageTag("pt-BR"));
	final String code; final Locale loc;
	Lang(String code, Locale loc) { this.code = code; this.loc = loc; }
	static Lang of(CommandSender s) { return of(s instanceof Player ? Compat.locale((Player) s) : Locale.getDefault().toString()); }
	static Lang of(String l) { for (Lang g : values()) if (l != null && l.toLowerCase(Locale.ROOT).startsWith(g.code)) return g; return EN; }
	String t(T k, Object... a) { return String.format(loc, k.v[ordinal()], a); }
	String num(long n) { return String.format(loc, "%,d", n); }
	static String time(long s) { return (s >= 86400 ? s / 86400 + "d " : "") + String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600 % 24, s / 60 % 60, s % 60); }
	enum T {
		PREFIX("§6[Chunk] §7", "§6[Chunk] §7"),
		NO_PERM("§cYou don't have permission to do that.", "§cVocê não tem permissão para isso."),
		USAGE("§eConsole: /chunkpregenerator [start <world> <radius> [confirm] | pause|resume|cancel <world>]", "§eConsole: /chunkpregenerator [start <mundo> <raio> [confirm] | pause|resume|cancel <mundo>]"),
		NONE("§7No generations.", "§7Nenhuma geração."),
		STATUS_LINE("§f%s§7: %s §f%.1f%% §8(%,d/%,d) §7%s §8| §7ETA %s", "§f%s§7: %s §f%.1f%% §8(%,d/%,d) §7%s §8| §7Resta %s"),
		STARTED("§aGeneration started in §f%s§a: §f%,d §achunks.", "§aGeração iniciada em §f%s§a: §f%,d §achunks."),
		PAUSED_MSG("§eGeneration paused in §f%s§e.", "§eGeração pausada em §f%s§e."),
		RESUMED_MSG("§aGeneration resumed in §f%s§a.", "§aGeração retomada em §f%s§a."),
		CANCELLED_MSG("§cGeneration cancelled in §f%s§c.", "§cGeração cancelada em §f%s§c."),
		COMPLETED_MSG("§aGeneration finished in §f%s§a: §f%,d §achunks in §f%s§a.", "§aGeração concluída em §f%s§a: §f%,d §achunks em §f%s§a."),
		RESTORED("§e%d unfinished generation(s) restored as paused. Use /chunkpregenerator to continue.", "§e%d geração(ões) incompleta(s) restaurada(s) como pausada(s). Use /chunkpregenerator para continuar."),
		ENABLED("§aChunkPreGenerator %s enabled. §7Mode: §f%s", "§aChunkPreGenerator %s ativado. §7Modo: §f%s"),
		CRITICAL_MSG("§cServer overloaded: new chunks are on hold until it recovers.", "§cServidor sobrecarregado: novos chunks aguardam até ele se recuperar."),
		RECOVERED_MSG("§aServer recovered: generation is resuming gradually.", "§aServidor recuperado: a geração está sendo retomada aos poucos."),
		WORLD_MISSING("§cWorld §f%s§c is not loaded.", "§cO mundo §f%s§c não está carregado."),
		EXISTS("§cThere is already an unfinished generation for §f%s§c.", "§cJá existe uma geração não concluída para §f%s§c."),
		NOT_FOUND("§cNo generation found for §f%s§c.", "§cNenhuma geração encontrada para §f%s§c."),
		BAD_RADIUS("§cInvalid radius. Use a value between 16 and 29,999,984.", "§cRaio inválido. Use um valor entre 16 e 29.999.984."),
		CONFIRM_HINT("§7Repeat the command with §fconfirm§7 at the end to start.", "§7Repita o comando com §fconfirm§7 no final para iniciar."),
		ERROR_MSG("§cError on chunk %d, %d in %s: %s", "§cErro no chunk %d, %d em %s: %s"),
		FILE_ERROR("§cCould not access generations.dat: %s", "§cNão foi possível acessar generations.dat: %s"),
		S_RUNNING("§aGENERATING", "§aGERANDO"), S_PAUSED("§ePAUSED", "§ePAUSADO"), S_DONE("§bCOMPLETED", "§bCONCLUÍDO"),
		H_OK("§aHealthy", "§aSaudável"), H_HIGH("§eReducing speed", "§eReduzindo velocidade"), H_CRIT("§cCritical - holding", "§cCrítico - aguardando"),
		M_ASYNC("Paper asynchronous", "Paper assíncrono"), M_TICKET("Spigot asynchronous (tickets)", "Spigot assíncrono (tickets)"), M_SYNC("Synchronous (time budget)", "Síncrono (orçamento de tempo)"), M_LEGACY("Legacy 1.8-1.12", "Legado 1.8-1.12"),
		C_SPAWN("Spawn", "Spawn"), C_PLAYER("Your position", "Sua posição"), C_BORDER("World border", "Borda do mundo"),
		SH_SQUARE("Square", "Quadrado"), SH_CIRCLE("Circle", "Círculo"),
		CALC("Calculating...", "Calculando..."), NA("n/a", "n/d"), SPEED("%.1f chunks/s", "%.1f chunks/s"),
		TI_MAIN("§8ChunkPreGenerator", "§8ChunkPreGenerator"), TI_WORLDS("§8Select world", "§8Selecionar mundo"), TI_RADIUS("§8Set radius", "§8Definir raio"),
		TI_CONFIRM("§8Confirm", "§8Confirmar"), TI_JOBS("§8Generations", "§8Gerações"), TI_JOB("§8Generation", "§8Geração"),
		IT_SERVER("§bServer information", "§bInformações do servidor"),
		L_SERVER("§7Platform: §f%s\n%s\n§7CPU cores: §f%d\n§7Running generations: §f%d", "§7Plataforma: §f%s\n%s\n§7Núcleos de CPU: §f%d\n§7Gerações em andamento: §f%d"),
		L_PERF("§7State: %s\n§7TPS: §f%.1f\n§7MSPT: §f%s\n§7JVM memory: §f%,d / %,d MB", "§7Estado: %s\n§7TPS: §f%.1f\n§7MSPT: §f%s\n§7Memória JVM: §f%,d / %,d MB"),
		IT_WORLD("§aWorld: §f%s", "§aMundo: §f%s"),
		L_SELECT("§eClick to select.", "§eClique para selecionar."),
		IT_RADIUS("§aRadius: §f%,d blocks", "§aRaio: §f%,d blocos"),
		L_RADIUS("§7Area: §f%,d x %,d blocks\n§7Chunks: §f%,d\n§eClick to change.", "§7Área: §f%,d x %,d blocos\n§7Chunks: §f%,d\n§eClique para alterar."),
		IT_AREA("§aGeneration area", "§aÁrea de geração"),
		L_AREA("§7Center: §f%s §7(%d, %d)\n§7Shape: §f%s\n§eLeft click: change center\n§eRight click: change shape", "§7Centro: §f%s §7(%d, %d)\n§7Formato: §f%s\n§eClique esquerdo: mudar centro\n§eClique direito: mudar formato"),
		IT_START("§aStart generation", "§aIniciar geração"),
		L_START("§7World: §f%s\n§7Chunks: §f%,d\n§eClick to review and confirm.", "§7Mundo: §f%s\n§7Chunks: §f%,d\n§eClique para revisar e confirmar."),
		IT_MANAGE("§6Manage generations", "§6Gerenciar gerações"),
		L_MANAGE("§7Generations: §f%d\n§eClick to open.", "§7Gerações: §f%d\n§eClique para abrir."),
		IT_PROGRESS("§bProgress", "§bProgresso"),
		L_NO_ACTIVE("§7No generation running.", "§7Nenhuma geração em andamento."),
		L_DETAILS("§eClick for details.", "§eClique para ver detalhes."),
		IT_AUTO("§dAutomatic settings", "§dConfigurações automáticas"),
		L_AUTO("§7Mode: §f%s\n§7%s\n§7State: %s\n§8Threads, RAM, speed and simultaneous\n§8chunks are adjusted automatically.", "§7Modo: §f%s\n§7%s\n§7Estado: %s\n§8Threads, RAM, velocidade e chunks\n§8simultâneos são ajustados automaticamente."),
		L_LIMIT("Simultaneous chunks: §f%d / %d", "Chunks simultâneos: §f%d / %d"),
		L_BUDGET("Time per tick: §f%.1f ms", "Tempo por tick: §f%.1f ms"),
		IT_CLOSE("§cClose", "§cFechar"), IT_BACK("§eBack", "§eVoltar"), IT_PREV("§ePrevious page", "§ePágina anterior"), IT_NEXT("§eNext page", "§ePróxima página"),
		IT_DELTA("%s §7blocks", "%s §7blocos"), IT_PRESET("§e%,d blocks", "§e%,d blocos"), IT_BORDER("§eWorld border §7(%,d blocks)", "§eBorda do mundo §7(%,d blocos)"),
		IT_CONFIRM("§aConfirm", "§aConfirmar"), IT_CANCEL("§cCancel", "§cCancelar"),
		IT_SUMMARY("§eYou are about to generate:", "§eVocê está prestes a gerar:"),
		L_SUMMARY("§7World: §f%s\n§7Center: §f%s §7(%d, %d)\n§7Radius: §f%,d blocks\n§7Shape: §f%s\n§7Chunks: §f%,d", "§7Mundo: §f%s\n§7Centro: §f%s §7(%d, %d)\n§7Raio: §f%,d blocos\n§7Formato: §f%s\n§7Chunks: §f%,d"),
		L_HUGE("§cVery large generation! It may take hours\n§cand use several GB of disk.", "§cGeração muito grande! Pode levar horas\n§ce ocupar vários GB de disco."),
		IT_CANCEL_ASK("§cCancel the generation of %s?", "§cCancelar a geração de %s?"),
		L_CANCEL_ASK("§7The progress will be lost.", "§7O progresso será perdido."),
		L_JOB("§7Status: %s\n§7Progress: §f%.1f%%\n§7Remaining time: §f%s", "§7Status: %s\n§7Progresso: §f%.1f%%\n§7Tempo restante: §f%s"),
		IT_BAR("§aProgress: §f%.1f%%", "§aProgresso: §f%.1f%%"),
		L_BAR("%s\n§7Chunks: §f%,d / %,d\n§7Remaining: §f%,d", "%s\n§7Chunks: §f%,d / %,d\n§7Restantes: §f%,d"),
		IT_SPEED("§aSpeed", "§aVelocidade"), L_SPEED("§f%s\n§7Processing now: §f%d chunks", "§f%s\n§7Em processamento: §f%d chunks"),
		IT_TIME("§aTime", "§aTempo"), L_TIME("§7Elapsed: §f%s\n§7Remaining: §f%s", "§7Decorrido: §f%s\n§7Restante: §f%s"),
		L_AREA_INFO("§7Center: §f%d, %d\n§7Radius: §f%,d blocks\n§7Shape: §f%s", "§7Centro: §f%d, %d\n§7Raio: §f%,d blocos\n§7Formato: §f%s"),
		IT_PERF("§aPerformance", "§aDesempenho"),
		IT_PAUSE("§ePause", "§ePausar"), IT_RESUME("§aResume", "§aContinuar"), IT_REMOVE("§cRemove from list", "§cRemover da lista"),
		L_STATUS("§7Status: %s", "§7Status: %s");
		final String[] v;
		T(String... v) { this.v = v; }
	}
}
