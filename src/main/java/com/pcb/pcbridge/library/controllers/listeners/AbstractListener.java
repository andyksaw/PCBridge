package com.pcb.pcbridge.library.controllers.listeners;

import com.pcb.pcbridge.PCBridge;

public abstract class AbstractListener 
{
	protected PCBridge _plugin;
	
	public void SetPlugin(PCBridge plugin)
	{
		this._plugin = plugin;
	}
}
