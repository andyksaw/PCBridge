package com.pcb.pcbridge.library.controllers.listeners;

import org.bukkit.event.Listener;

/**
 * Interface for controllers that have event listeners to register
 */

public interface IListenerController
{	
	public Listener[] GetListeners();
}
