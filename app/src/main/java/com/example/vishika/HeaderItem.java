package com.example.vishika;

import java.util.ArrayList;
import java.util.List;

public class HeaderItem implements ListItem {
    private String dateString;
    private boolean isExpanded;
    private List<LocationHistoryModel> childItems;

    public HeaderItem(String dateString) {
        this.dateString = dateString;
        this.isExpanded = false; // Start collapsed by default as requested in plan
        this.childItems = new ArrayList<>();
    }

    public String getDateString() {
        return dateString;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public List<LocationHistoryModel> getChildItems() {
        return childItems;
    }

    public void addChild(LocationHistoryModel item) {
        this.childItems.add(item);
    }

    @Override
    public int getItemType() {
        return TYPE_HEADER;
    }
}
