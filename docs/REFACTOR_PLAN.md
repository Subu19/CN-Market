# CN-Market refactor plan

Written 2026-11-24. Status: **planning done, decisions D1-D9 answered, no code changed yet.**

Goal: refactor the whole plugin for performance and maintainability **without changing features**, and fix two reported problems along the way:

1. SQLite queries on the main thread for every redstone change (spark profile: plugin went from 0.6% to 21.6% of the server thread, TPS 16.6).
2. Plot protection is weaker than GriefPrevention's.

Each phase below is written so it can be executed in a fresh session. Read "Ground rules" and "Verified facts" first, then the phase you are doing.

Paths are relative to the repo root. `M/` means `src/main/java/net/craftnepal/market/`.

---

## Progress tracker

Tick these as phases land. One phase per branch/PR.

- [x] Phase 1: Baseline build and test harness
- [x] Phase 2: In-memory plot index and cached settings (fixes Problem 1; shippable alone as a hotfix)
- [x] Phase 3: Protection core (Problem 2)
- [x] Phase 4: Shop index, serialized database access, async writes
- [x] Phase 5: Price and metrics file I/O off the main thread
- [x] Phase 6: Bug fixes (only the ones approved under "Decisions")
- [x] Phase 7: Cleanup
- [x] Phase 8: Final verification
- [x] Phase 9: Public release readiness

## Decisions (answered by the owner on 2026-11-24)

The goal changed with these answers: **the plugin is to be released publicly**, not only run on the owner's server. Compatibility with as many server types as possible matters.

| # | Decision | Answer |
|---|---|---|
| D1 | Paper API or Spigot API? | **Stay on Spigot API.** No Paper-only classes anywhere in the code. The four Paper-only events in "Verified facts" are out of scope. |
| D2 | Fix the behaviour bugs in Phase 6? | **Yes, all of them**, groups A and B. Where a group B item offers a choice, the chosen option is written in its row. |
| D3 | Add the new protection coverage in Phase 3? | **Yes.** |
| D4 | Take over what WorldGuard does in the market world (PvP, mob spawning, hunger)? | Not in the refactor phases. Because of the public release it is now planned as an **opt-in config section in Phase 9**: other servers will not have WorldGuard set up for this world. |
| D5 | Licence. | GriefPrevention (GPL v3) is used for ideas and coverage only. Copy no code. The repo has **no LICENSE file** although the README badge says MIT; Phase 9 adds one. |

Answered on 2026-11-24 as well:

| # | Question | Answer |
|---|---|---|
| D6 | Oldest Minecraft version to support. | **1.21 and newer.** |
| D7 | Default permissions. | **Player permissions default to everyone** (`market.use`, `market.shops`, `market.back`, `market.plot.*`, and declare `market.plots.limit.*`). Admin permissions stay `op`. |
| D8 | Folia support. | **Later, not in this release.** Say so in the README. |
| D9 | Spigot API version to compile against. | **Update to the 26.3 API** (latest Minecraft is 26.3; the live server runs Paper 26.3). |

### How D6 and D9 fit together

Compiling against the 26.3 API while promising 1.21 support has one risk: code can call a method that only exists in newer versions, compile fine, and then crash on a 1.21 server with `NoSuchMethodError`. The plan handles it like this:

- `pom.xml` default dependency: `spigot-api` for 26.3. **Unverified:** the exact version string and that it is published in the Spigot snapshots repository. Phase 1 confirms by resolving it. The current string is `26.1.2-R0.1-SNAPSHOT`.
- `plugin.yml`: `api-version: '1.21'` (the oldest supported version, so 1.21 servers load the plugin).
- Bytecode stays Java 21 (`<release>21</release>`): 1.21 servers run on Java 21, 26.x servers on Java 25, and Java 21 bytecode runs on both.
- A second Maven profile, `-Pspigot-1.21`, compiles the same sources against the 1.21 Spigot API. CI runs both. If the 1.21 compile fails, the code used something newer than 1.21 and must either avoid it or guard it with a class or method presence check.
- Phase 8 and Phase 9 boot tests run on 1.21 and on 26.3, Spigot and Paper.
- Known places to check first, from the audit: potion data access in `ShopUtils.getItemKey` and `PriceData`, `Enchantment.getByKey`, `Particle.DUST`, anything touching `GameRule`.

Do the API bump in **Phase 1**, as its own commit, before any refactoring, so compile errors from the bump are not mixed with refactor errors.

---

## Ground rules for every phase

1. **No feature changes** unless listed under an approved decision. The feature inventory in Appendix A is the contract.
2. **No SQL, no file I/O, no `config.get*` on the main thread during gameplay.** Startup, shutdown and admin commands that are explicitly one-off (`setup`, `reset`, `reload`) are exempt.
3. **Do not invent API.** Only use classes and methods listed under "Verified facts" or already used in the codebase. Anything else must be checked with `javap` against the jar first.
4. **Keep the SQLite schema as it is.** No migrations. Existing `market.db` files must load unchanged.
5. **Keep plot ids as they are** (`plot_<x>_<z>` for grid plots, `manual_<millis>` for selected plots).
6. Each phase ends with: project compiles, unit tests pass, server boots in Docker with no exceptions in the log.
7. Commit per phase. Do not mix cleanup into behaviour phases.

---

## Verified facts (Phase 0 output)

Gathered from real jars and source on 2026-11-24. Items marked **unverified** must be checked before relying on them.

### Environment

- Server: Paper 26.1.2 build 66 (`mc-data/paper-26.1.2-66.jar`). Needs Java 25 per its `version.json`. Local JDK is 27.
- Current compile dependency: `org.spigotmc:spigot-api:26.1.2-R0.1-SNAPSHOT`. Jar: `~/.m2/repository/org/spigotmc/spigot-api/26.1.2-R0.1-SNAPSHOT/`.
- Paper API jar for the server: `mc-data/libraries/io/papermc/paper/paper-api/26.1.2.build.66-stable/`. Coordinates `io.papermc.paper:paper-api:26.1.2.build.66-stable`.
- **Unverified:** Paper Maven repository URL (believed `https://repo.papermc.io/repository/maven-public/`). Confirm by resolving the dependency.
- **Unverified:** correct `api-version` for `plugin.yml` on 26.x (currently `"1.20.4"`). GriefPrevention uses `'26.2'` for its 26.2 build, so `'26.1'` is the likely value. Confirm by booting and reading the plugin load line in the log.
- The server bundles `sqlite-jdbc 3.49.1.0` (`mc-data/libraries/org/xerial/sqlite-jdbc/3.49.1.0/`). The pom has no sqlite dependency; keep relying on the server's copy.
- The baseline build has **not** been run in this planning session. Phase 1 starts by confirming the project compiles as-is.
- There are no tests in the repo.

### Events that exist in both Spigot and Paper jars

All in `org.bukkit.event.*`:

`PlayerBucketEmptyEvent`, `PlayerBucketFillEvent`, `PlayerBucketEntityEvent`, `EntityPlaceEvent`, `PlayerArmorStandManipulateEvent`, `PlayerTakeLecternBookEvent`, `SignChangeEvent`, `BlockFertilizeEvent`, `BlockMultiPlaceEvent`, `BlockDispenseEvent`, `BlockRedstoneEvent`, `SpongeAbsorbEvent`, `BlockFromToEvent`, `InventoryMoveItemEvent`, `InventoryPickupItemEvent`, `VehicleDamageEvent`, `VehicleDestroyEvent`, `HangingPlaceEvent`, `HangingBreakByEntityEvent`, `ProjectileHitEvent`, `EntityChangeBlockEvent`, `EntityExplodeEvent`, `EntityDamageByEntityEvent`, `PlayerInteractEntityEvent`, `PlayerInteractAtEntityEvent`, `block.TNTPrimeEvent`, `block.EntityBlockFormEvent`.

Getters confirmed by `javap` (Paper build 66):

| Event | Actor | Affected |
|---|---|---|
| `PlayerBucketEmptyEvent` / `FillEvent` | `getPlayer()` | `getBlock()` is the block affected; `getBlockClicked()`, `getBlockFace()` |
| `EntityPlaceEvent` | `getPlayer()` (nullable) | `getBlock()`, `getEntity()` |
| `PlayerArmorStandManipulateEvent` | `getPlayer()` | `getRightClicked()` |
| `PlayerTakeLecternBookEvent` | `getPlayer()` | `getLectern()` |
| `SignChangeEvent` | `getPlayer()` | `getBlock()` |
| `BlockFertilizeEvent` | `getPlayer()` (nullable) | `getBlock()`, `getBlocks()` (`List<BlockState>`) |
| `BlockMultiPlaceEvent` | `getPlayer()` | `getReplacedBlockStates()` |
| `TNTPrimeEvent` (Bukkit) | `getPrimingEntity()`, `getPrimingBlock()` | `getBlock()`, `getCause()` |
| `BlockDispenseEvent` | none | `getBlock()` (the dispenser), `getItem()` |
| `BlockRedstoneEvent` | none | `getBlock()`, `getOldCurrent()`, `getNewCurrent()`, `setNewCurrent(int)` |
| `InventoryMoveItemEvent` | none | `getSource()`, `getDestination()`, `getInitiator()` |
| `VehicleDamageEvent` / `DestroyEvent` | `getAttacker()` | `getVehicle()` |
| `HangingPlaceEvent` | `getPlayer()` | `getEntity()`, `getBlock()` |
| `ProjectileHitEvent` | `getEntity().getShooter()` | `getHitBlock()`, `getHitEntity()` |

### Paper-only (do NOT use: D1 answered "stay on Spigot API")

- `com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent`: `getEntity()`, `getHitBy()`. Note the package: it is **not** under `io.papermc.paper`.
- `io.papermc.paper.event.block.BlockPreDispenseEvent`: `getBlock()`, `getItemStack()`, `getSlot()`.
- `io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent`: `getPlayer()`, `getFlowerpot()`.
- `io.papermc.paper.event.entity.EntityInsideBlockEvent`: `getEntity()`, `getBlock()`.
- `io.papermc.paper.event.player.PlayerOpenSignEvent`: `getPlayer()`, `getSign()`.
- `Chunk.getChunkKey(...)`, `Block.getBlockKey(...)`, `Location.toBlockKey()`. **Do not use these**; compute keys ourselves so the index does not depend on D1.

### Things that do NOT exist or must not be used

- `BlockRedstoneEvent` is **not Cancellable**. The only lever is `setNewCurrent`. `ignoreCancelled` on its handler is meaningless.
- `org.bukkit.event.entity.EntityBlockFormEvent` does not exist. The class is `org.bukkit.event.block.EntityBlockFormEvent`.
- `io.papermc.paper.event.entity.EntityKnockbackByEntityEvent` does not exist (see correct package above).
- `com.destroystokyo.paper.event.block.TNTPrimeEvent` is deprecated for removal. Use `org.bukkit.event.block.TNTPrimeEvent`.
- `GameRule` constant names differ between the Spigot snapshot jar (new names such as `SPAWN_MOBS`, `ADVANCE_WEATHER`) and Paper build 66 (old names such as `DO_MOB_SPAWNING`, `DO_FIRE_TICK`). Do not reference game rule constants at all; use events instead (see Phase 9).

### GriefPrevention reference (ideas only, GPL v3)

Source read: GriefPrevention 18.0.0-SNAPSHOT, `DataStore.java`, `BlockEventHandler.java`, `EntityEventHandler.java`, `EntityDamageHandler.java`, `PlayerEventHandler.java`.

- **Lookup:** a map from chunk key to a short list of claims, each claim registered under every chunk it overlaps. A "last claim" is passed back in and tested first, so repeated lookups in the same claim skip the map.
- **Its chunk key is collision-prone** for negative z. Ours must be `((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL)`.
- **Pistons:** take the bounding box of moved blocks, grow it by one in the push direction; if the piston's own claim fully contains the box, allow with no per-block work.
- **Fluids:** flow straight down is always allowed; otherwise source claim and destination claim must match.
- **It has no `BlockRedstoneEvent` listener and no `InventoryMoveItemEvent` listener at all.** Our redstone handler is extra protection GP does not have.
- **Two GP bugs not to reproduce:** its burn handler compares a claim's owner with itself (`BlockEventHandler.java:1016`), and its cauldron handler checks the player's position instead of the cauldron.

---

## Root cause of Problem 1

`PlotUtils.getPlotIdByLocation` (`M/utils/PlotUtils.java:24-40`) runs `SELECT id FROM plots`, then **two more queries per plot** (`getPlotPosMin`, `getPlotPosMax`), then falls back to grid arithmetic. With N registered plots that is 1 + 2N queries per lookup, each a fresh `prepareStatement`.

Callers on hot paths:

| Caller | Lookups per event |
|---|---|
| `MarketRegionProtection.onBlockRedstone` (`:612`) | up to 7 (block + 6 neighbours) |
| `MarketWorldListener.onPlayerMove` (`:60`) | 1 per block moved, per player in the market world |
| `PlotUtils.canPlayerInteract` (`:295`) | 1, plus an owner query, a members query and a `config.getInt` |
| `ShopInteraction.onChestInteraction` (`:58`) | 1 per barrel click, plus `getShopAt` |
| Every other handler in `MarketRegionProtection` | 1 to N (explosions and structure growth do one per block) |

Related costs found in the same audit:

- `DatabaseManager.getShopAt` (`:630-639`) scans every shop and builds a serialized location string per shop, per call.
- `Market.getMarketWorld()` (`Market.java:130`) reads config and does a world lookup by name on every call; it is called from nearly every handler through `MarketUtils.isInMarketArea`.
- `allowed-interact-blocks` is read with `getStringList` on every right-click (`MarketRegionProtection.java:107`).

---

## Phase 1: Baseline build and test harness

**Why first:** nothing here changes behaviour, and every later phase needs a way to prove it did not break anything.

Tasks:

1. Run `mvn -q package -DskipTests` on the untouched code. Record whether it succeeds. If it fails, fix only what is needed to compile and note it.
1a. Bump `spigot-api` to the 26.3 version (D9), set `api-version: '1.21'` (D6), add the `spigot-1.21` compile profile. Fix compile errors from the bump only. Commit separately.
2. Add JUnit 5 to `pom.xml` (test scope) and the surefire plugin. Update `maven-compiler-plugin` from 3.8.1 to a current version and use `<release>21</release>`.
3. Extract the grid arithmetic from `PlotUtils.getAutomaticPlotIdAt` (`:42-75`), `registerAutomaticPlot` (`:77-101`), `isSpawnPlot` (`:153-164`) and `isSpawnLocation` (`:166-181`) into a class with **no Bukkit types**: `M/plot/PlotGrid.java`, constructed from `(plotSize, pathwayWidth, spawnRadius, maxHeight)`, working on plain ints. `PlotUtils` delegates to it. Logic must be moved, not rewritten.
4. Write characterization tests `src/test/java/.../PlotGridTest.java`:
   - Keep a verbatim private copy of the old arithmetic inside the test as the oracle.
   - Sweep x and z from -200 to 200 for at least these `(plotSize, pathwayWidth)` pairs: (16,3), (16,4), (10,5), (32,1), (7,2).
   - Assert the new class returns the same id (or null) as the oracle at every coordinate.
   - Assert every block inside the bounds computed by `registerAutomaticPlot` for plot `(px,pz)` maps back to `plot_px_pz`, and every block just outside does not.
5. Add a `docs/TESTING.md` with the exact Docker commands used for the smoke test (see Phase 8) once they are confirmed to work.

Verification:

- `mvn test` passes.
- `git diff --stat` shows only pom, the new class, the delegating lines in `PlotUtils`, and tests.

Guard: do not "fix" the arithmetic even if it looks odd for negative coordinates. The oracle test exists to keep it identical.

---

## Phase 2: In-memory plot index and cached settings

**This phase alone fixes Problem 1.** It is small enough to ship as a hotfix before the rest.

### Design

New package `M/plot/`:

- `Plot`: id, owner `UUID` (nullable), `Set<UUID>` members, int bounds `minX,minY,minZ,maxX,maxY,maxZ`, world name, spawn `Location` (nullable), flag `grid` (true for `plot_x_z`).
- `PlotIndex`: **main-thread only**, plain `HashMap`s, no locking.
  - `Map<String, Plot> byId`
  - `Map<Long, Plot> gridByCell` keyed by `((long) plotX << 32) | (plotZ & 0xFFFFFFFFL)`
  - `Map<Long, List<Plot>> manualByChunk` using the chunk key from Verified facts, each manual plot registered under every chunk it overlaps
  - `Map<UUID, List<Plot>> byOwner`
- `MarketSettings`: an immutable snapshot of everything hot paths read from config: market world name, plot size, pathway width, spawn radius, max height, max plots per player, allowed-interact materials as `EnumSet<Material>` (or `Set<Material>`), shop blacklist as a `Set<Material>`, `allow-flight`, `display-view-range`, `prefix`. Rebuilt on enable and on `/market admin reload`.
- A cached market `World` reference, refreshed on `WorldLoadEvent`, `WorldUnloadEvent`, setup, reset and reload. World checks become a reference comparison: `block.getWorld() == marketWorld`.

Lookup algorithm, `plotIdAt(world, x, y, z)`:

1. If `world != marketWorld`, check manual plots only (they store their own world). In practice return null fast.
2. Manual plots: `manualByChunk.get(key(x >> 4, z >> 4))`, test 3D bounds for each. Return the first hit.
3. Grid: `PlotGrid` arithmetic. Return `plot_<x>_<z>` or null for pathway. Y is ignored for grid plots, which matches current behaviour (the SQL bounds check fails outside y 64..maxHeight, then the arithmetic fallback returns the same id anyway).

To avoid building strings on hot paths, also expose `Plot plotAt(...)` and a `long zoneKeyAt(...)` that returns a stable numeric identity (grid cell key, a manual plot's identity, or a sentinel for pathway). Handlers that only compare "same plot or not" use `zoneKeyAt`.

One deliberate, tiny difference from today: when a manual plot overlaps a grid plot, manual wins. Today the winner depends on SQL row order. `RegionSelection.java:96-120` already rejects overlapping selections, so this should not occur in real data. Note it in the PR.

### Tasks

1. Add `DatabaseManager.loadAllPlots()`: one `SELECT * FROM plots` and one `SELECT * FROM plot_members`, called once in `initialize` after migration. Build the index.
2. Rewrite every read in `PlotUtils` to use the index: `getPlotIdByLocation`, `getPlotOwner`, `getPlotIdByPlayer`, `getPlotCount`, `getPlotPosMin/Max`, `getPlotSpawn`, `getPlotCenter`, `isPlayerInOwnPlot`, `canPlayerInteract`, `getActivePlotIds`, `getAvailablePlotIds`. Keep method names and signatures so callers do not change in this phase.
3. Every write updates the index first, then SQL: `savePlot`, `setPlotOwner`, `setPlotSpawn`, `deletePlot`, `addPlotMember`, `removePlotMember`, `clearPlotMembers`, `resetAllPlots`, `clearAllData`. SQL stays synchronous in this phase; it moves off-thread in Phase 4. (Writes are rare: claim, unclaim, member changes.)
4. Replace `Market.getMarketWorld()` internals with the cached reference. Replace `MarketUtils.isInMarketArea` internals with the reference comparison.
5. Replace per-event config reads in `MarketRegionProtection.java:107`, `Movement.java:49,79`, `MarketWorldListener.java:30` and `PlotUtils` with `MarketSettings`.
6. Redstone handler fast path (`MarketRegionProtection.java:611-636`), same outcome as today:
   - Return if not market world or the material is not in `REDSTONE_COMPONENTS` (unchanged).
   - Compute the block's zone. If the block is a grid-plot **interior** block (its local x and z are not on the plot's first or last row), all four horizontal neighbours are in the same plot and up/down are the same plot, so return with no further work.
   - Otherwise compare the six neighbours as today and `setNewCurrent(getOldCurrent())` on a mismatch.
   - Manual plots take the six-neighbour path always.
   - Do not allocate `Location` objects or a `BlockFace[]` per event; use a static array and block coordinates.
7. `MarketWorldListener.onPlayerMove`: use the index, cache owner display names in a `Map<UUID, String>` filled on join and on first lookup instead of calling `Bukkit.getOfflinePlayer(uuid).getName()` per plot change. Clear `playerPlotCache` on quit.

### Tests

- `PlotIndexTest` (pure Java): manual plot found inside bounds and not outside, including y; manual plot spanning several chunks; manual wins over grid; owner and member changes reflected; delete removes from every map; negative chunk coordinates do not collide.
- Redstone rule test: for a grid of coordinates, the fast-path decision equals the six-neighbour decision.

### Verification

- `grep -n "DatabaseManager\." M/utils/PlotUtils.java` shows only write calls.
- `grep -rn "getPlotPosMin\|getPlotPosMax\|getAllPlotIds" M/` shows no call from a listener.
- Boot in Docker, build a redstone clock in the market world, run spark for 60 seconds: `DatabaseManager` must not appear under `BlockRedstoneEvent` or `PlayerMoveEvent`. Compare the plugin's share with the reported 21.6%.

Guards:

- Do not touch `PlotIndex` from async code. `PlayerJoinListener` runs async; it must not call `PlotUtils`.
- Do not change the redstone rule's outcome. A component on a plot edge next to a pathway is frozen today; that stays.

---

## Phase 3: Protection core (Problem 2)

**Approach:** copy GriefPrevention's design, not its code, and do not depend on it. Reasons for no dependency: GP claims are per-player and 2D with their own trust commands, tools and messages; mirroring plots into GP claims creates two sources of truth; GP would conflict with the barrel-shop click rules in `ShopInteraction`.

### Design

One class, `M/protection/PlotProtection.java`, with two rules. Every listener method becomes a few lines that call one of them.

- **Actor rule** `boolean canBuild(Player, Block)` / `canBuild(Player, Location)`: exactly today's `PlotUtils.canPlayerInteract` logic: bypass list allows; outside market world allows; above max height denies; no plot (pathway/spawn) denies; owner or member allows; else deny.
- **Boundary rule** `boolean sameZone(Block a, Block b)`: both in the same plot, or both in no plot. Exactly today's `Objects.equals(plotA, plotB)`.

Listener classes, split by kind so each stays readable:

- `PlayerProtectionListener` (actor rule)
- `WorldProtectionListener` (boundary rule: pistons, fluids, fire, spread, growth, explosions, dispensers, sponges, falling blocks, redstone)
- `EntityProtectionListener` (entities, vehicles, hanging, projectiles)

`ShopProtectionListener` logic (remove a shop when its barrel is destroyed or moved) is folded into the same explosion, burn and piston handlers so those events are processed once.

### Existing behaviour to preserve exactly

Source of truth: `M/Listeners/MarketRegionProtection.java`. Every rule below must behave the same after the rewrite.

| # | Event | Current rule |
|---|---|---|
| 1 | `PlayerInteractEvent` | Wind charge use denied in market unless bypass. Right-click on an `allowed-interact-blocks` material always allowed. Barrel inside any plot passes (ShopInteraction decides). Shop location passes. Otherwise actor rule on the clicked block. |
| 2 | `PlayerInteractEntityEvent`, `EntityDamageByEntityEvent`, `HangingBreakByEntityEvent` | For `Hanging` and `ArmorStand`: actor rule at the entity, attributing projectiles to their shooter. |
| 3 | `BlockBreakEvent`, `BlockPlaceEvent` | Actor rule. |
| 4 | `EntityExplodeEvent`, `BlockExplodeEvent` | Source in pathway: clear the block list. Otherwise remove blocks outside the source's plot. |
| 5 | `BlockPistonExtendEvent` / `RetractEvent` | Cancel if any moved block or its destination is outside the piston's plot. |
| 6 | `BlockBurnEvent` | Cancel if the igniting block is unknown or in a different zone. |
| 7 | `BlockIgniteEvent` | SPREAD and LAVA: cancel if source unknown or in a different zone. LIGHTNING: cancel outside plots. |
| 8 | `BlockFromToEvent` | Boundary rule. |
| 9 | `ProjectileHitEvent` (wind charge) | Remove the charge and cancel when it lands outside the shooter's plot; dispenser-fired charges removed when landing in a pathway. |
| 10 | `BlockSpreadEvent` | Boundary rule (source to block). |
| 11 | `EntityChangeBlockEvent` | Players ignored. Falling blocks: cancel if origin and landing zones differ. Every other entity: cancel. |
| 12 | `StructureGrowEvent` | Remove grown blocks outside the source's plot. |
| 13 | `BlockFadeEvent` | Always cancelled in the market world. |
| 14 | `BlockFormEvent` | Entity-caused only: player needs the actor rule; other entities cancelled. Non-entity forms allowed. |
| 15 | `SpongeAbsorbEvent` | Remove absorbed blocks outside the sponge's plot. |
| 16 | `BlockDispenseEvent` | Cancel if the block the dispenser faces is in a different zone. |
| 17 | `BlockRedstoneEvent` | Phase 2 rule. |
| 18 | `LightningStrikeEvent` | Weather-caused strikes cancelled. |
| 19 | `ProjectileLaunchEvent` | Player-shot wind charges cancelled in market unless bypass. |

Duplicates to merge without changing outcomes: two `EntityExplodeEvent` handlers (`:209`, `:421`), two `EntityChangeBlockEvent` handlers (`:489`, `:644`), plus the five in `ShopProtectionListener`.

Performance changes inside the same rules:

- Explosions, structure growth, sponge: compute the source zone once, compare by `zoneKeyAt`, never build id strings per block.
- Pistons: bounding-box shortcut. If the box of moved blocks grown by one in the push direction lies inside the piston's plot, allow immediately.
- Fluids: if the source is a grid-plot interior block, allow immediately.
- No `Location` allocation in world-cause handlers; use `Block` coordinates.

### New coverage (needs D3)

These close holes that exist today. All events are confirmed in Verified facts.

| Hole | Event | Rule |
|---|---|---|
| Bucket placed from own plot into a neighbour; filling from another plot | `PlayerBucketEmptyEvent`, `PlayerBucketFillEvent` | Actor rule at `getBlock()` |
| Fish/axolotl scooped from another plot | `PlayerBucketEntityEvent` | Actor rule at the entity |
| Boats, minecarts, armor stands, end crystals placed | `EntityPlaceEvent` | Actor rule at `getBlock()`; no player means cancel |
| Paintings and item frames placed | `HangingPlaceEvent` | Actor rule; no player means boundary rule against the source block if known, else cancel |
| Any non-player entity interacted with or damaged, not only item frames and armor stands | `PlayerInteractEntityEvent`, `PlayerInteractAtEntityEvent`, `EntityDamageByEntityEvent` | Actor rule at the entity. Players as victims are untouched (PvP stays with WorldGuard, D4). |
| Armor stand item swap | `PlayerArmorStandManipulateEvent` | Actor rule |
| Lectern book taken | `PlayerTakeLecternBookEvent` | Actor rule |
| Sign edited | `SignChangeEvent` | Actor rule |
| Bone meal growing across a border | `BlockFertilizeEvent` | Remove states outside the source zone; player present means actor rule per affected plot |
| Bed or door straddling a border | `BlockMultiPlaceEvent` | Actor rule on every replaced state |
| Vehicles broken | `VehicleDamageEvent`, `VehicleDestroyEvent` | Player attacker: actor rule. Other attacker: cancel |
| TNT primed by a flaming arrow or another plot's redstone | `org.bukkit.event.block.TNTPrimeEvent` | Player/projectile shooter: actor rule. Block source: boundary rule |
| Projectiles breaking or triggering blocks (decorated pots, chorus flowers, targets, bells) | `ProjectileHitEvent` with `getHitBlock()` | Player shooter: actor rule at the hit block, cancel the hit. Other shooter: cancel when the hit block is in a plot different from the launch zone |
| Ender pearl or chorus fruit into someone's plot | `PlayerTeleportEvent` with those causes | **Decide during implementation:** plots are shops and visitors must walk in, so probably leave this allowed. Listed for completeness only. |
| Fishing rod pulling armor stands | `PlayerFishEvent` | Actor rule at the caught entity |
| Egg thrown to hatch chickens in another plot | `PlayerEggThrowEvent` | `setHatching(false)` when the actor rule fails |
| Splash potions on entities in other plots | `PotionSplashEvent` | Remove affected non-player entities where the actor rule fails |
| Nether portal lit across a border | `PortalCreateEvent` | Cancel if any block is outside the creator's permitted plot |
| Hoppers and hopper minecarts pulling from a neighbour's container | `InventoryMoveItemEvent` | **Optional, default off, measure first.** This event fires on every hopper transfer and GriefPrevention deliberately does not listen to it. If enabled: compare source and destination zones using `Inventory.getLocation()`; do not call `getHolder()` (it can build a block state snapshot). |

Knockback (D1 = Spigot):

- `org.bukkit.event.entity.EntityKnockbackByEntityEvent` exists in **both** the Spigot API jar and the Paper API jar (checked with `unzip -l` on both). Use it to restore the commented-out wind-charge layer 3 (`MarketRegionProtection.java:447-459`) and to protect armor stands from knockback.
- **Unverified:** whether Paper actually fires the Bukkit version of this event. GriefPrevention registers a separate Paper handler for knockback, which suggests it may not. Check its getters with `javap`, then test on both a Spigot and a Paper server. If Paper does not fire it, layers 1 and 2 of the wind-charge protection still apply, as today.
- Do not add `PlayerFlowerPotManipulateEvent`, `PlayerOpenSignEvent`, `BlockPreDispenseEvent` or `EntityInsideBlockEvent`. They are Paper-only. Flower pots and signs are already covered by the default-deny `PlayerInteractEvent` rule.

Before using any event from this table that is not in "Verified facts" (`PlayerFishEvent`, `PlayerEggThrowEvent`, `PotionSplashEvent`, `PortalCreateEvent`, `PlayerTeleportEvent`), confirm its getters with `javap` against the API jar.

### Implementation notes (2026-11-24)

Not added, on purpose:

- `PlayerTakeLecternBookEvent`: taking a book needs the lectern GUI, which the interact rule already denies.
- Generic `ProjectileHitEvent` on blocks: cancelling it makes arrows fly through walls. Block changes by projectiles (pots, chorus flowers) are already cancelled by the entity-change-block rule.
- `InventoryMoveItemEvent` (hoppers): left out, as GriefPrevention does; placing a hopper already needs build trust.
- `PlayerTeleportEvent` (ender pearls): plots are shops and visitors are welcome.

Found while implementing: the old `PlayerInteractEntityEvent` handler never saw armor stand right-clicks, because `PlayerInteractAtEntityEvent` and `PlayerArmorStandManipulateEvent` have their own handler lists. Both are now handled. Falling blocks now remember where they started (persistent data on the entity) instead of comparing the landing spot with itself.

### Verification

- Unit tests for `PlotProtection.canBuild` and `sameZone` against a fake index: owner, member, stranger, bypass, pathway, above max height, outside market world.
- A table-driven test listing each preserved rule above with a source and a target coordinate and the expected allow/deny.
- Manual in-game checklist `docs/PROTECTION_CHECKLIST.md`, one line per row of both tables, tried as owner, member and stranger. This part needs a real client and cannot be automated.
- `grep -rn "getPlotIdByLocation" M/protection` returns nothing (handlers use `plotAt`/`zoneKeyAt`).

Guards:

- Keep the shop exceptions in rule 1; `ShopInteraction` runs at HIGHEST with `ignoreCancelled=false` and depends on them.
- Shop display entities (`ItemDisplay`, `TextDisplay`) must not be affected by the new entity rules.
- Do not send a denial message from world-cause handlers; only from actor handlers, as today.

---

## Phase 4: Shop index, serialized database access, async writes

### Problems being fixed

- `getShopAt` linear scan with string building (`DatabaseManager.java:630-639`), called on every barrel click, every exploded block, every piston-moved barrel.
- `ShopUtils.getAllShops()` copies the full list twice; called on **every chunk load** in the market world (`DisplayUtils.java:111,124,165`) and from most menus.
- `DisplayUtils.handleChunkUnload` iterates every display per chunk unload.
- One shared `Connection` used from the async join task and the main thread at the same time (`PlayerJoinListener.java:27-90`). `getConnection()` is synchronized but statements run outside the lock.
- Every purchase writes SQL on the main thread (`ShopUtils.java:319,348,402`).
- **Ghost shops:** `deletePlot`, `resetAllPlots` and `clearAllData` never purge `shopCache`. After unclaim, delete or reset, the removed shops stay buyable until restart and money still goes to the old owner. This is a correctness bug that the new index must not inherit; fixing it is part of this phase.

### Design

- Split `DatabaseManager` (951 lines) into:
  - `Database`: connection, pragmas, schema creation, a **single-thread executor**. All SQL after startup goes through it. `flushAndClose()` on disable: stop accepting work, wait for the queue, close.
  - `PlotStore`, `ShopStore`, `PlayerStore`: SQL only, called from the executor.
  - `YamlMigration`: the legacy import (`:120-318`), unchanged in behaviour.
  - Location and item (de)serialization helpers in one place (today duplicated in `ShopUtils.java:512-534` and `DatabaseManager`).
- `ShopIndex` (main-thread only): `byId`, `byBlock` keyed by a packed block position (compute it ourselves from x, y, z; do not use Paper's `getBlockKey`), `byPlot`, `byOwner`, `byChunk`.
- Write pattern everywhere: update memory on the main thread, then submit SQL to the executor. Read pattern for data not held in memory (offline sales, offline earnings, last location): submit to the executor, then hop back to the main thread with the result.
- Keep `static` facades with today's method names during this phase so call sites change as little as possible; remove them in Phase 7.

### Tasks

1. Introduce `Database` and route every existing SQL call through its executor. Startup load stays synchronous.
2. Build `ShopIndex`; replace `getShopAt`, `getShopsByPlot`, `getShopsByOwner`, `getAllShops` internals. Add a non-copying `Collection<ChestShop>` view for iteration.
3. Deleting a plot, resetting plots and clearing data must remove the plot's shops from `ShopIndex` and remove their display entities.
4. `DisplayUtils`: use `byChunk` for chunk load and unload.
5. `PlayerJoinListener`: reads via executor then main thread; no Bukkit calls off-thread.
6. Cache market spawn and last-location lookups that are read on the main thread (`MarketCommandHandler.java:39`, `Back.java:48`).
7. Move `Bukkit.getOfflinePlayer(String)` lookups in `AddMember.java:49`, `RemoveMember.java:49`, `SetOwner.java:55` off the main thread, or resolve from online players and the name cache first.
8. Replace the 31 `printStackTrace()` calls in the database code with the plugin logger. A failed write must log at SEVERE with the operation and key.
9. `ShopCreateMenu.createShop` (`:204-224`): make sure the plot row exists before inserting the shop. The shops table has a foreign key to plots, and admin or spawn-plot shops can target a grid plot that was never registered, which makes the insert fail silently while the menu reports success.

### Implementation notes (2026-11-24)

- `Database` owns the connection and one executor thread; nothing else can reach the connection (no `getConnection()` left). Startup schema and load use `await`; rows are read on the database thread and turned into `Location`/`ItemStack` on the server thread.
- Deviation: SQL stays in `DatabaseManager` instead of separate `PlotStore`/`ShopStore`/`PlayerStore` classes. Each would be a thin file of statements with one caller; the facade is ~470 lines after `YamlMigration` moved out. Revisit in Phase 7 if it grows.
- Deviation: `ShopIndex.byPlot`/`byOwner` scan (few hundred shops, command/menu paths only). `byBlock` and `byChunk` are real maps.
- Last locations and the market spawn are held in memory (loaded at startup), so `/market back` and `/market spawn` never touch SQL.
- Found: `savePlot` used `INSERT OR REPLACE`, which deletes the old row and cascades to members and shops. Now an upsert. Verified on a copy of the dev DB: a plot with 46 shops keeps them all.
- Name lookups (`addmember`, `removemember`, `setowner`) use `PlayerUtils.findKnownPlayer`, which never calls Mojang. Players who never joined are reported as not found, as before.
- Skipped: the main-thread SQL assertion. With one private connection there is no other path to SQL.
- Store round-trip tests need a server (`Location` needs a `World`); covered by the smoke test (boot, load 50 plots / 218 shops, clean stop, `integrity_check` ok) and the in-game checklist.

Trade-off to state in the PR: writes are queued, so a hard crash can lose the last few queued writes. SQLite is already in WAL mode with `synchronous=NORMAL`, so this is close to today's durability. A clean shutdown loses nothing because `onDisable` flushes.

### Verification

- Store round-trip tests against a temporary SQLite file: save, reload, compare, for plots, members, shops, offline sales, last location, market spawn.
- Test: delete plot removes its shops from the index.
- `grep -rn "getConnection()" M/` shows uses only inside `Database` and the stores.
- Thread check: add an assertion helper that throws if SQL runs on the main thread after startup (`Bukkit.isPrimaryThread()`), enabled by a debug flag; boot and exercise the plugin with it on.

---

## Phase 5: Price and metrics file I/O off the main thread

Problems (all in `M/managers/DynamicPriceManager.java` unless noted):

- `recordPurchase`, `recordAdminPurchase`, `recordAdminSale` (`:165-184`) each write the whole metrics YAML. That is a full file write on **every buy or sell click**.
- `getDynamicPrice(String)` (`:66-109`) saves the whole dynamic price file whenever it seeds an item. It is reached from menu rendering (up to 36 items per page) and from `ChestShop.getPrice()` for admin shops, which display updates call.
- `triggerDailyUpdate` (`:186-344`) loops every `Material`, can save a file per newly seen item, and calls `autoScaleShops` per changed item (all shops scanned, one SQL update per match).
- The async timer (`:57-62`) reads a `YamlConfiguration` that the main thread mutates.
- `PriceCalculator.java:31,37` saves `price.yml` once per item during `/market admin calcprices`.
- `PriceData.java:38` uses `getInt`, which truncates fractional prices. `price.yml` has at least 11 fractional entries (for example `ANDESITE: 1.5`). **Fixing this changes prices; it belongs to D2 group B, not this phase.**

Tasks:

1. Keep prices and metrics in memory maps. Mark dirty on change. Save on a timer (for example every 5 minutes) and on disable, writing on the database executor or an async task from a snapshot taken on the main thread.
2. `triggerDailyUpdate`: compute on the main thread from memory, save once at the end. `autoScaleShops`: one pass over shops grouped by item key, batch the SQL updates.
3. The 5-minute timer checks elapsed time against a plain `volatile long`, not the YAML object.
4. `PriceCalculator`: collect results, save once.
5. `SchematicManager`: read the paste settings once per reload instead of per chunk (`:210,216,243-247`); drop the per-chunk INFO logging or lower it to debug.

Implementation notes (2026-11-24):

- Landed inside commit d5804b4 (a `git add` swept it into the A1-A5 commit); not split afterwards.
- Both YAML files stay `YamlConfiguration` objects in memory; the old save calls only mark them dirty. A server-thread timer (every 5 min) runs the daily update when due, then builds the YAML text and writes it on an async task to a temp file that is moved over the old one. `onDisable` writes synchronously.
- Verified on the smoke server: the timer ran the overdue daily update, 2 prices changed, each file was written once, no temp files left, max tick 86 ms for the once-a-day pass.
- Skipped: extracting the pricing maths for a unit test. This phase changed only file I/O; the maths is untouched.

Verification:

- `grep -n "\.save(" M/managers/DynamicPriceManager.java M/files/PriceData.java` shows saves only in the flush method.
- Buy an item 20 times with a file watcher on the plugin data folder: no write per click.
- Dynamic price results for a fixed set of metrics are the same before and after (unit test on the pure calculation, extracted from `triggerDailyUpdate`).

---

## Phase 6: Bug fixes (all approved under D2)

These change behaviour. The owner approved every item in both groups on 2026-11-24.

### Group A: security, duplication, loss, crashes

| # | Bug | Where |
|---|---|---|
| A1 | `/market _buy <plot> <shop> <amount>` can be typed by any player from anywhere. No proximity check, no permission check, amount not validated as positive. `ShopBuyerMenu` also parses the amount from the clicked item's display name, so a shop item renamed to look like a button can trigger a trade. Whether negative amounts are exploitable depends on the economy plugin; not verified. | `InternalCommandListener.java:20-27`, `ShopBuyerMenu.java:155-161`, `ShopUtils.processPurchase/processPlayerSale` |
| A2 | Selling to a shop with a full barrel: leftover items are destroyed and stock is overcounted. | `ShopUtils.java:396-402` |
| A3 | Selling when the owner cannot pay: items are returned to the player but also stay in the barrel. Duplication. | `ShopUtils.java:409-415` |
| A4 | Selling when the barrel block is missing: the player's items are taken, payment proceeds, items vanish. | `ShopUtils.java:390-395` |
| A5 | Buying: items returned to a full barrel on rollback are lost (return value of `addItem` ignored). | `ShopUtils.java:269,287,304` |
| A6 | Ghost shops after unclaim, delete plot and reset. (Index part is fixed in Phase 4; this row covers the leftover barrels and their contents, which stay in the world for the next claimant.) | `Unclaim.java:60-66`, `DeletePlot.java:49-55`, `Reset.java:127` |
| A7 | Shop is deleted when its barrel break is attempted, before protection can cancel the break (handler runs at LOWEST without `ignoreCancelled`). | `ShopInteraction.java:186-218` |
| A8 | `/market admin setspawn` does not check the world. Run outside the market world it saves that location as the market spawn **and overwrites that other world's spawn point**. | `subcommands/admin/SetSpawn.java:39-43` |
| A9 | Teleport result ignored: if another plugin cancels the teleport, the success callback still saves the last location and enables flight. | `utils/TeleportUtils.java:31-33` |
| A10 | After `/market admin reload`, values written by `setup` and `reset` are saved to a stale config object. | `Market.java:137-139`, `Setup.java:55-58`, `Reset.java:132-133` |
| A11 | Console running `/market admin forceupdate` throws `ClassCastException`; `/market admin toggleborder` with no argument throws `ArrayIndexOutOfBounds`. | `ForceUpdate.java:30-37`, `ToggleBorder.java:43` |
| A12 | Border particle tasks keep running after the player quits; several per-player maps are never cleared on quit (bypass mode, selection mode, plot cache, flight map). Bypass surviving a relog may be relied on; confirm before clearing that one. | `RegionUtils.java:38-106`, `Bypass.java:14`, `RegionSelection.java:21-23` |
| A13 | Transaction log rotation deletes the log even when zipping failed. | `TransactionLogUtils.java:90-95` |
| A14 | Shop creation with no known price allows a price of 0. | `ShopCreateMenu.java:46-49` |

### Group B: approved; chosen option noted where there was a choice

Choices made by the planner under "do the recommended thing":

- **B3:** delete the unregistered `/market spawn` class. Bare `/market` already does the same job.
- **B4:** keep the protected spawn zone centred on 0,0 and document it next to `spawn-radius` in `config.yml`. Moving it would shift which plots are claimable on existing servers.
- **B5:** add respawn handling: a player who dies in the market world respawns at the market spawn. Put it behind a config key so server owners can turn it off.
- **B12:** unclaiming a manual plot clears its owner, members and shops but keeps the plot row and bounds, the same way a grid plot stays claimable.
- **B14:** remove the `TRANSACTION_FLOW` mention from `config.yml` rather than implement a second pricing model.

| # | Item | Where |
|---|---|---|
| B1 | Market spawn saved with the world name: after changing the market world, the old row points at the old world or silently falls back. | `DatabaseManager.java:918-939`, `MarketWorldManager.java:39-46` |
| B2 | Default spawn is a fixed `0, 65, 0` with no safe-spot check. The floor is y=64 and border blocks sit at y=65. | `MarketWorldManager.java:46` |
| B3 | `/market spawn` exists in code but is not registered. Register it or delete it. | `subcommands/player/Spawn.java`, `Market.java:112-115` |
| B4 | The protected "Market Spawn" zone is always centred on 0,0 regardless of `setspawn`. | `PlotUtils.java:166-181` |
| B5 | Dying in the market world respawns the player at the server's main spawn; the plugin has no respawn handling. Adding it is a new feature. | none |
| B6 | Fractional prices in `price.yml` are truncated to integers. | `PriceData.java:38` |
| B7 | Price trend indicator never resets for items whose price stopped changing. | `DynamicPriceManager.java:271-276,316-322` |
| B8 | `autoScaleShops` rescales player buying shops too and never re-clamps to the 0.85x to 1.5x range. | `DynamicPriceManager.java:346-354` |
| B9 | Global stock and "cheapest" figures in menus count admin shops (9999 each) and buying shops. | `AllItemsMenu.java:58-66`, `PlotsSellingItemMenu.java:36,178` |
| B10 | Menu clicks are matched back to items by display name; colour-named items do not match and the click does nothing. | `AllItemsMenu.java:154-165`, `ShopItemListMenu.java:150-157` |
| B11 | Barrel clicks have no hand check, so both hands may fire; the handler also un-cancels other plugins' cancels. | `ShopInteraction.java:105,131` |
| B12 | Unclaiming a manual plot deletes its bounds permanently, so the plot ceases to exist. | `Unclaim.java:60-66` |
| B13 | `plugin.yml` declares `market.admin.*` child permissions that the code never checks, and does not declare `market.plots.limit.*`, which it does check. | `plugin.yml`, `PlotUtils.java:134,144` |
| B14 | `currency-exchange.pricing-model: TRANSACTION_FLOW` is documented in `config.yml:64` but not implemented. | `DynamicPriceManager.java:230` |
| B15 | Help text and usage strings are stale (`/amarket ...`, `/market back` instead of `leave`). | `HelpCommand.java:39`, several subcommands |

Each fix gets its own commit and, where the logic is testable without a server, a unit test.

Implementation notes (2026-11-24): fixes are grouped by area in seven commits (d5804b4 trades, 6b1adb2 shop removal, 920a5e1 spawn, 5cf1cfd console/quit/logs/zero price, d5313a1 fractional prices, 152748b prices and menus, 2130af3 permissions and text). A10 was already fixed in Phase 2. Choices: `/market _buy` and `/market _removeshop` had no sender in the code and were removed rather than guarded; unclaim is refused while shop barrels hold items (A6) instead of moving or deleting the items; trades require the player within 8 blocks of the barrel. None of these paths is unit-testable without a server; they are in the in-game checklist.

---

## Phase 7: Cleanup

No behaviour change. Do after everything above is merged.

- **Dead code (grep-verified, zero callers):** `files/RegionData.java`, `files/LocationData.java`, `menus/ShopListMenu.java`, `menus/ShopItemListMenu.java`, `subcommands/player/Spawn.java` (unless B3 registers it), `ShopUtils.getAllShopItemsAndCountsByPlotID`, `getPlotShopsByItemName`, `getAllShopsByItemName`, `PriceData.getTotalItemPrice`, `EconomyUtils.getEconomy`, `LocationUtils.saveLocation`, `MarketUtils.getAvailablePlotCount`, `isMarketRegionLoaded`, `DisplayUtils.getMarketDisplays`, `getDisplayPair`, `DatabaseManager.getPlotIdOfShop`, `getShopStock(String)`, `PlotUtils.teleportToPlotSpawn`, `RegionSelection.RegionBounds.contains`, the commented block at `MarketDisplayListener.java:60-87`. Re-run the grep before deleting each one.
- **Duplication to fold into one helper each:**
  - `RegionUtils.visibleRegionBorders` is the same 30 lines twice (`:38-65`, `:67-106`).
  - "5 second teleport, then save last location if outside market" appears in six places (`MarketCommandHandler.java:44-53`, `PlotTeleport.java:110-117`, `PlotUtils.java:271-278`, `PlotsSellingItemMenu.java:128-147`, `MarketManagementMenu.java:129-146`).
  - Pagination controls re-implemented in each paged menu.
  - Owner-or-admin shop permission check in five places.
  - "standing in own plot" check in four subcommands.
- **Style:** replace inline fully qualified names with imports; `Bukkit.getLogger()` to `plugin.getLogger()`; make `Bypass.bypassPlayers` private behind methods; remove the `static` facades left from Phase 4; remove unnecessary `runTask` calls that are already on the main thread (`ShopUtils.java:352,425`).
- **Small costs:** `PlotUtils.getPlotLimit` does up to 100 permission checks per call; `TeleportUtils.handleMovement` calls `getBlock()` twice per move event for every player on the server (compare block coordinates, and return early when no teleport is pending); `SearchListener` uses a plain `HashSet` from the async chat thread.
- **Build and metadata:** relocate the shaded SimpAPI package to avoid clashes with other plugins; set the correct `api-version` once confirmed; update the README version badge; add missing default keys to `config.yml` (`market-world.max-height`, and the orphan comment at `:17-19`); remove `join-message` if it stays unused.
- **Optional, cosmetic:** rename packages `Listeners` and `Entities` to lower case. Pure rename, large diff; do it last or skip it.
- `MenuCleanupListener.java:19-39` reflects into a private SimpAPI field. Leave it, but wrap it so a library change logs once instead of throwing on every quit.

### Implementation notes (2026-11-24)

Done: dead code (re-grepped; `ShopListMenu` and `ShopItemListMenu` were reachable from nothing), the teleport-then-remember-origin helper (`TeleportUtils.teleportFromOrigin`, five callers), the border-particle duplicate, `handleMovement` without `getBlock()`, a concurrent set in `SearchListener`, `MenuCleanupListener` logging once, `ShopProtectionListener` moved into `protection/`.

Build: SimpAPI is relocated to `net.craftnepal.market.lib.simpapi`. The SimpAPI jar is itself a fat jar carrying Jackson, SnakeYAML and JetBrains annotations for its config package, which this plugin does not use; the shade filter keeps only SimpAPI's own classes. Jar size 2.7 MB → 294 KB. Shade plugin 3.2.4 → 3.6.0 (relocation rewrites Java 21 class files).

Found: `/market admin ...` did nothing from the console because `NestedCommand` returned for non-players. Every subcommand already checks for a player itself, so the console can now run forceupdate, reload, syncstock and calcprices. Found: `/market leave` removed the player from a copy of the flight map.

Skipped as low value for the diff size: replacing fully qualified names with imports, `Bukkit.getLogger()` → plugin logger, making `Bypass.bypassPlayers` private, folding pagination and owner-or-admin checks, the lower-case package rename, and removing the `DatabaseManager` static facade (it is the storage API now, not a leftover).

---

## Phase 8: Final verification

1. **Unit tests:** `mvn test` green. Suites: `PlotGridTest`, `PlotIndexTest`, protection rule tests, store round-trip tests, price calculation test.
2. **Docker smoke test** (existing `docker-compose.yml`, Paper 26.1.2):
   - `mvn package`, start the container, wait for the "Done" line.
   - Log must contain no exception and no `Could not pass event` line mentioning Market.
   - Through the console: run setup, confirm the world is created, restart, confirm plots and shops reload.
   - Boot once against a copy of a real `market.db` from the live server to prove existing data loads unchanged.
3. **Lag regression for Problem 1:** build a redstone clock in the market world from the console, run spark for 60 seconds, confirm no `DatabaseManager` frames under any event and compare the plugin's share with the reported 21.6%.
4. **Grep checks:**
   - No SQL from listeners: `grep -rn "prepareStatement\|executeQuery\|executeUpdate" M/` only inside the store classes.
   - No hot-path config reads: `grep -rn "getMainConfig()" M/Listeners M/protection` returns nothing.
   - No invented API: every event class imported under `M/protection` appears in "Verified facts" or was checked with `javap`.
5. **Protection checklist:** `docs/PROTECTION_CHECKLIST.md` walked in game as owner, member and stranger. This step needs a human with a client.
6. **Feature checklist:** Appendix A walked once.

### Who runs what

- Small, fast model: builds, test runs, log scanning, grep checks, Docker boot.
- Mid-size model: writing unit tests, reviewing each phase's diff against Appendix A.
- Largest model: `PlotIndex`, `PlotProtection` and the transaction fixes in Phase 6 group A.

---

## Phase 9: Public release readiness

Added after the owner said the plugin is to be released publicly. Do this after Phase 8. D6 to D9 are answered in the Decisions section.

What a stranger's server needs that the owner's server does not:

1. **Works without WorldGuard.** Today `config.yml:15` and the code comments say WorldGuard and Paper handle PvP, mob spawning and hunger in the market world. Add an opt-in `market-world.rules` config section, every key off by default so the owner's server is unchanged: no PvP, no mob spawning, no hunger, no fall damage. Implement with plain Bukkit events (`EntityDamageEvent`, `CreatureSpawnEvent`, `FoodLevelChangeEvent`), not game rules, because game rule constant names differ between Spigot and Paper builds.
2. **Works without WorldEdit.** `plugin.yml` lists it as a soft dependency. Boot and run setup on a server with no WorldEdit and confirm nothing throws; pathways simply are not pasted.
3. **Works on Spigot and Paper**, on 1.21 and on 26.3 (D6, D9). Boot-test all four combinations; the Docker image supports `TYPE: SPIGOT` and `TYPE: PAPER`.
4. **Sensible defaults.** Permission defaults per D7. Every config key the code reads has an entry and a comment in `config.yml` (today `market-world.max-height` and others are read but absent).
5. **Player-facing text.** Messages are hard-coded in English across commands, menus and listeners. Minimum for release: one consistent prefix and tone, no debug lines to players, no stale command names. A `messages.yml` for translation is optional; skip unless asked.
6. **No console noise.** Remove per-chunk INFO logging (`SchematicManager.java:121,219,225`), the debug line in `Back.java:47`, and stray `printStackTrace` calls.
7. **Safe upgrades.** Store a schema version in `global_metadata`. The legacy YAML import stays.
8. **Licence and docs.** Add a `LICENSE` file matching the README badge (MIT, if that is still the intent). Rewrite the README from Appendix A: requirements (Vault plus an economy plugin, optional WorldEdit), supported versions, setup steps, full command and permission tables, config reference, known limits (no Folia).
9. **Versioning and build.** Bump to 2.0.0 (storage internals and permission defaults change). Remove the commented Windows path from `pom.xml`. `.github/workflows/build.yml` and `release.yml` already exist: read them, make sure they run `mvn test` and attach the shaded jar to a release.
10. **Listing.** Prepare the description for SpigotMC and Modrinth from the README. Publishing is a manual step for the owner.

Verification:

- Fresh server with only Vault, an economy plugin and Market: a non-op player can teleport in, claim a plot, create a shop and trade, with no other plugin installed.
- Same flow on Spigot and on Paper, oldest and newest supported version.
- Upgrade test: drop the new jar onto a copy of the owner's live data folder; everything loads and nothing in `config.yml` is lost.

---

### Phase 8 and 9 results (2026-11-25)

Automated, all passing:

- Unit tests: 23 (PlotGrid, PlotIndex, Zones, ShopIndex). Store round-trip and price tests were not written; the production upgrade test below covers loading.
- Lag: 13 observer clocks on a plot border, 0.7 ms per tick and TPS 20 (1.1.4: 56 ms, TPS 16.6).
- Independent review (separate model, read-only) found that shops were read before the market world loaded, an over-eager payment rollback, and out-of-order price file writes. All fixed. Pre-existing and left: if the owner's deposit fails, the buyer is refunded and keeps the items.
- Fresh server with Vault + EssentialsX + Market and no WorldEdit, setup from the console, price update: Paper 26.3, Paper 1.21.1, Spigot 1.21.1 (needs a Java 21 image), Spigot 26.3. This found that Market never started without WorldEdit (also in 1.1.4); WorldEdit calls now live in `WorldEditPaster`.
- Production upgrade (copy of the live backup, Paper 26.3, 1.1.4 → 2.0.0): 131 plots and 1981 shops load; after a clean stop the database passes `integrity_check` and plot, shop, member and offline-sale counts are unchanged; config.yml keeps every value and only gains the new keys. Idle tick 0.1 ms; daily price update one 61 ms tick. `syncstock` froze the server 1.4 s and now runs 5 ms per tick (peak 118 ms, TPS 20, 27 s).

Still needs a person with a client: `docs/PROTECTION_CHECKLIST.md` and the trade cases in `docs/TESTING.md`.

---

## Phase 10: Market world environment (2.1.0)

Asked for after 2.0.0: on CraftNepal the market world is kept calm by five WorldGuard global flags (pvp, mob-spawning, creeper-explosion, fire-spread deny; weather-lock clear). 2.0.0 only covers part of that, and its `rules` are off by default. A market world is a shopping hub, not survival land, so the plugin should ship it that way: sensible defaults on, each one switchable, no WorldGuard needed.

Approach: the market world belongs to the plugin, so use the world's own settings where they exist (no work per event), with an event as a backstop. Game rule names differ between 1.21 (`doMobSpawning`) and 26.x (`spawn_mobs`), so they are looked up by name at runtime, never by constant. Settings are applied when the world loads, on reload and after setup.

Config: new section `market-world.environment` replaces 2.0.0's `market-world.rules` (only 2.0.0 had it, released the same day; the old keys are ignored with one warning).

### Environment, on by default

| # | Key (default) | Behaviour | How |
|---|---|---|---|
| E1 | `mob-spawning: false` | No mob spawns on its own: no monsters, no animals, no phantoms, patrols, wandering traders, raids, slime splits, lightning conversions. Players may still use spawn eggs and buckets in their own plot, breed animals and build golems. | spawning game rules off + `CreatureSpawnEvent` allow-list (SPAWNER_EGG, DISPENSE_EGG, BUCKET, BREEDING, BUILD_SNOWMAN/IRONGOLEM/COPPERGOLEM, SHOULDER_ENTITY, COMMAND, CUSTOM); `RaidTriggerEvent` cancelled |
| E2 | `hostile-mobs: false` | No hostile mob by any player route either: no zombie from an egg, no wither built, no mob spawner or trial spawner. Commands and plugins still can. | `Enemy` check in the same handler |
| E3 | `pvp: false` | Players cannot hurt each other by any means (melee, arrows, potions, TNT). | `World.setPVP(false)` (exists in 1.21 and 26.3); the 2.0.0 handler stays as backstop |
| E4 | `explosions: off` | `off`: TNT, creepers, crystals, beds, anchors do nothing in the market world (no blocks, no damage, no item frames). `contained`: 2.0.0 behaviour, damage stays in its own plot. `vanilla`: protection only. | cancel `ExplosionPrimeEvent`/`EntityExplodeEvent`/`BlockExplodeEvent` |
| E5 | `fire-spread: false` | Fire never spreads, never burns blocks away, lava never lights anything. Fire a player lights on their own plot stays as decoration. | `BlockSpreadEvent` (fire), `BlockBurnEvent`, `BlockIgniteEvent` SPREAD/LAVA |
| E6 | `weather: clear` | Always clear: no rain on stalls, no lightning, no snow piling up. `cycle` for vanilla. | weather game rule off + clear on load + cancel `WeatherChangeEvent`/`ThunderChangeEvent` |
| E7 | `hunger: false` | Food does not drop while shopping. | `FoodLevelChangeEvent` (2.0.0 rule) |
| E8 | `fall-damage: false` | Flight is allowed in the market; landing after toggling it off should not hurt. | 2.0.0 rule |
| E9 | `void-rescue: true` | Falling into the void puts the player back at the market spawn instead of killing them and dropping their goods. | `EntityDamageEvent` cause VOID → teleport |

Optional, off by default:

| # | Key (default) | Behaviour |
|---|---|---|
| E10 | `time: cycle` | `day` locks the market at noon (daylight game rule off, time set once). |
| E11 | `max-mobs-per-plot: 24` | Breeding, eggs and buckets refused once a plot holds this many living mobs. Stops a plot from becoming an animal farm that lags the market. 0 = no cap. Counted only when such a spawn happens, from the plot's chunks. |

### Clean-up and admin

- C1 Existing worlds already hold mobs spawned under 1.x. When a market chunk loads, remove hostile mobs that are not named or persistent (when E2 is on).
- C2 `/market admin clearmobs <hostile|animals|all>`: one-off clean-up. Never removes named, tamed, leashed or ridden mobs, villagers with trades in progress, or anything not a mob.
- C3 `/market admin environment`: prints the effective settings for the market world, so an admin can see what is on without reading the config.

### Not doing (reasons)

- Blocking ender pearls and chorus fruit into other plots: plots are public storefronts; walking in is the point.
- Keeping mobs inside their plot: fencing is the owner's job, and with E1/E11 few mobs exist.
- Keep-inventory: with E1 to E9 there is almost no way to die in the market.
- `messages.yml` translation: unchanged from Phase 9.

### Tests

- Unit: spawn-reason allow-list and the setting parser (pure functions).
- Docker, fresh world on Paper 26.3 and Spigot 1.21.1 (game rule names differ): after 3 minutes no natural mobs exist; `summon` (COMMAND) still works; a lit TNT leaves the blocks; fire placed next to planks does not spread; `weather rain` reverts to clear; game rules read back as set.
- Production copy: the five WorldGuard flags are matched with WorldGuard removed from the copy.

Version 2.1.0. README, config comments and listing updated; the README's "leave these off if WorldGuard..." note goes.

---

## Appendix A: feature inventory (the contract)

### Commands

| Command | Permission | Behaviour |
|---|---|---|
| `/market` | `market.use` | 5 second no-move teleport to market spawn (saved spawn, else world spawn); saves last location if coming from outside; sound; enables flight if `allow-flight`. Shows a setup hint when no market world exists. |
| `/market help` | | Lists commands. |
| `/market leave` | `market.back` | Only inside the market world. Instant teleport to the saved last location, flight off, saved location cleared. |
| `/market shops` | `market.shops` | Opens the main shop menu. |
| `/market plot claim` | `market.plot.claim` | Claims the plot stood in. Refused in spawn plots. Limit from `market-world.max-plots-per-player`, overridden by `market.plots.limit.<n>` and `market.plots.limit.unlimited`. |
| `/market plot spawn` | `market.plot.teleport` | 5 second teleport to the first owned plot. |
| `/market plot visit <plotId or player>` | `market.plot.teleport` | 5 second teleport to plot spawn or centre, then shows borders briefly. |
| `/market plot setspawn` | `market.plot.setspawn` | Must stand in own plot. |
| `/market plot addmember <player>`, `removemember <player>` | `market.plot.addmember`, `market.plot.removemember` | Owner standing in the plot. Members can build and restock. |
| `/market plot unclaim` | `market.plot.unclaim` | Deletes the plot and its shops. No confirmation. |
| `/market plot info` | `market.plot.info` | Owner, shop count, members; internal id for admins. |
| `/market plot manage` | `market.use` | Opens the management menu. |
| `/market admin bypass` | `market.admin` | Toggles protection bypass and admin-shop mode (shops created are infinite). |
| `/market admin deleteplot <id>`, `listplots`, `reload`, `setowner <plotId> <player>`, `setspawn`, `forceupdate`, `calcprices`, `syncstock` | `market.admin` | As named. |
| `/market admin selectionmode plot` | `market.admin` | Toggle; with a stick, right-click two blocks to create a `manual_<millis>` plot. |
| `/market admin toggleborder plot [id]` | `market.admin` | Particle border. |
| `/market admin setup <world> <plotSize> <pathwayWidth>` | `market.admin` | Creates the market world. |
| `/market admin reset confirm` | `market.admin` | Deletes the world and all data. |
| `/market _buy ...`, `/market _removeshop ...` | none / owner or admin | Internal, intercepted before command dispatch. See bug A1. |

Tab completion hides `admin` from players without `market.admin`.

### Barrel interaction

- Sneaking: passes through to normal Minecraft behaviour if allowed.
- Shop barrel, right-click: owner or bypass opens the barrel to restock; others open the buy/sell menu.
- Shop barrel, left-click: owner or bypass opens the shop admin menu; others get a price message.
- Plain barrel, left-click by plot owner, bypass, or admin in a spawn plot: opens the create-shop menu. Held item must not be blacklisted unless bypass.
- Sneak-break by owner or bypass removes the shop; non-sneaking break is blocked with a hint.
- Closing a barrel inventory resyncs that shop's stock and display.

### Menus

- **Main (27):** Browse All Items, Search (chat prompt, `cancel` to abort), Close.
- **All items (54):** 36 per page with stock, price and trend; click opens sellers list; paging; back. Supports the search filter.
- **Sellers of an item (54):** 36 plots per page with owner, stock, cheapest price; click teleports (5 seconds) and highlights matching shops with particles.
- **Buy/sell (45):** item info; buttons for 1, 16, 64; close. Title "Sell Item" for buying shops.
- **Shop admin (45):** stats; remove shop; close.
- **Create shop (45):** selling/buying toggle; price buttons -10%, -5%, reset, +5%, +10%, clamped to 0.85x to 1.5x of fair price; confirm; cancel.
- **Management (54):** owner's out-of-stock shops, 28 per page; click teleports and highlights; placeholder "Plot Analytics (Coming Soon)".

### Other behaviour

- Action bar on entering a plot, pathway or spawn area: owner's name, "Unclaimed Plot", "Market Pathway", "Market Spawn".
- Flight granted inside the market world and removed on leaving, when `allow-flight` is true.
- Floating item and text display above each shop; spawned on chunk load, removed on unload and on disable.
- On join: offline sales summary with itemised hover, offline earnings paid, low-stock reminder.
- Schematic pathways pasted as market chunks load (needs WorldEdit).
- Dynamic pricing: check every 5 minutes, daily update after 24 hours; admin shop prices; optional currency exchange with CPI peg.
- Transaction log with rotation at 5 MB.
- One-time import of legacy YAML data, with the old files renamed to `.bak`.

### Config keys

`prefix`, `allow-flight`, `display-view-range`, `player-shop-blacklist`, `market-world.{name, plot-size, pathway-width, spawn-radius, max-height, max-plots-per-player, allowed-interact-blocks, pathway-schematic, pathway-y-level, floor-material, pathway-material, border-material}`, `admin-shops.{buy-price-multiplier, max-price-change-percent, min-price-multiplier, max-price-multiplier}`, `currency-exchange.{enabled, currency-item, pricing-model, min-price-multiplier, max-price-multiplier, cpi-settings.basket-items}`. Data files: `price.yml`, `dynamic_prices.yml`, `market_metrics.yml`, `transactions.log`, `market.db`.

## Appendix B: not verified

- Whether the project compiles as-is (build was not run).
- Paper Maven repository URL and the correct `api-version` string.
- How the installed economy plugin handles negative amounts (bug A1 impact).
- Whether `Location.getChunk()` loads chunks on this server version (`SyncStock.java:63-66`).
- Whether `Bukkit.getOfflinePlayer(uuid).getName()` hits disk on this version.
- Spawn schematic contents at `0, 65, 0` (item B2).
- `price.yml` beyond samples (2085 lines; only head and samples read).
- SimpAPI internals (subcommand permission handling, menu click cancellation).
- Getters of `PlayerFishEvent`, `PlayerEggThrowEvent`, `PotionSplashEvent`, `PortalCreateEvent`, `PlayerTeleportEvent`.
