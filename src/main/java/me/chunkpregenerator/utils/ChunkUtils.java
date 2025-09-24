package me.chunkpregenerator.utils;

import org.bukkit.World;

import org.bukkit.WorldBorder;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ChunkUtils {

	public static Stream<int[]> streamChunksOfWorld(World world) {
	    WorldBorder border = world.getWorldBorder();
	    double centerX = border.getCenter().getX();
	    double centerZ = border.getCenter().getZ();
	    double radius = border.getSize() / 2.0;

	    double minX = centerX - radius;
	    double maxX = centerX + radius;
	    double minZ = centerZ - radius;
	    double maxZ = centerZ + radius;

	    int minChunkX = (int) Math.floor(minX / 16.0);
	    int maxChunkX = (int) Math.floor(maxX / 16.0);
	    int minChunkZ = (int) Math.floor(minZ / 16.0);
	    int maxChunkZ = (int) Math.floor(maxZ / 16.0);

	    return IntStream.rangeClosed(minChunkX, maxChunkX)
	        .boxed()
	        .flatMap(chunkX -> IntStream.rangeClosed(minChunkZ, maxChunkZ)
	            .mapToObj(chunkZ -> new int[]{chunkX, chunkZ}));
	}
	
}