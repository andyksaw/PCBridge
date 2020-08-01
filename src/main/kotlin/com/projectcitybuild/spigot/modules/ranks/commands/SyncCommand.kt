package com.projectcitybuild.spigot.modules.ranks.commands

import com.projectcitybuild.api.APIProvider
import com.projectcitybuild.core.contracts.Commandable
import com.projectcitybuild.core.contracts.EnvironmentProvider
import com.projectcitybuild.entities.CommandInput
import com.projectcitybuild.entities.models.ApiResponse
import com.projectcitybuild.entities.models.AuthPlayerGroups
import com.projectcitybuild.entities.models.AuthURL
import com.projectcitybuild.spigot.modules.ranks.RankMapper
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.types.InheritanceNode
import org.bukkit.entity.Player
import retrofit2.Response
import java.util.*
import java.util.stream.Collectors

class SyncCommand(
        private val environment: EnvironmentProvider,
        private val apiProvider: APIProvider
): Commandable {

    override val label: String = "sync"
    override val permission: String = "pcbridge.sync.login"

    override fun execute(input: CommandInput): Boolean {
        if (input.sender !is Player) {
            input.sender.sendMessage("Console cannot use this command")
            return true
        }

        if (input.args.isEmpty()) {
            return beginSyncFlow(input.sender, environment, apiProvider)
        }
        if (input.args.size == 1 && input.args[0] == "finish") {
            return endSyncFlow(input.sender, environment)
        }
        return false
    }

    private fun beginSyncFlow(sender: Player, environment: EnvironmentProvider, apiProvider: APIProvider): Boolean {
        getVerificationLink(playerId = sender.uniqueId) { response ->
            val json = response.body()

            // TODO: handle error serialization in APIClient...
            if (!response.isSuccessful) {
                val annotation = object : Annotation {}
                val converter = apiProvider.pcb.instance
                        .responseBodyConverter<ApiResponse<AuthURL>>(ApiResponse::class.java, arrayOf(annotation))

                val body = response.errorBody() ?: throw Exception("Error body deserialization failed")
                val model = converter.convert(body)

                environment.sync {
                    if (model?.error?.id == "already_authenticated") {
                        sender.sendMessage("Error: You have already linked your account")
                    } else {
                        sender.sendMessage("Failed to fetch verification URL: ${model?.error?.detail}")
                    }
                }
                return@getVerificationLink
            }

            environment.sync {
                if (json?.error != null) {
                    sender.sendMessage("Failed to fetch verification URL: ${json.error.detail}")
                    return@sync
                }
                if (json?.data?.url == null) {
                    sender.sendMessage("Server failed to generate verification URL. Please try again later")
                    return@sync
                }
                sender.sendMessage("To link your account, please click the link and login if required:§9 ${json.data.url}")
            }
        }

        return true
    }

    private fun endSyncFlow(sender: Player, environment: EnvironmentProvider): Boolean {
        val permissions = environment.permissions ?: throw Exception("Permission plugin is null")

        getPlayerGroups(playerId = sender.uniqueId) { result ->
            environment.sync {
                val json = result.body()
                if (json?.error != null) {
                    sender.sendMessage("Sync failed: Trouble communicating with the authentication server")
                    return@sync
                }

                val lpUser = permissions.userManager.getUser(sender.uniqueId)
                if (lpUser == null) {
                    sender.sendMessage("Sync failed: Could not load user from permission system. Please contact a staff member")
                    throw Exception("Could not load user from LuckPerms")
                }

                // Remove all groups from the player before syncing
                lpUser.nodes.stream()
                        .filter(NodeType.INHERITANCE::matches)
                        .map(NodeType.INHERITANCE::cast)
                        .collect(Collectors.toSet())
                        .forEach { groupNode ->
                            lpUser.data().remove(groupNode)
                        }

                if (json?.data == null) {
                    val groupNode = InheritanceNode.builder("guest").build()
                    lpUser.data().add(groupNode)

                    sender.sendMessage("No account found: Set to Guest")
                    return@sync
                }

                val permissionGroups = RankMapper.mapGroupsToPermissionGroups(json.data.groups)
                permissionGroups.forEach { group ->
                    val groupNode = InheritanceNode.builder(group).build()
                    if (!lpUser.nodes.contains(groupNode)) {
                        lpUser.data().add(groupNode)
                    }
                }

                // Just in case, assign to Guest if no groups available (shouldn't happen though)
                if (permissionGroups.isEmpty()) {
                    val groupNode = InheritanceNode.builder("guest").build()
                    lpUser.data().add(groupNode)
                }

                permissions.userManager.saveUser(lpUser)

                sender.sendMessage("Account successfully linked. Your rank will be automatically synchronized with the PCB network")
            }
        }

        return true
    }

    private fun getVerificationLink(playerId: UUID, completion: (Response<ApiResponse<AuthURL>>) -> Unit) {
        val authApi = apiProvider.pcb.authApi

        environment.async<Response<ApiResponse<AuthURL>>> { resolve ->
            val request = authApi.getVerificationUrl(uuid = playerId.toString())
            val response = request.execute()

            resolve(response)
        }.startAndSubscribe(completion)
    }

    private fun getPlayerGroups(playerId: UUID, completion: (Response<ApiResponse<AuthPlayerGroups>>) -> Unit) {
        val authApi = apiProvider.pcb.authApi

        environment.async<Response<ApiResponse<AuthPlayerGroups>>> { resolve ->
            val request = authApi.getUserGroups(uuid = playerId.toString())
            val response = request.execute()

            resolve(response)
        }.startAndSubscribe(completion)
    }
}
