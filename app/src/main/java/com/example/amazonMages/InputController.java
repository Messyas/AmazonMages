package com.example.amazonMages;

import android.view.MotionEvent;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gamepanel.Joystick;
import com.example.amazonMages.combat.WeaponLoadout;

/** Multi-pointer gameplay input supporting Aim Charge, Parry/Guard, Dash, Quick-Item, Special & Swap. */
final class InputController {
    private final Player player;
    private final Joystick joystick;
    private final Joystick aimStick;
    private final WeaponLoadout weapons;
    private final Runnable special;
    private final Runnable parry;
    private final Runnable dash;
    private final Runnable quickItem;
    /** Stick pushes below this are "not aiming": a tap fires at the last aim, a drag back here cancels. */
    static final double AIM_DEADZONE = .28;
    private static final double AIM_TOUCH_SLACK = 1.3;
    private int movementPointer = -1, aimPointer = -1, pendingShots;
    private boolean aimDragged;
    private int aimHoldTicks;
    private int pendingChargedTicks;

    InputController(Player player, Joystick joystick, Joystick aimStick, WeaponLoadout weapons,
                    Runnable special, Runnable parry) {
        this(player, joystick, aimStick, weapons, special, parry, () -> {}, () -> {});
    }

    InputController(Player player, Joystick joystick, Joystick aimStick, WeaponLoadout weapons,
                    Runnable special, Runnable parry, Runnable dash, Runnable quickItem) {
        this.player = player;
        this.joystick = joystick;
        this.aimStick = aimStick;
        this.weapons = weapons;
        this.special = special;
        this.parry = parry;
        this.dash = dash != null ? dash : () -> {};
        this.quickItem = quickItem != null ? quickItem : () -> {};
    }

    void reset() {
        joystick.setIsPressed(false);
        joystick.resetActuator();
        releaseAimStick();
        movementPointer = -1;
        pendingShots = 0;
        pendingChargedTicks = 0;
    }

    private void releaseAimStick() {
        aimStick.setIsPressed(false);
        aimStick.resetActuator();
        aimPointer = -1;
        aimDragged = false;
        aimHoldTicks = 0;
    }

    void updateAimHold() {
        if (aimStick.getIsPressed() && aimStick.magnitude() >= AIM_DEADZONE) {
            aimHoldTicks++;
        } else if (!aimStick.getIsPressed()) {
            aimHoldTicks = 0;
        }
    }

    int currentAimHoldTicks() { return aimHoldTicks; }

    /** Follows the finger; the player's aim only changes once the stick leaves the dead zone. */
    private void dragAimStick(float x, float y) {
        aimStick.setActuator(x, y);
        if (aimStick.magnitude() >= AIM_DEADZONE) {
            aimDragged = true;
            player.aim(aimStick.getActuatorX(), aimStick.getActuatorY());
        }
    }

    /** Lifting the finger fires along the aim; dragging back to the centre first cancels the shot. */
    private void releaseAim() {
        boolean cancelled = aimDragged && aimStick.magnitude() < AIM_DEADZONE;
        if (!cancelled) {
            if (!aimDragged && player.getAttackDirectionX() == 0 && player.getAttackDirectionY() == 0) {
                player.aim(player.getDirectionX(), player.getDirectionY());
            }
            pendingChargedTicks = aimHoldTicks;
            attack();
        }
        releaseAimStick();
    }

    boolean consumeShot() {
        if (pendingShots == 0) return false;
        pendingShots--;
        return true;
    }

    int consumeLastHoldTicks() {
        int ticks = pendingChargedTicks;
        pendingChargedTicks = 0;
        return ticks;
    }

    private void attack() { pendingShots = Math.min(8, pendingShots + 1); }

    boolean touch(MotionEvent event, int width, int height) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_CANCEL:
                reset();
                return true;
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                int index = event.getActionIndex();
                float x = event.getX(index), y = event.getY(index);
                if (aimPointer < 0 && aimStick.contains(x, y, AIM_TOUCH_SLACK)) {
                    aimPointer = event.getPointerId(index);
                    aimDragged = false;
                    aimHoldTicks = 0;
                    aimStick.setIsPressed(true);
                    dragAimStick(x, y);
                } else if (x > width * .80f && y > height * .45f && y < height * .67f) {
                    weapons.next();
                } else if (x > width * .63f && x <= width * .80f && y > height * .54f && y <= height * .77f) {
                    dash.run();
                } else if (x > width * .46f && x <= width * .58f && y > height * .74f) {
                    quickItem.run();
                } else if (x > width * .58f && x <= width * .70f && y > height * .74f) {
                    parry.run();
                } else if (x > width * .70f && x <= width * .83f && y > height * .77f) {
                    special.run();
                } else if (joystick.getIsPressed()) {
                    attack();
                } else if (joystick.isPressed(x, y)) {
                    movementPointer = event.getPointerId(index);
                    joystick.setIsPressed(true);
                    joystick.setActuator(x, y);
                } else {
                    attack();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                int aim = event.findPointerIndex(aimPointer);
                if (aim >= 0 && aimStick.getIsPressed()) dragAimStick(event.getX(aim), event.getY(aim));
                int movement = event.findPointerIndex(movementPointer);
                if (joystick.getIsPressed() && movement >= 0) joystick.setActuator(event.getX(movement), event.getY(movement));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                int pointer = event.getPointerId(event.getActionIndex());
                if (aimPointer == pointer) releaseAim();
                if (movementPointer == pointer) {
                    movementPointer = -1;
                    joystick.setIsPressed(false);
                    joystick.resetActuator();
                }
                return true;
            default:
                return false;
        }
    }
}
