package com.benbanking.api.dto;

import java.util.List;

public class AlertListResponse {

    private final List<AlertResponse> items;
    private final long unreadCount;

    public AlertListResponse(List<AlertResponse> items, long unreadCount) {
        this.items = items;
        this.unreadCount = unreadCount;
    }

    public List<AlertResponse> getItems() {
        return items;
    }

    public long getUnreadCount() {
        return unreadCount;
    }
}
