package com.example.blockinfo.network;

import com.example.blockinfo.BlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** 容器内容同步包（服务端 -> 客户端）。 */
public class ContainerDataPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ContainerDataPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BlockInfo.MOD_ID, "container_data"));

    public static final StreamCodec<FriendlyByteBuf, ContainerDataPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.itemNames.size());
                for (int i = 0; i < packet.itemNames.size(); i++) {
                    buf.writeUtf(packet.itemNames.get(i));
                    buf.writeInt(packet.itemCounts.get(i));
                }
            },
            (buf) -> {
                BlockPos pos = buf.readBlockPos();
                int count = buf.readInt();
                List<String> itemNames = new ArrayList<>();
                List<Integer> itemCounts = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    itemNames.add(buf.readUtf());
                    itemCounts.add(buf.readInt());
                }
                return new ContainerDataPacket(pos, itemNames, itemCounts);
            }
    );

    public final BlockPos pos;
    public final List<String> itemNames;
    public final List<Integer> itemCounts;

    public ContainerDataPacket(BlockPos pos, List<String> itemNames, List<Integer> itemCounts) {
        this.pos = pos;
        this.itemNames = itemNames;
        this.itemCounts = itemCounts;
    }

    @Override
    public CustomPacketPayload.Type<ContainerDataPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                com.example.blockinfo.client.ClientContainerCache.updateData(pos, itemNames, itemCounts);
            }
        });
    }
}
