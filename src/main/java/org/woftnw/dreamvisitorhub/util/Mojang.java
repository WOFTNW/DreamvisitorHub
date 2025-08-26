package org.woftnw.dreamvisitorhub.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class Mojang {

    /**
     * Retrieves the current UUID linked to a username.
     *
     * @param username the username
     * @return the {@link UUID}
     */
    @NotNull
    public static UUID getUuidOfUsername(String username) throws IOException {
        return UUID.fromString(addHyphensToUuid(getJSONObject("https://api.mojang.com/users/profiles/minecraft/" + username).get("id").getAsString()));
    }

    /**
     * Adds the hyphens back into a String UUID.
     * @param uuid the UUID as a {@link String} without hyphens.
     * @return a UUID as a string with hyphens.
     */
    @Contract(pure = true)
    public static @NotNull String addHyphensToUuid(@NotNull String uuid) throws NullPointerException {
        return uuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5");
    }

    /**
     * Retrieves the current username linked to a UUID.
     *
     * @param uuid the UUID
     * @return the username
     */
    public static String getUsernameOfUuid(String uuid) throws IOException {
        return getJSONObject("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).get("name").getAsString();
    }

    /**
     * Retrieves the current skin of a UUID.
     *
     * @param uuid the UUID
     * @return the skin's URL
     */
    @Nullable
    public static String getSkinUrl(String uuid) throws IOException {
        JsonObject obj = getJSONObject("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        JsonArray properties = obj.get("properties").getAsJsonArray();
        for (JsonElement jsonElement : properties.asList()) {
            JsonObject prop = jsonElement.getAsJsonObject();

            String propName = prop.get("name").getAsString();
            String propValue = prop.get("value").getAsString();
            if (propName.equals("textures")) {
                JsonObject texture = JsonParser.parseString(new String(Base64.getDecoder().decode(propValue))).getAsJsonObject();
                return texture.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString();
            }
        }
        return null;
    }

    @NotNull
    private static JsonObject getJSONObject(String urlString) throws IOException {
        JsonObject obj;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        obj = JsonParser.parseString(content.toString()).getAsJsonObject();
        System.out.println(obj.toString());
        String error;
        try {
            error = obj.get("errorMessage").getAsString();
            if (error != null) {
                throw new RuntimeException(error);
            }
        } catch (Exception ignored) {
            // This means no error! (Theoretically)
        }

        return obj;
    }

}
