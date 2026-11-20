package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.datastore.AppPreferences
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.core.reminder.StudyReminderScheduler
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelNotificationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val syncRepository = mockk<SyncRepository>(relaxed = true)
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private val studyReminderScheduler = mockk<StudyReminderScheduler>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { syncRepository.getPendingCount() } returns Result.Success(0)
        every { appPreferences.dailyStudyReminderFlow } returns flowOf(true)
        every { tokenManager.notifySyncDoneFlow } returns flowOf(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dailyStudyReminder flow exposes preference value`() = runTest {
        val viewModel = SettingsViewModel(
            syncRepository = syncRepository,
            tokenManager = tokenManager,
            appPreferences = appPreferences,
            studyReminderScheduler = studyReminderScheduler
        )
        advanceUntilIdle()

        assertTrue(viewModel.dailyStudyReminder.value)
    }

    @Test
    fun `setDailyStudyReminder to false updates preferences and cancels scheduler`() = runTest {
        val viewModel = SettingsViewModel(
            syncRepository = syncRepository,
            tokenManager = tokenManager,
            appPreferences = appPreferences,
            studyReminderScheduler = studyReminderScheduler
        )

        viewModel.setDailyStudyReminder(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { appPreferences.setDailyStudyReminder(false) }
        verify(exactly = 1) { studyReminderScheduler.cancel() }
        verify(exactly = 0) { studyReminderScheduler.schedule() }
    }

    @Test
    fun `setDailyStudyReminder to true updates preferences and schedules reminder`() = runTest {
        val viewModel = SettingsViewModel(
            syncRepository = syncRepository,
            tokenManager = tokenManager,
            appPreferences = appPreferences,
            studyReminderScheduler = studyReminderScheduler
        )

        viewModel.setDailyStudyReminder(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { appPreferences.setDailyStudyReminder(true) }
        verify(exactly = 1) { studyReminderScheduler.schedule() }
        verify(exactly = 0) { studyReminderScheduler.cancel() }
    }
}
