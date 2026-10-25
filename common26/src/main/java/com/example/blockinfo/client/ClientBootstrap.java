package com.example.blockinfo.client;

import net.neoforged.neoforge.common.NeoForge;

/**
 * 客户端专用初始化。
 *
 * <p>只会在 {@code FMLEnvironment.getDist() == Dist.CLIENT} 时被 {@code BlockInfo} 调用，
 * 因此专用服务器永远不会解析这个类以及它引用的客户端类。
 */
public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.register(BlockInfoTracker.class);
        NeoForge.EVENT_BUS.register(BlockInfoRenderer.class);
    }
}
