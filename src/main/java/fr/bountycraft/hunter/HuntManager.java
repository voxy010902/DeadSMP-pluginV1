package fr.bountycraft.hunter;

import fr.bountycraft.BountyCraft;
import fr.bountycraft.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Évènement /hunt : pendant X minutes tous les joueurs doivent traquer la cible désignée.
 * Une boss bar affiche le temps restant, la prime et les coordonnées de la cible.
 */
public final class HuntManager {
    private final BountyCraft plugin;

    private UUID target;
    private String targetName;
    private long reward;
    private long startMs;
    private long endMs;
    private BossBar hunterBar;
    private BossBar targetBar;
    private Location lastKnown;

    public HuntManager(BountyCraft plugin) {
        this.plugin = plugin;
    }

    public boolean active() {
        return target != null;
    }

    public boolean isTarget(UUID id) {
        return target != null && target.equals(id);
    }

    public long reward() { return reward; }
    public String targetName() { return targetName; }
    public long secondsLeft() { return Math.max(0, (endMs - System.currentTimeMillis()) / 1000); }

    /** @return null si OK, sinon un message d'erreur. */
    public String start(CommandSender admin, Player tgt, Long rewardOverride) {
        if (active()) return "Une chasse est déjà en cours (/hunt stop pour l'arrêter).";

        int minutes = plugin.getConfig().getInt("hunt-event.duration-minutes", 30);
        long min = plugin.getConfig().getLong("hunt-event.min-reward", 5000);
        double mult = plugin.getConfig().getDouble("hunt-event.reward-multiplier", 5);
        reward = rewardOverride != null
                ? rewardOverride
                : Math.max(min, Math.round(plugin.contracts().bountyFor(tgt.getUniqueId()) * mult));

        target = tgt.getUniqueId();
        targetName = tgt.getName();
        lastKnown = tgt.getLocation();
        startMs = System.currentTimeMillis();
        endMs = startMs + minutes * 60_000L;

        hunterBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        targetBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        update();

        for (Player p : Bukkit.getOnlinePlayers()) {
            show(p);
            p.showTitle(Title.title(
                    Text.mm("<dark_red><bold>CHASSE À L'HOMME"),
                    Text.mm("<yellow>" + targetName + " <gray>- <gold>" + Text.money(reward))));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.2f);
        }
        Text.broadcast("<dark_red><bold>CHASSE À L'HOMME ! <gray>Tuez <yellow>" + targetName
                + "<gray> dans les <aqua>" + minutes + " minutes<gray> pour gagner <gold>"
                + Text.money(reward) + "<gray> !");
        return null;
    }

    /** Arrête la chasse. */
    public void stop(String reasonMiniMessage) {
        if (!active()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(hunterBar);
            p.hideBossBar(targetBar);
        }
        target = null;
        targetName = null;
        hunterBar = null;
        targetBar = null;
        lastKnown = null;
        if (reasonMiniMessage != null) Text.broadcast(reasonMiniMessage);
    }

    /** Arrêt silencieux (extinction du plugin). */
    public void shutdown() {
        if (active()) stop(null);
    }

    public void show(Player p) {
        if (!active()) return;
        p.showBossBar(p.getUniqueId().equals(target) ? targetBar : hunterBar);
    }

    public void onJoin(Player p) {
        show(p);
    }

    /** Appelé quand un joueur en tue un autre. */
    public void onKill(Player killer, Player victim) {
        if (!active() || !victim.getUniqueId().equals(target)) return;
        plugin.economy().deposit(killer.getUniqueId(), reward);
        String reason = "<green><bold>CHASSE TERMINÉE ! <white>" + killer.getName()
                + "<gray> a abattu <yellow>" + targetName + "<gray> et remporte <gold>"
                + Text.money(reward) + "<gray> !";
        long won = reward;
        stop(reason);
        Text.send(killer, "<green>Vous recevez <gold>" + Text.money(won) + "<green> !");
    }

    /** Appelé chaque seconde. */
    public void tick() {
        if (!active()) return;
        if (System.currentTimeMillis() >= endMs) {
            stop("<aqua><bold>CHASSE TERMINÉE ! <yellow>" + targetName
                    + "<gray> a survécu. Personne ne remporte la prime.");
            return;
        }
        update();
    }

    private void update() {
        long now = System.currentTimeMillis();
        long left = Math.max(0, (endMs - now) / 1000);
        float progress = (float) Math.max(0.0, Math.min(1.0, (endMs - now) / (double) (endMs - startMs)));

        Player t = Bukkit.getPlayer(target);
        if (t != null) lastKnown = t.getLocation();

        String coords = "?";
        if (lastKnown != null) {
            coords = "X " + lastKnown.getBlockX() + "  Y " + lastKnown.getBlockY() + "  Z " + lastKnown.getBlockZ()
                    + " (" + ContractManager.dimension(lastKnown.getWorld()) + ")";
        }
        String offline = t == null ? " <gray>[hors-ligne]" : "";

        hunterBar.name(Text.mm("<red><bold>CHASSE <white>» <yellow>" + targetName + offline
                + " <dark_gray>| <aqua>" + Text.time(left)
                + " <dark_gray>| <gold>" + Text.money(reward)
                + " <dark_gray>| <green>" + coords));
        hunterBar.progress(progress);

        targetBar.name(Text.mm("<light_purple><bold>VOUS ÊTES LA CIBLE ! <white>» <aqua>" + Text.time(left)
                + " <dark_gray>| <gold>" + Text.money(reward)
                + " <dark_gray>| <green>" + coords));
        targetBar.progress(progress);
    }
}
