package me.flashyreese.mods.smartofflinemode.server.auth.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.flashyreese.mods.smartofflinemode.server.auth.Account;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JSONDatabase {
    private List<Account> accountList;

    private final Gson gson = new Gson();

    private final File file;

    public JSONDatabase(File file) {
        this.file = file;
        try {
            if (this.file.exists()) {
                this.accountList = this.gson.fromJson(new FileReader(this.file), new TypeToken<ObjectArrayList<Account>>(){}.getType());
            } else {
                this.accountList = new ObjectArrayList<>();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void addAccount(Account account) {
        Optional<Account> accountOptional = this.accountList.stream().filter(acc -> acc.getUUID().equals(account.getUUID())).findFirst();
        if (accountOptional.isEmpty()) {
            this.accountList.add(account);
            writeAccountList();
        }
    }

    public Account getAccount(UUID uuid) {
        Optional<Account> accountOptional = this.accountList.stream().filter(acc -> acc.getUUID().equals(uuid)).findFirst();
        return accountOptional.orElse(null);
    }

    public boolean deleteAccount(Account account) {
        Optional<Account> accountOptional = this.accountList.stream().filter(acc -> acc.getUUID().equals(account.getUUID())).findFirst();
        if (accountOptional.isPresent()) {
            boolean result = this.accountList.remove(accountOptional.get());
            if (result)
                writeAccountList();
            return result;
        }
        return false;
    }

    private void writeAccountList() {
        String json = this.gson.toJson(this.accountList);
        try {
            FileWriter fw = new FileWriter(this.file);
            try {
                fw.write(json);
                fw.flush();
                fw.close();
            } catch (Throwable throwable) {
                try {
                    fw.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
