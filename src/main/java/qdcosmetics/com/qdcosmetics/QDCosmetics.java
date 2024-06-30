package qdcosmetics.com.qdcosmetics;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public final class QDCosmetics extends JavaPlugin implements @NotNull Listener {

    private HashMap<UUID, ArmorStand> armorStands = new HashMap<>();
    private final String inventoryTitle = "§8[cosmetic] §8코스튬";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public @NotNull ComponentLogger getComponentLogger() {
        return super.getComponentLogger();
    }

    @Override
    public void onDisable() {
        for (ArmorStand stand : armorStands.values()) {
            stand.remove();
        }
        armorStands.clear();
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && command.getName().equalsIgnoreCase("코스튬")) {
            Player player = (Player) sender;
            openCosmeticInventory(player);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack cosmeticItem = getItemFromConfig(player.getUniqueId(), "cosmetic.back");
        if (cosmeticItem != null) {
            spawnArmorStand(player, cosmeticItem);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeArmorStand(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ArmorStand stand = armorStands.remove(player.getUniqueId());
        if (stand != null) {
            stand.getEquipment().setHelmet(null);
            stand.remove();
            List<ItemStack> droppedItems = new ArrayList<>();
            droppedItems.addAll(List.of(stand.getEquipment().getArmorContents()));
            droppedItems.add(stand.getEquipment().getItemInMainHand());
            droppedItems.add(stand.getEquipment().getItemInOffHand());

            for (ItemStack item : droppedItems) {
                if (item != null && item.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
        }
    }
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        ItemStack cosmeticItem = getItemFromConfig(player.getUniqueId(), "cosmetic.back");
        if (cosmeticItem != null) {
            spawnArmorStand(player, cosmeticItem);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(inventoryTitle)) {
            Player player = (Player) event.getPlayer();
            Inventory inventory = event.getInventory();
            ItemStack slot2Item = inventory.getItem(2);

            if (slot2Item == null || slot2Item.getType() == Material.AIR) {
                removeArmorStand(player);
                deleteConfigKey(player.getUniqueId(), "cosmetic.back");
            } else {
                saveItemToConfig(player.getUniqueId(), "cosmetic.back", slot2Item);
                spawnArmorStand(player, slot2Item);
            }
        }
    }

    private void spawnArmorStand(Player player, ItemStack item) {
        removeArmorStand(player);

        Location loc = player.getLocation().add(0, 1.6, 0);

        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setMarker(true);
        stand.setHelmet(item);
        stand.setVisible(false);

        player.addPassenger(stand);

        armorStands.put(player.getUniqueId(), stand);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (stand.isValid() && player.isOnline()) {
                    // 지속적으로 아머 스탠드의 위치를 업데이트
                    Location playerLocation = player.getLocation().add(0, 1.5, 0);
                    stand.setRotation(playerLocation.getYaw(), 0);
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0L, 2L);
    }

    private void removeArmorStand(Player player) {
        ArmorStand stand = armorStands.remove(player.getUniqueId());
        if (stand != null) {
            stand.remove();
        }
    }

    private void saveItemToConfig(UUID playerUUID, String key, ItemStack item) {
        File playerFile = getPlayerFile(playerUUID);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set(key, item);
        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ItemStack getItemFromConfig(UUID playerUUID, String key) {
        File playerFile = getPlayerFile(playerUUID);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return config.getItemStack(key);
    }

    private void deleteConfigKey(UUID playerUUID, String key) {
        File playerFile = getPlayerFile(playerUUID);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        config.set(key, null);
        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getPlayerFile(UUID playerUUID) {
        return new File(getDataFolder(), "Players" + File.separator + playerUUID.toString() + ".yml");
    }

    public void openCosmeticInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.HOPPER, inventoryTitle);
        ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = blackPane.getItemMeta();
        meta.setDisplayName("§f");
        blackPane.setItemMeta(meta);

        inv.setItem(0, blackPane);
        inv.setItem(1, blackPane);
        inv.setItem(3, blackPane);
        inv.setItem(4, blackPane);

        ItemStack slot2Item = getItemFromConfig(player.getUniqueId(), "cosmetic.back");
        if (slot2Item != null) {
            inv.setItem(2, slot2Item);
        }

        player.openInventory(inv);
    }
}
