package com.example.amazonMages.graphics;

import java.util.EnumMap;

/** Maps an action and facing to a clip; supports a fallback for legacy single-view art. */
public final class AnimationSet {
    private final EnumMap<AnimationAction, EnumMap<Direction, AnimationClip>> clips =
        new EnumMap<>(AnimationAction.class);
    private final EnumMap<AnimationAction, AnimationClip> fallback =
        new EnumMap<>(AnimationAction.class);

    public AnimationSet put(AnimationAction action, Direction facing, AnimationClip clip) {
        EnumMap<Direction, AnimationClip> directional = clips.get(action);
        if (directional == null) {
            directional = new EnumMap<>(Direction.class);
            clips.put(action, directional);
        }
        directional.put(facing, clip);
        return this;
    }

    public AnimationSet putFallback(AnimationAction action, AnimationClip clip) {
        fallback.put(action, clip);
        return this;
    }

    public AnimationClip get(AnimationAction action, Direction facing) {
        EnumMap<Direction, AnimationClip> directional = clips.get(action);
        AnimationClip clip = directional == null ? null : directional.get(facing);
        if (clip == null) clip = fallback.get(action);
        if (clip == null) clip = fallback.get(AnimationAction.IDLE);
        return clip;
    }
}
