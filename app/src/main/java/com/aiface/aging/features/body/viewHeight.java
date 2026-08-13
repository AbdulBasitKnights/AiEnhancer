package com.aiface.aging.features.body;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

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


public class viewHeight implements BodyEditFragment.BackPressed, ScaleImage.TouchInterface, undoRedoData.PhotoLoadResponse {
    public Bitmap CurrentBitmap;
    private boolean aBoolean;
    private float aFloat;
    public float aFloat1;
    public int anInt;
    public int anInt1;
    public int anInt10;
    public int anInt11;
    private int anInt12;
    public int anInt2;
    private int anInt3;
    public int anInt7;
    public int anInt8;
    public int anInt9;
    public Bitmap centerBitmap;
    public Bitmap centerImageBitmap;
    public Bitmap currentoriginalBitmap;
    public ConstraintLayout customLayouts;
    public Bitmap downBitmap;
    public ImageView downImage;
    public int downLine;
    public int downLine1;
//    public ImageEditing editing1;
    public ImageEditingER editing1;
    public ScaleImage heightImageview;
    public boolean isEditmode;
    public LinearLayout linearLayout;
    public int mIdCurrent;
    private int mIdLast;
    private int mIdRequisite;
    private FrameLayout mainView;
    private MenuClick menuClick;
    public Canvas myCanvas;
    public Bitmap priviewBitmap;
    SeekBar seek;
    public ArcSeekBar seekBarView;
    public Bitmap upBitmap;
    public ImageView upImage;
    public ImageView view;
    public ImageView view1;
    private final int dencityDp = Math.round(Resources.getSystem().getDisplayMetrics().density);
    private List<historyData> heightHistories = new ArrayList();
    int seek_pos = 0;
    private ProgressListener onprogressChangeListener = new AnonymousClass1();
    private ProgressListener onstartTouchListener = new ProgressListener() {
        @Override
        public void invoke(int i) {
            viewHeight.this.heightImageview.setOnTouchInterface(null);
            if (!viewHeight.this.isEditmode) {
                viewHeight.this.isEditmode = true;
                viewHeight viewheight = viewHeight.this;
                viewheight.anInt7 = Math.round(viewheight.downLine * 0.1f);
                if (viewHeight.this.anInt11 > viewHeight.this.anInt8) {
                    viewHeight viewheight2 = viewHeight.this;
                    viewheight2.upBitmap = Bitmap.createBitmap(viewheight2.CurrentBitmap, 0, viewHeight.this.anInt8, viewHeight.this.currentoriginalBitmap.getWidth(), viewHeight.this.anInt11 - viewHeight.this.anInt8);
                    viewHeight.this.upImage.setVisibility(View.VISIBLE);
                } else {
                    viewHeight.this.upBitmap = null;
                    viewHeight.this.upImage.setVisibility(View.GONE);
                }
                viewHeight viewheight3 = viewHeight.this;
                viewheight3.centerImageBitmap = Bitmap.createBitmap(viewheight3.CurrentBitmap, 0, viewHeight.this.anInt11, viewHeight.this.currentoriginalBitmap.getWidth(), viewHeight.this.downLine);
                if (((viewHeight.this.CurrentBitmap.getHeight() - viewHeight.this.anInt11) - viewHeight.this.downLine) - viewHeight.this.anInt8 > 0) {
                    viewHeight viewheight4 = viewHeight.this;
                    viewheight4.downBitmap = Bitmap.createBitmap(viewheight4.CurrentBitmap, 0, viewHeight.this.anInt11 + viewHeight.this.downLine, viewHeight.this.currentoriginalBitmap.getWidth(), ((viewHeight.this.CurrentBitmap.getHeight() - viewHeight.this.anInt11) - viewHeight.this.downLine) - viewHeight.this.anInt8);
                    viewHeight.this.downImage.setVisibility(View.VISIBLE);
                } else {
                    viewHeight.this.downBitmap = null;
                    viewHeight.this.downImage.setVisibility(View.GONE);
                }
                viewHeight viewheight5 = viewHeight.this;
                viewheight5.centerBitmap = Bitmap.createBitmap(viewheight5.CurrentBitmap, 0, viewHeight.this.anInt11, viewHeight.this.currentoriginalBitmap.getWidth(), viewHeight.this.downLine);
                viewHeight.this.upImage.setImageBitmap(viewHeight.this.upBitmap);
                viewHeight.this.view1.setImageBitmap(viewHeight.this.centerImageBitmap);
                viewHeight.this.downImage.setImageBitmap(viewHeight.this.downBitmap);
                viewHeight viewheight6 = viewHeight.this;
                viewheight6.anInt9 = viewheight6.anInt8;
                viewHeight viewheight7 = viewHeight.this;
                viewheight7.downLine1 = viewheight7.downLine;
                viewHeight viewheight8 = viewHeight.this;
                viewheight8.anInt10 = viewheight8.anInt11;
            } else {
                viewHeight.this.changeViewswithcopies();
            }
            viewHeight.this.myCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            viewHeight.this.customLayouts.setVisibility(View.INVISIBLE);
            viewHeight.this.linearLayout.setVisibility(View.VISIBLE);
            viewHeight.this.linearLayout.requestLayout();
        }
    };
    private ProgressListener onstoptTouchListener = new ProgressListener() {
        @Override
        public void invoke(int i) {
            viewHeight.this.heightImageview.setOnTouchInterface(viewHeight.this);
            viewHeight.this.myCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            if (viewHeight.this.upBitmap != null) {
                viewHeight.this.myCanvas.drawBitmap(viewHeight.this.upBitmap, 0.0f, viewHeight.this.anInt9, (Paint) null);
            }
            Paint paint = null;
            viewHeight.this.myCanvas.drawBitmap(viewHeight.this.centerBitmap, 0.0f, viewHeight.this.anInt10, paint);
            if (viewHeight.this.downBitmap != null) {
                viewHeight.this.myCanvas.drawBitmap(viewHeight.this.downBitmap, 0.0f, viewHeight.this.anInt10 + viewHeight.this.downLine1, paint);
            }
            if (viewHeight.this.anInt10 == viewHeight.this.anInt11 && viewHeight.this.downLine1 == viewHeight.this.downLine) {
                viewHeight.this.isEditmode = false;
                viewHeight.this.anInt = 0;
                if (viewHeight.this.upBitmap != null) {
                    viewHeight.this.upBitmap.recycle();
                }
                viewHeight.this.centerImageBitmap.recycle();
                viewHeight.this.centerBitmap.recycle();
                if (viewHeight.this.downBitmap != null) {
                    viewHeight.this.downBitmap.recycle();
                }
            } else {
                viewHeight.this.changeViewswithcopies();
                viewHeight.this.customLayouts.setTranslationY((viewHeight.this.aFloat1 + (viewHeight.this.anInt11 * viewHeight.this.heightImageview.getCalculatedMinScale())) - viewHeight.this.anInt2);
                viewHeight.this.view.getLayoutParams().height = (int) (viewHeight.this.downLine * viewHeight.this.heightImageview.getCalculatedMinScale());
                viewHeight.this.view.requestLayout();
            }
            viewHeight.this.seekBarView.setProgress(viewHeight.this.anInt);
            viewHeight.this.heightImageview.invalidate();
            viewHeight.this.customLayouts.setVisibility(View.VISIBLE);
            viewHeight.this.linearLayout.setVisibility(View.INVISIBLE);
        }
    };


    class AnonymousClass1 implements ProgressListener {
        AnonymousClass1() {
        }

        @Override
        public void invoke(final int i) {
            if (viewHeight.this.isEditmode) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int i2 = i;
                        int i3 = (viewHeight.this.anInt7 * i2) / 50;
                        if ((i3 > 0 || viewHeight.this.downLine + (i3 * 2) < viewHeight.this.anInt1 * 2) && (viewHeight.this.anInt8 < i3 || i3 <= 0)) {
                            return;
                        }
                        viewHeight viewheight = viewHeight.this;
                        viewheight.anInt9 = viewheight.anInt8 - i3;
                        viewHeight viewheight2 = viewHeight.this;
                        viewheight2.anInt10 = viewheight2.anInt11 - i3;
                        viewHeight viewheight3 = viewHeight.this;
                        viewheight3.downLine1 = viewheight3.downLine + (i3 * 2);
                        viewHeight.this.centerBitmap.recycle();
                        viewHeight.this.anInt = i2;
                        viewHeight.this.centerBitmap = null;
                        if (viewHeight.this.downLine1 > 0) {
                            viewHeight viewheight4 = viewHeight.this;
                            viewheight4.centerBitmap = Bitmap.createScaledBitmap(viewheight4.centerImageBitmap, viewHeight.this.currentoriginalBitmap.getWidth(), viewHeight.this.downLine1, true);
                        }
                        viewHeight.this.editing1.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewHeight.this.view1.setImageBitmap(viewHeight.this.centerBitmap);
                            }
                        });
                    }
                }).run();
            }
        }
    }

    public void changeViewswithcopies() {
        int i = this.anInt8;
        this.anInt8 = this.anInt9;
        this.anInt9 = i;
        int i2 = this.anInt11;
        this.anInt11 = this.anInt10;
        this.anInt10 = i2;
        int i3 = this.downLine;
        this.downLine = this.downLine1;
        this.downLine1 = i3;
    }


    public void redo() {
        historyData historydata = this.heightHistories.get(this.mIdCurrent);
        Bitmap createBitmap = historydata.topMiddle > historydata.top ? Bitmap.createBitmap(this.CurrentBitmap, 0, historydata.top, this.currentoriginalBitmap.getWidth(), historydata.topMiddle - historydata.top) : null;
        Bitmap createBitmap2 = Bitmap.createBitmap(this.CurrentBitmap, 0, historydata.topMiddle, this.currentoriginalBitmap.getWidth(), historydata.height);
        Bitmap createBitmap3 = ((this.CurrentBitmap.getHeight() - historydata.topMiddle) - historydata.height) - historydata.top > 0 ? Bitmap.createBitmap(this.CurrentBitmap, 0, historydata.topMiddle + historydata.height, this.currentoriginalBitmap.getWidth(), ((this.CurrentBitmap.getHeight() - historydata.topMiddle) - historydata.height) - historydata.top) : null;
        Bitmap createScaledBitmap = Bitmap.createScaledBitmap(createBitmap2, this.currentoriginalBitmap.getWidth(), historydata.height - ((historydata.finalTop - historydata.top) * 2), true);
        createBitmap2.recycle();
        this.anInt8 = historydata.finalTop;
        this.anInt11 = historydata.topMiddle + (historydata.finalTop - historydata.top);
        this.downLine = historydata.height - ((historydata.finalTop - historydata.top) * 2);
        this.myCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (createBitmap != null) {
            this.myCanvas.drawBitmap(createBitmap, 0.0f, historydata.finalTop, (Paint) null);
            createBitmap.recycle();
        }
        Paint paint = null;
        this.myCanvas.drawBitmap(createScaledBitmap, 0.0f, this.anInt11, paint);
        createScaledBitmap.recycle();
        if (createBitmap3 != null) {
            this.myCanvas.drawBitmap(createBitmap3, 0.0f, this.anInt11 + this.downLine, paint);
            createBitmap3.recycle();
        }
        this.heightImageview.invalidate();
        this.mIdCurrent++;
        this.mIdRequisite++;
        this.customLayouts.setTranslationY((this.aFloat1 + (this.anInt11 * this.heightImageview.getCalculatedMinScale())) - this.anInt2);
        this.view.getLayoutParams().height = (int) (this.downLine * this.heightImageview.getCalculatedMinScale());
        this.view.requestLayout();
    }

//    public viewHeight(Bitmap bitmap, final ImageEditing imageEditing, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick) {
    public viewHeight(Bitmap bitmap, final ImageEditingER imageEditing, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick) {
        this.currentoriginalBitmap = bitmap;
        this.editing1 = imageEditing;
        this.heightImageview = scaleImage;
        this.seekBarView = arcSeekBar;
        this.seek = seekBar;
        this.menuClick = menuClick;
        onCreate();
        this.editing1.li_undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewHeight.this.saveFloatsValue();
                int i = viewHeight.this.mIdRequisite;
                if (i == viewHeight.this.mIdCurrent && i > 0) {
                    int i2 = i - 1;
                    viewHeight.this.mIdRequisite = i2;
                    viewHeight viewheight = viewHeight.this;
                    undoRedoData.getBitmapFromDisk(i, i2, "tool_" + (viewHeight.this.mIdRequisite + 1) + ".png", viewheight, viewheight.editing1);
                    viewHeight.this.editing1.li_undo.setImageDrawable(imageEditing.getResources().getDrawable(R.drawable.ic_new_undo));
                    viewHeight.this.editing1.li_redo.setImageDrawable(imageEditing.getResources().getDrawable(R.drawable.ic_new_redo));
                    return;
                }
                viewHeight.this.editing1.li_undo.setImageDrawable(imageEditing.getResources().getDrawable(R.drawable.ic_disable_undo));
            }
        });
        this.editing1.li_redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int i = viewHeight.this.mIdRequisite;
                if (i == viewHeight.this.mIdCurrent && i < viewHeight.this.mIdLast) {
                    if (!viewHeight.this.isEditmode) {
                        viewHeight.this.redo();
                        viewHeight.this.editing1.li_redo.setImageDrawable(imageEditing.getResources().getDrawable(R.drawable.ic_disable_redo));
                        return;
                    }
                    viewHeight.this.saveFloatsValue();
                    viewHeight.this.editing1.li_redo.setImageDrawable(imageEditing.getResources().getDrawable(R.drawable.ic_disable_redo));
                    return;
                }
                viewHeight.this.editing1.li_redo.setImageDrawable(imageEditing.getResources().getDrawable(R.drawable.ic_disable_redo));
            }
        });
    }

    private void onCreate() {
        this.heightImageview.resetToFitCenter();
        this.heightImageview.setScaleMode(true, false);
        this.mainView = (FrameLayout) this.editing1.findViewById(R.id.main_frameView);
        int round = Math.round(this.currentoriginalBitmap.getHeight() * 1.1f);
        this.anInt8 = (round - this.currentoriginalBitmap.getHeight()) / 2;
        drawCustomViews();
        this.editing1.isBlocked = false;
        Bitmap createBitmap = Bitmap.createBitmap(this.currentoriginalBitmap.getWidth(), round, Bitmap.Config.ARGB_8888);
        this.CurrentBitmap = createBitmap;
        if (!createBitmap.isMutable()) {
            Bitmap copy = this.CurrentBitmap.copy(Bitmap.Config.ARGB_8888, true);
            this.CurrentBitmap.recycle();
            this.CurrentBitmap = copy;
        }
        Canvas canvas = new Canvas(this.CurrentBitmap);
        this.myCanvas = canvas;
        canvas.drawBitmap(this.currentoriginalBitmap, 0.0f, this.anInt8, (Paint) null);
        this.priviewBitmap = this.CurrentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        this.editing1.li_preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (viewHeight.this.priviewBitmap != null && !viewHeight.this.priviewBitmap.isRecycled()) {
                        viewHeight.this.heightImageview.setImageBitmap(viewHeight.this.priviewBitmap);
                    }
                    viewHeight.this.heightImageview.setOnTouchInterface(null);
                    viewHeight.this.seekBarView.setEnabled(false);
                    viewHeight.this.customLayouts.setVisibility(View.INVISIBLE);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (viewHeight.this.CurrentBitmap != null && !viewHeight.this.CurrentBitmap.isRecycled()) {
                        viewHeight.this.heightImageview.setImageBitmap(viewHeight.this.CurrentBitmap);
                    }
                    viewHeight.this.heightImageview.setOnTouchInterface(viewHeight.this);
                    viewHeight.this.seekBarView.setEnabled(true);
                    viewHeight.this.customLayouts.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
        this.seekBarView.setProgress(0);
        this.anInt = 0;
        this.seekBarView.setOnStartTrackingTouch(this.onstartTouchListener);
        this.seekBarView.setOnStopTrackingTouch(this.onstoptTouchListener);
        this.seekBarView.setOnProgressChangedListener(this.onprogressChangeListener);
        this.heightImageview.setImageBitmap(this.CurrentBitmap);
        this.heightImageview.setOnTouchInterface(this);
        ScaleImage scaleImage = this.heightImageview;
        int i = this.anInt3;
        scaleImage.setPadding(i, 0, i, 0);
        this.heightImageview.resetToFitCenterManual();
        float pointXOnScreen = this.heightImageview.getPointXOnScreen(0.0f);
        this.view.getLayoutParams().width = (int) (this.heightImageview.getPointXOnScreen(this.currentoriginalBitmap.getWidth()) - pointXOnScreen);
        int min = Math.min(300, (int) (this.currentoriginalBitmap.getHeight() * this.heightImageview.getCalculatedMinScale()));
        this.view.getLayoutParams().height = min;
        this.anInt1 = (int) (this.anInt2 / this.heightImageview.getCalculatedMinScale());
        float f = min;
        this.anInt11 = (int) ((this.CurrentBitmap.getHeight() - (f / this.heightImageview.getCalculatedMinScale())) / 2.0f);
        this.downLine = (int) (f / this.heightImageview.getCalculatedMinScale());
        this.aFloat1 = (this.heightImageview.getHeight() - (this.CurrentBitmap.getHeight() * this.heightImageview.getCalculatedMinScale())) / 2.0f;
        this.customLayouts.setTranslationX(pointXOnScreen);
        this.customLayouts.setTranslationY((this.aFloat1 + (this.anInt11 * this.heightImageview.getCalculatedMinScale())) - this.anInt2);
        this.linearLayout.setTranslationX(pointXOnScreen);
        this.linearLayout.getLayoutParams().width = (int) (this.heightImageview.getPointXOnScreen(this.currentoriginalBitmap.getWidth()) - pointXOnScreen);
        this.heightImageview.setScaleMode(false, true);
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i2, boolean z) {
                viewHeight.this.seek_pos = i2;
                seekBar.setProgress(i2);
                viewHeight.this.seekBarView.getOnProgressChangedListener().invoke(i2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                viewHeight.this.seekBarView.getOnStartTrackingTouch().invoke(viewHeight.this.seek_pos);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                viewHeight.this.seekBarView.getOnStopTrackingTouch().invoke(viewHeight.this.seek_pos);
            }
        });
    }

    @SuppressLint("ResourceType")
    private void drawCustomViews() {
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(-2, -2);
        ConstraintLayout constraintLayout = new ConstraintLayout(this.editing1);
        this.customLayouts = constraintLayout;
        constraintLayout.setLayoutParams(layoutParams);
        this.customLayouts.setBackgroundColor(0);
        this.customLayouts.setId(10);
        FrameLayout frameLayout = new FrameLayout(this.editing1);
        frameLayout.setId(4);
//        frameLayout.setBackgroundColor(ContextCompat.getColor(this.editing1.getApplicationContext(), R.color.tint_1));
        frameLayout.setBackgroundColor(ContextCompat.getColor(this.editing1.getApplicationContext(), R.color.primaryColor));
        FrameLayout frameLayout2 = new FrameLayout(this.editing1);
//        frameLayout2.setBackgroundColor(ContextCompat.getColor(this.editing1.getApplicationContext(), R.color.tint_1));
        frameLayout2.setBackgroundColor(ContextCompat.getColor(this.editing1.getApplicationContext(), R.color.primaryColor));
        ImageView imageView = new ImageView(this.editing1);
        this.view = imageView;
        imageView.setImageResource(R.drawable.height_red_mask);
        this.view.setId(13);
        this.view.setVisibility(View.INVISIBLE);
        ImageView imageView2 = new ImageView(this.editing1);
        imageView2.setImageResource(R.drawable.iv_body_vertical_1);
        imageView2.setId(3);
        ImageView imageView3 = new ImageView(this.editing1);
        imageView3.setImageResource(R.drawable.iv_body_vertical_1);
        this.customLayouts.addView(this.view);
        this.customLayouts.addView(frameLayout);
        this.customLayouts.addView(frameLayout2);
        this.customLayouts.addView(imageView2);
        this.customLayouts.addView(imageView3);
        ConstraintLayout.LayoutParams layoutParams2 = new ConstraintLayout.LayoutParams(0, this.dencityDp * 2);
        layoutParams2.leftToLeft = 0;
        layoutParams2.topToTop = imageView2.getId();
        layoutParams2.rightToRight = this.view.getId();
        layoutParams2.bottomToBottom = imageView2.getId();
        frameLayout.setLayoutParams(layoutParams2);
        ConstraintLayout.LayoutParams layoutParams3 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams3.leftToLeft = 0;
        layoutParams3.topToTop = frameLayout.getId();
        layoutParams3.topMargin = this.dencityDp;
        this.view.setLayoutParams(layoutParams3);
        ConstraintLayout.LayoutParams layoutParams4 = new ConstraintLayout.LayoutParams(0, this.dencityDp * 2);
        layoutParams4.leftToLeft = 0;
        layoutParams4.topToBottom = this.view.getId();
        layoutParams4.bottomToBottom = this.view.getId();
        layoutParams4.rightToRight = this.view.getId();
        frameLayout2.setLayoutParams(layoutParams4);
        ConstraintLayout.LayoutParams layoutParams5 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams5.topToTop = 0;
        layoutParams5.rightToRight = this.view.getId();
        layoutParams5.leftToRight = this.view.getId();
        imageView2.setLayoutParams(layoutParams5);
        ConstraintLayout.LayoutParams layoutParams6 = new ConstraintLayout.LayoutParams(-2, -2);
        layoutParams6.topToBottom = this.view.getId();
        layoutParams6.bottomToBottom = this.view.getId();
        layoutParams6.rightToRight = this.view.getId();
        layoutParams6.leftToRight = this.view.getId();
        imageView3.setLayoutParams(layoutParams6);
        if (imageView2.getDrawable() != null) {
            this.anInt2 = imageView2.getDrawable().getIntrinsicHeight() / 2;
            this.anInt3 = imageView2.getDrawable().getIntrinsicWidth() / 2;
        }
        this.mainView.addView(this.customLayouts, 1);
        LinearLayout linearLayout = new LinearLayout(this.editing1);
        this.linearLayout = linearLayout;
        linearLayout.setLayoutParams(new ConstraintLayout.LayoutParams(0, this.heightImageview.getHeight()));
        this.linearLayout.setGravity(16);
        this.linearLayout.setOrientation(1);
        LinearLayout.LayoutParams layoutParams7 = new LinearLayout.LayoutParams(-1, -2);
        ImageView imageView4 = new ImageView(this.editing1);
        this.upImage = imageView4;
        imageView4.setLayoutParams(layoutParams7);
        this.upImage.setAdjustViewBounds(true);
        ImageView imageView5 = new ImageView(this.editing1);
        this.view1 = imageView5;
        imageView5.setLayoutParams(layoutParams7);
        this.view1.setAdjustViewBounds(true);
        ImageView imageView6 = new ImageView(this.editing1);
        this.downImage = imageView6;
        imageView6.setLayoutParams(layoutParams7);
        this.downImage.setAdjustViewBounds(true);
        this.linearLayout.addView(this.upImage);
        this.linearLayout.addView(this.view1);
        this.linearLayout.addView(this.downImage);
        this.mainView.addView(this.linearLayout, 1);
        this.linearLayout.setVisibility(View.INVISIBLE);
    }

    private void close(boolean z) {
        for (int i = 0; i <= this.mIdLast; i++) {
//            ImageEditing imageEditing = this.editing1;
            ImageEditingER imageEditing = this.editing1;
            imageEditing.deleteFile("tool_" + i + ".png");
        }
        this.mIdCurrent = -1;
        this.CurrentBitmap.recycle();
        this.priviewBitmap.recycle();
        if (this.isEditmode) {
            Bitmap bitmap = this.upBitmap;
            if (bitmap != null) {
                bitmap.recycle();
            }
            this.centerImageBitmap.recycle();
            this.centerBitmap.recycle();
            Bitmap bitmap2 = this.downBitmap;
            if (bitmap2 != null) {
                bitmap2.recycle();
            }
        }
        this.heightHistories.clear();
        this.customLayouts.removeAllViews();
        this.mainView.removeView(this.customLayouts);
        this.linearLayout.removeAllViews();
        this.mainView.removeView(this.linearLayout);
        this.priviewBitmap.recycle();
        this.heightImageview.setPadding(0, 0, 0, 0);
        this.seekBarView.setOnStartTrackingTouch(null);
        this.seekBarView.setOnStopTrackingTouch(null);
        this.seekBarView.setOnProgressChangedListener(null);
        this.heightImageview.setOnTouchInterface(null);
        this.editing1.li_preview.setOnTouchListener(this.editing1.previewTouchListener);
        this.heightImageview.setImageBitmap(this.currentoriginalBitmap);
        this.heightImageview.resetToFitCenterManual();
        this.heightImageview.setScaleMode(false, true);
        this.editing1.li_undo.setOnClickListener(this.editing1.undoClick);
        this.editing1.li_redo.setOnClickListener(this.editing1.redoClick);
    }

    private void save() {
        Bitmap createBitmap = Bitmap.createBitmap(this.CurrentBitmap, 0, this.anInt8, this.currentoriginalBitmap.getWidth(), this.CurrentBitmap.getHeight() - (this.anInt8 * 2));
        this.editing1.mCurrentBitmap.recycle();
        if (!createBitmap.isMutable()) {
            this.editing1.mCurrentBitmap = createBitmap.copy(Bitmap.Config.ARGB_8888, true);
            this.currentoriginalBitmap = this.editing1.mCurrentBitmap;
        } else {
            this.editing1.mCurrentBitmap = createBitmap;
            this.currentoriginalBitmap = createBitmap;
        }
        this.editing1.img_person1.setImageBitmap(this.editing1.mCurrentBitmap);
        this.editing1.addMainState();
    }

    @Override
    public void onBackPressed(boolean z) {
        if (z) {
            save();
        } else {
            close(z);
        }
    }


    public void saveFloatsValue() {
        if (this.isEditmode) {
            this.isEditmode = false;
            this.seekBarView.setProgress(0);
            this.anInt = 0;
            int i = this.mIdCurrent + 1;
            this.mIdCurrent = i;
            while (i <= this.mIdLast) {
                this.editing1.deleteFile("tool_" + i + ".png");
                List<historyData> list = this.heightHistories;
                list.remove(list.size() - 1);
                i++;
            }
            int i2 = this.mIdCurrent;
            this.mIdLast = i2;
            this.mIdRequisite = i2;
            this.heightHistories.add(new historyData(this.anInt9, this.downLine1, this.anInt10, this.anInt8));
            final String str = "tool_" + this.mIdCurrent + ".png";
            final Bitmap copy = this.centerImageBitmap.copy(Bitmap.Config.ARGB_8888, true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileOutputStream openFileOutput = viewHeight.this.editing1.openFileOutput(str, 0);
                        copy.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput);
                        copy.recycle();
                        openFileOutput.close();
                        if (viewHeight.this.mIdCurrent == -1) {
                            viewHeight.this.editing1.deleteFile(str);
                        }
                    } catch (Exception e) {
                        Log.d("My", "Error (save Bitmap): " + e.getMessage());
                    }
                }
            }).start();
            Bitmap bitmap = this.upBitmap;
            if (bitmap != null) {
                bitmap.recycle();
            }
            this.centerImageBitmap.recycle();
            Bitmap bitmap2 = this.downBitmap;
            if (bitmap2 != null) {
                bitmap2.recycle();
            }
            this.centerBitmap.recycle();
        }
    }

    @Override
    public void touch(int i, float f, float f2, float f3) {
        if (i == 0) {
            this.aBoolean = true;
            this.seekBarView.setEnabled(false);
            int i2 = this.anInt11;
            int i3 = this.anInt1;
            if (f2 >= i2 - i3 && f2 <= i2 + this.downLine + i3) {
                this.view.setVisibility(View.VISIBLE);
                saveFloatsValue();
            }
            int i4 = this.anInt11;
            int i5 = this.anInt1;
            if (f2 < i4 - i5 || f2 > i4 + i5) {
                if (f2 <= i4 + i5 || f2 >= (this.downLine + i4) - i5) {
                    int i6 = i4 + this.downLine;
                    if (f2 < i6 - i5 || f2 > i6 + i5) {
                        this.anInt12 = -1;
                    } else {
                        this.anInt12 = 2;
                    }
                } else {
                    this.anInt12 = 1;
                }
            } else {
                this.anInt12 = 0;
            }
            this.aFloat = f2;
        } else if (i != 1) {
            if (i == 2) {
                this.aBoolean = false;
                this.seekBarView.setEnabled(true);
                this.view.setVisibility(View.INVISIBLE);
            }
        } else if (this.aBoolean) {
            int i7 = this.anInt12;
            if (i7 == 0) {
                float f4 = this.aFloat;
                float f5 = f4 - f2;
                if (f5 < 0.0f) {
                    int i8 = this.downLine;
                    float f6 = i8;
                    if ((f4 + f6) - f2 >= this.anInt1 * 2) {
                        int i9 = (int) (f6 + f5);
                        this.downLine = i9;
                        this.anInt11 += i8 - i9;
                        this.view.getLayoutParams().height = (int) (this.downLine * this.heightImageview.getCalculatedMinScale());
                        this.customLayouts.setTranslationY((this.aFloat1 + (this.anInt11 * this.heightImageview.getCalculatedMinScale())) - this.anInt2);
                    } else {
                        this.anInt12 = 2;
                    }
                } else {
                    int i10 = this.downLine;
                    int min = Math.min((this.anInt11 + i10) - this.anInt8, (int) ((i10 + f4) - f2));
                    this.downLine = min;
                    this.anInt11 -= min - i10;
                    this.view.getLayoutParams().height = (int) (this.downLine * this.heightImageview.getCalculatedMinScale());
                    this.customLayouts.setTranslationY((this.aFloat1 + (this.anInt11 * this.heightImageview.getCalculatedMinScale())) - this.anInt2);
                }
                this.view.requestLayout();
            } else if (i7 == 1) {
                float f7 = this.aFloat;
                if (f7 - f2 > 0.0f) {
                    this.anInt11 = (int) Math.max(this.anInt8, (this.anInt11 + f2) - f7);
                } else {
                    this.anInt11 = (int) Math.min((this.CurrentBitmap.getHeight() - this.anInt8) - this.downLine, (this.anInt11 + f2) - this.aFloat);
                }
                this.customLayouts.setTranslationY((this.aFloat1 + (this.anInt11 * this.heightImageview.getCalculatedMinScale())) - this.anInt2);
            } else if (i7 == 2) {
                float f8 = this.aFloat;
                float f9 = f8 - f2;
                if (f9 > 0.0f) {
                    float f10 = this.downLine;
                    if ((f10 - f8) + f2 >= this.anInt1 * 2) {
                        this.downLine = (int) (f10 - f9);
                        this.view.getLayoutParams().height = (int) (this.downLine * this.heightImageview.getCalculatedMinScale());
                    } else {
                        this.anInt12 = 0;
                    }
                } else {
                    this.downLine = Math.min((this.CurrentBitmap.getHeight() - this.anInt8) - this.anInt11, (int) ((this.downLine + f2) - this.aFloat));
                    this.view.getLayoutParams().height = (int) (this.downLine * this.heightImageview.getCalculatedMinScale());
                }
                this.view.requestLayout();
            }
            this.aFloat = f2;
        }
    }

    @Override
    public void loadResponse(Bitmap bitmap, int i, int i2) {
        if (bitmap != null) {
            historyData historydata = this.heightHistories.get(this.mIdRequisite);
            Bitmap createBitmap = historydata.topMiddle > historydata.top ? Bitmap.createBitmap(this.CurrentBitmap, 0, historydata.finalTop, this.currentoriginalBitmap.getWidth(), historydata.topMiddle - historydata.top) : null;
            Bitmap createBitmap2 = ((this.CurrentBitmap.getHeight() - historydata.topMiddle) - historydata.height) - historydata.top > 0 ? Bitmap.createBitmap(this.CurrentBitmap, 0, (historydata.topMiddle + historydata.height) - (historydata.finalTop - historydata.top), this.currentoriginalBitmap.getWidth(), ((this.CurrentBitmap.getHeight() - historydata.topMiddle) - historydata.height) - historydata.top) : null;
            this.anInt8 = historydata.top;
            this.anInt11 = historydata.topMiddle;
            this.downLine = historydata.height;
            this.myCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            if (createBitmap != null) {
                this.myCanvas.drawBitmap(createBitmap, 0.0f, historydata.top, (Paint) null);
                createBitmap.recycle();
            }
            Paint paint = null;
            this.myCanvas.drawBitmap(bitmap, 0.0f, historydata.topMiddle, paint);
            bitmap.recycle();
            if (createBitmap2 != null) {
                this.myCanvas.drawBitmap(createBitmap2, 0.0f, historydata.topMiddle + historydata.height, paint);
                createBitmap2.recycle();
            }
            this.heightImageview.invalidate();
            this.mIdCurrent = i2;
            this.mIdRequisite = i2;
            this.customLayouts.setTranslationY((this.aFloat1 + (this.anInt11 * this.heightImageview.getCalculatedMinScale())) - this.anInt2);
            this.view.getLayoutParams().height = (int) (this.downLine * this.heightImageview.getCalculatedMinScale());
            this.view.requestLayout();
            return;
        }
        this.mIdRequisite = i;
    }


    public class historyData {
        int finalTop;
        int height;
        int top;
        int topMiddle;

        historyData(int i, int i2, int i3, int i4) {
            this.top = i;
            this.finalTop = i4;
            this.height = i2;
            this.topMiddle = i3;
        }
    }
}
