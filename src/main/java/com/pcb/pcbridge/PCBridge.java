package com.pcb.pcbridge;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.pcb.pcbridge.ban.BanController;
import com.pcb.pcbridge.library.UUIDLookup;
import com.pcb.pcbridge.library.controllers.AbstractController;
import com.pcb.pcbridge.library.controllers.ControllerManager;
import com.pcb.pcbridge.library.database.ConnectionManager;
import com.pcb.pcbridge.library.database.DbConn;
import com.pcb.pcbridge.library.database.adapters.AbstractAdapter;
import com.pcb.pcbridge.library.database.adapters.Adapter;
import com.pcb.pcbridge.players.PlayerController;
import com.pcb.pcbridge.players.PlayerManager;
import com.pcb.pcbridge.ranks.RankController;
import com.pcb.pcbridge.swearblock.SwearBlockController;
import com.pcb.pcbridge.utility.UtilityController;

/**
 * 
 * A plugin to bridge www.projectcitybuild.com and its servers
 * 
 * @author		Andy Saw <andy-saw@hotmail.com>
 * @created		19th September, 2015
 * 
 */

public final class PCBridge extends JavaPlugin 
{
	private ControllerManager _controllerManager;
	private ConnectionManager _connectionManager;
	private PlayerManager _playerManager;
	private UUIDLookup _uuidFetcher;
	private Permission _permissions;
	
	public AbstractAdapter GetAdapter(DbConn name)
	{
		return _connectionManager.GetAdapter(name);
	}
	
	public ControllerManager GetControllerManager()
	{
		return _controllerManager;
	}
	
	public UUIDLookup GetUUIDFetcher()
	{
		return _uuidFetcher;
	}
	
	public PlayerManager GetPlayerManager()
	{
		return _playerManager;
	}
	
	public Permission GetPermissions()
	{
		return _permissions;
	}
	
	@Override
	public void onEnable()
	{
		LoadConfig();
		HookPermissions();
		
		_uuidFetcher = new UUIDLookup();
		
		_connectionManager = new ConnectionManager(this);
		_connectionManager
			.AddAdapter(DbConn.LOCAL, Adapter.MYSQL)
			.AddAdapter(DbConn.REMOTE, Adapter.MYSQL);
		
		_controllerManager = new ControllerManager(this);
		_controllerManager.CreateControllers(new AbstractController[] 
		{
			new PlayerController(),
			new RankController(),
			new BanController(),
			new SwearBlockController(),
			new UtilityController()
		});		
		
		_playerManager = new PlayerManager(this);
		_playerManager.BuildPlayerList();
	}
	
	@Override
	public void onDisable()
	{
	}
	
	/**
	 * Hook into the permissions API via Vault
	 * 
	 * @return
	 */
	private void HookPermissions()
	{
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		if(rsp == null)
		{
			getLogger().severe("No permissions plugin to hook into!");
			return;
		}
		
		_permissions = rsp.getProvider();

		if(_permissions == null)
		{
			getLogger().severe("Failed to hook into permissions API");
			return;
		}
		else
		{
			getLogger().info("Hooked into permissions API");
		}
	    
	}
	
	/**
	 * Adds any missing config entries to the config
	 * (this should probably be moved into its own class if it gets any more complex)
	 */
	private void LoadConfig()
	{
		getConfig().addDefault("database.remote.address", "192.184.93.126");
		getConfig().addDefault("database.remote.port", 3306);
		getConfig().addDefault("database.remote.username", "root");
		getConfig().addDefault("database.remote.password", "");
		getConfig().addDefault("database.remote.database", "pcbridge_remote");
		
		getConfig().addDefault("database.local.address", "localhost");
		getConfig().addDefault("database.local.port", 3306);
		getConfig().addDefault("database.local.username", "root");
		getConfig().addDefault("database.local.password", "");
		getConfig().addDefault("database.local.database", "pcbridge_local");
		
		getConfig().addDefault("database.boot_test_connection", true);
		getConfig().addDefault("database.first_run", true);
		
		getConfig().addDefault("database.forum.database", "pcbridge_forums");
		
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
}
