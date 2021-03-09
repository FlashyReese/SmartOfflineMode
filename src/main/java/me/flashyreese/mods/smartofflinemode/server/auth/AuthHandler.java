package me.flashyreese.mods.smartofflinemode.server.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mojang.authlib.GameProfile;
import me.flashyreese.mods.smartofflinemode.server.auth.database.LMDB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthHandler {

    private final LMDB lmdb;
    private final EventHandler eventHandler;
    private final PlayerStateManager playerStateManager;

    private final List<GameProfile> unauthenticated = new ArrayList<>();

    public AuthHandler(File file) {
        this.lmdb = new LMDB(file);
        this.eventHandler = new EventHandler(this);
        this.playerStateManager = new PlayerStateManager();
    }

    public Account getAccount(GameProfile profile) {
        return this.lmdb.getAccount(profile.getId());
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

        Account account = Account.create(gameProfile.getId(), password);
        account.setIpAddress(ip);
        this.lmdb.addAccount(account);
        this.authenticateAccount(gameProfile, password, ip);
        return true;
    }

    public boolean unregisterAccount(GameProfile gameProfile) {
        if (!this.isRegistered(gameProfile)) return false;

        Account account = this.getAccount(gameProfile);
        this.lmdb.deleteAccount(account);
        return true;
    }

    public boolean authenticateAccount(GameProfile gameProfile, String password, String ip) {
        if (!this.isRegistered(gameProfile)) return false;

        Account account = this.getAccount(gameProfile);
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), account.getPassword());
        if (result.verified) {
            account.setIpAddress(ip);
            this.lmdb.addAccount(account);
            this.authenticateProfile(gameProfile);
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
        this.lmdb.addAccount(account);
        this.addUnauthenticated(gameProfile);
        return true;
    }

    public void addUnauthenticated(GameProfile profile) {
        Optional<GameProfile> optionalGameProfile = this.unauthenticated.stream().filter(gameProfile -> gameProfile.getId().toString().equals(profile.getId().toString()) && gameProfile.getName().equals(profile.getName())).findFirst();
        if (!optionalGameProfile.isPresent()) {
            this.unauthenticated.add(profile);
        }
    }

    public void changeAccountPassword(GameProfile gameProfile, String newPass) {
        Account updatedAccount = this.lmdb.getAccount(gameProfile.getId());
        updatedAccount.setPassword(BCrypt.withDefaults().hashToString(12, newPass.toCharArray()));
        this.lmdb.addAccount(updatedAccount);
    }

    public boolean isValidPassword(GameProfile gameProfile, String password) {
        Account account = this.lmdb.getAccount(gameProfile.getId());
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), account.getPassword());
        return result.verified;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    public List<GameProfile> getUnauthenticated() {
        return unauthenticated;
    }
}
