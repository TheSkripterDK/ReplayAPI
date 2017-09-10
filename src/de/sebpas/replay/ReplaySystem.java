package me.danilkp1234.robocat;

import de.sebpas.replay.RePlayer;
import de.sebpas.replay.Recorder;
import me.danilkp1234.robocat.RoboCat;
import de.sebpas.replay.command.CommandReplay;
import de.sebpas.replay.event.ReplayStoppedEvent;
import de.sebpas.replay.filesystem.FileManager;
import de.sebpas.replay.util.PlayingPlayer;
import me.danilkp1234.robocat.Util.Settings;
import me.danilkp1234.robocat.Util.User;
import me.danilkp1234.robocat.checks.CheckResults;
import me.danilkp1234.robocat.events.JoinLeaveListener;
import me.danilkp1234.robocat.events.MoveListener;
import me.danilkp1234.robocat.events.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import sun.reflect.generics.tree.ReturnType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import static org.bukkit.ChatColor.*;
/**
 * Created by danie on 27-08-2017.
 */
public class RoboCat extends JavaPlugin {
    private int ranTicks = 0;
    private FileManager fileSystem;
    private Recorder recorder;

    private static RoboCat instance = null;

    /** list of all replayers */
    private List<RePlayer> replayers = new ArrayList<RePlayer>();

    /** plugin prefixes */
    private static String prefix = "&8[&Robocat&8]: &r";
    private static String error = "&8[&cRobocat&8]: &c";


    /**
     * Bukkit methods
     */
    public static HashMap<UUID, User> USERS = new HashMap<>();

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("Starter RoboCat AntiCheat");
        if(recorder.isRecording()){
            this.stop();
        }
        for(RePlayer r : replayers)
            r.stopWithoutTask();
    }


    @Override
    public void onEnable() {
        PluginManager pm = Bukkit.getPluginManager();
        this.getServer().getPluginManager().registerEvents(new JoinLeaveListener(), this);
        this.getServer().getPluginManager().registerEvents(new MoveListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        System.out.println("[Replay]: Enabled!");
        this.fileSystem = new FileManager();
        this.recorder = new Recorder(this);

        this.getCommand("rplstart").setExecutor(new CommandReplay(this));
        this.getCommand("rplstop").setExecutor(new CommandReplay(this));
        this.getCommand("replay").setExecutor(new CommandReplay(this));

        this.getServer().getPluginManager().registerEvents(new PlayingPlayer(), this);

        instance = this;
        for (Player p : Bukkit.getOnlinePlayers())
            USERS.put(p.getUniqueId(), new User(p));
    }

    public static void log(CheckResults cr, User u) {
        String message = AQUA.toString() + BOLD + "RoboCat" + " " + RESET.toString() + RED + u.getPlayer().getName() + GRAY + " " + cr.getLevel().toString().toLowerCase() + " " + AQUA + cr.getType().getName() + GRAY + "; " + cr.getMessage();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(Settings.NOTIFY)) {
                p.sendMessage(message);
                Bukkit.getConsoleSender().sendMessage(message);
            }
        }
    }
    public static void RoboBan(Player p, CheckResults cr){
        Player player = p;
        Location eyelocation = player.getEyeLocation();
        Vector vec = player.getLocation().getDirection();
        Location spawn1 = eyelocation.add(vec);
        Ocelot ocelot = (Ocelot) player.getWorld().spawnEntity(spawn1, EntityType.OCELOT);
        ocelot.setCatType(Ocelot.Type.BLACK_CAT);
        ocelot.setTamed(true);
        ocelot.setCustomName("RoboCat");
        player.playSound(player.getLocation(), Sound.CAT_MEOW, 40, 0);
        String reason = AQUA.toString() + BOLD + "RoboCat" + " " + RESET.toString() + RED + "Du er bannet for" + " " + AQUA + cr.getType().getName();
        //Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(),reason, null, null);
        //player.kickPlayer(reason);
    }
    public static User getUser(Player p) {
        return USERS.get(p.getUniqueId());
    }

    //Replay Code
    public static RoboCat getInstance(){
        return instance;
    }

    public void start(){
        this.ranTicks = 0;
        this.fileSystem.reset();
        this.recorder.recorde();
    }

    public void stop(){
        this.recorder.stop();
        this.ranTicks = 0;
        this.fileSystem.save();
    }

    /** returns the amount of recorded ticks */
    public int getHandledTicks(){
        return this.ranTicks;
    }

    /** sends a message to all players and the console */
    public static void sendBroadcast(String msg){
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }

    /** sends an error message to all players and the console */
    public static void sendBroadcastError(String msg){
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', error + msg));
    }
    public String getPrefix(){
        return prefix;
    }
    public String getErrorPrefix(){
        return error;
    }

    /** the tick counter */
    public void addTick(){
        ++ ranTicks;
    }

    /** returns the filemanager */
    public FileManager getFileManager(){
        return fileSystem;
    }

    /** returns the recording thread */
    public Recorder getRecorder(){
        return this.recorder;
    }
    /** returns true if the player is already watching a replay */
    public boolean isAlreadyInReplay(Player p){
        for(RePlayer r : replayers)
            if(r.getPlayers().containsKey(p))
                return true;
        return false;
    }

    public void addPlayer(RePlayer p){
        this.replayers.add(p);
    }

    public void onPlayerStopped(RePlayer p){
        this.replayers.remove(p);
        synchronized (p) {
            this.getServer().getPluginManager().callEvent(new ReplayStoppedEvent(p));
        }
    }
    public RePlayer getPlayersRePlayer(Player p){
        for(RePlayer r : replayers){
            if(r.getPlayers().containsKey(p))
                return r;
        }
        return null;
    }
    public RePlayer getPlayersRePlayer(HumanEntity p){
        for(RePlayer r : replayers){
            for(Player t : r.getPlayers().keySet())
                if(t.getName().equals(p.getName()))
                    return r;
        }
        return null;
    }
}
