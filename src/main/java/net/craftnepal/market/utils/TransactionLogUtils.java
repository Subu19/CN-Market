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
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L; // 5 MB

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
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logLine = "[" + timestamp + "] " + message;

        // Perform logging and rotation check asynchronously to avoid main-thread blocking I/O lag
        Bukkit.getScheduler().runTaskAsynchronously(Market.getPlugin(), () -> {
            synchronized (TransactionLogUtils.class) {
                if (logFile == null) {
                    setup();
                }

                // Check if the log file exceeds the maximum size to trigger rotation
                if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                    rollLogFile();
                }
                
                try (FileWriter fw = new FileWriter(logFile, true);
                     PrintWriter pw = new PrintWriter(fw)) {
                    pw.println(logLine);
                } catch (IOException e) {
                    Bukkit.getLogger().severe("Could not write to transactions.log!");
                    e.printStackTrace();
                }
            }
        });
    }

    private static void rollLogFile() {
        File logsDir = new File(Market.getPlugin().getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File rotatedFile = new File(logsDir, "transactions_" + timeStamp + ".log");
        File zipFile = new File(logsDir, "transactions_" + timeStamp + ".zip");

        // Rename the current log file to start the rotation process
        if (logFile.renameTo(rotatedFile)) {
            // Compress the rotated log file into a zip file
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
                 java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);
                 java.io.FileInputStream fis = new java.io.FileInputStream(rotatedFile)) {

                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(rotatedFile.getName());
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                
                Bukkit.getLogger().info("[Market] Rotated and compressed transaction log to " + zipFile.getName());
            } catch (IOException e) {
                Bukkit.getLogger().severe("Failed to zip rotated transaction log!");
                e.printStackTrace();
            } finally {
                // Delete the uncompressed rotated log file
                if (rotatedFile.exists()) {
                    rotatedFile.delete();
                }
            }
        }

        // Recreate the fresh log file
        logFile = new File(Market.getPlugin().getDataFolder(), "transactions.log");
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not recreate transactions.log file!");
        }
    }
}
