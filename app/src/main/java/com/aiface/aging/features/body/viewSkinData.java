package com.aiface.aging.features.body;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;
import com.aiface.aging.R;
import com.aiface.aging.features.body.activities.ImageEditingER;
import com.aiface.aging.features.body.adapter.SkinColorAdapter;
import com.aiface.aging.features.body.controls.ScaleImage;
import com.aiface.aging.features.body.controls.undoRedoData;
import com.aiface.aging.features.body.fragment.BodyEditFragment;
import com.aiface.aging.features.body.inerfaces.MenuClick;

import java.io.FileOutputStream;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSoftLightBlendFilter;


public class viewSkinData implements ScaleImage.TouchInterface, BodyEditFragment.BackPressed, undoRedoData.PhotoLoadResponse {
    public RelativeLayout SeekView;
    public int alpha;
    private Bitmap colorBitmap;
    public int currentId;
    private Bitmap currentOriginalBitmap;
    public Bitmap dataBitmap;
    BodyEditFragment fragment;
    public GPUImage gpuImage;
    private boolean isDrawing;
    private boolean isMoved;
    private int lastId;
    private float lastX;
    private float lastY;
//    public ImageEditing mActivity;
    public ImageEditingER mActivity;
    public Paint mChangeColorPaint;
    private SkinColorAdapter mColorAdapter;
    public Canvas mColorCanvas;
    private int mIdRequisite;
    private ScaleImage mScaleImage;
    private int maxSizeOfBitmap;
    private MenuClick menuClick;
    public Canvas myCanvasView;
    public Bitmap paintBitmap;
    public Canvas paintCanvas;
    public Paint paintData;
    private Paint paintData1;
    public Paint paintData2;
    public Paint paintEdata;
    public Paint paintErData;
    public Runnable runnable;
    private final RecyclerView rv_sub_items_face;
    SeekBar seek;
    private ArcSeekBar seekBarView;
    public final Handler handler = new Handler();
    private SkinColorAdapter.ItemClick mColorClickListener = new SkinColorAdapter.ItemClick() {
        @Override
        public void onItemClick(final int i) {
            viewSkinData.this.mActivity.isBlocked = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    viewSkinData.this.paintData = viewSkinData.this.paintData2;
                    viewSkinData.this.mColorCanvas.drawColor(Constant.listOfSkinColor[i]);
                    viewSkinData.this.paintCanvas.drawBitmap(viewSkinData.this.gpuImage.getBitmapWithFilterApplied(), 0.0f, 0.0f, (Paint) null);
                    viewSkinData.this.handler.post(viewSkinData.this.runnable);
                }
            }).start();
        }
    };
    public BitmapDrawable[] mLayers = new BitmapDrawable[2];
    private Paint mSimplePaint = new Paint();
    int seek_pos = 0;

//    public viewSkinData(Bitmap bitmap, BodyEditFragment bodyEditFragment, ImageEditing imageEditing, ScaleImage scaleImage, RecyclerView recyclerView, ArcSeekBar arcSeekBar, RelativeLayout relativeLayout, SkinColorAdapter skinColorAdapter, SeekBar seekBar, MenuClick menuClick) {
    public viewSkinData(Bitmap bitmap, BodyEditFragment bodyEditFragment, ImageEditingER imageEditing, ScaleImage scaleImage, RecyclerView recyclerView, ArcSeekBar arcSeekBar, RelativeLayout relativeLayout, SkinColorAdapter skinColorAdapter, SeekBar seekBar, MenuClick menuClick) {
        this.currentOriginalBitmap = bitmap;
        this.mActivity = imageEditing;
        this.fragment = bodyEditFragment;
        this.mScaleImage = scaleImage;
        this.seekBarView = arcSeekBar;
        this.SeekView = relativeLayout;
        this.seek = seekBar;
        this.rv_sub_items_face = recyclerView;
        this.mColorAdapter = skinColorAdapter;
        this.menuClick = menuClick;
        setInitialValues();
//        this.fragment.img_draw.setColorFilter(ContextCompat.getColor(this.mActivity.getApplicationContext(), R.color.theme_Color));
        this.fragment.img_draw.setColorFilter(ContextCompat.getColor(this.mActivity.getApplicationContext(), R.color.primaryColor));
        this.fragment.img_eraser.setColorFilter(ContextCompat.getColor(this.mActivity.getApplicationContext(), R.color.icon_primary));
        this.fragment.txt_eraser.setTextColor(ContextCompat.getColor(this.mActivity.getApplicationContext(), R.color.text_secondary));
//        this.fragment.txt_draw.setTextColor(ContextCompat.getColor(this.mActivity.getApplicationContext(), R.color.theme_Color));
        this.fragment.txt_draw.setTextColor(ContextCompat.getColor(this.mActivity.getApplicationContext(), R.color.primaryColor));
        this.fragment.li_erase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                viewSkinData.this.fragment.img_eraser.setColorFilter(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.theme_Color));
                viewSkinData.this.fragment.img_eraser.setColorFilter(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.primaryColor));
                viewSkinData.this.fragment.img_draw.setColorFilter(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.icon_primary));
//                viewSkinData.this.fragment.txt_eraser.setTextColor(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.theme_Color));
                viewSkinData.this.fragment.txt_eraser.setTextColor(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.primaryColor));
                viewSkinData.this.fragment.txt_draw.setTextColor(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.icon_primary));
                viewSkinData viewskindata = viewSkinData.this;
                viewskindata.paintData = viewskindata.paintErData;
            }
        });
        this.fragment.li_draw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                viewSkinData.this.fragment.img_draw.setColorFilter(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.theme_Color));
                viewSkinData.this.fragment.img_draw.setColorFilter(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.primaryColor));
                viewSkinData.this.fragment.img_eraser.setColorFilter(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.icon_primary));
                viewSkinData.this.fragment.txt_eraser.setTextColor(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.text_secondary));
//                viewSkinData.this.fragment.txt_draw.setTextColor(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.theme_Color));
                viewSkinData.this.fragment.txt_draw.setTextColor(ContextCompat.getColor(viewSkinData.this.mActivity.getApplicationContext(), R.color.primaryColor));
                viewSkinData viewskindata = viewSkinData.this;
                viewskindata.paintData = viewskindata.paintData2;
            }
        });
        this.mActivity.li_undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewSkinData.this.mIdRequisite >= 1) {
                    int i = viewSkinData.this.mIdRequisite;
                    if (i > 1) {
                        int i2 = i - 1;
                        viewSkinData.this.mIdRequisite = i2;
                        viewSkinData viewskindata = viewSkinData.this;
                        undoRedoData.getBitmapFromDisk(i, i2, "tool_" + viewSkinData.this.mIdRequisite + ".jpg", viewskindata, viewskindata.mActivity);
                        viewSkinData.this.mActivity.li_undo.setImageDrawable(viewSkinData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_undo));
                        viewSkinData.this.mActivity.li_redo.setImageDrawable(viewSkinData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
                        return;
                    }
                    viewSkinData.this.mIdRequisite = 0;
                    viewSkinData.this.currentId = 0;
                    viewSkinData.this.myCanvasView.drawColor(0, PorterDuff.Mode.CLEAR);
                    viewSkinData.this.mLayers[1].invalidateSelf();
                    viewSkinData.this.mActivity.li_undo.setImageDrawable(viewSkinData.this.mActivity.getResources().getDrawable(R.drawable.ic_disable_undo));
                }
            }
        });
        this.mActivity.li_redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int i = viewSkinData.this.mIdRequisite;
                if (i < viewSkinData.this.lastId) {
                    int i2 = i + 1;
                    viewSkinData.this.mIdRequisite = i2;
                    viewSkinData viewskindata = viewSkinData.this;
                    undoRedoData.getBitmapFromDisk(i, i2, "tool_" + viewSkinData.this.mIdRequisite + ".jpg", viewskindata, viewskindata.mActivity);
                    viewSkinData.this.mActivity.li_undo.setImageDrawable(viewSkinData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_undo));
                    viewSkinData.this.mActivity.li_redo.setImageDrawable(viewSkinData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
                    return;
                }
                viewSkinData.this.mActivity.li_redo.setImageDrawable(viewSkinData.this.mActivity.getResources().getDrawable(R.drawable.ic_disable_redo));
            }
        });
    }

    public void onCreate() {
        this.mColorAdapter.setItemClick(this.mColorClickListener);
        Paint paint = new Paint(1);
        this.paintData2 = paint;
        paint.setShader(new BitmapShader(this.paintBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        this.paintData2.setStyle(Paint.Style.STROKE);
        this.paintData2.setStrokeJoin(Paint.Join.ROUND);
        this.paintData2.setStrokeCap(Paint.Cap.ROUND);
        Paint paint2 = new Paint(1);
        this.paintErData = paint2;
        paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.paintErData.setStyle(Paint.Style.STROKE);
        this.paintErData.setStrokeJoin(Paint.Join.ROUND);
        this.paintErData.setStrokeCap(Paint.Cap.ROUND);
        Paint paint3 = new Paint();
        this.paintData1 = paint3;
        paint3.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        this.paintData1.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f})));
        Paint paint4 = new Paint();
        this.paintEdata = paint4;
        paint4.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f})));
        this.paintData = this.paintData2;
        Bitmap createBitmap = Bitmap.createBitmap(this.currentOriginalBitmap.getWidth(), this.currentOriginalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        this.dataBitmap = createBitmap;
        if (!createBitmap.isMutable()) {
            Bitmap copy = this.dataBitmap.copy(Bitmap.Config.ARGB_8888, true);
            this.dataBitmap.recycle();
            this.dataBitmap = copy;
        }
        this.maxSizeOfBitmap = Math.max(this.currentOriginalBitmap.getWidth(), this.currentOriginalBitmap.getHeight());
        this.myCanvasView = new Canvas(this.dataBitmap);
        this.mLayers[0] = new BitmapDrawable(this.mActivity.getResources(), this.currentOriginalBitmap);
        this.mLayers[1] = new BitmapDrawable(this.mActivity.getResources(), this.dataBitmap);
        this.mScaleImage.setImageDrawable(new LayerDrawable(this.mLayers));
        this.mScaleImage.setOnTouchInterface(this);
        this.alpha = 255;
        this.seekBarView.setProgress(100);
        this.seekBarView.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int i) {
                viewSkinData.this.alpha = Math.round(i * 2.55f);
                viewSkinData.this.mLayers[1].setAlpha(viewSkinData.this.alpha);
            }
        });
        this.mActivity.li_preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (viewSkinData.this.mLayers != null && viewSkinData.this.mLayers.length > 1
                            && viewSkinData.this.mLayers[1] != null) {
                        viewSkinData.this.mLayers[1].setAlpha(0);
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (viewSkinData.this.mLayers != null && viewSkinData.this.mLayers.length > 1
                            && viewSkinData.this.mLayers[1] != null) {
                        viewSkinData.this.mLayers[1].setAlpha(viewSkinData.this.alpha);
                    }
                }
                return true;
            }
        });
        this.mLayers[1].setAlpha(255);
        this.mActivity.isBlocked = false;
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                viewSkinData.this.seek_pos = i;
                seekBar.setProgress(i);
                viewSkinData.this.seekBarView.getOnProgressChangedListener().invoke(i);
            }
        });
    }

    private void setInitialValues() {
        Paint paint = new Paint();
        this.mChangeColorPaint = paint;
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        Bitmap createBitmap = Bitmap.createBitmap(this.currentOriginalBitmap.getWidth(), this.currentOriginalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        this.colorBitmap = createBitmap;
        if (!createBitmap.isMutable()) {
            Bitmap copy = this.colorBitmap.copy(Bitmap.Config.ARGB_8888, true);
            this.colorBitmap.recycle();
            this.colorBitmap = copy;
        }
        this.mColorCanvas = new Canvas(this.colorBitmap);
        GPUImage gPUImage1 = new GPUImage(this.mActivity);
        this.gpuImage = gPUImage1;
        gPUImage1.setImage(this.currentOriginalBitmap);
        GPUImageSoftLightBlendFilter gPUImageSoftLightBlendFilter = new GPUImageSoftLightBlendFilter();
        gPUImageSoftLightBlendFilter.setBitmap(this.colorBitmap);
        this.gpuImage.setFilter(gPUImageSoftLightBlendFilter);
        this.runnable = new Runnable() {
            @Override
            public void run() {
                viewSkinData.this.mActivity.isBlocked = false;
                viewSkinData.this.myCanvasView.drawBitmap(viewSkinData.this.paintBitmap, 0.0f, 0.0f, viewSkinData.this.mChangeColorPaint);
                viewSkinData.this.mLayers[1].invalidateSelf();
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                viewSkinData.this.mColorCanvas.drawColor(Constant.listOfSkinColor[0]);
                viewSkinData viewskindata = viewSkinData.this;
                viewskindata.paintBitmap = viewskindata.gpuImage.getBitmapWithFilterApplied();
                if (!viewSkinData.this.paintBitmap.isMutable()) {
                    Bitmap copy2 = viewSkinData.this.paintBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    viewSkinData.this.paintBitmap.recycle();
                    viewSkinData.this.paintBitmap = copy2;
                }
                viewSkinData.this.paintCanvas = new Canvas(viewSkinData.this.paintBitmap);
                viewSkinData.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        viewSkinData.this.mActivity.isBlocked = false;
                        viewSkinData.this.onCreate();
                    }
                });
            }
        }).start();
    }

    private void close(boolean z) {
        for (int i = 0; i <= this.lastId; i++) {
//            ImageEditing imageEditing = this.mActivity;
            ImageEditingER imageEditing = this.mActivity;
            imageEditing.deleteFile("tool_" + i + ".jpg");
        }
        this.currentId = -1;
        this.dataBitmap.recycle();
        this.mActivity.li_preview.setOnTouchListener(this.mActivity.previewTouchListener);
        this.mScaleImage.setImageBitmap(this.mActivity.mCurrentBitmap);
        this.paintBitmap.recycle();
        this.colorBitmap.recycle();
        this.mScaleImage.setOnTouchInterface(null);
        this.seekBarView.setOnStartTrackingTouch(null);
        this.seekBarView.setOnStopTrackingTouch(null);
        this.seekBarView.setOnProgressChangedListener(null);
        this.mActivity.li_undo.setOnClickListener(this.mActivity.undoClick);
        this.mActivity.li_redo.setOnClickListener(this.mActivity.redoClick);
    }

    private void save() {
        this.mSimplePaint.setAlpha(this.alpha);
        Bitmap baseBitmap = this.mActivity.mCurrentBitmap;
        if (baseBitmap == null || baseBitmap.isRecycled()) {
            baseBitmap = this.currentOriginalBitmap;
        }
        Bitmap result = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
        new Canvas(result).drawBitmap(this.dataBitmap, 0.0f, 0.0f, this.mSimplePaint);
        this.mActivity.mCurrentBitmap = result;
        this.mActivity.img_person1.setImageBitmap(result);
        this.mActivity.addMainState();
    }

    @Override
    public void touch(int i, float f, float f2, float f3) {
        if (i == 0) {
            float f4 = this.maxSizeOfBitmap / (f3 * 20.0f);
            if (f4 != this.paintData.getStrokeWidth()) {
                this.paintData.setStrokeWidth(f4);
                this.paintData.setMaskFilter(new BlurMaskFilter(f4 / 2.0f, BlurMaskFilter.Blur.SOLID));
            }
            this.SeekView.setVisibility(View.INVISIBLE);
            this.rv_sub_items_face.setVisibility(View.INVISIBLE);
            this.lastX = f;
            this.lastY = f2;
            this.isDrawing = true;
            this.isMoved = false;
        } else if (i == 1) {
            if (this.isDrawing) {
                this.isMoved = true;
                this.myCanvasView.drawLine(this.lastX, this.lastY, f, f2, this.paintData);
                this.lastX = f;
                this.lastY = f2;
                this.mLayers[1].invalidateSelf();
            }
        } else if (i == 2) {
            if (this.isMoved) {
                int i2 = this.currentId + 1;
                this.currentId = i2;
                while (i2 <= this.lastId) {
                    this.mActivity.deleteFile("tool_" + i2 + ".jpg");
                    i2++;
                }
                int i3 = this.currentId;
                this.lastId = i3;
                this.mIdRequisite = i3;
                Bitmap createBitmap = Bitmap.createBitmap(this.currentOriginalBitmap.getWidth(), this.currentOriginalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                if (!createBitmap.isMutable()) {
                    createBitmap.recycle();
                    createBitmap = this.currentOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                }
                final String str = "tool_" + this.currentId + ".jpg";
                final Handler handler = new Handler();
                Bitmap finalCreateBitmap = createBitmap;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new Canvas(finalCreateBitmap).drawBitmap(viewSkinData.this.dataBitmap, 0.0f, 0.0f, viewSkinData.this.paintEdata);
                            FileOutputStream openFileOutput = viewSkinData.this.mActivity.openFileOutput(str, 0);
                            finalCreateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, openFileOutput);
                            openFileOutput.close();
                            if (viewSkinData.this.currentId == -1) {
                                viewSkinData.this.mActivity.deleteFile(str);
                            }
                        } catch (Exception e) {
                            Log.d("My", "Error (save Bitmap): " + e.getMessage());
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                finalCreateBitmap.recycle();
                            }
                        });
                    }
                }).start();
            }
            this.rv_sub_items_face.setVisibility(View.VISIBLE);
            this.SeekView.setVisibility(View.VISIBLE);
            this.isDrawing = false;
        }
    }

    @Override
    public void onBackPressed(boolean z) {
        if (z) {
            save();
        } else {
            close(z);
        }
    }

    @Override
    public void loadResponse(Bitmap bitmap, int i, int i2) {
        if (bitmap != null) {
            if ((i2 > i && this.currentId < i2) || (i2 < i && i2 < this.currentId)) {
                this.myCanvasView.drawBitmap(this.paintBitmap, 0.0f, 0.0f, this.mSimplePaint);
                this.myCanvasView.drawBitmap(bitmap, 0.0f, 0.0f, this.paintData1);
                this.mLayers[1].invalidateSelf();
                this.currentId = i2;
                this.mIdRequisite = i2;
            }
            bitmap.recycle();
            return;
        }
        this.mIdRequisite = i;
    }
}
