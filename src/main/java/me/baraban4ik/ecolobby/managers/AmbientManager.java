package me.baraban4ik.ecolobby.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.baraban4ik.ecolobby.config.ConfigManager;
import me.baraban4ik.ecolobby.config.files.modules.WorldsConfig;
import me.baraban4ik.ecolobby.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Falling particles around players. Every player gets his own entity-bound task,
 * so it always runs on the thread that owns the player (required by Folia).
 * The world settings are read on every tick, so the task follows the player between worlds.
 */
public class AmbientManager implements PlayerAware {

    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private volatile boolean running;

    @Override
    public void start() {
        running = true;
        Bukkit.getOnlinePlayers().forEach(this::addPlayer);
    }

    @Override
    public void stop() {
        running = false;
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    public void addPlayer(Player player) {
        if (!running) return;

        UUID uuid = player.getUniqueId();
        tasks.computeIfAbsent(uuid, id -> Scheduler.entityTimer(
                player,
                task -> tick(player, task),
                () -> tasks.remove(id),
                1L, 1L
        ));
    }

    @Override
    public void removePlayer(Player player) {
        ScheduledTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    @Override
    public void update(Player player) {
        addPlayer(player);
    }

    private void tick(Player player, ScheduledTask task) {
        if (!player.isOnline()) {
            task.cancel();
            tasks.remove(player.getUniqueId());
            return;
        }

        World world = player.getWorld();
        WorldsConfig worldsConfig = ConfigManager.getWorldsConfig();
        if (!worldsConfig.isAmbientEnabled(world)) return;

        int amount = worldsConfig.getAmbientAmount(world);
        double radius = worldsConfig.getAmbientRadius(world);
        Particle particle = worldsConfig.getAmbientParticle(world);

        for (int i = 0; i < amount; i++) {
            spawnFallParticles(player, radius, particle);
        }
    }

    private void spawnFallParticles(Player player, double radius, Particle particle) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location location = player.getLocation();

        double x = (random.nextDouble() * 2 - 1) * radius;
        double y = (random.nextDouble() * 2 - 1) * radius;
        double z = (random.nextDouble() * 2 - 1) * radius;

        player.spawnParticle(particle, location.add(x, y, z), 0, 25.0F, 13.0F, 8.0F, 0.0F);
    }
}
