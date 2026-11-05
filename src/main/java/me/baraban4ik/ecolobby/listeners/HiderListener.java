package me.baraban4ik.ecolobby.listeners;

import me.baraban4ik.ecolobby.EcoLobby;
import me.baraban4ik.ecolobby.config.ConfigManager;

import me.baraban4ik.ecolobby.config.files.Config;
import me.baraban4ik.ecolobby.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HiderListener implements Listener {

    private final Config config;

    public HiderListener() {
        config = ConfigManager.getConfig();
    }

    @EventHandler
    public void hideJoin(PlayerJoinEvent event) {
        if (config.isHideStreamAll() || config.isHideStreamJoin())
            event.setJoinMessage(null);

        boolean hidePlayers = config.isHidePlayers();

        for (Player otherPlayer : Bukkit.getServer().getOnlinePlayers()) {
            Player joined = event.getPlayer();

            // hide/show is executed on the thread of the player who "sees" the other one
            if (hidePlayers) {
                Scheduler.entity(otherPlayer, () -> otherPlayer.hidePlayer(EcoLobby.getInstance(), joined));
                Scheduler.entity(joined, () -> joined.hidePlayer(EcoLobby.getInstance(), otherPlayer));
                continue;
            }
            Scheduler.entity(otherPlayer, () -> otherPlayer.showPlayer(EcoLobby.getInstance(), joined));
            Scheduler.entity(joined, () -> joined.showPlayer(EcoLobby.getInstance(), otherPlayer));
        }
    }
    @EventHandler
    public void hideLeave(PlayerQuitEvent event) {
        if (config.isHideStreamAll() || config.isHideStreamLeave())
            event.setQuitMessage(null);
    }
    @EventHandler
    public void hideDeath(PlayerDeathEvent event) {
        if (config.isHideStreamAll() || config.isHideStreamDeath())
            event.setDeathMessage(null);
    }
    @EventHandler
    public void hideKick(PlayerKickEvent event) {
        if (config.isHideStreamAll() || config.isHideStreamKick())
            event.setLeaveMessage("");
    }
}
