package me.flashyreese.mods.smartofflinemode.server.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.util.UUID;

public class Account {
    private final String uuid;
    private String password;
    private String ipAddress;

    private Account(UUID uuid, String password){
        this.uuid = uuid.toString();
        this.password = password;
        this.ipAddress = "";
    }

    public static Account of(UUID uuid, String password) {
        return new Account(uuid, password);
    }

    public static Account create(UUID uuid, String password) {
        return new Account(uuid, BCrypt.withDefaults().hashToString(12, password.toCharArray()));
    }

    public UUID getUUID() {
        return UUID.fromString(this.uuid);
    }

    public String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
