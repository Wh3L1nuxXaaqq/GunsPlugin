package invoke.plugin;

import org.bukkit.ChatColor;
import java.util.Map;

public class TextUtil {
    public static String format(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }
}
