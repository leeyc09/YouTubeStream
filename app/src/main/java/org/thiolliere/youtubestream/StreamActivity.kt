package org.thiolliere.youtubestream

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLDecoder
import javax.net.ssl.HttpsURLConnection

class StreamActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        var mainURL: Pair<String, String>? = null
        val link = intent.getStringExtra(Intent.EXTRA_TEXT)

        Log.i("link", link as String)

        val policy = StrictMode.ThreadPolicy.Builder().permitNetwork().build()
        StrictMode.setThreadPolicy(policy)

        var match = """^.*(?:(?:youtu\.be/|v/|vi/|u/w/|embed/)|(?:(?:watch)?\?v(?:i)?=|&v(?:i)?=))([^#&?]*).*""".toRegex()
                .find(link)

        if (match == null) {
            Toast.makeText(applicationContext, "Invalid Link:\n$link", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val videoCode = match.destructured.component1()

        val url = URL("https://youtube.com/get_video_info?video_id=$videoCode&format=json")

        val response = StringBuffer()

        with(url.openConnection() as HttpsURLConnection) {
            val charArray = CharArray(180)
            BufferedReader(InputStreamReader(inputStream)).use {
                var len = it.read(charArray)
                while (len != -1) {
                    response.append(charArray, 0, len)
                    len = it.read(charArray)
                }
            }
        }

        match = """url_encoded_fmt_stream_map=([^&]*)""".toRegex().find(response.toString())

        if (match == null) {
            Toast.makeText(applicationContext, getString(R.string.fail_to_get_stream), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val streamsEncoded = match.destructured.component1()

        if (streamsEncoded == "") {
            Toast.makeText(applicationContext, getString(R.string.fail_to_get_stream), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        for (stream in URLDecoder.decode(streamsEncoded, "UTF-8").split(",")) {
            val stream_format = """type=([^#&?]*)""".toRegex().find(stream)!!.destructured.component1()
            //val quality = """quality=([^#&?]*)""".toRegex().find(stream)!!.destructured.component1()
            val stream_url = """url=([^#&?]*)""".toRegex().find(stream)!!.destructured.component1()

            if (mainURL == null) {
                mainURL = Pair(URLDecoder.decode(stream_url, "UTF-8"), URLDecoder.decode(stream_format,"UTF-8"))
            }
        }

        Log.i("stream", mainURL.toString())

        val (uri, format) = mainURL!!

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setDataAndType(Uri.parse(uri), format)
            startActivity(intent)
        }
        catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.error_activity_not_found).format(format), Toast.LENGTH_LONG).show()
        }

        finish()
    }
}
