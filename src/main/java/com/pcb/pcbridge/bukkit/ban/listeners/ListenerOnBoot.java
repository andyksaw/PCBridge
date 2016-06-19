package com.pcb.pcbridge.bukkit.ban.listeners;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;
import com.pcb.pcbridge.PCBridge;
import com.pcb.pcbridge.bukkit.ban.Ban;
import com.pcb.pcbridge.bukkit.ban.cache.BanCache;
import com.pcb.pcbridge.library.database.DbConn;
import com.pcb.pcbridge.library.database.adapters.AbstractAdapter;

/**
 * A special listener that invokes when PCBridge is enabled via onEnable()
 */

public class ListenerOnBoot 
{
	private PCBridge _plugin;
	
	public ListenerOnBoot(PCBridge plugin)
	{
		this._plugin = plugin;
	}
	
	/**
	 * Retrieves all ban records and stores it in cache
	 * 
	 * @param plugin
	 */
	public void Execute()
	{
		_plugin.getLogger().info("Building ban list cache...");
		
		AbstractAdapter adapter = _plugin.GetAdapter(DbConn.REMOTE);
		List<HashMap<String, Object>> result;
		try 
		{
			result = adapter.Query("SELECT * FROM banlist WHERE id>?",
				-1
			);	
		} 
		catch (SQLException err) 
		{
			_plugin.getLogger().severe("Failed to retrieve ban records for caching: " + err.getMessage());
			err.printStackTrace();
			return;
		}		
		
		int x = 0;
		ListIterator<HashMap<String, Object>> i = result.listIterator();		
		while(i.hasNext())
		{
			HashMap<String, Object> row = i.next();
			Ban ban = new Ban(row);
			_plugin.GetBanCache().Remember(ban.Name, ban, null);
			x++;
		}
		
		_plugin.getLogger().info("Build complete - " + x + " entries in memory");
		
		
		CheckedForBannedUsers();
	}
	
	/**
	 * Check if any online player is on the active ban list
	 */
	private void CheckedForBannedUsers()
	{
		_plugin.getLogger().info("Checking online players for existing bans...");
		
		BanCache cache = _plugin.GetBanCache();		
		
	    for (World world : Bukkit.getWorlds()) {
			for(Player player : world.getPlayers())
			{
				List<Ban> entries = cache.Get(player.getName());
				if(entries == null)
					continue;
				
				ListIterator<Ban> i = entries.listIterator();
				while(i.hasNext())
				{
					Ban entry = i.next();
					if(entry.IsActive)
					{
						// ban exists - boot the player
						player.kickPlayer("You have been kicked due to an existing ban");
						break;
					}
				}
			}
	    }
	    
	}
	
}
