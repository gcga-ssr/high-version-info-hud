package com.example.blockinfo.network;

import com.example.blockinfo.BlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 熔炉状态同步包（服务端 -> 客户端）。 */
public class FurnaceDataPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FurnaceDataPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BlockInfo.MOD_ID, "furnace_data"));

    public static final StreamCodec<FriendlyByteBuf, FurnaceDataPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.pos);
                buf.writeInt(packet.cookTime);
                buf.writeInt(packet.cookTotal);
                buf.writeInt(packet.burnTime);
                buf.writeInt(packet.burnTotal);
                buf.writeInt(packet.inputCount);
                buf.writeUtf(packet.inputName);
                buf.writeInt(packet.fuelCount);
                buf.writeUtf(packet.fuelName);
                buf.writeInt(packet.outputCount);
                buf.writeUtf(packet.outputName);
            },
            (buf) -> new FurnaceDataPacket(
                    buf.readBlockPos(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readUtf()
            )
    );

    public final BlockPos pos;
    public final int cookTime;
    public final int cookTotal;
    public final int burnTime;
    public final int burnTotal;
    public final int inputCount;
    public final String inputName;
    public final int fuelCount;
    public final String fuelName;
    public final int outputCount;
    public final String outputName;

    public FurnaceDataPacket(BlockPos pos, int cookTime, int cookTotal, int burnTime, int burnTotal,
                             int inputCount, String inputName, int fuelCount, String fuelName,
                             int outputCount, String outputName) {
        this.pos = pos;
        this.cookTime = cookTime;
        this.cookTotal = cookTotal;
        this.burnTime = burnTime;
        this.burnTotal = burnTotal;
        this.inputCount = inputCount;
        this.inputName = inputName;
        this.fuelCount = fuelCount;
        this.fuelName = fuelName;
        this.outputCount = outputCount;
        this.outputName = outputName;
    }

    @Override
    public CustomPacketPayload.Type<FurnaceDataPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // 只有客户端会收到 playToClient 的包；这里的 dist 判断是为了让
            // 专用服务器永远不会去解析 ClientFurnaceCache 这个客户端类。
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.example.blockinfo.client.ClientFurnaceCache.updateData(
                        pos, cookTime, cookTotal, burnTime, burnTotal,
                        inputCount, inputName, fuelCount, fuelName, outputCount, outputName
                );
            }
        });
    }
}
