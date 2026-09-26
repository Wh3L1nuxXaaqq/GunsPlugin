package invoke.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GunReloadListener implements Listener {

    JavaPlugin plugin;

    public GunReloadListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUnload(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event;
        if (!player.isSneaking() || !event.isRightClick()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String gunId = meta.getPersistentDataContainer().get(Gun.gunTypeKey, PersistentDataType.STRING);
        if (gunId == null) return;

        Gun gun = Gun.guns.get(gunId);
        if (gun == null) return;

        Integer ammo = meta.getPersistentDataContainer().get(Gun.currentAmmoKey, PersistentDataType.INTEGER);
        if (ammo == null || ammo <= 0) {
            player.sendMessage(ChatColor.GRAY + gun.razryadErr);
            return;
        }

        event.setCancelled(true);
        player.sendMessage(ChatColor.YELLOW + gun.razryad);
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                meta.getPersistentDataContainer().set(Gun.currentAmmoKey, PersistentDataType.INTEGER, 0);
                List<String> lore = meta.getLore();
                if (lore != null && !lore.isEmpty()) {
                    String loreTemplate = plugin.getConfig().getString("gun-lore", "&7Патроны: {ammo}/{gunAmmoCount}");

                    PersistentDataContainer pdcItem = meta.getPersistentDataContainer();
                    int totalBullets = Math.max(1, gun.bulletsPerShot);
                    Integer ammo = pdcItem.get(Gun.currentAmmoKey, PersistentDataType.INTEGER);

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("ammo", String.valueOf(ammo - totalBullets));
                    placeholders.put("gunAmmoCount", String.valueOf(gun.gunAmmoCount));

                    String formatted = TextUtil.format(loreTemplate, placeholders);
                    lore.set(0, ChatColor.GRAY + formatted);
                    meta.setLore(lore);
                }
                clicked.setItemMeta(meta);

                player.getInventory().addItem(new ItemStack(gun.ammoItem, ammo));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                player.sendMessage(ChatColor.GREEN + gun.razrfinish);
            }
        }.runTaskLater(Gun.getInstance(), 200 / 50L);
    }
}
