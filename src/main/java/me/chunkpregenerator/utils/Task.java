package me.chunkpregenerator.utils;

import org.bukkit.Bukkit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.chunkpregenerator.Core;

import java.lang.reflect.Method;

@Getter
@AllArgsConstructor
public class Task {

    private final Runnable runnable;

    public void run() {
        if (this.runnable == null) return;
        this.runnable.run();
    }

    @SuppressWarnings("deprecation")
    public static void run(Runnable runnable) {
        try {
            Method asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
            if (asyncScheduler != null) {
                Bukkit.getGlobalRegionScheduler().execute(Core.getInstance(), runnable);
                return;
            }
        } catch (NoSuchMethodException e) {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(Core.getInstance(), runnable);
                return;
            }
            runnable.run();
        }
    }

    @SuppressWarnings("deprecation")
    public static void runAsync(Runnable runnable) {
        try {
            Method asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
            if (asyncScheduler != null) {
                Bukkit.getAsyncScheduler().runNow(Core.getInstance(), task -> runnable.run());
                return;
            }
        } catch (NoSuchMethodException e) {
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(Core.getInstance(), runnable);
                return;
            }
            runnable.run();
        }
    }
}
