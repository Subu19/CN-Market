# Testing

## Unit tests

`mvn test`. Pure logic only, no server: `PlotGridTest`, `PlotIndexTest`, `ZonesTest`, `ShopIndexTest`.
CI runs them on every pull request, and also compiles against the oldest supported API: `mvn -Pspigot-1.21 compile`.

## Smoke test (Docker)

```
mvn package
scripts/smoke-test.sh /tmp/smoke                 # Paper 26.1.2, copy of ./mc-data
scripts/smoke-test.sh /tmp/smoke SPIGOT 1.21.1   # any type and version
docker exec market-smoke rcon-cli "market admin forceupdate"
docker rm -f market-smoke
```

The script fails if the log has an exception mentioning Market. Options are documented at its top.

Fresh server, no WorldEdit (what a new user gets):

```
SEED=none EXTRA_PLUGINS="Vault.jar EssentialsX.jar" WAIT=900 scripts/smoke-test.sh /tmp/fresh PAPER 26.3
docker exec market-smoke rcon-cli "market admin setup market 16 3"
```

Spigot builds itself with BuildTools on first start; give it `WAIT=900`.

## Market world environment

```
EXTRA_PLUGINS="Vault.jar EssentialsX.jar" scripts/environment-test.sh PAPER 26.3
EXTRA_PLUGINS="Vault.jar EssentialsX.jar" scripts/environment-test.sh SPIGOT 1.21.1
```

Checks the game rules (names differ between 1.21 and 26.x), no natural spawns after 150 s, `summon` still
working, TNT and fire doing nothing, and `clearmobs` keeping a named mob. Run both versions after any change to
`MarketEnvironment`: Paper's `GameRule` is an interface and Spigot 1.21's a class, so a direct call to it
breaks one of them.

## Lag regression (redstone)

With the smoke server running on the dev data:

```
scripts/redstone-clocks.sh
sleep 60; docker exec market-smoke rcon-cli mspt
```

Reference: 1.1.4 averaged 56 ms per tick (TPS 16.6); 2.0.0 averages 0.7 ms (TPS 20).

## Upgrade test

Copy (never mount) a production `plugins/Market` folder and the market world into a work dir, add Vault and the
economy plugin, and boot with `SEED=none`. Check that the plot and shop counts in
"Loaded N plots and M shops" match `sqlite3 market.db "select count(*) from plots; select count(*) from shops"`.

## In game (needs a client)

- `docs/PROTECTION_CHECKLIST.md`, as owner, member and stranger.
- Trades: buy and sell with a full inventory, an empty barrel, a full barrel, no money, and the owner offline.
- `/market`, `/market leave`, `/market plot visit`, moving during the 5 second teleport.
