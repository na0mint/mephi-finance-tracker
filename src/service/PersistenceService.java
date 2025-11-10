package service;

import model.Wallet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Сервис для сохранения и получения данных
 */
public class PersistenceService {

    private static final String PREFIX = "wallet_";
    private static final String SUFFIX = ".dat";

    public String walletFileName(String login) {
        return PREFIX + login + SUFFIX;
    }

    public void saveWallet(String login, Wallet wallet) {
        var f = new File(walletFileName(login));

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(wallet);
        } catch (Exception e) {
            System.out.println("Не удалось сохранить кошелёк пользователя " + login + ": " + e.getMessage());
        }
    }

    public Wallet loadWallet(String login) {
        var f = new File(walletFileName(login));

        if (!f.exists()) return new Wallet(login);

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object o = ois.readObject();
            if (o instanceof Wallet w) return w;
        } catch (Exception e) {
            System.out.println("Не удалось загрузить кошелёк пользователя " + login + ": " + e.getMessage());
        }

        return new Wallet(login);
    }

    public void saveAll(AuthService authService, WalletService walletService) {
        authService.saveUsers();

        for (var login : authService.knownUsers()) {
            var w = walletService.getWalletIfLoaded(login);
            if (w != null) saveWallet(login, w);
        }
    }
}
