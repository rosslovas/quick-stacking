package rosco.minecraftmods.quickstacking.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;

@Mixin(DoubleInventory.class)
public interface DoubleInventoryAccessor {
    @Accessor
    Inventory getFirst();

    @Accessor
    Inventory getSecond();
}
