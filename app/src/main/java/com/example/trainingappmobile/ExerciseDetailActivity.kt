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

        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Nome do Exercício"
        val exerciseDescription = intent.getStringExtra("EXERCISE_DESCRIPTION") ?: "Descrição não disponível."
        val exerciseVideo = intent.getStringExtra("EXERCISE_VIDEO")
        val exercisePhotos = intent.getStringArrayExtra("EXERCISE_PHOTOS")

        val exerciseTitle = findViewById<TextView>(R.id.exercise_title)
        exerciseTitle.text = "Como executar: $exerciseName".uppercase()

        val howToDoText = findViewById<TextView>(R.id.how_to_do_text)
        howToDoText.text = exerciseDescription

        val exerciseImage = findViewById<ImageView>(R.id.exercise_image)
        if (!exercisePhotos.isNullOrEmpty() && exercisePhotos[0].isNotEmpty()) {
            exerciseImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(exercisePhotos[0])
                .placeholder(android.R.color.transparent)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(exerciseImage)
        } else {
            exerciseImage.visibility = View.GONE
        }

        val videoLink = findViewById<TextView>(R.id.video_link)
        if (!exerciseVideo.isNullOrEmpty()) {
            videoLink.visibility = View.VISIBLE
            videoLink.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exerciseVideo))
                    startActivity(intent)
                } catch (e: Exception) {
                    // Handle invalid or inaccessible URLs
                    android.widget.Toast.makeText(this, "Não foi possível abrir o vídeo", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            videoLink.visibility = View.GONE
        }

        val backButton = findViewById<LinearLayout>(R.id.back_button_detail)
        backButton.setOnClickListener { finish() }
    }
}