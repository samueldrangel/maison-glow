package com.maisonglow.service;

/**
 * Summary of a customer's activity (RF-18).
 *
 * @param purchases             number of sales made to the customer
 * @param totalSpent            total amount of those sales
 * @param appointments          number of appointments booked, in any status
 * @param completedAppointments appointments that took place
 * @param noShows               appointments the customer missed
 */
public record CustomerStatistics(int purchases, double totalSpent, int appointments,
                                 int completedAppointments, int noShows) {
}
