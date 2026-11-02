package com.parental.kuncigame

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this).apply {
            text = "Aktifkan Proteksi Kunci Game"
            textSize = 18f
            setPadding(32, 32, 32, 32)
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(context, "Cari 'Layanan Pengunci Game Anak' lalu Aktifkan!", Toast.LENGTH_LONG).show()
            }
        }

        setContentView(button)
    }
}
