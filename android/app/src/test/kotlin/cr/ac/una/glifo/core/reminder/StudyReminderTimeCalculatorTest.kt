package cr.ac.una.glifo.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class StudyReminderTimeCalculatorTest {

    private val zone = ZoneId.of("America/Costa_Rica")
    private val date = LocalDate.of(2026, 11, 17)

    @Test
    fun `when current time is in the morning, initial delay targets 19 00 today`() {
        val now = ZonedDateTime.of(date, LocalTime.of(10, 0, 0), zone)
        val delay = StudyReminderTimeCalculator.calculateInitialDelay(now)

        assertEquals(Duration.ofHours(9), delay)
    }

    @Test
    fun `when current time is in the afternoon before 19 00, delay is remaining hours and minutes`() {
        val now = ZonedDateTime.of(date, LocalTime.of(15, 30, 0), zone)
        val delay = StudyReminderTimeCalculator.calculateInitialDelay(now)

        assertEquals(Duration.ofHours(3).plusMinutes(30), delay)
    }

    @Test
    fun `when current time is after 19 00, initial delay targets 19 00 tomorrow`() {
        val now = ZonedDateTime.of(date, LocalTime.of(21, 15, 0), zone)
        val delay = StudyReminderTimeCalculator.calculateInitialDelay(now)

        assertEquals(Duration.ofHours(21).plusMinutes(45), delay)
    }

    @Test
    fun `when current time is exactly 19 00, delay targets 19 00 tomorrow (24 hours)`() {
        val now = ZonedDateTime.of(date, LocalTime.of(19, 0, 0), zone)
        val delay = StudyReminderTimeCalculator.calculateInitialDelay(now)

        assertEquals(Duration.ofHours(24), delay)
    }

    @Test
    fun `when current time is shortly after midnight, delay targets 19 00 of the same day`() {
        val now = ZonedDateTime.of(date, LocalTime.of(0, 30, 0), zone)
        val delay = StudyReminderTimeCalculator.calculateInitialDelay(now)

        assertEquals(Duration.ofHours(18).plusMinutes(30), delay)
    }

    @Test
    fun `calculateInitialDelayMillis matches duration to millis`() {
        val now = ZonedDateTime.of(date, LocalTime.of(18, 0, 0), zone)
        val epochMillis = now.toInstant().toEpochMilli()

        val millis = StudyReminderTimeCalculator.calculateInitialDelayMillis(
            nowEpochMillis = epochMillis,
            zoneId = zone
        )

        assertEquals(3_600_000L, millis)
    }
}
