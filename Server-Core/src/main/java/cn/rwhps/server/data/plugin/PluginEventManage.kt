/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package cn.rwhps.server.data.plugin

import cn.rwhps.server.data.global.Data
import cn.rwhps.server.game.event.EventGlobalType.*
import cn.rwhps.server.game.event.EventType.*
import cn.rwhps.server.plugin.event.AbstractEvent
import cn.rwhps.server.plugin.event.AbstractGlobalEvent
import cn.rwhps.server.struct.Seq
import cn.rwhps.server.util.game.Events
import cn.rwhps.server.util.threads.GetNewThreadPool

/**
 * New plugin event manager
 * @author RW-HPS/Dr
 */
internal class PluginEventManage {
    companion object {
        private val pluginEventData = Seq<AbstractEvent>(8)
        private val pluginGlobalEventData = Seq<AbstractGlobalEvent>(8)
        private val executorService = GetNewThreadPool.getNewFixedThreadPool(5, "PluginEventASync-")
        @JvmStatic
        fun add(abstractEvent: AbstractEvent) {
            pluginEventData.add(abstractEvent)
        }
        @JvmStatic
        fun add(abstractGlobalEvent: AbstractGlobalEvent) {
            pluginGlobalEventData.add(abstractGlobalEvent)
        }

        private fun registerEventAll() {
            /* Sync */
            Events.on(PlayerJoinEvent::class.java) { e: PlayerJoinEvent ->
                pluginEventData.eachAll { p: AbstractEvent ->
                    p.registerPlayerJoinEvent(e.player)
                }
            }
            /* Sync */
            Events.on(PlayerReJoinEvent::class.java) { e: PlayerReJoinEvent ->
                pluginEventData.eachAll { p: AbstractEvent ->
                    p.registerPlayerReJoinEvent(e.player)
                }
            }
            /* Sync */
            Events.on(PlayerConnectPasswdCheckEvent::class.java) { e: PlayerConnectPasswdCheckEvent ->
                pluginEventData.eachAll { p: AbstractEvent ->
                    val strings = p.registerPlayerConnectPasswdCheckEvent(e.abstractNetConnect, e.passwd)
                    e.result = strings[0].toBoolean()
                    e.name = strings[1]
                }
            }

            /* ASync */
            Events.on(PlayerConnectEvent::class.java) { e: PlayerConnectEvent ->
                executorService.execute {
                    pluginEventData.eachAll { p: AbstractEvent ->
                        p.registerPlayerConnectEvent(e.player)
                    }
                }
            }
            /* ASync */
            Events.on(PlayerLeaveEvent::class.java) { e: PlayerLeaveEvent ->
                executorService.execute {
                    pluginEventData.eachAll { p: AbstractEvent ->
                        p.registerPlayerLeaveEvent(e.player)
                    }
                }
            }
            /* ASync */
            Events.on(PlayerChatEvent::class.java) { e: PlayerChatEvent ->
                executorService.execute {
                    pluginEventData.eachAll { p: AbstractEvent ->
                        p.registerPlayerChatEvent(e.player, e.message)
                    }
                }
            }
            /* ASync */
            Events.on(GameStartEvent::class.java) { _: GameStartEvent? ->
                executorService.execute {
                    pluginEventData.eachAll { obj: AbstractEvent ->
                        obj.registerGameStartEvent()
                    }
                }
            }
            /* ASync */
            Events.on(GameOverEvent::class.java) { _: GameOverEvent? ->
                if (Data.game.isGameover) {
                    pluginEventData.eachAll { obj: AbstractEvent ->
                        obj.registerGameOverEvent()
                    }
                }
            }
            /* ASync */
            Events.on(PlayerBanEvent::class.java) { e: PlayerBanEvent ->
                executorService.execute {
                    pluginEventData.eachAll { p: AbstractEvent ->
                        p.registerPlayerBanEvent(e.player)
                    }
                }
            }
            /* ASync */
            Events.on(PlayerUnbanEvent::class.java) { e: PlayerUnbanEvent ->
                executorService.execute {
                    pluginEventData.eachAll { p: AbstractEvent ->
                        p.registerPlayerUnbanEvent(e.player)
                    }
                }
            }
            /* ASync */
            Events.on(PlayerIpBanEvent::class.java) { e: PlayerIpBanEvent ->
                executorService.execute {
                    pluginEventData.eachAll { p: AbstractEvent ->
                        p.registerPlayerIpBanEvent(e.player)
                    }
                }
            }
            /* ASync */
            Events.on(PlayerIpUnbanEvent::class.java) { e: PlayerIpUnbanEvent ->
                executorService.execute {
                    pluginEventData.eachAll { p: AbstractEvent ->
                        p.registerPlayerIpUnbanEvent(e.ip)
                    }
                }
            }
        }

        private fun registerGlobalEventAll() {
            /* Sync */
            Events.on(GameLibLoadEvent::class.java) { _: GameLibLoadEvent ->
                pluginGlobalEventData.eachAll { obj: AbstractGlobalEvent ->
                    obj.registerGameLibLoadEvent()
                }
            }

            /* Sync */
            // 不应该 ASync 避免部分配置未加载
            Events.on(ServerLoadEvent::class.java) { _: ServerLoadEvent ->
                pluginGlobalEventData.eachAll { obj: AbstractGlobalEvent ->
                    obj.registerServerLoadEvent()
                }
            }

            Events.on(ServerStartTypeEvent::class.java) { e: ServerStartTypeEvent ->
                pluginGlobalEventData.eachAll { obj: AbstractGlobalEvent ->
                    obj.registerServerStartTypeEvent(e.serverNetType)
                }
            }

            /* Sync */
            Events.on(NewConnectEvent::class.java) { e: NewConnectEvent ->
                pluginGlobalEventData.eachAll { obj: AbstractGlobalEvent ->
                    if (obj.registerNewConnectEvent(e.connectionAgreement)) {
                        e.result = true
                    }
                }
            }

            /* Sync */
            Events.on(NewCloseEvent::class.java) { e: NewCloseEvent ->
                pluginGlobalEventData.eachAll { obj: AbstractGlobalEvent ->
                    obj.registerNewCloseEvent(e.connectionAgreement)
                }
            }
        }
    }

    init {
        registerEventAll()
        registerGlobalEventAll()
    }
}