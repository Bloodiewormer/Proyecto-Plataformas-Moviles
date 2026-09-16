package cr.ac.una.glifo.core.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralized telemetry and error reporter for Glifo.
 * Wraps Firebase Crashlytics to record custom logs, keys, and non-fatal exceptions
 * (especially useful for native library operations like OpenCV and ML Kit).
 */
object GlifoCrashReporter {

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    /**
     * Records a breadcrumb log in Crashlytics.
     */
    fun log(message: String) {
        crashlytics.log(message)
    }

    /**
     * Sets a custom string key-value pair for diagnostic context.
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Sets a custom boolean key-value pair.
     */
    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Sets a custom int key-value pair.
     */
    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Identifies the current logged in user in Crashlytics reports without storing PII.
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    /**
     * Records a non-fatal exception, such as an OpenCV segmentation failure or ML Kit decoding issue,
     * allowing the app to keep running while sending diagnostic info to the Firebase console.
     */
    fun recordException(throwable: Throwable, contextTag: String? = null) {
        if (contextTag != null) {
            crashlytics.setCustomKey("failure_context", contextTag)
        }
        crashlytics.recordException(throwable)
    }

    /**
     * Triggers a test crash to verify the Crashlytics installation in Firebase Console.
     * Per Firebase Crashlytics documentation, this should be tested once during setup.
     */
    fun testCrash(): Nothing {
        throw RuntimeException("Glifo Crashlytics Test Crash")
    }
}
