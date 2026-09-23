package io.github.amedeoalf.betterchrono

import java.io.File

class SavesManager(private val saveDir: File) {
    fun save(vm: ChronoViewModel, name: String) =
        saveDir.resolve(name).writer().use { vm.export(it) }

    fun listSaves() = saveDir.listFiles()!!.sortedByDescending { it.lastModified() }

    fun loadSave(vm: ChronoViewModel, name: String) =
        saveDir.resolve(name).reader().use { vm.import(it) }
}