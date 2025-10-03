package me.chunkpregenerator.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.chunkpregenerator.Core;

@Getter
@AllArgsConstructor
public class Task {

    @Setter private Runnable runnable;

    public void run() {
        if (this.runnable == null) return;
        this.runnable.run();
    }

    @SuppressWarnings("deprecation")
    public static void run(Runnable runnable) {
        if (runnable == null) return;
        
        if (isFolia()) {
            try {
                Bukkit.getGlobalRegionScheduler().execute(Core.getInstance(), runnable);
                return;
            } catch (Exception e) {}
        }
        
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(Core.getInstance(), runnable);
        }
    }

    @SuppressWarnings("deprecation")
    public static void runAsync(Runnable runnable) {
        if (runnable == null) return;
        
        if (isFolia()) {
            try {
                Bukkit.getAsyncScheduler().runNow(Core.getInstance(), task -> runnable.run());
                return;
            } catch (Exception e) {}
        }
        
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), runnable);
        } else {
            runnable.run();
        }
    }

    // NOVO MÉTODO: Para executar operações com chunks no Folia
    public static void runForChunk(World world, int chunkX, int chunkZ, Runnable runnable) {
        if (runnable == null) return;
        
        if (isFolia()) {
            try {
                // No Folia, usa RegionScheduler para a posição específica do chunk
                Bukkit.getRegionScheduler().execute(Core.getInstance(), world, chunkX, chunkZ, runnable);
                return;
            } catch (Exception e) {
                // Fallback para GlobalRegionScheduler
                Bukkit.getGlobalRegionScheduler().execute(Core.getInstance(), runnable);
            }
        } else {
            // Spigot padrão - executa na thread principal
            run(runnable);
        }
    }

    // NOVO MÉTODO: Para executar em localização específica
    public static void runAtLocation(Location location, Runnable runnable) {
        if (runnable == null || location == null) return;
        
        if (isFolia()) {
            try {
                Bukkit.getRegionScheduler().execute(Core.getInstance(), location, runnable);
                return;
            } catch (Exception e) {
                run(runnable);
            }
        } else {
            run(runnable);
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}