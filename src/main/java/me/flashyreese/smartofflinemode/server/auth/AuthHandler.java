package me.flashyreese.smartofflinemode.server.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthHandler {
    private final Gson gson = new Gson();

    private final File file;
    private final EventHandler eventHandler;

    private final List<Account> accounts = new ArrayList<>();
    private final List<GameProfile> unauthenticated = new ArrayList<>();

    public AuthHandler(File file) {
        this.file = file;
        this.eventHandler = new EventHandler(this);
        this.read();
    }

    public Account getAccount(GameProfile profile) {
        for (Account account : this.accounts) {
            if (account.getUUID().toString().equals(profile.getId().toString())) {
                return account;
            }
        }
        return null;
    }

    public boolean isLoggedIn(GameProfile gameProfile) {
        for (GameProfile gameProfile1 : this.getUnauthenticated()) {
            if (gameProfile.getId().toString().equals(gameProfile1.getId().toString())) {
                return false;
            }
        }
        return true;
    }

    public boolean isRegistered(GameProfile gameProfile) {
        return this.getAccount(gameProfile) != null;
    }

    public boolean registerAccount(GameProfile gameProfile, String password, String ip) {
        if (this.isRegistered(gameProfile)) return false;

        Account account = new Account(gameProfile.getId(), password);
        account.setIpAddress(ip);
        this.accounts.add(account);
        this.authenticateAccount(gameProfile, password, ip);
        this.writeChanges();
        return true;
    }

    public boolean unregisterAccount(GameProfile gameProfile) {
        if (!this.isRegistered(gameProfile)) return false;

        Account account = this.getAccount(gameProfile);
        this.accounts.remove(account);
        this.writeChanges();
        return true;
    }

    public boolean authenticateAccount(GameProfile gameProfile, String password, String ip) {
        if (!this.isRegistered(gameProfile)) return false;

        Account account = this.getAccount(gameProfile);
        if (account.isValidPassword(password)) {
            account.setIpAddress(ip);
            this.authenticateProfile(gameProfile);
            this.writeChanges();
            return true;
        }
        return false;
    }

    public boolean isLastIP(GameProfile gameProfile, String ip) {
        if (!this.isRegistered(gameProfile)) return false;

        Account account = this.getAccount(gameProfile);
        return account.getIpAddress().equals(ip);
    }

    protected void authenticateProfile(GameProfile profile) {
        Optional<GameProfile> optionalGameProfile = this.unauthenticated.stream().filter(gameProfile -> gameProfile.getId().toString().equals(profile.getId().toString()) && gameProfile.getName().equals(profile.getName())).findFirst();
        optionalGameProfile.ifPresent(this.unauthenticated::remove);
    }

    public boolean deauthenticateAccount(GameProfile gameProfile, boolean logout) {
        if (!this.isRegistered(gameProfile)) return false;

        Account account = this.getAccount(gameProfile);
        if (logout) {
            account.setIpAddress("");
        }
        this.unauthenticated.add(gameProfile);
        this.writeChanges();
        return true;
    }

    public List<GameProfile> getUnauthenticated() {
        return unauthenticated;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public void writeChanges() {
        try (FileWriter fw = new FileWriter(this.file)) {
            String json = this.gson.toJson(this.accounts);
            fw.write(json);
            fw.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void read() {
        if (!this.file.exists())
            this.writeChanges();

        try (FileReader reader = new FileReader(this.file)) {
            this.accounts.clear();
            this.accounts.addAll(this.gson.fromJson(reader, new TypeToken<List<Account>>() {
            }.getType()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
