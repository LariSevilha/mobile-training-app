package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity

class DaysOfWeekScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_days_of_week)

        // Recuperar o tipo de dados (TRAINING, DIET ou PDF)
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
            navigateToDetails("Segunda-feira", dataType)
        }
        tuesdayButton.setOnClickListener {
            navigateToDetails("Terça-feira", dataType)
        }
        wednesdayButton.setOnClickListener {
            navigateToDetails("Quarta-feira", dataType)
        }
        thursdayButton.setOnClickListener {
            navigateToDetails("Quinta-feira", dataType)
        }
        fridayButton.setOnClickListener {
            navigateToDetails("Sexta-feira", dataType)
        }
        saturdayButton.setOnClickListener {
            navigateToDetails("Sábado", dataType)
        }
        sundayButton.setOnClickListener {
            navigateToDetails("Domingo", dataType)
        }

        // Configurar clique no botão de voltar
        backButton.setOnClickListener {
            Log.d("DaysOfWeekScreen", "Botão Voltar clicado")
            finish()
        }
    }

    private fun navigateToDetails(dayOfWeek: String, dataType: String) {
        Log.d("DaysOfWeekScreen", "Navegando para tela de detalhes - Dia: $dayOfWeek, Tipo: $dataType")
        try {
            val intent = when (dataType) {
                "TRAINING" -> Intent(this, ExerciseDetailActivity::class.java)
                "DIET" -> Intent(this, DietDetailActivity::class.java)
                "PDF" -> Intent(this, PdfViewerScreen::class.java)
                else -> Intent(this, ExerciseDetailActivity::class.java) // Fallback
            }
            intent.putExtra("DAY_OF_WEEK", dayOfWeek)
            intent.putExtra("DATA_TYPE", dataType)
            startActivity(intent)
            Log.d("DaysOfWeekScreen", "Intent iniciado com sucesso")
        } catch (e: Exception) {
            Log.e("DaysOfWeekScreen", "Erro ao iniciar tela de detalhes: ${e.message}", e)
            Toast.makeText(this, "Erro ao navegar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        Log.d("DaysOfWeekScreen", "onBackPressed chamado")
        super.onBackPressed()
        finish()
    }
}