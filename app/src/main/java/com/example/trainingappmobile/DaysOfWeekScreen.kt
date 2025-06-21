package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity

class DaysOfWeekScreen : AppCompatActivity() {

    companion object {
        private const val TAG = "DaysOfWeekScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Iniciando DaysOfWeekScreen")
            setContentView(R.layout.activity_days_of_week)

            val screenType = intent.getStringExtra("SCREEN_TYPE")
            Log.d(TAG, "Tipo de tela recebido: $screenType")

            if (screenType == null) {
                Toast.makeText(this, "Erro: tipo da tela não informado", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            initializeViews(screenType)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar DaysOfWeekScreen: ${e.message}")
            finish()
        }
    }

    private fun initializeViews(screenType: String) {
        try {
            val mondayButton = findViewById<LinearLayout>(R.id.monday_button)
            val tuesdayButton = findViewById<LinearLayout>(R.id.tuesday_button)
            val wednesdayButton = findViewById<LinearLayout>(R.id.wednesday_button)
            val thursdayButton = findViewById<LinearLayout>(R.id.thursday_button)
            val fridayButton = findViewById<LinearLayout>(R.id.friday_button)
            val saturdayButton = findViewById<LinearLayout>(R.id.saturday_button)
            val sundayButton = findViewById<LinearLayout>(R.id.sunday_button)
            val backButton = findViewById<LinearLayout>(R.id.back_button)

            // Configurar listeners dos botões
            mondayButton?.setOnClickListener { navigateToDayDetails("Segunda-feira", screenType) }
            tuesdayButton?.setOnClickListener { navigateToDayDetails("Terça-feira", screenType) }
            wednesdayButton?.setOnClickListener { navigateToDayDetails("Quarta-feira", screenType) }
            thursdayButton?.setOnClickListener { navigateToDayDetails("Quinta-feira", screenType) }
            fridayButton?.setOnClickListener { navigateToDayDetails("Sexta-feira", screenType) }
            saturdayButton?.setOnClickListener { navigateToDayDetails("Sábado", screenType) }
            sundayButton?.setOnClickListener { navigateToDayDetails("Domingo", screenType) }
            backButton?.setOnClickListener {
                Log.d(TAG, "Botão voltar pressionado")
                finish()
            }

            Log.d(TAG, "Views inicializadas com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar views: ${e.message}")
            finish()
        }
    }

    private fun navigateToDayDetails(dayOfWeek: String, screenType: String) {
        try {
            Log.d(TAG, "Navegando para detalhes do dia: $dayOfWeek, tipo: $screenType")
            val intent = Intent(this, DayDetailsScreen::class.java)
            intent.putExtra("DAY_OF_WEEK", dayOfWeek)
            intent.putExtra("SCREEN_TYPE", screenType)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao navegar para DayDetailsScreen: ${e.message}")
        }
    }
}
