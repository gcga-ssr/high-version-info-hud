package com.example.blockinfo.client;

public class BlockInfoUtil {
    private BlockInfoUtil() {
    }

    public static String formatPercentage(float progress) {
        return String.format("%.0f%%", progress * 100);
    }
}
