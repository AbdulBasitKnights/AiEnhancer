package com.aiface.aging.features.body;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import androidx.core.app.NotificationCompat;


import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;
import com.aiface.aging.R;
import com.aiface.aging.features.body.activities.ImageEditingER;
import com.aiface.aging.features.body.controls.ScaleImage;
import com.aiface.aging.features.body.fragment.BodyEditFragment;
import com.aiface.aging.features.body.inerfaces.MenuClick;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;


public class viewRefineData implements BodyEditFragment.BackPressed, ScaleImage.TouchInterface {
    public Bitmap currentOriginalBitmap;
    public boolean isTouching;
    private float lastx;
    private float lasty;
//    public ImageEditing mActivity;
    public ImageEditingER mActivity;
    public int mCircleRadius;
    private int mColums;
    public Bitmap mCurrentBitmap;
    private Paint mInterruptPaint;
    public Bitmap mLastBitmap;
    private ArcSeekBar mMenuRefine;
    public Paint mPaint;
    private int mRow;
    public ScaleImage mScaleImage;
    private int maxSize;
    MenuClick menuClick;
    public Canvas myCanvasView;
    private SeekBar seek;
    public float stepX;
    private float stepY;
    private float[] verts;
    private int mCurrentState = -1;
    private List<refineHistoryData> mRefineHistory = new ArrayList();
    int seek_pos = 50;
    private ProgressListener onprogressChangeListener = new ProgressListener() {
        @Override
        public void invoke(int i) {
            viewRefineData.this.myCanvasView.drawBitmap(viewRefineData.this.mLastBitmap, 0.0f, 0.0f, (Paint) null);
            viewRefineData viewrefinedata = viewRefineData.this;
            viewrefinedata.mCircleRadius = (int) (viewrefinedata.stepX * 3.0f * ((i / 50.0f) + 1.0f));
            viewRefineData.this.myCanvasView.drawCircle(viewRefineData.this.currentOriginalBitmap.getWidth() / 2, viewRefineData.this.currentOriginalBitmap.getHeight() / 2, viewRefineData.this.mCircleRadius, viewRefineData.this.mPaint);
            viewRefineData.this.mScaleImage.invalidate();
        }
    };
    private ProgressListener onstartTouchListener = new ProgressListener() {
        @Override
        public void invoke(int i) {
            if (viewRefineData.this.mLastBitmap.isRecycled()) {
                viewRefineData viewrefinedata = viewRefineData.this;
                viewrefinedata.mLastBitmap = viewrefinedata.mCurrentBitmap.copy(Bitmap.Config.ARGB_8888, true);
            }
            viewRefineData.this.mScaleImage.setOnTouchInterface(null);
        }
    };
    private ProgressListener onstoptTouchListener = new ProgressListener() {
        @Override
        public void invoke(int i) {
            viewRefineData.this.myCanvasView.drawBitmap(viewRefineData.this.mLastBitmap, 0.0f, 0.0f, (Paint) null);
            if (!viewRefineData.this.isTouching) {
                viewRefineData.this.mLastBitmap.recycle();
            }
            viewRefineData.this.mScaleImage.invalidate();
            viewRefineData.this.mScaleImage.setOnTouchInterface(viewRefineData.this);
        }
    };
    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    static int access$010(viewRefineData viewrefinedata) {
        int i = viewrefinedata.mCurrentState;
        viewrefinedata.mCurrentState = i - 1;
        return i;
    }

//    public viewRefineData(Bitmap bitmap, ImageEditing imageEditing, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick) {
    public viewRefineData(Bitmap bitmap, ImageEditingER imageEditing, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick) {
        this.currentOriginalBitmap = bitmap;
        this.mActivity = imageEditing;
        this.mScaleImage = scaleImage;
        this.mMenuRefine = arcSeekBar;
        this.seek = seekBar;
        this.menuClick = menuClick;
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                viewRefineData.this.createMesh();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        viewRefineData.this.mActivity.isBlocked = false;
                        viewRefineData.this.onCreate();
                    }
                });
            }
        }).start();
        this.mActivity.li_undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewRefineData.this.mCurrentState > -1) {
                    refineHistoryData refinehistorydata = (refineHistoryData) viewRefineData.this.mRefineHistory.get(viewRefineData.this.mCurrentState);
                    viewRefineData.this.changeMesh(refinehistorydata.xLeft, refinehistorydata.yTop, refinehistorydata.xRight, refinehistorydata.yBottom, refinehistorydata.values, -1);
                    viewRefineData.access$010(viewRefineData.this);
                    if (viewRefineData.this.mCurrentState > -1) {
                        viewRefineData.this.mActivity.li_undo.setImageDrawable(viewRefineData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_undo));
                        viewRefineData.this.mActivity.li_redo.setImageDrawable(viewRefineData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
                    } else {
                        viewRefineData.this.mActivity.li_undo.setImageDrawable(viewRefineData.this.mActivity.getResources().getDrawable(R.drawable.ic_disable_undo));
                        viewRefineData.this.mActivity.li_redo.setImageDrawable(viewRefineData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
                    }
                    return;
                }
                viewRefineData.this.mActivity.li_undo.setImageDrawable(viewRefineData.this.mActivity.getResources().getDrawable(R.drawable.ic_disable_undo));
            }
        });
        this.mActivity.li_redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewRefineData.this.mCurrentState + 1 < viewRefineData.this.mRefineHistory.size()) {
                    int i = viewRefineData.this.mCurrentState + 1;
                    viewRefineData.this.mCurrentState = i;
                    refineHistoryData refinehistorydata = (refineHistoryData) viewRefineData.this.mRefineHistory.get(i);
                    viewRefineData.this.changeMesh(refinehistorydata.xLeft, refinehistorydata.yTop, refinehistorydata.xRight, refinehistorydata.yBottom, refinehistorydata.values, 1);
                    viewRefineData.this.mActivity.li_undo.setImageDrawable(viewRefineData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_undo));
                    viewRefineData.this.mActivity.li_redo.setImageDrawable(viewRefineData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
                    return;
                }
                viewRefineData.this.mActivity.li_redo.setImageDrawable(viewRefineData.this.mActivity.getResources().getDrawable(R.drawable.ic_disable_redo));
            }
        });
    }

    public void onCreate() {
        Paint paint = new Paint();
        this.mPaint = paint;
        paint.setStrokeWidth(3.0f);
        this.mPaint.setStyle(Paint.Style.STROKE);
//        this.mPaint.setFlags(1);
        this.mPaint.setFlags(ANTI_ALIAS_FLAG);
        this.mPaint.setColor(-1);
        Paint paint2 = new Paint();
        this.mInterruptPaint = paint2;
        paint2.setStrokeWidth(3.0f);
        this.mInterruptPaint.setStyle(Paint.Style.STROKE);
//        this.mInterruptPaint.setFlags(1);
        this.mPaint.setFlags(ANTI_ALIAS_FLAG);
        this.mInterruptPaint.setColor(-1);
        this.mInterruptPaint.setPathEffect(new DashPathEffect(new float[]{15.0f, 10.0f}, 0.0f));
        this.mCurrentBitmap = this.currentOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap createBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        this.mLastBitmap = createBitmap;
        createBitmap.recycle();
        this.myCanvasView = new Canvas(this.mCurrentBitmap);
        this.mScaleImage.setOnTouchInterface(this);
        this.mActivity.li_preview.setOnTouchListener(
                this.mActivity.createToolCompareTouchListener(
                        this.mScaleImage,
                        () -> viewRefineData.this.currentOriginalBitmap,
                        () -> viewRefineData.this.mCurrentBitmap
                )
        );
        this.mMenuRefine.setProgress(50);
        this.mMenuRefine.setOnStartTrackingTouch(this.onstartTouchListener);
        this.mMenuRefine.setOnStopTrackingTouch(this.onstoptTouchListener);
        this.mMenuRefine.setOnProgressChangedListener(this.onprogressChangeListener);
        this.mScaleImage.setImageBitmap(this.mCurrentBitmap);
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                viewRefineData.this.seek_pos = i;
                viewRefineData.this.myCanvasView.drawBitmap(viewRefineData.this.mLastBitmap, 0.0f, 0.0f, (Paint) null);
                viewRefineData viewrefinedata = viewRefineData.this;
                viewrefinedata.mCircleRadius = (int) (viewrefinedata.stepX * 3.0f * ((i / 50.0f) + 1.0f));
                viewRefineData.this.myCanvasView.drawCircle(viewRefineData.this.currentOriginalBitmap.getWidth() / 2, viewRefineData.this.currentOriginalBitmap.getHeight() / 2, viewRefineData.this.mCircleRadius, viewRefineData.this.mPaint);
                viewRefineData.this.mScaleImage.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (viewRefineData.this.mLastBitmap.isRecycled()) {
                    viewRefineData viewrefinedata = viewRefineData.this;
                    viewrefinedata.mLastBitmap = viewrefinedata.mCurrentBitmap.copy(Bitmap.Config.ARGB_8888, true);
                }
                viewRefineData.this.mScaleImage.setOnTouchInterface(null);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                viewRefineData.this.myCanvasView.drawBitmap(viewRefineData.this.mLastBitmap, 0.0f, 0.0f, (Paint) null);
                if (!viewRefineData.this.isTouching) {
                    viewRefineData.this.mLastBitmap.recycle();
                }
                viewRefineData.this.mScaleImage.invalidate();
                viewRefineData.this.mScaleImage.setOnTouchInterface(viewRefineData.this);
            }
        });
    }

    private void close(boolean z) {
        this.mCurrentBitmap.recycle();
        this.mRefineHistory.clear();
        this.mScaleImage.setOnTouchInterface(null);
        this.mMenuRefine.setOnStartTrackingTouch(null);
        this.mMenuRefine.setOnStopTrackingTouch(null);
        this.mMenuRefine.setOnProgressChangedListener(null);
        this.mActivity.li_preview.setOnTouchListener(this.mActivity.previewTouchListener);
        this.mScaleImage.setImageBitmap(this.mActivity.mCurrentBitmap);
        this.mActivity.li_undo.setOnClickListener(this.mActivity.undoClick);
        this.mActivity.li_redo.setOnClickListener(this.mActivity.redoClick);
    }

    @Override
    public void onBackPressed(boolean z) {
        if (z) {
            this.mActivity.saveEffect(this.mCurrentBitmap);
        } else {
            close(z);
        }
    }

    @Override
    public void touch(int i, float f, float f2, float f3) {
        Log.w(NotificationCompat.CATEGORY_MESSAGE, "touch:" + i);
        this.menuClick.onMenuClick(true);
        if (i == 0) {
            this.isTouching = true;
            this.lastx = f;
            this.lasty = f2;
            this.mLastBitmap = this.mCurrentBitmap.copy(Bitmap.Config.ARGB_8888, true);
            this.myCanvasView.drawCircle(f, f2, this.mCircleRadius, this.mPaint);
            this.mScaleImage.invalidate();
        } else if (i == 1) {
            if (this.isTouching) {
                this.myCanvasView.drawBitmap(this.mLastBitmap, 0.0f, 0.0f, (Paint) null);
                this.myCanvasView.drawCircle(this.lastx, this.lasty, this.mCircleRadius, this.mPaint);
                this.myCanvasView.drawCircle(f, f2, this.mCircleRadius, this.mPaint);
                this.myCanvasView.drawLine(this.lastx, this.lasty, f, f2, this.mInterruptPaint);
                this.mScaleImage.invalidate();
            }
        } else if (i == 2) {
            if (!this.mLastBitmap.isRecycled()) {
                this.myCanvasView.drawBitmap(this.mLastBitmap, 0.0f, 0.0f, (Paint) null);
                this.mLastBitmap.recycle();
            }
            if (this.isTouching && f != -1.0f) {
                float degrees = (float) Math.toDegrees(Math.atan2(this.lasty - f2, f - this.lastx));
                float sqrt = ((float) Math.sqrt(Math.pow(this.lasty - f2, 2.0d) + Math.pow(this.lastx - f, 2.0d))) / this.maxSize;
                double d = degrees;
                float cos = this.stepX * sqrt * ((float) Math.cos(Math.toRadians(d)));
                float sin = (-sqrt) * this.stepY * ((float) Math.sin(Math.toRadians(d)));
                int max = Math.max((int) ((this.lastx - this.mCircleRadius) / this.stepX), 0);
                int min = Math.min(((int) ((this.lastx + this.mCircleRadius) / this.stepX)) + 1, this.mColums);
                int max2 = Math.max((int) ((this.lasty - this.mCircleRadius) / this.stepY), 0);
                int min2 = Math.min(((int) ((this.lasty + this.mCircleRadius) / this.stepY)) + 1, this.mRow);
                if (min - max <= 0 || min2 - max2 <= 0) {
                    this.isTouching = false;
                    return;
                }
                this.mCurrentState++;
                while (this.mRefineHistory.size() > this.mCurrentState) {
                    List<refineHistoryData> list = this.mRefineHistory;
                    list.remove(list.size() - 1);
                }
                changeMesh(max, max2, min, min2, cos, sin);
            }
            this.isTouching = false;
        }
    }

    public void createMesh() {
        if (this.currentOriginalBitmap.getWidth() > this.currentOriginalBitmap.getHeight()) {
            this.mColums = 100;
            this.stepX = this.currentOriginalBitmap.getWidth() / this.mColums;
            this.mRow = (int) (this.currentOriginalBitmap.getHeight() / this.stepX);
            this.stepY = this.currentOriginalBitmap.getHeight() / this.mRow;
        } else {
            this.mRow = 100;
            this.stepY = this.currentOriginalBitmap.getHeight() / this.mRow;
            this.mColums = (int) (this.currentOriginalBitmap.getWidth() / this.stepY);
            this.stepX = this.currentOriginalBitmap.getWidth() / this.mColums;
        }
        this.mCircleRadius = (int) (this.stepX * 6.0f);
        this.maxSize = Math.max(this.currentOriginalBitmap.getHeight(), this.currentOriginalBitmap.getWidth()) / 2;
        int i = (this.mColums + 1) * (this.mRow + 1) * 2;
        this.verts = new float[i];
        for (int i2 = 0; i2 < i; i2 += 2) {
            float[] fArr = this.verts;
            int i3 = i2 / 2;
            int i4 = this.mColums + 1;
            fArr[i2] = (i3 % i4) * this.stepX;
            fArr[i2 + 1] = (i3 / i4) * this.stepY;
        }
    }

    private void changeMesh(int i, int i2, int i3, int i4, float f, float f2) {
        float[][][] fArr = (float[][][]) Array.newInstance(Float.TYPE, (i4 - i2) + 1, (i3 - i) + 1, 2);
        for (int i5 = i2; i5 <= i4; i5++) {
            for (int i6 = i; i6 <= i3; i6++) {
                int i7 = (((this.mColums + 1) * i5) + i6) * 2;
                float[] fArr2 = this.verts;
                float f3 = fArr2[i7];
                int i8 = i7 + 1;
                float f4 = fArr2[i8];
                float abs = Math.abs(this.lastx - f3);
                float abs2 = Math.abs(this.lasty - f4);
                float sqrt = (float) Math.sqrt((abs * abs) + (abs2 * abs2));
                float f5 = this.mCircleRadius;
                if (sqrt < f5) {
                    float f6 = (f5 - sqrt) / f5;
                    if (i6 == 0 || i6 == this.mColums) {
                        float[] fArr3 = this.verts;
                        float f7 = f6 * f2;
                        fArr3[i8] = fArr3[i8] + f7;
                        fArr[i5 - i2][i6 - i][1] = f7;
                    } else if (i5 == 0 || i5 == this.mRow) {
                        float[] fArr4 = this.verts;
                        float f8 = f6 * f;
                        fArr4[i7] = fArr4[i7] + f8;
                        fArr[i5 - i2][i6 - i][0] = f8;
                    } else {
                        float[] fArr5 = this.verts;
                        float f9 = f * f6;
                        fArr5[i7] = fArr5[i7] + f9;
                        float f10 = f2 * f6;
                        fArr5[i8] = fArr5[i8] + f10;
                        int i9 = i5 - i2;
                        int i10 = i6 - i;
                        fArr[i9][i10][0] = f9;
                        fArr[i9][i10][1] = f10;
                    }
                }
            }
        }
        this.mRefineHistory.add(new refineHistoryData(i, i2, i3, i4, fArr));
        this.myCanvasView.drawBitmapMesh(this.currentOriginalBitmap, this.mColums, this.mRow, this.verts, 0, null, 0, null);
        this.mScaleImage.invalidate();
    }


    public void changeMesh(int i, int i2, int i3, int i4, float[][][] fArr, int i5) {
        for (int i6 = i2; i6 <= i4; i6++) {
            for (int i7 = i; i7 <= i3; i7++) {
                int i8 = (((this.mColums + 1) * i6) + i7) * 2;
                float[] fArr2 = this.verts;
                int i9 = i6 - i2;
                int i10 = i7 - i;
                float f = i5;
                fArr2[i8] = fArr2[i8] + (fArr[i9][i10][0] * f);
                int i11 = i8 + 1;
                fArr2[i11] = fArr2[i11] + (fArr[i9][i10][1] * f);
            }
        }
        this.myCanvasView.drawBitmapMesh(this.currentOriginalBitmap, this.mColums, this.mRow, this.verts, 0, null, 0, null);
        this.mScaleImage.invalidate();
    }


    public class refineHistoryData {
        float[][][] values;
        int xLeft;
        int xRight;
        int yBottom;
        int yTop;

        refineHistoryData(int i, int i2, int i3, int i4, float[][][] fArr) {
            this.values = fArr;
            this.xLeft = i;
            this.xRight = i3;
            this.yBottom = i4;
            this.yTop = i2;
        }
    }
}
