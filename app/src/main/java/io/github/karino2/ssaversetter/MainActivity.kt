package io.github.karino2.ssaversetter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.onyx.android.sdk.api.device.screensaver.ScreenResourceManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent?.let {
            if (it.action == Intent.ACTION_SEND && it.type?.startsWith("image/") == true) {
                handleSendImage(it)
            }
        }

    }

    // 前回と同じ名前だとインテント受け取った側で無視されてしまう。
    // Pictures下にファイルがあるかどうかで判定しているっぽい。
    // このファイルのwrite permissionは無いので違う名前をつけていく。
    // 定期的に手で消してね。
    // このファイルパスで良いかはデバイス依存かもしれないので、無理そうならデフォルトの名前を返す。
    private fun findNextFileName() : String {
        try
        {
            val prefix = "/storage/emulated/0/Pictures/SSaverSetter"
            for(i in 0 until 100) {
                if(!File("${prefix}-${i}.png").exists())
                {
                    return "SSaverSetter-${i}.png"
                }
            }
            println("giveup")
            return "SSaverSetter.png"
        }catch(_: IOException) {
            println("io exception")
            return "SSaverSetter.png"
        }
    }

    private fun handleSendImage(intent: Intent) {
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        if (picturesDir != null && !picturesDir.exists()) picturesDir.mkdirs()

                        // これまで作ったファイルは削除する。基本的にはアプリprivateなエリアなので全部消して良いはずだが、
                        // 昔の変な実装が残ったデバイスとかあったら嫌なので名前でフィルタしておく。
                        picturesDir?.listFiles()?.forEach {
                            if (it.name.startsWith("SSaverSetter") && it.name.endsWith(".png")) {
                                it.delete()
                            }
                        }

                        val file = File(picturesDir, findNextFileName())

                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }

                        // Use ScreenResourceManager to set screensaver.
                        val path = file.absolutePath
                        val success = ScreenResourceManager.setScreensaver(this, path, true)

                        if (success) {
                            Toast.makeText(this, "Screensaver set!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to set screensaver.", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
