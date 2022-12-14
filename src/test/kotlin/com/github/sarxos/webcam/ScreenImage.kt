package com.github.sarxos.webcam

import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.SwingUtilities

/*
*  Convenience class to create and optionally save to a file a
*  BufferedImage of an area on the screen. Generally there are
*  four different scenarios. Create an image of:
*
*  a) an entire component
*  b) a region of the component
*  c) the entire desktop
*  d) a region of the desktop
*
*  The first two use the Swing paint() method to draw the
*  component image to the BufferedImage. The latter two use the
*  AWT Robot to create the BufferedImage.
*
*	The created image can then be saved to a file by usig the
*  writeImage(...) method. The type of file must be supported by the
*  ImageIO write method.
*
*  Although this class was originally designed to create an image of a
*  component on the screen it can be used to create an image of components
*  not displayed on a GUI. Behind the scenes the component will be given a
*  size and the component will be layed out. The default size will be the
*  preferred size of the component although you can invoke the setSize()
*  method on the component before invoking a createImage(...) method. The
*  default functionality should work in most cases. However the only
*  foolproof way to get a image to is make sure the component has been
*  added to a realized window with code something like the following:
*
*  JFrame frame = new JFrame();
*  frame.setContentPane( someComponent );
*  frame.pack();
*  ScreenImage.createImage( someComponent );
*
*/
object ScreenImage {
    private val types = listOf(*ImageIO.getWriterFileSuffixes())

    /*
	 * Create a BufferedImage for Swing components. The entire component will be captured to an
	 * image.
	 * @param component Swing component to create image from
	 * @return image the image for the given region
	 */
    fun createImage(component: JComponent): BufferedImage {
        var d = component.size
        if (d.width == 0 || d.height == 0) {
            d = component.preferredSize
            component.size = d
        }
        val region = Rectangle(0, 0, d.width, d.height)
        return createImage(component, region)
    }

    /*
	 * Create a BufferedImage for Swing components. All or part of the component can be captured to
	 * an image.
	 * @param component Swing component to create image from
	 * @param region The region of the component to be captured to an image
	 * @return image the image for the given region
	 */
    fun createImage(component: JComponent, region: Rectangle): BufferedImage {
        // Make sure the component has a size and has been layed out.
        // (necessary check for components not added to a realized frame)
        if (!component.isDisplayable) {
            var d = component.size
            if (d.width == 0 || d.height == 0) {
                d = component.preferredSize
                component.size = d
            }
            layoutComponent(component)
        }
        val image = BufferedImage(region.width, region.height, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()

        // Paint a background for non-opaque components,
        // otherwise the background will be black
        if (!component.isOpaque) {
            g2d.color = component.background
            g2d.fillRect(region.x, region.y, region.width, region.height)
        }
        g2d.translate(-region.x, -region.y)
        component.print(g2d)
        g2d.dispose()
        return image
    }

    /**
     * Convenience method to create a BufferedImage of the desktop
     *
     * @param fileName name of file to be created or null
     * @return image the image for the given region
     * @exception AWTException see Robot class constructors
     * @exception IOException if an error occurs during writing
     */
    @Throws(AWTException::class, IOException::class)
    fun createDesktopImage(): BufferedImage {
        val d = Toolkit.getDefaultToolkit().screenSize
        val region = Rectangle(0, 0, d.width, d.height)
        return createImage(region)
    }

    /*
	 * Create a BufferedImage for AWT components. This will include Swing components JFrame, JDialog
	 * and JWindow which all extend from Component, not JComponent.
	 * @param component AWT component to create image from
	 * @return image the image for the given region
	 * @exception AWTException see Robot class constructors
	 */
    @Throws(AWTException::class)
    fun createImage(component: Component): BufferedImage {
        val p = Point(0, 0)
        SwingUtilities.convertPointToScreen(p, component)
        val region = component.bounds
        region.x = p.x
        region.y = p.y
        return createImage(region)
    }

    /**
     * Create a BufferedImage from a rectangular region on the screen.
     *
     * @param region region on the screen to create image from
     * @return image the image for the given region
     * @exception AWTException see Robot class constructors
     */
    @Throws(AWTException::class)
    fun createImage(region: Rectangle?): BufferedImage {
        return Robot().createScreenCapture(region)
    }

    /**
     * Write a BufferedImage to a File.
     *
     * @param image image to be written
     * @param fileName name of file to be created
     * @exception IOException if an error occurs during writing
     */
    @Throws(IOException::class)
    fun writeImage(image: BufferedImage?, fileName: String?) {
        if (fileName == null) return
        val offset = fileName.lastIndexOf(".")
        if (offset == -1) {
            val message = "file suffix was not specified"
            throw IOException(message)
        }
        val type = fileName.substring(offset + 1)
        if (types.contains(type)) {
            ImageIO.write(image, type, File(fileName))
        } else {
            val message = "unknown writer file suffix ($type)"
            throw IOException(message)
        }
    }

    fun layoutComponent(component: Component) {
        synchronized(component.treeLock) {
            component.doLayout()
            if (component is Container) {
                for (child in component.components) {
                    layoutComponent(child)
                }
            }
        }
    }
}