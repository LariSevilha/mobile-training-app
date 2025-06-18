package com.example.trainingappmobile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi

class PdfViewerScreen : ComponentActivity() {

    private lateinit var pdfRenderView: PdfRenderView

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PROTEÇÃO CONTRA SCREENSHOTS E GRAVAÇÃO DE TELA
        setupSecurityMeasures()

        setContentView(R.layout.activity_pdf_viewer)

        initializeViews()
        loadPdfContent()
    }

    private fun setupSecurityMeasures() {
        // Impedir screenshots e gravação de tela
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Impedir que o app apareça no recent apps com conteúdo visível
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }

        // Adicionar proteção contra debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }
    }

    private fun initializeViews() {
        // Botão de voltar
        val backButton = findViewById<LinearLayout>(R.id.back_button)
        backButton.setOnClickListener {
            try {
                Log.d("PdfViewerScreen", "Voltando para a tela anterior")
                finish()
            } catch (e: Exception) {
                Log.e("PdfViewerScreen", "Erro ao voltar: ${e.message}", e)
                Toast.makeText(this, "Erro ao voltar", Toast.LENGTH_SHORT).show()
            }
        }

        // PDF Render View
        pdfRenderView = findViewById(R.id.pdf_render_view)
    }

    private fun loadPdfContent() {
        val pdfUrl = intent.getStringExtra("PDF_URL")

        if (pdfUrl.isNullOrEmpty()) {
            Log.e("PdfViewerScreen", "URL do PDF não fornecida")
            Toast.makeText(this, "PDF não disponível", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Verificar versão do Android
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.e("PdfViewerScreen", "Versão do Android incompatível (requer API 21+ para PdfRenderer)")
            Toast.makeText(this, "Versão do Android incompatível", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            Log.d("PdfViewerScreen", "Carregando PDF: $pdfUrl")
            pdfRenderView.loadPdfFromUrl(pdfUrl)
        } catch (e: Exception) {
            Log.e("PdfViewerScreen", "Erro ao carregar PDF: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar PDF", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reforçar proteções quando o app volta ao foco
        preventScreenshots()
    }

    override fun onPause() {
        super.onPause()
        // Limpar qualquer cache sensível
        clearSensitiveData()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            pdfRenderView.close()
            Log.d("PdfViewerScreen", "PDF viewer fechado e recursos liberados")
        } catch (e: Exception) {
            Log.e("PdfViewerScreen", "Erro ao fechar PDF viewer: ${e.message}", e)
        }
    }

    private fun preventScreenshots() {
        // Método adicional para reforçar a proteção
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    private fun clearSensitiveData() {
        // Limpar cache de arquivos temporários
        try {
            val cacheDir = cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_pdf_")) {
                    file.delete()
                    Log.d("PdfViewerScreen", "Arquivo temporário removido: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("PdfViewerScreen", "Erro ao limpar cache: ${e.message}", e)
        }
    }

    // Impedir que o usuário use o botão de voltar do sistema para sair rapidamente
    override fun onBackPressed() {
        try {
            clearSensitiveData()
            super.onBackPressed()
        } catch (e: Exception) {
            Log.e("PdfViewerScreen", "Erro no onBackPressed: ${e.message}", e)
            finish()
        }
    }
}