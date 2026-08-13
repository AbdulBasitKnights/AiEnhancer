package com.aiface.aging.features.body.activities;


import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.view.PointerIconCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.aiface.aging.BuildConfig;
import com.aiface.aging.AiFaceApp;
import com.aiface.aging.R;
import com.aiface.aging.ads_nextgen.NextGenInterstitialHelper;
import com.aiface.aging.shared.GlobalValuesKt;
import com.aiface.aging.shared.ads.AdError;
import com.aiface.aging.shared.ads.AdsHelper;
import com.aiface.aging.shared.ads.FullScreenContentCallback;
import com.aiface.aging.shared.ads.InterstitialBackPressGuardKt;
import com.aiface.aging.features.body.Constant;
import com.aiface.aging.features.body.controls.ScaleImage;
import com.aiface.aging.features.body.controls.undoRedoData;
import com.aiface.aging.features.body.fragment.BodyEditFragment;
import com.aiface.aging.features.body.inerfaces.MenuClick;
import com.aiface.aging.features.result.ResultLauncher;
import com.aiface.aging.features.result.ResultSource;
import com.aiface.aging.utils.DialogueUtils;
import com.aiface.aging.utils.FirebaseLogUtils;
import com.aiface.aging.utils.GlobalLoader;
import com.aiface.aging.utils.ImageUtils;
import com.aiface.aging.utils.LogUtils;
import com.aiface.aging.utils.SaveProgressHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import kotlin.Unit;

public class ImageEditingER extends AppCompatActivity implements undoRedoData.PhotoLoadResponse {

    private static final long SAVE_LOADER_MIN_MS = 1000L;

    public static int lastSelectedColor = 0;
    public static int selected_text_pos = -1;
    private int ScreenHeight;
    private int ScreenWidth;
    private BodyEditFragment bodyEditFragment;
    //    private ConstraintLayout cl_save_dialog;
//    private CardView cv_hd_save_image;
//    private CardView cv_save_image;
    public int currentId;
    private Bitmap currentOriginalBitmap;
    private Dialog dialog;
    FrameLayout frame_add_frag;
    ImageView img_dummy;
    public ScaleImage img_person1;
    public ImageView iv_main_back;
    public ImageView iv_save_image;
    public int lastId;
    LinearLayout li_items;
    public LinearLayout li_preview;
    public ImageView li_redo;
    LinearLayout li_textItems;
    public ImageView li_undo;
   // ProgressDialog loadingDialog;

    private KProgressHUD loadingDialog;

    public Bitmap mCurrentBitmap;
    public int mIdRequisite;
    FrameLayout main_frameView;
    Canvas myCanvasView;
    public RecyclerView rv_sub_items_face;
    Handler handler = new Handler();
    public boolean isBlocked = true;
    private final Bitmap[] tempBitmap = new Bitmap[1];

    private String thePath = "";
    private String savedShareFilePath = "";
    private Uri savedShareContentUri = null;

    InterstitialAd interstitialAd = null;
    private boolean navigatingToResult = false;

    public View.OnTouchListener previewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                ImageEditingER.this.showOriginalImage();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                ImageEditingER.this.showEditedImage();
            }
            return true;
        }
    };

    public View.OnClickListener undoClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.e(NotificationCompat.CATEGORY_MESSAGE, "undoClick");
            int i = ImageEditingER.this.mIdRequisite;
            if (i <= 1) {
                if (i == 1) {
                    Log.e(NotificationCompat.CATEGORY_MESSAGE, "undoClick else");
                    ImageEditingER.this.mIdRequisite = 0;
                    ImageEditingER.this.currentId = 0;
                    Bitmap copy = ImageEditingER.this.currentOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    ImageEditingER.this.mCurrentBitmap = copy;
                    ImageEditingER.this.img_person1.setImageBitmap(copy);
                    ImageEditingER.this.img_person1.resetToFitCenter();
                    ImageEditingER.this.updateUndoRedoIcons();
                    return;
                }
                return;
            }
            Log.e(NotificationCompat.CATEGORY_MESSAGE, "undoClick i3");
            int i2 = i - 1;
            ImageEditingER.this.mIdRequisite = i2;
            ImageEditingER ImageEditingER = ImageEditingER.this;
            undoRedoData.getBitmapFromDisk(i, i2, "main_" + ImageEditingER.this.mIdRequisite + ".png", ImageEditingER, ImageEditingER);
            ImageEditingER.this.updateUndoRedoIcons();
        }
    };

    public View.OnClickListener redoClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ImageEditingER.this.lambda$new$0$ImageEditingER(view);
        }
    };

    public void VisibleHideBottomView(boolean z) {
    }

    public void lambda$new$0$ImageEditingER(View view) {
        int i = this.mIdRequisite;
        Log.w(NotificationCompat.CATEGORY_MESSAGE, "redoClick I value = " + i + "lastId= " + this.lastId);
        if (i < this.lastId) {
            int i2 = i + 1;
            this.mIdRequisite = i2;
            undoRedoData.getBitmapFromDisk(i, i2, "main_" + this.mIdRequisite + ".png", this, this);
            updateUndoRedoIcons();
            return;
        }
        this.li_redo.setImageDrawable(getResources().getDrawable(R.drawable.ic_disable_redo));
    }

    public void showOriginalImage() {
        if (currentOriginalBitmap != null && !currentOriginalBitmap.isRecycled()) {
            img_person1.setImageBitmap(currentOriginalBitmap);
        }
    }

    public void showEditedImage() {
        if (mCurrentBitmap != null && !mCurrentBitmap.isRecycled()) {
            img_person1.setImageBitmap(mCurrentBitmap);
        }
    }

    public void updateUndoRedoIcons() {
        if (mIdRequisite > 0) {
            li_undo.setImageDrawable(getResources().getDrawable(R.drawable.ic_new_undo));
        } else {
            li_undo.setImageDrawable(getResources().getDrawable(R.drawable.ic_disable_undo));
        }
        if (mIdRequisite < lastId) {
            li_redo.setImageDrawable(getResources().getDrawable(R.drawable.ic_new_redo));
        } else {
            li_redo.setImageDrawable(getResources().getDrawable(R.drawable.ic_disable_redo));
        }
    }

    private void initializeUndoRedoState() {
        currentId = 0;
        mIdRequisite = 0;
        lastId = 0;
        updateUndoRedoIcons();
    }

    public interface BitmapSupplier {
        Bitmap get();
    }

    public View.OnTouchListener createToolCompareTouchListener(
            final ScaleImage imageView,
            final BitmapSupplier beforeSupplier,
            final BitmapSupplier afterSupplier) {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    Bitmap before = beforeSupplier.get();
                    if (before != null && !before.isRecycled()) {
                        imageView.setImageBitmap(before);
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    Bitmap after = afterSupplier.get();
                    if (after != null && !after.isRecycled()) {
                        imageView.setImageBitmap(after);
                    }
                }
                return true;
            }
        };
    }

    public Bitmap getBitmap(String str) {
        Bitmap bitmap = null;
        try {
            bitmap = decodeSampledBitmap(new File(str), 512, PointerIconCompat.TYPE_GRAB);
            int attributeInt = new ExifInterface(str).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Log.d("EXIF", "Exif: " + attributeInt);
            Matrix matrix = new Matrix();
            if (attributeInt == 6) {
                matrix.postRotate(90.0f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            } else if (attributeInt == 3) {
                matrix.postRotate(180.0f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            } else if (attributeInt == 8) {
                matrix.postRotate(270.0f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            Log.d("ImageEditingER", "getBitmap(tr)   Exception: " + e);
        }
        return bitmap;
    }

    public static Bitmap decodeSampledBitmap(File file, int i, int i2) throws FileNotFoundException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new FileInputStream(file), null, options);
        options.inSampleSize = calculateInSampleSize(options, i, i2);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(new FileInputStream(file), null, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int i, int i2) {
        int i3 = options.outHeight;
        int i4 = options.outWidth;
        int i5 = 1;
        if (i3 > i2 || i4 > i) {
            int i6 = i3 / 2;
            int i7 = i4 / 2;
            while (i6 / i5 >= i2 && i7 / i5 >= i) {
                i5 *= 2;
            }
        }
        return i5;
    }

    /** Downscale in-memory source before editor copies — avoids duplicate full-res bitmaps. */
    private static Bitmap downscaleWorkingBitmap(Bitmap source, int maxSide) {
        if (source == null || source.isRecycled()) return source;
        int width = source.getWidth();
        int height = source.getHeight();
        if (Math.max(width, height) <= maxSide) {
            return source;
        }
        float scale = maxSide / (float) Math.max(width, height);
        int newW = Math.max(1, (int) (width * scale));
        int newH = Math.max(1, (int) (height * scale));
        return Bitmap.createScaledBitmap(source, newW, newH, true);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_image_editing12);

//        AdsGoogle adsGoogle = new AdsGoogle(this);
//        adsGoogle.Banner_Show((RelativeLayout) findViewById(R.id.banner), this);
//        adsGoogle.Interstitial_Show_Counter(this);


        lastSelectedColor = -1;
        selected_text_pos = -1;


        loadingDialog = DialogueUtils.INSTANCE.getWaitDialogue("Saving...", this);

        this.dialog = new Dialog(this);

        initView();
        hideNavigationBar();
      //  setUpActionBar();

        if (AiFaceApp.Companion.isInterBodySaveHf() && AiFaceApp.Companion.isInterBodySave()) {
            loadInterBodyHf(this);
        } else if (AiFaceApp.Companion.isInterBodySave()) {
            loadInterBody(this);
        }

    }

    private void openResultScreen() {
        if (navigatingToResult || isFinishing()) {
            return;
        }
        if (savedShareFilePath == null || savedShareFilePath.isEmpty()) {
            Toast.makeText(this, "Image save failed", Toast.LENGTH_SHORT).show();
            return;
        }
        navigatingToResult = true;
        ResultLauncher.INSTANCE.openLocalPreview(
                this,
                ResultSource.BODY_EDITOR,
                savedShareFilePath,
                savedShareContentUri,
                false
        );
        // finishHost=false: inter helper finishes after ad (activity hop parallel)
    }

    public void showInterBody() {
        try {
            if (savedShareFilePath == null || savedShareFilePath.isEmpty()) {
                Toast.makeText(this, "Image save failed", Toast.LENGTH_SHORT).show();
                return;
            }
            // Dismiss-then-navigate — never finish before interstitial shows.
            com.aiface.aging.shared.ads.HomeInterstitialHelperKt.showHomeInterstitialThen(
                    this,
                    false,
                    () -> {
                        openResultScreen();
                        return Unit.INSTANCE;
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
            openResultScreen();
        }
    }

    public void loadInterBodyHf(Context context) {
        if (Boolean.TRUE.equals(AdsHelper.INSTANCE.isProVersion().getValue()) || interstitialAd != null) return;

        NextGenInterstitialHelper.INSTANCE.load(
                BuildConfig.inter_home_high,
                ad -> {
                    interstitialAd = InterstitialBackPressGuardKt.rememberAdUnitId(ad, BuildConfig.inter_home_high);
                    LogUtils.INSTANCE.printLog("collage_inter hf loaded", BuildConfig.inter_home_high);
                    return Unit.INSTANCE;
                },
                error -> {
                    interstitialAd = null;
                    loadInterBody(context);
                    LogUtils.INSTANCE.printLog("collage_inter hf failed", BuildConfig.inter_home_high);
                    return Unit.INSTANCE;
                }
        );
    }

    public void loadInterBody(Context context) {
        if (Boolean.TRUE.equals(AdsHelper.INSTANCE.isProVersion().getValue()) || interstitialAd != null) return;

        NextGenInterstitialHelper.INSTANCE.load(
                BuildConfig.inter_home,
                ad -> {
                    interstitialAd = InterstitialBackPressGuardKt.rememberAdUnitId(ad, BuildConfig.inter_home);
                    LogUtils.INSTANCE.printLog("collage_inter loaded", BuildConfig.inter_home);
                    return Unit.INSTANCE;
                },
                error -> {
                    interstitialAd = null;
                    LogUtils.INSTANCE.printLog("collage_inter failed", BuildConfig.inter_home);
                    return Unit.INSTANCE;
                }
        );
    }

    public void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
//            getWindow().setDecorFitsSystemWindows(true);

            WindowInsetsController controller = getWindow().getInsetsController();
            controller.hide(WindowInsets.Type.statusBars());
            controller.hide(WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
      //  if (hasFocus) hideSystemUI();
    }

    void setUpActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow_back);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        View inflate = getLayoutInflater().inflate(R.layout.custom_action_bar, null);
//        ((TextView) inflate.findViewById(R.id.title_tv)).setGravity(16);
        ((TextView) inflate.findViewById(R.id.title_tv)).setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(inflate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_edit_menu, menu);
        Log.w("TAG", "onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Log.w("TAG", "onOptionsItemSelected");
        if (Constant.editBitmap != this.mCurrentBitmap){
            menuItem.setEnabled(true);
            Log.w("TAG", "onOptionsItemSelected true ");
        }
        int itemId = menuItem.getItemId();
//        if (itemId == 16908332) {
            if (itemId == android.R.id.home) {
            onBackPressed();
        } else if (itemId == R.id.nav_save) {
//            showSaveImageDialog();
            saveImage(true);
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void showSaveImageDialog() {

        ImageEditingER.this.saveImage(true);
     //   startActivity(new Intent(this, ShareImageActivity.class));



/*        this.dialog.setContentView(R.layout.dialog_save_image);
//        this.dialog.getWindow().setBackgroundDrawableResource(R.color.shadow_2);
        this.dialog.getWindow().setBackgroundDrawableResource(R.color.color_grey_light);
//        this.dialog.getWindow().setLayout(-1, -1);
        this.dialog.getWindow().setLayout(MATCH_PARENT, MATCH_PARENT);
        this.dialog.setCancelable(true);
        CardView cv_save_image = this.dialog.findViewById(R.id.cv_save_photo);
        CardView cv_hd_save_image = this.dialog.findViewById(R.id.cardView);
        ConstraintLayout cl_save_dialog = this.dialog.findViewById(R.id.cl_save_dialog);
        cv_save_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageEditingER.this.lambda$showSaveImageDialog$1$ImageEditingER(view);
            }
        });
        cv_hd_save_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageEditingER.this.dialog.dismiss();
                ImageEditingER.this.saveImage(true);

            }
        });
        cl_save_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageEditingER.this.dialog.dismiss();
            }
        });
        this.dialog.show();*/
    }

    public void lambda$showSaveImageDialog$1$ImageEditingER(View view) {
        this.dialog.dismiss();
        ImageEditingER.this.saveImage(false);

    }

 /*   public void saveImages(boolean z) {
        ProgressDialog progressDialog = this.loadingDialog;
        if (progressDialog != null && !progressDialog.isShowing()) {
            this.loadingDialog.show();
        }
        this.main_frameView.setDrawingCacheEnabled(true);
        this.main_frameView.buildDrawingCache();
        Constant.editBitmap = this.mCurrentBitmap;
        this.main_frameView.destroyDrawingCache();
        String format = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File bitmapToFile = bitmapToFile(this, Constant.editBitmap, getResources().getString(R.string.app_name) + "_" + format + ".png", false, z);
        MediaScannerConnection.scanFile(this, new String[]{bitmapToFile.getPath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String str, Uri uri) {
                Log.i("TAG", "Finished scanning " + str);
                thePath = uri.toString();
            }
        });
//        startActivityForResult(new Intent(this, ShareImage.class)
//                .putExtra("path", bitmapToFile.getPath())
//                .putExtra("isFromSave", true)
//                .putExtra("isHdQuality", z), 102);

        ProgressDialog progressDialog2 = this.loadingDialog;
        if (progressDialog2 == null || !progressDialog2.isShowing()) {
            return;
        }
        this.loadingDialog.dismiss();
    }*/


    public void saveImage(boolean isHd) {
        FirebaseLogUtils.INSTANCE.logEvent("home_click_body_editor_save", "");

        Bitmap bitmapToSave = resolveBitmapForSave();
        if (bitmapToSave == null || bitmapToSave.isRecycled()) {
            Toast.makeText(this, "Failed to prepare image", Toast.LENGTH_SHORT).show();
            return;
        }

        Constant.editBitmap = bitmapToSave;
        mCurrentBitmap = bitmapToSave;

        final Bitmap saveCopy = bitmapToSave.copy(Bitmap.Config.ARGB_8888, true);
        SaveProgressHelper.showProcessing(this);

        new Thread(() -> {
            String cachePath = null;
            try {
                cachePath = ImageUtils.INSTANCE.saveBitmapToCache(ImageEditingER.this, saveCopy);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (saveCopy != null && !saveCopy.isRecycled()) {
                    saveCopy.recycle();
                }
            }

            final String pathResult = cachePath;
            runOnUiThread(() -> {
                SaveProgressHelper.hide(ImageEditingER.this);
                if (pathResult == null || pathResult.isEmpty()) {
                    Toast.makeText(this, "Failed to prepare image", Toast.LENGTH_SHORT).show();
                    return;
                }
                savedShareFilePath = pathResult;
                savedShareContentUri = null;
                showInterBody();
            });
        }).start();
    }

    private void showSaveLoader() {
        SaveProgressHelper.show(this);
    }

    private void hideSaveLoaderWithMinDuration(long saveStartMs, Runnable onComplete) {
        long elapsed = System.currentTimeMillis() - saveStartMs;
        long remainingDelay = Math.max(0L, SAVE_LOADER_MIN_MS - elapsed);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SaveProgressHelper.hide(ImageEditingER.this);
            if (onComplete != null) {
                onComplete.run();
            }
        }, remainingDelay);
    }

    public void setBodyEditFragment1() {
        this.bodyEditFragment = null;
        removeFragment();
        BodyEditFragment bodyEditFragment = new BodyEditFragment(this, this, this.rv_sub_items_face, new MenuClick() {
            @Override
            public void onMenuClick(boolean z) {
                ImageEditingER.this.li_undo.setImageDrawable(ImageEditingER.this.getResources().getDrawable(R.drawable.ic_new_undo));
            }
        });
        this.bodyEditFragment = bodyEditFragment;
        bodyEditFragment.setListener(new BodyEditFragment.OptionChoose() {
            @Override
            public void onItemClick(String str) {
            }

            @Override
            public void showOrigin(boolean z) {
                if (z) {
                    ImageEditingER.this.showOriginalImage();
                } else {
                    ImageEditingER.this.showEditedImage();
                }
            }

            @Override
            public void text_done(boolean z, String str) {
                if (z) {
                    ImageEditingER.this.img_person1.setDrawingCacheEnabled(true);
                    ImageEditingER.this.img_person1.buildDrawingCache();
                    ImageEditingER.this.img_person1.getDrawingCache();
                    Bitmap bitmap = ImageEditingER.this.mCurrentBitmap;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("ImageEditingER", "setBodyEditFragment1() text_done(z, str)   ProgressShow::SET_FACE_EE");
                        }
                    }, 500L);
                }
            }

            @Override
            public void text_done(boolean z) {
                if (z) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ImageEditingER.this.removeFragment();
                        }
                    }, 500L);
                }
            }

            @Override
            public void VisibleHideSeekView(boolean z) {
                Log.d("ImageEditingER", "setBodyEditFragment1() VisibleHideSeekView(z) hide");
                ImageEditingER.this.VisibleHideBottomView(z);
            }
        });
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_add_frag, this.bodyEditFragment).commit();
    }

    private void commitPendingEdits() {
        if (bodyEditFragment != null) {
            bodyEditFragment.commitPendingEditsIfNeeded();
        }
    }

    private Bitmap resolveBitmapForSave() {
        commitPendingEdits();

        if (mCurrentBitmap != null && !mCurrentBitmap.isRecycled()) {
            return mCurrentBitmap;
        }

        Drawable drawable = img_person1.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            Bitmap source = ((BitmapDrawable) drawable).getBitmap();
            if (source != null && !source.isRecycled()) {
                mCurrentBitmap = source.copy(Bitmap.Config.ARGB_8888, true);
                img_person1.setImageBitmap(mCurrentBitmap);
                return mCurrentBitmap;
            }
        }

        return null;
    }

    public void saveEffect(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        this.mCurrentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        this.myCanvasView = new Canvas(this.mCurrentBitmap);
        this.img_person1.setImageBitmap(this.mCurrentBitmap);
        addMainState();
        updateUndoRedoIcons();
    }

    public void addMainState() {
        int i = this.currentId + 1;
        this.currentId = i;
        if (i <= this.lastId) {
            while (i <= this.lastId) {
                deleteFile("main_" + i + ".png");
                i++;
            }
        }
        int i2 = this.currentId;
        this.lastId = i2;
        this.mIdRequisite = i2;
        final Bitmap copy = this.mCurrentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        if (this.bodyEditFragment.mCurrentInterface != null) {
            this.bodyEditFragment.mCurrentInterface.onBackPressed(false);
        }
        final String str = "main_" + this.currentId + ".png";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream openFileOutput = ImageEditingER.this.openFileOutput(str, 0);
                    copy.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput);
                    openFileOutput.close();
                    copy.recycle();
                } catch (Exception e) {
                    Log.d("My", "Error (save Bitmap): " + e.getMessage());
                }
            }
        }).start();
    }

    private void initView() {
        this.img_dummy = findViewById(R.id.img_dummy);
        this.main_frameView = findViewById(R.id.main_frameView);
        this.frame_add_frag = findViewById(R.id.frame_add_frag);
        this.li_preview = findViewById(R.id.li_preview);
        this.rv_sub_items_face = findViewById(R.id.rv_sub_items_face);
        this.li_undo = findViewById(R.id.li_undo);
        this.li_redo = findViewById(R.id.li_redo);
        this.iv_main_back = findViewById(R.id.iv_main_back);
        this.iv_save_image = findViewById(R.id.iv_save_image);
        this.img_person1 = findViewById(R.id.img_person1);
        this.li_textItems = findViewById(R.id.li_textitems);
        LinearLayout linearLayout = findViewById(R.id.li_items);
        this.li_items = linearLayout;
        linearLayout.setVisibility(View.VISIBLE);
        DisplayMetrics displayMetrics = this.img_person1.getContext().getResources().getDisplayMetrics();
        this.ScreenWidth = displayMetrics.widthPixels;
        this.ScreenHeight = displayMetrics.heightPixels;
        Log.d("ImageEditingER", "initView()   ScreenWidth:  " + this.ScreenWidth + "  ::  ScreenHeight:  " + this.ScreenHeight);
        setMainAdapter();
        setBodyEditFragment1();
        this.li_preview.setOnTouchListener(this.previewTouchListener);
        this.li_undo.setOnClickListener(this.undoClick);
        this.li_redo.setOnClickListener(this.redoClick);
        this.iv_main_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageEditingER.this.lambda$initView$2$ImageEditingER(view);
            }
        });
        this.iv_save_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageEditingER.this.lambda$initView$3$ImageEditingER(view);
            }
        });
    }

    public void lambda$initView$2$ImageEditingER(View view) {
        onBackPressed();
    }

    public void lambda$initView$3$ImageEditingER(View view) {
        showSaveImageDialog();
    }

    private void setMainAdapter() {
        DisplayMetrics displayMetrics = this.img_person1.getContext().getResources().getDisplayMetrics();
        this.ScreenWidth = displayMetrics.widthPixels;
        this.ScreenHeight = displayMetrics.heightPixels;
        Log.d("ImageEditingER", "setMainAdapter()   ScreenWidth:  " + this.ScreenWidth + "  ::  ScreenHeight:  " + this.ScreenHeight);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ImageEditingER.this.loadFace1();
            }
        }).start();
    }

    public File bitmapToFile(Context context, Bitmap bitmap, String str, boolean z, boolean z2) {
        File file = null;
        File file2 = null;
        try {
            if (z) {
                file = new File(getFilesDir().getAbsolutePath() + File.separator + context.getResources().getString(R.string.app_name) + File.separator);
            } else {
                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + context.getResources().getString(R.string.app_name));
            }
            if (!file.exists()) {
                Log.e("FILE_Exs", file.getPath() + "::");
                file.mkdirs();
            }
            if (Build.VERSION.SDK_INT > 29) {
                return saveFileOnAboveQ(bitmap, str, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + context.getResources().getString(R.string.app_name)), this);
//                return saveFileOnAboveQ(bitmap, str, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + context.getResources().getString(R.string.app_name)), this);
            }
            Log.e("FILE_Exs", file.getPath() + "::");
            File file3 = new File(file.getPath() + File.separator + str);
            try {
                file3.createNewFile();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                if (z2) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                } else {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
                }
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                FileOutputStream fileOutputStream = new FileOutputStream(file3);
                fileOutputStream.write(byteArray);
                fileOutputStream.flush();
                fileOutputStream.close();
                return file3;
            } catch (Exception e) {
                file2 = file3;
//                e = e;
//                e.printStackTrace();
                Log.e("M_TAG", "ImageEditingER.class,bitmapToFile():" + e);
                return file2;
            }
        } catch (Exception e2) {
//            e2.printStackTrace();
            Log.e("M_TAG", "ImageEditingER.class,bitmapToFile():" + e2);
        }
        return file;
    }

    public final File saveFileOnAboveQ(Bitmap bitmap, String str, File file, Context context) {
        String str2;
        FileOutputStream fileOutputStream;
        ContentResolver contentResolver = context.getContentResolver();
        String name = file.getName();
        FileOutputStream fileOutputStream2 = null;
        if (file.getPath().contains(Environment.DIRECTORY_PICTURES)) {
            str2 = Environment.DIRECTORY_PICTURES + File.separator + name;
        } else if (file.getPath().contains(Environment.DIRECTORY_DCIM)) {
            str2 = Environment.DIRECTORY_DCIM + File.separator + name;
        } else {
            str2 = null;
        }
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", str);
            contentValues.put("mime_type", "image/png");
            contentValues.put("relative_path", str2);
            Uri insert = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fileOutputStream = (FileOutputStream) contentResolver.openOutputStream(insert);
            try {
                try {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 0, fileOutputStream)) {
                        fileOutputStream.flush();
                    }
                    File file2 = new File(getPath(insert, context));
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return file2;
                } catch (Throwable th) {
                    th = th;
                    fileOutputStream2 = fileOutputStream;
                    if (fileOutputStream2 != null) {
                        try {
                            fileOutputStream2.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e3) {
                e3.printStackTrace();
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    }
                }
                return null;
            }
        } catch (Exception e5) {
            fileOutputStream = null;
        } catch (Throwable th2) {
        }
        return file;
    }

    public String getPath(Uri uri, Context context) {
        Cursor query = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
        if (query == null) {
            return null;
        }
        int columnIndexOrThrow = query.getColumnIndexOrThrow("_data");
        query.moveToFirst();
        String string = query.getString(columnIndexOrThrow);
        query.close();
        return string;
    }

    public void removeFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            Log.d("ImageEditingER", "ON_REMOVE_FRAGMENT");
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
        Fragment findFragmentById = getSupportFragmentManager().findFragmentById(R.id.frame_add_frag);
        if (findFragmentById != null) {
            getSupportFragmentManager().beginTransaction().remove(findFragmentById).commit();
        }
        ((FrameLayout) findViewById(R.id.frame_add_frag)).removeAllViews();
    }

    @Override
    public void onBackPressed() {
        Log.d("ImageEditingER", "BackPressed");
        BodyEditFragment bodyEditFragment = this.bodyEditFragment;
        if (bodyEditFragment != null) {
            if (bodyEditFragment.isBackVisible()) {
                this.bodyEditFragment.ClickBack();
            } else {
                alertSaveDialog();
            }
        }
    }

    public void alertSaveDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom1)
                .setTitle(R.string.save_dialog_title)
                .setMessage(R.string.save_dialog_message)
                .setPositiveButton(R.string.save_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finish();
                    }
                })
                .setNegativeButton(R.string.save_dialog_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.black));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.black));
    }

    /*
    public void loadFace1() {
        if (!getIntent().getBooleanExtra("camera", false)) {
            final String path = new File(getIntent().getStringExtra("selected_path")).getPath();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    try {
                        int attributeInt = new ExifInterface(new File(path).getAbsolutePath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                        int i = 0;
                        if (attributeInt == 3) {
                            i = 180;
                        } else if (attributeInt == 6) {
                            i = 90;
                        } else if (attributeInt == 8) {
                            i = 270;
                        }
                        Bitmap decodeFile = BitmapFactory.decodeFile(path, options);
                        if (decodeFile == null) {
                            Log.e("ImageEditingER","loadFace1  decodeFile null");
                            ImageEditingER.this.finish();
                            return;
                        }
                        int width = decodeFile.getWidth();
                        int height = decodeFile.getHeight();
                        if (Math.max(width, height) <= 1500.0f) {
                            ImageEditingER.this.currentOriginalBitmap = decodeFile;
                        } else {
                            float max = 1500.0f / Math.max(width, height);
                            width = (int) (width * max);
                            height = (int) (height * max);
                            ImageEditingER.this.currentOriginalBitmap = Bitmap.createScaledBitmap(decodeFile, width, height, true);
                            decodeFile.recycle();
                        }
                        int i2 = width;
                        int i3 = height;
                        if (i != 0) {
                            Matrix matrix = new Matrix();
                            matrix.postRotate(i);
                            Bitmap createBitmap = Bitmap.createBitmap(ImageEditingER.this.currentOriginalBitmap, 0, 0, i2, i3, matrix, true);
                            ImageEditingER.this.currentOriginalBitmap.recycle();
                            ImageEditingER.this.currentOriginalBitmap = createBitmap;
                        }
                        if (ImageEditingER.this.currentOriginalBitmap != null) {
                            if (!ImageEditingER.this.currentOriginalBitmap.isMutable()) {
                                Bitmap copy = ImageEditingER.this.currentOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                                ImageEditingER.this.currentOriginalBitmap.recycle();
                                ImageEditingER.this.currentOriginalBitmap = copy;
                            }
                            File file = new File(path);
                            if (file.getParentFile().equals(ImageEditingER.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES))) {
                                file.delete();
                            }
                            ImageEditingER ImageEditingER = ImageEditingER.this;
                            ImageEditingER.mCurrentBitmap = ImageEditingER.currentOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        FileOutputStream openFileOutput = ImageEditingER.this.openFileOutput("original.png", 0);
                                        ImageEditingER.this.currentOriginalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, openFileOutput);
                                        openFileOutput.close();
                                    } catch (Exception e) {
                                        Log.d("My", "Error (save Original): " + e.getMessage());
                                    }
                                }
                            }).start();
                            ImageEditingER.this.handler.post(new Runnable() {
                                @Override
                                public final void run() {
                                    runActivity();
                                }

                                public void runActivity() {
                                    ImageEditingER.this.img_person1.setImageBitmap(ImageEditingER.this.mCurrentBitmap);
                                }
                            });
                            return;
                        }
                        Log.e("ImageEditingER", "loadFace1  try");
                        ImageEditingER.this.finish();
                    } catch (IOException | OutOfMemoryError unused) {
                        Log.e("ImageEditingER", "loadFace1  catch");
                        ImageEditingER.this.finish();
                    }
                }
            }).start();
            return;
        }
        Bitmap bitmap = Constant.CameraBitmap;
    }
    */

    public void loadFace1() {

        if (getIntent().getBooleanExtra("camera", false)) {
            Bitmap bitmap = Constant.CameraBitmap;
            if (bitmap != null && !bitmap.isRecycled()) {
                Bitmap working = downscaleWorkingBitmap(bitmap, 1500);
                currentOriginalBitmap = working.copy(Bitmap.Config.ARGB_8888, true);
                mCurrentBitmap = currentOriginalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                handler.post(() -> {
                    img_person1.setImageBitmap(mCurrentBitmap);
                    initializeUndoRedoState();
                });
            }
            return;
        }

        try {
            final Uri imageUri = Uri.parse(getIntent().getStringExtra("selected_path"));

            new Thread(() -> {
                try {
                    ContentResolver resolver = getContentResolver();

                    // -----------------------------
                    // 1. Decode bitmap from URI (subsample — never full-res into heap)
                    // -----------------------------
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    InputStream boundsStream = resolver.openInputStream(imageUri);
                    BitmapFactory.decodeStream(boundsStream, null, options);
                    if (boundsStream != null) boundsStream.close();

                    options.inJustDecodeBounds = false;
                    options.inMutable = true;
                    options.inSampleSize = calculateInSampleSize(options, 1500, 1500);
                    options.inPreferredConfig = Bitmap.Config.RGB_565;

                    InputStream bitmapStream = resolver.openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(bitmapStream, null, options);
                    if (bitmapStream != null) bitmapStream.close();

                    if (bitmap == null) {
                        Log.e("ImageEditingER", "Bitmap decode failed");
                        finish();
                        return;
                    }

                    // -----------------------------
                    // 2. Read EXIF orientation
                    // -----------------------------
                    int rotation = 0;
                    InputStream exifStream = resolver.openInputStream(imageUri);
                    ExifInterface exif = new ExifInterface(exifStream);
                    if (exifStream != null) exifStream.close();

                    int orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                    );

                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;

                    // -----------------------------
                    // 3. Scale bitmap (max 1500px)
                    // -----------------------------
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();

                    if (Math.max(width, height) > 1500) {
                        float scale = 1500f / Math.max(width, height);
                        int newW = (int) (width * scale);
                        int newH = (int) (height * scale);

                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
                        bitmap.recycle();
                        bitmap = scaled;
                    }

                    // -----------------------------
                    // 4. Rotate bitmap if needed
                    // -----------------------------
                    if (rotation != 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);

                        Bitmap rotated = Bitmap.createBitmap(
                                bitmap,
                                0,
                                0,
                                bitmap.getWidth(),
                                bitmap.getHeight(),
                                matrix,
                                true
                        );

                        bitmap.recycle();
                        bitmap = rotated;
                    }

                    // -----------------------------
                    // 5. Ensure mutable
                    // -----------------------------
                    if (!bitmap.isMutable()) {
                        Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        bitmap.recycle();
                        bitmap = mutable;
                    }

                    currentOriginalBitmap = bitmap;
                    mCurrentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                    // -----------------------------
                    // 6. Save original in app storage
                    // -----------------------------
                    new Thread(() -> {
                        try {
                            FileOutputStream fos = openFileOutput("original.png", MODE_PRIVATE);
                            currentOriginalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();
                        } catch (Exception e) {
                            Log.e("ImageEditingER", "Save error: " + e.getMessage());
                        }
                    }).start();

                    // -----------------------------
                    // 7. Update UI
                    // -----------------------------
                    handler.post(() -> {
                        img_person1.setImageBitmap(mCurrentBitmap);
                        initializeUndoRedoState();
                    });

                } catch (OutOfMemoryError e) {
                    Log.e("ImageEditingER", "OOM: " + e.getMessage());
                    finish();
                } catch (Exception e) {
                    Log.e("ImageEditingER", "Error: " + e.getMessage());
                    finish();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadResponse(Bitmap bitmap, int i, int i2) {
        if (bitmap == null) {
            this.mIdRequisite = i;
            updateUndoRedoIcons();
            return;
        }

        if (this.mCurrentBitmap != null && !this.mCurrentBitmap.isRecycled()
                && this.mCurrentBitmap != bitmap) {
            this.mCurrentBitmap.recycle();
        }

        if (!bitmap.isMutable()) {
            this.mCurrentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmap.recycle();
        } else {
            this.mCurrentBitmap = bitmap;
        }

        this.img_person1.setImageBitmap(this.mCurrentBitmap);
        this.myCanvasView = new Canvas(this.mCurrentBitmap);
        this.currentId = i2;
        this.mIdRequisite = i2;
        this.img_person1.resetToFitCenter();
        updateUndoRedoIcons();
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 102) {
            if (i2 == -1) {
                setResult(-1);
                Log.e("ImageEditingER", "onActivityResult: Result_ok");
                finish();
            } else {
                Log.e("ImageEditingER", "onActivityResult: Result not ok, request match");
                finish();
            }
        } else {
            Log.e("ImageEditingER", "onActivityResult: request not match");
        }
        super.onActivityResult(i, i2, intent);
    }

    private void hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

}