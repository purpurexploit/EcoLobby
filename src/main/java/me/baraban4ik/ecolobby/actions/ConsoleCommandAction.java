package me.baraban4ik.ecolobby.actions;


import me.baraban4ik.ecolobby.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleCommandAction implements PlayerAction {
    @Override
    public void execute(Player player, String action) {
        // console commands must run on the global region thread (Folia)
        Scheduler.global(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action));
    }
}
