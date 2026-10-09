/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    subtitle: String? = null,
    actionText: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val ragamOrange = Color(0xFFEF4903)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(
                enabled = onClick != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick?.invoke()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            thumbnail?.invoke()

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                label?.let { labelText ->
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFAAAAAA),
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )

                subtitle?.let { subtitleText ->
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFAAAAAA),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }

        if (onClick != null) {
            Text(
                text = actionText ?: "Explore all",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ragamOrange
            )
        }
    }
}

@Composable
fun NavigationTitle(
    title: androidx.compose.ui.text.AnnotatedString,
    modifier: Modifier = Modifier,
    label: String? = null,
    subtitle: String? = null,
    actionText: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val ragamOrange = Color(0xFFEF4903)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(
                enabled = onClick != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick?.invoke()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            thumbnail?.invoke()

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                label?.let { labelText ->
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFAAAAAA),
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )

                subtitle?.let { subtitleText ->
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFAAAAAA),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }

        if (onClick != null) {
            Text(
                text = actionText ?: "Explore all",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ragamOrange
            )
        }
    }
}
