<div align="center">
  <img src="image.png" alt="CN-Market Cover" width="100%">

  # 🛒 CN-Market
  ### The Ultimate Minecraft Market Solution
  
  [![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/Subu19/CN-Market)
  [![Platform](https://img.shields.io/badge/platform-Spigot%20%7C%20Paper-green.svg)](https://papermc.io/)
  [![Java](https://img.shields.io/badge/java-21-orange.svg)](https://www.oracle.com/java/)
  [![License](https://img.shields.io/badge/license-MIT-red.svg)](LICENSE)

  **CN-Market** is a feature-rich, high-performance market plugin designed for modern Minecraft servers. It provides a dedicated world for player commerce, complete with a robust plot system, dynamic pricing, and ironclad grief protection.

  [Features](#-features) • [Installation](#-installation) • [Commands](#-commands) • [Permissions](#-permissions) • [Configuration](#-configuration)
</div>

---

## ✨ Features

- **🗺️ Dedicated Market World**: Automated world generation with pre-configured pathways and plot layouts.
- **🏠 Advanced Plot System**: Players can claim, manage, and add members to their own market plots.
- **🛡️ Plot Protection**: Explosions, fire, pistons, fluids, mobs and projectiles cannot cross plot borders.
- **📈 Dynamic Pricing**: Real-time price adjustments based on supply and demand, ensuring a balanced economy.
- **🖥️ Professional GUIs**: Intuitive, lag-free menus for browsing shops and managing listings.
- **✨ Visual Shop Displays**: Beautiful item displays that make the market feel alive and premium.
- **⚡ High Performance**: Built with optimization in mind to handle large player bases without TPS drops.

---

## 📋 Requirements

- Java 21, Spigot or Paper **1.21 or newer** (tested up to 26.3). Folia is not supported yet.
- [Vault](https://www.spigotmc.org/resources/vault.341/) and any Vault economy plugin (EssentialsX, CMI, ...).
- Optional: WorldEdit or FastAsyncWorldEdit, only to paste a custom pathway schematic. Without it the pathways are built from blocks.

---

## 📦 Installation

1. Drop `Market.jar` into `plugins/` and restart.
2. As an op or from the console, run `/market admin setup <worldName> <plotSize> <pathwayWidth>`, e.g. `/market admin setup market 32 7`.
3. Optional: stand where players should arrive and run `/market admin setspawn`.
4. Adjust `config.yml` and `price.yml`, then `/market admin reload`.

Do not change `plot-size` or `pathway-width` once players have claimed plots: plot positions are computed from them.

---

## 🛠️ Commands

### Players

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/market` | Teleport to the market. | `market.use` |
| `/market leave` | Return to where you were before the market. | `market.back` |
| `/market shops` | Browse every shop in a menu, with search. | `market.shops` |
| `/market help` | List commands. | |
| `/market plot claim` | Claim the plot you stand in. | `market.plot.claim` |
| `/market plot unclaim` | Give up your plot (refused while your shops hold stock). | `market.plot.unclaim` |
| `/market plot info` | Show the plot you stand in. | `market.plot.info` |
| `/market plot manage` | Manage your plot and see out-of-stock shops. | `market.use` |
| `/market plot spawn` | Teleport to your plot. | `market.plot.teleport` |
| `/market plot visit <player\|plotId>` | Teleport to someone's plot. | `market.plot.teleport` |
| `/market plot setspawn` | Set your plot's arrival point. | `market.plot.setspawn` |
| `/market plot addmember <player>` | Let a player build on your plot. | `market.plot.addmember` |
| `/market plot removemember <player>` | Remove a member. | `market.plot.removemember` |

Shops: place a barrel on your plot, then right-click it to create a selling or buying shop.

### Admins (`market.admin`)

| Command | Description |
| :--- | :--- |
| `/market admin setup <world> <plotSize> <pathwayWidth>` | Create the market world. Console allowed. |
| `/market admin setspawn` | Set the market arrival point to where you stand. |
| `/market admin reload` | Reload config and price files. Console allowed. |
| `/market admin forceupdate` | Run the daily price update now. Console allowed. |
| `/market admin calcprices` | Recalculate base prices. Console allowed. |
| `/market admin syncstock` | Re-count stock in every shop barrel. Console allowed. |
| `/market admin listplots` | List plots. |
| `/market admin setowner <plotId> <player>` | Give a plot to a player. Console allowed. |
| `/market admin deleteplot <plotId>` | Delete a plot and its shops. |
| `/market admin toggleborder plot [plotId]` | Show plot borders. |
| `/market admin selectionmode <mode>` | Select plot corners by hand. |
| `/market admin bypass` | Toggle bypassing plot protection. |
| `/market admin reset confirm` | Delete the market world and **all** plot and shop data. Console allowed. |

---

## 🔐 Permissions

Player permissions are granted to everyone by default; negate them in your permissions plugin to restrict.

| Permission | Default | Grants |
| :--- | :--- | :--- |
| `market.use` | everyone | `/market` |
| `market.back` | everyone | `/market leave` |
| `market.shops` | everyone | the shop browser |
| `market.plot.claim`, `.unclaim`, `.info`, `.teleport`, `.setspawn`, `.addmember`, `.removemember` | everyone | the matching `/market plot` command |
| `market.plots.limit.<n>` | none | claim up to `<n>` plots (overrides `max-plots-per-player`) |
| `market.plots.limit.unlimited` | nobody | no plot limit |
| `market.admin` | op | admin commands, admin shops, building anywhere in the market |

---

## ⚙️ Configuration

`config.yml` is commented. The main keys:

- `market-world.max-plots-per-player`, `max-height`, `spawn-radius` (unclaimable plots around 0,0).
- `market-world.rules`: optional `no-pvp`, `no-monsters`, `no-hunger`, `no-fall-damage` for the market world, all off by default. Leave them off if WorldGuard or another plugin already handles this.
- `market-world.allowed-interact-blocks`: blocks anyone may use anywhere (ender chests, crafting tables).
- `player-shop-blacklist`: items players cannot sell.
- `admin-shops` and `currency-exchange`: the dynamic pricing limits.
- `display-view-range`: lower it if clients lag in the market.

Data lives in `plugins/Market/market.db` (SQLite). Prices are in `price.yml` (base) and `dynamic_prices.yml` (current).

---

## 🛡️ Protection

Inside the market world only plot owners, members and admins can change a plot. Explosions, fire, pistons, fluids, growth, dispensers, projectiles, vehicles and entities cannot cross a plot border. Pathways and the spawn area are protected for everyone except admins. The full list is in [`docs/PROTECTION_CHECKLIST.md`](docs/PROTECTION_CHECKLIST.md).

---

## 🔄 Upgrading from 1.x

Replace the jar and restart. Existing `market.db`, `config.yml` and prices load unchanged. Note that player permissions now default to everyone, and the unused `market.admin.*` child permissions are gone: grant `market.admin` instead.

---

<div align="center">
  <sub>Built with ❤️ by the CraftNepal Team</sub>
</div>
