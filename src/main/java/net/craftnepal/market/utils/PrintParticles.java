package net.craftnepal.market.utils;

import jdk.vm.ci.code.site.Mark;
import net.craftnepal.market.Market;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.*;

public class PrintParticles {
    String text;
    Location origin;
    int index=0;
    World world;
    Color color;
    int cooldown;
    public static ArrayList<Integer> letters = new ArrayList<>();
    public PrintParticles(String text, Location origin) {
        this.text = text;
        this.origin = new Location(origin.getWorld(), origin.getX(),origin.getY()-4,origin.getZ());
        this.world = origin.getWorld();
        this.color =Color.RED;
        this.cooldown = 15;
    }

    public void update(){
        if(cooldown !=0){
            cooldown--;
            return;
        }
        if(index+1 > text.length()){
            launchFirework();
            cooldown = 5;

        }else{
            if(text.charAt(index) == 'H'){
                printH();
                cooldown=15;
            } else if (text.charAt(index) == 'A') {
                printA();
                cooldown=15;
            } else if (text.charAt(index) == 'P') {
                printP();
                cooldown=15;
            } else if (text.charAt(index) == 'Y') {
                printY();
                cooldown=15;
            }else if (text.charAt(index) == 'B') {
                printB();
                cooldown=15;
            }else if (text.charAt(index) == 'I') {
                printI();
                cooldown=15;
            }else if (text.charAt(index) == 'R') {
                printR();
                cooldown=15;
            }else if (text.charAt(index) == 'T') {
                printT();
                cooldown=15;
            }else if (text.charAt(index) == 'D') {
                printD();
                cooldown=15;
            }
            if(index+1 == text.length()){
                printHeart();
                Bukkit.getLogger().info("heart..");
            }
            if(index == 4){
                this.color = Color.LIME;
            }
            index++;
        }

    }

    private void printH(){
        Location letterOrigin = new Location(origin.getWorld(),origin.getX()+(index*8),origin.getY(),origin.getZ());
        Color pColor = this.color;
        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(),()->{
            // Generate a letter H of bold 2, width of 6, and height of 8
            for (int i = 0; i<8; i++){
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX(),letterOrigin.getY()+i,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+1,letterOrigin.getY()+i,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+4,letterOrigin.getY()+i,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+5,letterOrigin.getY()+i,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
            }
            for (int i = 0; i<6; i++){
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+i,letterOrigin.getY()+3,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+i,letterOrigin.getY()+4, letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
            }

        },1L,2L));
    }
    private void printI(){
        Location letterOrigin = new Location(origin.getWorld(),origin.getX()+(index*8),origin.getY(),origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(),()->{
            // Generate a letter H of bold 2, width of 6, and height of 8
            for (int i = 0; i<8; i++){
                if(i>1 && i<6){
                    world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+2,letterOrigin.getY()+i,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                            pColor, 15),true);
                    world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+3,letterOrigin.getY()+i,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                            pColor, 15),true);
                }
            }
            for (int i = 0; i<6; i++){
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+i,letterOrigin.getY(),letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+i,letterOrigin.getY()+1,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+i,letterOrigin.getY()+6, letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+i,letterOrigin.getY()+7, letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
            }

        },1L,2L));
    }
    private void printT(){
        Location letterOrigin = new Location(origin.getWorld(),origin.getX()+(index*8),origin.getY(),origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(),()->{
            // Generate a letter H of bold 2, width of 6, and height of 8
            // Generate a letter H of bold 2, width of 6, and height of 8
            for (int i = 0; i<8; i++){
                if(i<6){
                    world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+2,letterOrigin.getY()+i,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                            pColor, 15),true);
                    world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+3,letterOrigin.getY()+i,letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                            pColor, 15),true);
                }
            }
            for (int i = 0; i<6; i++){

                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+i,letterOrigin.getY()+6, letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
                world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+i,letterOrigin.getY()+7, letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                        pColor, 15),true);
            }

        },1L,2L));
    }
    private void printA(){
        Location letterOrigin = new Location(origin.getWorld(),origin.getX()+(index*8),origin.getY()+7,origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(),()->{
            ArrayList<Location> nonePP = new ArrayList<>();
            Collection<Location> locations = Arrays.asList(
                    new Location(letterOrigin.getWorld(), letterOrigin.getX(),letterOrigin.getY(),letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2,letterOrigin.getY()-2,letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3,letterOrigin.getY()-2,letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2,letterOrigin.getY()-5,letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2,letterOrigin.getY()-6,letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2,letterOrigin.getY()-7,letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3,letterOrigin.getY()-5,letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3,letterOrigin.getY()-6,letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3,letterOrigin.getY()-7,letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5,letterOrigin.getY(),letterOrigin.getZ())
            );

            nonePP.addAll(locations);

            for (int x = 0; x<6;x++){
                for (int y = 0; y<8;y++){
                    if(!nonePP.contains(new Location(letterOrigin.getWorld(),letterOrigin.getX()+x,letterOrigin.getY()-y,letterOrigin.getZ()))){
                        world.spawnParticle(Particle.REDSTONE,new Location(letterOrigin.getWorld(),letterOrigin.getX()+x,letterOrigin.getY()-y, letterOrigin.getZ()),3,0,0,0,0,new Particle.DustOptions(
                                pColor, 15),true);
                    }
                }
            }
        },1L,2L));
    }
    private void printP() {
        Location letterOrigin = new Location(origin.getWorld(), origin.getX() + (index * 8), origin.getY() + 7, origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(), () -> {
            ArrayList<Location> nonePP = new ArrayList<>();
            Collection<Location> locations = Arrays.asList(
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 2, letterOrigin.getY() - 2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 2, letterOrigin.getY() - 3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 3, letterOrigin.getY() - 2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 3, letterOrigin.getY() - 3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 2, letterOrigin.getY() - 6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 2, letterOrigin.getY() - 7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 3, letterOrigin.getY() - 6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 3, letterOrigin.getY() - 7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 4, letterOrigin.getY() - 6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 4, letterOrigin.getY() - 7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 5, letterOrigin.getY() - 6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 5, letterOrigin.getY() - 7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 5, letterOrigin.getY() - 5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 5, letterOrigin.getY(), letterOrigin.getZ())
            );

            nonePP.addAll(locations);

            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 8; y++) {
                    if (!nonePP.contains(new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()))) {
                        world.spawnParticle(Particle.REDSTONE, new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()), 3, 0,0,0,0,new Particle.DustOptions(
                                pColor, 15),true);
                    }
                }
            }
        }, 1L, 2L));
    }
    private void printY() {
        Location letterOrigin = new Location(origin.getWorld(), origin.getX() + (index * 8), origin.getY() + 7, origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(), () -> {
            ArrayList<Location> nonePP = new ArrayList<>();
            Collection<Location> locations = Arrays.asList(
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 2, letterOrigin.getY(), letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 3, letterOrigin.getY(), letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 2, letterOrigin.getY()-1, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() + 3, letterOrigin.getY()-1, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX(), letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() , letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() , letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() , letterOrigin.getY()-6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX() , letterOrigin.getY()-7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+1 , letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+1 , letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+1 , letterOrigin.getY()-6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+1 , letterOrigin.getY()-7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+4, letterOrigin.getY()-7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+4 , letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+4 , letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+4 , letterOrigin.getY()-6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5 , letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5 , letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5 , letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5 , letterOrigin.getY()-6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5 , letterOrigin.getY()-7, letterOrigin.getZ())
            );

            nonePP.addAll(locations);

            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 8; y++) {
                    if (!nonePP.contains(new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()))) {
                        world.spawnParticle(Particle.REDSTONE, new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()), 3,0,0,0,0, new Particle.DustOptions(
                                pColor, 15),true);
                    }
                }
            }
        }, 1L, 2L));
    }
    private void printB() {
        Location letterOrigin = new Location(origin.getWorld(), origin.getX() + (index * 8), origin.getY() + 7, origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(), () -> {
            ArrayList<Location> nonePP = new ArrayList<>();
            Collection<Location> locations = Arrays.asList(
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY(), letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-7, letterOrigin.getZ())
            );

            nonePP.addAll(locations);

            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 8; y++) {
                    if (!nonePP.contains(new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()))) {
                        world.spawnParticle(Particle.REDSTONE, new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()), 3,0,0,0,0, new Particle.DustOptions(
                                pColor, 15),true);
                    }
                }
            }
        }, 1L, 2L));
    }
    private void printR() {
        Location letterOrigin = new Location(origin.getWorld(), origin.getX() + (index * 8), origin.getY() + 7, origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(), () -> {
            ArrayList<Location> nonePP = new ArrayList<>();
            Collection<Location> locations = Arrays.asList(
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY(), letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()-3, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-7, letterOrigin.getZ())
            );

            nonePP.addAll(locations);

            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 8; y++) {
                    if (!nonePP.contains(new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()))) {
                        world.spawnParticle(Particle.REDSTONE, new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()), 3,0,0,0,0, new Particle.DustOptions(
                                pColor, 15),true);
                    }
                }
            }
        }, 1L, 2L));
    }
    private void printD() {
        Location letterOrigin = new Location(origin.getWorld(), origin.getX() + (index * 8), origin.getY() + 7, origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(), () -> {
            ArrayList<Location> nonePP = new ArrayList<>();
            Collection<Location> locations = Arrays.asList(
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+4, letterOrigin.getY(), letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY(), letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-1, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-6, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+4, letterOrigin.getY()-7, letterOrigin.getZ())
            );

            nonePP.addAll(locations);

            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 8; y++) {
                    if (!nonePP.contains(new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()))) {
                        world.spawnParticle(Particle.REDSTONE, new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()), 3,0,0,0,0, new Particle.DustOptions(
                                pColor, 15),true);
                    }
                }
            }
        }, 1L, 2L));
    }
    private void printG() {
        Location letterOrigin = new Location(origin.getWorld(), origin.getX() + (index * 8), origin.getY() + 7, origin.getZ());
        Color pColor = this.color;

        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(), () -> {
            ArrayList<Location> nonePP = new ArrayList<>();
            Collection<Location> locations = Arrays.asList(
                    new Location(letterOrigin.getWorld(), letterOrigin.getX(), letterOrigin.getY(), letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY(), letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX(), letterOrigin.getY()-7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-7, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()+2, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()+3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+4, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+5, letterOrigin.getY()-3, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-4, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+2, letterOrigin.getY()-5, letterOrigin.getZ()),
                    new Location(letterOrigin.getWorld(), letterOrigin.getX()+3, letterOrigin.getY()-5, letterOrigin.getZ())
            );
            nonePP.addAll(locations);

            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 8; y++) {
                    if (!nonePP.contains(new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()))) {
                        world.spawnParticle(Particle.REDSTONE, new Location(letterOrigin.getWorld(), letterOrigin.getX() + x, letterOrigin.getY() - y, letterOrigin.getZ()), 3,0,0,0,0, new Particle.DustOptions(
                                pColor, 15),true);
                    }
                }
            }
        }, 1L, 2L));
    }
    private void printHeart() {
        Location heartOrigin = new Location(origin.getWorld(), origin.getX() + ((index * 8)/2), origin.getY() -20, origin.getZ());
        Location letterOrigin1 = new Location(origin.getWorld(), origin.getX() + ((index * 8)/2)-7, origin.getY() -10, origin.getZ());
        Location letterOrigin2 = new Location(origin.getWorld(), origin.getX() + ((index * 8)/2)+2, origin.getY() -10, origin.getZ());

        ArrayList<Location> namePositions = new ArrayList<>();
        ArrayList<Location> nonePP = new ArrayList<>();
        Collection<Location> locations = Arrays.asList(
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX(), letterOrigin2.getY(), letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+5, letterOrigin2.getY(), letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX(), letterOrigin2.getY()-7, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+5, letterOrigin2.getY()-7, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+2, letterOrigin2.getY()-2, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+2, letterOrigin2.getY()-3, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+3, letterOrigin2.getY()-2, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+3, letterOrigin2.getY()-3, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+4, letterOrigin2.getY()-3, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+5, letterOrigin2.getY()-3, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+2, letterOrigin2.getY()-4, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+2, letterOrigin2.getY()-5, letterOrigin2.getZ()),
                new Location(letterOrigin2.getWorld(), letterOrigin2.getX()+3, letterOrigin2.getY()-5, letterOrigin2.getZ()),

                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+2, letterOrigin1.getY()-2, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+2, letterOrigin1.getY()-3, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+2, letterOrigin1.getY()-4, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+2, letterOrigin1.getY()-5, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+3, letterOrigin1.getY()-3, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+3, letterOrigin1.getY()-4, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+4, letterOrigin1.getY(), letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+5, letterOrigin1.getY(), letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+5, letterOrigin1.getY()-1, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+5, letterOrigin1.getY()-6, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+5, letterOrigin1.getY()-7, letterOrigin1.getZ()),
                new Location(letterOrigin1.getWorld(), letterOrigin1.getX()+4, letterOrigin1.getY()-7, letterOrigin1.getZ())


        );
        nonePP.addAll(locations);

        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 8; y++) {
                if (!nonePP.contains(new Location(letterOrigin1.getWorld(), letterOrigin1.getX() + x, letterOrigin1.getY() - y, letterOrigin1.getZ()))) {
                    namePositions.add(new Location(letterOrigin1.getWorld(), letterOrigin1.getX() + x, letterOrigin1.getY() - y, letterOrigin1.getZ()));
                }
                if (!nonePP.contains(new Location(letterOrigin2.getWorld(), letterOrigin2.getX() + x, letterOrigin2.getY() - y, letterOrigin2.getZ()))) {
                    namePositions.add(new Location(letterOrigin2.getWorld(), letterOrigin2.getX() + x, letterOrigin2.getY() - y, letterOrigin2.getZ()));
                }
            }
        }
        letters.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(Market.getPlugin(), () -> {
            //spawn name

            //spawn hearts

            int n = 15;
            for (int i = -3*n/2; i <= n; i++) {
                for (int j = -3*n/2; j <= 3*n/2; j++) {

                    // inside either diamond or two circles
                    if ((Math.abs(i) + Math.abs(j) < n)
                            || ((-n/2-i) * (-n/2-i) + ( n/2-j) * ( n/2-j) <= n*n/2)
                            || ((-n/2-i) * (-n/2-i) + (-n/2-j) * (-n/2-j) <= n*n/2)) {

                        if(namePositions.contains(new Location(heartOrigin.getWorld(), heartOrigin.getX() + j, heartOrigin.getY() - i, heartOrigin.getZ()))){
                            world.spawnParticle(Particle.REDSTONE, new Location(heartOrigin.getWorld(), heartOrigin.getX() + j, heartOrigin.getY() - i, heartOrigin.getZ()), 3,0,0,0,0, new Particle.DustOptions(
                                    Color.PURPLE, 15),true);
                        }else{
                            world.spawnParticle(Particle.REDSTONE, new Location(heartOrigin.getWorld(), heartOrigin.getX() + j, heartOrigin.getY() - i, heartOrigin.getZ()), 3,0,0,0,0, new Particle.DustOptions(
                                    Color.YELLOW, 15),true);
                        }

                    }
                }
            }
        }, 1L, 2L));
    }

    public void launchFirework(){

        Location fireworkLocation = new Location(world,origin.getX()+ new Random().nextInt(101),origin.getY()-30, origin.getZ());
        // Create a new Firework entity at the specified location
        Firework firework = world.spawn(fireworkLocation, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        // Define the FireworkEffect (you can customize this)
        FireworkEffect.Builder builder = FireworkEffect.builder();
        builder.withColor(Color.RED);
        builder.withColor(Color.ORANGE);
        builder.withColor(Color.YELLOW);
        builder.with(FireworkEffect.Type.BURST);
        builder.trail(true);

        // Set the FireworkEffect to the FireworkMeta
        meta.addEffect(builder.build());

        // Set other FireworkMeta properties (e.g., power)
        meta.setPower(new Random().nextInt(5));

        // Apply the FireworkMeta to the Firework entity
        firework.setFireworkMeta(meta);

        // Schedule the Firework entity for removal after a delay
        Bukkit.getScheduler().scheduleSyncDelayedTask(Market.getPlugin(),()->{
            firework.detonate();
        },new Random().nextLong(50));
    }
}
