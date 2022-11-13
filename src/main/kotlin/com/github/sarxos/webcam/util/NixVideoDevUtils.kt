package com.github.sarxos.webcam.util

import java.io.File
import java.io.FilenameFilter

class NixVideoDevUtils : FilenameFilter {
    override fun accept(dir: File, name: String): Boolean {
        return dir.name == "dev" && name.startsWith("video") && name.length > 5 && Character.isDigit(name[5])
    }

    companion object {
        private val DEV = File("/dev")
        val videoFiles: Array<File?>
            get() {
                val names = DEV.list(NixVideoDevUtils())
                val files = arrayOfNulls<File>(names.size)
                for (i in names.indices) {
                    files[i] = File(DEV, names[i])
                }
                return files
            }
    }
}