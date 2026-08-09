package com.raencel.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class RaencelAntiCheat extends JavaPlugin implements Listener {

    // Konum ve ihlal verilerini tutan haritalar
    private final HashMap<UUID, Location> lastLocations = new HashMap<>();
    private final HashMap<UUID, Integer> speedViolations = new HashMap<>();
    private final HashMap<UUID, Integer> flyViolations = new HashMap<>();
    
    // Combat (Killaura) kontrol sayaçları
    private final HashMap<UUID, Long> lastAttackTime = new HashMap<>();
    private final HashMap<UUID, Integer> combatViolations = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("[+] RAENCEL Anti-Cheat Sistemi Aktif Edildi!");
    }

    @Override
    public void onDisable() {
        lastLocations.clear();
        speedViolations.clear();
        flyViolations.clear();
        lastAttackTime.clear();
        combatViolations.clear();
        getLogger().info("[-] RAENCEL Anti-Cheat Kapatildi.");
    }

    // =========================================================================
    // MOTOR 1: HAREKET ANALİZİ (FLY & SPEEDHACK)
    // =========================================================================
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.getGameMode() == GameMode.CREATIVE || 
            player.getGameMode() == GameMode.SPECTATOR || 
            player.isGliding() || 
            player.getVehicle() != null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) {
            return;
        }

        if (!lastLocations.containsKey(uuid)) {
            lastLocations.put(uuid, from);
            return;
        }

        // --- SPEEDHACK KONTROLÜ ---
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));

        double maxSpeedLimit = 0.68;
        if (player.isSprinting()) {
            maxSpeedLimit = 0.75;
        }

        if (horizontalDistance > maxSpeedLimit) {
            int violations = speedViolations.getOrDefault(uuid, 0) + 1;
            speedViolations.put(uuid, violations);

            if (violations >= 4) {
                flagPlayer(player, "SpeedHack", horizontalDistance);
                event.setTo(from); // Geri çek (Rubberband)
                return;
            }
        } else {
            if (speedViolations.getOrDefault(uuid, 0) > 0) {
                speedViolations.put(uuid, speedViolations.get(uuid) - 1);
            }
        }

        // --- FLY / HOVER KONTROLÜ ---
        double deltaY = to.getY() - from.getY();
        
        if (!player.isOnGround() && to.getBlock().getRelative(0, -1, 0).getType() == Material.AIR) {
            if (deltaY >= 0.0 && !player.isFlying()) {
                int violations = flyViolations.getOrDefault(uuid, 0) + 1;
                flyViolations.put(uuid, violations);

                if (violations >= 5) {
                    flagPlayer(player, "Fly/Hover", deltaY);
                    event.setTo(from); // Yere çak
                    return;
                }
            }
        } else {
            flyViolations.put(uuid, 0);
        }

        lastLocations.put(uuid, to);
    }

    // =========================================================================
    // MOTOR 2: SAVAŞ ANALİZİ (KILLAURA)
    // =========================================================================
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Entity target = event.getEntity();
        UUID uuid = attacker.getUniqueId();

        if (attacker.getGameMode() == GameMode.CREATIVE) return;

        long currentTime = System.currentTimeMillis();
        long lastAttack = lastAttackTime.getOrDefault(uuid, 0L);
        lastAttackTime.put(uuid, currentTime);

        // --- 1. AÇI KONTROLÜ (Arkası Dönük Vurma Engeli) ---
        Vector attackerLook = attacker.getLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        double dotProduct = attackerLook.dot(toTarget);

        if (dotProduct < 0.45) { // 120 derecelik vuruş açısı limiti
            int violations = combatViolations.getOrDefault(uuid, 0) + 2;
            combatViolations.put(uuid, violations);
            
            if (violations >= 4) {
                flagPlayer(attacker, "KillAura (Aci Ihlali)", dotProduct);
                event.setCancelled(true); // Hasarı iptal et
                return;
            }
        }

        // --- 2. CPS / VURUŞ HIZI KONTROLÜ ---
        long timeDifference = currentTime - lastAttack;
        if (timeDifference < 45) { // Anormal tıklama hızı tespiti
            int violations = combatViolations.getOrDefault(uuid, 0) + 1;
            combatViolations.put(uuid, violations);

            if (violations >= 5) {
                flagPlayer(attacker, "KillAura (Yuksek CPS)", (double) timeDifference);
                event.setCancelled(true);
                return;
            }
        } else {
            if (combatViolations.getOrDefault(uuid, 0) > 0) {
                combatViolations.put(uuid, combatViolations.get(uuid) - 1);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastLocations.remove(uuid);
        speedViolations.remove(uuid);
        flyViolations.remove(uuid);
        lastAttackTime.remove(uuid);
        combatViolations.remove(uuid);
    }

    // =========================================================================
    // İHLAL VE CEZA SİSTEMİ
    // =========================================================================
    private void flagPlayer(Player player, String hackType, double value) {
        String alertMessage = String.format("§4[RAENCEL-AC] §c%s §7isimli oyuncu hile suphelisi: §e%s §7(Deger: %.2f)", 
                player.getName(), hackType, value);
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOp()) {
                online.sendMessage(alertMessage);
            }
        }

        int totalViolations = speedViolations.getOrDefault(player.getUniqueId(), 0) + 
                             flyViolations.getOrDefault(player.getUniqueId(), 0) + 
                             combatViolations.getOrDefault(player.getUniqueId(), 0);

        if (totalViolations >= 10) {
            getServer().getScheduler().runTask(this, () -> {
                player.kickPlayer("§4§lRAENCEL ANTI-CHEAT\n\n§cAnormal paket akisi nedeniyle sunucudan uzaklastirildiniz.\n§7Modul: " + hackType);
            });
        }
    }
}
