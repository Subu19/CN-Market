# Store listing (SpigotMC / Modrinth)

Publishing is manual. Paste and adjust.

**Name:** CN-Market
**Tagline:** A market world with claimable plots, barrel shops and supply-and-demand prices.
**Categories:** Economy, World Management
**Supports:** Spigot, Paper 1.21 – 26.3 · Java 21 · requires Vault + an economy plugin

---

CN-Market gives your server a separate market world, laid out as a grid of plots and pathways. Players claim a plot, place barrels, and turn them into shops that sell to or buy from other players. Prices for admin shops move each day with what players actually buy and sell.

**For players**
- `/market` to go there, `/market leave` to go back.
- Claim a plot, add friends as members, set your plot's arrival point.
- Right-click a barrel on your plot to open a shop; stock is whatever is in the barrel.
- `/market shops` browses and searches every shop on the server and teleports you to the one you pick.
- Sales made while you are offline are paid out and summarised when you log in.

**For admins**
- One command builds the world: `/market admin setup market 32 7`.
- Plot protection without other plugins: explosions, fire, pistons, fluids, mobs and projectiles cannot cross a plot border; pathways and spawn are protected.
- A calm market world out of the box: no mob spawning, PvP, explosions, fire spread, hunger or fall damage, clear weather, void rescue. Each one is a switch; no WorldGuard needed.
- Admin shops with unlimited stock and daily dynamic prices, bounded by limits you set.
- SQLite storage off the main thread. Redstone next to plot borders costs nothing measurable.
- Every player permission is on by default; restrict with any permissions plugin.

**Not supported yet:** Folia.

Source and issues: https://github.com/Subu19/CN-Market · MIT licence
