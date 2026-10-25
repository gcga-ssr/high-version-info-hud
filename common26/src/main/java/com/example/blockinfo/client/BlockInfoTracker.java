package com.example.blockinfo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockInfoTracker {
    private static final int UPDATE_INTERVAL = 2;

    private static final BlockInfoData CURRENT_DATA = new BlockInfoData();
    private static final Map<String, String> ITEM_NAME_CACHE = new HashMap<>();

    private static BlockPos lastPos = BlockPos.ZERO;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            CURRENT_DATA.clear();
            lastPos = BlockPos.ZERO;
            return;
        }

        HitResult hitResult = mc.hitResult;
        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos pos = blockHit.getBlockPos();

            tickCounter++;
            if (!pos.equals(lastPos) || tickCounter % UPDATE_INTERVAL == 0) {
                lastPos = pos.immutable();
                BlockState state = mc.level.getBlockState(pos);
                Block block = state.getBlock();
                BlockEntity blockEntity = mc.level.getBlockEntity(pos);
                collectBlockInfo(CURRENT_DATA, state, block, blockEntity, pos);
            }

            // 必须在 collectBlockInfo() 之后更新挖掘状态：
            // collectBlockInfo() 内部会调用 data.clear()，把 mineProgress / isMining 归零。
            // 之前这个赋值写在前面，于是每 UPDATE_INTERVAL(2) 个 tick 就被抹掉一次，
            // 进度（以及 HUD 的高度/宽度）在 0 和真实值之间以 10Hz 来回跳 —— 这就是
            // “挖掘时显示框闪烁”的根因。
            updateMiningState(mc, CURRENT_DATA);
        } else {
            CURRENT_DATA.clear();
            lastPos = BlockPos.ZERO;
        }
    }

    /**
     * 更新“是否正在挖掘”与挖掘进度。
     *
     * <p>是否显示进度条由两个信号共同决定：
     * <ul>
     *   <li>{@code gameMode.isDestroying()} —— 服务端认可的挖掘状态；</li>
     *   <li>{@code options.keyAttack.isDown()} —— 玩家正按住左键。</li>
     * </ul>
     * 只用前者不行：每挖破一块 {@code isDestroying} 会变 false 约 5 tick（连挖时进度条
     * 会随之闪一下），而且创造模式瞬破时它根本不会变 true。只用后者则在手柄等输入下失效。
     */
    private static void updateMiningState(Minecraft mc, BlockInfoData data) {
        if (mc.gameMode == null || mc.player == null) {
            data.isMining = false;
            data.mineProgress = 0f;
            return;
        }

        boolean destroying = mc.gameMode.isDestroying();
        boolean holdingAttack = mc.options != null && mc.options.keyAttack.isDown();
        data.isMining = destroying || holdingAttack;

        if (!data.isMining) {
            data.mineProgress = 0f;
            return;
        }

        // getDestroyStage(): 0..10 的档位，未在挖掘时为 -1。
        int stage = mc.gameMode.getDestroyStage();
        data.mineProgress = (stage >= 0 && stage <= 10) ? stage / 10.0f : 0f;
    }

    private static void collectBlockInfo(BlockInfoData data, BlockState state, Block block,
                                         BlockEntity blockEntity, BlockPos pos) {
        data.clear();

        data.blockName = block.getName().getString();
        data.modId = state.getBlock().builtInRegistryHolder().key().identifier().getNamespace();
        data.isVanilla = data.modId.equals("minecraft");
        data.blockPos = pos;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            data.canMine = mc.player.hasCorrectToolForDrops(state);
        }

        detectPlant(data, state);

        if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            data.isFurnace = true;
            ClientFurnaceCache.FurnaceData cacheData = ClientFurnaceCache.getData(pos);
            if (cacheData != null) {
                data.furnaceBurnTime = cacheData.burnTime;
                data.furnaceTotalBurnTime = cacheData.burnTotal;
                data.furnaceCookTime = cacheData.cookTime;
                data.furnaceTotalCookTime = cacheData.cookTotal;
                data.inputItemName = getCachedItemName(cacheData.inputName);
                data.inputItemCount = cacheData.inputCount;
                data.fuelItemName = getCachedItemName(cacheData.fuelName);
                data.fuelItemCount = cacheData.fuelCount;
                data.outputItemName = getCachedItemName(cacheData.outputName);
                data.outputItemCount = cacheData.outputCount;
            }
        }

        if (blockEntity instanceof BaseContainerBlockEntity container
                && !(blockEntity instanceof AbstractFurnaceBlockEntity)) {
            data.isContainer = true;
            ClientContainerCache.ContainerData cacheData = ClientContainerCache.getData(pos);
            if (cacheData != null && cacheData.itemNames != null && !cacheData.itemNames.isEmpty()) {
                List<String> items = new java.util.ArrayList<>();
                for (int i = 0; i < cacheData.itemNames.size(); i++) {
                    String displayName = getCachedItemName(cacheData.itemNames.get(i));
                    items.add(displayName + " x" + cacheData.itemCounts.get(i));
                }
                data.containerItems = items;
            } else {
                data.containerItems = collectContainerItems(container);
            }
        }
    }

    private static String getCachedItemName(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return "";
        }
        return ITEM_NAME_CACHE.computeIfAbsent(itemId, BlockInfoTracker::getItemNameFromId);
    }

    private static void detectPlant(BlockInfoData data, BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty intProp) {
                String name = intProp.getName();
                if (name.equals("age") || name.equals("stage") || name.equals("growth")
                        || name.equals("level") || name.equals("height")) {
                    data.isPlant = true;
                    Integer value = state.getValue(intProp);
                    int actualMax = intProp.getPossibleValues().stream()
                            .mapToInt(Integer::intValue).max().orElse(7);
                    if (actualMax > 0) {
                        data.plantGrowth = (float) value / actualMax;
                    }
                    return;
                }
            }
        }
    }

    private static List<String> collectContainerItems(BaseContainerBlockEntity container) {
        Map<String, Integer> itemCounts = new HashMap<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            if (!stack.isEmpty()) {
                String name = stack.getDisplayName().getString();
                itemCounts.put(name, itemCounts.getOrDefault(name, 0) + stack.getCount());
            }
        }
        return itemCounts.entrySet().stream()
                .map(e -> e.getKey() + " x" + e.getValue())
                .collect(java.util.stream.Collectors.toList());
    }

    public static BlockInfoData getCurrentData() {
        return CURRENT_DATA;
    }

    private static String getItemNameFromId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return "";
        }
        try {
            // 刻意不用 BuiltInRegistries.ITEM.get(id)：它在 1.21.1 返回 Item（找不到为 null），
            // 从 1.21.2 起改成返回 Optional<Holder.Reference<Item>>，跨版本签名不一致。
            // 直接遍历注册表，只依赖 Registry 的 Iterable 与 getKey(T)，整条 1.21.x / 26.x 都稳。
            // 结果会被 ITEM_NAME_CACHE 缓存，所以这次 O(n) 扫描每个 id 只发生一次。
            for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString().equals(itemId)) {
                    return item.getDefaultInstance().getDisplayName().getString();
                }
            }
        } catch (Exception ignored) {
            // 物品名解析失败时退回原始 id
        }
        return itemId;
    }
}