package com.projectcitybuild.spigot.modules.maintenance.commands

import com.projectcitybuild.core.contracts.Commandable
import com.projectcitybuild.core.contracts.EnvironmentProvider
import com.projectcitybuild.entities.CommandInput
import com.projectcitybuild.entities.PluginConfig

class MaintenanceCommand(
        private val environment: EnvironmentProvider
): Commandable {

    override val label: String = "maintenance"
    override val permission: String = "pcbridge.maintenance"

    enum class MaintenanceMode {
        ON, OFF
    }

    override fun execute(input: CommandInput): Boolean {
        if (input.args.size > 1) return false

        val isMaintenanceMode = environment.get(PluginConfig.Settings.MAINTENANCE_MODE()) as? Boolean
                ?: throw Exception("Cannot cast MAINTENANCE_MODE value to Boolean")

        if (input.args.isEmpty()) {
            if (isMaintenanceMode) {
                input.sender.sendMessage("Server is currently in maintenance mode")
            } else {
                input.sender.sendMessage("Server is not in maintenance mode")
            }
            return true
        }

        val newValueInput = input.args.first().toLowerCase()
        if (!MaintenanceMode.values().map { it.name }.contains(newValueInput)) {
            return false
        }

        when (MaintenanceMode.valueOf(newValueInput)) {
            MaintenanceMode.ON ->
                if (isMaintenanceMode) {
                    input.sender.sendMessage("Server is already in maintenance mode")
                } else {
                    environment.set(PluginConfig.Settings.MAINTENANCE_MODE(), true)
                    input.sender.sendMessage("Server is now in maintenance mode")
                }

            MaintenanceMode.OFF ->
                if (!isMaintenanceMode) {
                    input.sender.sendMessage("Server is not in maintenance mode")
                } else {
                    environment.set(PluginConfig.Settings.MAINTENANCE_MODE(), false)
                    input.sender.sendMessage("Server is now open to all players")
                }
        }

        return true
    }
}