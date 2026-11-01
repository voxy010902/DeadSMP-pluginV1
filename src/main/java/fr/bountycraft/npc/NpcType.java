package fr.bountycraft.npc;

import java.util.Locale;

public enum NpcType {
    HUNTER("hunter"),
    BLACKSMITH("blacksmith"),
    ALCHEMIST("alchemist"),
    GUIDE("guide");

    private final String configKey;

    NpcType(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }

    public static NpcType parse(String s) {
        if (s == null) return null;
        String low = s.toLowerCase(Locale.ROOT);
        for (NpcType t : values()) {
            if (t.name().equalsIgnoreCase(low) || t.configKey.equals(low)) return t;
        }
        // alias français
        return switch (low) {
            case "forgeron" -> BLACKSMITH;
            case "alchimiste" -> ALCHEMIST;
            default -> null;
        };
    }
}
