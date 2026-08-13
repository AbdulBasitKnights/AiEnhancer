package com.aiface.aging.features.body.controls;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.ScaleGestureDetectorCompat;

import java.util.Arrays;


public class ScaleImage extends AppCompatImageView implements ScaleGestureDetector.OnScaleGestureListener {
    private final float MAX_SCALE;
    private final float MIN_SCALE;
    private final int RESET_DURATION;
    private final RectF bounds;
    private float calculatedMaxScale;
    private float calculatedMinScale;
    private boolean isScaleMode;
    private PointF last;
    private Matrix matrix;
    private float[] matrixValues;
    private int previousPointerCount;
    private boolean restrictBounds;
    ScaleAndMoveInterface scaleAndMoveInterface;
    private float scaleBy;
    private ScaleGestureDetector scaleDetector;
    private int sizeOfMinSide;
    private float startScale;
    private float[] startValues;
    TouchInterface touchInterface;


    public interface ScaleAndMoveInterface {
        void move(float f, float f2, float f3, float f4);
    }


    public interface TouchInterface {
        void touch(int i, float f, float f2, float f3);
    }

    public ScaleImage(Context context) {
        this(context, null);
    }

    public ScaleImage(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ScaleImage(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.MIN_SCALE = 1.0f;
        this.MAX_SCALE = 6.0f;
        this.RESET_DURATION = 200;
        this.matrix = new Matrix();
        this.matrixValues = new float[9];
        this.startValues = null;
        this.calculatedMinScale = 1.0f;
        this.calculatedMaxScale = 6.0f;
        this.bounds = new RectF();
        this.restrictBounds = true;
        this.isScaleMode = true;
        this.last = new PointF(0.0f, 0.0f);
        this.startScale = 1.0f;
        this.scaleBy = 1.0f;
        this.previousPointerCount = 1;
        onCreate(context);
    }

    private void onCreate(Context context) {
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, this);
        this.scaleDetector = scaleGestureDetector;
        ScaleGestureDetectorCompat.setQuickScaleEnabled(scaleGestureDetector, false);
        super.setScaleType(ScaleType.FIT_CENTER);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        super.setScaleType(scaleType);
        this.startValues = null;
    }

    private void updateBounds(float[] fArr) {
        if (getDrawable() != null) {
            this.bounds.set(fArr[2], fArr[5], (getDrawable().getIntrinsicWidth() * fArr[0]) + fArr[2], (getDrawable().getIntrinsicHeight() * fArr[4]) + fArr[5]);
        }
    }

    public float getCurrentDisplayedWidth() {
        if (getDrawable() != null) {
            return getDrawable().getIntrinsicWidth() * this.matrixValues[0];
        }
        return 0.0f;
    }

    public float getCurrentDisplayedHeight() {
        if (getDrawable() != null) {
            return getDrawable().getIntrinsicHeight() * this.matrixValues[4];
        }
        return 0.0f;
    }

    private void setStartValues() {
        this.startValues = new float[9];
        new Matrix(getImageMatrix()).getValues(this.startValues);
        float[] fArr = this.startValues;
        this.calculatedMinScale = fArr[0] * 1.0f;
        this.calculatedMaxScale = fArr[0] * 6.0f;
        Drawable drawable = getDrawable();
        if (drawable != null) {
            if (drawable.getIntrinsicHeight() < drawable.getIntrinsicWidth()) {
                this.sizeOfMinSide = drawable.getIntrinsicHeight();
            } else {
                this.sizeOfMinSide = drawable.getIntrinsicWidth();
            }
        }
    }

    public void calculateXYOfImage(int i, float f, float f2) {
        float f3 = this.matrixValues[0];
        this.touchInterface.touch(i, ((f - this.bounds.left) - getPaddingLeft()) / f3, ((f2 - this.bounds.top) - getPaddingTop()) / f3, f3 / this.calculatedMinScale);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return super.onTouchEvent(motionEvent);
        }
        getScaleType();
        ScaleType scaleType = ScaleType.MATRIX;
        if (this.startValues == null) {
            setStartValues();
        }
        this.matrix.set(getImageMatrix());
        this.matrix.getValues(this.matrixValues);
        updateBounds(this.matrixValues);
        this.scaleDetector.onTouchEvent(motionEvent);
        if (motionEvent.getActionMasked() == 0 || motionEvent.getPointerCount() != this.previousPointerCount) {
            this.last.set(this.scaleDetector.getFocusX(), this.scaleDetector.getFocusY());
            if (this.touchInterface != null) {
                int actionMasked = motionEvent.getActionMasked();
                if (actionMasked == 0) {
                    calculateXYOfImage(0, this.scaleDetector.getFocusX(), this.scaleDetector.getFocusY());
                } else if (actionMasked != 5) {
                    if (actionMasked == 6 && motionEvent.getPointerCount() == 1) {
                        this.touchInterface.touch(2, -1.0f, -1.0f, 0.0f);
                    }
                } else if (motionEvent.getPointerCount() == 2) {
                    this.touchInterface.touch(2, -1.0f, -1.0f, 0.0f);
                }
            }
        } else if (motionEvent.getActionMasked() == 2) {
            if (motionEvent.getPointerCount() > 1) {
                if (this.isScaleMode) {
                    float focusX = this.scaleDetector.getFocusX();
                    float focusY = this.scaleDetector.getFocusY();
                    this.matrix.postTranslate(getXDistance(focusX, this.last.x), getYDistance(focusY, this.last.y));
                    Matrix matrix = this.matrix;
                    float f = this.scaleBy;
                    matrix.postScale(f, f, focusX, focusY);
                    setImageMatrix(this.matrix);
                    if (this.scaleAndMoveInterface != null) {
                        this.matrix.getValues(this.matrixValues);
                        ScaleAndMoveInterface scaleAndMoveInterface = this.scaleAndMoveInterface;
                        float[] fArr = this.matrixValues;
                        scaleAndMoveInterface.move(fArr[2], fArr[5], fArr[0], fArr[4]);
                    }
                    this.last.set(focusX, focusY);
                }
            } else if (this.touchInterface != null && (Math.abs(this.last.x - this.scaleDetector.getFocusX()) > 5.0f || Math.abs(this.last.y - this.scaleDetector.getFocusY()) > 5.0f)) {
                calculateXYOfImage(1, this.scaleDetector.getFocusX(), this.scaleDetector.getFocusY());
            }
        }
        if (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) {
            this.scaleBy = 1.0f;
            if (this.touchInterface != null) {
                calculateXYOfImage(2, motionEvent.getX(), motionEvent.getY());
            }
            resetImage();
        }
        this.previousPointerCount = motionEvent.getPointerCount();
        return true;
    }

    public float getPointXOnScreen(float f) {
        getImageMatrix().getValues(this.matrixValues);
        return this.matrixValues[2] + getPaddingLeft() + (this.matrixValues[0] * f);
    }

    public float getPointYOnScreen(float f) {
        getImageMatrix().getValues(this.matrixValues);
        return this.matrixValues[5] + getPaddingTop() + (this.matrixValues[4] * f);
    }

    public Bitmap getBitmap() {
        if (this.startValues == null) {
            setStartValues();
            Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
            float abs = Math.abs(this.startValues[2]);
            float[] fArr = this.startValues;
            int i = this.sizeOfMinSide;
            return Bitmap.createBitmap(bitmap, (int) (abs / fArr[0]), (int) (Math.abs(fArr[5]) / this.startValues[0]), i, i);
        }
        this.matrix.set(getImageMatrix());
        this.matrix.getValues(this.matrixValues);
        int i2 = (int) ((this.sizeOfMinSide * this.startValues[0]) / this.matrixValues[0]);
        Bitmap bitmap2 = ((BitmapDrawable) getDrawable()).getBitmap();
        float abs2 = Math.abs(this.matrixValues[2]);
        float[] fArr2 = this.matrixValues;
        return Bitmap.createBitmap(bitmap2, (int) (abs2 / fArr2[0]), (int) (Math.abs(fArr2[5]) / this.matrixValues[0]), i2, i2, getImageMatrix(), true);
    }

    public void setOnTouchInterface(TouchInterface touchInterface) {
        this.touchInterface = touchInterface;
    }

    public void setOnScaleAndMoveInterface(ScaleAndMoveInterface scaleAndMoveInterface) {
        this.scaleAndMoveInterface = scaleAndMoveInterface;
    }

    private void resetImage() {
        if (this.matrixValues[0] < this.startValues[0]) {
            reset();
        } else {
            center();
        }
    }

    private void center() {
        animateTranslationX();
        animateTranslationY();
    }

    public void reset() {
        animateToStartMatrix();
    }

    private void animateToStartMatrix() {
        final Matrix matrix = new Matrix(getImageMatrix());
        matrix.getValues(this.matrixValues);
        float[] fArr = this.startValues;
        float f = fArr[0];
        float[] fArr2 = this.matrixValues;
        final float f2 = f - fArr2[0];
        final float f3 = fArr[4] - fArr2[4];
        final float f4 = fArr[2] - fArr2[2];
        final float f5 = fArr[5] - fArr2[5];
        ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            final Matrix activeMatrix;
            final float[] values = new float[9];

            {
                this.activeMatrix = new Matrix(ScaleImage.this.getImageMatrix());
            }

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                this.activeMatrix.set(matrix);
                this.activeMatrix.getValues(this.values);
                float[] fArr3 = this.values;
                fArr3[2] = fArr3[2] + (f4 * floatValue);
                fArr3[5] = fArr3[5] + (f5 * floatValue);
                fArr3[0] = fArr3[0] + (f2 * floatValue);
                fArr3[4] = fArr3[4] + (f3 * floatValue);
                this.activeMatrix.setValues(fArr3);
                ScaleImage.this.setImageMatrix(this.activeMatrix);
            }
        });
        ofFloat.setDuration(200L);
        ofFloat.start();
    }

    private void animateTranslationX() {
        if (getCurrentDisplayedWidth() <= getWidth()) {
            float width = ((getWidth() - getCurrentDisplayedWidth()) / 2.0f) - getPaddingLeft();
            if (this.bounds.left != width) {
                animateMatrixIndex(2, width);
            }
        } else if (this.bounds.left + getPaddingLeft() > 0.0f) {
            animateMatrixIndex(2, 0 - getPaddingLeft());
        } else if (this.bounds.right < getWidth() - getPaddingRight()) {
            animateMatrixIndex(2, ((this.bounds.left + getWidth()) - this.bounds.right) - getPaddingRight());
        }
    }

    private void animateTranslationY() {
        if (getCurrentDisplayedHeight() <= getHeight()) {
            float height = ((getHeight() - getCurrentDisplayedHeight()) / 2.0f) - getPaddingTop();
            if (this.bounds.top != height) {
                animateMatrixIndex(5, height);
            }
        } else if (this.bounds.top + getPaddingTop() > 0.0f) {
            animateMatrixIndex(5, 0 - getPaddingTop());
        } else if (this.bounds.bottom < getHeight() - getPaddingBottom()) {
            animateMatrixIndex(5, ((this.bounds.top + getHeight()) - this.bounds.bottom) - getPaddingBottom());
        }
    }

    private void animateMatrixIndex(final int i, float f) {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(this.matrixValues[i], f);
        ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            Matrix current = new Matrix();
            final float[] values = new float[9];

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.current.set(ScaleImage.this.getImageMatrix());
                this.current.getValues(this.values);
                this.values[i] = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                this.current.setValues(this.values);
                ScaleImage.this.setImageMatrix(this.current);
            }
        });
        ofFloat.setDuration(200L);
        ofFloat.start();
    }

    private float getXDistance(float f, float f2) {
        float f3 = f - f2;
        if (this.restrictBounds) {
            f3 = getRestrictedXDistance(f3);
        }
        if (this.bounds.right + f3 < 0.0f) {
            return -this.bounds.right;
        }
        return this.bounds.left + f3 > ((float) getWidth()) ? getWidth() - this.bounds.left : f3;
    }

    private float getRestrictedXDistance(float f) {
        float width;
        float f2;
        float f3;
        if (getCurrentDisplayedWidth() >= getWidth()) {
            if (this.bounds.left <= 0.0f && this.bounds.left + f > 0.0f && !this.scaleDetector.isInProgress()) {
                f3 = this.bounds.left;
                return -f3;
            } else if (this.bounds.right < getWidth() || this.bounds.right + f >= getWidth() || this.scaleDetector.isInProgress()) {
                return f;
            } else {
                width = getWidth();
                f2 = this.bounds.right;
                return width - f2;
            }
        } else if (this.scaleDetector.isInProgress()) {
            return f;
        } else {
            if (this.bounds.left >= 0.0f && this.bounds.left + f < 0.0f) {
                f3 = this.bounds.left;
                return -f3;
            } else if (this.bounds.right > getWidth() || this.bounds.right + f <= getWidth()) {
                return f;
            } else {
                width = getWidth();
                f2 = this.bounds.right;
                return width - f2;
            }
        }
    }

    private float getYDistance(float f, float f2) {
        float f3 = f - f2;
        if (this.restrictBounds) {
            f3 = getRestrictedYDistance(f3);
        }
        if (this.bounds.bottom + f3 < 0.0f) {
            return -this.bounds.bottom;
        }
        return this.bounds.top + f3 > ((float) getHeight()) ? getHeight() - this.bounds.top : f3;
    }

    private float getRestrictedYDistance(float f) {
        float height;
        float f2;
        float f3;
        if (getCurrentDisplayedHeight() >= getHeight()) {
            if (this.bounds.top <= 0.0f && this.bounds.top + f > 0.0f && !this.scaleDetector.isInProgress()) {
                f3 = this.bounds.top;
                return -f3;
            } else if (this.bounds.bottom < getHeight() || this.bounds.bottom + f >= getHeight() || this.scaleDetector.isInProgress()) {
                return f;
            } else {
                height = getHeight();
                f2 = this.bounds.bottom;
                return height - f2;
            }
        } else if (this.scaleDetector.isInProgress()) {
            return f;
        } else {
            if (this.bounds.top >= 0.0f && this.bounds.top + f < 0.0f) {
                f3 = this.bounds.top;
                return -f3;
            } else if (this.bounds.bottom > getHeight() || this.bounds.bottom + f <= getHeight()) {
                return f;
            } else {
                height = getHeight();
                f2 = this.bounds.bottom;
                return height - f2;
            }
        }
    }

    public void setScaleMode(boolean z, boolean z2) {
        this.isScaleMode = z;
        getImageMatrix().getValues(this.matrixValues);
        if (z2) {
            float[] fArr = this.startValues;
            if (fArr == null || !Arrays.equals(fArr, this.matrixValues)) {
                resetToFitCenter();
            }
        }
    }

    public void updateMatrixValues(float[] fArr, float[] fArr2, float[] fArr3) {
        if (getScaleType() != ScaleType.MATRIX) {
            super.setScaleType(ScaleType.MATRIX);
        }
        if (this.startValues == null) {
            this.startValues = fArr2;
            this.calculatedMinScale = fArr[0];
            this.calculatedMaxScale = fArr[1];
            this.sizeOfMinSide = (int) fArr[2];
        }
        float[] fArr4 = (float[]) fArr3.clone();
        this.matrixValues = fArr4;
        this.matrix.setValues(fArr4);
        updateBounds(this.matrixValues);
        setImageMatrix(this.matrix);
    }

    public float[] getInitialData() {
        return new float[]{this.calculatedMinScale, this.calculatedMaxScale, this.sizeOfMinSide};
    }

    public float getCalculatedMinScale() {
        if (this.startValues == null) {
            setStartValues();
        }
        return this.calculatedMinScale;
    }

    public float[] getStartValues() {
        float[] fArr = this.startValues;
        if (fArr != null) {
            return fArr;
        }
        float[] fArr2 = new float[9];
        getImageMatrix().getValues(fArr2);
        return fArr2;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        float scaleFactor = this.startScale * scaleGestureDetector.getScaleFactor();
        float[] fArr = this.matrixValues;
        float f = scaleFactor / fArr[0];
        this.scaleBy = f;
        float f2 = f * fArr[0];
        float f3 = this.calculatedMinScale;
        if (f2 < f3) {
            this.scaleBy = f3 / fArr[0];
        } else {
            float f4 = this.calculatedMaxScale;
            if (f2 > f4) {
                this.scaleBy = f4 / fArr[0];
            }
        }
        return false;
    }

    public void resetToFitCenter() {
        setScaleType(ScaleType.FIT_CENTER);
        invalidate();
    }

    public void resetToFitCenterManual() {
        int intrinsicWidth = getDrawable().getIntrinsicWidth();
        int intrinsicHeight = getDrawable().getIntrinsicHeight();
        float width = (getWidth() - getPaddingLeft()) - getPaddingRight();
        float f = intrinsicWidth;
        float height = (getHeight() - getPaddingTop()) - getPaddingBottom();
        float f2 = intrinsicHeight;
        getImageMatrix().getValues(this.matrixValues);
        this.matrixValues[0] = Math.min(width / f, height / f2);
        float[] fArr = this.matrixValues;
        fArr[4] = fArr[0];
        fArr[2] = (width - (fArr[0] * f)) / 2.0f;
        fArr[5] = (height - (fArr[4] * f2)) / 2.0f;
        getImageMatrix().setValues(this.matrixValues);
        invalidate();
        this.startValues = null;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        this.startScale = this.matrixValues[0];
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        this.scaleBy = 1.0f;
    }
}
