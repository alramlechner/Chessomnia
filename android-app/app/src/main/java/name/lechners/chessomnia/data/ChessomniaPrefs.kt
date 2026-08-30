package name.lechners.chessomnia.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import name.lechners.chessomnia.rules.Side

/**
 * Settings and the game in progress.
 *
 * SharedPreferences rather than DataStore, on purpose: this is read SYNCHRONOUSLY at
 * startup. The ViewModel can therefore restore the position in its constructor and the
 * very first frame already shows the game. DataStore is flow-based and would force an
 * empty board or a loading state for one frame, without gaining anything at this data
 * volume. `apply()` also writes off-thread, so saving after every move carries no ANR
 * risk.
 */
class ChessomniaPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("chessomnia", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _settings: MutableStateFlow<Settings>
    val settings: StateFlow<Settings> get() = _settings.asStateFlow()

    init {
        migrate()
        _settings = MutableStateFlow(readSettings())
    }

    /**
     * One-off adjustments to settings that are already stored.
     *
     * Needed because a changed default has no effect for anyone who has touched the
     * settings before - for them the old value is already in the preferences.
     */
    private fun migrate() {
        val version = prefs.getInt(KEY_SETTINGS_VERSION, 1)
        if (version < 2) {
            // v2 once converted LOCKED to PORTRAIT. Moot since the orientation is no
            // longer a setting (v3) - the step stays as a placeholder so that the chain
            // and its version numbering remain gap-free.
        }
        if (version < 3) {
            // v3: the "screen orientation" setting is gone. From targetSdk 36 Android
            // ignores screenOrientation on displays of 600dp and wider anyway - that is,
            // on tablets, the target device. Clean up the orphaned key.
            prefs.edit().remove(KEY_ORIENTATION).apply()
        }
        prefs.edit().putInt(KEY_SETTINGS_VERSION, SETTINGS_VERSION).apply()
    }

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        writeSettings(next)
        _settings.value = next
    }

    // ── Settings ────────────────────────────────────────────────────────────────

    private fun readSettings() = Settings(
        clockEnabled = prefs.getBoolean(KEY_CLOCK_ENABLED, true),
        showHints = prefs.getBoolean(KEY_HINTS, true),
        showCoordinates = prefs.getBoolean(KEY_COORDS, true),
        boardBottomSide = if (prefs.getBoolean(KEY_BOTTOM_IS_WHITE, true)) Side.WHITE else Side.BLACK,
        confirmTakeback = prefs.getBoolean(KEY_CONFIRM_TAKEBACK, true),
        keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
    )

    private fun writeSettings(s: Settings) {
        prefs.edit()
            .putBoolean(KEY_CLOCK_ENABLED, s.clockEnabled)
            .putBoolean(KEY_HINTS, s.showHints)
            .putBoolean(KEY_COORDS, s.showCoordinates)
            .putBoolean(KEY_BOTTOM_IS_WHITE, s.boardBottomSide == Side.WHITE)
            .putBoolean(KEY_CONFIRM_TAKEBACK, s.confirmTakeback)
            .putBoolean(KEY_KEEP_SCREEN_ON, s.keepScreenOn)
            .apply()
    }

    // ── Game in progress ────────────────────────────────────────────────────────

    fun saveGame(snapshot: GameSnapshot?) {
        val editor = prefs.edit()
        if (snapshot == null) editor.remove(KEY_GAME)
        else editor.putString(KEY_GAME, json.encodeToString(GameSnapshot.serializer(), snapshot))
        editor.apply()
    }

    /** An unreadable save must not block startup - then there is simply no game. */
    fun loadGame(): GameSnapshot? {
        val raw = prefs.getString(KEY_GAME, null) ?: return null
        return runCatching { json.decodeFromString(GameSnapshot.serializer(), raw) }.getOrNull()
    }

    private companion object {
        const val KEY_CLOCK_ENABLED = "clock_enabled"
        const val KEY_HINTS = "show_hints"
        const val KEY_COORDS = "show_coordinates"
        const val KEY_BOTTOM_IS_WHITE = "board_bottom_is_white"
        const val KEY_CONFIRM_TAKEBACK = "confirm_takeback"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        /** Only still here for migration v3 (the cleanup). */
        const val KEY_ORIENTATION = "screen_orientation"
        const val KEY_GAME = "current_game"
        const val KEY_SETTINGS_VERSION = "settings_version"
        const val SETTINGS_VERSION = 3
    }
}
