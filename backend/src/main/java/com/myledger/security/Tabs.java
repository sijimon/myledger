package com.myledger.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Grantable contractor tab keys. Owners implicitly have all of them. Stored per user as
 * a comma-separated string; exposed to the frontend to drive contractor navigation and
 * enforced server-side for read-only sections (expenses, dashboard).
 */
public final class Tabs {

    public static final String FUND_REQUESTS = "FUND_REQUESTS";
    public static final String EXPENSES = "EXPENSES";
    public static final String DASHBOARD = "DASHBOARD";
    public static final String REPORTS = "REPORTS";

    /** All valid keys, in display order. */
    public static final Set<String> ALL =
            new LinkedHashSet<>(Arrays.asList(FUND_REQUESTS, EXPENSES, DASHBOARD, REPORTS));

    private Tabs() {
    }

    /** Parse a stored CSV into an ordered set of valid keys (unknown keys dropped). */
    public static Set<String> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(ALL::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Serialise a set of keys to CSV, keeping only valid ones in canonical order. */
    public static String toCsv(Set<String> keys) {
        return ALL.stream().filter(keys::contains).collect(Collectors.joining(","));
    }
}
