package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.VerificationResult
import com.example.data.model.reliabilityLabel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateAndSharePdfReport(context: Context, result: VerificationResult) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#14102B")
                textSize = 22f
                isFakeBoldText = true
            }
            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#444444")
                textSize = 11f
            }
            val headerPaint = Paint().apply {
                color = Color.parseColor("#0F62FE")
                textSize = 13f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#212121")
                textSize = 10.5f
            }
            val verdictPaint = Paint().apply {
                textSize = 17f
                isFakeBoldText = true
                color = when (result.verdict.name) {
                    "REAL" -> Color.parseColor("#2E7D32")
                    "FAKE" -> Color.parseColor("#C62828")
                    "POTENTIAL_SCAM" -> Color.parseColor("#D32F2F")
                    "MISLEADING" -> Color.parseColor("#EF6C00")
                    else -> Color.parseColor("#616161")
                }
            }

            var y = 50f
            val xMargin = 40f

            // Top Header Banner
            canvas.drawRect(0f, 0f, 595f, 90f, Paint().apply { color = Color.parseColor("#080B16") })
            canvas.drawText("TruthGuard AI — Verification Certificate", xMargin, 45f, Paint().apply {
                color = Color.WHITE
                textSize = 19f
                isFakeBoldText = true
            })
            canvas.drawText("Official Fact-Check & Scam Analysis Report | National Helpline 1930", xMargin, 68f, Paint().apply {
                color = Color.parseColor("#00E5FF")
                textSize = 10.5f
            })

            y = 120f

            // Date & Ref ID
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
            canvas.drawText("Generated On: ${sdf.format(Date(result.timestamp))}", xMargin, y, subtitlePaint)
            canvas.drawText("Reference ID: TG-${result.id}-${(result.timestamp % 100000)}", 350f, y, subtitlePaint)

            y += 24f
            canvas.drawLine(xMargin, y, 555f, y, Paint().apply { color = Color.LTGRAY })

            y += 28f
            canvas.drawText("ANALYSIS VERDICT:", xMargin, y, headerPaint)
            y += 22f
            canvas.drawText("${result.verdict.label}  [${result.reliabilityLabel}]", xMargin, y, verdictPaint)

            if (!result.scamType.isNullOrBlank()) {
                y += 20f
                canvas.drawText("Scam Classification: ${result.scamType}", xMargin, y, Paint().apply {
                    color = Color.parseColor("#C62828")
                    textSize = 11.5f
                    isFakeBoldText = true
                })
            }

            y += 30f
            canvas.drawText("VERIFIED CONTENT / CLAIM:", xMargin, y, headerPaint)
            y += 18f
            val claimLines = wrapText(result.claimTitle, 75)
            for (line in claimLines) {
                canvas.drawText(line, xMargin, y, bodyPaint)
                y += 15f
            }

            y += 18f
            canvas.drawText("PLAIN ENGLISH SUMMARY:", xMargin, y, headerPaint)
            y += 18f
            val easyLines = wrapText(result.easyExplanation, 75)
            for (line in easyLines) {
                canvas.drawText(line, xMargin, y, bodyPaint)
                y += 15f
            }

            y += 18f
            canvas.drawText("DETAILED AI REASONING & EVIDENCE:", xMargin, y, headerPaint)
            y += 18f
            val detailLines = wrapText(result.detailedExplanation, 75)
            for (line in detailLines) {
                canvas.drawText(line, xMargin, y, bodyPaint)
                y += 15f
            }

            y += 18f
            canvas.drawText("RECOMMENDED ACTION:", xMargin, y, headerPaint)
            y += 18f
            val recLines = wrapText(result.recommendation, 75)
            for (line in recLines) {
                canvas.drawText(line, xMargin, y, Paint().apply {
                    color = Color.parseColor("#0F62FE")
                    textSize = 10.5f
                    isFakeBoldText = true
                })
                y += 15f
            }

            y += 18f
            canvas.drawText("OFFICIAL SOURCES & CITATIONS:", xMargin, y, headerPaint)
            y += 18f
            for (src in result.trustedSources) {
                canvas.drawText("• $src", xMargin + 8, y, bodyPaint)
                y += 15f
            }

            // Footer
            canvas.drawRect(0f, 790f, 595f, 842f, Paint().apply { color = Color.parseColor("#F5F7FB") })
            canvas.drawText("TruthGuard AI Platform | Indian Cyber Coordination Centre (I4C) | cybercrime.gov.in", xMargin, 816f, Paint().apply {
                color = Color.GRAY
                textSize = 9.5f
            })

            pdfDocument.finishPage(page)

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
            val file = File(dir, "TruthGuard_Verification_${result.id}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            // Share PDF via FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "TruthGuard AI Verification Certificate")
                putExtra(Intent.EXTRA_TEXT, "Here is the verified fact-check certificate for: ${result.claimTitle}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Verification Certificate PDF"))

        } catch (e: Exception) {
            Toast.makeText(context, "Error generating PDF certificate: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.length + word.length + 1 > maxCharsPerLine) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
