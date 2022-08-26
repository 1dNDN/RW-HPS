/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package cn.rwhps.server.custom

import cn.rwhps.server.data.plugin.PluginManage
import cn.rwhps.server.plugin.beta.httpapi.ApiMain
import cn.rwhps.server.plugin.beta.noattack.ConnectionLimit
import cn.rwhps.server.plugin.beta.uplist.UpList
import cn.rwhps.server.plugin.beta.upstatistics.GetServerData

internal class LoadCoreCustomPlugin {
    private val core = "[Core Plugin]"
    private val coreEx = "[Core Plugin Extend]"
    private val amusement = "[Amusement Plugin]"
    init {
        PluginManage.addPluginClass("UpList","Dr","$core UpList","1.0", UpList(), mkdir = false, skip = true)
        PluginManage.addPluginClass("DataCollection","Dr","$core DataCollection","1.0", GetServerData(),false)

        PluginManage.addPluginClass("ConnectionLimit","Dr","$coreEx ConnectionLimit","1.0", ConnectionLimit(), mkdir = false, skip = true)
        PluginManage.addPluginClass("HttpApi", "zhou2008", "$coreEx HttpApi", "1.0", ApiMain(), mkdir = true, skip = true)

    }
}