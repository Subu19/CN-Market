package net.craftnepal.market.utils;

import net.craftnepal.market.Market;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionLogUtils {
    
    private static File logFile;

    public static void setup() {
        if (!Market.getPlugin().getDataFolder().exists()) {
            Market.getPlugin().getDataFolder().mkdir();
        }
        logFile = new File(Market.getPlugin().getDataFolder(), "transactions.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Could not create transactions.log file!");
            }
        }
    }

    public static void log(String message) {
        if (logFile == null) {
            setup();
        }
        
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
             
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            pw.println("[" + timestamp + "] " + message);
            
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not write to transactions.log!");
            e.printStackTrace();
        }
    }
}
