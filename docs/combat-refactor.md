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
- `Player.beginParry` and `HostileProjectileWorld`: short frontal parry window intercepts acid projectiles; the HUD shows its recovery state.
- `PlayerDefenseFx`: directional leaf ward, successful-parry flash and posture-break flash use the authored PNG atlases.

## Current parry behavior

Tap the leaf shield button to open a 10-tick parry window with a 36-tick cooldown. The attack aim sets the facing direction; only enemy projectiles entering the frontal 100-degree arc are intercepted. Other hits keep their current damage and acid slow behavior. Posture depletion displays a flash but does not stun or otherwise change combat behavior. Enemies currently have no melee collision to block.

## Important behavior

Current player maximum health remains 5 and is still displayed as an integer. Internal HP uses a double so a 20% damage reduction is not lost to integer truncation. Combat modifiers default to neutral values; posture recovers after pressure. A depleted player posture currently has visual feedback only.

## Validation

`assembleDebug` passed after the parry integration. Automated combat tests and physical-device checks remain pending; verify touch target scaling, aim direction, projectile interception, low-health hits, save/restore and lifecycle pause/resume.

## Equipment and coconut prototype

- `WeaponDefinition`, `WeaponType`, `WeaponLoadout`: four quick slots, one starter intrinsic magic, basic and special cooldowns.
- Starter coconut basic: brown circular projectile with physical hit and posture pressure; uses independent attack aim vector.
- Starter coconut storm: 150-tick area effect, eight to nine randomized impact pulses in a 125px radius, 1080-tick cooldown. Area preview drawn on Canvas; falling-fruit sprites, telegraphs and sounds are still pending.
- Provisional Canvas HUD: lower-right attack, special and weapon switch zones. The starter is the only equipped weapon; cycling does not yet show other options until acquired/equipped.
- Touch handling now uses the correct pointer index for joystick movement and an independent attack pointer for aiming. Drag from the attack region to aim subsequent attacks; current basic fires on press.
- This is a playable prototype layer, **not** a finished inventory or combat feature set. Catalyst and physical equipment have data categories but no distinct attack executors yet. No mana, potions, equipment persistence, shield/parry, skill tree or full inventory.
- Android compilation and device tests were not available; validate resource loading, HUD scaling, multi-touch, pause/resume and save slots before merging.
