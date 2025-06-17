package com.example.trainingappmobile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class PdfViewerScreen : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        val backButton = findViewById<LinearLayout>(R.id.back_button)
        backButton.setOnClickListener {
            try {
                Log.d("PdfViewerScreen", "Voltando para a tela anterior")
                onBackPressed()
            } catch (e: Exception) {
                Log.e("PdfViewerScreen", "Erro ao voltar: ${e.message}", e)
                Toast.makeText(this, "Erro ao voltar", Toast.LENGTH_SHORT).show()
            }
        }

        val pdfRenderView = findViewById<com.example.trainingappmobile.PdfRenderView>(R.id.pdf_render_view)
        val pdfUrl = intent.getStringExtra("PDF_URL")
        if (pdfUrl.isNullOrEmpty()) {
            Log.e("PdfViewerScreen", "URL do PDF não fornecida")
            Toast.makeText(this, "PDF não disponível", Toast.LENGTH_SHORT).show()
        } else {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pdfRenderView.loadPdfFromUrl(pdfUrl)
                } else {
                    Log.e("PdfViewerScreen", "Versão do Android incompatível (requer API 23+ para PdfRenderer)")
                    Toast.makeText(this, "Versão incompatível", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PdfViewerScreen", "Erro ao carregar PDF: ${e.message}", e)
                Toast.makeText(this, "Erro ao carregar PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        findViewById<com.example.trainingappmobile.PdfRenderView>(R.id.pdf_render_view)?.close()
    }
}