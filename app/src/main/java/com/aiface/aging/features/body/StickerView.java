package com.aiface.aging.features.body;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.MotionEventCompat;

import com.aiface.aging.R;
import com.aiface.aging.features.body.TouchListeners.ScaleGestureDetector;


public class StickerView extends View {
    private static final float BITMAP_SCALE = 0.1f;
    private static final String TAG = "StickerView";
    private float MAX_SCALE;
    private float MIN_SCALE;
    private int bottom;
    private final Context context;
    private Bitmap deleteBitmap;
    private int deleteBitmapHeight;
    private int deleteBitmapWidth;
    private DisplayMetrics dm;
    private Rect dst_delete;
    private Rect dst_flipV;
    private Rect dst_resize;
    private Rect dst_top;
    private float f3;
    private float f6;
    private Bitmap flipVBitmap;
    private int flipVBitmapHeight;
    private int flipVBitmapWidth;
    private double halfDiagonalLength;
    private float height;
    private float initialRotation;
    private boolean isClickable;
    private boolean isHorizonMirror;
    private boolean isInEdit;
    private boolean isInResize;
    private boolean isInSide;
    private boolean isPointerDown;
    private float lastLength;
    private float lastRotateDegree;
    private float lastX;
    private float lastY;
    private int left;
    private Paint localPaint;
    private Bitmap mBitmap;
    private ScaleGestureDetector.OnScaleGestureListener mOnClickListener;
    private int mScreenHeight;
    private int mScreenWidth;
    private Matrix matrix;
    private PointF mid;
    private float oldDis;
    private OperationListener operationListener;
    private float oringinWidth;
    private final float pointerLimitDis;
    private final float pointerZoomCoeff;
    private Bitmap resizeBitmap;
    private int resizeBitmapHeight;
    private int resizeBitmapWidth;
    private int right;
    private float rotation;
    private final long stickerId;
    private int top;
    private Bitmap topBitmap;
    private int topBitmapHeight;
    private int topBitmapWidth;
    private float width;
    private float xLength;
    private float yLength;


    public interface OperationListener {
        void onDeleteClick();

        void onEdit(StickerView stickerView, MotionEvent motionEvent);

        void onTop(StickerView stickerView);

        void onTouch();
    }

    public StickerView(Context context, AttributeSet attributeSet, Context context2) {
        super(context, attributeSet);
        this.isClickable = true;
        this.mid = new PointF();
        this.isPointerDown = false;
        this.pointerLimitDis = 20.0f;
        this.pointerZoomCoeff = 0.09f;
        this.isInResize = false;
        this.matrix = new Matrix();
        this.isInEdit = true;
        this.MIN_SCALE = 0.5f;
        this.MAX_SCALE = 1.2f;
        this.oringinWidth = 0.0f;
        this.isHorizonMirror = false;
        this.rotation = 0.0f;
        this.context = context2;
        this.stickerId = 0L;
        init();
    }

    public StickerView(Context context) {
        super(context);
        this.isClickable = true;
        this.mid = new PointF();
        this.isPointerDown = false;
        this.pointerLimitDis = 20.0f;
        this.pointerZoomCoeff = 0.09f;
        this.isInResize = false;
        this.matrix = new Matrix();
        this.isInEdit = true;
        this.MIN_SCALE = 0.5f;
        this.MAX_SCALE = 1.2f;
        this.oringinWidth = 0.0f;
        this.isHorizonMirror = false;
        this.rotation = 0.0f;
        this.context = context;
        this.stickerId = 0L;
        init();
    }

    public StickerView(Context context, AttributeSet attributeSet, int i, Context context2) {
        super(context, attributeSet, i);
        this.isClickable = true;
        this.mid = new PointF();
        this.isPointerDown = false;
        this.pointerLimitDis = 20.0f;
        this.pointerZoomCoeff = 0.09f;
        this.isInResize = false;
        this.matrix = new Matrix();
        this.isInEdit = true;
        this.MIN_SCALE = 0.5f;
        this.MAX_SCALE = 1.2f;
        this.oringinWidth = 0.0f;
        this.isHorizonMirror = false;
        this.rotation = 0.0f;
        this.context = context2;
        this.stickerId = 0L;
        init();
    }

    private void init() {
        this.dst_delete = new Rect();
        this.dst_resize = new Rect();
        this.dst_flipV = new Rect();
        this.dst_top = new Rect();
        Paint paint = new Paint();
        this.localPaint = paint;
//        paint.setColor(getResources().getColor(R.color.design_default_color_background));
        this.localPaint.setAntiAlias(true);
        this.localPaint.setDither(true);
        this.localPaint.setStyle(Paint.Style.STROKE);
        this.localPaint.setStrokeWidth(2.0f);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.dm = displayMetrics;
        this.mScreenWidth = displayMetrics.widthPixels;
        this.mScreenHeight = this.dm.heightPixels;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mBitmap != null) {
            float[] fArr = new float[9];
            this.matrix.getValues(fArr);
            this.xLength = fArr[2];
            this.yLength = fArr[5];
            float f = (fArr[0] * 0.0f) + (fArr[1] * 0.0f) + fArr[2];
            float f2 = (fArr[3] * 0.0f) + (fArr[4] * 0.0f) + fArr[5];
            this.f3 = (fArr[0] * this.mBitmap.getWidth()) + (fArr[1] * 0.0f) + fArr[2];
            float width = (fArr[3] * this.mBitmap.getWidth()) + (fArr[4] * 0.0f) + fArr[5];
            float height = (fArr[0] * 0.0f) + (fArr[1] * this.mBitmap.getHeight()) + fArr[2];
            this.f6 = (fArr[3] * 0.0f) + (fArr[4] * this.mBitmap.getHeight()) + fArr[5];
            float width2 = (fArr[0] * this.mBitmap.getWidth()) + (fArr[1] * this.mBitmap.getHeight()) + fArr[2];
            float width3 = (fArr[3] * this.mBitmap.getWidth()) + (fArr[4] * this.mBitmap.getHeight()) + fArr[5];
            canvas.save();
            canvas.drawBitmap(this.mBitmap, this.matrix, null);
            this.rotation = (float) Math.round(Math.atan2(fArr[1], fArr[0]) * 57.29577951308232d);
            this.dst_delete.left = (int) (this.f3 - (this.deleteBitmapWidth / 2));
            this.dst_delete.right = (int) (this.f3 + (this.deleteBitmapWidth / 2));
            this.dst_delete.top = (int) (width - (this.deleteBitmapHeight / 2));
            this.dst_delete.bottom = (int) (width + (this.deleteBitmapHeight / 2));
            this.dst_resize.left = (int) (width2 - this.resizeBitmapWidth);
            this.dst_resize.right = (int) (width2 + (this.resizeBitmapWidth / 2));
            this.dst_resize.top = (int) (width3 - this.resizeBitmapHeight);
            this.dst_resize.bottom = (int) (width3 + (this.resizeBitmapHeight / 2));
            this.dst_top.left = (int) (f - (this.topBitmapWidth / 2));
            this.dst_top.right = (int) (f + (this.topBitmapWidth / 2));
            this.dst_top.top = (int) (f2 - (this.topBitmapHeight / 2));
            this.dst_top.bottom = (int) (f2 + (this.topBitmapHeight / 2));
            this.dst_flipV.left = (int) (height - (this.flipVBitmapWidth / 2));
            this.dst_flipV.right = (int) (height + (this.flipVBitmapWidth / 2));
            this.dst_flipV.top = (int) (this.f6 - (this.flipVBitmapHeight / 2));
            this.dst_flipV.bottom = (int) (this.f6 + (this.flipVBitmapHeight / 2));
            if (this.isInEdit) {
                if (this.isClickable) {
                    canvas.drawBitmap(this.resizeBitmap, (Rect) null, this.dst_resize, (Paint) null);
                }
                canvas.restore();
            }
        }
    }

    public void setImageBitmap(Bitmap bitmap, float f, float f2, float f3) {
        this.initialRotation = f3;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        setBitmap(bitmap, f, f2, f3);
    }

    public void setBitmap(Bitmap bitmap, float f, float f2, float f3) {
        this.matrix.reset();
        float[] fArr = new float[9];
        this.matrix.getValues(fArr);
        float f4 = fArr[2];
        float f5 = fArr[5];
        this.mBitmap = bitmap;
        setDiagonalLength();
        initBitmaps();
        int width = this.mBitmap.getWidth();
        this.mBitmap.getHeight();
        this.oringinWidth = width;
        this.matrix.postScale(1.0f, 1.0f);
        this.matrix.postTranslate(f, f2);
        invalidate();
    }

    private void setDiagonalLength() {
        this.halfDiagonalLength = Math.hypot(this.mBitmap.getWidth(), this.mBitmap.getHeight()) / 2.0d;
    }

    private void initBitmaps() {
        if (this.mBitmap.getWidth() >= this.mBitmap.getHeight()) {
            float f = this.mScreenWidth / 8;
            if (this.mBitmap.getWidth() < f) {
                this.MIN_SCALE = 1.0f;
            } else {
                this.MIN_SCALE = (f * 1.0f) / this.mBitmap.getWidth();
            }
            int width = this.mBitmap.getWidth();
            int i = this.mScreenWidth;
            if (width > i) {
                this.MAX_SCALE = 1.0f;
            } else {
                this.MAX_SCALE = (i * 1.0f) / this.mBitmap.getWidth();
            }
        } else {
            float f2 = this.mScreenWidth / 8;
            if (this.mBitmap.getHeight() < f2) {
                this.MIN_SCALE = 1.0f;
            } else {
                this.MIN_SCALE = (f2 * 1.0f) / this.mBitmap.getHeight();
            }
            int height = this.mBitmap.getHeight();
            int i2 = this.mScreenWidth;
            if (height > i2) {
                this.MAX_SCALE = 1.0f;
            } else {
                this.MAX_SCALE = (i2 * 1.0f) / this.mBitmap.getHeight();
            }
        }
//        this.deleteBitmap = drawableToBitmap(this.context.getResources().getDrawable(R.drawable.zooms));
//        this.flipVBitmap = drawableToBitmap(this.context.getResources().getDrawable(R.drawable.zooms));
        this.resizeBitmap = drawableToBitmap(this.context.getResources().getDrawable(R.drawable.zooms));
//        this.topBitmap = drawableToBitmap(this.context.getResources().getDrawable(R.drawable.zooms));
//        this.deleteBitmapWidth = (int) (this.deleteBitmap.getWidth() * 0.1f);
//        this.deleteBitmapHeight = (int) (this.deleteBitmap.getHeight() * 0.1f);
        this.resizeBitmapWidth = (int) (this.resizeBitmap.getWidth() * 0.1f);
        this.resizeBitmapHeight = (int) (this.resizeBitmap.getHeight() * 0.1f);
//        this.flipVBitmapWidth = (int) (this.flipVBitmap.getWidth() * 0.1f);
//        this.flipVBitmapHeight = (int) (this.flipVBitmap.getHeight() * 0.1f);
//        this.topBitmapWidth = (int) (this.topBitmap.getWidth() * 0.1f);
//        this.topBitmapHeight = (int) (this.topBitmap.getHeight() * 0.1f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        boolean handled = true;
        if (this.isClickable) {
            float f = 1.0f;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (isInButton(event, dst_delete)) {
                        if (operationListener != null) {
                            operationListener.onDeleteClick();
                        }
                    } else if (isInResize(event)) {
                        isInResize = true;
                        lastRotateDegree = rotationToStartPoint(event);
                        midPointToStartPoint(event);
                        lastLength = diagonalLength(event);
                    } else if (isInButton(event, dst_flipV)) {
                        PointF localPointF = new PointF();
                        midDiagonalPoint(localPointF);
                        matrix.postScale(-1.0F, 1.0F, localPointF.x, localPointF.y);
                        invalidate();
                    } else if (isInButton(event, dst_top)) {
                        PointF pointF2 = new PointF();
                        midDiagonalPoint(pointF2);
                        this.matrix.postScale(1.0f, -1.0f, pointF2.x, pointF2.y);
                        invalidate();


                    } else if (isInBitmap(event)) {
                        this.isInSide = true;
                        this.lastX = event.getX(0);
                        this.lastY = event.getY(0);
                    } else {

                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (spacing(event) > 20.0f) {
                        this.oldDis = spacing(event);
                        this.isPointerDown = true;
                        midPointToStartPoint(event);
                    } else {
                        this.isPointerDown = false;
                    }
                    this.isInSide = false;
                    this.isInResize = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (this.isPointerDown) {
                        float spacing = spacing(event);
                        float f2 = (spacing == 0.0f || spacing < 20.0f) ? 1.0f : (((spacing / this.oldDis) - 1.0f) * 0.09f) + 1.0f;
                        float abs = (Math.abs(this.dst_flipV.left - this.dst_resize.left) * f2) / this.oringinWidth;
                        if ((abs > this.MIN_SCALE || f2 >= 1.0f) && (abs < this.MAX_SCALE || f2 <= 1.0f)) {
                            this.lastLength = diagonalLength(event);
                            f = f2;
                        }
                        this.matrix.postScale(f, f, this.mid.x, this.mid.y);
                        this.height *= f;
                        this.width *= f;
                        invalidate();
                    } else if (this.isInResize) {
                        this.lastRotateDegree = rotationToStartPoint(event);
                        float diagonalLength = diagonalLength(event) / this.lastLength;
                        if ((diagonalLength(event) / this.halfDiagonalLength <= this.MIN_SCALE && diagonalLength < 1.0f) || (diagonalLength(event) / this.halfDiagonalLength >= this.MAX_SCALE && diagonalLength > 1.0f)) {
                            if (!isInResize(event)) {
                                this.isInResize = false;
                            }
                        } else {
                            this.lastLength = diagonalLength(event);
                            f = diagonalLength;
                        }
                        this.matrix.postScale(f, f, this.mid.x, this.mid.y);
                        this.height *= f;
                        this.width *= f;
                        invalidate();
                    } else if (this.isInSide) {
                        float x = event.getX(0);
                        float y = event.getY(0);
                        this.matrix.postTranslate(x - this.lastX, y - this.lastY);
                        this.lastX = x;
                        this.lastY = y;
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    isInResize = false;
                    isInSide = false;
                    isPointerDown = false;
                    break;

            }
        }
        if (handled && operationListener != null) {
            operationListener.onEdit(this, event);
        }
        return handled;
    }


//    public StickerPropertyModel calculate(StickerPropertyModel stickerPropertyModel) {
//        float[] fArr = new float[9];
//        this.matrix.getValues(fArr);
//        float f = fArr[2];
//        float f2 = fArr[5];
//        Log.d(TAG, "tx : " + f + " ty : " + f2);
//        float f3 = fArr[0];
//        float f4 = fArr[3];
//        float sqrt = (float) Math.sqrt((double) ((f3 * f3) + (f4 * f4)));
//        Log.d(TAG, "rScale : " + sqrt);
//        float round = (float) Math.round(Math.atan2((double) fArr[1], (double) fArr[0]) * 57.29577951308232d);
//        Log.d(TAG, "rAngle : " + round);
//        PointF pointF = new PointF();
//        midDiagonalPoint(pointF);
//        Log.d(TAG, " width  : " + (this.mBitmap.getWidth() * sqrt) + " height " + (this.mBitmap.getHeight() * sqrt));
//        float f5 = pointF.x;
//        float f6 = pointF.y;
//        Log.d(TAG, "midX : " + f5 + " midY : " + f6);
//        stickerPropertyModel.setDegree((float) Math.toRadians((double) round));
//        stickerPropertyModel.setScaling((((float) this.mBitmap.getWidth()) * sqrt) / ((float) this.mScreenWidth));
//        stickerPropertyModel.setxLocation(f5 / ((float) this.mScreenWidth));
//        stickerPropertyModel.setyLocation(f6 / ((float) this.mScreenWidth));
//        stickerPropertyModel.setStickerId(this.stickerId);
//        if (this.isHorizonMirror) {
//            stickerPropertyModel.setHorizonMirror(1);
//        } else {
//            stickerPropertyModel.setHorizonMirror(2);
//        }
//        return stickerPropertyModel;
//    }

    private boolean isInBitmap(MotionEvent motionEvent) {
        float[] fArr = new float[9];
        this.matrix.getValues(fArr);
        float f = (fArr[0] * 0.0f) + (fArr[1] * 0.0f) + fArr[2];
        float f2 = (fArr[3] * 0.0f) + (fArr[4] * 0.0f) + fArr[5];
        float width = (fArr[0] * this.mBitmap.getWidth()) + (fArr[1] * 0.0f) + fArr[2];
        float width2 = (fArr[3] * this.mBitmap.getWidth()) + (fArr[4] * 0.0f) + fArr[5];
        float height = (fArr[0] * 0.0f) + (fArr[1] * this.mBitmap.getHeight()) + fArr[2];
        float height2 = (fArr[3] * 0.0f) + (fArr[4] * this.mBitmap.getHeight()) + fArr[5];
        return pointInRect(new float[]{f, width, (fArr[0] * this.mBitmap.getWidth()) + (fArr[1] * this.mBitmap.getHeight()) + fArr[2], height}, new float[]{f2, width2, (fArr[3] * this.mBitmap.getWidth()) + (fArr[4] * this.mBitmap.getHeight()) + fArr[5], height2}, motionEvent.getX(0), motionEvent.getY(0));
    }

    private boolean pointInRect(float[] fArr, float[] fArr2, float f, float f2) {
        double hypot = Math.hypot(fArr[0] - fArr[1], fArr2[0] - fArr2[1]);
        double hypot2 = Math.hypot(fArr[1] - fArr[2], fArr2[1] - fArr2[2]);
        double hypot3 = Math.hypot(fArr[3] - fArr[2], fArr2[3] - fArr2[2]);
        double hypot4 = Math.hypot(fArr[0] - fArr[3], fArr2[0] - fArr2[3]);
        double hypot5 = Math.hypot(f - fArr[0], f2 - fArr2[0]);
        double hypot6 = Math.hypot(f - fArr[1], f2 - fArr2[1]);
        double hypot7 = Math.hypot(f - fArr[2], f2 - fArr2[2]);
        double hypot8 = Math.hypot(f - fArr[3], f2 - fArr2[3]);
        double d = ((hypot + hypot5) + hypot6) / 2.0d;
        double d2 = ((hypot2 + hypot6) + hypot7) / 2.0d;
        double d3 = ((hypot3 + hypot7) + hypot8) / 2.0d;
        double d4 = ((hypot4 + hypot8) + hypot5) / 2.0d;
        return Math.abs((hypot * hypot2) - (((Math.sqrt((((d - hypot) * d) * (d - hypot5)) * (d - hypot6)) + Math.sqrt((((d2 - hypot2) * d2) * (d2 - hypot6)) * (d2 - hypot7))) + Math.sqrt((((d3 - hypot3) * d3) * (d3 - hypot7)) * (d3 - hypot8))) + Math.sqrt((((d4 - hypot4) * d4) * (d4 - hypot8)) * (d4 - hypot5)))) < 0.5d;
    }

    private boolean isInButton(MotionEvent motionEvent, Rect rect) {
        return motionEvent.getX(0) >= ((float) rect.left) && motionEvent.getX(0) <= ((float) rect.right) && motionEvent.getY(0) >= ((float) rect.top) && motionEvent.getY(0) <= ((float) rect.bottom);
    }

    private boolean isInResize(MotionEvent motionEvent) {
        this.left = this.dst_resize.left - 20;
        this.top = this.dst_resize.top - 20;
        this.right = this.dst_resize.right + 20;
        this.bottom = this.dst_resize.bottom + 20;
        return motionEvent.getX(0) >= ((float) this.left) && motionEvent.getX(0) <= ((float) this.right) && motionEvent.getY(0) >= ((float) this.top) && motionEvent.getY(0) <= ((float) this.bottom);
    }

    private void midPointToStartPoint(MotionEvent motionEvent) {
        float[] fArr = new float[9];
        this.matrix.getValues(fArr);
        this.mid.set(((((fArr[0] * 0.0f) + (fArr[1] * 0.0f)) + fArr[2]) + motionEvent.getX(0)) / 2.0f, ((((fArr[3] * 0.0f) + (fArr[4] * 0.0f)) + fArr[5]) + motionEvent.getY(0)) / 2.0f);
    }

    private void midDiagonalPoint(PointF pointF) {
        float[] fArr = new float[9];
        this.matrix.getValues(fArr);
        float f = (fArr[0] * 0.0f) + (fArr[1] * 0.0f) + fArr[2];
        float f2 = (fArr[3] * 0.0f) + (fArr[4] * 0.0f) + fArr[5];
        pointF.set((f + (((fArr[0] * this.mBitmap.getWidth()) + (fArr[1] * this.mBitmap.getHeight())) + fArr[2])) / 2.0f, (f2 + (((fArr[3] * this.mBitmap.getWidth()) + (fArr[4] * this.mBitmap.getHeight())) + fArr[5])) / 2.0f);
    }

    private float rotationToStartPoint(MotionEvent motionEvent) {
        float[] fArr = new float[9];
        this.matrix.getValues(fArr);
        float f = (fArr[0] * 0.0f) + (fArr[1] * 0.0f) + fArr[2];
        return (float) Math.toDegrees(Math.atan2(motionEvent.getY(0) - (((fArr[3] * 0.0f) + (fArr[4] * 0.0f)) + fArr[5]), motionEvent.getX(0) - f));
    }

    private float diagonalLength(MotionEvent motionEvent) {
        return (float) Math.hypot(motionEvent.getX(0) - this.mid.x, motionEvent.getY(0) - this.mid.y);
    }

    private float spacing(MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() == 2) {
            float x = motionEvent.getX(0) - motionEvent.getX(1);
            float y = motionEvent.getY(0) - motionEvent.getY(1);
            return (float) Math.sqrt((x * x) + (y * y));
        }
        return 0.0f;
    }

    public void setOperationListener(OperationListener operationListener) {
        this.operationListener = operationListener;
    }

    public void setInEdit(boolean z) {
        this.isInEdit = z;
        invalidate();
    }

    public Matrix getImageMatrix() {
        return this.matrix;
    }

    public void setStickerClickable(boolean z) {
        this.isClickable = z;
        invalidate();
    }

    public float getH() {
        return this.height;
    }

    public float getW() {
        return this.width;
    }

    public float getx() {
        return this.xLength;
    }

    public float gety() {
        return this.yLength;
    }

    @Override
    public float getRotation() {
        if (this.lastRotateDegree == 0.0f) {
            return this.initialRotation;
        }
        return -this.rotation;
    }

    public void flipX() {
        PointF pointF = new PointF();
        midDiagonalPoint(pointF);
        this.matrix.postScale(-1.0f, 1.0f, pointF.x, pointF.y);
        invalidate();
    }

    public void flipY() {
        PointF pointF = new PointF();
        midDiagonalPoint(pointF);
        this.matrix.postScale(1.0f, -1.0f, pointF.x, pointF.y);
        invalidate();
    }

    public Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap createBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return createBitmap;
    }
}
