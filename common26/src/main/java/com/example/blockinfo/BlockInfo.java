package com.example.blockinfo;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * 合并后的唯一入口。
 *
 * <p>客户端与服务端逻辑现在在同一个 jar 里：
 * <ul>
 *   <li>网络通道与客户端 HUD 只在客户端真正工作；</li>
 *   <li>服务端同步处理器在专用服务器和单人游戏的集成服务器上都会注册；</li>
 *   <li>所有通道都是 optional 的，因此任意一侧没装本模组时连接都不会被拒绝。</li>
 * </ul>
 */
@Mod(BlockInfo.MOD_ID)
public class BlockInfo {
    public static final String MOD_ID = "blockinfo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlockInfo(IEventBus modEventBus) {
        modEventBus.addListener(BlockInfoNetwork::registerPayloadHandlers);

        // 服务端同步：专用服务器和单人游戏的集成服务器都需要，所以不按 dist 过滤。
        // ServerTickEvent 只会在逻辑服务端触发，纯客户端上这个处理器永远不会被调用。
        NeoForge.EVENT_BUS.register(new com.example.blockinfo.server.ServerSyncHandler());

        // 客户端部分只在客户端初始化。JVM 到这一行才会去解析 ClientBootstrap，
        // 所以专用服务器不会加载 Minecraft 的客户端类（Minecraft / GuiGraphicsExtractor 等）。
        if (com.example.blockinfo.compat.SideCompat.isClient()) {
            com.example.blockinfo.client.ClientBootstrap.init();
        }
    }
}
