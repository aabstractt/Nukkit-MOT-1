package cn.nukkit.inventory;

import cn.nukkit.item.Item;
import lombok.NonNull;

public interface InventoryListener {

    /**
     * Called when an item is added to the inventory.
     * @param inventory The inventory that the item was added to
     * @param oldItem The item that was replaced, or null if the slot was empty
     * @param slot The slot that the item was added to
     */
    void onInventoryChanged(@NonNull Inventory inventory, @NonNull Item oldItem, int slot);
}