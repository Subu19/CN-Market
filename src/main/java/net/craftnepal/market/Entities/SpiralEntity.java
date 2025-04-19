package net.craftnepal.market.Entities;

import com.sun.org.apache.xpath.internal.operations.Bool;
import net.craftnepal.market.utils.PrintParticles;
import net.craftnepal.market.utils.SendMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class SpiralEntity {
    private Integer radius;
    private Location center;
    private Double acceleration = 0D;
    private Double angle = 0D;
    private Boolean flag = true;
    private Player p;
    private Boolean stage1count;
    PrintParticles printParticles;
    public SpiralEntity(Integer radius, Location center, Player player) {
        this.radius = radius;
        this.p = player;
        String text = "HAPPYBIRTHDAY";
        this.stage1count = false;
        this.center = new Location(center.getWorld(),center.getX()-((text.length()/2)*8),center.getY()+40,center.getZ()-50);
        this.printParticles = new PrintParticles(text,this.center);
    }

    public void update(){
            if(!stage1count){
                double x, y,upx,upy,downx,downy;
                x= (Math.cos(angle)*radius);
                y= (Math.sin(angle)*radius);

                upx= (Math.cos(angle-90)*(radius+1));
                upy= (Math.sin(angle-90)*(radius+1));
                downx= (Math.cos(angle-180)*(radius-1));
                downy= (Math.sin(angle-180)*(radius-1));

                Location particleLocation = new Location(p.getWorld(),center.getX()+x,center.getY()+y,center.getZ());
                Location upParticleLocation = new Location(p.getWorld(),center.getX()+upx,center.getY()+upy,center.getZ());
                Location downParticleLocation = new Location(p.getWorld(),center.getX()+downx,center.getY()+downy,center.getZ());

                p.getWorld().spawnParticle(Particle.REDSTONE,particleLocation,3,0,0,0,0,new Particle.DustOptions(
                        Color.RED, 15),true);
                p.getWorld().spawnParticle(Particle.REDSTONE,upParticleLocation,3,0,0,0,0,new Particle.DustOptions(
                        Color.LIME, 15),true);
                p.getWorld().spawnParticle(Particle.REDSTONE,downParticleLocation,3,0,0,0,0,new Particle.DustOptions(
                        Color.BLUE, 15),true);
                if(angle > 50){
                    stage1count = true;
                }
                angle+=0.1;

            }else{
               updateStage1();
            }

    }
    private void updateStage1(){
        printParticles.update();
        if(angle<360){
            double x, y,upx,upy,downx,downy;
            x= (Math.cos(angle)*radius);
            y= (Math.sin(angle)*radius);

            upx= (Math.cos(angle-90)*(radius+1));
            upy= (Math.sin(angle-90)*(radius+1));
            downx= (Math.cos(angle-180)*(radius-1));
            downy= (Math.sin(angle-180)*(radius-1));

            Location particleLocation = new Location(p.getWorld(),center.getX()+ acceleration,center.getY()+y,center.getZ());
            Location upParticleLocation = new Location(p.getWorld(),center.getX()+ acceleration,center.getY()+upy,center.getZ()+upx);
            Location downParticleLocation = new Location(p.getWorld(),center.getX()+ acceleration,center.getY()+downy,center.getZ()+downx);

            p.getWorld().spawnParticle(Particle.REDSTONE,particleLocation,3,0,0,0,0,new Particle.DustOptions(
                    Color.RED, 15),true);
            p.getWorld().spawnParticle(Particle.REDSTONE,upParticleLocation,3,0,0,0,0,new Particle.DustOptions(
                    Color.LIME, 15),true);
            p.getWorld().spawnParticle(Particle.REDSTONE,downParticleLocation,3,0,0,0,0,new Particle.DustOptions(
                    Color.BLUE, 15),true);



            //handle z axis
            if(flag){
                acceleration +=0.5;
            }else{
                acceleration -=0.5;
            }
            //change flag
            if(acceleration>=100){
                flag = false;
            }else if(acceleration<=0){
                flag = true;
            }
            angle+=0.1;
        }else{
            angle = 0D;
        }
    }
}
