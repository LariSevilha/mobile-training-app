package com.example.trainingappmobile

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class DayDetailsScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)

        val day = intent.getStringExtra("DAY_OF_WEEK")
        val type = intent.getStringExtra("SCREEN_TYPE")

        if (day == null || type == null) {
            Toast.makeText(this, "Erro ao carregar dados da tela", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("DayDetailsScreen", "Abrindo detalhes para $day / $type")

        val title = findViewById<TextView>(R.id.day_title)
        title.text = "$type para $day"
    }
}
