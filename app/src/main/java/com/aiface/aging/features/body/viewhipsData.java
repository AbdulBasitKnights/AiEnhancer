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


public class viewhipsData implements BodyEditFragment.BackPressed, View.OnTouchListener, ScaleImage.ScaleAndMoveInterface, undoRedoData.PhotoLoadResponse {
    private float aFloat;
    private float aFloat1;
    private float aFloat2;
    private float aFloat3;
    private float aFloat4;
    private float aFloat5;
    public int anInt = 30;
    public int anInt1 = 10;
    public int anInt2;
    public int anInt3;
    public Bitmap currentBitmap;
    public int currentId;
    public Bitmap currentMeshArea;
    public Bitmap currentoriginalBitmap;
    public ConstraintLayout customLayouts;
    private ImageView downImageview;
    private int heightData;
    private List<hipshistorydata> hipsHistories = new ArrayList();
    public ScaleImage hipsImageView;
//    public ImageEditing imageEditing;
    public ImageEditingER imageEditing;
    public boolean isEditmode;

    public int lastId;
    public long lastProgressvalue;
    private ImageView leftimageview;
    private ImageView mCenterImage;

    public int mIdRequisite;
    private int mWidth;
    private FrameLayout mainView;
    public float[] matrixValues = new float[9];
    public float[] maxVertexes;
    private MenuClick menuClick;
    public float[] meshfloats;
    private int minHeight;
    private int minWidth;
    public Canvas myCanvasView;
    private ProgressListener onprogressChangeListener = new ProgressListener() {
        public void invoke(final int i) {
            if (viewhipsData.this.isEditmode) {
                new Thread(new Runnable() {
                    public void run() {
                        long j = ((long) i) - viewhipsData.this.lastProgressvalue;
                        viewhipsData.this.lastProgressvalue = (long) i;
                        for (int i = 0; i < viewhipsData.this.anInt3; i += 2) {
                            float[] fArr = viewhipsData.this.meshfloats;
                            fArr[i] = fArr[i] + ((viewhipsData.this.maxVertexes[i] * ((float) j)) / 50.0f);
                        }
                        Bitmap createBitmap = Bitmap.createBitmap(viewhipsData.this.currentMeshArea.getWidth(), viewhipsData.this.currentMeshArea.getHeight(), Bitmap.Config.ARGB_8888);
                        if (!createBitmap.isMutable()) {
                            Bitmap copy = createBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            createBitmap.recycle();
                            createBitmap = copy;
                        }
                        Paint paint = null;
                        new Canvas(createBitmap).drawBitmapMesh(viewhipsData.this.currentMeshArea, viewhipsData.this.anInt2, viewhipsData.this.row, viewhipsData.this.meshfloats, 0, (int[]) null, 0, paint);
                        viewhipsData.this.myCanvasView.drawBitmap(createBitmap, (float) viewhipsData.this.xStart, (float) viewhipsData.this.yStart, paint);
                        createBitmap.recycle();
                        viewhipsData.this.imageEditing.runOnUiThread(new Runnable() {
                            public void run() {
                                viewhipsData.this.hipsImageView.invalidate();
                            }
                        });
                    }
                }).run();
            }
        }
    };
    private ProgressListener onstartTouchListener = new ProgressListener() {
        public void invoke(int i) {
            viewhipsData.this.customLayouts.setVisibility(View.INVISIBLE);
            if (!viewhipsData.this.isEditmode) {
                viewhipsData.this.startEditing();
            }
        }
    };
    private ProgressListener onstoptTouchListener = new ProgressListener() {
        public void invoke(int i) {
            if (!viewhipsData.this.isEditmode) {
                viewhipsData.this.seekBarView.setProgress(0);
            }
            viewhipsData.this.customLayouts.setVisibility(View.VISIBLE);
        }
    };
    private ImageView rightImageView;
    public int row;
    SeekBar seek;
    public ArcSeekBar seekBarView;
    int seek_pos = 0;
    private ImageView upImageView;
    public int xStart;
    public int yStart;


    public void startEditing() {
        float f;
        float f2;
        float f3;
        this.lastProgressvalue = 0;
        this.hipsImageView.getImageMatrix().getValues(this.matrixValues);
        float translationX = this.customLayouts.getTranslationX();
        float[] fArr = this.matrixValues;
        int i = 0;
        this.xStart = Math.round((translationX - fArr[2]) / fArr[0]);
        float translationY = this.customLayouts.getTranslationY();
        float[] fArr2 = this.matrixValues;
        this.yStart = Math.round((translationY - fArr2[5]) / fArr2[4]);
        int round = Math.round(((float) this.customLayouts.getWidth()) / this.matrixValues[0]);
        int round2 = Math.round(((float) this.customLayouts.getHeight()) / this.matrixValues[4]);
        float f4 = 2.0f;
        float f5 = ((float) round) / 2.0f;
        float f6 = ((float) round2) / 2.0f;
        int i2 = this.xStart;
        if (i2 < 0) {
            round += i2;
            f = ((float) i2) + f5;
            this.xStart = 0;
        } else {
            f = f5;
        }
        int i3 = this.yStart;
        if (i3 < 0) {
            f2 = (float) (-i3);
            round2 += i3;
            this.yStart = 0;
        } else {
            f2 = 0.0f;
        }
        int min = Math.min(round, this.currentBitmap.getWidth() - this.xStart);
        int min2 = Math.min(round2, this.currentBitmap.getHeight() - this.yStart);
        if (min >= 50 && min2 >= 50) {
            this.currentMeshArea = Bitmap.createBitmap(this.currentBitmap, this.xStart, this.yStart, min, min2);
            int i4 = this.anInt1;
            this.anInt2 = i4;
            float f7 = ((float) min) / ((float) i4);
            int min3 = Math.min(min2 / 10, this.anInt);
            this.row = min3;
            float f8 = ((float) min2) / ((float) min3);
            int i5 = (this.anInt2 + 1) * 2 * (min3 + 1);
            this.anInt3 = i5;
            this.meshfloats = new float[i5];
            this.maxVertexes = new float[i5];
            while (i < this.anInt3) {
                int i6 = i / 2;
                int i7 = this.anInt2;
                int i8 = i6 % (i7 + 1);
                float f9 = ((float) i8) * f7;
                float f10 = ((float) (i6 / (i7 + 1))) * f8;
                float[] fArr3 = this.meshfloats;
                fArr3[i] = f9;
                fArr3[i + 1] = f10;
                if (i8 == 0 || i8 == i7) {
                    f3 = f6;
                } else {
                    f3 = f6;
                    this.maxVertexes[i] = ((((float) Math.sin((((double) (f10 + f2)) * 3.141592653589793d) / ((double) (f6 * f4)))) * f7) * (f9 - f)) / f5;
                }
                i += 2;
                f6 = f3;
                f4 = 2.0f;
            }
            this.isEditmode = true;
        }
    }

//    public viewhipsData(Bitmap bitmap, ImageEditing imageEditing2, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick2) {
    public viewhipsData(Bitmap bitmap, ImageEditingER imageEditing2, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick2) {
        this.currentoriginalBitmap = bitmap;
        this.imageEditing = imageEditing2;
        this.hipsImageView = scaleImage;
        this.seekBarView = arcSeekBar;
        this.seek = seekBar;
        this.menuClick = menuClick2;
        onCreate();
        this.imageEditing.li_undo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                viewhipsData.this.saveFloatsValue();
                int access$200 = viewhipsData.this.mIdRequisite;
                if (access$200 != viewhipsData.this.currentId || access$200 <= 0) {
                    viewhipsData.this.imageEditing.li_undo.setImageDrawable(viewhipsData.this.imageEditing.getResources().getDrawable(R.drawable.ic_disable_undo));
                    return;
                }
                int access$2002 = viewhipsData.this.mIdRequisite;
                int i = access$2002 - 1;
                int unused = viewhipsData.this.mIdRequisite = i;
                viewhipsData viewhipsdata = viewhipsData.this;
                undoRedoData.getBitmapFromDisk(access$2002, i, "tool_" + (viewhipsData.this.mIdRequisite + 1) + ".png", viewhipsdata, viewhipsdata.imageEditing);
                viewhipsData.this.imageEditing.li_undo.setImageDrawable(viewhipsData.this.imageEditing.getResources().getDrawable(R.drawable.ic_new_undo));
                viewhipsData.this.imageEditing.li_redo.setImageDrawable(viewhipsData.this.imageEditing.getResources().getDrawable(R.drawable.ic_new_redo));
            }
        });
        this.imageEditing.li_redo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int access$200 = viewhipsData.this.mIdRequisite;
                if (access$200 != viewhipsData.this.currentId || access$200 >= viewhipsData.this.lastId) {
                    viewhipsData.this.imageEditing.li_redo.setImageDrawable(viewhipsData.this.imageEditing.getResources().getDrawable(R.drawable.ic_disable_redo));
                } else if (!viewhipsData.this.isEditmode) {
                    int access$2002 = viewhipsData.this.mIdRequisite;
                    int i = access$2002 + 1;
                    int unused = viewhipsData.this.mIdRequisite = i;
                    viewhipsData viewhipsdata = viewhipsData.this;
                    undoRedoData.getBitmapFromDisk(access$2002, i, "tool_" + viewhipsData.this.mIdRequisite + ".png", viewhipsdata, viewhipsdata.imageEditing);
                    viewhipsData.this.imageEditing.li_undo.setImageDrawable(viewhipsData.this.imageEditing.getResources().getDrawable(R.drawable.ic_new_undo));
                    viewhipsData.this.imageEditing.li_redo.setImageDrawable(viewhipsData.this.imageEditing.getResources().getDrawable(R.drawable.ic_new_redo));
                } else {
                    viewhipsData.this.saveFloatsValue();
                    viewhipsData.this.imageEditing.li_undo.setImageDrawable(viewhipsData.this.imageEditing.getResources().getDrawable(R.drawable.ic_new_undo));
                    viewhipsData.this.imageEditing.li_redo.setImageDrawable(viewhipsData.this.imageEditing.getResources().getDrawable(R.drawable.ic_new_redo));
                }
            }
        });
    }

    private void onCreate() {
        this.mainView = (FrameLayout) this.imageEditing.findViewById(R.id.main_frameView);
        this.imageEditing.isBlocked = false;
        drawCustomView();
        this.currentBitmap = this.currentoriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        this.myCanvasView = new Canvas(this.currentBitmap);
        this.imageEditing.li_preview.setOnTouchListener(
                this.imageEditing.createToolCompareTouchListener(
                        this.hipsImageView,
                        () -> viewhipsData.this.currentoriginalBitmap,
                        () -> viewhipsData.this.currentBitmap
                )
        );
        this.seekBarView.setProgress(0);
        this.seekBarView.setOnStartTrackingTouch(this.onstartTouchListener);
        this.seekBarView.setOnStopTrackingTouch(this.onstoptTouchListener);
        this.seekBarView.setOnProgressChangedListener(this.onprogressChangeListener);
        this.hipsImageView.setImageBitmap(this.currentBitmap);
        this.hipsImageView.setOnScaleAndMoveInterface(this);
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                viewhipsData.this.seek_pos = i;
                seekBar.setProgress(i);
                viewhipsData.this.seekBarView.getOnProgressChangedListener().invoke(i);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                viewhipsData.this.seekBarView.getOnStartTrackingTouch().invoke(viewhipsData.this.seek_pos);
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!viewhipsData.this.isEditmode) {
                    viewhipsData.this.seekBarView.setProgress(0);
                }
                viewhipsData.this.customLayouts.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressLint("ResourceType")
    private void drawCustomView() {
        this.customLayouts = new ConstraintLayout(this.imageEditing);
        ImageView imageView = new ImageView(this.imageEditing);
        this.upImageView = imageView;
        imageView.setId(14);
        this.upImageView.setImageResource(R.drawable.iv_up_arrow);
        ImageView imageView2 = new ImageView(this.imageEditing);
        this.downImageview = imageView2;
        imageView2.setId(8);
        this.downImageview.setImageResource(R.drawable.iv_down_arrow);
        ImageView imageView3 = new ImageView(this.imageEditing);
        this.leftimageview = imageView3;
        imageView3.setId(11);
        this.leftimageview.setImageResource(R.drawable.iv_left_arrow);
        ImageView imageView4 = new ImageView(this.imageEditing);
        this.rightImageView = imageView4;
        imageView4.setId(12);
        this.rightImageView.setImageResource(R.drawable.iv_right_arrow);
        ImageView imageView5 = new ImageView(this.imageEditing);
        this.mCenterImage = imageView5;
        imageView5.setId(9);
        this.mCenterImage.setImageResource(R.drawable.iv_center_move);
        FrameLayout frameLayout = new FrameLayout(this.imageEditing);
        frameLayout.setId(1);
        frameLayout.setBackgroundResource(R.drawable.iv_center_line);
        FrameLayout frameLayout2 = new FrameLayout(this.imageEditing);
        frameLayout2.setBackgroundResource(R.drawable.iv_hips_center_horizontal);
        FrameLayout frameLayout3 = new FrameLayout(this.imageEditing);
        frameLayout3.setId(6);
        frameLayout3.setBackgroundResource(R.drawable.iv_hips_left_line);
        FrameLayout frameLayout4 = new FrameLayout(this.imageEditing);
        frameLayout4.setId(7);
        frameLayout4.setBackgroundResource(R.drawable.iv_hips_right_line);
        int intrinsicHeight = this.upImageView.getDrawable().getIntrinsicHeight();
        this.minHeight = intrinsicHeight * 4;
        this.minWidth = this.leftimageview.getDrawable().getIntrinsicWidth() * 4;
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(Math.round(((float) this.minHeight) / 2.5f), Math.round(((float) this.minWidth) / 2.5f));
        layoutParams.leftToLeft = 0;
        layoutParams.rightToRight = 0;
        layoutParams.topToTop = 0;
        layoutParams.bottomToBottom = 0;
        this.mCenterImage.setLayoutParams(layoutParams);
        ConstraintLayout.LayoutParams layoutParams2 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams2.leftToLeft = 0;
        layoutParams2.rightToRight = 0;
        layoutParams2.topToTop = 0;
        this.upImageView.setLayoutParams(layoutParams2);
        ConstraintLayout.LayoutParams layoutParams3 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams3.leftToLeft = 0;
        layoutParams3.rightToRight = 0;
        layoutParams3.bottomToBottom = 0;
        this.downImageview.setLayoutParams(layoutParams3);
        ConstraintLayout.LayoutParams layoutParams4 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams4.topToTop = 0;
        layoutParams4.bottomToBottom = 0;
        this.leftimageview.setLayoutParams(layoutParams4);
        ConstraintLayout.LayoutParams layoutParams5 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams5.rightToRight = 0;
        layoutParams5.topToTop = 0;
        layoutParams5.bottomToBottom = 0;
        this.rightImageView.setLayoutParams(layoutParams5);
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
        frameLayout3.setLayoutParams(layoutParams7);
        ConstraintLayout.LayoutParams layoutParams8 = new ConstraintLayout.LayoutParams(-2, 0);
        layoutParams8.topToTop = frameLayout.getId();
        layoutParams8.bottomToBottom = frameLayout.getId();
        layoutParams8.rightToRight = 0;
        frameLayout4.setLayoutParams(layoutParams8);
        ConstraintLayout.LayoutParams layoutParams9 = new ConstraintLayout.LayoutParams(0, -2);
        layoutParams9.topToTop = 0;
        layoutParams9.bottomToBottom = 0;
        layoutParams9.leftToLeft = 0;
        layoutParams9.rightToRight = 0;
        frameLayout2.setLayoutParams(layoutParams9);
        this.customLayouts.addView(frameLayout3);
        this.customLayouts.addView(frameLayout4);
        this.customLayouts.addView(frameLayout);
        this.customLayouts.addView(frameLayout2);
        this.customLayouts.addView(this.upImageView);
        this.customLayouts.addView(this.rightImageView);
        this.customLayouts.addView(this.downImageview);
        this.customLayouts.addView(this.leftimageview);
        this.customLayouts.addView(this.mCenterImage);
        FrameLayout.LayoutParams layoutParams10 = new FrameLayout.LayoutParams(this.minWidth, this.minHeight);
        this.customLayouts.setLayoutParams(layoutParams10);
        this.mainView.addView(this.customLayouts, 1);
        this.mWidth = this.hipsImageView.getWidth();
        this.heightData = this.hipsImageView.getHeight();
        this.customLayouts.setTranslationX(((float) (this.mWidth - layoutParams10.width)) / 2.0f);
        this.customLayouts.setTranslationY(((float) (this.heightData - layoutParams10.height)) / 2.0f);
        this.upImageView.setOnTouchListener(this);
        this.downImageview.setOnTouchListener(this);
        this.leftimageview.setOnTouchListener(this);
        this.rightImageView.setOnTouchListener(this);
        this.mCenterImage.setOnTouchListener(this);
    }

    private void close(boolean z) {
        for (int i = 0; i <= this.lastId; i++) {
            this.imageEditing.deleteFile("tool_" + i + ".png");
        }
        this.currentId = -1;
        Bitmap bitmap = this.currentMeshArea;
        if (bitmap != null && !bitmap.isRecycled()) {
            this.currentMeshArea.recycle();
        }
        this.currentBitmap.recycle();
        this.customLayouts.removeAllViews();
        this.mainView.removeView(this.customLayouts);
        this.hipsHistories.clear();
        View.OnTouchListener onTouchListener = null;
        this.upImageView.setOnTouchListener(onTouchListener);
        this.downImageview.setOnTouchListener(onTouchListener);
        this.leftimageview.setOnTouchListener(onTouchListener);
        this.rightImageView.setOnTouchListener(onTouchListener);
        this.mCenterImage.setOnTouchListener(onTouchListener);
        this.seekBarView.setOnStartTrackingTouch((ProgressListener) null);
        this.seekBarView.setOnStopTrackingTouch((ProgressListener) null);
        this.seekBarView.setOnProgressChangedListener((ProgressListener) null);
        this.hipsImageView.setOnScaleAndMoveInterface((ScaleImage.ScaleAndMoveInterface) null);
        this.imageEditing.li_preview.setOnTouchListener(this.imageEditing.previewTouchListener);
        this.hipsImageView.setImageBitmap(this.imageEditing.mCurrentBitmap);
        this.imageEditing.li_undo.setOnClickListener(this.imageEditing.undoClick);
        this.imageEditing.li_redo.setOnClickListener(this.imageEditing.redoClick);
    }

    public void onBackPressed(boolean z) {
        if (z) {
            this.imageEditing.saveEffect(this.currentBitmap);
        } else {
            close(z);
        }
    }


    public void saveFloatsValue() {
        if (this.isEditmode) {
            this.isEditmode = false;
            if (this.seekBarView.getProgress() != 0) {
                int i = this.currentId + 1;
                this.currentId = i;
                while (i <= this.lastId) {
                    this.imageEditing.deleteFile("tool_" + i + ".png");
                    List<hipshistorydata> list = this.hipsHistories;
                    list.remove(list.size() - 1);
                    i++;
                }
                int i2 = this.currentId;
                this.lastId = i2;
                this.mIdRequisite = i2;
                final Bitmap copy = this.currentMeshArea.copy(Bitmap.Config.ARGB_8888, true);
                this.currentMeshArea.recycle();
                this.hipsHistories.add(new hipshistorydata((float[]) this.meshfloats.clone(), (float) this.xStart, (float) this.yStart, this.anInt2, this.row));
                this.seekBarView.setProgress(0);
                final String str = "tool_" + this.currentId + ".png";
                final Handler handler = new Handler();
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            FileOutputStream openFileOutput = viewhipsData.this.imageEditing.openFileOutput(str, 0);
                            copy.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput);
                            openFileOutput.close();
                            if (viewhipsData.this.currentId == -1) {
                                viewhipsData.this.imageEditing.deleteFile(str);
                            }
                        } catch (Exception e) {
                            Log.d("My", "Error (save Bitmap): " + e.getMessage());
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
            this.aFloat4 = motionEvent.getRawX();
            this.aFloat5 = motionEvent.getRawY();
            this.aFloat1 = this.customLayouts.getTranslationX();
            this.aFloat2 = this.customLayouts.getTranslationY();
            this.aFloat3 = (float) this.customLayouts.getWidth();
            this.aFloat = (float) this.customLayouts.getHeight();
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
                    int rawY = (int) ((this.aFloat + motionEvent.getRawY()) - this.aFloat5);
                    if (rawY >= this.minHeight && ((float) rawY) <= ((float) this.heightData) - this.aFloat2) {
                        this.customLayouts.getLayoutParams().height = rawY;
                        this.customLayouts.requestLayout();
                        break;
                    }
                case 9:
                    float rawX = (this.aFloat1 + motionEvent.getRawX()) - this.aFloat4;
                    float rawY2 = (this.aFloat2 + motionEvent.getRawY()) - this.aFloat5;
                    if (rawX >= 0.0f && rawX <= ((float) this.mWidth) - this.aFloat3) {
                        this.customLayouts.setTranslationX(rawX);
                    }
                    if (rawY2 >= 0.0f && rawY2 <= ((float) this.heightData) - this.aFloat) {
                        this.customLayouts.setTranslationY(rawY2);
                        break;
                    }
                case 11:
                    float rawX2 = motionEvent.getRawX() - this.aFloat4;
                    float f = this.aFloat3;
                    int i = (int) (f - rawX2);
                    if (i >= this.minWidth && ((float) i) <= f + this.aFloat1) {
                        this.customLayouts.getLayoutParams().width = i;
                        this.customLayouts.setTranslationX(this.aFloat1 + rawX2);
                        this.customLayouts.requestLayout();
                        break;
                    }
                case 12:
                    int rawX3 = (int) ((this.aFloat3 + motionEvent.getRawX()) - this.aFloat4);
                    if (rawX3 >= this.minWidth && ((float) rawX3) <= ((float) this.mWidth) - this.aFloat1) {
                        this.customLayouts.getLayoutParams().width = rawX3;
                        this.customLayouts.requestLayout();
                        break;
                    }
                case 14:
                    float rawY3 = motionEvent.getRawY() - this.aFloat5;
                    float f2 = this.aFloat;
                    int i2 = (int) (f2 - rawY3);
                    if (i2 >= this.minHeight && ((float) i2) <= this.aFloat2 + f2) {
                        this.customLayouts.getLayoutParams().height = i2;
                        this.customLayouts.setTranslationY(this.aFloat2 + rawY3);
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
                hipshistorydata hipshistorydata2 = this.hipsHistories.get(i2 - 1);
                Paint paint = null;
                canvas.drawBitmapMesh(bitmap, hipshistorydata2.column, hipshistorydata2.row, hipshistorydata2.currentMesh, 0, (int[]) null, 0, paint);
                this.myCanvasView.drawBitmap(createBitmap, hipshistorydata2.f185x, hipshistorydata2.f186y, paint);
                createBitmap.recycle();
                this.currentId = i2;
                this.mIdRequisite = i2;
            } else if (i2 < i && i2 < this.currentId) {
                this.myCanvasView.drawBitmap(bitmap, this.hipsHistories.get(i2).f185x, this.hipsHistories.get(i2).f186y, (Paint) null);
                this.currentId = i2;
                this.mIdRequisite = i2;
            }
            this.hipsImageView.invalidate();
            bitmap.recycle();
            return;
        }
        this.mIdRequisite = i;
    }

    class hipshistorydata {
        int column;
        float[] currentMesh;
        float f185x;
        float f186y;
        int row;

        hipshistorydata(float[] fArr, float f, float f2, int i, int i2) {
            this.currentMesh = fArr;
            this.f185x = f;
            this.f186y = f2;
            this.column = i;
            this.row = i2;
        }
    }
}





























































































































































































































































































































































































































































































































































































































































































