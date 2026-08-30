package name.lechners.chessomnia.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import name.lechners.chessomnia.R
import name.lechners.chessomnia.ui.report.BugReportButton

@Composable
fun HomeScreen(
    versionName: String,
    hasResumableGame: Boolean,
    moveCount: Int,
    onResumeGame: () -> Unit,
    onNewGame: () -> Unit,
    onSettings: () -> Unit,
    buildBugReport: (String) -> String,
    modifier: Modifier = Modifier,
) {
    var confirmNewGame by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_wordmark),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxWidth(0.62f).widthIn(max = 480.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(36.dp))

        val buttonWidth = Modifier.fillMaxWidth().widthIn(max = 460.dp)

        // Resume comes first and is the default action: the menu does not end the
        // game, it just leaves it waiting.
        if (hasResumableGame) {
            Button(onClick = onResumeGame, modifier = buttonWidth.height(64.dp)) {
                Text(stringResource(R.string.home_resume), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                pluralStringResource(R.plurals.home_moves_played, moveCount, moveCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { confirmNewGame = true },
                modifier = buttonWidth.height(56.dp),
            ) { Text(stringResource(R.string.home_new_game)) }
        } else {
            Button(onClick = onNewGame, modifier = buttonWidth.height(64.dp)) {
                Text(stringResource(R.string.home_new_game), style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onSettings, modifier = buttonWidth) { Text(stringResource(R.string.home_settings)) }

        Spacer(Modifier.height(28.dp))

        Text(
            stringResource(R.string.home_version, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        BugReportButton(buildReport = buildBugReport)

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.home_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }

    if (confirmNewGame) {
        AlertDialog(
            onDismissRequest = { confirmNewGame = false },
            title = { Text(stringResource(R.string.home_confirm_new_title)) },
            text = {
                Text(pluralStringResource(R.plurals.home_confirm_new_text, moveCount, moveCount))
            },
            confirmButton = {
                Button(onClick = { confirmNewGame = false; onNewGame() }) {
                    Text(stringResource(R.string.home_confirm_new_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewGame = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
