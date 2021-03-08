package me.flashyreese.mods.smartofflinemode.server.auth.database;

import me.flashyreese.mods.smartofflinemode.server.auth.Account;
import org.lmdbjava.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LMDB {

    private final String UUID_PASSWORD_DB = "uuid_password";
    private final String UUID_IP_DB = "uuid_ip";
    private final Env<ByteBuffer> environment;

    private final Dbi<ByteBuffer> passwordDB;
    private final Dbi<ByteBuffer> ipDB;

    public LMDB(File file){
        //todo: Create path
        if (!file.exists())
            file.mkdirs();

        this.environment = Env.create().setMapSize(1_000_000).setMaxDbs(2).open(file);
        this.passwordDB = this.environment.openDbi(UUID_PASSWORD_DB, DbiFlags.MDB_CREATE);
        this.ipDB = this.environment.openDbi(UUID_IP_DB, DbiFlags.MDB_CREATE);
    }

    public void addAccount(Account account){
        this.putPassword(account.getUUID(), account.getPassword());
        this.putIP(account.getUUID(), account.getIpAddress());
    }

    public Account getAccount(UUID uuid){
        String password = this.getPassword(uuid);
        if (password == null) {
            return null;
        }
        String ip = this.getIP(uuid);
        Account account = new Account(uuid, password);
        account.setIpAddress(ip == null ? "" : ip);
        return account;
    }

    public boolean deleteAccount(Account account) {
        boolean password = this.deletePassword(account.getUUID());
        this.deleteIP(account.getUUID());

        return password;
    }

    private void putPassword(UUID uuid, String ip) {
        final ByteBuffer key = ByteBuffer.allocateDirect(this.environment.getMaxKeySize());
        final ByteBuffer val = ByteBuffer.allocateDirect(256);
        key.put(uuid.toString().getBytes(StandardCharsets.UTF_8)).flip();
        val.put(ip.getBytes(StandardCharsets.UTF_8)).flip();
        this.passwordDB.put(key, val);
    }

    private String getPassword(UUID uuid) {
        final ByteBuffer key = ByteBuffer.allocateDirect(this.environment.getMaxKeySize());
        key.put(uuid.toString().getBytes(StandardCharsets.UTF_8)).flip();

        try (Txn<ByteBuffer> txn = this.environment.txnRead()) {
            final ByteBuffer found = this.passwordDB.get(txn, key);
            if (found == null) {
                return null;
            } else {
                final ByteBuffer fetchedValue = txn.val();
                return StandardCharsets.UTF_8.decode(fetchedValue).toString();
            }
        }
    }

    private boolean deletePassword(UUID uuid){
        final ByteBuffer key = ByteBuffer.allocateDirect(this.environment.getMaxKeySize());
        key.put(uuid.toString().getBytes(StandardCharsets.UTF_8)).flip();

        return this.passwordDB.delete(key);
    }


    private void putIP(UUID uuid, String ip) {
        final ByteBuffer key = ByteBuffer.allocateDirect(this.environment.getMaxKeySize());
        final ByteBuffer val = ByteBuffer.allocateDirect(15);
        key.put(uuid.toString().getBytes(StandardCharsets.UTF_8)).flip();
        val.put(ip.getBytes(StandardCharsets.UTF_8)).flip();
        this.ipDB.put(key, val);
    }

    private String getIP(UUID uuid) {
        final ByteBuffer key = ByteBuffer.allocateDirect(this.environment.getMaxKeySize());
        key.put(uuid.toString().getBytes(StandardCharsets.UTF_8)).flip();

        try (Txn<ByteBuffer> txn = this.environment.txnRead()) {
            final ByteBuffer found = this.ipDB.get(txn, key);
            if (found == null) {
                return null;
            } else {
                final ByteBuffer fetchedValue = txn.val();
                return StandardCharsets.UTF_8.decode(fetchedValue).toString();
            }
        }
    }

    private boolean deleteIP(UUID uuid){
        final ByteBuffer key = ByteBuffer.allocateDirect(this.environment.getMaxKeySize());
        key.put(uuid.toString().getBytes(StandardCharsets.UTF_8)).flip();

        return this.ipDB.delete(key);
    }
}
