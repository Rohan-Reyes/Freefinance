package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * RecurrenceHelper: Pure Kotlin utility for calculating recurring item schedules,
 * occurrences in calendar heatmap, cashflow forecasting, and clean human-readable badges.
 */
object RecurrenceHelper {

    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val shortDateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())

    /**
     * Formats a recurrence rule and date into a clean, human-friendly label.
     * Examples:
     * - "Weekdays at 8:00 AM"
     * - "Daily at 12:00 PM"
     * - "Every Mon, Wed, Fri at 8:00 AM"
     * - "Monthly on the 15th at 9:00 AM"
     * - "Every 2 Weeks on Friday"
     */
    fun formatSchedule(rule: String?, date: Date): String {
        if (rule.isNullOrBlank() || rule.equals("None", ignoreCase = true)) {
            return "One-time (${shortDateFormatter.format(date)})"
        }

        val cal = Calendar.getInstance().apply { time = date }
        val timeStr = timeFormatter.format(date)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Day"
        val ordinalDay = getOrdinal(dayOfMonth)

        val cleanRule = rule.trim()
        return when {
            cleanRule.equals("Daily", ignoreCase = true) -> "Daily at $timeStr"
            cleanRule.equals("Weekdays", ignoreCase = true) -> "Weekdays (Mon–Fri) at $timeStr"
            cleanRule.equals("Weekends", ignoreCase = true) -> "Weekends (Sat–Sun) at $timeStr"
            cleanRule.equals("Weekly", ignoreCase = true) -> "Every $dayName at $timeStr"
            cleanRule.equals("Biweekly", ignoreCase = true) || cleanRule.equals("Bi-weekly", ignoreCase = true) -> "Every 2 Weeks ($dayName) at $timeStr"
            cleanRule.startsWith("Custom:", ignoreCase = true) -> {
                val daysPart = cleanRule.substringAfter("Custom:").trim()
                "Every $daysPart at $timeStr"
            }
            cleanRule.startsWith("Monthly:", ignoreCase = true) -> {
                val daySpec = cleanRule.substringAfter("Monthly:").trim()
                if (daySpec.equals("Last", ignoreCase = true)) {
                    "Monthly on Last Day at $timeStr"
                } else {
                    val num = daySpec.toIntOrNull() ?: dayOfMonth
                    "Monthly on the ${getOrdinal(num)} at $timeStr"
                }
            }
            cleanRule.equals("Monthly", ignoreCase = true) -> "Monthly on the $ordinalDay at $timeStr"
            cleanRule.equals("Yearly", ignoreCase = true) || cleanRule.equals("Annual", ignoreCase = true) -> "Yearly on ${shortDateFormatter.format(date)}"
            else -> "$cleanRule at $timeStr"
        }
    }

    /**
     * Calculates the ordinal string for numbers (e.g. 1st, 2nd, 3rd, 15th).
     */
    fun getOrdinal(n: Int): String {
        return if (n in 11..13) {
            "${n}th"
        } else {
            when (n % 10) {
                1 -> "${n}st"
                2 -> "${n}nd"
                3 -> "${n}rd"
                else -> "${n}th"
            }
        }
    }

    /**
     * Resolves which days (1..daysInMonth) in the given month/year the recurring item falls on.
     */
    fun getOccurrencesInMonth(rule: String?, startDate: Date, month: Int, year: Int): List<Int> {
        if (rule.isNullOrBlank() || rule.equals("None", ignoreCase = true)) return emptyList()

        val startCal = Calendar.getInstance().apply { time = startDate }
        val startYear = startCal.get(Calendar.YEAR)
        val startMonth = startCal.get(Calendar.MONTH)
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)

        // If the schedule starts in a future year or future month of same year
        if (year < startYear || (year == startYear && month < startMonth)) {
            return emptyList()
        }

        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val result = mutableListOf<Int>()

        val isStartMonth = (year == startYear && month == startMonth)
        val firstDayToCheck = 1

        val cleanRule = rule.trim()

        for (day in firstDayToCheck..daysInMonth) {
            val checkCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 7 = Saturday

            val matches = when {
                cleanRule.equals("Daily", ignoreCase = true) -> true
                cleanRule.equals("Weekdays", ignoreCase = true) -> {
                    dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
                }
                cleanRule.equals("Weekends", ignoreCase = true) -> {
                    dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
                }
                cleanRule.equals("Weekly", ignoreCase = true) -> {
                    dayOfWeek == startCal.get(Calendar.DAY_OF_WEEK)
                }
                cleanRule.equals("Biweekly", ignoreCase = true) || cleanRule.equals("Bi-weekly", ignoreCase = true) -> {
                    val daysDiff = ((checkCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    daysDiff >= 0 && (daysDiff % 14 == 0)
                }
                cleanRule.startsWith("Custom:", ignoreCase = true) -> {
                    val daysSpec = cleanRule.substringAfter("Custom:").uppercase(Locale.getDefault())
                    val dayAbbr = when (dayOfWeek) {
                        Calendar.SUNDAY -> "SUN"
                        Calendar.MONDAY -> "MON"
                        Calendar.TUESDAY -> "TUE"
                        Calendar.WEDNESDAY -> "WED"
                        Calendar.THURSDAY -> "THU"
                        Calendar.FRIDAY -> "FRI"
                        Calendar.SATURDAY -> "SAT"
                        else -> ""
                    }
                    daysSpec.contains(dayAbbr)
                }
                cleanRule.startsWith("Monthly:", ignoreCase = true) -> {
                    val daySpec = cleanRule.substringAfter("Monthly:").trim()
                    if (daySpec.equals("Last", ignoreCase = true)) {
                        day == daysInMonth
                    } else {
                        val targetDay = (daySpec.toIntOrNull() ?: startDay).coerceIn(1, daysInMonth)
                        day == targetDay
                    }
                }
                cleanRule.equals("Monthly", ignoreCase = true) -> {
                    val targetDay = startDay.coerceIn(1, daysInMonth)
                    day == targetDay
                }
                cleanRule.equals("Yearly", ignoreCase = true) || cleanRule.equals("Annual", ignoreCase = true) -> {
                    month == startMonth && day == startDay.coerceIn(1, daysInMonth)
                }
                else -> {
                    val targetDay = startDay.coerceIn(1, daysInMonth)
                    day == targetDay
                }
            }

            if (matches) {
                result.add(day)
            }
        }

        return result
    }

    /**
     * Calculates the estimated monthly run-rate equivalent of a recurring rule.
     */
    fun calculateMonthlyEquivalent(amount: Double, rule: String?): Double {
        if (rule.isNullOrBlank() || rule.equals("None", ignoreCase = true)) return amount
        val cleanRule = rule.trim()
        return when {
            cleanRule.equals("Daily", ignoreCase = true) -> amount * 30.4
            cleanRule.equals("Weekdays", ignoreCase = true) -> amount * 21.7 // Average 21.7 weekdays/mo
            cleanRule.equals("Weekends", ignoreCase = true) -> amount * 8.7  // Average 8.7 weekend days/mo
            cleanRule.equals("Weekly", ignoreCase = true) -> amount * 4.333
            cleanRule.equals("Biweekly", ignoreCase = true) || cleanRule.equals("Bi-weekly", ignoreCase = true) -> amount * 2.166
            cleanRule.startsWith("Custom:", ignoreCase = true) -> {
                val daysSpec = cleanRule.substringAfter("Custom:").split(",")
                val count = daysSpec.size.coerceAtLeast(1)
                amount * (count * 4.333)
            }
            cleanRule.startsWith("Monthly", ignoreCase = true) -> amount
            cleanRule.equals("Yearly", ignoreCase = true) || cleanRule.equals("Annual", ignoreCase = true) -> amount / 12.0
            else -> amount
        }
    }

    /**
     * Alias for calculateMonthlyEquivalent.
     */
    fun calculateMonthlyRunRate(amount: Double, rule: String?): Double = calculateMonthlyEquivalent(amount, rule)

    /**
     * Calculates the total remaining amount for a recurring rule in a given month from `currentDay` onwards.
     */
    fun calculateRemainingMonthlyAmount(
        amount: Double,
        recurrenceRule: String?,
        startDate: Date,
        currentDay: Int,
        month: Int,
        year: Int
    ): Double {
        val occurrences = getOccurrencesInMonth(recurrenceRule, startDate, month, year)
        val remainingCount = occurrences.count { it >= currentDay }
        return amount * remainingCount
    }

    /**
     * Calculates the next upcoming due date after or on today.
     */
    fun calculateNextDueDate(startDate: Date, rule: String?): Date {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = Calendar.getInstance().apply { time = startDate }
        val hour = startCal.get(Calendar.HOUR_OF_DAY)
        val minute = startCal.get(Calendar.MINUTE)

        if (rule.isNullOrBlank() || rule.equals("None", ignoreCase = true)) {
            return startDate
        }

        // Search next 90 days for the first matching occurrence
        val searchCal = Calendar.getInstance().apply {
            time = today.time
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0..90) {
            val curMonth = searchCal.get(Calendar.MONTH)
            val curYear = searchCal.get(Calendar.YEAR)
            val curDay = searchCal.get(Calendar.DAY_OF_MONTH)

            val monthOccurrences = getOccurrencesInMonth(rule, startDate, curMonth, curYear)
            if (curDay in monthOccurrences) {
                // If today is an occurrence, check if time hasn't passed or if it's today
                if (searchCal.timeInMillis >= System.currentTimeMillis() - 60_000 || i > 0) {
                    return searchCal.time
                }
            }
            searchCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return startDate
    }

    /**
     * Calculates the subsequent occurrence strictly after a reference date (e.g. for advancing a recurring cycle).
     */
    fun calculateNextOccurrenceStrictlyAfter(afterDate: Date, startDate: Date, rule: String?): Date {
        val startCal = Calendar.getInstance().apply { time = startDate }
        val hour = startCal.get(Calendar.HOUR_OF_DAY)
        val minute = startCal.get(Calendar.MINUTE)

        if (rule.isNullOrBlank() || rule.equals("None", ignoreCase = true)) {
            return startDate
        }

        val searchCal = Calendar.getInstance().apply {
            time = afterDate
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // strictly after
        }

        for (i in 0..120) {
            val curMonth = searchCal.get(Calendar.MONTH)
            val curYear = searchCal.get(Calendar.YEAR)
            val curDay = searchCal.get(Calendar.DAY_OF_MONTH)

            val monthOccurrences = getOccurrencesInMonth(rule, startDate, curMonth, curYear)
            if (curDay in monthOccurrences) {
                return searchCal.time
            }
            searchCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return startDate
    }

    /**
     * Days until the target date from today.
     */
    fun daysUntil(targetDate: Date): Long {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val diff = targetDate.time - today
        return max(0L, diff / (1000 * 60 * 60 * 24))
    }
}
