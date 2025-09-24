package me.chunkpregenerator.utils;

import org.bukkit.Bukkit;


import lombok.AllArgsConstructor;
import lombok.Getter;
import me.chunkpregenerator.Core;

@Getter
@AllArgsConstructor
public class Task {
	
	private final Runnable runnable;
	
	public void run() {
		if (this.runnable == null) return;
		this.runnable.run();
	}
	
	public static void run(Runnable runnable) {
		if (!Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().runTask(Core.getInstance(), runnable);
			return;
		}
		runnable.run();
	}
	
	public static void runAsync(Runnable runnable) {
		if (Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), runnable);
			return;
		}
		runnable.run();
	}
	
}
