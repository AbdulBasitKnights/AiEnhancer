package com.aiface.aging.features.body;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

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

public class vieenhanceeeData implements BodyEditFragment.BackPressed, View.OnTouchListener, ScaleImage.ScaleAndMoveInterface, undoRedoData.PhotoLoadResponse {
    public int bitmapCurrentSize;
    public int column;
    public int currentId;
    public Bitmap currentOriginalBitmap;
    public StickerView customLayouts;
    private List<waistHistoryData> history = new ArrayList();
    private float initialWidth;
    private float initialX;
    private float initialY;
    private float initialheight;
    public boolean isEditmode;

    public int lastId;
    private float lastX;
    private float lastY;
//    public ImageEditing mActivity;
    public ImageEditingER mActivity;
    public Bitmap mCurrentBitmap;
    private int mHeight;

    public int mIdRequisite;
    private int mMinHeight;
    private int mMinWidth;
    private int mWidth;
    private FrameLayout mainView;
    public float[] matrixValues = new float[9];
    public float[] matrixValues11 = new float[9];
    private int maxBitmapSize;
    public float[] maxVertesValues;
    private MenuClick menuClick;
    public Canvas myCanvasView;
    private ProgressListener onprogressChangeListener = new ProgressListener() {
        public void invoke(final int i) {
            if (vieenhanceeeData.this.isEditmode) {
                new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < vieenhanceeeData.this.vertsNumbers; i += 2) {
                            int i2 = i / 2;
                            float f = ((float) (i2 % (vieenhanceeeData.this.column + 1))) * vieenhanceeeData.this.step;
                            float f2 = ((float) (i2 / (vieenhanceeeData.this.column + 1))) * vieenhanceeeData.this.step;
                            float f3 = ((float) i) / 75.0f;
                            vieenhanceeeData.this.vertexesmesh[i] = f + (vieenhanceeeData.this.maxVertesValues[i] * f3);
                            int i3 = i + 1;
                            vieenhanceeeData.this.vertexesmesh[i3] = f2 + (vieenhanceeeData.this.maxVertesValues[i3] * f3);
                        }
                        Bitmap createBitmap = Bitmap.createBitmap(vieenhanceeeData.this.originalMeshArea.getWidth(), vieenhanceeeData.this.originalMeshArea.getHeight(), Bitmap.Config.ARGB_8888);
                        if (!createBitmap.isMutable()) {
                            createBitmap.recycle();
                            createBitmap = vieenhanceeeData.this.originalMeshArea.copy(Bitmap.Config.ARGB_8888, true);
                        }
                        Paint paint = null;
                        new Canvas(createBitmap).drawBitmapMesh(vieenhanceeeData.this.originalMeshArea, vieenhanceeeData.this.column, vieenhanceeeData.this.column, vieenhanceeeData.this.vertexesmesh, 0, (int[]) null, 0, paint);
                        vieenhanceeeData.this.myCanvasView.drawBitmap(createBitmap, (float) vieenhanceeeData.this.xStart, (float) vieenhanceeeData.this.yStart, paint);
                        createBitmap.recycle();
                        vieenhanceeeData.this.mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                vieenhanceeeData.this.waistImageView.invalidate();
                            }
                        });
                    }
                }).run();
            }
        }
    };
    private ProgressListener onstartTouchListener = new ProgressListener() {
        public void invoke(int i) {
            if (!vieenhanceeeData.this.isEditmode) {
                vieenhanceeeData.this.startEditing();
            }
        }
    };
    private ProgressListener onstoptTouchListener = new ProgressListener() {
        public void invoke(int i) {
            if (!vieenhanceeeData.this.isEditmode) {
                vieenhanceeeData.this.seekBarView.setProgress(0);
            }
        }
    };
    public Bitmap originalMeshArea;
    SeekBar seek;
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
    public float step;
    public float[] vertexesmesh;
    public int vertsNumbers;
    public ScaleImage waistImageView;
    public int xStart;
    public int yStart;


    public void startEditing() {
        this.waistImageView.getImageMatrix().getValues(this.matrixValues);
        this.customLayouts.getImageMatrix().getValues(this.matrixValues11);
        float[] fArr = this.matrixValues11;
        float f = fArr[2];
        float f2 = fArr[5];
        this.xStart = (int) (((f - this.matrixValues[2]) - ((float) this.waistImageView.getPaddingLeft())) / this.matrixValues[0]);
        Log.e("JHSdgcDsfcfg", this.customLayouts.getWidth() + "::" + this.customLayouts.getW() + "::" + this.customLayouts.getH() + ">::" + this.customLayouts.getHeight());
        this.yStart = (int) (((f2 - this.matrixValues[5]) - ((float) this.waistImageView.getPaddingTop())) / this.matrixValues[4]);
        int w = (int) ((((f + this.customLayouts.getW()) - this.matrixValues[2]) - ((float) this.waistImageView.getPaddingLeft())) / this.matrixValues[0]);
        int w2 = (int) ((((f2 + this.customLayouts.getW()) - this.matrixValues[5]) - ((float) this.waistImageView.getPaddingTop())) / this.matrixValues[4]);
        if (w >= 1 && w2 >= 1 && this.xStart < this.currentOriginalBitmap.getWidth() && this.yStart < this.currentOriginalBitmap.getHeight()) {
            this.isEditmode = true;
            this.originalMeshArea = Bitmap.createBitmap(w - this.xStart, w2 - this.yStart, Bitmap.Config.ARGB_8888);
            new Canvas(this.originalMeshArea).drawBitmap(this.mCurrentBitmap, (float) (-this.xStart), (float) (-this.yStart), (Paint) null);
            int min = Math.min((int) (((float) (w - this.xStart)) / 5.0f), 10);
            this.column = min;
            int i = (min + 1) * (min + 1) * 2;
            this.vertsNumbers = i;
            this.maxVertesValues = new float[i];
            this.vertexesmesh = new float[i];
            this.step = ((float) this.originalMeshArea.getWidth()) / ((float) this.column);
            float width = ((float) this.originalMeshArea.getWidth()) / 2.0f;
            float width2 = ((float) this.originalMeshArea.getWidth()) / 2.0f;
            for (int i2 = 0; i2 < this.vertsNumbers; i2 += 2) {
                int i3 = i2 / 2;
                int i4 = this.column;
                float f3 = this.step;
                float f4 = (((float) (i3 % (i4 + 1))) * f3) - width2;
                float f5 = (((float) (i3 / (i4 + 1))) * f3) - width2;
                float sqrt = (float) Math.sqrt(Math.pow((double) f4, 2.0d) + Math.pow((double) f5, 2.0d));
                if (sqrt < width) {
                    float f6 = (width - sqrt) / width;
                    float[] fArr2 = this.maxVertesValues;
                    fArr2[i2] = f4 * f6;
                    fArr2[i2 + 1] = f6 * f5;
                } else {
                    float[] fArr3 = this.maxVertesValues;
                    fArr3[i2] = 0.0f;
                    fArr3[i2 + 1] = 0.0f;
                }
            }
        }
    }

//    public vieenhanceeeData(Bitmap bitmap, ImageEditing imageEditing, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick2) {
    public vieenhanceeeData(Bitmap bitmap, ImageEditingER imageEditing, ScaleImage scaleImage, ArcSeekBar arcSeekBar, SeekBar seekBar, MenuClick menuClick2) {
        this.currentOriginalBitmap = bitmap;
        this.mActivity = imageEditing;
        this.waistImageView = scaleImage;
        this.seekBarView = arcSeekBar;
        this.seek = seekBar;
        this.menuClick = menuClick2;
        onCreate();
        this.mActivity.li_undo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                vieenhanceeeData.this.saveFloatsValue1();
                int access$200 = vieenhanceeeData.this.mIdRequisite;
                if (access$200 == vieenhanceeeData.this.currentId && access$200 > 0) {
                    int i = access$200 - 1;
                    int unused = vieenhanceeeData.this.mIdRequisite = i;
                    vieenhanceeeData vieenhanceeedata = vieenhanceeeData.this;
                    undoRedoData.getBitmapFromDisk(access$200, i, "tool_" + (vieenhanceeeData.this.mIdRequisite + 1) + ".png", vieenhanceeedata, vieenhanceeedata.mActivity);
                    vieenhanceeeData.this.mActivity.li_undo.setImageDrawable(vieenhanceeeData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_undo));
                    vieenhanceeeData.this.mActivity.li_redo.setImageDrawable(vieenhanceeeData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
                }
            }
        });
        this.mActivity.li_redo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                int access$200 = vieenhanceeeData.this.mIdRequisite;
                if (access$200 == vieenhanceeeData.this.currentId && access$200 < vieenhanceeeData.this.lastId) {
                    if (!vieenhanceeeData.this.isEditmode) {
                        int access$2002 = vieenhanceeeData.this.mIdRequisite;
                        int i = access$2002 + 1;
                        int unused = vieenhanceeeData.this.mIdRequisite = i;
                        vieenhanceeeData vieenhanceeedata = vieenhanceeeData.this;
                        undoRedoData.getBitmapFromDisk(access$2002, i, "tool_" + vieenhanceeeData.this.mIdRequisite + ".png", vieenhanceeedata, vieenhanceeedata.mActivity);
                        vieenhanceeeData.this.mActivity.li_undo.setImageDrawable(vieenhanceeeData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_undo));
                        vieenhanceeeData.this.mActivity.li_redo.setImageDrawable(vieenhanceeeData.this.mActivity.getResources().getDrawable(R.drawable.ic_new_redo));
                        return;
                    }
                    vieenhanceeeData.this.saveFloatsValue1();
                }
            }
        });
    }

    private void onCreate() {
        this.mainView = (FrameLayout) this.mActivity.findViewById(R.id.main_frameView);
        this.maxBitmapSize = (int) (((float) Math.min(this.currentOriginalBitmap.getHeight(), this.currentOriginalBitmap.getWidth())) * this.waistImageView.getCalculatedMinScale());
        this.mActivity.isBlocked = false;
        drawCustomView();
        this.mCurrentBitmap = this.currentOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        this.myCanvasView = new Canvas(this.mCurrentBitmap);
        this.mActivity.li_preview.setOnTouchListener(
                this.mActivity.createToolCompareTouchListener(
                        this.waistImageView,
                        () -> vieenhanceeeData.this.currentOriginalBitmap,
                        () -> vieenhanceeeData.this.mCurrentBitmap
                )
        );
        this.seekBarView.setProgress(0);
        this.seekBarView.setOnStartTrackingTouch(this.onstartTouchListener);
        this.seekBarView.setOnStopTrackingTouch(this.onstoptTouchListener);
        this.seekBarView.setOnProgressChangedListener(this.onprogressChangeListener);
        this.waistImageView.setImageBitmap(this.mCurrentBitmap);
        this.waistImageView.setOnScaleAndMoveInterface(this);
        this.seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                vieenhanceeeData.this.seek_pos = i;
                seekBar.setProgress(i);
                vieenhanceeeData.this.seekBarView.getOnProgressChangedListener().invoke(i);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                vieenhanceeeData.this.seekBarView.getOnStartTrackingTouch().invoke(vieenhanceeeData.this.seek_pos);
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!vieenhanceeeData.this.isEditmode) {
                    vieenhanceeeData.this.seekBarView.setProgress(0);
                }
            }
        });
    }

    private void drawCustomView() {
        int min = (int) Math.min(((float) this.mActivity.getResources().getDrawable(R.drawable.enhance_arrow).getIntrinsicWidth()) * 2.5f, (float) this.maxBitmapSize);
        int i = this.maxBitmapSize;
        if (min != i) {
            min = (int) (((float) (min + i)) * 0.25f);
        }
        this.bitmapCurrentSize = min;
        this.customLayouts = new StickerView(this.mActivity.getApplicationContext());

        Bitmap bitmap = BitmapFactory.decodeResource(this.mActivity.getApplicationContext().getResources(), R.drawable.enhance_circle);

        Log.e("viewEnhance", "bitmap :   " + bitmap + "\nbitmap Width :   " + bitmap.getWidth() + "   bitmap Height :   " + bitmap.getHeight());


//        this.customLayouts.setImageBitmap(BitmapFactory.decodeResource(this.mActivity.getApplicationContext().getResources(), R.drawable.enhance_circle), (float) ((this.waistImageView.getMeasuredWidth() - this.bitmapCurrentSize) / 2), (float) ((this.waistImageView.getMeasuredHeight() - this.bitmapCurrentSize) / 2), 0.0f);
        this.customLayouts.setImageBitmap(bitmap, (float) ((this.waistImageView.getMeasuredWidth() - this.bitmapCurrentSize) / 2), (float) ((this.waistImageView.getMeasuredHeight() - this.bitmapCurrentSize) / 2), 0.0f);
        new RelativeLayout.LayoutParams(-1, -1);
        this.customLayouts.setOperationListener(new StickerView.OperationListener() {
            public void onDeleteClick() {
            }

            public void onTop(StickerView stickerView) {
            }

            public void onTouch() {
            }

            public void onEdit(StickerView stickerView, MotionEvent motionEvent) {
                Log.e("SHDGhSdfhgas", "SJDVSGHDV");
                if (motionEvent.getAction() == 0) {
                    vieenhanceeeData.this.startWorkWithControl();
                } else if (motionEvent.getAction() == 1 || motionEvent.getAction() == 3) {
                    vieenhanceeeData.this.seekBarView.setEnabled(true);
                }
            }
        });
        this.mainView.addView(this.customLayouts);
    }

    private void close(boolean z) {
        for (int i = 0; i <= this.lastId; i++) {
//            ImageEditing imageEditing = this.mActivity;
            ImageEditingER imageEditing = this.mActivity;
            imageEditing.deleteFile("tool_" + i + ".png");
        }
        this.currentId = -1;
        Bitmap bitmap = this.originalMeshArea;
        if (bitmap != null && !bitmap.isRecycled()) {
            this.originalMeshArea.recycle();
        }
        this.mCurrentBitmap.recycle();
        this.mainView.removeView(this.customLayouts);
        this.history.clear();
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


    public void saveFloatsValue1() {
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
                List<waistHistoryData> list2 = this.history;
                int i3 = this.column;
                list2.add(new waistHistoryData((float[]) this.vertexesmesh.clone(), (float) this.xStart, (float) this.yStart, i3, i3));
                this.seekBarView.setProgress(0);
                final String str = "tool_" + this.currentId + ".png";
                final Handler handler = new Handler();
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            FileOutputStream openFileOutput = vieenhanceeeData.this.mActivity.openFileOutput(str, 0);
                            copy.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput);
                            openFileOutput.close();
                            if (vieenhanceeeData.this.currentId == -1) {
                                vieenhanceeeData.this.mActivity.deleteFile(str);
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


    public void startWorkWithControl() {
        this.seekBarView.setEnabled(false);
        saveFloatsValue1();
    }

    public void move(float f, float f2, float f3, float f4) {
        saveFloatsValue1();
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
                this.waistImageView.invalidate();
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
}




























































































































































































































































































































































































































































































































































































