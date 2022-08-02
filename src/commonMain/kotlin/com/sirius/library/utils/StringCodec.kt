package com.sirius.library.utils



expect class StringCodec() {

    fun fromByteArrayToASCIIString(byteArray: ByteArray) : String

    fun fromASCIIStringToByteArray(string: String?) : ByteArray

    fun escapeStringLikePython(string: String): String
}