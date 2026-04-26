# Market Plugin - Shop Interaction & Economy Implementation Plan

This document outlines the architecture and step-by-step implementation for the player-to-player shop economy, handling transactions, UI, and stock management.

## Phase 1: Economy Integration (Vault)
To process transactions, we need to hook into the server's economy plugin (e.g., EssentialsX) using the Vault API.

1. **Dependencies:** Add `VaultAPI` to `pom.xml` and `depend: [Vault]` to `plugin.yml`.
2. **Hooking System:** Create an `EconomyUtils` helper class.
   - Initialize the `Economy` provider in `Market.java`'s `onEnable()`.
   - Create safe wrapper methods: `hasSufficientBalance(UUID, amount)`, `withdraw(UUID, amount)`, `deposit(UUID, amount)`.
   - Ensure these methods support offline players (so the shop owner gets paid even if they are offline).

## Phase 2: Visitor Shop UI (Interactive Chat)
When a visitor interacts with a shop, they shouldn't open the barrel. Instead, they will be presented with a sleek, chat-based interactive UI, similar to the price-setting UI.

1. **Event Listener (`ShopInteraction.java`):**
   - Intercept `PlayerInteractEvent` (Right-Click block).
   - If the block is a shop barrel and the player is **not** the owner, cancel the event to prevent them from stealing items.
   - Trigger the `openShopMenu(Player, ChestShop)` method.
2. **Chat Component UI:**
   - Display the Item name, Shop Owner, Price per item, and Current Stock.
   - Render interactive BungeeCord `TextComponent` buttons.
   - **Buttons for Buying:** `[Buy 1]` `[Buy 16]` `[Buy 64]`
   - **Buttons for Selling:** `[Sell 1]` `[Sell 16]` `[Sell 64]` *(If the shop owner is willing to buy items from players)*.
   - Each button triggers a hidden command (e.g., `/market transaction <shopId> buy 16`).

## Phase 3: Transaction Logic (Buying & Selling)
We need a robust, exploit-free transaction processor.

### Buying Flow (Visitor buys from Shop):
1. **Pre-checks:**
   - Does the barrel still exist and is it registered?
   - Does the barrel have `X` items in stock?
   - Does the buyer have enough money (`X * price`)?
   - Does the buyer have enough empty inventory slots to receive `X` items?
2. **Execution:**
   - Deduct money from the buyer via Vault.
   - Deposit money to the shop owner via Vault.
   - Remove `X` items from the barrel's inventory.
   - Give `X` items to the buyer's inventory.
3. **Post-Transaction:**
   - Send receipt message to the buyer.
   - Update the hologram/display to reflect the new stock count.

### Selling Flow (Visitor sells to Shop):
*(If you want shops to act as both buy/sell points)*
1. **Pre-checks:**
   - Does the visitor have `X` items in their inventory?
   - Does the shop owner have enough money to pay the visitor?
   - Does the barrel have empty space for `X` items?
2. **Execution:**
   - Deduct money from the shop owner.
   - Deposit money to the visitor.
   - Remove `X` items from the visitor.
   - Add `X` items to the barrel.

## Phase 4: Robustness and Security
1. **Strict Item Matching:** Ensure the plugin correctly matches exact items (including enchantments, custom metadata, and durability) so players can't scam shops with fake items.
2. **Shop Destruction Security:** Update `BlockBreakEvent`. If an owner (or admin) breaks the barrel, the shop must be securely unregistered, displays removed, and data cleared from `RegionData.yml`.
3. **Stock Holograms:** Update `DisplayUtils` to automatically refresh the floating text above the barrel whenever a transaction occurs so the "In Stock" number is always accurate.

---
### Questions for You:
1. Do you want shops to support **both** Buying and Selling, or should shops strictly be for the owner to sell items to visitors?
2. Is Vault the correct economy API to use for your server?
