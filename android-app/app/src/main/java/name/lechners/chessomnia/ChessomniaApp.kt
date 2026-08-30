package name.lechners.chessomnia

import android.app.Application
import android.os.Build
import name.lechners.chessomnia.data.AppInfo
import name.lechners.chessomnia.data.ChessomniaPrefs

class ChessomniaApp : Application() {

    lateinit var prefs: ChessomniaPrefs
        private set

    lateinit var appInfo: AppInfo
        private set

    override fun onCreate() {
        super.onCreate()
        // A dependency injection framework would cost more than it returns for two
        // dependencies.
        prefs = ChessomniaPrefs(this)

        val pkg = packageManager.getPackageInfo(packageName, 0)
        appInfo = AppInfo(
            versionName = pkg.versionName ?: "?",
            versionCode = pkg.longVersionCode,
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidRelease = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
        )
    }
}
