package com.example.vpdmoney

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Toast
import com.example.vpdmoney.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var socketListener: SocketListener
    private lateinit var webSocket: WebSocket
    private val okHttpClient = OkHttpClient()

    private var encOutputMode: Int = 0
    private var decOutputMode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.let {
            encOutputMode = it.encryptionOutputMode.selectedItemPosition
            decOutputMode = it.decryptionOutputMode.selectedItemPosition

            it.encryptionOutputMode.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    encOutputMode = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }

            it.decryptionOutputMode.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                decOutputMode = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

            it.encryptBtn.setOnClickListener {
                startEncrypt()
            }

            it.decryptBtn.setOnClickListener {
                startDecrypt()
            }
        }

        socketListener = SocketListener.getInstance()
        socketListener.setListener {
            runOnUiThread {
                when (it) {
                    SocketListener.SocketEvent.Closed -> {
                        Toast.makeText(this, "Socket Closed", Toast.LENGTH_LONG).show()
                    }
                    SocketListener.SocketEvent.Closing -> {
                        Toast.makeText(this, "Socket Closing ...", Toast.LENGTH_LONG).show()
                    }
                    is SocketListener.SocketEvent.Failure -> {
                        Toast.makeText(this, it.t.localizedMessage, Toast.LENGTH_LONG).show()
                    }
                    is SocketListener.SocketEvent.Message -> {
                        Toast.makeText(this, it.text, Toast.LENGTH_LONG).show()
                    }
                    SocketListener.SocketEvent.Open -> {
                        Toast.makeText(this, "Socket Open", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        webSocket = okHttpClient.newWebSocket(createRequest(), socketListener)
    }

    private fun createRequest(): Request {
        return Request.Builder()
            .url("wss://socketsbay.com/wss/v2/1/demo/")
            .build()
    }

    private fun startDecrypt() {
        val textToDecrypt = viewBinding.textToDecrypt.text.toString().trim()
        val decryptionKey = viewBinding.decryptionKey.text.toString().trim()
        val decryptionIv = viewBinding.decryptionIv.text.toString()

        if (textToDecrypt.isEmpty() || decryptionIv.isEmpty() || decryptionKey.isEmpty()) {
            Toast.makeText(this, "Fill all the decryption text fields", Toast.LENGTH_SHORT).show()
            return
        }

        doDecryption(textToDecrypt, decryptionKey, decryptionIv)
    }

    private fun doDecryption(
        text: String,
        secret: String,
        iv: String,
    ) {
        var result = ""

        val textToDecrypt =  if (decOutputMode == 0) {
            Base64.decode(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        } else {
            text.toByteArray(Charsets.UTF_8)
        }

        val secretKey = generateKey(secret)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val decrypted: ByteArray
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv.toByteArray(Charsets.UTF_8)))
            decrypted = cipher.doFinal(textToDecrypt)
        } catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
            return
        }

        result = decrypted.toString(Charsets.UTF_8)
        Log.d("DECRYPTION", result)
        viewBinding.decryptResult.text = String(result.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
    }

    private fun startEncrypt() {
        val textToEncrypt = viewBinding.textToEncrypt.text.toString().trim()
        val encryptionKey = viewBinding.encryptionKey.text.toString().trim()
        val encryptionIv = viewBinding.encryptionIv.text.toString()

        if (textToEncrypt.isEmpty() || encryptionIv.isEmpty() || encryptionKey.isEmpty()) {
            Toast.makeText(this, "Fill all the encryption text fields", Toast.LENGTH_SHORT).show()
            return
        }

        doEncryption(textToEncrypt, encryptionKey, encryptionIv)

    }

    private fun doEncryption(
        text: String,
        secret: String,
        iv: String,
    ) {
        var result = ""

        val secretKey = generateKey(secret)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv.toByteArray(Charsets.UTF_8)))
        } catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
            return
        }
        val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        result = if (encOutputMode == 0) {
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } else {
            encrypted.toString(Charsets.UTF_8)
        }
        Log.d("ENCRYPTION", result)
        viewBinding.encryptResult.text = result

    }

    private fun generateKey(key: String): SecretKey {
        return SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
    }

    override fun onDestroy() {
        super.onDestroy()
        okHttpClient.dispatcher.executorService.shutdown()
    }
}