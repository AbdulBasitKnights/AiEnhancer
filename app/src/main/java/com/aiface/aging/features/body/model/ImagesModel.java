package com.aiface.aging.features.body.model;


public class ImagesModel {
    boolean isSeletced = false;
    String name;
    String path;

    public boolean isSeletced() {
        return this.isSeletced;
    }

    public void setSeletced(boolean z) {
        this.isSeletced = z;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String str) {
        this.name = str;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String str) {
        this.path = str;
    }
}
