# Protection checklist

Walk this in game after changes to `src/main/java/net/craftnepal/market/protection/`.
Use three accounts: the plot **owner**, a **member** (`/market plot addmember`), and a **stranger**.
Expected results: owner and member allowed, stranger denied, unless the row says otherwise.
Bypass (`/market admin bypass`) allows everything.

World-cause rows (no player involved) can be checked from the console; see `docs/TESTING.md`.

## Player actions (actor rule)

| Action | Where | Expected |
|---|---|---|
| Break / place a block | own plot; neighbour plot; pathway; spawn; above `max-height` | allowed only in own plot below the limit |
| Place a bed or door with its second half across the plot edge | plot edge | denied |
| Open a chest, press a button, flip a lever, step on a pressure plate | neighbour plot | denied (no message for pressure plates) |
| Right-click an `allowed-interact-blocks` block (ender chest, crafting table) | anywhere | allowed |
| Right-click / left-click a shop barrel | neighbour plot | shop menus open (handled by ShopInteraction) |
| Empty a water/lava bucket so the liquid lands across the edge | plot edge | denied |
| Fill a bucket from a neighbour's water | neighbour plot | denied |
| Milk a cow with a bucket | anywhere | allowed |
| Edit a sign | neighbour plot | denied |
| Place a boat, minecart, armor stand, end crystal | neighbour plot | denied |
| Place an item frame or painting | neighbour plot | denied |
| Light a nether portal | neighbour plot or pathway | denied |
| Use a wind charge | anywhere in the market world | denied |
| Throw an egg | into a neighbour plot | no chickens hatch |
| Bone meal grass next to the edge | own plot | flowers do not appear in the neighbour plot |

## Entities

| Action | Where | Expected |
|---|---|---|
| Rotate / take from item frame, hit a painting | neighbour plot or pathway | denied |
| Take or swap armor stand items, hit an armor stand | neighbour plot or pathway | denied |
| Shoot an item frame or armor stand with an arrow | neighbour plot | denied |
| Hit, leash, shear, feed, ride an animal | neighbour plot | denied |
| Same with an animal | pathway | allowed (public space) |
| Trade with a villager or wandering trader | anywhere | allowed |
| Fight a hostile mob | anywhere | allowed |
| Break a boat or minecart | neighbour plot | denied |
| Scoop a fish or axolotl into a bucket | neighbour plot | denied |
| Pull an armor stand or animal with a fishing rod | neighbour plot | denied |
| Throw a harmful splash potion at animals | neighbour plot | animals unaffected |

## World causes (boundary rule)

| Cause | Expected |
|---|---|
| Water / lava flowing across a plot edge | stops at the edge |
| TNT / creeper exploding near an edge | only blocks in its own plot break; pathway explosions break nothing |
| TNT next to an edge exploding | item frames and armor stands in the neighbour plot survive |
| Piston pushing or pulling across an edge | does not move |
| Fire spreading across an edge | does not spread |
| Flaming arrow shot at a neighbour's TNT or wood | does not ignite |
| Sand / gravel pushed across an edge, then falling | does not land there |
| Tree or mushroom growing across an edge | the crossing part is cut off |
| Sponge next to an edge | does not absorb the neighbour's water |
| Dispenser facing across an edge | does not fire |
| Redstone dust or repeater on the plot edge next to a pathway or neighbour | does not change power |
| Ice, coral, leaves, snow | never melt, die or decay |
| Endermen, ravagers, sheep | do not change blocks |
| Natural lightning | does not strike |
