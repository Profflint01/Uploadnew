package de.profflint.versionaq;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Versionaq extends JavaPlugin implements Listener {

    private final HashMap<UUID, Zombie> playerMinions = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("MinionPlugin enabled!");
        getCommand("minion").setExecutor(new MinionCommand());
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        playerMinions.values().forEach(minion -> {
            if (!minion.isDead()) {
                minion.remove();
            }
        });
        getLogger().info("MinionPlugin disabled!");
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Zombie && playerMinions.containsValue(event.getEntity())) {
            if (event.getTarget() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!playerMinions.containsKey(playerUUID)) {
            return;
        }

        Zombie minion = playerMinions.get(playerUUID);
        if (minion.isDead()) {
            return;
        }

        if (event.getItemDrop().getItemStack().getType() == Material.BREAD) {
            double newHealth = Math.min(minion.getHealth() + 10, minion.getMaxHealth());
            minion.setHealth(newHealth);
            event.getItemDrop().remove();
            player.sendMessage("Your minion has been healed!");
        }
    }

    public class MinionCommand implements org.bukkit.command.TabExecutor {

        @Override
        public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;
            UUID playerUUID = player.getUniqueId();

            if (args.length < 1) {
                player.sendMessage("Usage: /minion <spawn|status|remove|attackmobs>");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "spawn":
                    if (playerMinions.containsKey(playerUUID)) {
                        player.sendMessage("You already have a minion.");
                        return true;
                    }

                    Zombie minion = (Zombie) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
                    minion.setCustomName(player.getName() + "'s Minion");
                    minion.setCustomNameVisible(true);
                    minion.setBaby(false);
                    minion.setAI(true);
                    minion.setPersistent(true);
                    minion.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                    minion.getEquipment().setHelmetDropChance(0);
                    minion.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                    minion.getEquipment().setItemInMainHandDropChance(0);
                    minion.setMaxHealth(200);
                    minion.setHealth(200);

                    playerMinions.put(playerUUID, minion);
                    player.sendMessage("Minion spawned!");

                    //Follow Player and atack mobs arround
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (minion.isDead() || !playerMinions.containsKey(playerUUID)) {
                                cancel();
                                return;
                            }

                            // Calculate distance to player
                            Location playerLocation = player.getLocation();
                            double distance = minion.getLocation().distance(playerLocation);

                            // Follow the player if 6 block distance
                            if (distance > 6) {
                                minion.teleport(playerLocation);
                            }

                            // Attack nearby mobs
                            List<LivingEntity> nearbyEntities = minion.getNearbyEntities(10, 10, 10).stream()
                                    .filter(entity -> entity instanceof LivingEntity && !(entity instanceof Player) && entity != minion)
                                    .map(entity -> (LivingEntity) entity)
                                    .toList();

                            if (!nearbyEntities.isEmpty()) {
                                minion.setTarget(nearbyEntities.get(0));
                            } else {
                                minion.setTarget(null);
                            }
                        }
                    }.runTaskTimer(Versionaq.this, 0, 20);

                    break;

                case "status":
                    if (!playerMinions.containsKey(playerUUID)) {
                        player.sendMessage("You don't have a minion.");
                        return true;
                    }

                    minion = playerMinions.get(playerUUID);
                    if (minion.isDead()) {
                        player.sendMessage("Your minion is dead.");
                    } else {
                        player.sendMessage("Your minion has " + minion.getHealth() + " health left.");
                    }
                    break;

                case "remove":
                    if (!playerMinions.containsKey(playerUUID)) {
                        player.sendMessage("You don't have a minion.");
                        return true;
                    }

                    minion = playerMinions.get(playerUUID);
                    if (!minion.isDead()) {
                        minion.remove();
                    }
                    playerMinions.remove(playerUUID);
                    player.sendMessage("Your minion has been removed.");
                    break;

                case "attackmobs":
                    player.sendMessage("The minion will now attack mobs around you.");
                    break;

                default:
                    player.sendMessage("Unknown command. Usage: /minion <spawn|status|remove|attackmobs>");
            }

            return true;
        }

        @Override
        public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
            if (args.length == 1) {
                return java.util.Arrays.asList("spawn", "status", "remove", "attackmobs");
            }
            return java.util.Collections.emptyList();
        }
    }
}

