package rosco.minecraftmods.quickstacking.mixins;

import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;
import rosco.minecraftmods.quickstacking.QuickStackingMod;

@Mixin(value = HandledScreen.class, priority = 1)
public abstract class HandledScreenMixin extends DrawableHelper {

    @Shadow
    protected abstract Slot getSlotAt(double x, double y);

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawFav(MatrixStack matrices, Slot slot, CallbackInfo info) {
        var stack = slot.getStack();
        var mainInventory = MinecraftClient.getInstance().player.getInventory().main;
        if (!mainInventory.contains(stack) ||
                !stack.hasNbt() ||
                !stack.getNbt().getBoolean("favourited")) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);
        HandledScreen.fillGradient(matrices, slot.x, slot.y, slot.x + 16, slot.y + 16,
                0x663AD01C,
                0x663AD01C,
                this.getZOffset());
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
    }

    @Inject(method = "mouseDragged", at = @At("RETURN"), cancellable = true)
    public void onMouseDragged(double x2, double y2, int button, double x1, double y1,
            CallbackInfoReturnable<Boolean> info) {
        if (button == 0 && getSlotAt(x2, y2) != null && HandledScreen.hasAltDown()) {
            info.setReturnValue(info.getReturnValue());
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (HandledScreen.hasAltDown() && button == 0) {
            var slot = this.callGetSlotAt(mouseX, mouseY);
            if (slot != null) {
                var stack = slot.getStack();
                if (stack.isEmpty()) {
                    return;
                }

                var index = MinecraftClient.getInstance().player.getInventory().main.indexOf(stack);
                if (index >= 0) {
                    var buf = PacketByteBufs.create();
                    buf.writeInt(index);
                    ClientPlayNetworking.send(QuickStackingMod.FAVOURITE, buf);
                }
                info.setReturnValue(true);
            }
        }
    }

    @Invoker
    protected abstract Slot callGetSlotAt(double x, double y);
}
