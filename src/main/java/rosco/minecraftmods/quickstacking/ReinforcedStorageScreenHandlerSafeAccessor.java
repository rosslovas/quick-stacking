package rosco.minecraftmods.quickstacking;

import net.minecraft.inventory.Inventory;

// By making the mixin extend this interface, we can access properties of this modded class without
// having to depend on the mod actually existing at compile time or runtime.
public interface ReinforcedStorageScreenHandlerSafeAccessor {
    Inventory getInventory();

    boolean getIsShulkerBox();
}
