package re.domi.doors.config;

import net.fabricmc.loader.api.FabricLoader;
import re.domi.doors.ConnectedDoors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config
{
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "connected-doors.properties");
    private static final String CONFIG_COMMENT = "Connected Doors config file";

    private static final String SERVER_IP_BLACKLIST = "serverIpBlacklist";
    private static final String SERVER_NAME_BLACKLIST = "serverNameBlacklist";
    private static final String CONNECT_DOORS = "connectDoors";
    private static final String CONNECT_FENCE_GATES = "connectFenceGates";
    private static final String CONNECTED_FENCE_GATE_LIMIT = "connectedFenceGateLimit";

    public static String serverIpBlacklist = "";
    public static String serverNameBlacklist = "";
    public static boolean connectDoors = true;
    public static boolean connectFenceGates = true;
    public static int connectedFenceGateLimit = 64;

    public static void read()
    {
        try
        {
            if (CONFIG_FILE.createNewFile())
            {
                write();
                return;
            }

            FileInputStream configInputStream = new FileInputStream(CONFIG_FILE);

            Properties properties = new Properties();
            properties.load(configInputStream);

            serverIpBlacklist = properties.getProperty(SERVER_IP_BLACKLIST, "");
            serverNameBlacklist = properties.getProperty(SERVER_NAME_BLACKLIST, "");
            connectDoors = "true".equals(properties.getProperty(CONNECT_DOORS, "true"));
            connectFenceGates = "true".equals(properties.getProperty(CONNECT_FENCE_GATES, "true"));
            connectedFenceGateLimit = Integer.parseInt(properties.getProperty(CONNECTED_FENCE_GATE_LIMIT, "64"));
        }
        catch (IOException e)
        {
            ConnectedDoors.LOGGER.error("Cannot read configuration file:", e);
        }
    }

    private static void write()
    {
        try
        {
            FileOutputStream outputStream = new FileOutputStream(CONFIG_FILE);

            Properties properties = new Properties();
            properties.setProperty(SERVER_IP_BLACKLIST, serverIpBlacklist);
            properties.setProperty(SERVER_NAME_BLACKLIST, serverNameBlacklist);
            properties.setProperty(CONNECT_DOORS, String.valueOf(connectDoors));
            properties.setProperty(CONNECT_FENCE_GATES, String.valueOf(connectFenceGates));
            properties.setProperty(CONNECTED_FENCE_GATE_LIMIT, String.valueOf(connectedFenceGateLimit));
            properties.store(outputStream, CONFIG_COMMENT);
        }
        catch (IOException e)
        {
            ConnectedDoors.LOGGER.error("Cannot write configuration file:", e);
        }
    }
}
