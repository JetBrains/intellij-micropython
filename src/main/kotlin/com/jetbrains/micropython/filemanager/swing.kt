package com.jetbrains.intellij.fileManager

import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Swing : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = ApplicationManager.getApplication().invokeLater(block)

    @ExperimentalCoroutinesApi
    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return !SwingUtilities.isEventDispatchThread()
    }
}

suspend fun <T> runInBackground(action: () -> T): T = suspendCoroutine { c ->
    ApplicationManager.getApplication().executeOnPooledThread {
        try {
            c.resume(action())
        } catch (e: Throwable) {
            c.resumeWithException(e)
        }
    }
}