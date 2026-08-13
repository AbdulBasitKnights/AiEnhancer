package com.aiface.aging.utils;

import android.graphics.Bitmap;

import java.util.HashMap;

public class ResultContainer {
    private static ResultContainer instance = null;
    // frame
    private HashMap<String, Bitmap> mDecodedImageMap = new HashMap<>();

    public static ResultContainer getInstance() {
        if (instance == null) {
            instance = new ResultContainer();
        }

        return instance;
    }

    private ResultContainer() {

    }
    public void putImage(String key, Bitmap bitmap) {
        mDecodedImageMap.put(key, bitmap);
    }

    public Bitmap getImage(String key) {
        return mDecodedImageMap.get(key);
    }
}
