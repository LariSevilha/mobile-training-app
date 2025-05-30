package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Recuperar o nome do usuário logado
        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val userName = sharedPrefs.getString("name", "Usuário")
        val greetingText = findViewById<TextView>(R.id.greeting_text)
        greetingText.text = "Bem-vindo(a), $userName!"

        // Referências aos elementos do novo layout
        val trainingButton = findViewById<LinearLayout>(R.id.training_button)
        val dietButton = findViewById<LinearLayout>(R.id.diet_button)
        val noDataText = findViewById<TextView>(R.id.no_data_text)

        // Configurar cliques dos botões
        trainingButton.setOnClickListener {
            try {
                Log.d("HomeScreen", "Navegando para DaysOfWeekScreen com DATA_TYPE=TRAINING")
                val intent = Intent(this, DaysOfWeekScreen::class.java)
                intent.putExtra("DATA_TYPE", "TRAINING")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("HomeScreen", "Erro ao navegar para DaysOfWeekScreen: ${e.message}", e)
                Toast.makeText(this, "Erro ao abrir a tela: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        dietButton.setOnClickListener {
            try {
                Log.d("HomeScreen", "Navegando para DaysOfWeekScreen com DATA_TYPE=DIET")
                val intent = Intent(this, DaysOfWeekScreen::class.java)
                intent.putExtra("DATA_TYPE", "DIET")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("HomeScreen", "Erro ao navegar para DaysOfWeekScreen: ${e.message}", e)
                Toast.makeText(this, "Erro ao abrir a tela: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Verificar autenticação
        val apiKey = sharedPrefs.getString("api_key", null)
        val deviceId = sharedPrefs.getString("device_id", null)
        Log.d("HomeScreen", "Credenciais: apiKey=$apiKey, deviceId=$deviceId")

        if (apiKey == null || deviceId == null) {
            Log.e("HomeScreen", "apiKey ou deviceId está nulo")
            Toast.makeText(this, "Por favor, faça login novamente", Toast.LENGTH_SHORT).show()
            sharedPrefs.edit().clear().apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // Verificar treinos e dieta via API
        checkPlanilhaData(apiKey, deviceId, trainingButton, dietButton, noDataText)
    }

    private fun checkPlanilhaData(
        apiKey: String,
        deviceId: String,
        trainingButton: LinearLayout,
        dietButton: LinearLayout,
        noDataText: TextView
    ) {
        val authHeader = "Bearer $apiKey"
        Log.d("HomeScreen", "Verificando dados com authHeader=$authHeader, deviceId=$deviceId")
        RetrofitClient.apiService.getPlanilha(authHeader, deviceId).enqueue(object : Callback<PlanilhaResponse> {
            override fun onResponse(call: Call<PlanilhaResponse>, response: Response<PlanilhaResponse>) {
                Log.d("HomeScreen", "Resposta recebida: ${response.code()} - ${response.raw()}")
                if (response.isSuccessful && response.body() != null) {
                    val planilha = response.body()!!
                    Log.d("HomeScreen", "Dados da planilha: $planilha")
                    if (planilha.error == null) {
                        // Verificar existência de treinos e dieta
                        val hasTrainings = !planilha.trainings.isNullOrEmpty()
                        val hasMeals = !planilha.meals.isNullOrEmpty()

                        // Ajustar visibilidade dos botões
                        trainingButton.visibility = if (hasTrainings) View.VISIBLE else View.GONE
                        dietButton.visibility = if (hasMeals) View.VISIBLE else View.GONE

                        // Se não houver nenhum dado, exibir mensagem
                        noDataText.visibility = if (!hasTrainings && !hasMeals) View.VISIBLE else View.GONE
                    } else {
                        Log.e("HomeScreen", "Erro na resposta: ${planilha.error}")
                        Toast.makeText(this@HomeScreen, planilha.error, Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    }
                } else {
                    Log.e("HomeScreen", "Resposta não foi bem-sucedida: ${response.code()} - ${response.message()}")
                    Log.e("HomeScreen", "Corpo de erro: ${response.errorBody()?.string()}")
                    Toast.makeText(this@HomeScreen, "Erro ao verificar dados: ${response.message()}", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            }

            override fun onFailure(call: Call<PlanilhaResponse>, t: Throwable) {
                Log.e("HomeScreen", "Falha na requisição: ${t.message}", t)
                Toast.makeText(this@HomeScreen, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
        })
    }

    private fun navigateToLogin() {
        val sharedPreferences = getSharedPreferences("auth", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}