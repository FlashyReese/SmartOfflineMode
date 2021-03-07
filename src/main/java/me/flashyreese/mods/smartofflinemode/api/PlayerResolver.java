package me.flashyreese.mods.smartofflinemode.api;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class PlayerResolver {

    //Todo: Should use mojang's endpoint but only supports name, not uuid
    public static final String API_ENDPOINT = "https://playerdb.co/api/player/minecraft";

    public static Optional<Result> findPlayer(String nameOrUUID) {
        Optional<Result> optionalResult;
        try {
            URL url = new URL(String.format("%s/%s", API_ENDPOINT, nameOrUUID));
            URLConnection urlConnection = url.openConnection();
            urlConnection.addRequestProperty("User-Agent", "Mozilla");
            urlConnection.setReadTimeout(5000);
            urlConnection.setConnectTimeout(5000);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))) {
                optionalResult = Optional.of(new Gson().fromJson(reader, Result.class));
            }catch (Exception e){
                //e.printStackTrace();
                optionalResult = Optional.empty();
            }
        }catch (IOException e){
            e.printStackTrace();
            optionalResult = Optional.empty();
        }
        return optionalResult;
    }

    public static boolean isOnlineAccount(String nameOrUUID) {
        Optional<Result> optionalResult = findPlayer(nameOrUUID);
        if (optionalResult.isPresent()){
            Result result = optionalResult.get();
            return result.success;
        }
        return false;
    }

    public static boolean isOnlineAccount(GameProfile gameProfile) {
        return (gameProfile.getName() != null && isOnlineAccount(gameProfile.getName())) || (gameProfile.getId() != null && isOnlineAccount(gameProfile.getId().toString()));
    }

    public static UUID getUUID(String name) {
        Optional<Result> optionalResult = findPlayer(name);
        if (optionalResult.isPresent()){
            Result result = optionalResult.get();
            if (result.success) {
                return UUID.fromString(result.data.player.id);
            }
        }
        return UUID.nameUUIDFromBytes(String.format("OfflinePlayer:%s", name).getBytes(StandardCharsets.UTF_8));
    }

    public static GameProfile getGameProfile(String name) {
        Optional<Result> optionalResult = findPlayer(name);
        if (optionalResult.isPresent()){
            Result result = optionalResult.get();
            if (result.success) {
                return new GameProfile(UUID.fromString(result.data.player.id), name);
            }
        }
        return new GameProfile(null, name);
    }

    private static class Result{
        private String code;
        private String message;
        private ResultData data;
        private boolean success;
        private boolean error;
    }

    private static class ResultData{
        private ResultDataPlayer player;
    }

    private static class ResultDataPlayer{
        //Missing meta
        private String username;
        private String id;
        private String raw_id;
        private String avatar;
    }
}
