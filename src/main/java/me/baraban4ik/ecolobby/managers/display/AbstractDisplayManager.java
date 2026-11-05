package me.baraban4ik.ecolobby.managers.display;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.baraban4ik.ecolobby.managers.PlayerAware;
import me.baraban4ik.ecolobby.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDisplayManager<T> implements PlayerAware {

    protected final Map<UUID, T> playerDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> updateTasks = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    private volatile boolean running;

    public abstract boolean isEnabled();

    protected abstract long getRefreshTicks();
    protected abstract void createDisplay(Player player);

    protected void removeDisplay(T display) {}
    protected void removeDisplay(Player player) {}

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
        removeAllPlayers();
    }

    /** Called from onDisable: schedulers are no longer available, so clean up directly. */
    public void shutdown() {
        running = false;

        for (UUID uuid : new ArrayList<>(playerDisplays.keySet())) {
            ScheduledTask task = updateTasks.remove(uuid);
            if (task != null) task.cancel();

            T display = playerDisplays.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            try {
                if (display != null) removeDisplay(display);
                if (player != null) removeDisplay(player);
            } catch (Exception ignored) {}
        }
        pending.clear();
    }

    /** Thread-safe: the display is created on the thread that owns the player. */
    public void send(Player player) {
        if (!running) return;

        UUID uuid = player.getUniqueId();
        if (playerDisplays.containsKey(uuid) || !pending.add(uuid)) return;

        Scheduler.entity(player, () -> {
            try {
                if (!running || !player.isOnline() || playerDisplays.containsKey(uuid)) return;

                createDisplay(player);
                update(player);
                startUpdateTask(player);
            } finally {
                pending.remove(uuid);
            }
        });
    }

    private void startUpdateTask(Player player) {
        UUID uuid = player.getUniqueId();
        long period = getRefreshTicks();

        ScheduledTask task = Scheduler.entityTimer(player, t -> {
            if (!running || !player.isOnline()) {
                t.cancel();
                return;
            }
            update(player);
        }, () -> release(uuid), period, period);

        if (task != null) updateTasks.put(uuid, task);
    }

    public void removeAllPlayers() {
        for (UUID uuid : new ArrayList<>(playerDisplays.keySet())) {
            ScheduledTask task = updateTasks.remove(uuid);
            if (task != null) task.cancel();

            T display = playerDisplays.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            Scheduler.entity(player, () -> {
                if (display != null) removeDisplay(display);
                removeDisplay(player);
            });
        }
    }

    protected void addPlayer(Player player, T value) {
        playerDisplays.put(player.getUniqueId(), value);
    }

    @Override
    public void removePlayer(Player player) {
        release(player.getUniqueId());
    }

    private void release(UUID uuid) {
        ScheduledTask task = updateTasks.remove(uuid);
        if (task != null) task.cancel();
        playerDisplays.remove(uuid);
    }
}
