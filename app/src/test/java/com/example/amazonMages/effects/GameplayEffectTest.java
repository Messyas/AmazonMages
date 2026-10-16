package com.example.amazonMages.effects;
import com.example.amazonMages.combat.*;
import com.example.amazonMages.items.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class GameplayEffectTest {
    static class Target implements GameplayEffect.Target {
        double health=3,mana=80;
        StatusEffects effects=new StatusEffects();
        public boolean damage(DamageEvent hit) { health-=DamageResolver.resolve(hit,null,effects); return true; }
        public boolean heal(double n) { if(health>=5)return false; health=Math.min(5,health+n);return true; }
        public boolean mana(double n) { if(mana>=100)return false;mana=Math.min(100,mana+n);return true; }
        public StatusEffects statuses() {return effects;}
    }
    @Test public void consumablesApplyOnlyWhenUsefulAndBuffDoesNotStack() {
        ItemRegistry catalog=ItemCatalog.create(); Inventory bag=new Inventory(catalog);
        bag.add("erva",2);bag.add("mana",2);bag.add("bencao",2);
        Target target=new Target(); ConsumableService service=new ConsumableService();
        assertTrue(service.use(bag,catalog.get("erva"),target));assertEquals(5,target.health,0);
        assertFalse(service.use(bag,catalog.get("erva"),target));assertEquals(1,bag.count("erva"));
        assertTrue(service.use(bag,catalog.get("mana"),target));assertEquals(100,target.mana,0);
        assertFalse(service.use(bag,catalog.get("mana"),target));
        assertTrue(service.use(bag,catalog.get("bencao"),target));
        assertFalse(service.use(bag,catalog.get("bencao"),target));
        target.effects.grantSlow(2,.2); assertEquals(.92,target.effects.speedMultiplier(),.0001);
        target.effects.update();target.effects.update();assertEquals(1.15,target.effects.speedMultiplier(),.0001);
        for(int i=0;i<298;i++)target.effects.update();assertEquals(1,target.effects.speedMultiplier(),0);
    }
}
