package com.sirius.library.utils



expect class StringCodec() {

    fun fromByteArrayToASCIIString(byteArray: ByteArray) : String

    fun fromASCIIStringToByteArray(string: String?) : ByteArray

    fun fromUTF8StringToByteArray(string: String?) : ByteArray

    fun fromByteArrayToUTF8String(byteArray: ByteArray) : String

    fun escapeStringLikePython(string: String): String
}