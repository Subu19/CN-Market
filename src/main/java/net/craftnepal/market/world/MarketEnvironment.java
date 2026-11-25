package net.craftnepal.market.world;

import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * How the market world behaves as a place: mobs, PvP, explosions, fire, weather, time, hunger.
 * Read from {@code market-world.environment}; the defaults make it a calm shopping hub.
 *
 * <p>Where the world has its own setting (game rules, the PvP flag, weather) {@link #apply} sets it,
 * so no work happens per event; the listener checks the same values as a backstop. Game rule names
 * changed in 26.x ({@code doMobSpawning} became {@code spawn_mobs}), so rules are found by name.
 */
public record MarketEnvironment(
        boolean mobSpawning,
        boolean hostileMobs,
        boolean pvp,
        Explosions explosions,
        boolean fireSpread,
        boolean clearWeather,
        boolean hunger,
        boolean fallDamage,
        boolean voidRescue,
        boolean lockDay,
        int maxMobsPerPlot) {

    public enum Explosions {
        /** No explosion does anything in the market world. */
        OFF,
        /** Explosions only affect blocks in their own plot (plot protection). */
        CONTAINED
    }

    /** Spawns a player or plugin causes on purpose. Everything else is the world spawning mobs. */
    static final Set<SpawnReason> DELIBERATE = EnumSet.of(
            SpawnReason.SPAWNER_EGG, SpawnReason.DISPENSE_EGG, SpawnReason.EGG,
            SpawnReason.BREEDING, SpawnReason.BUILD_SNOWMAN, SpawnReason.BUILD_IRONGOLEM,
            SpawnReason.BUILD_WITHER, SpawnReason.SHOULDER_ENTITY, SpawnReason.CURED,
            SpawnReason.COMMAND, SpawnReason.CUSTOM, SpawnReason.DEFAULT);

    /** Spawns that count against {@code max-mobs-per-plot}. */
    static final Set<SpawnReason> PLAYER_MADE = EnumSet.of(
            SpawnReason.SPAWNER_EGG, SpawnReason.DISPENSE_EGG, SpawnReason.EGG,
            SpawnReason.BREEDING, SpawnReason.BUILD_SNOWMAN, SpawnReason.BUILD_IRONGOLEM);

    static {
        // Newer than the 1.21 API: fish and axolotls from buckets, copper golems.
        for (String name : new String[] {"BUCKET", "BUILD_COPPERGOLEM"}) {
            try {
                SpawnReason reason = SpawnReason.valueOf(name);
                DELIBERATE.add(reason);
                PLAYER_MADE.add(reason);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static MarketEnvironment defaults() {
        return new MarketEnvironment(false, false, false, Explosions.OFF, false, true, false, false, true, false, 0);
    }

    public static MarketEnvironment from(ConfigurationSection c, Logger log) {
        MarketEnvironment d = defaults();
        if (c == null) return d;
        Explosions explosions = d.explosions();
        String mode = c.getString("explosions", "off").trim().toLowerCase(Locale.ROOT);
        switch (mode) {
            case "off", "false" -> explosions = Explosions.OFF;
            case "contained", "vanilla", "true" -> explosions = Explosions.CONTAINED;
            default -> log.warning("market-world.environment.explosions: unknown value '" + mode + "', using off");
        }
        String weather = c.getString("weather", "clear").trim().toLowerCase(Locale.ROOT);
        String time = c.getString("time", "cycle").trim().toLowerCase(Locale.ROOT);
        return new MarketEnvironment(
                c.getBoolean("mob-spawning", d.mobSpawning()),
                c.getBoolean("hostile-mobs", d.hostileMobs()),
                c.getBoolean("pvp", d.pvp()),
                explosions,
                c.getBoolean("fire-spread", d.fireSpread()),
                !weather.equals("cycle"),
                c.getBoolean("hunger", d.hunger()),
                c.getBoolean("fall-damage", d.fallDamage()),
                c.getBoolean("void-rescue", d.voidRescue()),
                time.equals("day"),
                Math.max(0, c.getInt("max-mobs-per-plot", d.maxMobsPerPlot())));
    }

    /** Whether a mob may spawn in the market world. {@code hostile}: the mob is an enemy. */
    public boolean allowsSpawn(SpawnReason reason, boolean hostile) {
        boolean byCommand = reason == SpawnReason.COMMAND || reason == SpawnReason.CUSTOM;
        if (hostile && !hostileMobs && !byCommand) return false;
        return mobSpawning || DELIBERATE.contains(reason);
    }

    /** Whether a spawn counts against {@link #maxMobsPerPlot()}. */
    public boolean capped(SpawnReason reason) {
        return maxMobsPerPlot > 0 && PLAYER_MADE.contains(reason);
    }

    // ── Applying to the world ───────────────────────────────────────────────────

    /** A boolean game rule: names to try (1.21, then 26.x) and the value that restricts it. */
    private record Rule(String id, boolean restricted, String... names) {}

    private static final List<Rule> RULES = List.of(
            new Rule("mobs", false, "doMobSpawning", "spawn_mobs"),
            new Rule("patrols", false, "doPatrolSpawning", "spawn_patrols"),
            new Rule("traders", false, "doTraderSpawning", "spawn_wandering_traders"),
            new Rule("wardens", false, "doWardenSpawning", "spawn_wardens"),
            new Rule("phantoms", false, "doInsomnia", "spawn_phantoms"),
            new Rule("raids", false, "raids"),
            new Rule("raids-old", true, "disableRaids"),
            new Rule("spawners", false, "spawner_blocks_work"),
            new Rule("monsters", false, "spawn_monsters"),
            new Rule("weather", false, "doWeatherCycle", "advance_weather"),
            new Rule("time", false, "doDaylightCycle", "advance_time"));

    private boolean wants(Rule rule) {
        return switch (rule.id()) {
            case "monsters" -> !hostileMobs;
            case "weather" -> clearWeather;
            case "time" -> lockDay;
            default -> !mobSpawning;
        };
    }

    private static final NamespacedKey MANAGED = new NamespacedKey("market", "managed_game_rules");

    /**
     * Sets the world's own settings. A rule this plugin restricted earlier and no longer should is
     * put back to its vanilla value; rules the plugin never touched are left as the admin set them.
     */
    public void apply(World world) {
        String stored = world.getPersistentDataContainer().getOrDefault(MANAGED, PersistentDataType.STRING, "");
        Set<String> before = new LinkedHashSet<>(List.of(stored.split(",")));
        Set<String> now = new LinkedHashSet<>();
        for (Rule rule : RULES) {
            if (wants(rule)) {
                if (setRule(world, rule, rule.restricted())) now.add(rule.id());
            } else if (before.contains(rule.id())) {
                setRule(world, rule, !rule.restricted());
            }
        }
        world.getPersistentDataContainer().set(MANAGED, PersistentDataType.STRING, String.join(",", now));

        world.setPVP(pvp);
        if (clearWeather) {
            world.setStorm(false);
            world.setThundering(false);
        }
        if (lockDay) world.setTime(6000);
    }

    /**
     * {@code GameRule} is a class on Spigot and an interface on Paper, so a direct call to it compiled
     * against one fails on the other. Find the rule by reflection; only call methods on {@link World},
     * which is an interface on both.
     */
    private static final java.lang.reflect.Method GET_BY_NAME = findGetByName();

    private static java.lang.reflect.Method findGetByName() {
        try {
            // initialize=false: GameRule's static setup needs a running server
            return Class.forName("org.bukkit.GameRule", false, MarketEnvironment.class.getClassLoader())
                    .getMethod("getByName", String.class);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean setRule(World world, Rule rule, boolean value) {
        if (GET_BY_NAME == null) return false;
        for (String name : rule.names()) {
            try {
                Object found = GET_BY_NAME.invoke(null, name);
                if (found == null) continue;
                GameRule<Boolean> gameRule = (GameRule<Boolean>) found;
                if (!(world.getGameRuleValue(gameRule) instanceof Boolean)) continue;
                world.setGameRule(gameRule, value);
                return true;
            } catch (ReflectiveOperationException | ClassCastException e) {
                // not a boolean rule here; try the next name
            }
        }
        return false; // not on this server version; the listener still enforces it
    }

    /** One line per setting, for {@code /market admin environment}. */
    public List<String> describe() {
        return List.of(
                "mob-spawning: " + mobSpawning + "  hostile-mobs: " + hostileMobs,
                "pvp: " + pvp + "  explosions: " + explosions.name().toLowerCase(Locale.ROOT)
                        + "  fire-spread: " + fireSpread,
                "weather: " + (clearWeather ? "clear" : "cycle") + "  time: " + (lockDay ? "day" : "cycle"),
                "hunger: " + hunger + "  fall-damage: " + fallDamage + "  void-rescue: " + voidRescue,
                "max-mobs-per-plot: " + (maxMobsPerPlot == 0 ? "no cap" : maxMobsPerPlot));
    }
}
