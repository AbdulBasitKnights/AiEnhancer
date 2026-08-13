package com.aiface.aging.features.body;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;
import com.aiface.aging.R;
import com.aiface.aging.features.body.activities.ImageEditingER;
import com.aiface.aging.features.body.controls.ScaleImage;
import com.aiface.aging.features.body.controls.undoRedoData;
import com.aiface.aging.features.body.fragment.BodyEditFragment;
import com.aiface.aging.features.body.inerfaces.MenuClick;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class viewWaistData implements BodyEditFragment.BackPressed, View.OnTouchListener, ScaleImage.ScaleAndMoveInterface, undoRedoData.PhotoLoadResponse {
    public int column;
    public int currentId;
    public Bitmap currentOriginalBitmap;
    public ConstraintLayout customLayouts;
    private List<waistHistoryData> history = new ArrayList();
    private float initialWidth;
    private float initialX;
    private float initialY;
    private float initialheight;
    public boolean isEditmode;

    public int lastId;
    public long lastProgressvalue;
    private float lastX;
    private float lastY;
//    public ImageEditing mActivity;
    public ImageEditingER mActivity;
    private ImageView mBottomImage;
    private ImageView mCenterImage;
    public Bitmap mCurrentBitmap;
    private int mHeight;

    public int mIdRequisite;
    private ImageView mLeftImage;
    private int mMinHeight;
    private int mMinWidth;
    private ImageView mRightImage;
    private ImageView mTopImage;
    private int mWidth;
    private FrameLayout mainView;
    public float[] matrixValues = new float[9];
    public float[] maxVertesValues;
    private MenuClick menuClick;
    public Canvas myCanvasView;
    private ProgressListener onprogressChangeListener = new ProgressListener() {
        public void invoke(final int i) {
            if (viewWaistData.this.isEditmode) {
                new Thread(new Runnable() {
                    public void run() {
                        Log.d("Waist-Data", "onProgressChangeListener; i: " + i);
                        Log.d("Waist-Data", "originalMeshArea-Width: " + viewWaistData.this.originalMeshArea.getWidth());
                        long j = viewWaistData.this.lastProgressvalue - ((long) viewWaistData.this.seek_pos);
                        viewWaistData.this.lastProgressvalue = (long) viewWaistData.this.seek_pos;
                        for (int i = 0; i < viewWaistData.this.vertsNumbers; i += 2) {
                            float[] fArr = viewWaistData.this.vertexesmesh;
                            fArr[i] = fArr[i] + ((viewWaistData.this.maxVertesValues[i] * ((float) j)) / 50.0f);
                        }
                        Bitmap createBitmap = Bitmap.createBitmap(viewWaistData.this.originalMeshArea.getWidth(), viewWaistData.this.originalMeshArea.getHeight(), Bitmap.Config.ARGB_8888);
                        if (!createBitmap.isMutable()) {
                            Bitmap copy = createBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            createBitmap.recycle();
                            createBitmap = copy;
                        }
                        Paint paint = null;
                        new Canvas(createBitmap).drawBitmapMesh(viewWaistData.this.originalMeshArea, viewWaistData.this.column, viewWaistData.this.row, viewWaistData.this.vertexesmesh, 0, (int[]) null, 0, paint);
                        viewWaistData.this.myCanvasView.drawBitmap(createBitmap, (float) viewWaistData.this.xStart, (float) viewWaistData.this.yStart, paint);
                        createBitmap.recycle();
                        viewWaistData.this.mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                viewWaistData.this.waistImageView.invalidate();
                            }
                        });
                    }
                }).run();
            }
        }
    };
    private ProgressListener onstartTouchListener = new ProgressListener() {
        public void invoke(int i) {
            float f;
            float f2;
            int i2 = viewWaistData.this.seek_pos;
            Log.d("Waist-Data", "onStartTouchListener; i2: " + i2);
            viewWaistData.this.customLayouts.setVisibility(View.INVISIBLE);
            if (!viewWaistData.this.isEditmode) {
                viewWaistData.this.lastProgressvalue = 0;
                viewWaistData.this.waistImageView.getImageMatrix().getValues(viewWaistData.this.matrixValues);
                viewWaistData viewwaistdata = viewWaistData.this;
                int i3 = 0;
                viewwaistdata.xStart = Math.round((viewwaistdata.customLayouts.getTranslationX() - viewWaistData.this.matrixValues[2]) / viewWaistData.this.matrixValues[0]);
                viewWaistData viewwaistdata2 = viewWaistData.this;
                viewwaistdata2.yStart = Math.round((viewwaistdata2.customLayouts.getTranslationY() - viewWaistData.this.matrixValues[5]) / viewWaistData.this.matrixValues[4]);
                int round = Math.round(((float) viewWaistData.this.customLayouts.getWidth()) / viewWaistData.this.matrixValues[0]);
                int round2 = Math.round(((float) viewWaistData.this.customLayouts.getHeight()) / viewWaistData.this.matrixValues[4]);
                float f3 = 2.0f;
                float f4 = ((float) round) / 2.0f;
                float f5 = ((float) round2) / 2.0f;
                if (viewWaistData.this.xStart < 0) {
                    round += viewWaistData.this.xStart;
                    f = ((float) viewWaistData.this.xStart) + f4;
                    viewWaistData.this.xStart = 0;
                } else {
                    f = f4;
                }
                if (viewWaistData.this.yStart < 0) {
                    f2 = (float) (-viewWaistData.this.yStart);
                    round2 += viewWaistData.this.yStart;
                    viewWaistData.this.yStart = 0;
                } else {
                    f2 = 0.0f;
                }
                int min = Math.min(round, viewWaistData.this.mCurrentBitmap.getWidth() - viewWaistData.this.xStart);
                int min2 = Math.min(round2, viewWaistData.this.mCurrentBitmap.getHeight() - viewWaistData.this.yStart);
                if (min >= 50 && min2 >= 50) {
                    viewWaistData viewwaistdata3 = viewWaistData.this;
                    viewwaistdata3.originalMeshArea = Bitmap.createBitmap(viewwaistdata3.mCurrentBitmap, viewWaistData.this.xStart, viewWaistData.this.yStart, min, min2);
                    viewWaistData.this.column = 10;
                    float f6 = ((float) min) / ((float) viewWaistData.this.column);
                    viewWaistData.this.row = Math.min(min2 / 10, 30);
                    float f7 = ((float) min2) / ((float) viewWaistData.this.row);
                    viewWaistData viewwaistdata4 = viewWaistData.this;
                    viewwaistdata4.vertsNumbers = (viewwaistdata4.column + 1) * 2 * (viewWaistData.this.row + 1);
                    viewWaistData viewwaistdata5 = viewWaistData.this;
                    viewwaistdata5.vertexesmesh = new float[viewwaistdata5.vertsNumbers];
                    viewWaistData viewwaistdata6 = viewWaistData.this;
                    viewwaistdata6.maxVertesValues = new float[viewwaistdata6.vertsNumbers];
                    while (i3 < viewWaistData.this.vertsNumbers) {
                        int i4 = i3 / 2;
                        int i5 = i4 % (viewWaistData.this.column + 1);
                        float f8 = ((float) i5) * f6;
                        float f9 = ((float) (i4 / (viewWaistData.this.column + 1))) * f7;
                        viewWaistData.this.vertexesmesh[i3] = f8;
                        viewWaistData.this.vertexesmesh[i3 + 1] = f9;
                        if (!(i5 == 0 || i5 == viewWaistData.this.column)) {
                            viewWaistData.this.maxVertesValues[i3] = ((((float) Math.sin((((double) (f9 + f2)) * 3.141592653589793d) / ((double) (f5 * f3)))) * f6) * (f8 - f)) / f4;
                        }
                        i3 += 2;
                        f3 = 2.0f;
                    }
                    viewWaistData.this.isEditmode = true;
                }
            }
        }
    };
    private ProgressListener onstoptTouchListener = new ProgressListener() {
        public void invoke(int i) {
            Log.d("Waist-Data", "onStopTouchListener; i: " + i);
            if (!viewWaistData.this.isEditmode) {
                viewWaistData.this.seekBarView.setProgress(0);
            }
            viewWaistData.this.customLayouts.setVisibility(View.VISIBLE);
        }
    };
    public Bitmap originalMeshArea;
    public int row;
    public SeekBar seek;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };
    public ArcSeekBar seekBarView;
    int seek_pos = 0;
    public float[] vertexesmesh;
    public int vertsNumbers;
    public ScaleImage waistImageView;
    public int xStart;
    public int yStart;

//    public viewWaistData(Bitmap bitmap, ImageEditing imageEditing, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick2) {
    public viewWaistData(Bitmap bitmap, ImageEditingER imageEditing, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick2) {
        this.currentOriginalBitmap = bitmap;
        this.mActivity = imageEditing;
        this.waistImageView = scaleImage;
        this.seekBarView = arcSeekBar;
        this.seek = seekBar;
        this.menuClick = menuClick2;
        onCreate();
        this.mActivity.li_undo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                viewWaistData.this.saveFloatsValue();
                int access$100 = viewWaistData.this.mIdRequisite;
                if (access$100 != viewWaistData.this.currentId || access$100 <= 0) {
                    viewWaistData.this.mActivity.li_undo.setImageDrawable(viewWaistData.this.mActivity.getResources().getDrawable(R.drawable.ic_disable_undo));
                    return;
                }
                int i = access$100 - 1;
                int unused = viewWaistData.this.mIdRequisite = i;
                viewWaistData viewwaistdata = viewWaistData.this;
                undoRedoData.getBitmapFromDisk(access$100, i, "tool_" + (viewWaistData.this.mIdRequisite + 1) + ".png", viewwaistdata, viewwaistdata.mActivity);
                viewWaistData.this.mActivity.li_undo.setImageDrawable(viewWaistData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_undo));
                viewWaistData.this.mActivity.li_redo.setImageDrawable(viewWaistData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
            }
        });
        this.mActivity.li_redo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int access$100 = viewWaistData.this.mIdRequisite;
                if (access$100 != viewWaistData.this.currentId || access$100 >= viewWaistData.this.lastId) {
                    viewWaistData.this.mActivity.li_redo.setImageDrawable(viewWaistData.this.mActivity.getResources().getDrawable(R.drawable.ic_disable_redo));
                } else if (!viewWaistData.this.isEditmode) {
                    int access$1002 = viewWaistData.this.mIdRequisite;
                    int i = access$1002 + 1;
                    int unused = viewWaistData.this.mIdRequisite = i;
                    viewWaistData viewwaistdata = viewWaistData.this;
                    undoRedoData.getBitmapFromDisk(access$1002, i, "tool_" + viewWaistData.this.mIdRequisite + ".png", viewwaistdata, viewwaistdata.mActivity);
                    viewWaistData.this.mActivity.li_undo.setImageDrawable(viewWaistData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_undo));
                    viewWaistData.this.mActivity.li_redo.setImageDrawable(viewWaistData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
                } else {
                    viewWaistData.this.saveFloatsValue();
                }
            }
        });
    }

    private void onCreate() {
        this.mainView = (FrameLayout) this.mActivity.findViewById(R.id.main_frameView);
        this.mActivity.isBlocked = false;
        drawCustomView();
        this.mCurrentBitmap = this.currentOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        this.myCanvasView = new Canvas(this.mCurrentBitmap);
        this.mActivity.li_preview.setOnTouchListener(
                this.mActivity.createToolCompareTouchListener(
                        this.waistImageView,
                        () -> viewWaistData.this.currentOriginalBitmap,
                        () -> viewWaistData.this.mCurrentBitmap
                )
        );
        this.seekBarView.setProgress(50);
        this.seekBarView.setOnStartTrackingTouch(this.onstartTouchListener);
        this.seekBarView.setOnStopTrackingTouch(this.onstoptTouchListener);
        this.seekBarView.setOnProgressChangedListener(this.onprogressChangeListener);
        this.waistImageView.setImageBitmap(this.mCurrentBitmap);
        this.waistImageView.setOnScaleAndMoveInterface(this);
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                viewWaistData.this.seek_pos = i;
                seekBar.setProgress(i);
                viewWaistData.this.seekBarView.getOnProgressChangedListener().invoke(i);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                viewWaistData.this.seekBarView.getOnStartTrackingTouch().invoke(viewWaistData.this.seek_pos);
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!viewWaistData.this.isEditmode) {
                    viewWaistData.this.seekBarView.setProgress(0);
                }
                viewWaistData.this.customLayouts.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressLint("ResourceType")
    private void drawCustomView() {
        this.customLayouts = new ConstraintLayout(this.mActivity);
        ImageView imageView = new ImageView(this.mActivity);
        this.mTopImage = imageView;
        imageView.setId(14);
        this.mTopImage.setImageResource(R.drawable.iv_up_arrow);
        ImageView imageView2 = new ImageView(this.mActivity);
        this.mBottomImage = imageView2;
        imageView2.setId(8);
        this.mBottomImage.setImageResource(R.drawable.iv_down_arrow);
        ImageView imageView3 = new ImageView(this.mActivity);
        this.mLeftImage = imageView3;
        imageView3.setId(11);
        this.mLeftImage.setImageResource(R.drawable.iv_left_arrow);
        ImageView imageView4 = new ImageView(this.mActivity);
        this.mRightImage = imageView4;
        imageView4.setId(12);
        this.mRightImage.setImageResource(R.drawable.iv_right_arrow);
        ImageView imageView5 = new ImageView(this.mActivity);
        this.mCenterImage = imageView5;
        imageView5.setId(9);
        this.mCenterImage.setImageResource(R.drawable.iv_center_move);
        FrameLayout frameLayout = new FrameLayout(this.mActivity);
        frameLayout.setId(1);
        frameLayout.setBackgroundResource(R.drawable.iv_center_line);
        FrameLayout frameLayout2 = new FrameLayout(this.mActivity);
        frameLayout2.setId(6);
        frameLayout2.setBackgroundResource(R.drawable.iv_left_line);
        FrameLayout frameLayout3 = new FrameLayout(this.mActivity);
        frameLayout3.setId(7);
        frameLayout3.setBackgroundResource(R.drawable.iv_right_line);
        int intrinsicHeight = this.mTopImage.getDrawable().getIntrinsicHeight();
        this.mMinHeight = intrinsicHeight * 4;
        this.mMinWidth = this.mLeftImage.getDrawable().getIntrinsicWidth() * 3;
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams.leftToLeft = 0;
        layoutParams.rightToRight = 0;
        layoutParams.topToTop = 0;
        layoutParams.bottomToBottom = 0;
        this.mCenterImage.setLayoutParams(layoutParams);
        ConstraintLayout.LayoutParams layoutParams2 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams2.leftToLeft = 0;
        layoutParams2.rightToRight = 0;
        layoutParams2.topToTop = 0;
        this.mTopImage.setLayoutParams(layoutParams2);
        ConstraintLayout.LayoutParams layoutParams3 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams3.leftToLeft = 0;
        layoutParams3.rightToRight = 0;
        layoutParams3.bottomToBottom = 0;
        this.mBottomImage.setLayoutParams(layoutParams3);
        ConstraintLayout.LayoutParams layoutParams4 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams4.leftToRight = frameLayout2.getId();
        layoutParams4.rightToRight = frameLayout2.getId();
        layoutParams4.topToTop = frameLayout.getId();
        layoutParams4.bottomToBottom = frameLayout.getId();
        this.mLeftImage.setLayoutParams(layoutParams4);
        ConstraintLayout.LayoutParams layoutParams5 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams5.leftToLeft = frameLayout3.getId();
        layoutParams5.rightToLeft = frameLayout3.getId();
        layoutParams5.topToTop = frameLayout.getId();
        layoutParams5.bottomToBottom = frameLayout.getId();
        this.mRightImage.setLayoutParams(layoutParams5);
        ConstraintLayout.LayoutParams layoutParams6 = new ConstraintLayout.LayoutParams(-2, 0);
        layoutParams6.leftToLeft = 0;
        layoutParams6.rightToRight = 0;
        layoutParams6.topToTop = 0;
        layoutParams6.bottomToBottom = 0;
        int i = intrinsicHeight / 2;
        layoutParams6.topMargin = i;
        layoutParams6.bottomMargin = i;
        frameLayout.setLayoutParams(layoutParams6);
        ConstraintLayout.LayoutParams layoutParams7 = new ConstraintLayout.LayoutParams(-2, 0);
        layoutParams7.topToTop = frameLayout.getId();
        layoutParams7.bottomToBottom = frameLayout.getId();
        frameLayout2.setLayoutParams(layoutParams7);
        ConstraintLayout.LayoutParams layoutParams8 = new ConstraintLayout.LayoutParams(-2, 0);
        layoutParams8.topToTop = frameLayout.getId();
        layoutParams8.bottomToBottom = frameLayout.getId();
        layoutParams8.rightToRight = 0;
        frameLayout3.setLayoutParams(layoutParams8);
        this.customLayouts.addView(frameLayout2);
        this.customLayouts.addView(frameLayout3);
        this.customLayouts.addView(frameLayout);
        this.customLayouts.addView(this.mTopImage);
        this.customLayouts.addView(this.mRightImage);
        this.customLayouts.addView(this.mBottomImage);
        this.customLayouts.addView(this.mLeftImage);
        this.customLayouts.addView(this.mCenterImage);
        ConstraintLayout.LayoutParams layoutParams9 = new ConstraintLayout.LayoutParams(this.mMinWidth, this.mMinHeight);
        this.customLayouts.setLayoutParams(layoutParams9);
        this.mainView.addView(this.customLayouts, 1);
        this.mWidth = this.waistImageView.getWidth();
        this.mHeight = this.waistImageView.getHeight();
        this.customLayouts.setTranslationX(((float) (this.mWidth - layoutParams9.width)) / 2.0f);
        this.customLayouts.setTranslationY(((float) (this.mHeight - layoutParams9.height)) / 2.0f);
        this.mTopImage.setOnTouchListener(this);
        this.mBottomImage.setOnTouchListener(this);
        this.mLeftImage.setOnTouchListener(this);
        this.mRightImage.setOnTouchListener(this);
        this.mCenterImage.setOnTouchListener(this);
    }

    private void close(boolean z) {
        for (int i = 0; i <= this.lastId; i++) {
            this.mActivity.deleteFile("tool_" + i + ".png");
        }
        this.currentId = -1;
        Bitmap bitmap = this.originalMeshArea;
        if (bitmap != null && !bitmap.isRecycled()) {
            this.originalMeshArea.recycle();
        }
        this.mCurrentBitmap.recycle();
        this.customLayouts.removeAllViews();
        this.mainView.removeView(this.customLayouts);
        this.history.clear();
        View.OnTouchListener onTouchListener = null;
        this.mTopImage.setOnTouchListener(onTouchListener);
        this.mBottomImage.setOnTouchListener(onTouchListener);
        this.mLeftImage.setOnTouchListener(onTouchListener);
        this.mRightImage.setOnTouchListener(onTouchListener);
        this.mCenterImage.setOnTouchListener(onTouchListener);
        this.seekBarView.setOnStartTrackingTouch((ProgressListener) null);
        this.seekBarView.setOnStopTrackingTouch((ProgressListener) null);
        this.seekBarView.setOnProgressChangedListener((ProgressListener) null);
        this.waistImageView.setOnScaleAndMoveInterface((ScaleImage.ScaleAndMoveInterface) null);
        this.mActivity.li_preview.setOnTouchListener(this.mActivity.previewTouchListener);
        this.waistImageView.setImageBitmap(this.mActivity.mCurrentBitmap);
        this.mActivity.li_undo.setOnClickListener(this.mActivity.undoClick);
        this.mActivity.li_redo.setOnClickListener(this.mActivity.redoClick);
    }

    public void onBackPressed(boolean z) {
        if (z) {
            this.mActivity.saveEffect(this.mCurrentBitmap);
        } else {
            close(z);
        }
    }

    public void saveFloatsValue() {
        Log.d(" Waist-Data", "save value");
        if (this.isEditmode) {
            this.isEditmode = false;
            if (this.seekBarView.getProgress() != 0) {
                int i = this.currentId + 1;
                this.currentId = i;
                while (i <= this.lastId) {
                    this.mActivity.deleteFile("tool_" + i + ".png");
                    List<waistHistoryData> list = this.history;
                    list.remove(list.size() - 1);
                    i++;
                }
                int i2 = this.currentId;
                this.lastId = i2;
                this.mIdRequisite = i2;
                final Bitmap copy = this.originalMeshArea.copy(Bitmap.Config.ARGB_8888, true);
                this.originalMeshArea.recycle();
                this.history.add(new waistHistoryData((float[]) this.vertexesmesh.clone(), (float) this.xStart, (float) this.yStart, this.column, this.row));
                this.seekBarView.setProgress(0);
                final String str = "tool_" + this.currentId + ".png";
                final Handler handler = new Handler();
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            FileOutputStream openFileOutput = viewWaistData.this.mActivity.openFileOutput(str, 0);
                            copy.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput);
                            openFileOutput.close();
                            if (viewWaistData.this.currentId == -1) {
                                viewWaistData.this.mActivity.deleteFile(str);
                            }
                        } catch (Exception e) {
                            Log.d("Waist-Data", "Error (save Bitmap):  " + e.getMessage());
                        }
                        handler.post(new Runnable() {
                            public void run() {
                                copy.recycle();
                            }
                        });
                    }
                }).start();
            }
        }
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.lastX = motionEvent.getRawX();
            this.lastY = motionEvent.getRawY();
            this.initialX = this.customLayouts.getTranslationX();
            this.initialY = this.customLayouts.getTranslationY();
            this.initialWidth = (float) this.customLayouts.getWidth();
            this.initialheight = (float) this.customLayouts.getHeight();
            startWorkWithControl();
            return true;
        } else if (motionEvent.getAction() != 2) {
            if (motionEvent.getAction() == 1 || motionEvent.getAction() == 3) {
                this.seekBarView.setEnabled(true);
            }
            return true;
        } else {
            switch (view.getId()) {
                case 8:
                    int rawY = (int) ((this.initialheight + motionEvent.getRawY()) - this.lastY);
                    if (rawY >= this.mMinHeight && ((float) rawY) <= ((float) this.mHeight) - this.initialY) {
                        this.customLayouts.getLayoutParams().height = rawY;
                        this.customLayouts.requestLayout();
                        break;
                    }
                case 9:
                    float rawX = (this.initialX + motionEvent.getRawX()) - this.lastX;
                    float rawY2 = (this.initialY + motionEvent.getRawY()) - this.lastY;
                    if (rawX >= 0.0f && rawX <= ((float) this.mWidth) - this.initialWidth) {
                        this.customLayouts.setTranslationX(rawX);
                    }
                    if (rawY2 >= 0.0f && rawY2 <= ((float) this.mHeight) - this.initialheight) {
                        this.customLayouts.setTranslationY(rawY2);
                        break;
                    }
                case 11:
                    float rawX2 = motionEvent.getRawX() - this.lastX;
                    float f = this.initialWidth;
                    int i = (int) (f - rawX2);
                    if (i >= this.mMinWidth && ((float) i) <= f + this.initialX) {
                        this.customLayouts.getLayoutParams().width = i;
                        this.customLayouts.setTranslationX(this.initialX + rawX2);
                        this.customLayouts.requestLayout();
                        break;
                    }
                case 12:
                    int rawX3 = (int) ((this.initialWidth + motionEvent.getRawX()) - this.lastX);
                    if (rawX3 >= this.mMinWidth && ((float) rawX3) <= ((float) this.mWidth) - this.initialX) {
                        this.customLayouts.getLayoutParams().width = rawX3;
                        this.customLayouts.requestLayout();
                        break;
                    }
                case 14:
                    float rawY3 = motionEvent.getRawY() - this.lastY;
                    float f2 = this.initialheight;
                    int i2 = (int) (f2 - rawY3);
                    if (i2 >= this.mMinHeight && ((float) i2) <= this.initialY + f2) {
                        this.customLayouts.getLayoutParams().height = i2;
                        this.customLayouts.setTranslationY(this.initialY + rawY3);
                        this.customLayouts.requestLayout();
                        break;
                    }
            }
            return true;
        }
    }

    private void startWorkWithControl() {
        this.seekBarView.setEnabled(false);
        saveFloatsValue();
    }

    public void move(float f, float f2, float f3, float f4) {
        saveFloatsValue();
    }

    public void loadResponse(Bitmap bitmap, int i, int i2) {
        if (bitmap != null) {
            if (i2 > i && this.currentId < i2) {
                Bitmap createBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                if (!createBitmap.isMutable()) {
                    Bitmap copy = createBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    createBitmap.recycle();
                    createBitmap = copy;
                }
                Canvas canvas = new Canvas(createBitmap);
                waistHistoryData waisthistorydata = this.history.get(i2 - 1);
                Paint paint = null;
                canvas.drawBitmapMesh(bitmap, waisthistorydata.column, waisthistorydata.row, waisthistorydata.currentMesh, 0, (int[]) null, 0, paint);
                this.myCanvasView.drawBitmap(createBitmap, waisthistorydata.fx, waisthistorydata.fy, paint);
                createBitmap.recycle();
                this.currentId = i2;
                this.mIdRequisite = i2;
            } else if (i2 < i && i2 < this.currentId) {
                this.myCanvasView.drawBitmap(bitmap, this.history.get(i2).fx, this.history.get(i2).fy, (Paint) null);
                this.currentId = i2;
                this.mIdRequisite = i2;
            }
            this.waistImageView.invalidate();
            bitmap.recycle();
            return;
        }
        this.mIdRequisite = i;
    }

    class waistHistoryData {
        int column;
        float[] currentMesh;
        float fx;
        float fy;
        int row;

        waistHistoryData(float[] fArr, float f, float f2, int i, int i2) {
            this.currentMesh = fArr;
            this.fx = f;
            this.fy = f2;
            this.column = i;
            this.row = i2;
        }
    }

    public void onStartFun(int i) {
        float f;
        float f2;
        float f3;
        Log.d("Waist-Data", "onStartFun()  onStartTouchListener; i: " + i);
        this.customLayouts.setVisibility(View.INVISIBLE);
        if (!this.isEditmode) {
            this.lastProgressvalue = 0;
            this.waistImageView.getImageMatrix().getValues(this.matrixValues);
            float translationX = this.customLayouts.getTranslationX();
            float[] fArr = this.matrixValues;
            int i2 = 0;
            this.xStart = Math.round((translationX - fArr[2]) / fArr[0]);
            float translationY = this.customLayouts.getTranslationY();
            float[] fArr2 = this.matrixValues;
            this.yStart = Math.round((translationY - fArr2[5]) / fArr2[4]);
            int round = Math.round(((float) this.customLayouts.getWidth()) / this.matrixValues[0]);
            int round2 = Math.round(((float) this.customLayouts.getHeight()) / this.matrixValues[4]);
            float f4 = 2.0f;
            float f5 = ((float) round) / 2.0f;
            float f6 = ((float) round2) / 2.0f;
            int i3 = this.xStart;
            if (i3 < 0) {
                round += i3;
                f = ((float) i3) + f5;
                this.xStart = 0;
            } else {
                f = f5;
            }
            int i4 = this.yStart;
            if (i4 < 0) {
                f2 = (float) (-i4);
                round2 += i4;
                this.yStart = 0;
            } else {
                f2 = 0.0f;
            }
            int min = Math.min(round, this.mCurrentBitmap.getWidth() - this.xStart);
            int min2 = Math.min(round2, this.mCurrentBitmap.getHeight() - this.yStart);
            if (min >= 50 && min2 >= 50) {
                this.originalMeshArea = Bitmap.createBitmap(this.mCurrentBitmap, this.xStart, this.yStart, min, min2);
                this.column = 10;
                float f7 = ((float) min) / ((float) 10);
                int min3 = Math.min(min2 / 10, 30);
                this.row = min3;
                float f8 = ((float) min2) / ((float) min3);
                int i5 = (this.column + 1) * 2 * (min3 + 1);
                this.vertsNumbers = i5;
                this.vertexesmesh = new float[i5];
                this.maxVertesValues = new float[i5];
                while (i2 < this.vertsNumbers) {
                    int i6 = i2 / 2;
                    int i7 = this.column;
                    int i8 = i6 % (i7 + 1);
                    float f9 = ((float) i8) * f7;
                    float f10 = ((float) (i6 / (i7 + 1))) * f8;
                    float[] fArr3 = this.vertexesmesh;
                    fArr3[i2] = f9;
                    fArr3[i2 + 1] = f10;
                    if (i8 == 0 || i8 == i7) {
                        f3 = f6;
                    } else {
                        f3 = f6;
                        this.maxVertesValues[i2] = ((((float) Math.sin((((double) (f10 + f2)) * 3.141592653589793d) / ((double) (f6 * f4)))) * f7) * (f9 - f)) / f5;
                    }
                    i2 += 2;
                    f6 = f3;
                    f4 = 2.0f;
                }
                this.isEditmode = true;
            }
        }
    }

    public void progressChangeFun(final int i) {
        new Thread(new Runnable() {
            public void run() {
                Log.d("Waist-Data", "onProgressChangeListener; i: " + i);
                long j = viewWaistData.this.lastProgressvalue;
                int i = 0;
                long j2 = j - ((long) i);
                viewWaistData.this.lastProgressvalue = (long) i;
                for (int i2 = 0; i2 < viewWaistData.this.vertsNumbers; i2 += 2) {
                    float[] fArr = viewWaistData.this.vertexesmesh;
                    fArr[i2] = fArr[i2] + ((viewWaistData.this.maxVertesValues[i2] * ((float) j2)) / 50.0f);
                }
                Bitmap createBitmap = Bitmap.createBitmap(viewWaistData.this.originalMeshArea.getWidth(), viewWaistData.this.originalMeshArea.getHeight(), Bitmap.Config.ARGB_8888);
                if (!createBitmap.isMutable()) {
                    Bitmap copy = createBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    createBitmap.recycle();
                    createBitmap = copy;
                }
                Paint paint = null;
                new Canvas(createBitmap).drawBitmapMesh(viewWaistData.this.originalMeshArea, viewWaistData.this.column, viewWaistData.this.row, viewWaistData.this.vertexesmesh, 0, (int[]) null, 0, paint);
                viewWaistData.this.myCanvasView.drawBitmap(createBitmap, (float) viewWaistData.this.xStart, (float) viewWaistData.this.yStart, paint);
                createBitmap.recycle();
                viewWaistData.this.mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        viewWaistData.this.waistImageView.invalidate();
                    }
                });
            }
        }).run();
    }
}
