package me.chunkpregenerator.utils.tasks;

import java.util.Queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private World world;
    private long lastExecuted;
    private final ConfigManager config = ConfigManager.get();
    private final Queue<int[]> chunks = new ConcurrentLinkedQueue<>();
    private int totalToGenerate;
    private int totalGenerated;
    private boolean canExecute = true;

    public TaskWorld() {
        super(()-> {});
        setRunnable(() -> {
        	
        	long currentTime = System.currentTimeMillis();
        	if (currentTime-lastExecuted < config.getTimePerChunk()) return;
        	lastExecuted = currentTime;
        	
            if (world == null || chunks.isEmpty() || !canExecute) {
                return;
            }
            
            canExecute = false;
            Task.run(()-> {
            	for (int i = 0; i < config.getChunks_per_tick(); i++) {
                    if (chunks.isEmpty()) break;

                    int[] coordinates = chunks.poll();
                    if (coordinates == null) continue;

                    int x = coordinates[0];
                    int z = coordinates[1];

                    int percent = totalToGenerate == 0 ? 0 : (totalGenerated * 100) / totalToGenerate;
                    Bukkit.getConsoleSender().sendMessage(
                        String.format("§7Generated §f%d §7of §f%d chunks §6[%d%%] §3%s §2[X:%d, Z:%d]",
                            totalGenerated, totalToGenerate, percent, world.getName(), x, z)
                    );
                    
                    Task.runForChunk(world, x, z, ()-> {
                    	try {
                        	Chunk chunk = world.getChunkAt(x, z);
                        	if (!chunk.isLoaded()) {
                        		chunk.load(true);
                        		chunk.unload(false);
                        	}
                        } catch (Exception e) {
                        	Bukkit.getConsoleSender().sendMessage(
                        			"§cError generating chunk at [X:" + x + ", Z:" + z + "]: " + e.getMessage()
                        	);
                        }
                    });

                    totalGenerated++;

                    if (chunks.isEmpty()) {
                        Bukkit.getConsoleSender().sendMessage(
                            "§aPregeneration completed for world " + world.getName() + "! " +
                            "Total: " + totalGenerated + " chunks"
                        );
                        reset();
                        break;
                    }
                }
            	canExecute = true;
            });
            
        });
    }

    public void initializeWorld(World world) {
        reset();
        this.world = world;

        Task.run(() -> {
            if (world == null) {
                Bukkit.getConsoleSender().sendMessage("§cInvalid world for pregeneration!");
                return;
            }

            double borderSize = world.getWorldBorder().getSize();
            if (borderSize > config.getMax_border_size()) {
                world.getWorldBorder().setSize(config.getMax_border_size());
                Bukkit.getConsoleSender().sendMessage(
                    "§eWorld border adjusted to " + config.getMax_border_size() + " blocks."
                );
            }

            Task.runAsync(() -> {
                Stream<int[]> chunkStream = ChunkUtils.streamChunksOfWorld(world);
                this.chunks.addAll(chunkStream.collect(Collectors.toList()));
                
                totalToGenerate = this.chunks.size()-1;
                
                Bukkit.getConsoleSender().sendMessage(
                        "§aTotal chunks to generate: " + totalToGenerate
                );
            });
        });
    }

    private void reset() {
        this.totalToGenerate = 0;
        this.totalGenerated = 0;
    }

    public static TaskWorld get() {
        return Core.getInstance().getTaskWorld();
    }
}
