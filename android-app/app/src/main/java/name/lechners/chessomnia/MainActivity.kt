package name.lechners.chessomnia

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import name.lechners.chessomnia.ui.about.LicensesScreen
import name.lechners.chessomnia.ui.game.GameScreen
import name.lechners.chessomnia.ui.game.GameViewModel
import name.lechners.chessomnia.ui.home.HomeScreen
import name.lechners.chessomnia.ui.settings.SettingsScreen
import name.lechners.chessomnia.ui.theme.ChessomniaTheme

private enum class Screen { HOME, GAME, SETTINGS, LICENSES }

class MainActivity : ComponentActivity() {

    private val app: ChessomniaApp get() = application as ChessomniaApp
    private val gameViewModel: GameViewModel by viewModels { GameViewModel.Factory(app.prefs, app.appInfo) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 36 enforces edge-to-edge; without safeDrawingPadding content
        // slides underneath the status and navigation bars.
        //
        // The app does NOT pin an orientation. From targetSdk 36 Android ignores
        // screenOrientation on displays of 600dp and wider - that is, on tablets, the
        // target device. The layout was built for both orientations anyway: the board
        // square in the middle, the panels along the two edges where somebody sits.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ChessomniaTheme {
                val settings by app.prefs.settings.collectAsState()
                val gameState by gameViewModel.ui.collectAsState()
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

                if (screen == Screen.GAME && settings.keepScreenOn) {
                    DisposableEffect(Unit) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                    }
                }

                // Back leads one step up the screens instead of out of the app.
                // Without this the app has exactly one back behaviour - quit - which is
                // not what anyone expects from a settings or game screen.
                BackHandler(enabled = screen != Screen.HOME) {
                    screen = if (screen == Screen.LICENSES) Screen.SETTINGS else Screen.HOME
                }

                Surface(Modifier.fillMaxSize()) {
                    when (screen) {
                        // Switching to the menu does NOT end the game - the ViewModel
                        // holds it, and it is persisted on top of that.
                        Screen.HOME -> HomeScreen(
                            versionName = app.appInfo.versionName,
                            hasResumableGame = gameViewModel.hasResumableGame,
                            moveCount = gameState.moveCount,
                            onResumeGame = { screen = Screen.GAME },
                            onNewGame = { gameViewModel.newGame(); screen = Screen.GAME },
                            onSettings = { screen = Screen.SETTINGS },
                            buildBugReport = gameViewModel::buildBugReport,
                            modifier = Modifier.safeDrawingPadding(),
                        )

                        Screen.GAME -> GameScreen(
                            vm = gameViewModel,
                            onExit = { screen = Screen.HOME },
                            modifier = Modifier.safeDrawingPadding(),
                        )

                        Screen.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onChange = { transform -> app.prefs.update(transform) },
                            buildBugReport = gameViewModel::buildBugReport,
                            onLicenses = { screen = Screen.LICENSES },
                            onBack = { screen = Screen.HOME },
                            modifier = Modifier.safeDrawingPadding(),
                        )

                        Screen.LICENSES -> LicensesScreen(
                            onBack = { screen = Screen.SETTINGS },
                            modifier = Modifier.safeDrawingPadding(),
                        )
                    }
                }
            }
        }
    }
}
