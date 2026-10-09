/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.nikhil.yt.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nikhil.yt.R
import com.nikhil.yt.ui.screens.OptionStats

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.08f),
    icons: Map<E, Int> = emptyMap(),
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.width(12.dp))

        chips.forEach { (value, label) ->
            val isSelected = currentValue == value
            val iconRes = icons[value]

            FilterChip(
                selected = isSelected,
                onClick = { onValueUpdate(value) },
                label = { 
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFFD0D0D0)
                    ) 
                },
                leadingIcon = {
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.done),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    } else if (iconRes != null) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = Color(0xFFD0D0D0),
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isSelected) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f)
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    selectedContainerColor = Color.White.copy(alpha = 0.22f),
                    labelColor = Color(0xFFD0D0D0),
                    selectedLabelColor = Color.White,
                    iconColor = Color(0xFFD0D0D0),
                    selectedLeadingIconColor = Color.White
                ),
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <Int> ChoiceChipsRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.08f),
) {
    var expandIconDegree by remember { mutableFloatStateOf(0f) }
    val rotationAnimation by animateFloatAsState(
        targetValue = expandIconDegree,
        animationSpec = tween(durationMillis = 400),
        label = "",
    )

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            AssistChip(
                onClick = {
                    expanded = !expanded
                    expandIconDegree -= 180
                },
                label = {
                    Text(
                        text =
                        when (selectedOption) {
                            OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                            OptionStats.MONTHS -> stringResource(id = R.string.months)
                            OptionStats.YEARS -> stringResource(id = R.string.years)
                            OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                        },
                        color = Color.White
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                    )
                },
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.White.copy(alpha = 0.10f),
                    labelColor = Color.White
                )
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandIn() + fadeIn(),
                exit = shrinkOut() + fadeOut(),
            ) {
                DropdownMenu(
                    modifier = Modifier.padding(start = 12.dp),
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        expandIconDegree -= 180
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.second, color = Color.White) },
                            onClick = {
                                onSelectionChange(option.first)
                                expandIconDegree -= 180
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedOption,
            transitionSpec = { slideInHorizontally() + fadeIn() togetherWith slideOutHorizontally() + fadeOut() },
            label = "",
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                chips.forEach { (value, label) ->
                    Spacer(Modifier.width(8.dp))

                    FilterChip(
                        label = { Text(label, color = if (currentValue == value) Color.White else Color(0xFFD0D0D0)) },
                        selected = currentValue == value,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            selectedContainerColor = Color.White.copy(alpha = 0.22f),
                            labelColor = Color(0xFFD0D0D0),
                            selectedLabelColor = Color.White
                        ),
                        onClick = { onValueUpdate(value) },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (currentValue == value) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    }
}
