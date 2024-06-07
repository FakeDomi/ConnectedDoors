package re.domi.doors;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

import static re.domi.doors.ConnectedDoors.DoorPacket.PACKET_CODEC;

public class ConnectedDoors implements ModInitializer
{
    public static final Identifier PACKET_ID = Identifier.of("connected-doors", "hello");

    public static boolean serverModPresent = true;

    public record DoorPacket(UUID door) implements CustomPayload {
        public static final CustomPayload.Id<DoorPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of("connected-doors", "hello"));
        public static final PacketCodec<RegistryByteBuf, DoorPacket> PACKET_CODEC = Uuids.PACKET_CODEC.xmap(DoorPacket::new, DoorPacket::door).cast();

        @Override
        public Id<? extends CustomPayload> getId() {
            return null;
        }
    }

    @Override
    public void onInitialize()
    {
        PayloadTypeRegistry.playC2S().register(DoorPacket.PACKET_ID, PACKET_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DoorPacket.PACKET_ID, (payload, context) -> { });
    }
}
