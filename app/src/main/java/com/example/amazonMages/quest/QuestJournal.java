package com.example.amazonMages.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import com.example.amazonMages.save.QuestProgressEntity;

/** Runtime quest state. Persist a snapshot per save slot before using in production. */
public final class QuestJournal {
    private final Map<String, QuestDefinition> definitions = new HashMap<>();
    private final Map<String, QuestState> states = new HashMap<>();
    private final Map<String, Integer> progress = new HashMap<>();

    public List<QuestProgressEntity> snapshot(int slotId) {
        List<QuestProgressEntity> rows = new ArrayList<>();
        for (String id : definitions.keySet())
            rows.add(new QuestProgressEntity(slotId, id, states.get(id).name(), progress.get(id)));
        return rows;
    }
    public void restore(List<QuestProgressEntity> rows) {
        if (rows == null) return;
        for (QuestProgressEntity row : rows) {
            QuestDefinition definition = definitions.get(row.questId);
            if (definition == null) continue;
            try {
                QuestState state = QuestState.valueOf(row.state);
                states.put(row.questId, state);
                progress.put(row.questId, Math.max(0, Math.min(definition.targetCount, row.progress)));
            } catch (IllegalArgumentException | NullPointerException ignored) {
                // Unknown future/legacy quest state: retain default.
            }
        }
    }
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
