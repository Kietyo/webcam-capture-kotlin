package com.github.sarxos.webcam

import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*

object WebcamDriverUtils {
    private val LOG = LoggerFactory.getLogger(WebcamDriverUtils::class.java)

    /**
     * Find webcam driver. Scan packages to search drivers specified in the
     * argument.
     *
     * @param names array of driver names to search for
     * @return Driver if found or throw exception
     * @throw WebcamException
     */
    internal fun findDriver(names: List<String>, classes: List<Class<*>>): WebcamDriver? {
        for (name in names) {
            LOG.info("Searching driver {}", name)
            var clazz: Class<*>? = null
            for (c in classes) {
                if (c.canonicalName == name) {
                    clazz = c
                    break
                }
            }
            if (clazz == null) {
                try {
                    clazz = Class.forName(name)
                } catch (e: ClassNotFoundException) {
                    LOG.trace("Class not found {}, fall thru", name)
                }
            }
            if (clazz == null) {
                LOG.debug("Driver {} not found", name)
                continue
            }
            LOG.info("Webcam driver {} has been found", name)
            return try {
                clazz.newInstance() as WebcamDriver
            } catch (e: InstantiationException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }
        return null
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     *
     * @param packageName The base package
     * @param flat scan only one package level, do not dive into subdirectories
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    internal fun getClasses(pkgname: String, flat: Boolean): Array<Class<*>> {
        val dirs: MutableList<File> = ArrayList()
        val classes: MutableList<Class<*>> = ArrayList()
        val classLoader = Thread.currentThread().contextClassLoader
        val path = pkgname.replace('.', '/')
        val resources: Enumeration<URL> = try {
            classLoader.getResources(path)
        } catch (e: IOException) {
            throw RuntimeException("Cannot read path $path", e)
        }
        while (resources.hasMoreElements()) {
            val resource = resources.nextElement()
            dirs.add(File(resource.file))
        }
        for (directory in dirs) {
            try {
                classes.addAll(findClasses(directory, pkgname, flat))
            } catch (e: ClassNotFoundException) {
                throw RuntimeException("Class not found", e)
            }
        }
        return classes.toTypedArray()
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * subdirectories.
     *
     * @param dir base directory
     * @param pkgname package name for classes found inside the base directory
     * @param flat scan only one package level, do not dive into subdirectories
     * @return Classes list
     * @throws ClassNotFoundException
     */
    @Throws(ClassNotFoundException::class)
    private fun findClasses(dir: File, pkgname: String, flat: Boolean): List<Class<*>> {
        val classes: MutableList<Class<*>> = ArrayList()
        if (!dir.exists()) {
            return classes
        }
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory && !flat) {
                classes.addAll(findClasses(file, pkgname + "." + file.name, flat))
            } else if (file.name.endsWith(".class")) {
                classes.add(Class.forName(pkgname + '.' + file.name.substring(0, file.name.length - 6)))
            }
        }
        return classes
    }
}