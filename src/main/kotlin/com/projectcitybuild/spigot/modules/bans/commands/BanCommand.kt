package com.projectcitybuild.spigot.modules.bans.commands

import com.projectcitybuild.core.contracts.Commandable
import com.projectcitybuild.core.contracts.EnvironmentProvider
import com.projectcitybuild.spigot.extensions.getOfflinePlayer
import com.projectcitybuild.actions.CreateBanAction
import com.projectcitybuild.api.APIProvider
import com.projectcitybuild.core.extensions.joinWithWhitespaces
import org.bukkit.ChatColor
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class BanCommand: Commandable {

    override var environment: EnvironmentProvider? = null
    override var apiProvider: APIProvider? = null
    override val label: String = "ban"
    override val permission: String = "pcbridge.ban.ban"

    override fun execute(sender: CommandSender, args: Array<String>, isConsole: Boolean): Boolean {
        val environment = environment ?: throw Exception("EnvironmentProvider is null")

        if (args.isEmpty()) return false

        val staffPlayer = if(isConsole) null else sender as Player
        val reason = args.joinWithWhitespaces(1 until args.size)
        val targetPlayerName = args.first()

        getOfflinePlayerUUID(server = sender.server, playerName = targetPlayerName) { uuid ->
            if (uuid == null) {
                environment.sync {
                    sender.sendMessage("Error: Failed to retrieve UUID of given player")
                }
                return@getOfflinePlayerUUID
            }

            createBan(playerId = uuid, playerName = targetPlayerName, staffId = staffPlayer?.uniqueId, reason = reason) { result ->
                environment.sync {
                    when (result) {
                        is CreateBanAction.Result.FAILED -> {
                            val message = when (result.reason) {
                                CreateBanAction.Failure.PLAYER_ALREADY_BANNED -> "${args.first()} is already banned"
                                CreateBanAction.Failure.BAD_REQUEST -> "Bad request sent to the ban server. Please contact an administrator to have this fixed"
                                CreateBanAction.Failure.DESERIALIZE_FAILED -> "Error: Bad response received from the ban server. Please contact an admin"
                            }
                            sender.sendMessage(message)
                        }

                        is CreateBanAction.Result.SUCCESS -> {
                            sender.server.broadcast("${ChatColor.GRAY}${args.first()} has been banned by ${sender.name}: ${reason ?: "No reason given"}", "*")

                            val player = sender.server.onlinePlayers.first { player ->
                                player.name.toLowerCase() == targetPlayerName.toLowerCase()
                            }
                            player?.kickPlayer("You have been banned")
                        }
                    }

                }
            }
        }

        return true
    }

    private fun getOfflinePlayerUUID(server: Server, playerName: String, completion: (UUID?) -> Unit) {
        val environment = environment ?: throw Exception("EnvironmentProvider is null")
        val apiProvider = apiProvider ?: throw Exception("API provider is null")

        environment.async<UUID?> { resolve ->
            val uuid = server.getOfflinePlayer(
                    name = playerName,
                    environment = environment,
                    apiProvider = apiProvider
            )
            resolve(uuid)
        }.startAndSubscribe(completion)
    }

    private fun createBan(playerId: UUID, playerName: String, staffId: UUID?, reason: String?, completion: (CreateBanAction.Result) -> Unit) {
        val environment = environment ?: throw Exception("EnvironmentProvider is null")
        val apiProvider = apiProvider ?: throw Exception("API provider is null")

        environment.async<CreateBanAction.Result> { resolve ->
            val action = CreateBanAction(environment, apiProvider)
            val result = action.execute(playerId, playerName, staffId, reason)
            resolve(result)
        }.startAndSubscribe(completion)
    }

}