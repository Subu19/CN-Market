#!/usr/bin/env bash
# Checks the market world environment defaults on a fresh server.
#   EXTRA_PLUGINS="Vault.jar EssentialsX.jar" scripts/environment-test.sh PAPER 26.3
# Needs Vault and an economy plugin (EXTRA_PLUGINS). Takes about 5 minutes after the server is up.
cd "$(dirname "$0")/.."
WORK=${WORK:-/tmp/market-envtest}
T=${1:?TYPE}; V=${2:?VERSION}
[ "$V" = 1.21.1 ] && export IMAGE_TAG=java21
SEED=none WAIT=${WAIT:-1200} \
  scripts/smoke-test.sh "$WORK" "$T" "$V" > "$WORK.log" 2>&1 || { tail -5 "$WORK.log"; }
r() { docker exec market-smoke rcon-cli "$@" | sed 's/\x1b\[[0-9;]*m//g; s/§.//g'; }
m() { r "execute in minecraft:market run $*"; }
ok() { echo "  $1: $2"; }
echo "=== $T $V"
r 'market admin setup market 16 3' | tail -1; sleep 15
r 'market admin environment'
if [ "$V" = 1.21.1 ]; then
  for g in doMobSpawning doWeatherCycle doInsomnia doPatrolSpawning disableRaids doDaylightCycle; do ok "gamerule $g" "$(m gamerule $g | tail -1)"; done
else
  for g in spawn_mobs spawn_monsters advance_weather spawn_phantoms raids spawner_blocks_work advance_time; do ok "gamerule $g" "$(m gamerule $g | tail -1)"; done
fi
m 'forceload add 0 0 160 160' >/dev/null
echo "  waiting 150s for natural spawns..."; sleep 150
ok "mobs after 150s (want 0)" "$(m 'execute if entity @e[x=-20,y=-64,z=-20,dx=200,dy=400,dz=200,type=!player,type=!item,type=!item_display,type=!text_display]' )"
m 'summon zombie 50 81 15' >/dev/null
ok "summon zombie by command (want 1)" "$(m 'execute if entity @e[x=-20,y=-64,z=-20,dx=200,dy=400,dz=200,type=zombie]')"
m 'kill @e[x=-20,y=-64,z=-20,dx=200,dy=400,dz=200,type=zombie]' >/dev/null
# TNT: a powered TNT block stays a block; a summoned TNT breaks nothing.
m 'fill 45 79 10 58 79 25 stone' >/dev/null
m 'setblock 50 80 12 tnt' >/dev/null; m 'setblock 51 80 12 redstone_block' >/dev/null; sleep 3
ok "powered TNT block still there" "$(m 'execute if block 50 80 12 tnt')"
m 'summon tnt 50 80 20 {fuse:1}' >/dev/null; sleep 3
ok "stone under summoned TNT (want 9)" "$(for x in 49 50 51; do for z in 19 20 21; do m "execute if block $x 79 $z stone"; done; done | grep -c passed)"
# Fire: fire next to planks neither spreads nor burns them.
m 'fill 45 80 23 55 80 23 oak_planks' >/dev/null; m 'setblock 45 81 23 fire' >/dev/null; sleep 30
ok "planks after 30s of fire (want 11)" "$(for x in $(seq 45 55); do m "execute if block $x 80 23 oak_planks"; done | grep -c passed)"
ok "fire spread to other planks (want 0)" "$(for x in $(seq 46 55); do m "execute if block $x 81 23 fire"; done | grep -c passed)"
# Weather: rain is refused.
m 'weather rain' >/dev/null
ok "rain refused" "$(docker logs market-smoke 2>&1 | grep -c 'Changing to rain')"
# clearmobs: unnamed cows go, the named one stays.
for i in 1 2 3; do m "summon cow 50 81 1$i" >/dev/null; done
m 'summon cow 52 81 12 {CustomName:"\"Bessie\""}' >/dev/null
r 'market admin clearmobs animals'
ok "cows left (want 1, named)" "$(m 'execute if entity @e[x=-20,y=-64,z=-20,dx=200,dy=400,dz=200,type=cow]')"
docker logs market-smoke 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -iE 'exception|could not pass|error occurred' | grep -iv essentials | head -5
docker logs market-smoke > "$WORK.server.log" 2>&1
