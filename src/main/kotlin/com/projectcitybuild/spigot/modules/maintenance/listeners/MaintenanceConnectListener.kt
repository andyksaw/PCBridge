package com.projectcitybuild.spigot.modules.maintenance.listeners

import com.projectcitybuild.api.APIProvider
import com.projectcitybuild.core.contracts.EnvironmentProvider
import com.projectcitybuild.core.contracts.Listenable
import com.projectcitybuild.entities.PluginConfig
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerLoginEvent

class MaintenanceConnectListener : Listenable<PlayerLoginEvent> {
    override var environment: EnvironmentProvider? = null
    override var apiProvider: APIProvider? = null

    @EventHandler(priority = EventPriority.HIGHEST)
    override fun observe(event: PlayerLoginEvent) {
        val environment = environment ?: return

        val isMaintenanceMode = environment.get(PluginConfig.Settings.MAINTENANCE_MODE()) as? Boolean
            ?: throw Exception("Cannot cast MAINTENANCE_MODE value to Boolean")

        if (event.player.hasPermission("pcbridge.maintenance.bypass")) {
            return
        }

        if (isMaintenanceMode) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "Server is currently in maintenance mode. Please try again later")
        }
    }
}