package rosco.minecraftmods.quickstacking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import rosco.minecraftmods.quickstacking.mixins.DoubleInventoryAccessor;
import rosco.minecraftmods.quickstacking.mixins.ShulkerBoxScreenHandlerAccessor;

public final class QuickStackingUtils {

    public static void quickStackOrDumpNearby(
            ServerPlayerEntity serverPlayerEntity, boolean includeHotbar, boolean dump) {

        if (serverPlayerEntity.isSpectator()) {
            return;
        }

        var containers = findAndHighlightNearbyInventories(serverPlayerEntity, 3);

        for (var info : containers) {
            var container = info.inventory;
            // QuickStackingMod.LOGGER.info(
            // (dump ? "Dumping" : "Quick stacking") + " to " +
            // container.getClass().getSimpleName());

            quickStackOrDump(serverPlayerEntity.getInventory(), container,
                    includeHotbar, dump, container instanceof ShulkerBoxBlockEntity);
        }

        // if (containers.size() == 0) {
        // QuickStackingMod.LOGGER.info("Nowhere to " + (dump ? "dump" : "quick
        // stack"));
        // }
    }

    public static void quickStackOrDumpSpecific(
            ServerPlayerEntity serverPlayerEntity, boolean includeHotbar, boolean dump) {

        if (serverPlayerEntity.isSpectator()) {
            return;
        }

        var screenHandler = serverPlayerEntity.currentScreenHandler;
        if (screenHandler == null) {
            return;
        }

        Inventory inventory = null;
        var isShulker = false;
        if (screenHandler instanceof GenericContainerScreenHandler) {
            inventory = ((GenericContainerScreenHandler) screenHandler).getInventory();
        } else if (screenHandler instanceof ShulkerBoxScreenHandler) {
            inventory = ((ShulkerBoxScreenHandlerAccessor) screenHandler).getInventory();
            isShulker = true;
        } else if (screenHandler instanceof ReinforcedStorageScreenHandlerSafeAccessor) {
            var handler = (ReinforcedStorageScreenHandlerSafeAccessor) screenHandler;
            inventory = handler.getInventory();
            isShulker = handler.getIsShulkerBox();
        }

        if (inventory == null || inventory.size() < 27) {
            return;
        }

        // QuickStackingMod.LOGGER.info(
        // (dump ? "Dumping" : "Quick stacking") + " (specific) to " +
        // inventory.getClass().getSimpleName());

        quickStackOrDump(serverPlayerEntity.getInventory(), inventory, includeHotbar, dump, isShulker);
    }

    public static void toggleFavourited(ServerPlayerEntity serverPlayerEntity, int stackIndex) {
        if (stackIndex < 0) {
            return;
        }

        var mainInventory = serverPlayerEntity.getInventory().main;
        if (stackIndex > mainInventory.size()) {
            return;
        }

        var stack = mainInventory.get(stackIndex);
        if (stack == null || stack.isEmpty()) {
            return;
        }

        if (stack.hasNbt()) {
            var nbt = stack.getNbt();
            if (nbt.getBoolean("favourited")) {
                nbt.remove("favourited");
                if (nbt.isEmpty()) {
                    stack.setNbt(null);
                }
            } else {
                nbt.putBoolean("favourited", true);
            }
        } else {
            var nbt = new NbtCompound();
            nbt.putBoolean("favourited", true);
            stack.setNbt(nbt);
        }
    }

    private static ArrayList<ContainerInfo> findAndHighlightNearbyInventories(
            ServerPlayerEntity serverPlayerEntity, int blockRadius) {
        var world = serverPlayerEntity.world;

        var x1 = 0;
        var x2 = 0;
        var z1 = 0;
        var z2 = 0;
        var cameraYaw = MathHelper.wrapDegrees(serverPlayerEntity.getYaw());
        if (cameraYaw > -67.5f && cameraYaw <= 67.5f) {
            ++z1;
        }
        if (cameraYaw > 22.5f && cameraYaw <= 157.5f) {
            ++x2;
        }
        if (cameraYaw > 112.5f || cameraYaw <= -112.5f) {
            ++z2;
        }
        if (cameraYaw > -157.5f && cameraYaw <= -22.5f) {
            ++x1;
        }

        final int size = 2;
        var pitch = serverPlayerEntity.getPitch();
        var axisAligned = x1 + x2 + z1 + z2 == 1 ? 1 : 0;

        var containers = new ArrayList<ContainerInfo>();
        var chestsAlreadyHandled = new HashSet<Inventory>();

        var playerPos = serverPlayerEntity.getBlockPos();
        for (var x = -size - x2; x <= size + x1; ++x) {
            for (var z = -size - z2; z <= size + z1; ++z) {
                for (var y = -2; y <= 3; ++y) {
                    if (y == -2 &&
                            (pitch < 20 ||
                                    ((x1 == 1 && x < axisAligned) ||
                                            (x2 == 1 && x > -axisAligned) ||
                                            (z1 == 1 && z < axisAligned) ||
                                            (z2 == 1 && z > -axisAligned)))) {
                        continue;
                    } else if (y == 3 &&
                            (pitch > 0 ||
                                    ((x1 == 1 && x < axisAligned) ||
                                            (x2 == 1 && x > -axisAligned) ||
                                            (z1 == 1 && z < axisAligned) ||
                                            (z2 == 1 && z > -axisAligned)))) {
                        continue;
                    }

                    var pos = playerPos.add(x, y, z);
                    if (world.isOutOfHeightLimit(pos)) {
                        continue;
                    }

                    var blockEntity = world.getWorldChunk(pos).getBlockEntity(pos);
                    if (blockEntity == null ||
                            !(blockEntity instanceof LootableContainerBlockEntity ||
                                    blockEntity instanceof EnderChestBlockEntity)) {
                        continue;
                    }

                    var info = (blockEntity instanceof EnderChestBlockEntity)
                            ? new ContainerInfo(
                                    (Inventory) serverPlayerEntity.getEnderChestInventory(), blockEntity.getPos())
                            : new ContainerInfo((Inventory) blockEntity, blockEntity.getPos());
                    if (info.inventory.size() < 27 ||
                            chestsAlreadyHandled.contains(info.inventory) ||
                            (info.inventory instanceof LootableContainerBlockEntity
                                    && !((LootableContainerBlockEntity) info.inventory)
                                            .checkUnlocked(serverPlayerEntity))) {
                        continue;
                    }

                    if (info.inventory instanceof ChestBlockEntity) {
                        var chest = (ChestBlockEntity) info.inventory;

                        var blockState = chest.getCachedState();
                        var chestType = blockState.get(ChestBlock.CHEST_TYPE);
                        if (chestType != ChestType.SINGLE) {
                            getDoubleChestFromOneHalf(info, blockState, chestType, world);
                            var left = ((DoubleInventoryAccessor) info.inventory).getFirst();
                            var right = ((DoubleInventoryAccessor) info.inventory).getSecond();

                            if (chest == left) {
                                chestsAlreadyHandled.add(right);
                            } else {
                                chestsAlreadyHandled.add(left);
                            }
                        }
                    }

                    containers.add(info);
                }
            }
        }

        var targetPos = serverPlayerEntity.getEyePos()
                .add(serverPlayerEntity.getCameraEntity().getRotationVector().multiply(1.25));

        Collections.sort(containers, new Comparator<ContainerInfo>() {
            @Override
            public int compare(ContainerInfo l, ContainerInfo r) {
                return l.getDistanceSquared(targetPos) < r.getDistanceSquared(targetPos) ? -1 : 1;
            }
        });

        if (!containers.isEmpty()) {
            var buf = PacketByteBufs.create();
            buf.writeInt(containers.size());
            for (var info : containers) {
                buf.writeBlockPos(info.pos);
                buf.writeInt(info.type);
                buf.writeFloat((float) Math.sqrt(info.distanceSquared));
            }
            ServerPlayNetworking.send(serverPlayerEntity, QuickStackingMod.HIGHLIGHT, buf);
        }

        return containers;
    }

    private static void quickStackOrDump(
            PlayerInventory source,
            Inventory dest,
            boolean includeHotbar,
            boolean dump,
            boolean isShulker) {

        var min = includeHotbar ? 0 : 9;
        var max = includeHotbar ? source.main.size() : 36;
        for (var sourceSlot = min; sourceSlot < max; ++sourceSlot) {

            var stack = source.getStack(sourceSlot);
            if (stack.hasNbt() && stack.getNbt().getBoolean("favourited")) {
                continue;
            }

            var item = stack.getItem();
            if (isShulker && (Block.getBlockFromItem(item) instanceof ShulkerBoxBlock)) {
                continue;
            }

            var sourceStack = source.removeStack(sourceSlot);
            var emptyTargetSlot = -1;
            var found = dump;
            for (var destSlot = 0; destSlot < dest.size(); ++destSlot) {
                var destStack = dest.getStack(destSlot);

                if (emptyTargetSlot == -1 && destStack.isEmpty()) {
                    emptyTargetSlot = destSlot;
                    continue;
                }

                if (destStack.getItem() != item) {
                    continue;
                }

                found = true;
                if (!ItemStack.canCombine(sourceStack, destStack)) {
                    continue;
                }

                var destCount = destStack.getCount();
                var freeSpace = destStack.getMaxCount() - destCount;
                if (freeSpace <= 0) {
                    continue;
                }

                var sourceCount = sourceStack.getCount();
                var toMove = Math.min(sourceCount, freeSpace);
                sourceStack.setCount(sourceCount - toMove);
                destStack.setCount(destCount + toMove);

                if (sourceCount == toMove) {
                    break;
                }
            }

            if (sourceStack.getCount() > 0) {
                if (found && emptyTargetSlot >= 0) {
                    dest.setStack(emptyTargetSlot, sourceStack);
                } else {
                    source.setStack(sourceSlot, sourceStack);
                }
            }
        }

    }

    private static void getDoubleChestFromOneHalf(
            ContainerInfo info, BlockState blockState, ChestType chestType, World world) {
        var chest = (ChestBlockEntity) info.inventory;
        var chestFacing = ChestBlock.getFacing(blockState);
        var otherHalfPos = chest.getPos().add(chestFacing.getVector());
        var otherHalf = world.getWorldChunk(otherHalfPos).getBlockEntity(otherHalfPos);
        if (!(otherHalf instanceof ChestBlockEntity)) {
            throw new RuntimeException("Expected other half of double chest");
        }

        // Confusing, but this is actually the right half
        if (chestType == ChestType.LEFT) {
            info.inventory = new DoubleInventory((Inventory) otherHalf, chest);
        } else {
            info.inventory = new DoubleInventory(chest, (Inventory) otherHalf);
        }

        var chestPos = chest.getPos();
        var otherPos = otherHalf.getPos();
        var x1 = chestPos.getX();
        var x2 = otherPos.getX();
        var z1 = chestPos.getZ();
        var z2 = otherPos.getZ();
        if (x1 != x2) {
            info.pos = new BlockPos(Math.min(x1, x2), chestPos.getY(), z1);
            info.pos2 = info.pos.add(1, 0, 0);
            info.type = 1;
        } else if (z1 != z2) {
            info.pos = new BlockPos(x1, chestPos.getY(), Math.min(z1, z2));
            info.pos2 = info.pos.add(0, 0, 1);
            info.type = 2;
        }
    }

    private static class ContainerInfo {
        public BlockPos pos;
        public BlockPos pos2 = null;
        public Inventory inventory;
        public int type = 0;

        private boolean distanceCalculated = false;
        public double distanceSquared = 0.0;

        public ContainerInfo(Inventory inventory, BlockPos pos) {
            this.inventory = inventory;
            this.pos = pos;
        }

        public double getDistanceSquared(Vec3d playerPos) {
            if (!distanceCalculated) {
                distanceCalculated = true;
                var dx = playerPos.x - (pos.getX() + 0.5);
                var dy = playerPos.y - (pos.getY() + 0.5);
                var dz = playerPos.z - (pos.getZ() + 0.5);
                distanceSquared = dx * dx + dy * dy + dz * dz;

                if (pos2 != null) {
                    dx = playerPos.x - (pos2.getX() + 0.5);
                    dy = playerPos.y - (pos2.getY() + 0.5);
                    dz = playerPos.z - (pos2.getZ() + 0.5);
                    var distanceSquared2 = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared2 < distanceSquared) {
                        distanceSquared = distanceSquared2;
                    }
                }
            }

            return distanceSquared;
        }

        @Override
        public int hashCode() {
            return inventory.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null &&
                    (obj instanceof ContainerInfo && ((ContainerInfo) obj).inventory == inventory);
        }
    }
}
