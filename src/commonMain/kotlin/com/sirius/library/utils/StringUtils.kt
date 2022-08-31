package com.sirius.library.utils

object StringUtils {

    enum class CODEC{
        US_ASCII,
        UTF_8
    }
 //   const val US_ASCII  : String = "US_ASCII"
    const val UTF_8  : String = "UTF_8"


    fun stringToBytes(string: String,encodeCharset: CODEC): ByteArray {
        val codec = StringCodec()
        if (encodeCharset == CODEC.US_ASCII) {
            return codec.fromASCIIStringToByteArray(string)
        } else if (encodeCharset == CODEC.UTF_8) {
            return codec.fromUTF8StringToByteArray(string)
        }
        return ByteArray(0)
    }

    fun bytesToString(bytes: ByteArray,encodeCharset: CODEC ): String {
        val codec = StringCodec()
        if (encodeCharset == CODEC.US_ASCII) {
            return codec.fromByteArrayToASCIIString(bytes)
        } else if (encodeCharset == CODEC.UTF_8) {
            return codec.fromByteArrayToUTF8String(bytes)
        }
        return ""
    }

    fun stringToBase58String(string: String): String {
        val bytes = stringToBytes(string, CODEC.UTF_8)
        return bytesToBase58String(bytes)
    }

    fun bytesToBase58String(bytes: ByteArray?): String {
        return Base58.encode(bytes!!)
    }




    public fun String.toCharArray(): CharArray = CharArray(length) { get(it) }


}
