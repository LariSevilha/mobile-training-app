package com.example.trainingappmobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.view.WindowManager

class ExerciseDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        // Impedir capturas de tela
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Recuperar dados do treino
        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Nome do Exercício"
        val exerciseDescription = intent.getStringExtra("EXERCISE_DESCRIPTION") ?: "Descrição não disponível."
        val exerciseVideo = intent.getStringExtra("EXERCISE_VIDEO")
        val exercisePhotos = intent.getStringArrayExtra("EXERCISE_PHOTOS") // Array de URLs de fotos

        // Atualizar título
        val exerciseTitle = findViewById<TextView>(R.id.exercise_title)
        exerciseTitle.text = "Como executar: $exerciseName".uppercase()

        // Atualizar instruções
        val howToDoText = findViewById<TextView>(R.id.how_to_do_text)
        howToDoText.text = "COMO FAZER:\n$exerciseDescription"

        // Configurar imagens (se disponíveis)
        val exerciseImage = findViewById<ImageView>(R.id.exercise_image)
        if (!exercisePhotos.isNullOrEmpty()) {
            exerciseImage.visibility = ImageView.VISIBLE
        }

        // Configurar vídeo (se disponível)
        val videoLink = findViewById<TextView>(R.id.video_link)
        if (!exerciseVideo.isNullOrEmpty()) {
            videoLink.visibility = TextView.VISIBLE
            videoLink.text = "VEJA NA PRÁTICA: Clique para ver o vídeo"
            videoLink.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exerciseVideo))
                startActivity(intent)
            }
        }

        // Configurar botão de voltar
        val backButton = findViewById<LinearLayout>(R.id.back_button_detail)
        backButton.setOnClickListener {
            finish()
        }
    }
}