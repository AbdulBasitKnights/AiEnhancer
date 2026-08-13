package com.aiface.aging.features.body.model;


public class FaceData {
    String[] GradianData;
    Integer icon;
    boolean isSelected;
    String item_name;

    public Integer getIcon() {
        return this.icon;
    }

    public String[] getGradianData() {
        return this.GradianData;
    }

    public void setGradianData(String[] strArr) {
        this.GradianData = strArr;
    }

    public void setIcon(Integer num) {
        this.icon = num;
    }

    public FaceData(String str, String[] strArr, boolean z) {
        this.isSelected = false;
        this.item_name = str;
        this.GradianData = strArr;
        this.isSelected = z;
    }

    public FaceData(String str, boolean z, Integer num) {
        this.isSelected = false;
        this.item_name = str;
        this.isSelected = z;
        this.icon = num;
    }

    public FaceData(String str, boolean z) {
        this.isSelected = false;
        this.item_name = str;
        this.isSelected = z;
    }

    public String getItem_name() {
        return this.item_name;
    }

    public void setItem_name(String str) {
        this.item_name = str;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean z) {
        this.isSelected = z;
    }
}
