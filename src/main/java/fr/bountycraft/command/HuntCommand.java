package fr.bountycraft.command;

import fr.bountycraft.BountyCraft;
import fr.bountycraft.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /hunt <joueur> [prime] | /hunt stop | /hunt info */
public final class HuntCommand implements CommandExecutor, TabCompleter {
    private final BountyCraft plugin;

    public HuntCommand(BountyCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            Text.send(s, "<red>Usage : /hunt <joueur> [prime] | /hunt stop | /hunt info");
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "stop" -> {
                if (!plugin.hunts().active()) {
                    Text.send(s, "<red>Aucune chasse en cours.");
                } else {
                    plugin.hunts().stop("<yellow>La chasse a été arrêtée par un administrateur.");
                }
            }
            case "info" -> {
                if (!plugin.hunts().active()) {
                    Text.send(s, "<gray>Aucune chasse en cours.");
                } else {
                    Text.send(s, "<gray>Cible : <yellow>" + plugin.hunts().targetName() + "<gray> | Prime : <gold>"
                            + Text.money(plugin.hunts().reward()) + "<gray> | Reste : <aqua>"
                            + Text.time(plugin.hunts().secondsLeft()));
                }
            }
            default -> {
                Player target = Bukkit.getPlayerExact(a[0]);
                if (target == null) {
                    Text.send(s, "<red>Joueur introuvable ou hors-ligne : " + a[0]);
                    return true;
                }
                Long reward = null;
                if (a.length > 1) {
                    try {
                        reward = Long.parseLong(a[1]);
                        if (reward <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        Text.send(s, "<red>Prime invalide.");
                        return true;
                    }
                }
                String err = plugin.hunts().start(s, target, reward);
                if (err != null) Text.send(s, "<red>" + err);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            String last = a[0].toLowerCase(Locale.ROOT);
            for (String o : List.of("stop", "info")) if (o.startsWith(last)) out.add(o);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(last)) out.add(p.getName());
            }
        }
        return out;
    }
}
