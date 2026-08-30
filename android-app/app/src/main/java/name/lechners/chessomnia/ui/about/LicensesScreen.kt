package name.lechners.chessomnia.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import name.lechners.chessomnia.R

/**
 * Displays res/raw/licenses.txt - the attribution for the chess pieces
 * (BSD 3-Clause), the Montserrat typeface (OFL 1.1) and the Apache-2.0 libraries
 * in use.
 *
 * WARNING: reading R.raw.licenses is not merely display, it is the reason the file
 * ships at all. `isShrinkResources = true` removes every resource that no code
 * refers to. Before this screen existed, the file was in *no* released APK -
 * provable as "@raw/licenses : reachable=false" in
 * build/outputs/mapping/release/resources.txt. res/raw/keep.xml is a second
 * safeguard, but it does not replace this screen: a file that ships but cannot be
 * reached satisfies no attribution requirement.
 */
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val text = remember {
        resources.openRawResource(R.raw.licenses)
            .bufferedReader()
            .use { it.readText() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.licenses_back)) }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.licenses_title), style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(16.dp))

        // Monospace, because the file is laid out with underlines and indentation -
        // in a proportional face that formatting falls apart.
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
    }
}
