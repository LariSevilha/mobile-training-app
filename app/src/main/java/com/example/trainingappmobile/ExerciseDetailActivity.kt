package com.example.trainingappmobile

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class ExerciseDetailActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Receber dados da Intent
        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Nome do Exercício"
        val exerciseDescription = intent.getStringExtra("EXERCISE_DESCRIPTION") ?: "Descrição não disponível."
        val exerciseVideo = intent.getStringExtra("EXERCISE_VIDEO")
        val exercisePhotos = intent.getStringArrayExtra("EXERCISE_PHOTOS")
        val serieAmount = intent.getStringExtra("SERIE_AMOUNT") ?: "0"
        val repeatAmount = intent.getStringExtra("REPEAT_AMOUNT") ?: "0"
        val weekday = intent.getStringExtra("WEEKDAY") ?: "Não definido"

        setupUI(exerciseName, exerciseDescription, exerciseVideo, exercisePhotos, serieAmount, repeatAmount, weekday)
    }

    private fun setupUI(
        exerciseName: String,
        exerciseDescription: String,
        exerciseVideo: String?,
        exercisePhotos: Array<String>?,
        serieAmount: String,
        repeatAmount: String,
        weekday: String
    ) {
        // Configurar título do exercício
        val exerciseTitle = findViewById<TextView>(R.id.exercise_title)
        exerciseTitle.text = exerciseName.uppercase()

        // Configurar séries e repetições
        val seriesCount = findViewById<TextView>(R.id.series_count)
        val repetitionsCount = findViewById<TextView>(R.id.repetitions_count)
        seriesCount.text = serieAmount
        repetitionsCount.text = repeatAmount

        // Configurar dia da semana
        val weekdayText = findViewById<TextView>(R.id.weekday_text)
        weekdayText.text = convertWeekdayToPortuguese(weekday)

        // Configurar descrição/instruções
        val howToDoText = findViewById<TextView>(R.id.how_to_do_text)
        howToDoText.text = exerciseDescription

        // Configurar imagem do exercício
        setupExerciseImage(exercisePhotos)

        // Configurar vídeo
        setupVideoLink(exerciseVideo)

        // Configurar botão voltar
        val backButton = findViewById<LinearLayout>(R.id.back_button_detail)
        backButton.setOnClickListener { finish() }
    }

    private fun setupExerciseImage(exercisePhotos: Array<String>?) {
        val exerciseImage = findViewById<ImageView>(R.id.exercise_image)
        val noImageText = findViewById<TextView>(R.id.no_image_text)

        if (!exercisePhotos.isNullOrEmpty() && exercisePhotos[0].isNotEmpty()) {
            exerciseImage.visibility = View.VISIBLE
            noImageText.visibility = View.GONE

            Glide.with(this)
                .load(exercisePhotos[0])
                .placeholder(android.R.color.transparent)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(exerciseImage)
        } else {
            exerciseImage.visibility = View.GONE
            noImageText.visibility = View.VISIBLE
        }
    }

    private fun setupVideoLink(exerciseVideo: String?) {
        val videoContainer = findViewById<LinearLayout>(R.id.video_container)
        val videoLink = findViewById<LinearLayout>(R.id.video_link)

        if (!exerciseVideo.isNullOrEmpty()) {
            videoContainer.visibility = View.VISIBLE
            videoLink.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exerciseVideo))
                    startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        this,
                        "Não foi possível abrir o vídeo",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            videoContainer.visibility = View.GONE
        }
    }

    private fun convertWeekdayToPortuguese(weekday: String): String {
        return when (weekday.lowercase()) {
            "monday", "segunda", "0" -> "Segunda-feira"
            "tuesday", "terça", "terca", "1" -> "Terça-feira"
            "wednesday", "quarta", "2" -> "Quarta-feira"
            "thursday", "quinta", "3" -> "Quinta-feira"
            "friday", "sexta", "4" -> "Sexta-feira"
            "saturday", "sábado", "sabado", "5" -> "Sábado"
            "sunday", "domingo", "6" -> "Domingo"
            else -> weekday
        }
    }
}