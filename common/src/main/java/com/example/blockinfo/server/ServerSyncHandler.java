package com.example.blockinfo.server;

import com.example.blockinfo.BlockInfo;
import com.example.blockinfo.network.ContainerDataPacket;
import com.example.blockinfo.network.FurnaceDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端同步：每隔几个 tick 把玩家准星所指的熔炉 / 容器状态发给该玩家。
 *
 * <p>在专用服务器和单人游戏的集成服务器上都会注册（见 {@link BlockInfo}）。
 */
public class ServerSyncHandler {
    private static final int SYNC_INTERVAL = 5;

    private int tickCounter = 0;

    private static final Field COOKING_TIMER;
    private static final Field COOKING_TOTAL_TIME;
    private static final Field LIT_TIME_REMAINING;
    private static final Field LIT_TOTAL_TIME;

    // 兼容不同 MC 版本的字段名（Mojmap）：1.21.1 使用 cookingProgress/litTime/litDuration，
    // 更高版本重命名为 cookingTimer/litTimeRemaining/litTotalTime
    private static Field findField(String... names) {
        for (String name : names) {
            try {
                Field f = AbstractFurnaceBlockEntity.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    static {
        COOKING_TIMER = findField("cookingProgress", "cookingTimer");
        COOKING_TOTAL_TIME = findField("cookingTotalTime");
        LIT_TIME_REMAINING = findField("litTime", "litTimeRemaining");
        LIT_TOTAL_TIME = findField("litDuration", "litTotalTime");
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % SYNC_INTERVAL != 0) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            // serverLevel() 从 1.21.6 起改名为 level()。用 Entity.level() 再转换，
            // 1.21.1 ~ 26.3 全都能编译（新版返回类型是协变的 ServerLevel，转换是冗余但合法）。
            ServerLevel level = (ServerLevel) player.level();
            if (level == null) {
                continue;
            }

            HitResult hitResult = player.pick(5.0D, 0.0F, false);
            if (!(hitResult instanceof BlockHitResult blockHit)) {
                continue;
            }

            BlockPos pos = blockHit.getBlockPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
                syncFurnaceData(player, furnace, pos);
            } else if (blockEntity instanceof BaseContainerBlockEntity container) {
                syncContainerData(player, container, pos);
            }
        }
    }

    private void syncFurnaceData(ServerPlayer player, AbstractFurnaceBlockEntity furnace, BlockPos pos) {
        try {
            int cookTime = 0;
            int cookTotal = 200;
            int burnTime = 0;
            int burnTotal = 200;

            if (COOKING_TIMER != null) {
                cookTime = COOKING_TIMER.getInt(furnace);
            }
            if (COOKING_TOTAL_TIME != null) {
                cookTotal = COOKING_TOTAL_TIME.getInt(furnace);
            }
            if (LIT_TIME_REMAINING != null) {
                burnTime = LIT_TIME_REMAINING.getInt(furnace);
            }
            if (LIT_TOTAL_TIME != null) {
                burnTotal = LIT_TOTAL_TIME.getInt(furnace);
            }

            if (cookTotal <= 0) {
                cookTotal = 200;
            }
            if (burnTotal <= 0) {
                burnTotal = 200;
            }

            ItemStack input = furnace.getItem(0);
            ItemStack fuel = furnace.getItem(1);
            ItemStack output = furnace.getItem(2);

            FurnaceDataPacket packet = new FurnaceDataPacket(
                    pos, cookTime, cookTotal, burnTime, burnTotal,
                    input.getCount(), itemId(input),
                    fuel.getCount(), itemId(fuel),
                    output.getCount(), itemId(output)
            );
            PacketDistributor.sendToPlayer(player, packet);
        } catch (Exception e) {
            BlockInfo.LOGGER.error("同步熔炉数据失败: {}", e.getMessage());
        }
    }

    private void syncContainerData(ServerPlayer player, BaseContainerBlockEntity container, BlockPos pos) {
        try {
            Map<String, Integer> itemMap = new HashMap<>();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    String id = itemId(stack);
                    itemMap.put(id, itemMap.getOrDefault(id, 0) + stack.getCount());
                }
            }

            List<String> itemNames = new ArrayList<>();
            List<Integer> itemCounts = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : itemMap.entrySet()) {
                itemNames.add(entry.getKey());
                itemCounts.add(entry.getValue());
            }

            PacketDistributor.sendToPlayer(player, new ContainerDataPacket(pos, itemNames, itemCounts));
        } catch (Exception e) {
            BlockInfo.LOGGER.error("同步容器数据失败: {}", e.getMessage());
        }
    }

    private static String itemId(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getItem().builtInRegistryHolder().key().location().toString();
    }
}
