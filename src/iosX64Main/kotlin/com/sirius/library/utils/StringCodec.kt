package com.sirius.library.utils

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

actual class StringCodec actual constructor() {
    actual fun fromByteArrayToASCIIString(byteArray: ByteArray): String {
        val d = memScoped{
            NSData.create(bytes = allocArrayOf(byteArray), length = byteArray.size.toULong())
        }
        val nsString = NSString.create(d, NSASCIIStringEncoding)
        return nsString as String
    }

    actual fun fromASCIIStringToByteArray(string: String?): ByteArray {
        val string = NSString.create(string = string ?:"")
        val data = string.dataUsingEncoding(NSASCIIStringEncoding)!!
        val d = memScoped{data}
        return  ByteArray(d.length.toInt()).apply {
            usePinned {
                memcpy(it.addressOf(0), d.bytes,d.length)
            }
        }
    }

    actual fun fromUTF8StringToByteArray(string: String?): ByteArray {
       // if (string==null) {
            return ByteArray(0)
      //  }
      //  return string.toByteArray(StandardCharsets.UTF_8)
    }

    actual fun fromByteArrayToUTF8String(byteArray: ByteArray): String {
      return ""
    }

    actual fun escapeStringLikePython(string: String): String {
       return ""
    }
}