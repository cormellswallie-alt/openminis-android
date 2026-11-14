package com.openminis.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.openminis.app.R

/**
 * Material 3 Expressive Confirmation Dialog.
 *
 * Uses a 28dp `surfaceContainerHigh` container, expressive bold headline
 * typography, and filled / tonal pill actions for clear visual hierarchy.
 */
@Composable
fun MinisAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    text: String? = null,
    dismissText: String = stringResource(R.string.cancel),
    isDestructive: Boolean = false,
    onDismiss: () -> Unit = onDismissRequest,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (text != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                val confirmButtonColors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }

                if (neutralText != null && onNeutral != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MinisButton(
                            onClick = onNeutral,
                            colors = confirmButtonColors,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(neutralText, fontWeight = FontWeight.SemiBold)
                        }
                        MinisTonalButton(
                            onClick = onConfirm,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(confirmText, fontWeight = FontWeight.SemiBold)
                        }
                        MinisTextButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(dismissText)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (dismissText.isNotEmpty()) {
                            MinisTextButton(onClick = onDismiss) {
                                Text(dismissText)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        MinisButton(
                            onClick = onConfirm,
                            colors = confirmButtonColors,
                        ) {
                            Text(confirmText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
