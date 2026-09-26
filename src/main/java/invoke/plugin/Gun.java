package invoke.plugin;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Gun {
    public static Map<String, Gun> guns = new HashMap<>();
    public static NamespacedKey gunTypeKey;
    public static NamespacedKey currentAmmoKey;
    public static NamespacedKey aimingKey;

    private static JavaPlugin pluginInstance;

    public String id;
    public String displayName;
    public double damageBody;
    public int range;
    public long cooldown;
    public long reloadTime;
    public Material ammoItem;
    public int gunAmmoCount;
    public Material material;
    public Sound shootSound;
    public Sound reloadSound;
    public int bulletsPerShot;

    public float recoilYawDef;
    public float recoilPitchDef;
    public float recoilYawShift;
    public float recoilPitchShift;
    public float spread;

    public double damageHead;
    public double damageLeggs;

    public boolean scope;

    //str
    public String razryad;
    public String razryadErr;
    public String loreTemplate;
    public String needammo;
    public String fullammo;
    public String notammo;
    public String reloading;
    public String reloadFinish;
    public String shootnoammo;
    public String razrfinish;

    public double weight;

    public static JavaPlugin getInstance() {
        return pluginInstance;
    }

    public static void loadConfig(JavaPlugin plugin) {
        pluginInstance = plugin;
        Logger logger = plugin.getLogger();
        //logger.info("loading cfg");

        gunTypeKey = new NamespacedKey(plugin, "gun_type");
        currentAmmoKey = new NamespacedKey(plugin, "current_ammo");
        aimingKey = new NamespacedKey(plugin, "is_aiming");

        ConfigurationSection gunsSection = plugin.getConfig().getConfigurationSection("guns");
        if (gunsSection == null) {
            logger.severe("Секция guns не найдена");
            return;
        }

        int loaded = 0;
        for (String key : gunsSection.getKeys(false)) {
            try {
                Gun gun = new Gun();
                gun.id = key;

                gun.weight = gunsSection.getDouble(key + ".weight", 0.0);

                //str
                gun.razryad = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-razryad", "&bРазрядка..."));

                gun.razryadErr = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-razrerror", "&bОружие уже разряжено!"));

                gun.needammo = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-needammo", "&bНедостаточно патронов!"));

                gun.shootnoammo = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-shootnoammo", "&bПатроны закончились! (Shift+Q)."));

                gun.razrfinish = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-razrfinish", "&bОружие разряжено!"));

                gun.reloadFinish = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-reloadfinish", "&bПерезарядка завершена!"));

                gun.notammo = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-notammo", "&bНет патронов!"));

                gun.reloading = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-reloading", "&bПерезарядка..."));

                gun.fullammo = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("str-fullammo", "&bМагазин уже полон!"));

                gun.loreTemplate = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("gun-lore", "&7Патроны: {ammo}/{gunAmmoCount}"));

                gun.displayName = ChatColor.translateAlternateColorCodes('&',
                        gunsSection.getString(key + ".display-name", "&bGun"));

                gun.scope = gunsSection.getBoolean(key + ".scope", true);

                gun.damageBody = gunsSection.getDouble(key + ".damage-body", 6.0);
                gun.damageLeggs = gunsSection.getDouble(key + ".damage-leggs", 6.0);
                gun.damageHead = gunsSection.getDouble(key + ".damage-head", 6.0);

                gun.range = gunsSection.getInt(key + ".range", 15);
                gun.cooldown = gunsSection.getLong(key + ".cooldown", 100);
                gun.reloadTime = gunsSection.getLong(key + ".reload-time", 1000);
                gun.bulletsPerShot = gunsSection.getInt(key + ".bullets-per-shot", 1);

                gun.recoilYawDef = (float) gunsSection.getDouble(key + ".recoil-yaw-def", 0.0);
                gun.recoilPitchDef = (float) gunsSection.getDouble(key + ".recoil-pitch-def", 0.0);
                gun.recoilYawShift = (float) gunsSection.getDouble(key + ".recoil-yaw-shift", 0.0);
                gun.recoilPitchShift = (float) gunsSection.getDouble(key + ".recoil-pitch-shift", 0.0);
                gun.spread = (float) gunsSection.getDouble(key + ".spread", 0.08);

                String ammoItemStr = gunsSection.getString(key + ".ammo-item", "STICK");
                gun.ammoItem = Material.matchMaterial(ammoItemStr);
                if (gun.ammoItem == null) {
                    logger.warning("ammo-item error '" + key + "' witch: " + ammoItemStr);
                    continue;
                }

                String materialStr = gunsSection.getString(key + ".material", "WOODEN_HOE");
                gun.material = Material.matchMaterial(materialStr);
                if (gun.material == null) {
                    logger.warning("material for '" + key + "' error: " + materialStr);
                    continue;
                }

                gun.gunAmmoCount = gunsSection.getInt(key + ".gun-ammo-count", 30);

                String shootSoundStr = gunsSection.getString(key + ".shoot-sound", "ENTITY_ARROW_SHOOT").toUpperCase();
                try {
                    gun.shootSound = Sound.valueOf(shootSoundStr);
                } catch (IllegalArgumentException e) {
                    logger.warning("Error shoot-sound witch '" + key + "': " + shootSoundStr);
                    gun.shootSound = Sound.ENTITY_ARROW_SHOOT;
                }

                String reloadSoundStr = gunsSection.getString(key + ".reload-sound", "BLOCK_NOTE_BLOCK_BASS").toUpperCase();
                try {
                    gun.reloadSound = Sound.valueOf(reloadSoundStr);
                } catch (IllegalArgumentException e) {
                    logger.warning("Error reload-sound witch '" + key + "': " + reloadSoundStr);
                    gun.reloadSound = Sound.BLOCK_NOTE_BLOCK_BASS;
                }

                guns.put(key, gun);
                loaded++;
                //logger.info("Loading: " + key);
            } catch (Exception e) {
                logger.severe("Error load gun '" + key + "': " + e.getMessage());
            }
        }

        logger.info("loaded:" + loaded + " guns.");
    }
}
