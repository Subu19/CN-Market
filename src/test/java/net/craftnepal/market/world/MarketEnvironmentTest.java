package net.craftnepal.market.world;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static net.craftnepal.market.world.MarketEnvironment.Explosions.CONTAINED;
import static net.craftnepal.market.world.MarketEnvironment.Explosions.OFF;
import static org.junit.jupiter.api.Assertions.*;

class MarketEnvironmentTest {

    private static final Logger LOG = Logger.getLogger("test");

    private static MarketEnvironment parse(String yaml) throws Exception {
        YamlConfiguration c = new YamlConfiguration();
        c.loadFromString(yaml);
        return MarketEnvironment.from(c.getConfigurationSection("environment"), LOG);
    }

    @Test
    void missingSectionGivesCalmDefaults() {
        MarketEnvironment env = MarketEnvironment.from(null, LOG);
        assertEquals(MarketEnvironment.defaults(), env);
        assertFalse(env.mobSpawning());
        assertFalse(env.hostileMobs());
        assertFalse(env.pvp());
        assertEquals(OFF, env.explosions());
        assertFalse(env.fireSpread());
        assertTrue(env.clearWeather());
        assertTrue(env.voidRescue());
        assertFalse(env.lockDay());
        assertEquals(0, env.maxMobsPerPlot());
    }

    @Test
    void readsEveryKey() throws Exception {
        MarketEnvironment env = parse("""
                environment:
                  mob-spawning: true
                  hostile-mobs: true
                  pvp: true
                  explosions: contained
                  fire-spread: true
                  weather: cycle
                  time: day
                  hunger: true
                  fall-damage: true
                  void-rescue: false
                  max-mobs-per-plot: 24
                """);
        assertEquals(new MarketEnvironment(true, true, true, CONTAINED, true, false, true, true, false, true, 24), env);
    }

    @Test
    void yamlBooleansForExplosionsStillWork() throws Exception {
        // YAML 1.1 reads an unquoted off/on as a boolean.
        assertEquals(OFF, parse("environment:\n  explosions: off\n").explosions());
        assertEquals(CONTAINED, parse("environment:\n  explosions: on\n").explosions());
        assertEquals(OFF, parse("environment:\n  explosions: nonsense\n").explosions());
        assertEquals(0, parse("environment:\n  max-mobs-per-plot: -5\n").maxMobsPerPlot());
    }

    @Test
    void defaultsAllowOnlyDeliberateSpawns() {
        MarketEnvironment env = MarketEnvironment.defaults();
        // The world spawning things on its own.
        for (SpawnReason r : new SpawnReason[] {SpawnReason.NATURAL, SpawnReason.CHUNK_GEN, SpawnReason.SPAWNER,
                SpawnReason.PATROL, SpawnReason.RAID, SpawnReason.REINFORCEMENTS, SpawnReason.LIGHTNING,
                SpawnReason.SLIME_SPLIT, SpawnReason.TRAP, SpawnReason.VILLAGE_DEFENSE}) {
            assertFalse(env.allowsSpawn(r, false), r.name());
        }
        // Players acting on purpose, for animals.
        for (SpawnReason r : new SpawnReason[] {SpawnReason.SPAWNER_EGG, SpawnReason.BREEDING, SpawnReason.valueOf("BUCKET"),
                SpawnReason.BUILD_IRONGOLEM, SpawnReason.DISPENSE_EGG}) {
            assertTrue(env.allowsSpawn(r, false), r.name());
        }
        // Hostile mobs only by command or plugin.
        assertFalse(env.allowsSpawn(SpawnReason.SPAWNER_EGG, true));
        assertFalse(env.allowsSpawn(SpawnReason.BUILD_WITHER, true));
        assertTrue(env.allowsSpawn(SpawnReason.COMMAND, true));
        assertTrue(env.allowsSpawn(SpawnReason.CUSTOM, true));
    }

    @Test
    void vanillaSettingsAllowEverything() throws Exception {
        MarketEnvironment env = parse("environment:\n  mob-spawning: true\n  hostile-mobs: true\n");
        assertTrue(env.allowsSpawn(SpawnReason.NATURAL, true));
        assertTrue(env.allowsSpawn(SpawnReason.SPAWNER, false));
    }

    @Test
    void capCountsPlayerMadeSpawnsOnly() throws Exception {
        assertFalse(MarketEnvironment.defaults().capped(SpawnReason.BREEDING)); // no cap by default
        MarketEnvironment env = parse("environment:\n  max-mobs-per-plot: 24\n");
        assertTrue(env.capped(SpawnReason.BREEDING));
        assertTrue(env.capped(SpawnReason.SPAWNER_EGG));
        assertFalse(env.capped(SpawnReason.COMMAND));
    }
}
