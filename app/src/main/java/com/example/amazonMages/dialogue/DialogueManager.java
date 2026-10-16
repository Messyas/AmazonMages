package com.example.amazonMages.dialogue;

/** Frame-based dialogue controller; the renderer can read visible characters and open progress. */
public final class DialogueManager {
    public interface EventListener { void onDialogueEvent(String eventId); }
    private DialogueSequence active;
    private EventListener listener;
    private int index;
    private int visibleCharacters;
    private int openingTicks;

    public void setEventListener(EventListener listener) { this.listener = listener; }
    public void start(DialogueSequence sequence) {
        active = sequence;
        index = 0;
        visibleCharacters = 0;
        openingTicks = 0;
    }
    public boolean isActive() { return active != null; }
    public DialogueLine currentLine() { return active == null ? null : active.lines.get(index); }
    public int visibleCharacters() { return visibleCharacters; }
    public float openingProgress() { return Math.min(1f, openingTicks / 15f); }
    public void update() {
        if (active == null) return;
        if (openingTicks < 15) openingTicks++;
        if (visibleCharacters < currentLine().text.length() && openingTicks >= 8) visibleCharacters++;
    }
    public void advance() {
        if (active == null) return;
        DialogueLine line = currentLine();
        if (visibleCharacters < line.text.length()) { visibleCharacters = line.text.length(); return; }
        if (line.eventId != null && listener != null) listener.onDialogueEvent(line.eventId);
        index++;
        if (index >= active.lines.size()) { active = null; return; }
        visibleCharacters = 0;
    }
}
