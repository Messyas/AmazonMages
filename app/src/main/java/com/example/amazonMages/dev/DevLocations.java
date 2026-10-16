package com.example.amazonMages.dev;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Named world coordinates; keep IDs stable when map content evolves. */
public final class DevLocations {
    private static final Map<String, double[]> LOCATIONS;
    static {
        Map<String, double[]> locations = new LinkedHashMap<>();
        locations.put("inicio", new double[]{1000, 500});
        locations.put("pantano", new double[]{1200, 900});
        locations.put("clareira", new double[]{1800, 1200});
        LOCATIONS = Collections.unmodifiableMap(locations);
    }
    private DevLocations() {}
    public static double[] find(String id) {
        double[] point = LOCATIONS.get(id);
        return point == null ? null : point.clone();
    }
    public static String names() { return String.join(", ", LOCATIONS.keySet()); }
}
