package rosco.minecraftmods.quickstacking.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "areNbtEqual", at = @At("RETURN"), cancellable = true)
    private static void areNbtEqualDisregardingFavourite(
            ItemStack left, ItemStack right, CallbackInfoReturnable<Boolean> info) {

        if (info.getReturnValue() == false &&
                !left.isEmpty() && !right.isEmpty() &&
                left != null && right != null &&
                left.isOf(right.getItem())) {

            var l = left.getNbt();
            var r = right.getNbt();

            NbtCompound nonFavourited;
            NbtCompound favourited;
            if (l != null && l.contains("favourited")) {
                if (r != null && r.contains("favourited")) {
                    return;
                }
                nonFavourited = r;
                favourited = l;
            } else if (r != null && r.contains("favourited")) {
                nonFavourited = l;
                favourited = r;
            } else {
                return;
            }

            if (nonFavourited == null) {
                if (favourited.getKeys().size() == 1) {
                    info.setReturnValue(true);
                }
                return;
            }

            var copy = favourited.copy();
            copy.remove("favourited");
            info.setReturnValue(copy.equals(nonFavourited));
        }
    }
}
