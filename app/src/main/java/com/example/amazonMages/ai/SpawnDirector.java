package com.example.amazonMages.ai;
import java.util.Random;

/** Region content and timing are independent of entity storage and AI. */
public final class SpawnDirector {
    public static final class Region {
        public final int tileType, population, intervalTicks;
        public final double minDistance,maxDistance;
        private final EnemyTemplate[] types;
        public Region(int tileType,int population,int intervalTicks,double minDistance,double maxDistance,EnemyTemplate... types) {
            if (population<1 || population>100 || intervalTicks<1 || !Double.isFinite(minDistance) || !Double.isFinite(maxDistance)
                || minDistance<0 || maxDistance<minDistance || types.length==0) throw new IllegalArgumentException("spawn region");
            for(EnemyTemplate type:types) if(type==null || type.isBossEncounter())throw new IllegalArgumentException("ambient boss");
            this.tileType=tileType;this.population=population;this.intervalTicks=intervalTicks;
            this.minDistance=minDistance;this.maxDistance=maxDistance;this.types=types.clone();
        }
    }
    public final Region region;
    private int ticks;
    public SpawnDirector(Region region) { this.region=region; }
    public EnemyTemplate update(int population,Random random) {
        if(++ticks<region.intervalTicks)return null;
        ticks=0;
        return population>=region.population ? null : region.types[random.nextInt(region.types.length)];
    }
    public int updatePeriod(double distance) { return distance<800?1:distance<1250?6:60; }
    public boolean shouldDespawn(double distance,boolean boss) { return !boss && distance>1900; }
}
