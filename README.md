<div align="center">
  <img src="image.png" alt="CN-Market Cover" width="100%">

  # 🛒 CN-Market
  ### The Ultimate Minecraft Market Solution
  
  [![Version](https://img.shields.io/badge/version-1.0.10-blue.svg)](https://github.com/Subu19/CN-Market)
  [![Platform](https://img.shields.io/badge/platform-Spigot%20%7C%20Paper-green.svg)](https://papermc.io/)
  [![Java](https://img.shields.io/badge/java-21-orange.svg)](https://www.oracle.com/java/)
  [![License](https://img.shields.io/badge/license-MIT-red.svg)](LICENSE)

  **CN-Market** is a feature-rich, high-performance market plugin designed for modern Minecraft servers. It provides a dedicated world for player commerce, complete with a robust plot system, dynamic pricing, and ironclad grief protection.

  [Features](#-features) • [Commands](#-commands) • [Permissions](#-permissions) • [Setup](#-setup) • [Installation](#-installation)
</div>

---

## ✨ Features

- **🗺️ Dedicated Market World**: Automated world generation with pre-configured pathways and plot layouts.
- **🏠 Advanced Plot System**: Players can claim, manage, and add members to their own market plots.
- **🛡️ Comprehensive Protection**: State-of-the-art grief protection covering 18+ different vectors, from TNT to wind charges.
- **📈 Dynamic Pricing**: Real-time price adjustments based on supply and demand, ensuring a balanced economy.
- **🖥️ Professional GUIs**: Intuitive, lag-free menus for browsing shops and managing listings.
- **✨ Visual Shop Displays**: Beautiful item displays that make the market feel alive and premium.
- **⚡ High Performance**: Built with optimization in mind to handle large player bases without TPS drops.

---

## 🛠️ Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/market` | Main command to teleport to the market world. | `market.use` |
| `/market plot claim` | Claim the plot you are currently standing in. | `market.plot.claim` |
| `/market plot unclaim` | Unclaim your current plot. | `market.plot.unclaim` |
| `/market plot info` | View detailed information about a plot. | `market.plot.info` |
| `/market shops` | Open the graphical shop browser. | `market.shops` |
| `/market back` | Return to your previous location outside the market. | `market.back` |
| `/market admin setup` | Initialize the market world and its configuration. | `market.admin.setup` |

---

## 🔐 Permissions

### Player Permissions
- `market.use`: Allows access to the market world and base commands.
- `market.plot.claim`: Allows players to claim their own market space.
- `market.plot.addmember`: Allows adding trusted friends to a plot.
- `market.shops`: Allows opening the shop browsing menu.

### Admin Permissions
- `market.admin`: Full access to all administrative functions.
- `market.admin.bypass`: Bypass all region protections in the market world.
- `market.admin.forceupdate`: Manually trigger a dynamic price update cycle.
- `market.admin.reset`: Completely reset the market world and its database.

---

## 🚀 Setup

1. **Install Dependencies**: Ensure you have [Vault](https://www.spigotmc.org/resources/vault.341/) and an economy plugin installed.
2. **Initial Setup**: Run `/market admin setup <worldName> <plotSize> <pathwayWidth>` to generate the world.
3. **Configure**: Adjust `config.yml` and `price.yml` to fit your server's economy.
4. **Enjoy**: Players can now start claiming plots and building their commercial empire!

---

## 📦 Installation

1. Download the latest `Market.jar`.
2. Drop it into your server's `plugins` folder.
3. Restart the server.
4. Profit!

---

<div align="center">
  <sub>Built with ❤️ by the CraftNepal Team</sub>
</div>
