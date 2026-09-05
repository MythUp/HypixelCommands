package com.mythup.hypixelcommands;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import com.mythup.hypixelcommands.brigadier.HypixelCommandDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HypixelCommandsMod implements ClientModInitializer {
    public static final String MOD_ID = "hypixelcommands";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing HypixelCommands mod...");
        HypixelCommandDispatcher.buildTree();
        LOGGER.info("HypixelCommands mod initialized.");
    }

    public static boolean isHypixel() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return false;
        }
        var server = client.getCurrentServer();
        if (server == null || server.ip == null) {
            return false;
        }
        return server.ip.toLowerCase().contains("hypixel.net");
    }
}
