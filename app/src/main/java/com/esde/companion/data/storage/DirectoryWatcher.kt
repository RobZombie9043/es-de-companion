package com.esde.companion.data.storage

import android.os.FileObserver
import java.io.File

/**
 * Seam for the directory-watching mechanism used by [SelfHealingDirectoryWatcher] (and,
 * through it, `EsdeLogFileRepository`/`FileEsdeInstallationRepository`) - injected the same
 * way as those repositories' `bootTimeMillis` parameter, so tests can substitute a no-op
 * fake instead of touching real [FileObserver]. [FileObserver]'s method bodies are stubbed
 * to throw under a plain JUnit unit test (no Robolectric, no Android runtime), so without
 * this seam every collection of a flow that builds one would race the producer coroutine's
 * real `Dispatchers.IO` thread against the test's own cancellation to reach
 * `startWatching()` before it throws - a genuine, observed flake, not a hypothetical one.
 */
interface DirectoryWatcher {
    fun startWatching()

    fun stopWatching()

    /**
     * Re-evaluate whether this watcher should be (re)armed. No-op by default so existing
     * fakes that only implement [startWatching]/[stopWatching] keep compiling unchanged -
     * only [SelfHealingDirectoryWatcher] (and tests that specifically exercise self-healing)
     * need a real implementation.
     */
    fun recheck(forceRearm: Boolean = false) {}
}

/**
 * Watches [targetPath]'s parent directory and invokes [onChange] for any event in [mask]
 * on an entry matching its file name. Watching the parent directory rather than the file
 * itself survives the file being deleted-and-recreated (rather than edited in place) -
 * watching the file directly would tie the inotify watch to its current inode, which goes
 * dead silently the moment that happens.
 */
internal class FileObserverDirectoryWatcher(
    targetPath: String,
    mask: Int,
    onChange: () -> Unit,
) : DirectoryWatcher {
    private val targetFile = File(targetPath)
    private val observer =
        object : FileObserver(targetFile.parentFile ?: File("/"), mask) {
            override fun onEvent(
                event: Int,
                path: String?,
            ) {
                if (path == null || path == targetFile.name) onChange()
            }
        }

    override fun startWatching() = observer.startWatching()

    override fun stopWatching() = observer.stopWatching()
}

/**
 * Wraps a [DirectoryWatcher] (by default [FileObserverDirectoryWatcher]) with self-healing,
 * closing two gaps neither [FileObserver] nor the rest of this codebase previously handled:
 *
 * - **Never arms against a directory that doesn't exist yet.** [FileObserver.startWatching]
 *   does not throw when the underlying `inotify_add_watch` fails (e.g. because the parent
 *   directory - possibly on removable storage that isn't mounted yet - doesn't exist) - it
 *   silently no-ops. Since inotify has no way to "start watching a path once it exists
 *   later," arming in that state would leave the watch permanently dead for no benefit.
 *   Instead, [startWatching] records intent and defers actually arming until [recheck]
 *   confirms the directory exists.
 * - **Can be told to rearm.** [recheck] is the hook callers use once they have a reason to
 *   believe the current state might have changed - either proof the existing watch (if any)
 *   has gone stale (e.g. a fallback poll caught a real change the watch should have but
 *   didn't - [forceRearm] true, unconditionally tear down and reconstruct) or a storage
 *   mount/unmount signal that might mean a previously-nonexistent directory now exists (or
 *   vice versa - [forceRearm] false, only arm if currently unarmed and now possible).
 *
 * `@Synchronized` because [recheck] can legitimately be invoked concurrently from two
 * different coroutines both dispatched on `Dispatchers.IO`'s shared thread pool - a
 * repository's own fallback-poll branch and a separate storage-events collector are both
 * real call sites, not a hypothetical race.
 */
class SelfHealingDirectoryWatcher(
    private val targetPath: String,
    private val mask: Int,
    private val onChange: () -> Unit,
    private val watcherFactory: (String, Int, () -> Unit) -> DirectoryWatcher = ::FileObserverDirectoryWatcher,
) : DirectoryWatcher {
    private var delegate: DirectoryWatcher? = null
    private var started = false

    @Synchronized
    override fun startWatching() {
        started = true
        armIfPossible()
    }

    @Synchronized
    override fun stopWatching() {
        started = false
        delegate?.stopWatching()
        delegate = null
    }

    @Synchronized
    override fun recheck(forceRearm: Boolean) {
        if (!started) return
        if (forceRearm) {
            delegate?.stopWatching()
            delegate = null
        }
        armIfPossible()
    }

    private fun armIfPossible() {
        if (delegate != null) return
        // Must match FileObserverDirectoryWatcher's own parent-directory resolution -
        // arming here only when that constructor's watch would actually succeed.
        val effectiveParent = File(targetPath).parentFile ?: File("/")
        if (!effectiveParent.exists()) return
        delegate = watcherFactory(targetPath, mask, onChange).also { it.startWatching() }
    }
}
