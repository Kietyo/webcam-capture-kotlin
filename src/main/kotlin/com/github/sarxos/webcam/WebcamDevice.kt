package com.github.sarxos.webcam

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

/**
 * Webcam device abstraction.
 *
 * @author Bartosz Firyn (SarXos)
 */
interface WebcamDevice {
    /**
     * This interface should be implemented by all webcam devices supporting
     * possibility to access raw bytes or direct bytes buffer from native webcam
     * device.
     *
     * @author Bartosz Firyn (SarXos)
     */
    interface BufferAccess {
        /**
         * Read the underlying image memory buffer. This method will return new
         * reference to pre-allocated off-heap memory where image bytes are
         * stored. The size of this buffer is image width * height * 3 bytes.<br></br>
         * <br></br>
         *
         * **NOTE!** Do **not** use this buffer to set bytes value. It
         * should be used only for read purpose!
         *
         * @return Bytes buffer
         */
        val imageByteBuffer: ByteBuffer?

        /**
         * Copy the underlying image memory into the target buffer passed as the
         * argument.The remaining capacity of the target buffer needs to be at
         * least image width * height * 3 bytes.
         *
         * @param target the buffer to which image data should be copied
         */
        fun getImageBytes(target: ByteBuffer)
    }

    interface FPSSource {
        /**
         * Get current device FPS.
         *
         * @return FPS
         */
        val fps: Double
    }

    /**
     * This interface may be implemented by devices which expect any specific
     * parameters.
     *
     * @author Martin Krok (krok32)
     */
    interface Configurable {
        /**
         * Sets device parameters. Each device implementation may accept its own
         * set of parameters. All accepted keys, value types, possible values
         * and defaults should be reasonably documented by the implementor. May
         * be called before the open method or later in dependence of the device
         * implementation.
         *
         * @param parameters - Map of parameters changing device defaults
         * @see Webcam.setParameters
         */
        fun setParameters(parameters: Map<String?, *>?)
    }

    val preallocatedImageBytes: ByteArray

    /**
     * Get device name.
     *
     * @return Device name
     */
    val name: String

    /**
     * Get the list of all possible image resolutions.
     *
     * @return Possible resolutions
     */
    val resolutions: Array<Dimension>
    /**
     * Get currently set image size.
     *
     * @return The size which is currently set
     */
    fun getResolution(): Dimension

    /**
     * Set new expected image size.
     *
     * @param size the size to be set
     */
    fun setResolution(size: Dimension)

    /**
     * Fetch image from underlying camera.
     *
     * @return Image
     */
    val image: BufferedImage?

    /**
     * Open device, it can be closed any time.
     */
    fun open()

    /**
     * Close device, however it can be open again.
     */
    fun close()

    /**
     * Dispose device. After device is disposed it cannot be open again.
     */
    fun dispose()

    /**
     * Is webcam device open?
     *
     * @return True if webcam device is open, false otherwise
     */
    val isOpen: Boolean
}