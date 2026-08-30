package name.lechners.chessomnia.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import name.lechners.chessomnia.R
import name.lechners.chessomnia.data.Settings
import name.lechners.chessomnia.ui.report.BugReportButton

@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    buildBugReport: (String) -> String,
    onLicenses: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Spacer(Modifier.height(20.dp))

        Section(R.string.settings_section_clock) {
            SwitchRow(
                R.string.settings_clock_title,
                R.string.settings_clock_desc,
                settings.clockEnabled,
            ) { on -> onChange { it.copy(clockEnabled = on) } }
        }

        Section(R.string.settings_section_hints) {
            SwitchRow(
                R.string.settings_hints_title,
                R.string.settings_hints_desc,
                settings.showHints,
            ) { on -> onChange { it.copy(showHints = on) } }

            SwitchRow(
                R.string.settings_coords_title,
                R.string.settings_coords_desc,
                settings.showCoordinates,
            ) { on -> onChange { it.copy(showCoordinates = on) } }

            SwitchRow(
                R.string.settings_confirm_takeback_title,
                R.string.settings_confirm_takeback_desc,
                settings.confirmTakeback,
            ) { on -> onChange { it.copy(confirmTakeback = on) } }
        }

        Section(R.string.settings_section_screen) {
            SwitchRow(
                R.string.settings_keep_screen_on_title,
                R.string.settings_keep_screen_on_desc,
                settings.keepScreenOn,
            ) { on -> onChange { it.copy(keepScreenOn = on) } }
        }

        Section(R.string.settings_section_bugreport) {
            Text(
                stringResource(R.string.settings_bugreport_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            BugReportButton(buildReport = buildBugReport)
        }

        Section(R.string.settings_section_about) {
            Text(
                stringResource(R.string.settings_about_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.settings_about_credits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onLicenses, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.settings_about_licenses))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked && enabled, onCheckedChange = onChange, enabled = enabled)
    }
}
