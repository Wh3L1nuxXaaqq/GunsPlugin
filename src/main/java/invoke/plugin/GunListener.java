package invoke.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;


public class GunListener implements Listener {

    public JavaPlugin plugin;

    public GunListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private int removeItems(Inventory inv, Material mat, int amount) {
        int removed = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == mat) {
                int amt = item.getAmount();
                if (amt > amount - removed) {
                    item.setAmount(amt - (amount - removed));
                    removed = amount;
                    break;
                } else {
                    removed += amt;
                    item.setAmount(0);
                    if (removed >= amount) break;
                }
            }
        }
        return removed;
    }

    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) {
            removeWeightEffect(player);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(Gun.gunTypeKey, PersistentDataType.STRING)) {
            removeWeightEffect(player);
            return;
        }

        String gunId = pdc.get(Gun.gunTypeKey, PersistentDataType.STRING);
        Gun gun = Gun.guns.get(gunId);
        if (gun == null || gun.weight <= 0) {
            removeWeightEffect(player);
            return;
        }

        double slowPercent = gun.weight;
        int amplifier = (int) Math.max(0, (slowPercent / 10.0) - 1); // 10% = lvl 0
        org.bukkit.potion.PotionEffectType slow = org.bukkit.potion.PotionEffectType.SLOW;
        org.bukkit.potion.PotionEffect current = player.getPotionEffect(slow);

        if (current == null || current.getAmplifier() != amplifier) {
            player.removePotionEffect(slow);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    slow, 60, amplifier, false, false, false
            ));
        }
    }

    private void removeWeightEffect(Player player) {
        org.bukkit.potion.PotionEffectType slow = org.bukkit.potion.PotionEffectType.SLOW;
        if (player.hasPotionEffect(slow)) {
            player.removePotionEffect(slow);
        }
    }

    private final Random random = new Random();

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdcItem = meta.getPersistentDataContainer();
        if (!pdcItem.has(Gun.gunTypeKey, PersistentDataType.STRING)) return;

        String gunId = pdcItem.get(Gun.gunTypeKey, PersistentDataType.STRING);
        Gun gun = Gun.guns.get(gunId);
        if (gun == null) return;

        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {//(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
            if (!gun.scope) {
                return;
            }
            PersistentDataContainer pdcPlayer = player.getPersistentDataContainer();
            boolean isAiming = pdcPlayer.has(Gun.aimingKey, PersistentDataType.BYTE) &&
                    pdcPlayer.get(Gun.aimingKey, PersistentDataType.BYTE) == (byte)1;
            if (!isAiming) {
                pdcPlayer.set(Gun.aimingKey, PersistentDataType.BYTE, (byte)1);
                String effectName = plugin.getConfig().getString("scope-effect", "SLOW");
                int power = plugin.getConfig().getInt("scope-effect-multiple", 20);
                try {
                    org.bukkit.potion.PotionEffectType pet = org.bukkit.potion.PotionEffectType.getByName(effectName.toUpperCase());
                    if (pet != null) {
                        plugin.getLogger().info("Scope effect: " + effectName + " => " + pet);
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(pet, 999999, Math.max(0, power - 1), false, false));
                    }
                } catch (Exception ignored) {}
                //player.sendMessage(ChatColor.GRAY + "1");
            } else {
                pdcPlayer.set(Gun.aimingKey, PersistentDataType.BYTE, (byte)0);
                String effectName = plugin.getConfig().getString("scope-effect", "SLOW");
                try {
                    org.bukkit.potion.PotionEffectType pet = org.bukkit.potion.PotionEffectType.getByName(effectName.toUpperCase());
                    if (pet != null) player.removePotionEffect(pet);
                } catch (Exception ignored) {}
                //player.sendMessage(ChatColor.GRAY + "0");
            }
            event.setCancelled(true);
            return;
        }
//if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Material mat = item.getType();
        if (player.hasCooldown(mat)) return;

        Integer ammo = pdcItem.get(Gun.currentAmmoKey, PersistentDataType.INTEGER);
        if (ammo == null) ammo = 0;
        if (ammo <= 0) {
            player.sendMessage(ChatColor.RED + gun.shootnoammo);
            return;
        }

        int totalBullets = Math.max(1, gun.bulletsPerShot);
        if (ammo < totalBullets) {
            player.sendMessage(ChatColor.RED + gun.needammo);
            return;
        }

        RecoilManager.applyRecoil(player, gun);
        int cooldownTicks = (int) Math.ceil(gun.cooldown / 50.0);
        player.setCooldown(mat, cooldownTicks);
        player.playSound(player.getLocation(), gun.shootSound, 1.0f, 1.0f);
        pdcItem.set(Gun.currentAmmoKey, PersistentDataType.INTEGER, ammo - totalBullets);
        item.setItemMeta(meta);

        ItemMeta updated = item.getItemMeta();
        if (updated != null) {
            List<String> lore = new ArrayList<>();

            String loreTemplate = plugin.getConfig().getString("gun-lore", "&7Патроны: {ammo}/{gunAmmoCount}");

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("ammo", String.valueOf(ammo - totalBullets));
            placeholders.put("gunAmmoCount", String.valueOf(gun.gunAmmoCount));

            String formatted = TextUtil.format(loreTemplate, placeholders);

            lore.add(formatted);
            updated.setLore(lore);
            item.setItemMeta(updated);

        }

        for (int i = 0; i < totalBullets; i++) {
            Vector dir = player.getEyeLocation().getDirection().clone();
            float spread = gun.spread;
            double rx = (random.nextDouble() - 0.5) * spread;
            double ry = (random.nextDouble() - 0.5) * spread;
            double rz = (random.nextDouble() - 0.5) * spread;
            dir.add(new Vector(rx, ry, rz));
            dir.normalize();

            Projectile proj = player.launchProjectile(org.bukkit.entity.Snowball.class, dir.multiply(2.0));
            proj.getPersistentDataContainer().set(Gun.gunTypeKey, PersistentDataType.STRING, gun.id);

            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (proj.isDead() || !proj.isValid() || ticks++ > 200) {
                        cancel();
                        return;
                    }
                    if (proj.getLocation().distance(player.getLocation()) > gun.range) {
                        proj.remove();
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) return;
        Projectile proj = (Projectile) event.getDamager();

        if (!(proj.getShooter() instanceof Player)) return;
        Player shooter = (Player) proj.getShooter();

        PersistentDataContainer pdc = proj.getPersistentDataContainer();
        if (!pdc.has(Gun.gunTypeKey, PersistentDataType.STRING)) return;

        String gunId = pdc.get(Gun.gunTypeKey, PersistentDataType.STRING);
        Gun gun = Gun.guns.get(gunId);
        if (gun == null) return;

        if (!(event.getEntity() instanceof Player)) return;
        Player target = (Player) event.getEntity();

        double hitY = proj.getLocation().getY();
        double feetY = target.getLocation().getY();
        double relativeY = hitY - feetY;

        double damage = gun.damageBody;
        String hitPart = "body";

        if (relativeY < 0.6) {
            damage = gun.damageLeggs;
            hitPart = "leggs";
        } else if (relativeY > 1.4) {
            damage = gun.damageHead;
            hitPart = "head";
        }

        event.setDamage(damage);
        shooter.sendMessage(ChatColor.GRAY + hitPart);
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(Gun.gunTypeKey, PersistentDataType.STRING)) {
            event.getEntity().remove();
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(Gun.gunTypeKey, PersistentDataType.STRING)) return;
        String gunId = pdc.get(Gun.gunTypeKey, PersistentDataType.STRING);
        Gun gun = Gun.guns.get(gunId);
        if (gun == null) return;

        event.setCancelled(true);
        Integer currentAmmo = pdc.get(Gun.currentAmmoKey, PersistentDataType.INTEGER);
        if (currentAmmo == null) currentAmmo = 0;

        int missing = gun.gunAmmoCount - currentAmmo;
        if (missing <= 0) {
            player.sendMessage(ChatColor.GRAY + gun.fullammo);
            return;
        }

        Inventory inv = player.getInventory();
        int removed = removeItems(inv, gun.ammoItem, missing);
        if (removed <= 0) {
            player.sendMessage(ChatColor.RED + gun.notammo);
            return;
        }

        player.sendMessage(ChatColor.YELLOW + gun.reloading);
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
        player.setCooldown(item.getType(), (int) (gun.reloadTime / 50L));

        int finalCurrentAmmo = currentAmmo;
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || !handItem.hasItemMeta()) return;

                ItemMeta meta = handItem.getItemMeta();
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (!pdc.has(Gun.gunTypeKey, PersistentDataType.STRING)) return;

                String gunId = pdc.get(Gun.gunTypeKey, PersistentDataType.STRING);
                Gun gun = Gun.guns.get(gunId);
                if (gun == null) return;

                int newAmmo = gun.gunAmmoCount;
                pdc.set(Gun.currentAmmoKey, PersistentDataType.INTEGER, newAmmo);

                java.util.List<String> lore = new java.util.ArrayList<>();


                String loreTemplate = plugin.getConfig().getString("gun-lore", "&7Патроны: {ammo}/{gunAmmoCount}");

                PersistentDataContainer pdcItem = meta.getPersistentDataContainer();
                int totalBullets = Math.max(1, gun.bulletsPerShot);
                Integer ammo = pdcItem.get(Gun.currentAmmoKey, PersistentDataType.INTEGER);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("ammo", String.valueOf(ammo - totalBullets));
                placeholders.put("gunAmmoCount", String.valueOf(gun.gunAmmoCount));

                String formatted = TextUtil.format(loreTemplate, placeholders);

                lore.add(ChatColor.GRAY + formatted);
                meta.setLore(lore);

                handItem.setItemMeta(meta);
                player.updateInventory();

                player.playSound(player.getLocation(), gun.reloadSound, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GREEN + gun.reloadFinish);
            }
        }.runTaskLater(plugin, gun.reloadTime / 50L);
    }
}
