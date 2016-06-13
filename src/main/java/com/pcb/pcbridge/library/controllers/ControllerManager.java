package com.pcb.pcbridge.library.controllers;

import java.util.HashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import com.pcb.pcbridge.PCBridge;

/**
 * Instantiates controllers and registers each of their commands & listeners with Bukkit
 */

public final class ControllerManager implements CommandExecutor
{
	public HashMap<String, AbstractCommand> Commands = new HashMap<String, AbstractCommand>();
	private PCBridge _plugin;

	public ControllerManager(PCBridge plugin)
	{
		this._plugin = plugin;
	}
	

	public void CreateControllers(AbstractController[] controllers)
	{
		for(AbstractController controller : controllers)
		{			
			if(controller instanceof ICommandController)
				RegisterCommands((ICommandController)controller);
			
			if(controller instanceof IListenerController)
				RegisterListeners((IListenerController)controller);
			
			controller.OnBoot(_plugin);
		}
	}
	
	/**
	 * Register all commands contained within the provided controller
	 * 
	 * @param controller
	 */
	private void RegisterCommands(ICommandController controller)
	{
		CommandRoute[] routes = controller.GetCommands();
		for(CommandRoute route : routes)
		{
			try
			{
				_plugin.getCommand(route.Alias).setExecutor(this);
				Commands.put(route.Alias, route.Command);
			}
			catch(NullPointerException e)
			{
				_plugin.getLogger().info("ERROR: Failed to register command [" + route.Alias + "] - missing definition in plugin.yml");
			}
			
			route.Command.SetPlugin(_plugin);
		}			
	}
	
	/**
	 * Register all listeners contained within the provided controller
	 * 
	 * @param controller
	 */
	private void RegisterListeners(IListenerController controller)
	{
		Listener[] listeners = controller.GetListeners();
		for(Listener listener : listeners)
		{
			_plugin.getServer().getPluginManager().registerEvents(listener, _plugin);
			((AbstractListener)listener).SetPlugin(_plugin);
		}
	}
	
	/**
	 * Listen for commands and route them to an appropriate command handler
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) 
	{
		AbstractCommand handler = Commands.get( command.getName().toLowerCase() );
		
		if(handler != null)
		{
			CommandArgs packet = new CommandArgs(sender, label, args);
			return handler.Execute(packet);
		}
		
		return false;
	}
	
	
	/**
	 * Manually invokes a command route via code
	 * 
	 * @param e			Command data
	 * @param routeName	The route to invoke
	 * @param args		Any extra args to supply to the route
	 * @return
	 */
	public boolean InvokeRoute(CommandArgs e, String routeName, Object... args)
	{
		AbstractCommand handler = Commands.get( routeName );
		
		if(args.length > 0)
			e.RouteArgs = args;
		
		if(handler != null)
			return handler.Execute(e);
				
		return false;
	}
	
	
	
}
