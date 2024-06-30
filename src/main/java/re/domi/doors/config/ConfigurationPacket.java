package re.domi.doors.config;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConfigurationPacket(boolean connectDoors, boolean connectFenceGates, int connectedFenceGateLimit) implements CustomPayload
{
    public static final CustomPayload.Id<ConfigurationPacket> ID = new CustomPayload.Id<>(Identifier.of("connected-doors", "configuration"));
    public static final PacketCodec<PacketByteBuf, ConfigurationPacket> CODEC = CustomPayload.codecOf(ConfigurationPacket::write, ConfigurationPacket::read);

    private void write(PacketByteBuf buf)
    {
        buf.writeBoolean(this.connectDoors);
        buf.writeBoolean(this.connectFenceGates);
        buf.writeInt(this.connectedFenceGateLimit);
    }

    private static ConfigurationPacket read(PacketByteBuf buf)
    {
        return new ConfigurationPacket(buf.readBoolean(), buf.readBoolean(), buf.readInt());
    }

    @Override
    public Id<? extends CustomPayload> getId()
    {
        return ID;
    }
}
