package re.domi.doors;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ConnectedDoorsClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            ConnectedDoors.modPresent = ClientPlayNetworking.canSend(ConnectedDoors.PACKET_ID);
        });
    }
}
