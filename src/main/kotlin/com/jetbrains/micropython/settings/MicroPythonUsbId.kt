package com.jetbrains.micropython.settings

/**
 * @author vlan
 */
data class MicroPythonUsbId(val vendorId: Int, val productId: Int) {
    companion object {
        fun parse(vendorAndProductId: String): MicroPythonUsbId {
            val ints = vendorAndProductId.split(':').map { Integer.decode(it) }
            return MicroPythonUsbId(ints[0], ints[1])
        }
    }
}
