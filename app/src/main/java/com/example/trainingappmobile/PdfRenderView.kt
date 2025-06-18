package com.example.trainingappmobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.math.max
import kotlin.math.min

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PdfRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex = 0
    private var totalPages = 0

    private val imageView: ImageView
    private val progressBar: ProgressBar
    private val pageInfoText: TextView
    private val controlsLayout: LinearLayout
    private val prevButton: TextView
    private val nextButton: TextView

    private var scaleFactor = 1.0f
    private var maxScale = 3.0f
    private var minScale = 0.5f

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    private var job: Job? = null

    init {
        // Configurar layout
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        // Criar componentes
        imageView = ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.MATRIX
            setBackgroundColor(Color.WHITE)
        }

        progressBar = ProgressBar(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            visibility = View.GONE
        }

        // Layout de controles
        controlsLayout = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM
            }
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E0000000"))
            setPadding(16, 16, 16, 16)
        }

        prevButton = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = "< Anterior"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(16, 12, 16, 12)
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FF6B6B"))
            setOnClickListener { previousPage() }
        }

        pageInfoText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
        }

        nextButton = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = "Próximo >"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(16, 12, 16, 12)
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.parseColor("#4ECDC4"))
            setOnClickListener { nextPage() }
        }

        controlsLayout.addView(prevButton)
        controlsLayout.addView(pageInfoText)
        controlsLayout.addView(nextButton)

        // Adicionar views
        addView(imageView)
        addView(progressBar)
        addView(controlsLayout)

        // Configurar gestos
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())

        // Configurar toque
        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }

        updatePageInfo()
    }

    fun loadPdfFromUrl(url: String) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            try {
                showLoading(true)
                val file = downloadPdf(url)
                loadPdfFromFile(file)
            } catch (e: Exception) {
                Log.e("PdfRenderView", "Erro ao carregar PDF: ${e.message}", e)
                showError("Erro ao carregar PDF")
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun downloadPdf(url: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")

        URL(url).openStream().use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        file
    }

    private fun loadPdfFromFile(file: File) {
        try {
            close() // Fechar PDF anterior se existir

            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            totalPages = pdfRenderer!!.pageCount
            currentPageIndex = 0

            renderCurrentPage()
            updatePageInfo()

        } catch (e: Exception) {
            Log.e("PdfRenderView", "Erro ao abrir PDF: ${e.message}", e)
            showError("Erro ao abrir PDF")
        }
    }

    private fun renderCurrentPage() {
        pdfRenderer?.let { renderer ->
            if (currentPageIndex < renderer.pageCount) {
                currentPage?.close()
                currentPage = renderer.openPage(currentPageIndex)

                val page = currentPage!!
                val bitmap = Bitmap.createBitmap(
                    (page.width * scaleFactor).toInt(),
                    (page.height * scaleFactor).toInt(),
                    Bitmap.Config.ARGB_8888
                )

                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                imageView.setImageBitmap(bitmap)
                updateNavigationButtons()
            }
        }
    }

    private fun updatePageInfo() {
        if (totalPages > 0) {
            pageInfoText.text = "${currentPageIndex + 1} / $totalPages"
        } else {
            pageInfoText.text = "0 / 0"
        }
    }

    private fun updateNavigationButtons() {
        prevButton.isEnabled = currentPageIndex > 0
        nextButton.isEnabled = currentPageIndex < totalPages - 1

        prevButton.alpha = if (prevButton.isEnabled) 1.0f else 0.5f
        nextButton.alpha = if (nextButton.isEnabled) 1.0f else 0.5f
    }

    private fun previousPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--
            renderCurrentPage()
            updatePageInfo()
        }
    }

    private fun nextPage() {
        if (currentPageIndex < totalPages - 1) {
            currentPageIndex++
            renderCurrentPage()
            updatePageInfo()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        controlsLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        // Criar TextView de erro
        val errorText = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            text = message
            textSize = 16f
            setTextColor(Color.RED)
            gravity = android.view.Gravity.CENTER
        }

        removeAllViews()
        addView(errorText)
    }

    fun close() {
        job?.cancel()
        currentPage?.close()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()

        currentPage = null
        pdfRenderer = null
        parcelFileDescriptor = null
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(minScale, min(scaleFactor, maxScale))
            renderCurrentPage()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            scaleFactor = if (scaleFactor > 1.0f) 1.0f else 2.0f
            renderCurrentPage()
            return true
        }
    }
}