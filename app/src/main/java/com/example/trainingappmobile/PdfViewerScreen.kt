package com.example.trainingappmobile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class PdfViewerScreen : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var pageNumberText: TextView
    private lateinit var totalPagesText: TextView
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfAdapter: PdfPagesAdapter? = null
    private var job: Job? = null
    private var tempFile: File? = null

    companion object {
        internal const val TAG = "PdfViewerScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupMaximumSecurity()
        setContentView(R.layout.activity_pdf_viewer)

        initializeViews()
        loadPdfContent()
    }

    private fun setupMaximumSecurity() {
        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setRecentsScreenshotEnabled(false)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                )
            }

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar segurança: ${e.message}", e)
        }
    }

    private fun initializeViews() {
        try {
            val backButton = findViewById<LinearLayout>(R.id.back_button)
            backButton.setOnClickListener { finish() }

            recyclerView = findViewById(R.id.pdf_recycler_view)
            progressBar = findViewById(R.id.progress_bar)
            pageNumberText = findViewById(R.id.page_number_text)
            totalPagesText = findViewById(R.id.total_pages_text)

            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.setHasFixedSize(true)

            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updatePageNumber()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar views: ${e.message}", e)
            showError("Erro ao inicializar interface")
        }
    }

    private fun updatePageNumber() {
        try {
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.let {
                val currentPage = it.findFirstVisibleItemPosition() + 1
                val totalPages = pdfAdapter?.itemCount ?: 0
                if (currentPage > 0 && totalPages > 0) {
                    pageNumberText.text = currentPage.toString()
                    totalPagesText.text = "de $totalPages"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar número da página: ${e.message}", e)
        }
    }

    private fun loadPdfContent() {
        val pdfUrl = intent.getStringExtra("PDF_URL")
        val pdfPath = intent.getStringExtra("PDF_PATH")

        Log.d(TAG, "=== CARREGANDO PDF ===")
        Log.d(TAG, "PDF_URL: $pdfUrl")
        Log.d(TAG, "PDF_PATH: $pdfPath")

        when {
            !pdfUrl.isNullOrEmpty() -> loadPdfFromUrl(pdfUrl)
            !pdfPath.isNullOrEmpty() -> loadPdfFromPath(pdfPath)
            else -> {
                Log.e(TAG, "Nenhuma URL ou caminho do PDF fornecido")
                showError("PDF não disponível - URL/caminho não fornecido")
            }
        }
    }

    private fun loadPdfFromPath(pdfPath: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            showError("Android 5.0+ necessário para visualizar PDFs")
            return
        }

        progressBar.visibility = View.VISIBLE

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(pdfPath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        showError("Arquivo PDF não encontrado")
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    renderPdf(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Erro ao carregar PDF local: ${e.message}", e)
                    showError("Erro ao carregar PDF: ${e.message}")
                }
            }
        }
    }

    private fun loadPdfFromUrl(pdfUrl: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            showError("Android 5.0+ necessário para visualizar PDFs")
            return
        }

        Log.d(TAG, "Verificando versão Android: ${Build.VERSION.SDK_INT}")
        progressBar.visibility = View.VISIBLE

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Iniciando download do PDF: $pdfUrl")

                if (!isValidUrl(pdfUrl)) {
                    withContext(Dispatchers.Main) {
                        showError("URL do PDF inválida")
                    }
                    return@launch
                }

                val downloadedFile = downloadPdfSecurely(pdfUrl)
                Log.d(TAG, "PDF baixado com sucesso: ${downloadedFile.absolutePath}")

                withContext(Dispatchers.Main) {
                    renderPdf(downloadedFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Erro ao processar PDF: ${e.message}", e)
                    showError("Erro ao carregar PDF: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (e: Exception) {
            Log.e(TAG, "URL inválida: $url", e)
            false
        }
    }

    private suspend fun downloadPdfSecurely(pdfUrl: String): File = withContext(Dispatchers.IO) {
        Log.d(TAG, "Iniciando download seguro do PDF: $pdfUrl")
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null) ?: throw Exception("Token não encontrado")
        val deviceId = prefs.getString("device_id", null) ?: throw Exception("Device ID não encontrado")

        val url = URL(pdfUrl)
        val connection = if (pdfUrl.startsWith("https")) {
            (url.openConnection() as HttpsURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Device-ID", deviceId)
                setRequestProperty("User-Agent", "TrainingApp-Mobile/1.0")
                setRequestProperty("Accept", "application/pdf")
            }
        } else {
            (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 30000
                readTimeout = 60000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Device-ID", deviceId)
                setRequestProperty("User-Agent", "TrainingApp-Mobile/1.0")
                setRequestProperty("Accept", "application/pdf")
            }
        }

        try {
            connection.connect()
            Log.d(TAG, "Conexão estabelecida")
            if (connection is java.net.HttpURLConnection) {
                val responseCode = connection.responseCode
                Log.d(TAG, "Código de resposta: $responseCode")
                if (responseCode != 200) {
                    throw Exception("Erro do servidor: $responseCode - ${connection.responseMessage}")
                }
            }

            val input = connection.getInputStream()
            tempFile = File(cacheDir, "secure_pdf_${System.currentTimeMillis()}.pdf")
            FileOutputStream(tempFile!!).use { output ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytes = 0
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }
                Log.d(TAG, "Download concluído: $totalBytes bytes")
            }
            input.close()

            if (!tempFile!!.exists() || tempFile!!.length() == 0L) {
                throw Exception("Falha ao baixar arquivo PDF")
            }
            tempFile!!
        } finally {
            connection.disconnect()
        }
    }
    private fun renderPdf(file: File) {
        try {
            Log.d(TAG, "=== RENDERIZANDO PDF ===")
            Log.d(TAG, "Arquivo: ${file.absolutePath}")
            Log.d(TAG, "Existe: ${file.exists()}")
            Log.d(TAG, "Tamanho: ${file.length()} bytes")

            progressBar.visibility = View.VISIBLE

            closePdfRenderer()

            if (!file.exists() || file.length() == 0L) {
                showError("Arquivo PDF não encontrado ou vazio")
                return
            }

            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)

            val pageCount = pdfRenderer!!.pageCount
            Log.d(TAG, "PDF carregado com sucesso. Páginas: $pageCount")

            if (pageCount == 0) {
                showError("PDF vazio ou corrompido")
                return
            }

            pdfAdapter = PdfPagesAdapter(pdfRenderer!!, pageCount)
            recyclerView.adapter = pdfAdapter

            totalPagesText.text = "de $pageCount"
            pageNumberText.text = "1"

            progressBar.visibility = View.GONE
            Log.d(TAG, "PDF renderizado com sucesso")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao renderizar PDF: ${e.message}", e)
            showError("Erro ao abrir PDF: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Erro: $message")
        progressBar.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        reinforceSecurity()
    }

    override fun onPause() {
        super.onPause()
        clearSensitiveData()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            job?.cancel()
            closePdfRenderer()
            clearSensitiveData()
            Log.d(TAG, "PDF viewer fechado e recursos liberados")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fechar PDF viewer: ${e.message}", e)
        }
    }

    private fun reinforceSecurity() {
        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao reforçar segurança: ${e.message}", e)
        }
    }

    private fun clearSensitiveData() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("secure_pdf_")) {
                    if (file.delete()) {
                        Log.d(TAG, "Arquivo temporário removido: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar cache: ${e.message}", e)
        }
    }

    private fun closePdfRenderer() {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fechar PdfRenderer: ${e.message}", e)
        } finally {
            pdfRenderer = null
            fileDescriptor = null
        }
    }
}

// Adapter para as páginas do PDF
class PdfPagesAdapter(
    private val pdfRenderer: PdfRenderer,
    private val pageCount: Int
) : RecyclerView.Adapter<PdfPagesAdapter.PdfPageViewHolder>() {

    companion object {
        private const val TAG = "PdfPagesAdapter"
    }

    class PdfPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.page_image_view)
        val progressBar: ProgressBar = itemView.findViewById(R.id.page_progress_bar)
        val pageNumberText: TextView = itemView.findViewById(R.id.page_number_small)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.pageNumberText.text = "Página ${position + 1}"
        holder.progressBar.visibility = View.VISIBLE
        holder.imageView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = renderPage(position)

                if (holder.itemView.isAttachedToWindow) {
                    CoroutineScope(Dispatchers.Main).launch {
                        holder.imageView.setImageBitmap(bitmap)
                        holder.imageView.visibility = View.VISIBLE
                        holder.progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao renderizar página $position: ${e.message}", e)

                if (holder.itemView.isAttachedToWindow) {
                    CoroutineScope(Dispatchers.Main).launch {
                        holder.progressBar.visibility = View.GONE
                        holder.pageNumberText.text = "Erro ao carregar página ${position + 1}"
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = pageCount

    private fun renderPage(pageIndex: Int): Bitmap {
        val page = pdfRenderer.openPage(pageIndex)

        try {
            val displayMetrics = android.content.res.Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels - 64 // margin maior

            val pageWidth = page.width
            val pageHeight = page.height
            val aspectRatio = pageHeight.toFloat() / pageWidth.toFloat()

            val bitmapWidth = screenWidth
            val bitmapHeight = (screenWidth * aspectRatio).toInt()

            val bitmap = Bitmap.createBitmap(
                bitmapWidth,
                bitmapHeight,
                Bitmap.Config.ARGB_8888
            )

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            return bitmap
        } finally {
            page.close()
        }
    }
}