package re.domi.doors;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import re.domi.doors.config.Config;
import re.domi.doors.config.ConfigurationPacket;
import re.domi.doors.config.EffectiveConfig;
import re.domi.doors.mixin.ClientPlayerInteractionManagerMixin;

public class ConnectedDoorsClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ClientConfigurationConnectionEvents.INIT.register((handler, client) ->
            ConnectedDoors.resetEffectiveConfig(false));

        ClientConfigurationNetworking.registerGlobalReceiver(ConfigurationPacket.ID, (payload, context) ->
        {
            EffectiveConfig.serverModPresent = true;
            EffectiveConfig.connectDoors = payload.connectDoors();
            EffectiveConfig.connectFenceGates = payload.connectFenceGates();
            EffectiveConfig.connectedFenceGateLimit = payload.connectedFenceGateLimit();
        });

        ClientPlayConnectionEvents.INIT.register((handler, client) ->
        {
            ServerInfo serverInfo = handler.getServerInfo();

            if (serverInfo != null &&
                (hasBlacklistMatch(Config.serverIpBlacklist, serverInfo.address) || hasBlacklistMatch(Config.serverNameBlacklist, serverInfo.name)))
            {
                EffectiveConfig.connectDoors = false;
                EffectiveConfig.connectFenceGates = false;
            }
        });
    }

    @Environment(EnvType.CLIENT)
    public static void sendUsePacket(World world, Hand hand, BlockHitResult hit)
    {
        //noinspection ConstantConditions
        ((ClientPlayerInteractionManagerMixin)MinecraftClient.getInstance().interactionManager).callSendSequencedPacket((ClientWorld)world, i -> new PlayerInteractBlockC2SPacket(hand, hit, i));
    }

    private static boolean hasBlacklistMatch(String blacklist, String toMatch)
    {
        if (blacklist.isEmpty()) return false;

        for (String entry : blacklist.split(";"))
        {
            if (entry.equalsIgnoreCase(toMatch))
            {
                return true;
            }
        }

        return false;
    }
}
