package rosco.minecraftmods.quickstacking.mixins.compat.reinforcedstorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.inventory.Inventory;
import rosco.minecraftmods.quickstacking.ReinforcedStorageScreenHandlerSafeAccessor;

@Pseudo
@Mixin(targets = "atonkish.reinfcore.screen.ReinforcedStorageScreenHandler", remap = false)
public interface ReinforcedStorageScreenHandlerAccessor extends ReinforcedStorageScreenHandlerSafeAccessor {
    @Accessor
    Inventory getInventory();

    @Accessor
    boolean getIsShulkerBox();
}
