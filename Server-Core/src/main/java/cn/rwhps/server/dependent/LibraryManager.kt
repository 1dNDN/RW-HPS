/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package cn.rwhps.server.dependent

import cn.rwhps.server.data.global.Data
import cn.rwhps.server.io.input.DisableSyncByteArrayInputStream
import cn.rwhps.server.net.HttpRequestOkHttp
import cn.rwhps.server.struct.Seq
import cn.rwhps.server.util.file.FileUtil
import cn.rwhps.server.util.log.Log
import cn.rwhps.server.util.log.exp.LibraryManagerError
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.project.MavenProject
import java.io.File
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile


class LibraryManager : AgentAttachData() {
    private val source = Seq<UrlData>()

    fun addSource(source: UrlData) {
        this.source.add(source)
    }

    /**
     * 导入本地的依赖
     * @param file FileUtil
     */
    fun customImportLib(file: FileUtil) {
        dependenciesFile.add(file.file)
    }

    @Throws(LibraryManagerError.DependencyNotFoundException::class)
    @JvmOverloads
    fun implementation(text: String, block: (LibraryManager.() -> Unit)? = null) {
        block?.run { this() }
        importLib0(text)
    }

    @Throws(LibraryManagerError.DependencyNotFoundException::class)
    @JvmOverloads
    fun implementation(group: String, module: String, version: String, block: (LibraryManager.() -> Unit)? = null) {
        block?.run { this() }
        importLib0(group, module, version)
    }

    fun exclude(group: String, module: String) {
        tempGroup.add(ImportGroupData(group,module,""))
    }

    fun loadToClassLoader() {
        Log.clog(Data.i18NBundle.getinput("server.load.jar"));
        load();
        val loader = getClassLoader()
        load.eachAll {
            if (!loadEnd.contains(it)) {
                if (loader(it)) {
                    loadEnd.add(it)
                }
            }
        }
        dependenciesFile.eachAll {
            if (!loadEnd.contains(it)) {
                if (loader(it)) {
                    loadEnd.add(it)
                }
            }
        }
    }

    /**
     * 下载依赖
     */
    private fun load() {
        dependenciesDown.eachAll {
            val file = FileUtil.getFolder(Data.Plugin_Lib_Path).toFile(it.fileName).file
            if (!file.exists()) {
                HttpRequestOkHttp.downUrl(it.getDownUrl(),file).also {
                    if (it) {
                        load.add(file)
                    } else {
                        Log.clog("Download Failed ${file.name}")
                    }
                }
            }
        }
    }

    /**
     * 获取一个加载器URLClassLoader
     */
    private fun getClassLoader(): (File)->Boolean {
        return if (ClassLoader.getSystemClassLoader() is URLClassLoader) {
            val f: Method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
            f.isAccessible = true

            { file: File ->
                try {
                    f.invoke(ClassLoader.getSystemClassLoader(), file.toURI().toURL())
                    Log.info("Load Lib Jar", file.name)
                    true
                } catch (classLoad: Exception) {
                    Log.error("Jar 1.8 Load", classLoad)
                    false
                }
            }
        } else {
            { file: File ->
                try {
                    instrumentation.appendToSystemClassLoaderSearch(JarFile(file))
                    Log.info("Load Lib Jar", file.name)
                    true
                } catch (classLoad: Exception) {
                    Log.error("Jar 1.8+ Load", classLoad)
                    false
                }
            }
        }
    }

    @Throws(LibraryManagerError.DependencyNotFoundException::class)
    private fun importLib0(text: String, down: Boolean = true) {
        val array = text.split(":")
        importLib0(array[0],array[1],array[2],down)
    }
    @Throws(LibraryManagerError.DependencyNotFoundException::class)
    private fun importLib0(group: String, module: String, version: String, down: Boolean = true) {
        val groupArray = group.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val constructSource = StringBuilder("/")
        for (s in groupArray) {
            constructSource.append(s).append("/")
        }
        constructSource.append(module).append("/")
            .append(version).append("/")
            .append(module).append("-")
            .append(version)
        val savePath = "$module-$version.jar"
        val groupLibData = ImportGroupData(group,module,version)
        if (tempGroup.contains(groupLibData)) {
            Log.clog(module)
            return
        }
        tempGroup.add(groupLibData)
        getDepend(ImportData(constructSource.toString(), savePath),down)
    }

    @Throws(LibraryManagerError.DependencyNotFoundException::class)
    private fun getDepend(importData: ImportData, down: Boolean) {
        source.eachAll {
            val result = HttpRequestOkHttp.doGet(importData.getDownPom(it))
            if (result.isNotBlank() && !result.trim().equals("404 Not Found",true)) {
                importData.mainSource = it
                try {
                    val mavenreader = MavenXpp3Reader()
                    val model = mavenreader.read(DisableSyncByteArrayInputStream(result.toByteArray()))
                    val project = MavenProject(model).model
                    for (lib in project.dependencies) {
                        when (lib.scope) {
                            "test","provided" -> {}
                            "compile","runtime" -> importLib0(lib.groupId,lib.artifactId,lib.version, down)
                            else -> {}
                        }
                    }
                } catch (ex: Exception) {
                }
                if (down) {
                    dependenciesDown.add(importData)
                }
                return@eachAll
            }
        }
    }

    private class ImportGroupData(val group: String, val module: String, val version: String) {
        override fun equals(other: Any?): Boolean {
            if (other is ImportGroupData) {
                return other.group == group && other.module == module
            }
            return false
        }

        override fun hashCode(): Int {
            var result = group.hashCode()
            result = 31 * result + module.hashCode()
            return result
        }
    }

    private class ImportData(private val constructSource: String, val fileName: String) {
        var mainSource: UrlData = UrlData.Maven

        fun getDownUrl(): String {
            return if (constructSource.startsWith("/")) {
                "${mainSource.url}$constructSource.jar"
            } else {
                "${mainSource.url}/$constructSource.jar"
            }
        }

        fun getDownPom(mainSource: UrlData): String {
            return if (constructSource.startsWith("/")) {
                "${mainSource.url}$constructSource.pom"
            } else {
                "${mainSource.url}/$constructSource.pom"
            }
        }
    }

    public enum class UrlData(val url: String) {
        Maven("https://repo1.maven.org/maven2"),
        MavenAli("https://maven.aliyun.com/repository/central"),
        JitPack("https://jitpack.io"),
    }

    companion object {
        private val loadEnd = Seq<File>()
        private val load = Seq<File>()

        private val dependenciesDown = Seq<ImportData>()
        private val dependenciesFile = Seq<File>()
        private val tempGroup = Seq<ImportGroupData>()
    }
}