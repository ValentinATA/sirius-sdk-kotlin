package com.sirius.library.encryption

import com.ionspin.kotlin.crypto.aead.crypto_aead_chacha20poly1305_ietf_NPUBBYTES
import com.ionspin.kotlin.crypto.aead.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES
import com.ionspin.kotlin.crypto.box.crypto_box_NONCEBYTES
import com.sirius.library.errors.sirius_exceptions.SiriusCryptoError
import com.sirius.library.errors.sirius_exceptions.SiriusFieldValueError
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidType
import com.sirius.library.naclJava.CryptoAead
import com.sirius.library.utils.*

import com.sodium.LibSodium

class Ed25519 {
    var custom: Custom = Custom
    fun ensureIsBytes(b58_or_bytes: String?): ByteArray {
        return custom.b58ToBytes(b58_or_bytes!!)
    }

    /**
     * Assemble the recipients block of a packed message.
     *
     * @param to_verkeys:  Verkeys of recipients
     * @param from_verkey: Sender Verkey needed to authcrypt package
     * @param from_sigkey: Sender Sigkey needed to authcrypt package
     * @return A tuple of (json result, key)
     */

    fun prepare_pack_recipient_keys(
        to_verkeys: List<ByteArray>,
        from_verkey: ByteArray?,
        from_sigkey: ByteArray
    ): Pair<String, Key> {
        if (from_verkey != null && from_sigkey == null || from_verkey == null && from_sigkey != null) {
            throw SiriusCryptoError("Both verkey and sigkey needed to authenticated encrypt message")
        }

        val cek: Key = LibSodium.getInstance().cryptoSecretStreamKeygen()
        val recips = JSONArray()
        var enc_cek: ByteArray? = null
        var enc_sender: ByteArray? = null
        var nonce: ByteArray? = null
        for (target_vk in to_verkeys) {
            val keyPairToConvert = KeyPair(Key.fromBytes(target_vk), Key.fromBytes(from_sigkey))
            val convertedKeyPair: KeyPair =
                LibSodium.getInstance().convertKeyPairEd25519ToCurve25519(keyPairToConvert)
            val target_pk: Key = convertedKeyPair.getPublicKey()
            if (from_verkey != null) {
                val sender_vk = custom.bytesToB58(from_verkey)
                enc_sender = CryptoAead().cryptoBoxSeal(sender_vk, target_pk)
                nonce =   LibSodium.getInstance().randomBytesBuf(crypto_box_NONCEBYTES)
                enc_cek = CryptoAead().cryptoBox(cek.asBytes, nonce, convertedKeyPair)
            } else {
                enc_sender = null
                nonce = null
                enc_cek = CryptoAead().cryptoBoxSeal(cek.asBytes, target_pk)
            }
            val jsonObject = JSONObject()
            jsonObject.put("encrypted_key", custom.bytesToB64(enc_cek, true))
            val headerObject = JSONObject()
            headerObject.put("kid", custom.bytesToB58(target_vk))
            if (enc_sender == null) {
                headerObject.put("sender", enc_sender)
            } else {
                headerObject.put("sender", custom.bytesToB64(enc_sender, true))
            }
            println("put sender="+custom.bytesToB64(enc_sender, true))
            if (nonce == null) {
                headerObject.put("iv", nonce)
            } else {
                headerObject.put("iv", custom.bytesToB64(nonce, true))
            }
            jsonObject.put("header", headerObject)
            recips.put(jsonObject)
        }
        val data = JSONObject()
        data.put("enc", "xchacha20poly1305_ietf")
        data.put("typ", "JWM/1.0")
        if (from_verkey != null) {
            data.put("alg", "Authcrypt")
        } else {
            data.put("alg", "Anoncrypt")
        }
        data.put("recipients", recips)
        return Pair(data.toString(), cek)
    }

    /**
     * Locate pack recipient key.
     * Decode the encryption key and sender verification key from a
     * corresponding recipient block, if any is defined.
     *
     * @param recipients Recipients to locate
     * @return bytes, str, str A tuple of (cek, sender_vk, recip_vk_b58)
     * @throws SiriusFieldValueError: If no corresponding recipient key found
     */

    fun locate_pack_recipient_key(recipients: List<JSONObject?>, keyPair: KeyPair): DecryptModel {
        val not_found: MutableList<String> = ArrayList<String>()
        for (recip in recipients) {
            if (recip == null || !recip.has("header") || !recip.has("encrypted_key")) {
                throw SiriusFieldValueError("Invalid recipient header")
            }
            //  JSONObject recipObj = new JSONObject(recip);
            val headerObj: JSONObject = recip.getJSONObject("header") ?: JSONObject()
            val recip_vk_b58: String = headerObj.getString("kid") ?: ""
            if (!custom.bytesToB58(keyPair.getPublicKey().asBytes).equals(recip_vk_b58)) {
                not_found.add(recip_vk_b58)
                continue
            }
            val convertedKeyPair: KeyPair =
                LibSodium.getInstance().convertKeyPairEd25519ToCurve25519(keyPair)
            val encrypted_key = custom.b64ToBytes(recip.getString("encrypted_key") ?:"", true)
            val iv: String = headerObj.optString("iv") ?:""
            val sender: String = headerObj.optString("sender") ?:""
            println("open sender="+sender)
            var nonce: ByteArray? = null
            var enc_sender: ByteArray? = null
            if (iv != null && sender != null) {
                nonce = custom.b64ToBytes(iv, true)
                enc_sender = custom.b64ToBytes(sender, true)
            } else {
                nonce = null
                enc_sender = null
            }
            var sender_vk: ByteArray? = null
            var cek: ByteArray? = null
            if (nonce != null && enc_sender != null) {
                sender_vk = CryptoAead().cryptoBoxSealOpen(enc_sender, convertedKeyPair) ?: ByteArray(0)
                val senderBytes = custom.b58ToBytes(StringUtils.bytesToString(sender_vk, StringUtils.CODEC.US_ASCII))
                val senderKey: Key = Key.fromBytes(senderBytes)
                val senderKeyPair = KeyPair(senderKey, senderKey)
                val senderConvertedKeyPair: KeyPair =
                    LibSodium.getInstance().convertKeyPairEd25519ToCurve25519(senderKeyPair)
                val sender_pk: Key = senderConvertedKeyPair.getPublicKey()
                val openKeyPair = KeyPair(sender_pk, convertedKeyPair.getSecretKey())
                cek = CryptoAead().cryptoBoxOpen(encrypted_key, nonce, openKeyPair)
            } else {
                sender_vk = null
                cek = CryptoAead().cryptoBoxSealOpen(encrypted_key, convertedKeyPair)
            }
            return DecryptModel(cek, sender_vk!!, recip_vk_b58)
        }
        throw SiriusFieldValueError("No corresponding recipient key found in $not_found")
    }

    /**
     * Encrypt the payload of a packed message.
     *
     * @param message  Message to encrypt
     * @param add_data additional data
     * @param key      Key used for encryption
     * @return A tuple of (ciphertext, nonce, tag)
     */
    fun encryptPlaintext(
        message: String, add_data: String?, key: Key?
    ): EncryptModel {
        println("encryptPlaintext message="+message)
        val nonce: ByteArray =
            LibSodium.getInstance().randomBytesBuf(crypto_aead_chacha20poly1305_ietf_NPUBBYTES)
        val bytesOutput = CryptoAead().encrypt(message, add_data, nonce, key!!, "CHACHA20_POLY1305_IETF")
        //    String outputHex = LibSodium.getInstance().getLazyAaed().encrypt(message, add_data, nonce, key, AEAD.Method.CHACHA20_POLY1305_IETF);
        //    byte[] outputBytes = LazySodium.toBin(outputHex);
        //   String output = new String(outputBytes,StandardCharsets.US_ASCII);
        println("encryptPlaintext message1="+StringUtils.bytesToString(bytesOutput ?: ByteArray(0),
            StringUtils.CODEC.UTF_8
        ))
        println("encryptPlaintext message2="+StringUtils.bytesToString(bytesOutput?: ByteArray(0), StringUtils.CODEC.US_ASCII))
        val mlen = message.length
      /*  val bObj: java.io.ByteArrayOutputStream = java.io.ByteArrayOutputStream()
        bObj.reset()
        var i = 0
        for (byteOut in bytesOutput!!) {
            i++
            bObj.write(byteOut.toInt())
            if (i == mlen) {
                break
            }
        }
       */

     /*   val bObj2: java.io.ByteArrayOutputStream = java.io.ByteArrayOutputStream()
        bObj2.reset()
        var z = 0
        for (byteOut in bytesOutput) {
            z++
            if (z <= mlen) {
                continue
            }
            bObj2.write(byteOut.toInt())
        }
*/
        bytesOutput?.let {
            val ciphertext: ByteArray =  bytesOutput.copyOfRange(0,mlen)
            val tag: ByteArray =  bytesOutput.copyOfRange(mlen,bytesOutput.size)
            return EncryptModel(ciphertext, nonce, tag)
        }
        return EncryptModel(ByteArray(0), nonce, ByteArray(0))
        //String tag = output.substring(mlen);

    }

    /**
     * Decrypt the payload of a packed message.
     *
     * @param ciphertext
     * @param recips_bin
     * @param nonce
     * @param key
     * @return The decrypted string
     */
    fun decryptPlaintext(ciphertext: ByteArray, recips_bin: ByteArray, nonce: ByteArray, key: ByteArray): String {
        val keys = Key.fromBytes(key!!)
        val output = CryptoAead().decrypt(ciphertext!!, recips_bin, nonce, keys, "CHACHA20_POLY1305_IETF")
        //String output = LibSodium.getInstance().getLazyAaed().decrypt(ciphertext, new String(recips_bin,StandardCharsets.US_ASCII), nonce, keys, AEAD.Method.CHACHA20_POLY1305_IETF);
        return StringUtils.bytesToString(output?:ByteArray(0), StringUtils.CODEC.US_ASCII)
    }


    /**
     * Assemble a packed message for a set of recipients, optionally including
     * the sender.
     *
     * @param message    The message to pack
     * @param toVerkeys  (Sequence of bytes or base58 string) The verkeys to pack the message for
     * @param fromVerkey (bytes or base58 string) The sender verkey
     * @param fromSigkey (bytes or base58 string) The sender sigkey
     * @return The encoded message
     */
    /* def pack_message(
            message: str,
            to_verkeys: Sequence[Union[bytes, str]],
            from_verkey: Union[bytes, str] = None,
            from_sigkey: Union[bytes, str] = None
    ) -> bytes:
    */
    //toVerkeys - LIST?

    fun packMessage(
        message: String,
        toVerkeys: List<String?>,
        fromVerkey: String?,
        fromSigkey: String?
    ): String {
        val toVerKeysBytes: MutableList<ByteArray> = ArrayList<ByteArray>()
        for (vk in toVerkeys) {
            val to_verkeys = ensureIsBytes(vk)
            toVerKeysBytes.add(to_verkeys)
        }

        val from_verkey = ensureIsBytes(fromVerkey)
        val from_sigkey = ensureIsBytes(fromSigkey)
        val (recips_json, second) = prepare_pack_recipient_keys(toVerKeysBytes, from_verkey, from_sigkey)
        val recips_b64 = custom.bytesToB64( StringUtils.stringToBytes(recips_json, StringUtils.CODEC.US_ASCII), true)
        val messageEscaped = StringCodec().escapeStringLikePython(message)
        val model = encryptPlaintext(messageEscaped, recips_b64, second)
        val nonce64 = custom.bytesToB64(model.nonce, true)
        val ciphertext64 = custom.bytesToB64(model.ciphertext, true)
        val tag64 = custom.bytesToB64(model.tag, true)
        val jsonObject = JSONObject()
        jsonObject.put("protected", recips_b64)
        jsonObject.put("iv", nonce64)
        jsonObject.put("ciphertext", ciphertext64)
        jsonObject.put("tag", tag64)
        return jsonObject.toString()
    }

    /**
     * Decode a packed message.
     * Disassemble and unencrypt a packed message, returning the message content,
     * verification key of the sender (if available), and verification key of the
     * recipient.
     *
     * @param encMessage: The encrypted message
     * @param myVerkey:   (bytes or base58 string) Verkey for decrypt
     * @param mySigkey:   (bytes or base58 string) Sigkey for decrypt
     * @return A tuple of (message, sender_vk, recip_vk)
     * @throws Exception (ValueError) If the packed message is invalid
     * @throws Exception * If the packed message reipients are invalid
     * @throws Exception If the pack algorithm is unsupported
     * @throws Exception If the sender's public key was not provided
     */

    fun unpackMessage(encMessage: String?, myVerkey: String?, mySigkey: String?): UnpackModel {
        val my_verkey = ensureIsBytes(myVerkey)
        val my_sigkey = ensureIsBytes(mySigkey)
        val keyPair = KeyPair(Key.fromBytes(my_verkey), Key.fromBytes(my_sigkey))
        var error = ""
        return try {
            error = "Expected dictionary"
            val encMessJson = JSONObject(encMessage)
            error = "Invalid packed message"
            val protected_bin: String = encMessJson.getString("protected") ?:""
            val recips_json = custom.b64ToBytes(protected_bin, true)
            error = "Invalid packed message recipients"
            val recips_outer = JSONObject(StringUtils.bytesToString(recips_json, StringUtils.CODEC.UTF_8))
            val alg: String = recips_outer.getString("alg") ?:""
            val is_authcrypt = alg == "Authcrypt"
            if (!is_authcrypt && alg != "Anoncrypt") {
                throw SiriusFieldValueError("Unsupported pack algorithm: $alg")
            }
            val recipentsArray: JSONArray = recips_outer.getJSONArray("recipients") ?: JSONArray()
            val recipents: MutableList<JSONObject> = ArrayList<JSONObject>()
            for (i in 0 until recipentsArray.length()) {
                val recip: JSONObject? = recipentsArray.getJSONObject(i)
                recip?.let {
                    recipents.add(recip)
                }
            }
            //  cek, sender_vk, recip_vk
            println("recipents="+recipents)
            val decryptModel = locate_pack_recipient_key(recipents, keyPair)
            if (decryptModel.sender_vk == null && is_authcrypt) {
                throw SiriusFieldValueError("Sender public key not provided for Authcrypt message")
            }
            val chiperText: String = encMessJson.getString("ciphertext")?:""
            val ivNonce: String = encMessJson.getString("iv") ?:""
            val tagTExt: String = encMessJson.getString("tag") ?:""
            val ciphertext = custom.b64ToBytes(chiperText, true)
            val nonce = custom.b64ToBytes(ivNonce, true)
            val tag = custom.b64ToBytes(tagTExt, true)
         //   val allByte = ciphertext + tag
            //val allByteArray = ByteArray(ciphertext.size + tag.size)
           // val buff: java.nio.ByteBuffer = java.nio.ByteBuffer.wrap(allByteArray)
           // buff.put(ciphertext)
           // buff.put(tag)
            val combined: ByteArray =ciphertext + tag
            val message = decryptPlaintext(
                combined, StringUtils.stringToBytes(protected_bin, StringUtils.CODEC.US_ASCII),
                nonce, decryptModel.cek
            )
            UnpackModel(
                message,
                StringUtils.bytesToString(decryptModel.sender_vk, StringUtils.CODEC.US_ASCII),
                decryptModel.recip_vk_b58
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw SiriusInvalidType(error)
        }
           return  UnpackModel(
               null,
               null,
               null
           );
    }
}
