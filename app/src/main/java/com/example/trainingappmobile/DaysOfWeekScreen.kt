package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

class DaysOfWeekScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_days_of_week)

        // Recuperar o tipo de dados (TRAINING ou DIET)
        val dataType = intent.getStringExtra("DATA_TYPE") ?: "TRAINING"
        Log.d("DaysOfWeekScreen", "DATA_TYPE recebido: $dataType")

        // Referências aos botões dos dias
        val mondayButton = findViewById<LinearLayout>(R.id.monday_button)
        val tuesdayButton = findViewById<LinearLayout>(R.id.tuesday_button)
        val wednesdayButton = findViewById<LinearLayout>(R.id.wednesday_button)
        val thursdayButton = findViewById<LinearLayout>(R.id.thursday_button)
        val fridayButton = findViewById<LinearLayout>(R.id.friday_button)
        val saturdayButton = findViewById<LinearLayout>(R.id.saturday_button)
        val sundayButton = findViewById<LinearLayout>(R.id.sunday_button)
        val backButton = findViewById<LinearLayout>(R.id.back_button)

        // Configurar cliques para cada dia
        mondayButton.setOnClickListener {
            navigateToDayDetails("Segunda-feira", dataType)
        }

        tuesdayButton.setOnClickListener {
            navigateToDayDetails("Terça-feira", dataType)
        }

        wednesdayButton.setOnClickListener {
            navigateToDayDetails("Quarta-feira", dataType)
        }

        thursdayButton.setOnClickListener {
            navigateToDayDetails("Quinta-feira", dataType)
        }

        fridayButton.setOnClickListener {
            navigateToDayDetails("Sexta-feira", dataType)
        }

        saturdayButton.setOnClickListener {
            navigateToDayDetails("Sábado", dataType)
        }

        sundayButton.setOnClickListener {
            navigateToDayDetails("Domingo", dataType)
        }

        // Configurar clique no botão de voltar
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun navigateToDayDetails(dayOfWeek: String, dataType: String) {
        Log.d("DaysOfWeekScreen", "Navegando para DayDetailsScreen - Dia: $dayOfWeek, Tipo: $dataType")
        val intent = Intent(this, DayDetailsScreen::class.java)
        intent.putExtra("DAY_OF_WEEK", dayOfWeek)
        intent.putExtra("DATA_TYPE", dataType)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}