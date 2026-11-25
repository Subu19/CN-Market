#!/usr/bin/env bash
# Lag test: builds 13 observer clocks on the west edge of plot_1_0 (x=43) in the smoke server.
# Assumes the dev data (plot-size 16, pathway 3). Then: docker exec market-smoke rcon-cli mspt
r() { docker exec market-smoke rcon-cli "$@" >/dev/null; }
r "execute in minecraft:market run forceload add 30 0 60 40"
for z in $(seq 6 2 30); do
  r "execute in minecraft:market run fill 42 69 $z 47 69 $z minecraft:stone"
  r "execute in minecraft:market run setblock 43 70 $z minecraft:redstone_wire"
  r "execute in minecraft:market run setblock 44 70 $z minecraft:redstone_wire"
  r "execute in minecraft:market run setblock 45 70 $z minecraft:observer[facing=east]"
  r "execute in minecraft:market run setblock 46 70 $z minecraft:observer[facing=west]"
done
