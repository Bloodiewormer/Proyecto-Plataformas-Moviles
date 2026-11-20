package cr.ac.una.glifo.core.push

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import cr.ac.una.glifo.core.datastore.TokenManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncLocalNotificationLogicTest {

    private val context = mockk<Context>(relaxed = true)
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val appVisibilityChecker = mockk<AppVisibilityChecker>()
    private val notificationManagerCompat = mockk<NotificationManagerCompat>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(context) } returns notificationManagerCompat
        every { notificationManagerCompat.areNotificationsEnabled() } returns true
        every { context.packageName } returns "cr.ac.una.glifo"
        coEvery { tokenManager.isNotifySyncDoneEnabled() } returns true
    }

    @After
    fun tearDown() {
        unmockkStatic(NotificationManagerCompat::class)
    }

    @Test
    fun `showLocalSyncCompleteNotification returns false when uploaded count is zero or negative`() = runTest {
        val presenter = SyncNotificationPresenter(context, tokenManager, appVisibilityChecker)
        val result = presenter.showLocalSyncCompleteNotification(uploadedCount = 0)
        assertFalse(result)
    }

    @Test
    fun `showLocalSyncCompleteNotification returns false when user disabled notification in settings`() = runTest {
        coEvery { tokenManager.isNotifySyncDoneEnabled() } returns false
        every { appVisibilityChecker.isAppInForeground() } returns false

        val presenter = SyncNotificationPresenter(context, tokenManager, appVisibilityChecker)
        val result = presenter.showLocalSyncCompleteNotification(uploadedCount = 2)

        assertFalse(result)
    }

    @Test
    fun `showLocalSyncCompleteNotification returns false when app is in foreground`() = runTest {
        coEvery { tokenManager.isNotifySyncDoneEnabled() } returns true
        every { appVisibilityChecker.isAppInForeground() } returns true

        val presenter = SyncNotificationPresenter(context, tokenManager, appVisibilityChecker)
        val result = presenter.showLocalSyncCompleteNotification(uploadedCount = 1)

        assertFalse(result)
    }

    @Test
    fun `showLocalSyncCompleteNotification returns false when notification permission is not granted`() = runTest {
        coEvery { tokenManager.isNotifySyncDoneEnabled() } returns true
        every { appVisibilityChecker.isAppInForeground() } returns false

        val presenter = spyk(SyncNotificationPresenter(context, tokenManager, appVisibilityChecker))
        every { presenter.hasNotificationPermission() } returns false

        val result = presenter.showLocalSyncCompleteNotification(uploadedCount = 1)

        assertFalse(result)
    }
}
