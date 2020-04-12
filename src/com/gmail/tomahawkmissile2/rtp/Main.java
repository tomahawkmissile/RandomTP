package com.gmail.tomahawkmissile2.rtp;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

	public static Main plugin;
	
	public static volatile ConcurrentHashMap<Player,Integer> cooldown = new ConcurrentHashMap<Player,Integer>();
	
	public static AtomicBoolean stopChecker=new AtomicBoolean(false);
	
	public void onEnable() {
		Main.plugin=this;
		this.getServer().getPluginManager().registerEvents(this,this);
		if(!new File(this.getDataFolder()+"/config.yml").exists()) {
			try {
				new File(this.getDataFolder()+"/config.yml").createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Config.setDefaults();
		}
		for(DefaultConfigValue v:DefaultConfigValue.values()) {
			if(Config.get(v.getPath())==null || Config.get(v.getPath()).equals("")) {
				Config.setDefault(v);
			}
		}
		this.getCommand("rtp").setExecutor(this);
		this.getCommand("wild").setExecutor(this);
		Main.stopChecker.set(false);;
		this.runChecker();
	}
	public void onDisable() {
		stopChecker.set(true);
		System.out.println(ChatColor.DARK_GREEN+"[RTP]"+ChatColor.GREEN+" Successfully stopped cooldown watchdog thread.");
	}
	public void runChecker() {
		Thread t = new Thread() {
			public void run() {
				while(!Main.stopChecker.get()) {
					for(Player p:cooldown.keySet()) {
						if(cooldown.get(p)<=0) {
							cooldown.remove(p);
						} else {
							cooldown.put(p, cooldown.get(p)-1000);
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		System.out.println(ChatColor.DARK_GREEN+"[RTP]"+ChatColor.GREEN+" Successfully started cooldown watchdog thread.");
		t.start();
	}
	public void queuePlayerTimer(Player p) {
		if(p.isOp() || p.hasPermission("rtp.unlimited")) {
			return;
		} else {
			int cool=5000;
			try {
				cool = Integer.parseInt((String) Config.get("cooldown"));
			} catch(NumberFormatException e) {
				System.out.println("[RTP] Config error! Invalid setting for cooldown!");
			}
			cooldown.putIfAbsent(p, cool);
		}
	}
	public void tpPlayerWhenSafe(Player p) {
		p.sendMessage(ChatColor.GOLD+"[RTP]"+ChatColor.YELLOW+" Finding safe location in world.");
		queuePlayerTimer(p);
		new Thread() {
			@Override
			public void run() {
				String worldName = (String)Config.get("world");
				if(Bukkit.getWorld(worldName)==null) {
					System.out.println(ChatColor.DARK_RED+"[RTP]"+ChatColor.RED+" World "+worldName+" does not exist! Using first overworld file in server!");
					for(World w:Bukkit.getWorlds()) {
						if(w.getWorldType()==WorldType.NORMAL) {
							worldName=w.getName();
							break;
						}
					}
				}
				World tp = Bukkit.getWorld(worldName);
				int maxx,maxz;
				try {
					maxx = Integer.parseInt((String) Config.get("x.max"));
					maxz = Integer.parseInt((String) Config.get("z.max"));
				} catch(NumberFormatException e) {
					maxx=2500;
					maxz=2500;
				}
				int x = (int)(Math.random()*(maxx+1));
				int z = (int)(Math.random()*(maxz+1));
				for(int i=255;i>=0;i--) {
					Block b = tp.getBlockAt(new Location(tp, x, i, z));
					if(b.getType()!=Material.AIR) {
						if(i<56) {
							i=255;
							x+=(Math.random()-0.5)*(20);
							z+=(Math.random()-0.5)*(20);
						}
						switch(b.getType()) {
						case OAK_LEAVES:
						case BIRCH_LEAVES:
						case SPRUCE_LEAVES:
						case DARK_OAK_LEAVES:
						case ACACIA_LEAVES:
						case JUNGLE_LEAVES:
							if(tp.getBlockAt(new Location(tp,b.getX(),b.getY()-1,b.getZ())).getType()==Material.AIR)
								if(tp.getBlockAt(new Location(tp,b.getX(),b.getY()-2,b.getZ())).getType()==Material.AIR)
									continue;
						case WATER:
						case LAVA:
						case CACTUS:
						case FIRE:
						case SEAGRASS:
						case KELP_PLANT:
						case KELP:
						case BEDROCK:
						case COBWEB:
							i=255;
							x+=(Math.random()-0.5)*(20);
							z+=(Math.random()-0.5)*(20);
							break;
						default:
							queuePlayerTimer(p);
							new SafeTp(p, tp, x, i+1, z).run();
							return;
						}
					}
				}
			}
		}.start();
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if((cmd.getName().equalsIgnoreCase("rtp")||cmd.getName().equalsIgnoreCase("wild")) && p.hasPermission("rtp.use")) {
				if(args.length!=0) {
					p.sendMessage(ChatColor.DARK_RED+"[RTP]"+ChatColor.RED+" Invalid command.");
					return true;
				}
				if(cooldown.containsKey(p)) {
					p.sendMessage(ChatColor.DARK_RED+"[RTP]"+ChatColor.RED+" You must wait "+(cooldown.get(p)/1000)+" seconds to use that command again.");
				} else {
					this.tpPlayerWhenSafe(p);
				}
				return true;
			}
		}
		return false;
	}
}
class SafeTp implements Runnable {
	private World w;
	private int x,y,z;
	private Player p;
	public SafeTp(Player p,World w,int x,int y,int z) {
		this.w=w;
		this.x=x;
		this.y=y;
		this.z=z;
		this.p=p;
	}
	@Override
	public void run() {
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				p.teleport(new Location(w,x+0.5,y,z+0.5));
				p.sendMessage(ChatColor.DARK_GREEN+"[RTP]"+ChatColor.GREEN+" Teleporting to safe location in world.");
			}
		}.runTask(Main.plugin);
	}
	
}