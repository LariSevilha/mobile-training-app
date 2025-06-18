package com.example.trainingappmobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class PdfRenderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0

    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null

    init {


            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }

            try {
            } catch (e: Exception) {
                Log.e("PdfRenderView", "Erro ao carregar PDF: ${e.message}", e)
                    val errorText = TextView(context).apply {
                        gravity = android.view.Gravity.CENTER
                        textSize = 16f
                    }
                    addView(errorText)
    }

    fun close() {
            currentPage?.close()
            pdfRenderer?.close()
        }
    }

    }
}