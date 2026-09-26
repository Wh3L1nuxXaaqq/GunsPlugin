package invoke.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class GunCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public GunCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use command.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("Usage: /getgun <id>");
            return true;
        }
        String gunId = "weapon" + args[0];
        Gun gun = Gun.guns.get(gunId);
        if (gun == null) {
            sender.sendMessage("Gun not found.");
            return true;
        }
        Player player = (Player) sender;
        ItemStack item = new ItemStack(gun.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', gun.displayName));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(Gun.gunTypeKey, PersistentDataType.STRING, gun.id);
            pdc.set(Gun.currentAmmoKey, PersistentDataType.INTEGER, gun.gunAmmoCount);
            item.setItemMeta(meta);
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(ChatColor.GRAY + "Ammo: " + gun.gunAmmoCount + "/" + gun.gunAmmoCount);
            meta.setLore(lore);
        }
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "You received " + gun.displayName);
        return true;
    }
}