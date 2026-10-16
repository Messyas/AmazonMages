package com.example.amazonMages.ecs;
import com.example.amazonMages.gameobject.Enemy;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/** Ordered ECS membership. Enemy remains authoritative for AI, animation, position and health. */
public final class EnemyRegistry extends AbstractList<Enemy> {
    public static final class EnemyComponent implements Component {
        public final Enemy model;
        private boolean defeatReported;
        EnemyComponent(Enemy model) { this.model=model; }
        public double x() { return model.getPositionX(); }
        public double y() { return model.getPositionY(); }
        public int health() { return model.getHealth(); }
    }
    private final World world=new World();
    private final List<Entity> ordered=new ArrayList<>();
    @Override public Enemy get(int index) { return ordered.get(index).get(EnemyComponent.class).model; }
    @Override public int size() { return ordered.size(); }
    @Override public boolean add(Enemy enemy) {
        ordered.add(world.create().add(EnemyComponent.class,new EnemyComponent(enemy))); modCount++; return true;
    }
    @Override public Enemy remove(int index) {
        Entity entity=ordered.remove(index); world.destroy(entity.id()); modCount++;
        return entity.get(EnemyComponent.class).model;
    }
    @Override public void clear() { ordered.clear();world.clear();modCount++; }
    public long claimDefeat(Enemy enemy) {
        for(Entity entity:ordered) {
            EnemyComponent component=entity.get(EnemyComponent.class);
            if(component.model==enemy && !component.defeatReported && enemy.isDead()) {
                component.defeatReported=true; return entity.id();
            }
        }
        return 0;
    }
}
