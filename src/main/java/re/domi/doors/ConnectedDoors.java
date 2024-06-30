package re.domi.doors;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import re.domi.doors.config.Config;
import re.domi.doors.config.ConfigurationPacket;
import re.domi.doors.config.EffectiveConfig;

public class ConnectedDoors implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Config.read();
        resetEffectiveConfig(true);

        PayloadTypeRegistry.configurationS2C().register(ConfigurationPacket.ID, ConfigurationPacket.CODEC);

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) ->
        {
            if (ServerConfigurationNetworking.canSend(handler, ConfigurationPacket.ID))
            {
                ServerConfigurationNetworking.send(handler, new ConfigurationPacket(
                    Config.connectDoors,
                    Config.connectFenceGates,
                    Config.connectedFenceGateLimit
                ));
            }
        });
    }

    public static void resetEffectiveConfig(boolean isServer)
    {
        EffectiveConfig.serverModPresent = isServer;

        EffectiveConfig.connectDoors = Config.connectDoors;
        EffectiveConfig.connectFenceGates = Config.connectFenceGates;
        EffectiveConfig.connectedFenceGateLimit = Config.connectedFenceGateLimit;
    }
}
