package com.example.blockinfo;

import com.example.blockinfo.network.ContainerDataPacket;
import com.example.blockinfo.network.FurnaceDataPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 网络通道注册（客户端与服务端共用同一份定义）。 */
public final class BlockInfoNetwork {
    /** 通道协议版本，编码格式变更时才需要改。 */
    public static final String PROTOCOL_VERSION = "1";

    private BlockInfoNetwork() {
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        // optional() 很关键：
        //  - 客户端装、服务端不装 —— 否则客户端会被 “服务端缺少此客户端需要的网络通道” 踢掉；
        //  - 服务端装、客户端不装 —— 否则服务器的原版客户端全部进不来。
        // 标记为可选后，对端缺失时只是收不到同步数据。
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();

        registrar.playToClient(
                FurnaceDataPacket.TYPE,
                FurnaceDataPacket.STREAM_CODEC,
                FurnaceDataPacket::handle
        );
        registrar.playToClient(
                ContainerDataPacket.TYPE,
                ContainerDataPacket.STREAM_CODEC,
                ContainerDataPacket::handle
        );
    }
}
