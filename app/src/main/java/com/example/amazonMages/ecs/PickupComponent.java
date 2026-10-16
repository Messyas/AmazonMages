package com.example.amazonMages.ecs;

public final class PickupComponent implements Component {
    public final String itemId;
    public final int quantity;
    public PickupComponent(String itemId, int quantity) {
        if (itemId == null || itemId.trim().isEmpty() || quantity < 1)
            throw new IllegalArgumentException("pickup");
        this.itemId = itemId;
        this.quantity = quantity;
    }
}
