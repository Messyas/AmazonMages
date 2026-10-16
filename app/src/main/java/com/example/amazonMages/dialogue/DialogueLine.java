package com.example.amazonMages.dialogue;

/** Dialogue content is data, not UI or quest logic. */
public final class DialogueLine {
    public final String speakerId;
    public final String text;
    public final String expression;
    public final String eventId;

    public DialogueLine(String speakerId, String text, String expression, String eventId) {
        this.speakerId = speakerId;
        this.text = text;
        this.expression = expression;
        this.eventId = eventId;
    }
}
