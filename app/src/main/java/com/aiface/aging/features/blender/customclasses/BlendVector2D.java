package com.aiface.aging.features.blender.customclasses;

import android.graphics.PointF;

public class BlendVector2D extends PointF {

    public BlendVector2D() {
        super();
    }

    public BlendVector2D(float x, float y) {
        super(x, y);
    }

    public static float getAngle(BlendVector2D vector1, BlendVector2D vector2) {
        vector1.normalize();
        vector2.normalize();
        double degrees = (180.0 / Math.PI) * (Math.atan2(vector2.y, vector2.x) - Math.atan2(vector1.y, vector1.x));
        return (float) degrees;
    }

    public void normalize() {
        float length = (float) Math.sqrt(x * x + y * y);
        x /= length;
        y /= length;
    }
}