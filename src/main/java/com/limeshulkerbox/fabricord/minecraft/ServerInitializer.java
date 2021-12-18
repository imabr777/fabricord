package com.limeshulkerbox.fabricord.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.limeshulkerbox.fabricord.discord.ChatThroughDiscord;
import com.limeshulkerbox.fabricord.discord.UpdateConfigsCommand;
import com.limeshulkerbox.fabricord.discord.UpdateConfigsInterface;
import com.limeshulkerbox.fabricord.minecraft.events.*;
import com.limeshulkerbox.fabricord.other.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Environment(EnvType.SERVER)
public class ServerInitializer implements DedicatedServerModInitializer, UpdateConfigsInterface {

    public static Config config;
    public static boolean jdaReady = false;
    static JDA api;
    static Path configPath = Paths.get(FabricLoader.getInstance().getConfigDir() + "/limeshulkerbox/fabricord.json");
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final UUID modUUID = UUID.fromString("0c4fc385-8b46-4ef2-8375-fcd19d71f45e");
    public static final int messageSplitterAmount = 2000;

    public static void stopDiscordBot() {
        if (api == null) return;
        jdaReady = false;
        api.shutdown();
    }

    public static JDA getDiscordApi() {
        return api;
    }

    //Method to grab stuff from the config file
    @Override
    public void updateConfigs() {
        try {
            String contents = Files.readString(configPath);
            config = gson.fromJson(contents, Config.class);
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
            config = new Config("",
                    "",
                    false,
                    "",
                    true,
                    true,
                    false,
                    "",
                    false,
                    true,
                    true,
                    true,
                    false,
                    "",
                    "Server starting",
                    "Server started",
                    "Server stopping",
                    "Server stopped",
                    new String[0],
                    new String[0]);
        }
    }

    @Override
    public void onInitializeServer() {

        new UpdateConfigsCommand();

        //Create default config file
        try {
            if (!Files.exists(configPath)) {
                if (!Files.exists(configPath.getParent())) {
                    Files.createDirectory(configPath.getParent());
                }
                String contents =
                        """
                                {
                                    "botToken": "",
                                    "commandsAccessRoleID": "",
                                    
                                    "chatEnabled": "false",
                                    "chatChannelID": "",
                                    "commandsInChatChannel": "true",
                                    "promptsEnabled": "true",
                                    
                                    "consoleEnabled": "false",
                                    "consoleChannelID": "",
                                    "showInfoLogsInConsole": "true",
                                    "showWarnLogsInConsole": "true",
                                    "showErrorLogsInConsole": "true",
                                    "showDebugLogsInConsole": "false",
                                    
                                    "webhooksEnabled": "false",
                                    "webhookURL": "",
                                    
                                    "serverStartingPrompt": "Server starting",
                                    "serverStartedPrompt": "Server started",
                                    "serverStoppingPrompt": "Server stopping",
                                    "serverStoppedPrompt": "Server stopped"
                                    
                                    "commandsForEveryone": [""],
                                    "keysToSendToDiscord": [""]
                                }
                                        """;
                Files.writeString(configPath, contents);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Get the configs from the json
        updateConfigs();

        if (!config.getBotToken().equals("")) {
            //Make the Discord bot come alive
            try {
                api = JDABuilder.createDefault(config.getBotToken()).addEventListeners(new ChatThroughDiscord()).build();
            } catch (LoginException e) {
                e.printStackTrace();
            }

            //Register and make appender
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ConsoleAppender consoleAppender = new ConsoleAppender();
            consoleAppender.start();
            ctx.getRootLogger().addAppender(consoleAppender);
            ctx.updateLoggers();

            //Register prompt events
            ServerLifecycleEvents.SERVER_STARTING.register(new GetServerStartingEvent());
            ServerLifecycleEvents.SERVER_STARTED.register(new GetServerStartedEvent());
            ServerLifecycleEvents.SERVER_STOPPING.register(new GetServerStoppingEvent());
            ServerLifecycleEvents.SERVER_STOPPED.register(new GetServerStoppedEvent());
        }
    }
}