package name.lechners.chessomnia.ui.report

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.lechners.chessomnia.R

/**
 * "Report a problem": asks for a description and pushes the finished report into any
 * other app via Android's share sheet.
 *
 * Deliberately no transport of its own (no upload, no mail): the report is plain text,
 * and where it goes is the user's decision in the share dialog.
 *
 * [buildReport] receives the description and returns the complete text, so the game is
 * collected when it is shared, not already when the dialog opens.
 */
@Composable
fun BugReportButton(
    buildReport: (String) -> String,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    var open by remember { mutableStateOf(false) }

    TextButton(onClick = { open = true }, modifier = modifier) {
        Text(label ?: stringResource(R.string.bugreport_button))
    }

    if (open) {
        BugReportDialog(
            onDismiss = { open = false },
            onShare = { open = false },
            buildReport = buildReport,
        )
    }
}

@Composable
private fun BugReportDialog(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    buildReport: (String) -> String,
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    val subject = stringResource(R.string.bugreport_subject)
    val chooserTitle = stringResource(R.string.bugreport_chooser)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bugreport_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.bugreport_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                    placeholder = { Text(stringResource(R.string.bugreport_placeholder)) },
                )
                Spacer(Modifier.height(12.dp))
                // Honesty about what leaves the device.
                Text(
                    stringResource(R.string.bugreport_included),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val text = buildReport(description)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, chooserTitle))
                onShare()
            }) { Text(stringResource(R.string.bugreport_share)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
