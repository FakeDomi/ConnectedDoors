package re.domi.doors;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import re.domi.doors.mixin.ClientPlayerInteractionManagerMixin;

public class ConnectedDoorsClient implements ClientModInitializer
{
    public static boolean serverBlacklisted = false;

    @Override
    public void onInitializeClient()
    {
        Config.read();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
        {
            ConnectedDoors.serverModPresent = ClientPlayNetworking.canSend(ConnectedDoors.PACKET_ID);
            ConnectedDoorsClient.serverBlacklisted = false;

            ServerInfo serverInfo = handler.getServerInfo();

            if (serverInfo != null &&
                (hasBlacklistMatch(Config.serverIpBlacklist, serverInfo.address) || hasBlacklistMatch(Config.serverNameBlacklist, serverInfo.name)))
            {
                ConnectedDoorsClient.serverBlacklisted = true;
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
        if (blacklist.length() == 0) return false;

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
