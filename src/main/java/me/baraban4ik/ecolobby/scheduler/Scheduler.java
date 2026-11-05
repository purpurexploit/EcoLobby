package me.baraban4ik.ecolobby.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.baraban4ik.ecolobby.EcoLobby;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Thin wrapper over the Paper/Folia schedulers.
 * Works on Folia and on regular Paper 1.20+ (the same API is implemented there).
 */
public final class Scheduler {

    private static final boolean FOLIA = detectFolia();

    private Scheduler() {}

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    private static Plugin plugin() {
        return EcoLobby.getInstance();
    }

    private static boolean canSchedule() {
        return plugin().isEnabled();
    }

    /** Runs on the global region (console commands, world rules, time...). */
    public static void global(Runnable task) {
        if (!canSchedule()) return;
        Bukkit.getGlobalRegionScheduler().execute(plugin(), task);
    }

    /** Runs off the server threads (IO, HTTP...). */
    public static void async(Runnable task) {
        if (!canSchedule()) return;
        Bukkit.getAsyncScheduler().runNow(plugin(), t -> task.run());
    }

    /** Runs on the thread that owns the entity: immediately if we are already there, otherwise ASAP. */
    public static void entity(Entity entity, Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return;
        }
        if (!canSchedule()) return;
        entity.getScheduler().run(plugin(), t -> task.run(), null);
    }

    public static ScheduledTask entityLater(Entity entity, Runnable task, long delayTicks) {
        if (!canSchedule()) return null;
        return entity.getScheduler().runDelayed(plugin(), t -> task.run(), null, Math.max(1L, delayTicks));
    }

    /**
     * Repeating task bound to the entity. It follows the entity between regions
     * and is dropped automatically (with {@code retired} called) when the entity is removed.
     */
    public static ScheduledTask entityTimer(Entity entity, Consumer<ScheduledTask> task, Runnable retired,
                                            long delayTicks, long periodTicks) {
        if (!canSchedule()) return null;
        return entity.getScheduler().runAtFixedRate(plugin(), task, retired,
                Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    }

    /** Teleport that is safe on Folia (cross-region). */
    public static void teleport(Player player, Location location) {
        player.teleportAsync(location);
    }
}
