package net.craftnepal.market.files;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LocationData {
    private static File file;
    private static FileConfiguration config;

    public static void setup(){
        file = new File(Bukkit.getServer().getPluginManager().getPlugin("Market").getDataFolder(),"LocationData.yml");
        if(!file.exists()){
            try{
                file.createNewFile();
            }catch (IOException e){
                System.out.println("Couldnt create location file.");
            }

        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get(){
        return config;
    }
    public static void save(){
        try{
            config.save(file);
        }catch (IOException e){
            System.out.println("Couldnt save file!");
        }

    }
    public static void reload(){
        config = YamlConfiguration.loadConfiguration(file);
    }
}
