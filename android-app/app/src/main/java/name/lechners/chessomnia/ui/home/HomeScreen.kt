package name.lechners.chessomnia.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import name.lechners.chessomnia.R
import name.lechners.chessomnia.data.Settings
import name.lechners.chessomnia.engine.Level
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.ui.report.BugReportButton

@Composable
fun HomeScreen(
    versionName: String,
    hasResumableGame: Boolean,
    moveCount: Int,
    settings: Settings,
    onResumeGame: () -> Unit,
    onNewGame: () -> Unit,
    onPlayDevice: (Level, Side) -> Unit,
    onSettings: () -> Unit,
    buildBugReport: (String) -> String,
    modifier: Modifier = Modifier,
) {
    // What to start once the running game has been given up. Null while nothing is
    // pending - both start buttons discard the same thing, so both go through here.
    var pendingStart by remember { mutableStateOf<PendingStart?>(null) }
    var chooseOpponent by remember { mutableStateOf(false) }

    val startTwoPlayers = {
        if (hasResumableGame) pendingStart = PendingStart.TWO_PLAYERS else onNewGame()
    }
    val startAgainstDevice = {
        if (hasResumableGame) pendingStart = PendingStart.AGAINST_DEVICE else chooseOpponent = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Decorative: the wordmark right below carries the name, so announcing the
        // mark as well would just repeat it.
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier.height(76.dp),
        )
        Spacer(Modifier.height(20.dp))
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
                onClick = startTwoPlayers,
                modifier = buttonWidth.height(56.dp),
            ) { Text(stringResource(R.string.home_new_game)) }
        } else {
            Button(onClick = startTwoPlayers, modifier = buttonWidth.height(64.dp)) {
                Text(stringResource(R.string.home_new_game), style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = startAgainstDevice,
            modifier = buttonWidth.height(56.dp),
        ) { Text(stringResource(R.string.home_play_device)) }

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

    if (chooseOpponent) {
        OpponentDialog(
            initialLevel = settings.opponentLevel,
            initialHumanPlaysWhite = settings.opponentHumanPlaysWhite,
            onDismiss = { chooseOpponent = false },
            onStart = { level, humanSide ->
                chooseOpponent = false
                onPlayDevice(level, humanSide)
            },
        )
    }

    // ⚠️ Both ways of starting discard the running game, so both ask. Playing the device
    // used to start straight away and throw the game away without a word, while the
    // button right above it asked - the same loss, warned about only on one path.
    pendingStart?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingStart = null },
            title = { Text(stringResource(R.string.home_confirm_new_title)) },
            text = {
                Text(pluralStringResource(R.plurals.home_confirm_new_text, moveCount, moveCount))
            },
            confirmButton = {
                Button(onClick = {
                    pendingStart = null
                    when (pending) {
                        PendingStart.TWO_PLAYERS -> onNewGame()
                        // The strength and colour are only asked for once the game is
                        // actually being given up - otherwise the setup would be filled
                        // in and then thrown away by a "cancel".
                        PendingStart.AGAINST_DEVICE -> chooseOpponent = true
                    }
                }) {
                    Text(stringResource(R.string.home_confirm_new_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStart = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** Which kind of game is waiting for the running one to be given up. */
private enum class PendingStart { TWO_PLAYERS, AGAINST_DEVICE }

/**
 * Picking a strength and a colour before playing the device.
 *
 * Both choices are remembered, because in practice they are made once and then kept - a
 * dialog that reverts to its defaults every time turns a two-tap start into a four-tap
 * one.
 */
@Composable
private fun OpponentDialog(
    initialLevel: Level,
    initialHumanPlaysWhite: Boolean,
    onDismiss: () -> Unit,
    onStart: (Level, Side) -> Unit,
) {
    var level by remember { mutableStateOf(initialLevel) }
    var humanPlaysWhite by remember { mutableStateOf(initialHumanPlaysWhite) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.opponent_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.opponent_strength),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Level.entries.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().selectable(
                                selected = level == option,
                                onClick = { level = option },
                                role = Role.RadioButton,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = level == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(levelNameRes(option)))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.opponent_colour),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = humanPlaysWhite,
                        onClick = { humanPlaysWhite = true },
                        label = { Text(stringResource(R.string.side_white)) },
                    )
                    FilterChip(
                        selected = !humanPlaysWhite,
                        onClick = { humanPlaysWhite = false },
                        label = { Text(stringResource(R.string.side_black)) },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onStart(level, if (humanPlaysWhite) Side.WHITE else Side.BLACK)
            }) { Text(stringResource(R.string.opponent_start)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@StringRes
private fun levelNameRes(level: Level): Int = when (level) {
    Level.BEGINNER -> R.string.opponent_level_beginner
    Level.CASUAL -> R.string.opponent_level_casual
    Level.CLUB -> R.string.opponent_level_club
}
