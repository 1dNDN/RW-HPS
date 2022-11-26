/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package cn.rwhps.server.game.simulation

import cn.rwhps.server.core.Core
import cn.rwhps.server.data.global.Data
import cn.rwhps.server.data.global.NetStaticData
import cn.rwhps.server.game.GameStartInit
import cn.rwhps.server.game.simulation.gameFramework.GameNet
import cn.rwhps.server.net.core.IRwHps
import cn.rwhps.server.plugin.event.AbstractGlobalEvent
import cn.rwhps.server.util.log.Log

class GameHeadlessEventGlobal : AbstractGlobalEvent {
    var newGameInit = false
    var newGameFlag = false

    override fun registerGameLibLoadEvent() {
        if (newGameFlag) {
            return
        }
        newGameFlag = true

        GameNet.newConnect()
        Log.clog(Data.i18NBundle.getinput("server.load.end"))
        Log.clog("Run GameHeadless")
    }

    override fun registerServerStartTypeEvent(serverNetType: IRwHps.NetType) {
        if (NetStaticData.ServerNetType != IRwHps.NetType.ServerProtocol) {
            Log.clog(Data.i18NBundle.getinput("server.load.end"))
            return
        }

        if (newGameInit) {
            Log.clog("拒绝多次启动")
            Core.exit()
            return
        }
        newGameInit = true

        GameStartInit.init()

        Log.clog(Data.i18NBundle.getinput("server.load.headless"))
        com.corrodinggames.rts.java.Main.main(arrayOf("-disable_vbos","-disable_atlas","-nomusic","-nosound","-nodisplay"))
    }
}