package com.example.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Working days are Monday to Friday - the team's decision for "next working day" in the ON_SITE scheduling rule
 * (the business requirements do not define working days or holidays, so holidays are not tested).
 * Reference week: Mon 2026-10-26 ... Sun 2026-11-01.
 */
class WorkingDaysTest {

    @ParameterizedTest
    @CsvSource({"2026-10-26", "2026-10-27", "2026-10-28", "2026-10-29", "2026-10-30"})
    void mondayToFridayAreWorkingDays(LocalDate date) {
        assertTrue(WorkingDays.isWorkingDay(date));
    }

    @ParameterizedTest
    @CsvSource({"2026-10-31", "2026-11-01"})
    void saturdayAndSundayAreNotWorkingDays(LocalDate date) {
        assertFalse(WorkingDays.isWorkingDay(date));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "2026-10-26, 2026-10-27", // Mon -> Tue
            "2026-10-29, 2026-10-30", // Thu -> Fri
    })
    void nextWorkingDayFromMondayToThursdayIsTheFollowingDay(LocalDate date, LocalDate expected) {
        assertEquals(expected, WorkingDays.nextWorkingDay(date));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "2026-10-30, 2026-11-02", // Fri -> Mon (scheduling an ON_SITE service on Friday)
            "2026-10-31, 2026-11-02", // Sat -> Mon
            "2026-11-01, 2026-11-02", // Sun -> Mon
    })
    void weekendIsSkipped(LocalDate date, LocalDate expected) {
        assertEquals(expected, WorkingDays.nextWorkingDay(date));
    }

    @Test
    void nextWorkingDayIsStrictlyAfterAWorkingDay() {
        LocalDate wednesday = LocalDate.of(2026, 10, 28);

        LocalDate result = WorkingDays.nextWorkingDay(wednesday);

        // An ON_SITE service can never be scheduled for the same day
        assertEquals(LocalDate.of(2026, 10, 29), result);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "2027-12-31, 2028-01-03", // year boundary on a Friday, Jan 1-2 are a weekend
            "2026-07-31, 2026-08-03", // month boundary on a Friday
            "2020-02-28, 2020-03-02", // leap year: Fri -> skips Sat Feb 29 and Sun Mar 1
            "2028-02-28, 2028-02-29", // leap year: Mon -> Tue Feb 29
    })
    void calendarBoundaries(LocalDate date, LocalDate expected) {
        assertEquals(expected, WorkingDays.nextWorkingDay(date));
    }

    @Test
    void resultIsAlwaysAWorkingDayWithinThreeDays() {
        LocalDate start = LocalDate.of(2026, 10, 26);

        for (int i = 0; i < 14; i++) {
            LocalDate date = start.plusDays(i);
            LocalDate result = WorkingDays.nextWorkingDay(date);

            assertTrue(WorkingDays.isWorkingDay(result), "not a working day for " + date);
            assertTrue(result.isAfter(date) && !result.isAfter(date.plusDays(3)), "out of range for " + date);
        }
    }
}
