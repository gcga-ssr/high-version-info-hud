package com.example.blockinfo.client;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class ClientFurnaceCache {
    public static class FurnaceData {
        public int cookTime;
        public int cookTotal;
        public int burnTime;
        public int burnTotal;
        public int inputCount;
        public String inputName;
        public int fuelCount;
        public String fuelName;
        public int outputCount;
        public String outputName;
        public long lastUpdateTime;
    }

    private static final Map<BlockPos, FurnaceData> CACHE = new HashMap<>();

    private ClientFurnaceCache() {
    }

    public static void updateData(BlockPos pos, int cookTime, int cookTotal, int burnTime, int burnTotal,
                                  int inputCount, String inputName, int fuelCount, String fuelName,
                                  int outputCount, String outputName) {
        FurnaceData data = CACHE.computeIfAbsent(pos, k -> new FurnaceData());
        data.cookTime = cookTime;
        data.cookTotal = cookTotal;
        data.burnTime = burnTime;
        data.burnTotal = burnTotal;
        data.inputCount = inputCount;
        data.inputName = inputName;
        data.fuelCount = fuelCount;
        data.fuelName = fuelName;
        data.outputCount = outputCount;
        data.outputName = outputName;
        data.lastUpdateTime = System.currentTimeMillis();
    }

    public static FurnaceData getData(BlockPos pos) {
        return CACHE.get(pos);
    }

    public static void clear() {
        CACHE.clear();
    }
}
