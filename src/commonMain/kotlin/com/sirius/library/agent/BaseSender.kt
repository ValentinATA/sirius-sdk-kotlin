package com.sirius.library.agent

abstract class BaseSender {
    abstract suspend fun  sendTo(endpoint: String?, data: ByteArray?): Boolean
    abstract fun open(endpoint: String?)
    abstract fun close()
}
