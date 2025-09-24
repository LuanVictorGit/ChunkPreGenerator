package me.chunkpregenerator.utils;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import me.chunkpregenerator.objects.Manager;

@Getter
public class Timer extends BukkitRunnable {

	private final Manager manager = Manager.get();
	
	@Override
	public void run() {
		manager.getTasks().forEach(Task::run);
	}

}
