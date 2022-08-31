package com.sirius.library.utils


import com.sirius.library.utils.StringUtils.toCharArray
import org.apache.commons.lang3.CharUtils
import java.nio.charset.StandardCharsets

actual class StringCodec actual constructor() {

    actual fun fromByteArrayToASCIIString(byteArray: ByteArray): String {
        return String(byteArray, StandardCharsets.US_ASCII)
    }

    actual fun fromASCIIStringToByteArray(string: String?): ByteArray {
        if (string==null) {
            return ByteArray(0)
        }
        return string.toByteArray(StandardCharsets.US_ASCII)
    }


    actual fun escapeStringLikePython(string: String): String {
        val chars = string.toCharArray()
        val escapedString: StringBuilder = StringBuilder()
        //TODO CharUtils.isAscii(charOne)
        for (charOne in chars) {
            if (CharUtils.isAscii(charOne)) {
                escapedString.append(charOne)
            } else {
                val escapedStr: String = CharUtils.unicodeEscaped(charOne)
                escapedString.append(escapedStr)
            }
        }
        return escapedString.toString()
    }



    actual fun fromUTF8StringToByteArray(string: String?): ByteArray {
        if (string==null) {
            return ByteArray(0)
        }
        return string.toByteArray(StandardCharsets.UTF_8)
    }

    actual fun fromByteArrayToUTF8String(byteArray: ByteArray): String {
        return String(byteArray, StandardCharsets.UTF_8)
    }


}