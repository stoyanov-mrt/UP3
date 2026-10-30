package com.example.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Working day calendar: Monday to Friday. Public holidays are not part of the requirements.
 */
public final class WorkingDays {

    private WorkingDays() {
    }

    public static boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /**
     * The first working day strictly after the given date.
     */
    public static LocalDate nextWorkingDay(LocalDate date) {
        LocalDate result = date.plusDays(1);
        while (!isWorkingDay(result)) {
            result = result.plusDays(1);
        }
        return result;
    }
}
