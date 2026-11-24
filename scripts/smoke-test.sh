#!/usr/bin/env bash
# Boots a throwaway server with the freshly built plugin and checks the log for errors.
#
#   scripts/smoke-test.sh <work-dir> [TYPE] [VERSION]
#
# <work-dir>  scratch directory; it is wiped and filled with a copy of ./mc-data
#             (so the dev server data is never touched). Use an empty dir for a fresh server:
#             SEED=none scripts/smoke-test.sh /tmp/x
# TYPE        PAPER (default) or SPIGOT
# VERSION     Minecraft version, default 26.1.2
# JAR=path    test another jar instead of target/Market.jar
# EXTRA_PLUGINS="a.jar b.jar"  copy these jars into plugins/ too (e.g. Vault on a fresh server)
# MODRINTH="essentialsx"       let the image download these Modrinth projects
# WAIT=360    seconds to wait for "Done" (Spigot builds itself with BuildTools on first run: raise it)
#
# Leaves the container running as "market-smoke" so you can send console commands:
#   docker exec market-smoke rcon-cli <command>
# Stop it with: docker rm -f market-smoke
set -euo pipefail

WORK=${1:?work dir required}
TYPE=${2:-PAPER}
VERSION=${3:-26.1.2}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
JAR=${JAR:-"$ROOT/target/Market.jar"}
[ -f "$JAR" ] || { echo "build first: mvn package"; exit 1; }

docker rm -f market-smoke >/dev/null 2>&1 || true
rm -rf "$WORK" && mkdir -p "$WORK"
if [ "${SEED:-copy}" = "copy" ]; then
  cp -a "$ROOT/mc-data/." "$WORK/"
  rm -f "$WORK/plugins/Market.jar"
fi
mkdir -p "$WORK/plugins"
cp "$JAR" "$WORK/plugins/Market.jar"
for p in ${EXTRA_PLUGINS:-}; do cp "$p" "$WORK/plugins/"; done

docker run -d --name market-smoke \
  -e EULA=TRUE -e TYPE="$TYPE" -e VERSION="$VERSION" -e MEMORY=2G \
  -e UID="$(id -u)" -e GID="$(id -g)" -e ENABLE_RCON=true -e RCON_PASSWORD=smoke \
  -e ONLINE_MODE=FALSE ${MODRINTH:+-e MODRINTH_PROJECTS="$MODRINTH"} \
  -v "$WORK:/data" itzg/minecraft-server >/dev/null

echo "waiting for server start..."
for _ in $(seq 1 $(( ${WAIT:-360} / 2 ))); do
  if docker logs market-smoke 2>&1 | grep -q 'Done ('; then break; fi
  docker ps -q -f name=market-smoke | grep -q . || break   # container exited
  sleep 2
done
docker logs market-smoke 2>&1 | grep -q 'Done (' || { echo "server did not start"; docker logs --tail 80 market-smoke; exit 1; }

echo "== Market lines"
docker logs market-smoke 2>&1 | grep -E '\[Market\]|Market' | grep -v 'issued server command' || true
echo "== Errors mentioning Market"
if docker logs market-smoke 2>&1 | grep -nE 'Exception|Could not pass event|Error occurred' | grep -iE 'market|craftnepal'; then
  echo "SMOKE TEST FAILED"; exit 2
fi
echo "SMOKE TEST OK"
