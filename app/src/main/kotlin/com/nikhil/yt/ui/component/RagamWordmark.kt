package com.nikhil.yt.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nikhil.yt.R

private val RagamBrandFontFamily = FontFamily(
    Font(R.font.sfprodisplaybold, FontWeight.Bold),
    Font(R.font.sfprodisplaybold, FontWeight.ExtraBold),
    Font(R.font.sfprodisplaybold, FontWeight.Black)
)

@Composable
fun RagamWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = "RAGAM",
        modifier = modifier,
        style = TextStyle(
            fontFamily = RagamBrandFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = fontSize,
            letterSpacing = 2.sp,
            color = color
        )
    )
}
