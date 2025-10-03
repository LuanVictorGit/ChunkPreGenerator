package me.chunkpregenerator.utils;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bukkit.World;
import org.bukkit.WorldBorder;

public class ChunkUtils {

    public static Stream<int[]> streamChunksOfWorld(World world) {
        WorldBorder border = world.getWorldBorder();
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();
        double size = border.getSize();
        
        // Calcular limites em chunks
        int minChunkX = (int) Math.floor((centerX - size/2) / 16.0);
        int maxChunkX = (int) Math.floor((centerX + size/2) / 16.0);
        int minChunkZ = (int) Math.floor((centerZ - size/2) / 16.0);
        int maxChunkZ = (int) Math.floor((centerZ + size/2) / 16.0);

        // Criar stream sequencial para melhor performance
        return IntStream.rangeClosed(minChunkX, maxChunkX)
                .boxed()
                .flatMap(x -> IntStream.rangeClosed(minChunkZ, maxChunkZ)
                        .mapToObj(z -> new int[]{x, z}));
    }
}