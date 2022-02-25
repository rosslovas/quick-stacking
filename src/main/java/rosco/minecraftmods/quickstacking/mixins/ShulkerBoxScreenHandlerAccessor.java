package rosco.minecraftmods.quickstacking.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ShulkerBoxScreenHandler;

@Mixin(ShulkerBoxScreenHandler.class)
public interface ShulkerBoxScreenHandlerAccessor {
    @Accessor
    Inventory getInventory();
}
