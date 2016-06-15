package com.pcb.pcbridge.bukkit.ban.listeners;

import java.util.List;
import java.util.ListIterator;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import com.pcb.pcbridge.bukkit.ban.Ban;
import com.pcb.pcbridge.bukkit.ban.BanCache;
import com.pcb.pcbridge.library.controllers.AbstractListener;

/**
 * Check if a player is banned upon entry to the server
 */

public final class ListenerOnPlayerLogin extends AbstractListener implements Listener
{
	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnPlayerLogin(PlayerLoginEvent e)
	{		
		String username = e.getPlayer().getName();

		BanCache cache = _plugin.GetBanCache();
		List<Ban> entries = cache.Get(username);
		
		if(entries != null)
		{
			ListIterator<Ban> i = entries.listIterator();		
			while(i.hasNext())
			{
				Ban entry = i.next();
				
				if(entry.IsActive)
				{
					String message = "��c" + "You are currently banned.\n\n" +
							
							 "��8" + "Reason: ��f" + entry.Reason + "\n" +
							 "��8" + "Expires: ��f" + "Never" + "\n\n" + 
									 
							 "��b" + "Appeal @ www.projectcitybuild.com";
			
					
					e.disallow(PlayerLoginEvent.Result.KICK_BANNED, message);
				}
			}		
			
			
		}
		
	}
}
