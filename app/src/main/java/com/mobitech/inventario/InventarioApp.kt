package com.mobitech.inventario

import android.app.Application
import com.mobitech.inventario.data.local.AppDatabase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class InventarioApp : Application() {
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        AppDatabase.seed(appScope, database)
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = File(filesDir, "inventario/logs").apply { mkdirs() }
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val detailed = buildString {
                    appendLine("THREAD: ${thread.name} (${thread.id})")
                    appendLine("TIME: $ts")
                    appendLine("EXCEPTION: ${throwable::class.java.name}: ${throwable.message}")
                    appendLine("STACKTRACE:")
                    throwable.stackTrace.forEach { appendLine(it.toString()) }
                    throwable.cause?.let { c ->
                        appendLine()
                        appendLine("CAUSE: ${c::class.java.name}: ${c.message}")
                        c.stackTrace.forEach { st -> appendLine(st.toString()) }
                    }
                }
                // Arquivo datado
                File(dir, "crash_$ts.txt").writeText(detailed)
                // Último crash
                File(dir, "last_crash.txt").writeText(detailed)
            } catch (_: Throwable) {
                // Ignora falhas de escrita
            }
            // Encaminha para handler anterior para comportamento padrão (encerra app)
            previous?.uncaughtException(thread, throwable) ?: run {
                // fallback
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }
    }
}
