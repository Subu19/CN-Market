package net.craftnepal.market.Entities;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

public class DisplayPair {
    private ItemDisplay itemDisplay;
    private TextDisplay textDisplay;
    private Location location;

    public DisplayPair(ItemDisplay itemDisplay, TextDisplay textDisplay, Location location) {
        this.itemDisplay = itemDisplay;
        this.textDisplay = textDisplay;
        this.location = location;
    }

    public void remove() {
        if (itemDisplay != null) {
            itemDisplay.remove();
        }
        if (textDisplay != null) {
            textDisplay.remove();
        }
    }

    public void update(ItemStack item, String text) {
        if (itemDisplay != null) {
            itemDisplay.setItemStack(item);
        }
        if (textDisplay != null) {
            textDisplay.setText(text);
        }
    }

    public ItemDisplay getItemDisplay() {
        return itemDisplay;
    }

    public TextDisplay getTextDisplay() {
        return textDisplay;
    }

    public Location getLocation() {
        return location;
    }

    public void setItemDisplay(ItemDisplay itemDisplay) {
        this.itemDisplay = itemDisplay;
    }

    public void setTextDisplay(TextDisplay textDisplay) {
        this.textDisplay = textDisplay;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
