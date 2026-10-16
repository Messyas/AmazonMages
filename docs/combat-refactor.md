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

## Equipment and coconut prototype

- `WeaponDefinition`, `WeaponType`, `WeaponLoadout`: four quick slots, one starter intrinsic magic, basic and special cooldowns.
- Starter coconut basic: brown circular projectile with physical hit and posture pressure; uses independent attack aim vector.
- Starter coconut storm: 150-tick area effect, eight to nine randomized impact pulses in a 125px radius, 1080-tick cooldown. Area preview drawn on Canvas; falling-fruit sprites, telegraphs and sounds are still pending.
- Provisional Canvas HUD: lower-right attack, special and weapon switch zones. The starter is the only equipped weapon; cycling does not yet show other options until acquired/equipped.
- Touch handling now uses the correct pointer index for joystick movement and an independent attack pointer for aiming. Drag from the attack region to aim subsequent attacks; current basic fires on press.
- This is a playable prototype layer, **not** a finished inventory or combat feature set. Catalyst and physical equipment have data categories but no distinct attack executors yet. No mana, potions, equipment persistence, shield/parry, skill tree or full inventory.
- Android compilation and device tests were not available; validate resource loading, HUD scaling, multi-touch, pause/resume and save slots before merging.
