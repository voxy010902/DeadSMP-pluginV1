package fr.bountycraft.economy;

import fr.bountycraft.BountyCraft;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Économie du serveur + statistiques de PvP (utilisées pour calculer les primes).
 * Toutes les méthodes sont à appeler depuis le thread principal.
 * Stockage : plugins/BountyCraft/data.yml
 */
public final class EconomyManager {
    private static final long MAX_BALANCE = 1_000_000_000_000_000L;

    public static final class Account {
        private final UUID id;
        private String name;
        private long balance;
        private int deathsByPlayers;
        private int kills;

        Account(UUID id, String name, long balance, int deathsByPlayers, int kills) {
            this.id = id;
            this.name = name;
            this.balance = balance;
            this.deathsByPlayers = deathsByPlayers;
            this.kills = kills;
        }

        public UUID id() { return id; }
        public String name() { return name; }
        public long balance() { return balance; }
        public int deathsByPlayers() { return deathsByPlayers; }
        public int kills() { return kills; }
    }

    private final BountyCraft plugin;
    private final File file;
    private final Object ioLock = new Object();
    private final Map<UUID, Account> accounts = new HashMap<>();
    private boolean dirty;

    public EconomyManager(BountyCraft plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
        long period = Math.max(1, plugin.getConfig().getLong("economy.autosave-minutes", 5)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAsync, period, period);
    }

    // ------------------------------------------------------------------ comptes

    public Account get(UUID id) {
        return accounts.get(id);
    }

    /** Récupère (ou crée) le compte d'un joueur en ligne et met à jour son pseudo. */
    public Account of(Player p) {
        Account a = accounts.get(p.getUniqueId());
        if (a == null) {
            long start = plugin.getConfig().getLong("economy.starting-balance", 500);
            a = new Account(p.getUniqueId(), p.getName(), start, 0, 0);
            accounts.put(a.id, a);
            dirty = true;
        } else if (!a.name.equals(p.getName())) {
            a.name = p.getName();
            dirty = true;
        }
        return a;
    }

    public Account findByName(String name) {
        for (Account a : accounts.values()) {
            if (a.name.equalsIgnoreCase(name)) return a;
        }
        return null;
    }

    public List<String> knownNames() {
        List<String> out = new ArrayList<>();
        for (Account a : accounts.values()) out.add(a.name);
        return out;
    }

    // ------------------------------------------------------------------ argent

    public long balance(UUID id) {
        Account a = accounts.get(id);
        return a == null ? 0 : a.balance;
    }

    public void deposit(UUID id, long amount) {
        Account a = accounts.get(id);
        if (a == null || amount <= 0) return;
        a.balance = (a.balance > MAX_BALANCE - amount) ? MAX_BALANCE : a.balance + amount;
        dirty = true;
    }

    public boolean withdraw(UUID id, long amount) {
        Account a = accounts.get(id);
        if (a == null || amount < 0 || a.balance < amount) return false;
        a.balance -= amount;
        dirty = true;
        return true;
    }

    public void set(UUID id, long amount) {
        Account a = accounts.get(id);
        if (a == null) return;
        a.balance = Math.max(0, Math.min(MAX_BALANCE, amount));
        dirty = true;
    }

    public boolean transfer(UUID from, UUID to, long amount) {
        if (!withdraw(from, amount)) return false;
        deposit(to, amount);
        return true;
    }

    public List<Account> top(int n) {
        List<Account> all = new ArrayList<>(accounts.values());
        all.sort(Comparator.comparingLong((Account a) -> a.balance).reversed());
        return all.subList(0, Math.min(n, all.size()));
    }

    // ------------------------------------------------------------------ stats PvP

    /** Nombre de fois où le joueur est mort de la main d'un autre joueur. */
    public int deathsByPlayers(UUID id) {
        Account a = accounts.get(id);
        return a == null ? 0 : a.deathsByPlayers;
    }

    public void addDeathByPlayer(UUID id) {
        Account a = accounts.get(id);
        if (a != null) { a.deathsByPlayers++; dirty = true; }
    }

    public void addKill(UUID id) {
        Account a = accounts.get(id);
        if (a != null) { a.kills++; dirty = true; }
    }

    // ------------------------------------------------------------------ persistance

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("accounts");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection a = s.getConfigurationSection(key);
                if (a == null) continue;
                accounts.put(id, new Account(id, a.getString("name", "?"), a.getLong("balance"),
                        a.getInt("deaths"), a.getInt("kills")));
            } catch (IllegalArgumentException ignored) {
                // clé invalide : on ignore
            }
        }
    }

    private String snapshot() {
        YamlConfiguration y = new YamlConfiguration();
        for (Account a : accounts.values()) {
            String base = "accounts." + a.id;
            y.set(base + ".name", a.name);
            y.set(base + ".balance", a.balance);
            y.set(base + ".deaths", a.deathsByPlayers);
            y.set(base + ".kills", a.kills);
        }
        return y.saveToString();
    }

    private void write(String data) {
        synchronized (ioLock) {
            try {
                Files.createDirectories(file.getParentFile().toPath());
                Path tmp = new File(file.getParentFile(), "data.yml.tmp").toPath();
                Files.writeString(tmp, data, StandardCharsets.UTF_8);
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de sauvegarder data.yml : " + e.getMessage());
            }
        }
    }

    public void saveAsync() {
        if (!dirty) return;
        dirty = false;
        String data = snapshot();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(data));
    }

    public void saveNow() {
        dirty = false;
        write(snapshot());
    }
}
