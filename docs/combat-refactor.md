# Combat foundation (incremental migration)

Branch: `fix/android-performance`.

## Implemented

- `combat/DamageType`: PHYSICAL, MAGIC, ACID.
- `combat/DamageEvent`: immutable damage plus posture pressure.
- `combat/DamageResolver`: resistance and temporary damage reduction with multiplicative stacking.
- `combat/CombatStats`: per-entity damage/resistance and movement modifiers, neutral defaults.
- `combat/Posture`: meter, delayed recovery and pressure accumulation.
- `combat/StatusEffects`: tick-based speed/resistance boost.
- `Player.receiveHit` and `Enemy.receiveHit`: fractional HP internally, existing integer HP API retained for compatibility.
- Acid projectiles and player basic spells now pass through typed hit events.

## Not yet implemented

Posture break/stun consequences, parry shield collision and directional timing, stat leveling, skill tree and prerequisites, equipment/loadouts, mana, inventory/consumables, intrinsic coconut basic/special, UI and save migration. These should be developed on top of the foundation rather than claimed as complete.

## Important behavior

Current player maximum health remains 5 and is still displayed as an integer. Internal HP uses a double so a 20% damage reduction is not lost to integer truncation. Combat modifiers default to neutral values; the existing basic spell deals 1 MAGIC damage. Posture currently accumulates and regenerates but does not trigger a stagger; no shield has been wired in yet.

## Validation

No Android/Gradle build has been run in this environment. Compile and run tests on the Android project before merging. In particular verify damage at low HP, fused ghosts, projectile collision, save/restore and lifecycle pause/resume.
