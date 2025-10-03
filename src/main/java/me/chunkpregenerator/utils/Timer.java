package me.chunkpregenerator.utils;
import java.util.TimerTask;

import lombok.Getter;
import me.chunkpregenerator.objects.Manager;

@Getter
public class Timer extends TimerTask {

	private final Manager manager = Manager.get();
	
	@Override
	public void run() {
		manager.getTasks().forEach(Task::run);
	}

}
