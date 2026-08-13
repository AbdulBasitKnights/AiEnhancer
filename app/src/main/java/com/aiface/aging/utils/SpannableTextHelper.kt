package com.aiface.aging.utils

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

data class TextLineStyle(
    @ColorInt val color: Int,
    val textSizeSp: Float,
    @FontRes val fontRes: Int? = null,
    val bold: Boolean = false,
)

object SpannableTextHelper {

    fun buildMultiLineText(
        context: Context,
        lines: List<String>,
        styles: List<TextLineStyle>,
    ): SpannableString {
        require(lines.size == styles.size) { "lines and styles count must match" }
        val fullText = lines.joinToString("\n")
        val spannable = SpannableString(fullText)

        var cursor = 0
        lines.forEachIndexed { index, line ->
            val style = styles[index]
            val start = cursor
            val end = start + line.length

            spannable.setSpan(
                ForegroundColorSpan(style.color),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            spannable.setSpan(
                AbsoluteSizeSpan(spToPx(context, style.textSizeSp)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )

            style.fontRes?.let { fontRes ->
                ResourcesCompat.getFont(context, fontRes)?.let { typeface ->
                    spannable.setSpan(
                        CustomTypefaceSpan(typeface),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            } ?: run {
                if (style.bold) {
                    spannable.setSpan(
                        android.text.style.StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }

            cursor = end + 1
        }

        return spannable
    }

    fun twoLineText(
        context: Context,
        topLine: String,
        bottomLine: String,
        @ColorInt topColor: Int,
        @ColorInt bottomColor: Int,
        topTextSizeSp: Float = 13f,
        bottomTextSizeSp: Float = 11f,
        @FontRes topFontRes: Int? = null,
        @FontRes bottomFontRes: Int? = null,
    ): SpannableString {
        return buildMultiLineText(
            context = context,
            lines = listOf(topLine, bottomLine),
            styles = listOf(
                TextLineStyle(
                    color = topColor,
                    textSizeSp = topTextSizeSp,
                    fontRes = topFontRes,
                    bold = topFontRes == null,
                ),
                TextLineStyle(
                    color = bottomColor,
                    textSizeSp = bottomTextSizeSp,
                    fontRes = bottomFontRes,
                ),
            ),
        )
    }

    fun TextView.setTwoLineStyledText(
        topLine: String,
        bottomLine: String,
        @ColorInt topColor: Int = ContextCompat.getColor(context, com.aiface.aging.R.color.text_primary),
        @ColorInt bottomColor: Int = ContextCompat.getColor(context, com.aiface.aging.R.color.text_secondary),
        topTextSizeSp: Float = 13f,
        bottomTextSizeSp: Float = 11f,
        @FontRes topFontRes: Int = com.aiface.aging.R.font.inter_semibold,
        @FontRes bottomFontRes: Int = com.aiface.aging.R.font.inter_regular,
    ) {
        text = twoLineText(
            context = context,
            topLine = topLine,
            bottomLine = bottomLine,
            topColor = topColor,
            bottomColor = bottomColor,
            topTextSizeSp = topTextSizeSp,
            bottomTextSizeSp = bottomTextSizeSp,
            topFontRes = topFontRes,
            bottomFontRes = bottomFontRes,
        )
    }

    private fun spToPx(context: Context, sp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics,
        ).toInt()
    }
}

private class CustomTypefaceSpan(private val typeface: Typeface) : TypefaceSpan("") {

    override fun updateDrawState(textPaint: TextPaint) {
        applyTypeface(textPaint, typeface)
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        applyTypeface(textPaint, typeface)
    }

    private fun applyTypeface(paint: TextPaint, typeface: Typeface) {
        val oldTypeface = paint.typeface
        val oldStyle = oldTypeface?.style ?: 0
        val fakeStyle = oldStyle and typeface.style.inv()
        if (fakeStyle and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }
        if (fakeStyle and Typeface.ITALIC != 0) {
            paint.textSkewX = -0.25f
        }
        paint.typeface = typeface
    }
}
