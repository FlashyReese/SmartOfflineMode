package me.flashyreese.mods.smartofflinemode.server.util;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class PlayerResolver {

    private static final Long2BooleanOpenHashMap playerCacheByUUID = new Long2BooleanOpenHashMap();
    private static final Object2BooleanOpenHashMap<String> playerCacheByName = new Object2BooleanOpenHashMap<>();

    public static final String API_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft";
    public static final String SESSION_ENDPOINT = "https://sessionserver.mojang.com/session/minecraft/profile";

    public static Optional<Result> getPlayer(String name) {
        return getResult(API_ENDPOINT, name);
    }

    public static Optional<Result> getPlayer(UUID uuid) {
        return getResult(SESSION_ENDPOINT, uuid.toString());
    }

    public static Optional<Result> getResult(String endPoint, String string) {
        Optional<Result> optionalResult;
        try {
            URL url = new URL(String.format("%s/%s", endPoint, string));
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(5000);
            urlConnection.setConnectTimeout(5000);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))) {
                optionalResult = Optional.of(new Gson().fromJson(reader, Result.class));
            } catch (Exception e) {
                optionalResult = Optional.empty();
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
            optionalResult = Optional.empty();
        }
        return optionalResult;
    }

    public static boolean isOnlineAccount(String name) {
        if (playerCacheByName.containsKey(name)){
            return playerCacheByName.getBoolean(name);
        } else {
            Optional<Result> optionalResult = getPlayer(name);
            if (optionalResult.isPresent()){
                boolean isOnline = optionalResult.get().error == null;
                playerCacheByName.put(name, isOnline);
                return isOnline;
            }
        }
        return false;
    }

    public static boolean isOnlineAccount(UUID uuid) {
        long uuidLong = uuid.getMostSignificantBits();
        if (playerCacheByUUID.containsKey(uuidLong)){
            return playerCacheByUUID.get(uuidLong);
        } else {
            Optional<Result> optionalResult = getPlayer(uuid);
            if (optionalResult.isPresent()){
                boolean isOnline = optionalResult.get().error == null;
                playerCacheByUUID.put(uuidLong, isOnline);
                return isOnline;
            }
        }
        return false;
    }

    public static boolean isOnlineAccount(GameProfile gameProfile) {
        return (gameProfile.getName() != null && isOnlineAccount(gameProfile.getName())) || (gameProfile.getId() != null && isOnlineAccount(gameProfile.getId()));
    }

    public static GameProfile getGameProfile(String name) {
        Optional<Result> optionalResult = getPlayer(name);
        if (optionalResult.isPresent()) {
            Result result = optionalResult.get();
            if (result.error == null) {
                return new GameProfile(UUID.fromString(result.id), name);
            }
        }
        return new GameProfile(null, name);
    }

    private static class Result {
        private String name;
        private String id;
        private String error;
        private String errorMessage;
    }
}
