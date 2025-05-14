package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("api_key", null)
        val deviceId = sharedPrefs.getString("device_id", null)

        if (apiKey != null && deviceId != null) {
            Log.d("MainActivity", "Credenciais encontradas, navegando para HomeScreen")
            startActivity(Intent(this, HomeScreen::class.java))
            finish()
            return
        }

        val emailEditText = findViewById<EditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<EditText>(R.id.password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_button)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginRequest = LoginRequest(email, password, "android-device-id")
            RetrofitClient.apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()!!
                        if (loginResponse.error == null) {
                            Log.d("MainActivity", "Login bem-sucedido: ${loginResponse.api_key}")
                            sharedPrefs.edit().apply {
                                putString("api_key", loginResponse.api_key)
                                putString("device_id", "android-device-id")
                                putString("user_role", loginResponse.user_role)
                                apply()
                            }
                            Toast.makeText(this@MainActivity, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MainActivity, HomeScreen::class.java))
                            finish()
                        } else {
                            Log.e("MainActivity", "Erro no login: ${loginResponse.error}")
                            Toast.makeText(this@MainActivity, loginResponse.error, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("MainActivity", "Erro na resposta: ${response.code()} - ${response.message()}")
                        Toast.makeText(this@MainActivity, "Erro ao fazer login: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("MainActivity", "Falha na requisição: ${t.message}", t)
                    Toast.makeText(this@MainActivity, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}