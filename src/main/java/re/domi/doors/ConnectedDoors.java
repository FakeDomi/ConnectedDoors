package re.domi.doors;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class ConnectedDoors implements ModInitializer
{
    public static final Identifier PACKET_ID = new Identifier("connected-doors", "hello");

    public static boolean serverModPresent = true;

    @Override
    public void onInitialize()
    {
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, (server, player, handler, buf, responseSender) -> { });
    }
}
