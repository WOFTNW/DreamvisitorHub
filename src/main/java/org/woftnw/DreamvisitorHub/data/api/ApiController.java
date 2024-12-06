package org.woftnw.DreamvisitorHub.data.api;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.woftnw.DreamvisitorHub.data.SessionData;
import org.woftnw.DreamvisitorHub.data.functions.BukkitConsole;
import org.woftnw.DreamvisitorHub.data.storage.StorageManager;

@RestController
@RequestMapping(path = "/api", value = {"json/test"})
public class ApiController {

    @GetMapping("/stats")
    public String receiveStats(@RequestParam @NotNull String data) {

        System.out.println("data: " + data);

        JsonObject jsonObject = new JsonObject().getAsJsonObject(data);

        SessionData.playerCount = jsonObject.get("playerCount").getAsInt();
        SessionData.maxPlayerCount = jsonObject.get("maxPlayerCount").getAsInt();
        SessionData.autoRestart = jsonObject.get("autoRestart").getAsBoolean();
        SessionData.newResourcePack = jsonObject.get("newResourcePack").getAsBoolean();
        BukkitConsole.addLine(jsonObject.get("consoleLine").toString());
        // TODO: move to /config context
//        StorageManager.setPauseChat(jsonObject.get("chatPause").getAsBoolean());

        return "Data received: " + data;
    }

    @GetMapping("/config")
    public String receiveConfig(@RequestParam @NotNull String data) {

        System.out.println("data: " + data);

        JsonObject jsonObject = new JsonObject().getAsJsonObject(data);

        SessionData.playerCount = jsonObject.get("playerCount").getAsInt();
        SessionData.maxPlayerCount = jsonObject.get("maxPlayerCount").getAsInt();
        SessionData.autoRestart = jsonObject.get("autoRestart").getAsBoolean();
        SessionData.newResourcePack = jsonObject.get("newResourcePack").getAsBoolean();
        BukkitConsole.addLine(jsonObject.get("consoleLine").toString());
        // TODO: move to /config context
        StorageManager.setPauseChat(jsonObject.get("chatPause").getAsBoolean());

        return "Data received: " + data;
    }

}
