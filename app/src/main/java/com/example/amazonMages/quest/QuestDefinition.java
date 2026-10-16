package com.example.amazonMages.quest;

/** Immutable quest metadata; IDs must remain stable across save versions. */
public final class QuestDefinition {
    public final String id;
    public final String title;
    public final String objectiveEvent;
    public final int targetCount;
    public final String unlockId;

    public QuestDefinition(String id, String title, String objectiveEvent, int targetCount, String unlockId) {
        if (id == null || id.isEmpty() || objectiveEvent == null || objectiveEvent.isEmpty() || targetCount <= 0)
            throw new IllegalArgumentException("Invalid quest definition");
        this.id = id;
        this.title = title;
        this.objectiveEvent = objectiveEvent;
        this.targetCount = targetCount;
        this.unlockId = unlockId;
    }
}
