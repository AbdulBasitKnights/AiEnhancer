package com.aiface.aging.utils.multitouchlistener;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import androidx.core.view.MotionEventCompat;



/* compiled from: MainActivityTest */

public class MultiTouchListener implements OnTouchListener {
    public static final int INVALID_POINTER_ID = -1;
    public boolean isRotateEnabled;
    public boolean isScaleEnabled;
    public boolean isTranslateEnabled;
    public int mActivePointerId;
    public float mPrevX;
    public float mPrevY;
    public ScaleGestureDetector mScaleGestureDetector;
    public float maximumScale;
    public float minimumScale;
    //////////////////
    private OnDoubleTapListener onDoubleTapListener;
    private OnImageViewTouchListener onImageViewTouchListener;
    private GestureDetector gestureDetector;
    public boolean bringViewToFront;
    private View view;

    /* compiled from: MainActivityTest */
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

    ////////Constructor single Imageview
    public MultiTouchListener(Context context, boolean bringViewToFront) {
        this.bringViewToFront = bringViewToFront;
        this.isRotateEnabled = true;
        this.isTranslateEnabled = true;
        this.isScaleEnabled = true;
        this.minimumScale = 0.1f;
        this.maximumScale = 10.0f;
        this.mActivePointerId = INVALID_POINTER_ID;
        this.mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
        this.gestureDetector = new GestureDetector(context, new GestureListener());
    }

    ////////Constructor Double Frame

    public MultiTouchListener(Context context, View view, boolean bringViewToFront,
                              OnDoubleTapListener onDoubleTapListener, OnImageViewTouchListener onImageViewTouchListener) {
        this.view = view;
        this.bringViewToFront = bringViewToFront;
        this.onDoubleTapListener = onDoubleTapListener;
        this.onImageViewTouchListener = onImageViewTouchListener;
        this.gestureDetector = new GestureDetector(context, new GestureListener());
        /////////////////
        this.isRotateEnabled = true;
        this.isTranslateEnabled = true;
        this.isScaleEnabled = true;
        this.minimumScale = 0.1f;
        this.maximumScale = 10.0f;
        this.mActivePointerId = INVALID_POINTER_ID;
        this.mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
    }

    public MultiTouchListener(Context context, View view, boolean bringViewToFront,
                              OnDoubleTapListener onDoubleTapListener) {
        this.view = view;
        this.bringViewToFront = bringViewToFront;
        this.onDoubleTapListener = onDoubleTapListener;
        this.onImageViewTouchListener = onImageViewTouchListener;
        this.gestureDetector = new GestureDetector(context, new GestureListener());
        /////////////////
        this.isRotateEnabled = true;
        this.isTranslateEnabled = true;
        this.isScaleEnabled = true;
        this.minimumScale = 0.1f;
        this.maximumScale = 10.0f;
        this.mActivePointerId = INVALID_POINTER_ID;
        this.mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
    }

    public MultiTouchListener(Context context, View view, boolean bringViewToFront) {
        this.view = view;
        this.bringViewToFront = bringViewToFront;
        this.gestureDetector = new GestureDetector(context, new GestureListener());
        /////////////////
        this.isRotateEnabled = true;
        this.isTranslateEnabled = true;
        this.isScaleEnabled = true;
        this.minimumScale = 0.1f;
        this.maximumScale = 10.0f;
        this.mActivePointerId = INVALID_POINTER_ID;
        this.mScaleGestureDetector = new ScaleGestureDetector(new ScaleGestureListener());
    }

    /* compiled from: MainActivityTest */
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
            info.deltaScale = MultiTouchListener.this.isScaleEnabled ? detector.getScaleFactor() : 1.0f;
            if (MultiTouchListener.this.isRotateEnabled) {
                angle = Vector2D.getAngle(this.mPrevSpanVector, detector.getCurrentSpanVector());
            } else {
                angle = 0.0f;
            }
            info.deltaAngle = angle;
            if (MultiTouchListener.this.isTranslateEnabled) {
                angle = detector.getFocusX() - this.mPivotX;
            } else {
                angle = 0.0f;
            }
            info.deltaX = angle;
            if (MultiTouchListener.this.isTranslateEnabled) {
                f = detector.getFocusY() - this.mPivotY;
            }
            info.deltaY = f;
            info.pivotX = this.mPivotX;
            info.pivotY = this.mPivotY;
            info.minimumScale = MultiTouchListener.this.minimumScale;
            info.maximumScale = MultiTouchListener.this.maximumScale;
            MultiTouchListener.move(view, info);
            return false;
        }
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

    public static void move(View view, TransformInfo info) {
        computeRenderOffset(view, info.pivotX, info.pivotY);
        adjustTranslation(view, info.deltaX, info.deltaY);
        float scale = Math.max(info.minimumScale, Math.min(info.maximumScale, view.getScaleX() * info.deltaScale));
        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setRotation(adjustAngle(view.getRotation() + info.deltaAngle));
    }

    public static void adjustTranslation(View view, float deltaX, float deltaY) {
        float[] deltaVector = new float[]{deltaX, deltaY};
        view.getMatrix().mapVectors(deltaVector);
        view.setTranslationX(view.getTranslationX() + deltaVector[0]);
        view.setTranslationY(view.getTranslationY() + deltaVector[1]);
    }

    public static void computeRenderOffset(View view, float pivotX, float pivotY) {
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

    public boolean onTouch(View view, MotionEvent event) {
        try {
            this.mScaleGestureDetector.onTouchEvent(view, event);
            this.gestureDetector.onTouchEvent(event);
            this.onImageViewTouchListener.onImageTouchListener(view);
            if (!this.isTranslateEnabled) {
                return true;
            }

            if (bringViewToFront)
                view.bringToFront();

            int action = event.getAction();
            int pointerIndex;
            switch (event.getActionMasked() & action) {
                case MotionEvent.ACTION_DOWN:
                    this.mPrevX = event.getX();
                    this.mPrevY = event.getY();
                    this.mActivePointerId = event.getPointerId(0);
                    break;
                case MotionEvent.ACTION_UP:
                    this.mActivePointerId = INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_MOVE:
                    pointerIndex = event.findPointerIndex(this.mActivePointerId);
                    if (pointerIndex != INVALID_POINTER_ID) {
                        float currX = event.getX(pointerIndex);
                        float currY = event.getY(pointerIndex);
                        if (!this.mScaleGestureDetector.isInProgress()) {
                            adjustTranslation(view, currX - this.mPrevX, currY - this.mPrevY);
                            break;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    this.mActivePointerId = INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    pointerIndex = (MotionEventCompat.ACTION_POINTER_INDEX_MASK & action) >> 8;
                    if (event.getPointerId(pointerIndex) == this.mActivePointerId) {
                        int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        this.mPrevX = event.getX(newPointerIndex);
                        this.mPrevY = event.getY(newPointerIndex);
                        this.mActivePointerId = event.getPointerId(newPointerIndex);
                        break;
                    }
                    break;
            }
            return true;
        } catch (IllegalArgumentException e) {
        }
        return false;

    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (onDoubleTapListener != null) {
                onDoubleTapListener.onDoubleTapListner(view);
            }
            return super.onDoubleTap(e);
        }
    }
}