
package com.example.trainingappmobile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide

class ExerciseDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_details)

        // Retrieve data from Intent
        val exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: "Nome não disponível"
        val exerciseDescription = intent.getStringExtra("EXERCISE_DESCRIPTION") ?: "Descrição não disponível"
        val exerciseVideo = intent.getStringExtra("EXERCISE_VIDEO")
        val exercisePhotos = intent.getStringArrayExtra("EXERCISE_PHOTOS") ?: emptyArray()
        val serieAmount = intent.getStringExtra("SERIE_AMOUNT") ?: "0"
        val repeatAmount = intent.getStringExtra("REPEAT_AMOUNT") ?: "0"
        val weekday = intent.getStringExtra("WEEKDAY") ?: "Não definido"

        // Set up UI elements
        val exerciseTitle = findViewById<TextView>(R.id.exercise_title)
        val seriesCount = findViewById<TextView>(R.id.series_count)
        val repetitionsCount = findViewById<TextView>(R.id.repetitions_count)
        val weekdayText = findViewById<TextView>(R.id.weekday_text)
        val howToDoText = findViewById<TextView>(R.id.how_to_do_text)
        val howToDoContainer = findViewById<LinearLayout>(R.id.how_to_do_container)
        val exerciseImage = findViewById<ImageView>(R.id.exercise_image)
        val noImageText = findViewById<TextView>(R.id.no_image_text)
        val videoContainer = findViewById<LinearLayout>(R.id.video_container)
        val videoLink = findViewById<LinearLayout>(R.id.video_link)
        val backButton = findViewById<LinearLayout>(R.id.back_button_detail)

        // Populate UI elements
        exerciseTitle.text = exerciseName
        seriesCount.text = serieAmount
        repetitionsCount.text = repeatAmount
        weekdayText.text = weekday

        // Handle description
        if (exerciseDescription == "Descrição não disponível") {
            howToDoContainer.visibility = android.view.View.GONE
        } else {
            howToDoText.text = exerciseDescription
            howToDoContainer.visibility = android.view.View.VISIBLE
        }

        // Handle exercise image
        if (exercisePhotos.isNotEmpty()) {
            Glide.with(this)
                .load(exercisePhotos[0])
                .into(exerciseImage)
            exerciseImage.visibility = android.view.View.VISIBLE
            noImageText.visibility = android.view.View.GONE
        } else {
            exerciseImage.visibility = android.view.View.GONE
            noImageText.visibility = android.view.View.VISIBLE
        }

        // Handle video link
        if (!exerciseVideo.isNullOrEmpty()) {
            videoContainer.visibility = android.view.View.VISIBLE
            videoLink.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(exerciseVideo))
                    startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "Não foi possível abrir o vídeo", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            videoContainer.visibility = android.view.View.GONE
        }

        // Handle back button
        backButton.setOnClickListener { finish() }
    }
}