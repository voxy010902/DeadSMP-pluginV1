package fr.bountycraft.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Clés PersistentDataContainer utilisées par le plugin. */
public final class Keys {
    public static NamespacedKey NPC_TYPE;
    public static NamespacedKey ITEM_ID;
    public static NamespacedKey ELIXIR_ID;
    public static NamespacedKey TEMP_UNTIL;
    public static NamespacedKey PLAYER_ELIXIR;
    public static NamespacedKey BREATH;

    private Keys() {}

    public static void init(Plugin plugin) {
        NPC_TYPE = new NamespacedKey(plugin, "npc_type");
        ITEM_ID = new NamespacedKey(plugin, "item_id");
        ELIXIR_ID = new NamespacedKey(plugin, "elixir_id");
        TEMP_UNTIL = new NamespacedKey(plugin, "temp_until");
        PLAYER_ELIXIR = new NamespacedKey(plugin, "player_elixir");
        BREATH = new NamespacedKey(plugin, "dragon_breath");
    }
}
