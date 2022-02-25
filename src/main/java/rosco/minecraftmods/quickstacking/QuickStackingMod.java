package rosco.minecraftmods.quickstacking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class QuickStackingMod implements ModInitializer {

	public static final Logger LOGGER = LogManager.getLogger("quickstacking");

	public static final Identifier QUICK_STACK_NEARBY = new Identifier("quickstacking", "stacknear");
	public static final Identifier QUICK_STACK_SPECIFIC = new Identifier("quickstacking", "stackone");
	public static final Identifier DUMP_NEARBY = new Identifier("quickstacking", "dumpnear");
	public static final Identifier DUMP_SPECIFIC = new Identifier("quickstacking", "dumpone");
	public static final Identifier FAVOURITE = new Identifier("quickstacking", "favourite");
	public static final Identifier HIGHLIGHT = new Identifier("quickstacking", "highlight");

	@Override
	public void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(
				QuickStackingMod.QUICK_STACK_NEARBY,
				(MinecraftServer minecraftServer,
						ServerPlayerEntity serverPlayerEntity,
						ServerPlayNetworkHandler serverPlayNetworkHandler,
						PacketByteBuf packetByteBuf,
						PacketSender packetSender) -> QuickStackingUtils.quickStackOrDumpNearby(
								serverPlayerEntity, packetByteBuf.readBoolean(), false));

		ServerPlayNetworking.registerGlobalReceiver(
				QuickStackingMod.QUICK_STACK_SPECIFIC,
				(MinecraftServer minecraftServer,
						ServerPlayerEntity serverPlayerEntity,
						ServerPlayNetworkHandler serverPlayNetworkHandler,
						PacketByteBuf packetByteBuf,
						PacketSender packetSender) -> QuickStackingUtils.quickStackOrDumpSpecific(
								serverPlayerEntity, packetByteBuf.readBoolean(), false));

		ServerPlayNetworking.registerGlobalReceiver(
				QuickStackingMod.DUMP_NEARBY,
				(MinecraftServer minecraftServer,
						ServerPlayerEntity serverPlayerEntity,
						ServerPlayNetworkHandler serverPlayNetworkHandler,
						PacketByteBuf packetByteBuf,
						PacketSender packetSender) -> QuickStackingUtils.quickStackOrDumpNearby(
								serverPlayerEntity, packetByteBuf.readBoolean(), true));

		ServerPlayNetworking.registerGlobalReceiver(
				QuickStackingMod.DUMP_SPECIFIC,
				(MinecraftServer minecraftServer,
						ServerPlayerEntity serverPlayerEntity,
						ServerPlayNetworkHandler serverPlayNetworkHandler,
						PacketByteBuf packetByteBuf,
						PacketSender packetSender) -> QuickStackingUtils.quickStackOrDumpSpecific(
								serverPlayerEntity, packetByteBuf.readBoolean(), true));

		ServerPlayNetworking.registerGlobalReceiver(
				QuickStackingMod.FAVOURITE,
				(MinecraftServer minecraftServer,
						ServerPlayerEntity serverPlayerEntity,
						ServerPlayNetworkHandler serverPlayNetworkHandler,
						PacketByteBuf packetByteBuf,
						PacketSender packetSender) -> QuickStackingUtils.toggleFavourited(
								serverPlayerEntity, packetByteBuf.readInt()));
	}

	void debug(PlayerEntity player, String message) {
		player.sendSystemMessage(new LiteralText(message), Util.NIL_UUID);
	}
}
