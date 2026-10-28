package com.example.amazonMages.dev;
import com.example.amazonMages.map.WorldDefinition;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Named locations use the same authored geometry as gameplay and the overview. */
public final class DevLocations {
    private static final Map<String, double[]> LOCATIONS;
    static {
        WorldDefinition world = new WorldDefinition();
        Map<String, double[]> locations = new LinkedHashMap<>();
        locations.put("inicio", new double[]{world.spawn.worldX(), world.spawn.worldY()});
        locations.put("clareira", new double[]{world.spawn.worldX(), world.spawn.worldY()});
        locations.put("pantano", new double[]{730 * 64, 1120 * 64});
        locations.put("pantano_leste", new double[]{1230 * 64, 900 * 64});
        for (WorldDefinition.Site site : world.landmarks) {
            WorldDefinition.Position safe = world.safePosition(site.center.worldX(), site.center.worldY(), 32);
            locations.put(site.id, new double[]{safe.x, safe.y});
        }
        for (WorldDefinition.Site site : world.arenas)
            locations.put(site.id, new double[]{site.center.worldX(), site.center.worldY()});
        LOCATIONS = Collections.unmodifiableMap(locations);
    }
    private DevLocations() {}
    public static double[] find(String id) {
        double[] point = LOCATIONS.get(id);
        return point == null ? null : point.clone();
    }
    public static String names() { return String.join(", ", LOCATIONS.keySet()); }
}