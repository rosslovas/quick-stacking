package rosco.minecraftmods.quickstacking;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.MessageType;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.LiteralText;
import rosco.minecraftmods.betterkeybinding.BetterKeyBinding;
import rosco.minecraftmods.betterkeybinding.BetterKeyBindingClient;

public class QuickStackingClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		var quickStackBinding = new BetterKeyBinding(
				"key.quickstacking.quickStack", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "category.quickstacking");
		KeyBindingHelper.registerKeyBinding(quickStackBinding);
		BetterKeyBindingClient.registerKeyBinding(quickStackBinding);

		var dumpBinding = new BetterKeyBinding(
				"key.quickstacking.dump", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.quickstacking");
		KeyBindingHelper.registerKeyBinding(dumpBinding);
		BetterKeyBindingClient.registerKeyBinding(dumpBinding);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {

			while (quickStackBinding.wasPressed()) {
				if (client.currentScreen == null || client.currentScreen instanceof InventoryScreen) {
					var buf = PacketByteBufs.create();
					buf.writeBoolean(Screen.hasAltDown());
					ClientPlayNetworking.send(QuickStackingMod.QUICK_STACK_NEARBY, buf);
				} else {
					var screenHandler = client.player.currentScreenHandler;
					if (screenHandler != null
							&& (screenHandler instanceof GenericContainerScreenHandler ||
									screenHandler instanceof ShulkerBoxScreenHandler ||
									screenHandler instanceof ReinforcedStorageScreenHandlerSafeAccessor)) {
						var buf = PacketByteBufs.create();
						buf.writeBoolean(Screen.hasAltDown());
						ClientPlayNetworking.send(QuickStackingMod.QUICK_STACK_SPECIFIC, buf);
					}
				}
			}

			while (dumpBinding.wasPressed()) {
				if (client.currentScreen == null || client.currentScreen instanceof InventoryScreen) {
					var buf = PacketByteBufs.create();
					buf.writeBoolean(Screen.hasAltDown());
					ClientPlayNetworking.send(QuickStackingMod.DUMP_NEARBY, buf);
				} else {
					var screenHandler = client.player.currentScreenHandler;
					if (screenHandler != null
							&& (screenHandler instanceof GenericContainerScreenHandler ||
									screenHandler instanceof ShulkerBoxScreenHandler ||
									screenHandler instanceof ReinforcedStorageScreenHandlerSafeAccessor)) {
						var buf = PacketByteBufs.create();
						buf.writeBoolean(Screen.hasAltDown());
						ClientPlayNetworking.send(QuickStackingMod.DUMP_SPECIFIC, buf);
					}
				}
			}

		});

		ClientPlayNetworking.registerGlobalReceiver(
				QuickStackingMod.HIGHLIGHT,
				(client, handler, buf, responseSender) -> HighlightRenderer.addHighlights(buf));

		WorldRenderEvents.LAST.register(context -> HighlightRenderer.render(context));
	}

	public static void debug(String message) {
		var client = MinecraftClient.getInstance();
		client.inGameHud.addChatMessage(
				MessageType.SYSTEM, new LiteralText(message), client.player.getUuid());
	}
}
