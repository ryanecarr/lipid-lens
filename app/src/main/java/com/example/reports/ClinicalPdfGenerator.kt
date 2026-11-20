package com.example.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ClinicalTargets
import com.example.data.model.DoseLog
import com.example.data.model.LipidAnalytics
import com.example.data.model.LipidPanel
import com.example.data.model.StatinRegimen
import com.example.data.model.DateTimeUtils
import com.example.data.model.DoseFrequency
import com.example.data.model.FeelLevel
import com.example.data.model.SideEffectOption
import com.example.data.remote.ClinicianVisitBrief
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClinicalReportData(
    val rangeTitle: String,
    val generatedAtLabel: String,
    val regimens: List<StatinRegimen>,
    val logs: List<DoseLog>,
    val lipidPanels: List<LipidPanel> = emptyList(),
    val clinicalTargets: ClinicalTargets = ClinicalTargets(),
    val baselinePanel: LipidPanel? = null,
    val takenCount: Int,
    val skippedCount: Int,
    val adherencePercent: Int,
    val moodBreakdown: List<Pair<FeelLevel, Int>>,
    val sideEffectBreakdown: List<Pair<SideEffectOption, Int>>,
    val aiVisitBrief: ClinicianVisitBrief? = null
)

object ClinicalPdfGenerator {
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN_X = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)
    private const val BOTTOM_LIMIT = PAGE_HEIGHT - 54f

    private val sageColor = Color.rgb(44, 107, 91)
    private val sageLight = Color.rgb(220, 239, 232)
    private val inkColor = Color.rgb(23, 38, 33)
    private val mutedColor = Color.rgb(84, 104, 97)
    private val dividerColor = Color.rgb(213, 225, 220)
    private val coralColor = Color.rgb(199, 92, 70)
    private val rowStripe = Color.rgb(247, 250, 248)
    private val briefSurface = Color.rgb(238, 244, 252)
    private val briefAccent = Color.rgb(29, 84, 159)
    private val briefBorder = Color.rgb(208, 218, 232)

    fun createShareablePdfUri(context: Context, data: ClinicalReportData): Uri {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val outFile = File(reportsDir, "LipidLens_Report_$stamp.pdf")
        FileOutputStream(outFile).use { stream ->
            writePdf(data, stream)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun writePdfToUri(context: Context, uri: Uri, data: ClinicalReportData): Boolean {
        return context.contentResolver.openOutputStream(uri)?.use { stream ->
            writePdf(data, stream)
            true
        } ?: false
    }

    private fun writePdf(data: ClinicalReportData, outputStream: OutputStream) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            var canvas = page.canvas
            var y = 40f

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sageLight
                textSize = 10.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sageColor
                textSize = 12.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedColor
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = dividerColor
                strokeWidth = 1f
            }

            fun finishPageAndStartNext() {
                drawFooter(canvas, pageNumber, mutedPaint, linePaint)
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
                y = 44f
            }

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > BOTTOM_LIMIT) {
                    finishPageAndStartNext()
                }
            }

            // Header banner
            fillPaint.color = sageColor
            canvas.drawRoundRect(
                RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 66f),
                10f,
                10f,
                fillPaint
            )
            canvas.drawText("Lipid Lens — Clinical Lipid & Therapy Report", MARGIN_X + 16f, y + 26f, titlePaint)
            canvas.drawText(
                "Scope: ${data.rangeTitle}   |   Generated: ${data.generatedAtLabel}",
                MARGIN_X + 16f,
                y + 46f,
                subtitlePaint
            )
            y += 80f

            data.aiVisitBrief?.let { brief ->
                val textWidth = CONTENT_WIDTH - 28f
                val lipidLines = wrapText("Lipids: ${brief.lipidTrajectory}", bodyPaint, textWidth)
                val therapyLines = wrapText("Therapy: ${brief.therapyAndTolerance}", bodyPaint, textWidth)
                val highlightLines = brief.clinicalHighlights.take(3).flatMap { item ->
                    wrapText("• $item", bodyPaint, textWidth)
                }
                val topicLines = brief.discussionTopics.take(2).flatMap { item ->
                    wrapText("• $item", bodyPaint, textWidth)
                }
                val disclaimerLines = wrapText(
                    "AI-assisted synthesis of patient-entered records; verify with primary labs. Not medical advice.",
                    mutedPaint,
                    textWidth
                )

                var cardHeight = 28f // header row
                cardHeight += lipidLines.size * 12.5f + 4f
                cardHeight += therapyLines.size * 12.5f + 6f
                if (highlightLines.isNotEmpty()) {
                    cardHeight += 13f + highlightLines.size * 12f + 5f
                }
                if (topicLines.isNotEmpty()) {
                    cardHeight += 13f + topicLines.size * 12f + 5f
                }
                cardHeight += disclaimerLines.size * 11f + 10f

                ensureSpace(cardHeight + 14f)

                val cardRect = RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + cardHeight)
                fillPaint.color = briefSurface
                canvas.drawRoundRect(cardRect, 8f, 8f, fillPaint)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = briefBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

                fillPaint.color = briefAccent
                canvas.drawRoundRect(
                    RectF(MARGIN_X, y, MARGIN_X + 4f, y + cardHeight),
                    4f,
                    4f,
                    fillPaint
                )

                val briefTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = briefAccent
                    textSize = 9.5f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                var by = y + 17f
                val bx = MARGIN_X + 14f
                canvas.drawText("CLINICAL VISIT BRIEF (AI-ASSISTED SYNTHESIS)", bx, by, briefTitlePaint)
                by += 14f

                lipidLines.forEach { line ->
                    canvas.drawText(line, bx, by, bodyPaint)
                    by += 12.5f
                }
                by += 4f
                therapyLines.forEach { line ->
                    canvas.drawText(line, bx, by, bodyPaint)
                    by += 12.5f
                }
                by += 6f

                if (highlightLines.isNotEmpty()) {
                    canvas.drawText("Key Takeaways:", bx, by, boldBodyPaint)
                    by += 13f
                    highlightLines.forEach { line ->
                        canvas.drawText(line, bx, by, bodyPaint)
                        by += 12f
                    }
                    by += 5f
                }

                if (topicLines.isNotEmpty()) {
                    canvas.drawText("For Visit Discussion:", bx, by, boldBodyPaint)
                    by += 13f
                    topicLines.forEach { line ->
                        canvas.drawText(line, bx, by, bodyPaint)
                        by += 12f
                    }
                    by += 5f
                }

                disclaimerLines.forEach { line ->
                    canvas.drawText(line, bx, by, mutedPaint)
                    by += 11f
                }

                y += cardHeight + 16f
            }

            // 1. Adherence & Wellness Summary
            ensureSpace(78f)
            canvas.drawText("1. Adherence & Tolerance Summary", MARGIN_X, y, sectionPaint)
            y += 10f
            fillPaint.color = rowStripe
            canvas.drawRoundRect(
                RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 54f),
                8f,
                8f,
                fillPaint
            )
            val colW = CONTENT_WIDTH / 4f
            drawStatBox(canvas, MARGIN_X + 12f, y + 20f, "ADHERENCE", "${data.adherencePercent}%", boldBodyPaint, mutedPaint)
            drawStatBox(canvas, MARGIN_X + colW + 12f, y + 20f, "DOSES TAKEN", "${data.takenCount}", boldBodyPaint, mutedPaint)
            drawStatBox(canvas, MARGIN_X + colW * 2 + 12f, y + 20f, "DOSES SKIPPED", "${data.skippedCount}", boldBodyPaint, mutedPaint)
            drawStatBox(canvas, MARGIN_X + colW * 3 + 12f, y + 20f, "TOTAL ENTRIES", "${data.logs.size}", boldBodyPaint, mutedPaint)
            y += 72f

            // 2. Lipid-Lowering Regimens & Switch Timeline
            ensureSpace(70f)
            canvas.drawText("2. Lipid-Lowering Regimens & Switch History", MARGIN_X, y, sectionPaint)
            y += 14f
            if (data.regimens.isEmpty()) {
                canvas.drawText("No lipid-lowering regimens recorded.", MARGIN_X, y, mutedPaint)
                y += 18f
            } else {
                data.regimens.forEach { regimen ->
                    ensureSpace(34f)
                    val statusLabel = if (regimen.active) "ACTIVE" else "PRIOR"
                    val roleLabel = if (regimen.normalizedRole() == StatinRegimen.THERAPY_ROLE_ADJUNCT) "ADJUNCT" else "PRIMARY"
                    val brandSuffix = if (regimen.brandName.isNotBlank()) " (${regimen.brandName})" else ""
                    val freqLabel = DoseFrequency.fromKey(regimen.frequencyKey).shortLabel
                    val endLabel = regimen.endedAtMillis?.let { DateTimeUtils.formatDateMillis(it) } ?: "Present"
                    val headerLine = "[$statusLabel • $roleLabel] ${regimen.statinName}$brandSuffix — ${regimen.doseMg} mg • $freqLabel @ ${DateTimeUtils.formatMinutesOfDay(regimen.scheduledTimeMinutes)}"
                    canvas.drawText(headerLine, MARGIN_X, y, boldBodyPaint)
                    y += 13f
                    val reasonSuffix = if (regimen.switchReason.isNotBlank()) " • Switch note: ${regimen.switchReason}" else ""
                    canvas.drawText(
                        "Period: ${DateTimeUtils.formatDateMillis(regimen.startedAtMillis)} to $endLabel$reasonSuffix",
                        MARGIN_X + 10f,
                        y,
                        mutedPaint
                    )
                    y += 16f
                }
            }
            y += 6f

            // 3. Mood & Side-Effect Frequency
            ensureSpace(80f)
            canvas.drawText("3. Reported Side Effects & Daily Feel", MARGIN_X, y, sectionPaint)
            y += 14f
            val sideEffectText = if (data.sideEffectBreakdown.isEmpty()) {
                "No side effects reported during this period."
            } else {
                data.sideEffectBreakdown.joinToString("   •   ") { (effect, count) ->
                    "${effect.label}: $count"
                }
            }
            val sideEffectLines = wrapText(sideEffectText, bodyPaint, CONTENT_WIDTH)
            sideEffectLines.forEach { line ->
                ensureSpace(14f)
                canvas.drawText(line, MARGIN_X, y, bodyPaint)
                y += 13f
            }

            val moodText = if (data.moodBreakdown.isEmpty()) {
                "Mood ratings: None recorded."
            } else {
                "Mood ratings: " + data.moodBreakdown.joinToString("  |  ") { (feel, count) ->
                    "${feel.label}: $count"
                }
            }
            wrapText(moodText, mutedPaint, CONTENT_WIDTH).forEach { line ->
                ensureSpace(14f)
                canvas.drawText(line, MARGIN_X, y, mutedPaint)
                y += 13f
            }
            y += 10f

            // 4. Blood Work / Lipid Panels (if recorded)
            if (data.lipidPanels.isNotEmpty()) {
                ensureSpace(78f)
                canvas.drawText("4. Lipid Panel History & Targets (mg/dL)", MARGIN_X, y, sectionPaint)
                y += 14f

                val targets = data.clinicalTargets
                val targetLine = buildString {
                    append("Prescribed Targets (${targets.riskLabel}): LDL-C <${targets.ldlTargetMgDl}")
                    targets.nonHdlTargetMgDl?.let { append("  |  Non-HDL-C <$it") }
                    targets.apoBTargetMgDl?.let { append("  |  ApoB <$it") }
                    if (targets.clinicianNote.isNotBlank()) append("  •  ${targets.clinicianNote}")
                }
                wrapText(targetLine, mutedPaint, CONTENT_WIDTH).forEach { line ->
                    ensureSpace(12f)
                    canvas.drawText(line, MARGIN_X, y, mutedPaint)
                    y += 12f
                }

                val baseline = data.baselinePanel
                val latest = data.lipidPanels.firstOrNull()
                if (baseline != null && latest != null && baseline.id != latest.id) {
                    val ldlPct = LipidAnalytics.percentDelta(baseline.ldlMgDl, latest.ldlMgDl)
                    val pctText = ldlPct?.let { "${if (it > 0) "+" else ""}$it%" } ?: "n/a"
                    ensureSpace(14f)
                    canvas.drawText(
                        "Change vs Baseline (${DateTimeUtils.formatDateKeyShort(baseline.drawDateKey)} -> ${DateTimeUtils.formatDateKeyShort(latest.drawDateKey)}): LDL-C ${baseline.ldlMgDl} -> ${latest.ldlMgDl} mg/dL ($pctText)",
                        MARGIN_X,
                        y,
                        boldBodyPaint
                    )
                    y += 14f
                } else {
                    y += 3f
                }

                data.lipidPanels.take(8).forEach { panel ->
                    ensureSpace(28f)
                    val nonHdl = LipidAnalytics.effectiveNonHdl(panel)
                    val fastingLabel = if (panel.fasting) "Fasting" else "Non-fasting"
                    val baselineTag = if (panel.baselinePanel || panel.id == baseline?.id) " [BASELINE]" else ""
                    val optText = buildString {
                        if (panel.apoBMgDl != null) append("  |  ApoB ${panel.apoBMgDl}")
                        if (panel.lpAMgDl != null) append("  |  Lp(a) ${panel.lpAMgDl}")
                    }
                    canvas.drawText(
                        "${DateTimeUtils.formatDateKeyShort(panel.drawDateKey)} ($fastingLabel)$baselineTag — LDL-C ${panel.ldlMgDl}, Total ${panel.totalCholesterolMgDl}, HDL-C ${panel.hdlMgDl}, TG ${panel.triglyceridesMgDl}, Non-HDL $nonHdl$optText",
                        MARGIN_X,
                        y,
                        bodyPaint
                    )
                    y += 13f
                    if (panel.labName.isNotBlank() || panel.notes.isNotBlank()) {
                        val sub = listOfNotNull(
                            panel.labName.takeIf { it.isNotBlank() },
                            panel.notes.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                        wrapText(sub, mutedPaint, CONTENT_WIDTH - 10f).forEach { line ->
                            ensureSpace(12f)
                            canvas.drawText(line, MARGIN_X + 10f, y, mutedPaint)
                            y += 12f
                        }
                    }
                }
                y += 8f
            }

            // 5. Chronological Daily Log Table
            ensureSpace(50f)
            val logSectionNum = if (data.lipidPanels.isNotEmpty()) "5" else "4"
            canvas.drawText("$logSectionNum. Daily Medication & Symptom Log", MARGIN_X, y, sectionPaint)
            y += 12f
            drawTableHeader(canvas, y, fillPaint, boldBodyPaint)
            y += 22f

            if (data.logs.isEmpty()) {
                canvas.drawText("No dose logs available for the selected range.", MARGIN_X, y + 6f, mutedPaint)
            } else {
                data.logs.forEachIndexed { idx, log ->
                    val feel = FeelLevel.fromKey(log.feelLevelKey).label
                    val effects = SideEffectOption.parseCsv(log.sideEffectsCsv)
                        .joinToString(", ") { it.label }
                        .ifBlank { "None" }
                    val notePart = if (log.notes.isNotBlank()) " — Note: ${log.notes}" else ""
                    val details = "$effects$notePart"
                    val detailLines = wrapText(details, bodyPaint, 215f)
                    val rowHeight = maxOf(20f, detailLines.size * 12f + 8f)

                    if (y + rowHeight > BOTTOM_LIMIT) {
                        finishPageAndStartNext()
                        drawTableHeader(canvas, y, fillPaint, boldBodyPaint)
                        y += 22f
                    }

                    if (idx % 2 == 1) {
                        fillPaint.color = rowStripe
                        canvas.drawRect(MARGIN_X, y - 4f, PAGE_WIDTH - MARGIN_X, y + rowHeight - 4f, fillPaint)
                    }

                    val dateLabel = DateTimeUtils.formatDateKeyShort(log.logDateKey)
                    val timeLabel = DateTimeUtils.formatMinutesOfDay(log.takenAtMinutes)
                    val medLabel = "${log.statinName} ${log.doseMg}mg"
                    val statusLabel = if (log.taken) "Taken" else "Skipped"

                    canvas.drawText(dateLabel, MARGIN_X + 4f, y + 9f, bodyPaint)
                    canvas.drawText(timeLabel, MARGIN_X + 74f, y + 9f, bodyPaint)
                    canvas.drawText(medLabel.take(18), MARGIN_X + 132f, y + 9f, bodyPaint)

                    val statusPaint = Paint(boldBodyPaint).apply {
                        color = if (log.taken) sageColor else coralColor
                    }
                    canvas.drawText(statusLabel, MARGIN_X + 234f, y + 9f, statusPaint)
                    canvas.drawText(feel, MARGIN_X + 282f, y + 9f, bodyPaint)

                    var lineY = y + 9f
                    detailLines.forEach { dLine ->
                        canvas.drawText(dLine, MARGIN_X + 332f, lineY, bodyPaint)
                        lineY += 12f
                    }

                    y += rowHeight
                    canvas.drawLine(MARGIN_X, y - 4f, PAGE_WIDTH - MARGIN_X, y - 4f, linePaint)
                }
            }

            drawFooter(canvas, pageNumber, mutedPaint, linePaint)
            document.finishPage(page)
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun drawStatBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        label: String,
        value: String,
        valuePaint: Paint,
        labelPaint: Paint
    ) {
        val bigPaint = Paint(valuePaint).apply { textSize = 15f }
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(value, x, y + 20f, bigPaint)
    }

    private fun drawTableHeader(
        canvas: Canvas,
        y: Float,
        fillPaint: Paint,
        headerPaint: Paint
    ) {
        fillPaint.color = sageLight
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 18f, fillPaint)
        val textY = y + 13f
        canvas.drawText("Date", MARGIN_X + 4f, textY, headerPaint)
        canvas.drawText("Time", MARGIN_X + 74f, textY, headerPaint)
        canvas.drawText("Medication", MARGIN_X + 132f, textY, headerPaint)
        canvas.drawText("Status", MARGIN_X + 234f, textY, headerPaint)
        canvas.drawText("Feel", MARGIN_X + 282f, textY, headerPaint)
        canvas.drawText("Side Effects & Notes", MARGIN_X + 332f, textY, headerPaint)
    }

    private fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        mutedPaint: Paint,
        linePaint: Paint
    ) {
        val footerY = PAGE_HEIGHT - 28f
        canvas.drawLine(MARGIN_X, footerY - 12f, PAGE_WIDTH - MARGIN_X, footerY - 12f, linePaint)
        canvas.drawText(
            "Patient-reported lipid & therapy log • Generated by Lipid Lens",
            MARGIN_X,
            footerY,
            mutedPaint
        )
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN_X - 42f, footerY, mutedPaint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
