package com.sirius.library

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.box.crypto_box_NONCEBYTES
import com.sirius.library.encryption.Custom
import com.sirius.library.encryption.Ed25519
import com.sirius.library.encryption.UnpackModel
import com.sirius.library.errors.sirius_exceptions.SiriusCryptoError
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidType
import com.sirius.library.messaging.Message
import com.sirius.library.messaging.MessageFabric
import com.sirius.library.naclJava.CryptoAead
import com.sirius.library.rpc.AddressedTunnel
import com.sirius.library.rpc.Future
import com.sirius.library.rpc.Parsing
import com.sirius.library.utils.*
import com.sodium.LibSodium
import io.ktor.util.*
import kotlin.test.*

class TestEncryption {
    var seed1 = "000000000000000000000000000SEED1"
    var seed2 = "000000000000000000000000000SEED2"
    val testMessage: String
        get() {
            val string = "Test encryption строка"
            val enc_message: JSONObject = JSONObject()
            enc_message.put("content", string)
            val message: String = enc_message.toString()
            return StringCodec().escapeStringLikePython(message)
        }

    val testMessage2 : String
    get(){
        val  expirationTime : Long = 1635520202
        val  msgType = "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/sirius_rpc/1.0/ping_agent"

        val future = Future.FuturePromise("12","redis://redis/23423423423-909", expirationTime)
        val request: Message = Parsing.buildRequest(msgType, future, null)
        request.setId("a9ab256b-dd19-47fe-973a-55501443101e")
        return request.serialize()
    }
    @BeforeTest
    fun initSdk() {
        MessageFabric.registerAllMessagesClass()
        val future = CompletableFutureKotlin<Boolean>()
        LibsodiumInitializer.initializeWithCallback {
            future.complete(true)
        }
        future.get(60)
    }


    @Test
    fun encrypt() {
        // CREATE KEYPAIR

            val codec = StringCodec()

            val keyPairRecipient: KeyPair =
                Custom.createKeypair(codec.fromASCIIStringToByteArray(seed1))
            val verkeyRecipient: String = Custom.bytesToB58(keyPairRecipient.getPublicKey().asBytes)
            val sigkeyRecipient: String = Custom.bytesToB58(keyPairRecipient.getSecretKey().asBytes)
            val keyPairSender: KeyPair = Custom.createKeypair(codec.fromASCIIStringToByteArray(seed2))
            val verkeySender: String = Custom.bytesToB58(keyPairSender.getPublicKey().asBytes)
            val sigkeySender: String = Custom.bytesToB58(keyPairSender.getSecretKey().asBytes)

            //CREATE TESTmessage
            val message = testMessage
            val ed25519 = Ed25519()
//val message = "{\"@type\":\"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/sirius_rpc/1.0/create_key\",\"@id\":\"4214da00-d0a4-4c59-8753-a1bae05753c0\",\"@promise\":{\"id\":\"445b5b70-43a7-4663-a452-5988f5fe50f4\",\"channel_address\":\"redis://redis/de28da3f418f491d93276bcace293c26\",\"expiration_stamp\":1634821996},\"params\":{\"seed\":{\"mime_type\":null,\"payload\":null},\"crypto_type\":{\"mime_type\":null,\"payload\":null}}}"

            //PACK MESSAGE
            val verkeys: MutableList<String> = ArrayList<String>()
            verkeys.add(verkeyRecipient)
            val packedString: String = ed25519.packMessage(message, verkeys, verkeySender, sigkeySender)
            println("packedString="+packedString)
            //UNPACK MESSAGE
            val unpackedModel: UnpackModel = ed25519.unpackMessage(packedString, verkeyRecipient, sigkeyRecipient)

            println("unpackedModelmessage="+unpackedModel.message)
            println("unpackedModel.recip_vk="+unpackedModel.recip_vk)
            println("unpackedModel.sender_vk="+unpackedModel.sender_vk)

            println("message="+message)
            println("verkeyRecipient="+verkeyRecipient)
            println("verkeySender="+verkeySender)

            //ASSERTING
            assertEquals(unpackedModel.sender_vk, verkeySender)
            assertEquals(unpackedModel.recip_vk, verkeyRecipient)
            assertEquals(message, unpackedModel.message)

    }

    //read={"protected":"eyJlbmMiOiJ4Y2hhY2hhMjBwb2x5MTMwNV9pZXRmIiwidHlwIjoiSldNLzEuMCIsImFsZyI6IkF1dGhjcnlwdCIsInJlY2lwaWVudHMiOlt7ImVuY3J5cHRlZF9rZXkiOiJYT2x2emVXcm5EdGhWUEEyN2wwZVp3NFBBOUxJT0x0WUwwRjdEcTBwNUR3c1N6RjA1bVU0a0hITnJPdWtweE9LIiwiaGVhZGVyIjp7ImtpZCI6IjZRdlEzWTVwUE1HTmd6dnM4Nk4zQVFvOThwRjVXcnpNMWg2V2tLSDNkTDdmIiwiaXYiOiJVbEo4NlRxM0E2aVlDbi1NNnZfeUJHWkxZbDZjMzFvTiIsInNlbmRlciI6IjM1b1h4azI5dDVzVTVvSF9BRnhNUmx0RTZqMWNKUlRhaV9pRmx3RDNXaGM4YlhHMkdrbGxhMjQ1aE1waVdFVFJnYWdxNjYyeGtnRXFvcEFSd2UzanZ6bzV0VkhrWTlhN2pvM095RE1GOEtNcU9fMEhjMmk1bjJhZ05qTT0ifX1dfQ==","iv":"YINZWei8GWFxaw0o","ciphertext":"NMXqUXeGzxDxcFhTDW2QU0G3DPShSydosdMtFhRUVDw7Fl9mzqP3m9AiDT5IlfywOhyRTkkS5KfO9lcAv3PU46q6kFQNnoYf9eddALmoGmPFESJKF5gwItEdzFtUCuozzWcCi1kY85sJO9D-JOzFa-SZCluBftBbMU6qcgM_2vWm1iPP2CBLK5nQAfcDQzj5L_tkB5CMcXTMv1Wbq0EPIDT2_kFXi9Dn7L8eLWAWCmjMbdU80qXzX6KsntXSJ_ibKlEiGrvR3_clyg21L2xwzWlS9GBgv1P28dyC1Ofcd2xPpQclvg7e5nSB5scoKgsDrCdHX7_DllXsMA7uymwj7mIV9rifw4zwkldH_LApajYXbEpod-uEeN0KFu5TyhmwKKCfALtBZ6CctrqOLYm6D-rJCKzP7gUjfWKxwsNiXrhIy38LCQrO25nJ7Z8NPSbIaktpRiMJbz4oaJrmdvcjXVR1d-e8uDzRkvwvyEBRVFJKpuBKP9E-4HHLXh_F-6A=","tag":"8tOGZg-i1AO9RO0Eh2mh5Q=="}
    @Test
    fun test_fixture() {

            var keyPairRecipient: KeyPair? = null
            //CREATE KEYPAIR
            val codec = StringCodec()
            keyPairRecipient = Custom.createKeypair(codec.fromASCIIStringToByteArray(seed1))
            val verkey_recipient: String = Custom.bytesToB58(keyPairRecipient.getPublicKey().asBytes)
            val sigkey_recipient: String = Custom.bytesToB58(keyPairRecipient.getSecretKey().asBytes)
            val keyPairSender: KeyPair = Custom.createKeypair(codec.fromASCIIStringToByteArray(seed2))
            val verkeySender: String = Custom.bytesToB58(keyPairSender.getPublicKey().asBytes)
            val sigkeySender: String = Custom.bytesToB58(keyPairSender.getSecretKey().asBytes)
            val packed =
                "{\"protected\": \"eyJlbmMiOiAieGNoYWNoYTIwcG9seTEzMDVfaWV0ZiIsICJ0eXAiOiAiSldNLzEuMCIsICJhbGciOiAiQXV0aGNyeXB0IiwgInJlY2lwaWVudHMiOiBbeyJlbmNyeXB0ZWRfa2V5IjogInBKcW1xQS1IVWR6WTNWcFFTb2dySGx4WTgyRnc3Tl84YTFCSmtHU2VMT014VUlwT0RQWTZsMVVsaVVvOXFwS0giLCAiaGVhZGVyIjogeyJraWQiOiAiM1ZxZ2ZUcDZRNFZlRjhLWTdlVHVXRFZBWmFmRDJrVmNpb0R2NzZLR0xtZ0QiLCAic2VuZGVyIjogIjRlYzhBeFRHcWtxamd5NHlVdDF2a0poeWlYZlNUUHo1bTRKQjk1cGZSMG1JVW9KajAwWmswNmUyUEVDdUxJYmRDck8xeTM5LUhGTG5NdW5YQVJZWk5rZ2pyYV8wYTBQODJpbVdNcWNHc1FqaFd0QUhOcUw1OGNkUUYwYz0iLCAiaXYiOiAiVU1PM2o1ZHZwQnFMb2Rvd3V0c244WEMzTkVqSWJLb2oifX1dfQ==\", \"iv\": \"MchkHF2M-4hneeUJ\", \"ciphertext\": \"UgcdsV-0rIkP25eJuRSROOuqiTEXp4NToKjPMmqqtJs-Ih1b5t3EEbrrHxeSfPsHtlO6J4OqA1jc5uuD3aNssUyLug==\", \"tag\": \"sQD8qgJoTrRoyQKPeCSBlQ==\"}"
            val ed25519 = Ed25519()
            //UNPACK MESSAGE
            val unpackedModel: UnpackModel = ed25519.unpackMessage(packed, verkey_recipient, sigkey_recipient)
            //В pytone при json.dumps добавляется пробел между ключом значением.
            val testMessage = "{\"content\": \"Test encryption \\u0441\\u0442\\u0440\\u043e\\u043a\\u0430\"}"
            //ASSERTING
            assertEquals(unpackedModel.sender_vk, verkeySender)
            assertEquals(unpackedModel.recip_vk, verkey_recipient)
            assertEquals(unpackedModel.message, testMessage)

    }

    @Test
    fun test_CryptoSign() {


        val codec = StringCodec()
        val kp: KeyPair =
            Custom.createKeypair("0000000000000000000000000000SEED".encodeToByteArray())
        val msg = "message"
        var signature: ByteArray =
            Custom.signMessage(msg.encodeToByteArray(), kp.getSecretKey().asBytes)
        assertEquals(
            "3tfqJYZ8ME8gTFUSHcH4uVTUx5kV7S1qPJJ65k2VtSocMfXvnzR1sbbfq6F2RcXrFtaufjEr4KQVu7aeyirYrcRm",
            Custom.bytesToB58(signature)
        )
          val messagebyteArray =   Custom.verifySignedMessage(
                kp.getPublicKey().asBytes,
                msg.encodeToByteArray(), signature
            )
           val string =  messagebyteArray.decodeToString()
        assertEquals(msg, string)
        val kp2: KeyPair =
            Custom.createKeypair("000000000000000000000000000SEED2".encodeToByteArray())
        assertNotEquals(kp2.getPublicKey().asBytes, kp.getPublicKey().asBytes)
        signature = Custom.signMessage(
            msg.encodeToByteArray(),
            kp2.getSecretKey().asBytes
        )
            val messagebyteArray2 =    Custom.verifySignedMessage(
                kp.getPublicKey().asBytes,
                msg.encodeToByteArray(), signature
            )
            val string2 =  messagebyteArray2.decodeToString()
        assertNotEquals(msg,string2)

    }

    @Test
    fun test_didFromVerkey() {


            val kp: KeyPair =
                Custom.createKeypair("0000000000000000000000000000SEED".encodeToByteArray())

            println("kp.getPublicKey().asBytes)"+kp.getPublicKey().asBytes)
            println("Custom.bytesToB58(kp.getPublicKey().asBytes)"+Custom.bytesToB58(kp.getPublicKey().asBytes))
            assertEquals(
                "GXhjv2jGf2oT1sqMyvJtgJxNYPMHmTsdZ3c2ZYQLJExj",
                Custom.bytesToB58(kp.getPublicKey().asBytes)
            )
            val did: ByteArray? = Custom.didFromVerkey(kp.getPublicKey().asBytes)
            assertEquals("VVZbGvuFqBdoVNY1Jh4j9Q", Custom.bytesToB58(did ?: ByteArray(0)))



    }


    @Test
    fun codecTest() {
        val text = "Message"
        val textBytes = StringUtils.stringToBytes(text, StringUtils.CODEC.US_ASCII)
        val textBytesToString = StringUtils.bytesToString(textBytes, StringUtils.CODEC.US_ASCII)
        //val text1 = Custom.bytesToB64(text.encodeToByteArray(), true) ?: ""\
        println("textBytes="+textBytes)
        println("textBytesToString="+textBytesToString)
        assertEquals(text,textBytesToString)
        assertNotEquals(textBytes.size, 0)
      //  assertEquals(text, text2)
    }


    @Test
    fun base64TestUrl() {
        val text = "Message"
        val text1 = Custom.bytesToB64(text.encodeToByteArray(), true) ?: ""
        val bytes = Custom.b64ToBytes(text1, true)
        val text2 = bytes.decodeToString()
        println("text2="+text2)
        assertEquals(text, text2)
    }

    @Test
    fun base64Test() {
        val text = "Message"
        val text1 = Custom.bytesToB64(text.encodeToByteArray(), false) ?: ""
        val bytes = Custom.b64ToBytes(text1, false)
        val text2 = bytes.decodeToString()
        println("text2="+text2)
        assertEquals(text, text2)
    }


    @Test
    fun base58Test() {
        val text = "Message"
        val text1 = Custom.bytesToB58(text.encodeToByteArray())
        val bytes = Custom.b58ToBytes(text1)
        val text2 = bytes.decodeToString()
        assertEquals(text, text2)
    }

/*    @Test
    fun cryptoBoxSeal() {
        LibsodiumInitializer.initializeWithCallback {


            val custom = Custom
            //INIT
            val keyPairRecipient: KeyPair =
                Custom.createKeypair(StringUtils.stringToBytes(seed1, StringUtils.US_ASCII))
            val verkeyRecipient : String = custom . bytesToB58 (keyPairRecipient.getPublicKey().asBytes)
            val sigkeyRecipient: String = custom.bytesToB58(keyPairRecipient.getSecretKey().asBytes)
            val keyPairSender: KeyPair = custom.createKeypair(StringUtils.stringToBytes(seed2, StringUtils.US_ASCII))
            val verkeySender: String = custom.bytesToB58(keyPairSender.getPublicKey().asBytes)
            val sigkeySender: String = custom.bytesToB58(keyPairSender.getSecretKey().asBytes)

            //SEAL


            val from_verkey = Ed25519().ensureIsBytes(verkeySender)
            val from_sigkey = Ed25519().ensureIsBytes(sigkeySender)
            val to_verkeys = Ed25519().ensureIsBytes(verkeyRecipient)

            val keyPairToConvert = KeyPair(Key.fromBytes(to_verkeys), Key.fromBytes(from_sigkey))
            val convertedKeyPair: KeyPair =
                LibSodium.getInstance().convertKeyPairEd25519ToCurve25519(keyPairToConvert)
            val target_pk: Key = convertedKeyPair.getPublicKey()

            val sender_vk = custom.bytesToB58(from_verkey)
            val enc_sender = CryptoAead().cryptoBoxSeal(sender_vk, target_pk) ?: ByteArray(0)


            val cek: Key = LibSodium.getInstance().cryptoSecretStreamKeygen()
            val  nonce =   LibSodium.getInstance().randomBytesBuf(crypto_box_NONCEBYTES)
            val enc_cek = CryptoAead().cryptoBox(cek.asBytes, nonce, convertedKeyPair)

            //OPEN SEAL


            val my_verkey = Ed25519().ensureIsBytes(verkeyRecipient)
            val my_sigkey = Ed25519().ensureIsBytes(sigkeyRecipient)
            val keyPairToConvert2 = KeyPair(Key.fromBytes(my_verkey), Key.fromBytes(my_sigkey))
            val convertedKeyPair2: KeyPair =
                LibSodium.getInstance().convertKeyPairEd25519ToCurve25519(keyPairToConvert2)

            val sender_vk1 = CryptoAead().cryptoBoxSealOpen(enc_sender, convertedKeyPair2)

            val sender_vk2 = custom.bytesToB58(sender_vk1)
            println("sender_vk="+sender_vk)
            println("sender_vk1="+sender_vk1)
            println("enc_sender="+enc_sender)
            println("sender_vk2="+sender_vk2)
            assertTrue(true)
            //assertTrue { sender_vk==sender_vk1}

            val senderBytes = custom.b58ToBytes(sender_vk)
            val senderKey: Key = Key.fromBytes(senderBytes)

            val senderKeyPair = KeyPair(senderKey, senderKey)
            val senderConvertedKeyPair: KeyPair =
                LibSodium.getInstance().convertKeyPairEd25519ToCurve25519(senderKeyPair)
            val sender_pk: Key = senderConvertedKeyPair.getPublicKey()
            val openKeyPair = KeyPair(sender_pk, convertedKeyPair.getSecretKey())
            val cek1 = CryptoAead().cryptoBoxOpen(enc_cek, nonce, openKeyPair)
        }
    }*/
}
