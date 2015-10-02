package com.pcb.pcbridge.ban;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.pcb.pcbridge.PCBridge;
import com.pcb.pcbridge.library.database.AbstractAdapter;

/**
 * Collection of Ban related helper methods
 */

public final class BanHelper 
{
	
	/**
	 * Gets the ban record of the specified username and/or UUID.
	 * Returns null if user is not currently banned.
	 * 
	 * TODO: this should be refactored to be asynchronous!
	 * 
	 * @param adapter
	 * @param name
	 * @param uuid
	 * @return
	 * @throws SQLException
	 */
	public static List<HashMap<String, Object>> LookupPlayer(AbstractAdapter adapter, String name, String uuid) throws SQLException
	{
		String SQL = "SELECT * FROM pcban_active_bans WHERE is_active=1 ";
		List<HashMap<String, Object>> results;
		
		if(name != null && uuid != null)
		{
			results = adapter.Query(SQL + "and (banned_name=? or banned_uuid=?)",
				name, uuid
			);
		}
		else if(uuid == null)
		{
			results = adapter.Query(SQL + "and banned_name=?",
				name
			);
		}
		else
		{
			results = adapter.Query(SQL + "and banned_uuid=?",
				uuid
			);
		}
		
		// TODO: check if its a temp ban and has now expired
			
		return results;		
	}
	
	/**
	 * Checks if the specified username and/or UUID is currently banned
	 * 
	 * @param adapter
	 * @param name
	 * @param uuid
	 * @return
	 * @throws SQLException
	 */
	public static boolean IsPlayerBanned(AbstractAdapter adapter, String name, String uuid) throws SQLException
	{
		List<HashMap<String, Object>> results = LookupPlayer(adapter, name, uuid);
		return (results != null && results.size() > 0);
	}
	
	/**
	 * Retrieves a player's UUID regardless of online state or join history
	 * 
	 * @param plugin
	 * @param username
	 * @return
	 */
	public static PlayerUUID GetUUID(PCBridge plugin, String username)
	{
		UUID uuid = null;
		String ip = "";
		
		// check if player is currently online
		@SuppressWarnings("deprecation")
		Player player = plugin.getServer().getPlayer(username);
		
		if(player != null)
		{
			uuid 	= player.getUniqueId();
			ip 		= player.getAddress().getHostString();
			
			return new PlayerUUID(username, ip, uuid, true, true);
		}
		
		// otherwise check if player has played before
		@SuppressWarnings("deprecation")
		OfflinePlayer offlinePlayer	= plugin.getServer().getOfflinePlayer(username);
		
		boolean hasPlayedBefore = offlinePlayer.hasPlayedBefore();			
		if(hasPlayedBefore)
		{
			uuid = offlinePlayer.getUniqueId();
		}
		else
		{
			uuid = null;
			
			// TODO: retrieve the player's UUID from Mojang since they've never joined the server before
			// (UUID cannot be accurately determined if they've never joined the server)
		}
		
		return new PlayerUUID(username, ip, uuid, false, hasPlayedBefore);
			
	}
}
