package me.chunkpregenerator.objects;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import me.chunkpregenerator.Core;
import me.chunkpregenerator.utils.Task;

@Getter
public class Manager {

	private final List<Task> tasks = new ArrayList<>();
	
	public static Manager get() {return Core.getInstance().getManager();}
	
}
