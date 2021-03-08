package me.flashyreese.mods.smartofflinemode.server.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.util.UUID;

public class Account {
    private final String uuid;
    private String password;
    private String ipAddress;

    public Account(UUID uuid, String password){
        this.uuid = uuid.toString();
        this.password = password;
        this.ipAddress = "";
    }

    public UUID getUUID() {
        return UUID.fromString(this.uuid);
    }

    public String getPassword() {
        return password;
    }

    private void setPassword(String password) {
        this.password = password;
    }

    public boolean isValidPassword(String password){
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), BCrypt.withDefaults().hashToChar(12, this.password.toCharArray()));//fixme
        return result.verified;
    }

    public boolean updatePassword(String oldPassword, String newPassword){
        if (this.isValidPassword(oldPassword)){
            this.setPassword(String.valueOf(BCrypt.withDefaults().hashToChar(12, newPassword.toCharArray())));
            return true;
        }
        return false;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
