package wealthsei.service;

import java.time.LocalDate;
import wealthsei.domain.Money;

/** Tells the goal service how much money is left over each month (implemented by the analytics layer). */
public interface MonthlySurplusProvider {

    /** Income minus bills and recurring payments per month; zero when there is no plan yet. */
    Money monthlySurplus(LocalDate today);
}
