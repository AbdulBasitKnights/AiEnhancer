package com.aiface.aging.utils.multitouchlistener;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class SpiralMultiTouchListener implements OnTouchListener {
    private static final int INVALID_POINTER_ID = -1;
    private final View spiralBg;
    private final View spiralFg;
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector mScaleGestureDetector;
    private float prevX;
    private float prevY;
    private int activePointerId1;
    private int activePointerId2;
    private boolean isRotateEnabled;
    private boolean isScaleEnabled;
    private boolean isTranslateEnabled;
    private float minimumScale;
    private float maximumScale;

    public class TransformInfo {
        public float deltaAngle;
        public float deltaScale;
        public float deltaX;
        public float deltaY;
        public float maximumScale;
        public float minimumScale;
        public float pivotX;
        public float pivotY;

        public TransformInfo() {
        }
    }

    public SpiralMultiTouchListener(Context context, View spiralBg, View spiralFg) {
        this.spiralBg = spiralBg;
        this.spiralFg = spiralFg;
        this.gestureDetector = new GestureDetector(context, new GestureListener());
        this.mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());

        this.isRotateEnabled = true;
        this.isTranslateEnabled = true;
        this.isScaleEnabled = true;
        this.minimumScale = 0.1f;
        this.maximumScale = 10.0f;
        this.activePointerId1 = INVALID_POINTER_ID;
        this.activePointerId2 = INVALID_POINTER_ID;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        try {
            this.mScaleGestureDetector.onTouchEvent(view, event);
            this.gestureDetector.onTouchEvent(event);

            int action = event.getAction();
            int pointerIndex;

            switch (event.getActionMasked() & action) {
                case MotionEvent.ACTION_DOWN:
                    this.prevX = event.getX();
                    this.prevY = event.getY();
                    this.activePointerId1 = event.getPointerId(0);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (activePointerId1 != INVALID_POINTER_ID) {
                        this.activePointerId2 = event.getPointerId(1);
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    if (event.getPointerId(event.getActionIndex()) == activePointerId2) {
                        activePointerId2 = INVALID_POINTER_ID;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    this.activePointerId1 = INVALID_POINTER_ID;
                    this.activePointerId2 = INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    this.activePointerId1 = INVALID_POINTER_ID;
                    this.activePointerId2 = INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_MOVE:
                    pointerIndex = event.findPointerIndex(this.activePointerId1);
                    if (pointerIndex != INVALID_POINTER_ID) {
                        float currX = event.getX(pointerIndex);
                        float currY = event.getY(pointerIndex);

                        if (!this.mScaleGestureDetector.isInProgress()) {
                            adjustTranslation(spiralFg, currX - this.prevX, currY - this.prevY);
                            adjustTranslation(spiralBg, currX - this.prevX, currY - this.prevY);
                        }

                        if (activePointerId2 != INVALID_POINTER_ID) {
                            pointerIndex = event.findPointerIndex(activePointerId2);
                            if (pointerIndex != INVALID_POINTER_ID) {
                                float currX2 = event.getX(pointerIndex);
                                float currY2 = event.getY(pointerIndex);

                                if (!this.mScaleGestureDetector.isInProgress()) {
                                    adjustTranslation(spiralFg, currX2 - this.prevX, currY2 - this.prevY);
                                    adjustTranslation(spiralBg, currX2 - this.prevX, currY2 - this.prevY);
                                }
                            }
                        }
                    }
                    break;
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void computeRenderOffset(View view, float pivotX, float pivotY) {
        if (view.getPivotX() != pivotX || view.getPivotY() != pivotY) {
            float[] prevPoint = new float[]{0.0f, 0.0f};
            view.getMatrix().mapPoints(prevPoint);
            view.setPivotX(pivotX);
            view.setPivotY(pivotY);
            float[] currPoint = new float[]{0.0f, 0.0f};
            view.getMatrix().mapPoints(currPoint);
            float offsetY = currPoint[1] - prevPoint[1];
            view.setTranslationX(view.getTranslationX() - (currPoint[0] - prevPoint[0]));
            view.setTranslationY(view.getTranslationY() - offsetY);
        }
    }

    public void move(View view, TransformInfo info) {
        computeRenderOffset(view, info.pivotX, info.pivotY);
        adjustTranslation(view, info.deltaX, info.deltaY);
        float scale = Math.max(info.minimumScale, Math.min(info.maximumScale, view.getScaleX() * info.deltaScale));
        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setRotation(adjustAngle(view.getRotation() + info.deltaAngle));
    }

    public static float adjustAngle(float degrees) {
        if (degrees > 180.0f) {
            return degrees - 360.0f;
        }
        if (degrees < -180.0f) {
            return degrees + 360.0f;
        }
        return degrees;
    }

    private void adjustTranslation(View view, float deltaX, float deltaY) {
        float[] deltaVector = new float[]{deltaX, deltaY};
        view.getMatrix().mapVectors(deltaVector);

        view.setTranslationX(view.getTranslationX() + deltaVector[0]);
        view.setTranslationY(view.getTranslationY() + deltaVector[1]);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Handle double-tap here if needed
            return true;
        }
    }

    public class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        public float mPivotX;
        public float mPivotY;
        public Vector2D mPrevSpanVector;

        public ScaleGestureListener() {
            this.mPrevSpanVector = new Vector2D();
        }

        public boolean onScaleBegin(View view, ScaleGestureDetector detector) {
            this.mPivotX = detector.getFocusX();
            this.mPivotY = detector.getFocusY();
            this.mPrevSpanVector.set(detector.getCurrentSpanVector());
            return true;
        }

        public boolean onScale(View view, ScaleGestureDetector detector) {
            float angle;
            float f = 0.0f;
            TransformInfo info = new TransformInfo();
            info.deltaScale = isScaleEnabled ? detector.getScaleFactor() : 1.0f;
            if (isRotateEnabled) {
                angle = Vector2D.getAngle(mPrevSpanVector, detector.getCurrentSpanVector());
            } else {
                angle = 0.0f;
            }
            info.deltaAngle = angle;
            if (isTranslateEnabled) {
                angle = detector.getFocusX() - this.mPivotX;
            } else {
                angle = 0.0f;
            }
            info.deltaX = angle;
            if (isTranslateEnabled) {
                f = detector.getFocusY() - this.mPivotY;
            }
            info.deltaY = f;
            info.pivotX = this.mPivotX;
            info.pivotY = this.mPivotY;
            info.minimumScale = minimumScale;
            info.maximumScale = maximumScale;

            move(spiralFg, info);
            move(spiralBg, info);
            return false;
        }
    }
}
