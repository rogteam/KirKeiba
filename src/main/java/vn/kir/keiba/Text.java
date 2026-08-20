package vn.kir.keiba;

import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class Text {
    private Text() { }

    static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    static String render(String value, Map<String, ?> variables) {
        String rendered = value == null ? "" : value;
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        rendered = rendered.replaceAll("\\{[a-zA-Z0-9_-]+}", "—");
        return color(rendered);
    }

    static List<String> render(List<String> values, Map<String, ?> variables) {
        List<String> rendered = new ArrayList<>();
        for (String value : values) rendered.add(render(value, variables));
        return rendered;
    }
}
