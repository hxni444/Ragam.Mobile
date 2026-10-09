package com.nikhil.yt.ui.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikhil.yt.R

private fun launchUpiPaymentDialog(context: Context, upiId: String, payeeName: String) {
    val note = "Support Ragam Mobile"
    val uriString = "upi://pay?pa=$upiId&pn=${Uri.encode(payeeName)}&tn=${Uri.encode(note)}&cu=INR"
    val uri = Uri.parse(uriString)
    val intent = Intent(Intent.ACTION_VIEW, uri)

    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("UPI ID", upiId)
        clipboard.setPrimaryClip(clip)
    } catch (_: Exception) {}

    try {
        val chooser = Intent.createChooser(intent, "Pay with UPI")
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "UPI ID copied to clipboard: $upiId", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarDialog(
    onDismissRequest: () -> Unit,
    onStar: () -> Unit,
    onLater: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF161618),
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "Support development",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "Hey there! I'm Hani, the developer of Ragam. I have been putting a lot of love into making this app better every day. \n\nIf you enjoy using Ragam, you can support its development by giving the project a star on GitHub or buying me a chai with UPI!\n\nThanks a bunch for your support and for being part of this journey!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAAAAA)
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        launchUpiPaymentDialog(context, "hanialziya4@okicici", "Hani")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = 0.22f),
                        RoundedCornerShape(12.dp)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_send),
                        contentDescription = "UPI",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "UPI", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/hxni444/Ragam")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onStar()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.10f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = 0.18f),
                        RoundedCornerShape(12.dp)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.star),
                        contentDescription = "Star",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "Star", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLater,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF9E9E9E)
                )
            ) {
                Text(text = "Later", color = Color(0xFF9E9E9E))
            }
        }
    )
}
