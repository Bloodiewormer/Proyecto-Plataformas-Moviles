package cr.ac.una.glifo.core.reminder

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object StudyReminderTimeCalculator {
    const val DEFAULT_TARGET_HOUR = 19
    const val DEFAULT_TARGET_MINUTE = 0

    fun calculateInitialDelay(
        now: ZonedDateTime,
        targetHour: Int = DEFAULT_TARGET_HOUR,
        targetMinute: Int = DEFAULT_TARGET_MINUTE
    ): Duration {
        var target = now.withHour(targetHour)
            .withMinute(targetMinute)
            .withSecond(0)
            .withNano(0)

        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target)
    }

    fun calculateInitialDelayMillis(
        nowEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        targetHour: Int = DEFAULT_TARGET_HOUR,
        targetMinute: Int = DEFAULT_TARGET_MINUTE
    ): Long {
        val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId)
        return calculateInitialDelay(now, targetHour, targetMinute).toMillis()
    }
}
