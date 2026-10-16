package com.example.amazonMages.dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DialogueSequence {
    public final String id;
    public final List<DialogueLine> lines;

    public DialogueSequence(String id, List<DialogueLine> lines) {
        if (id == null || lines == null || lines.isEmpty()) throw new IllegalArgumentException("Empty dialogue");
        this.id = id;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }
}
