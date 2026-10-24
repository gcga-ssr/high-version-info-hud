package com.example.blockinfo.client;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientContainerCache {
    public static class ContainerData {
        public List<String> itemNames;
        public List<Integer> itemCounts;
        public long lastUpdateTime;
    }

    private static final Map<BlockPos, ContainerData> CACHE = new HashMap<>();

    private ClientContainerCache() {
    }

    public static void updateData(BlockPos pos, List<String> itemNames, List<Integer> itemCounts) {
        ContainerData data = CACHE.computeIfAbsent(pos, k -> new ContainerData());
        data.itemNames = itemNames;
        data.itemCounts = itemCounts;
        data.lastUpdateTime = System.currentTimeMillis();
    }

    public static ContainerData getData(BlockPos pos) {
        return CACHE.get(pos);
    }

    public static void clear() {
        CACHE.clear();
    }
}
