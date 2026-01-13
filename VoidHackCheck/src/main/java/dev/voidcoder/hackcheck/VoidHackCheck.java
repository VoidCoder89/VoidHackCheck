package dev.voidcoder.hackcheck;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * VoidHackCheck
 * Sistema di controlli hack manuali per staff
 */
public final class VoidHackCheck extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<UUID> checking = new HashSet<>();
    private final Map<UUID, Location> frozenLocation = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("check").setExecutor(this);
        getCommand("uncheck").setExecutor(this);

        getLogger().info("VoidHackCheck avviato");
    }

    // ========== COMANDI ==========
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("Solo i player possono usare questo comando");
            return true;
        }

        if (!staff.hasPermission("staff.check")) {
            staff.sendMessage("§cNon hai il permesso");
            return true;
        }

        if (label.equalsIgnoreCase("check")) {
            if (args.length != 1) {
                staff.sendMessage("§cUso: /check <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                staff.sendMessage("§cPlayer offline");
                return true;
            }

            startCheck(staff, target);
            return true;
        }

        if (label.equalsIgnoreCase("uncheck")) {
            if (args.length != 1) {
                staff.sendMessage("§cUso: /uncheck <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                staff.sendMessage("§cPlayer offline");
                return true;
            }

            stopCheck(target);
            staff.sendMessage("§aControllo terminato per " + target.getName());
            return true;
        }
        return true;
    }

    // ========== LOGICA ==========
    private void startCheck(Player staff, Player target) {
        if (checking.contains(target.getUniqueId())) {
            staff.sendMessage("§cIl player è già in controllo");
            return;
        }

        checking.add(target.getUniqueId());
        frozenLocation.put(target.getUniqueId(), target.getLocation());

        target.sendMessage("§cSei stato messo in controllo hack. Non disconnetterti.");
        staff.sendMessage("§aHai avviato il controllo hack su " + target.getName());

        target.setGameMode(GameMode.ADVENTURE);
        target.setAllowFlight(false);

        // timer disconnessione
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!checking.contains(target.getUniqueId())) {
                    cancel();
                    return;
                }

                if (!target.isOnline()) {
                    Bukkit.broadcastMessage("§c" + target.getName() + " si è disconnesso durante un controllo hack");
                    cancel();
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void stopCheck(Player target) {
        checking.remove(target.getUniqueId());
        frozenLocation.remove(target.getUniqueId());
        target.sendMessage("§aControllo hack terminato");
    }

    // ========== FREEZE ==========
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (!checking.contains(p.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX() ||
                from.getBlockY() != to.getBlockY() ||
                from.getBlockZ() != to.getBlockZ()) {
            event.setTo(from);
        }
    }
}
