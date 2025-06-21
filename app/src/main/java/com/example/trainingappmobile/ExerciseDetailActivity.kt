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
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ExerciseDetailActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Nome do Exercício"
        val exerciseDescription = intent.getStringExtra("EXERCISE_DESCRIPTION") ?: "Descrição não disponível."
        val exerciseVideo = intent.getStringExtra("EXERCISE_VIDEO")
        val exercisePhotos = intent.getStringArrayExtra("EXERCISE_PHOTOS")

        val exerciseTitle = findViewById<TextView>(R.id.exercise_title)
        exerciseTitle.text = "Como executar: $exerciseName".uppercase()

        val howToDoText = findViewById<TextView>(R.id.how_to_do_text)
        howToDoText.text = "COMO FAZER:\n$exerciseDescription"

        val exerciseImage = findViewById<ImageView>(R.id.exercise_image)
        if (!exercisePhotos.isNullOrEmpty()) {
            exerciseImage.visibility = View.VISIBLE
//            Glide.with(this)
//                .load(exercisePhotos[0])
//                .placeholder(android.R.color.transparent)
//                .error(R.drawable.ic_error)
//                .into(exerciseImage)
        }

        val videoLink = findViewById<TextView>(R.id.video_link)
        if (!exerciseVideo.isNullOrEmpty()) {
            videoLink.visibility = View.VISIBLE
            videoLink.text = "VEJA NA PRÁTICA: Clique para ver o vídeo"
            videoLink.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exerciseVideo))
                startActivity(intent)
            }
        }

        val backButton = findViewById<LinearLayout>(R.id.back_button_detail)
        backButton.setOnClickListener { finish() }

        // Hide tips section since it's redundant
        findViewById<TextView>(R.id.tips_title).visibility = View.GONE
        findViewById<TextView>(R.id.tips_text).visibility = View.GONE
    }
}