package com.projectcitybuild.platforms.bungeecord

import com.projectcitybuild.core.entities.PluginConfig
import com.projectcitybuild.core.network.NetworkClients
import com.projectcitybuild.core.network.mojang.client.MojangClient
import com.projectcitybuild.core.network.pcb.client.PCBClient
import com.projectcitybuild.platforms.bungeecord.commands.CheckBanCommand
import com.projectcitybuild.platforms.bungeecord.environment.BungeecordConfig
import com.projectcitybuild.platforms.bungeecord.environment.BungeecordLogger
import com.projectcitybuild.platforms.bungeecord.environment.BungeecordScheduler
import com.projectcitybuild.platforms.bungeecord.listeners.BanConnectionListener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File

class BungeecordPlatform: Plugin() {

    private val bungeecordLogger = BungeecordLogger(logger = this.logger)
    private val scheduler = BungeecordScheduler(plugin = this)
    private val config = BungeecordConfig(plugin = this)
    private var commandDelegate: BungeecordCommandDelegate? = null
    private var listenerDelegate: BungeecordListenerDelegate? = null

    private val networkClients: NetworkClients by lazy {
        createAPIProvider()
    }

    override fun onEnable() {
        createDefaultConfig()

        val commandDelegate = BungeecordCommandDelegate(plugin = this, logger = bungeecordLogger)
        registerCommands(commandDelegate)
        this.commandDelegate = commandDelegate

        val listenerDelegate = BungeecordListenerDelegate(plugin = this, logger = bungeecordLogger)
        registerListeners(listenerDelegate)
        this.listenerDelegate = listenerDelegate
    }

    override fun onDisable() {
        listenerDelegate?.unregisterAll()

        commandDelegate = null
        listenerDelegate = null
    }

    private fun registerCommands(delegate: BungeecordCommandDelegate) {
        arrayOf(
                CheckBanCommand(proxy, scheduler, networkClients)
        )
        .forEach { command -> delegate.register(command) }
    }

    private fun registerListeners(delegate: BungeecordListenerDelegate) {
        arrayOf(
                BanConnectionListener(networkClients)
        )
        .forEach { listener -> delegate.register(listener) }
    }

    private fun createDefaultConfig() {
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        val file = File(dataFolder, "config.yml")

        if (!file.exists()) {
            // FIXME
//            try {
//                getResourceAsStream("config.yml").use { `in` ->
//                    val out = FileOutputStream(file)
//                    ByteStreams.copy(in, out)
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
        }

        if (!file.exists()) {
            val configProvider = ConfigurationProvider
                    .getProvider(YamlConfiguration::class.java)

            val config = configProvider.load(file)

            config.set(PluginConfig.Settings.MAINTENANCE_MODE().key, false)
            config.set(PluginConfig.API.KEY().key, "")
            config.set(PluginConfig.API.BASE_URL().key, "")
            config.set(PluginConfig.API.IS_LOGGING_ENABLED().key, false)

            configProvider.save(config, file)
        }
    }

    private fun createAPIProvider(): NetworkClients {
        val isLoggingEnabled = config.get(PluginConfig.API.IS_LOGGING_ENABLED())

        val pcbClient = PCBClient(
                authToken = config.get(PluginConfig.API.KEY()),
                baseUrl = config.get(PluginConfig.API.BASE_URL()),
                withLogging = isLoggingEnabled
        )
        val mojangClient = MojangClient(
                withLogging = isLoggingEnabled
        )
        return NetworkClients(pcb = pcbClient, mojang = mojangClient)
    }
}