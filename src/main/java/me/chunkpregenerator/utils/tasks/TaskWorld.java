package me.chunkpregenerator.utils.tasks;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import lombok.Getter;
import lombok.Setter;
import me.chunkpregenerator.Core;
import me.chunkpregenerator.utils.ChunkUtils;
import me.chunkpregenerator.utils.ConfigManager;
import me.chunkpregenerator.utils.Task;

@Getter
@Setter
public class TaskWorld extends Task {

	private static World world;
	private static ConfigManager config = ConfigManager.get();
	private static final Queue<int[]> chunks = new ConcurrentLinkedQueue<>();
	private static int totalToGenerate;
	private static int totalGenerated;
	
	public TaskWorld() {
		super(()-> {
			
			for (int i = 0; i < config.getChunks_per_tick(); i++) {
			    if (world == null || chunks.isEmpty()) return;

			    int[] coordinates = chunks.poll();
			    int x = coordinates[0];
			    int z = coordinates[1];

			    int percent = totalToGenerate == 0 ? 100 : totalGenerated * 100 / totalToGenerate;
			    Bukkit.getConsoleSender().sendMessage("§7Generated §f" + totalGenerated + " §7of §f" + totalToGenerate + " chunks §6["+percent+"%] §3" + world.getName() + " §2[X:"+x + ",Z:"+z+"]");

			    Task.run(() -> {
			        Chunk chunk = world.getChunkAt(x, z);
			        if (!chunk.isLoaded()) {
			            chunk.load(true);
			            chunk.unload(false);
			        }
			    });

			    totalGenerated++;

			    if (chunks.isEmpty()) {
			        Bukkit.getConsoleSender().sendMessage("§aPregeneration complete for world " + world.getName() + "!");
			    }
			}
			
		});
	}
	
	public static void setWorld(World world) {
		Task.run(()-> {
			TaskWorld.world = world;
			chunks.clear();
			
			if (world.getWorldBorder().getSize() > ConfigManager.get().getMax_border_size()) {
				world.getWorldBorder().setSize(ConfigManager.get().getMax_border_size());
			}
			
			Task.runAsync(()-> {
				chunks.addAll(ChunkUtils.streamChunksOfWorld(world).collect(Collectors.toList()));
				
				totalToGenerate = chunks.size()-1;
				totalGenerated = 0;
			});
		});
	}
	
	public static TaskWorld get() {
		return Core.getInstance().getTaskWorld();
	}
	
}
