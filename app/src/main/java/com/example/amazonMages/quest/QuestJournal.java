package com.example.amazonMages.quest;

import java.util.HashMap;
import java.util.Map;

/** Runtime quest state. Persist a snapshot per save slot before using in production. */
public final class QuestJournal {
    private final Map<String, QuestDefinition> definitions = new HashMap<>();
    private final Map<String, QuestState> states = new HashMap<>();
    private final Map<String, Integer> progress = new HashMap<>();

    public void register(QuestDefinition quest) {
        if (definitions.containsKey(quest.id)) throw new IllegalArgumentException("Duplicate quest id");
        definitions.put(quest.id, quest);
        states.put(quest.id, QuestState.LOCKED);
        progress.put(quest.id, 0);
    }
    public QuestState state(String id) { return states.get(id); }
    public int progress(String id) { Integer value = progress.get(id); return value == null ? 0 : value; }
    public boolean offer(String id) { return change(id, QuestState.LOCKED, QuestState.AVAILABLE); }
    public boolean accept(String id) { return change(id, QuestState.AVAILABLE, QuestState.ACTIVE); }
    public void onEvent(String eventId) {
        if (eventId == null) return;
        for (QuestDefinition quest : definitions.values()) {
            if (states.get(quest.id) != QuestState.ACTIVE || !eventId.equals(quest.objectiveEvent)) continue;
            int count = Math.min(quest.targetCount, progress.get(quest.id) + 1);
            progress.put(quest.id, count);
            if (count == quest.targetCount) states.put(quest.id, QuestState.COMPLETED);
        }
    }
    /** Returns reward key once, then marks quest rewarded; caller must persist atomically with reward. */
    public String claimReward(String id) {
        if (states.get(id) != QuestState.COMPLETED) return null;
        states.put(id, QuestState.REWARDED);
        return definitions.get(id).unlockId;
    }
    private boolean change(String id, QuestState from, QuestState to) {
        if (states.get(id) != from) return false;
        states.put(id, to);
        return true;
    }
}
