package com.aiface.aging.features.body;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;

import androidx.core.graphics.drawable.DrawableCompat;


import com.aiface.aging.R;
import com.aiface.aging.features.body.model.ImageData;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import kotlinx.coroutines.scheduling.WorkQueueKt;

public class Constant {
    public static String Body = "Body";
    public static Bitmap CameraBitmap = null;
    public static boolean IS_FROM_MYWORK_SCREEN = false;
    public static boolean IS_FROM_SAVE_SCREEN = false;
    public static final String IS_FROM_SETTINGS = "isFromSetting";
    public static Bitmap editBitmap;

    /** Release static bitmap holders — call on low memory / app trim. */
    public static void releaseStaticBitmaps() {
        if (CameraBitmap != null && !CameraBitmap.isRecycled()) {
            CameraBitmap.recycle();
        }
        CameraBitmap = null;
        if (editBitmap != null && !editBitmap.isRecycled()) {
            editBitmap.recycle();
        }
        editBitmap = null;
    }

    public static HashMap<String, ArrayList<ImageData>> folderData = new HashMap<>();
    public static String All_photos = "All Photos";
    public static String DistanceEye = "Distance";
    public static String Refine = "Refine";
    public static String Enhance = "Enhance";
    public static String Height = "Height";
    public static String Waist = "Waist";
    public static String Hips = "Hips";
    public static String PaintSkin = "PaintSkin";
    public static String[] body_array = {Refine, Enhance, Height, Waist, Hips, PaintSkin};
    public static int[] body_array_icons = {R.drawable.iv_body_refine, R.drawable.iv_big_enhance, R.drawable.iv_body_height_img, R.drawable.iv_body_waist, R.drawable.iv_body_hips, R.drawable.iv_body_paint_skin};
    public static boolean isSplashScreen = true;
    public static boolean isInterShow = false;
    public static final int[] listOfSkinColor = {Color.rgb(65, 49, 32), Color.rgb(90, 29, 29), Color.rgb(126, 79, 28), Color.rgb(147, 122, 96), Color.rgb(142, 0, 0), Color.rgb((int) WorkQueueKt.MASK, 39, 0), Color.rgb(105, 47, 81), Color.rgb(156, 73, 88), Color.rgb(215, 161, 105), Color.rgb(219, 125, 69), Color.rgb(246, 189, 123), Color.rgb(242, 207, 169), Color.rgb(255, 209, 198), Color.rgb(255, 241, 226), Color.rgb(244, 246, 225), Color.rgb(255, 232, 240), Color.rgb(246, 225, 237), Color.rgb(247, 183, 183), Color.rgb(184, 157, 160), Color.rgb(196, 137, 169), Color.rgb(120, 112, 130), Color.rgb(104, 125, 159), Color.rgb(212, 222, 241), Color.rgb(225, 246, 240), Color.rgb(128, 219, 160), Color.rgb(167, 179, 140), Color.rgb(117, 118, 70), Color.rgb(189, 172, 153), Color.rgb(255, 150, 0), Color.rgb(246, 227, 123)};
    public static String selected_data = DistanceEye;
    public static String privacy = "https://tflsignatureapps.terafort.com/privacy-policy.html";

    public static String feedback_email = "mayursmaiyani1@gmail.com";
    public static String PREF_TAG = "APP_PREF";
    public static String PREF_LANG_TAG = "SelectedLang";
    public static String PREF_IS_LANG_TAG = "isSelectedLang";
    public static String PREF_PRIVACY_TAG = "Accept_Privacy";
    public static String PREF_RATE_TAG = "Rating";
    public static String appmetrica = "f1dce783-624f-479f-ad03-fbfa5b29fff0";

    public static void setAppLocale(Context context, String str) {
        Locale locale = new Locale(str);
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        context.createConfigurationContext(configuration);
        context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
    }

    public static void updateLanguage(Activity activity) {
        try {
            String languageCode = getLanguageCode(activity);
            Configuration configuration = activity.getResources().getConfiguration();
            if (languageCode == null || languageCode.trim().isEmpty()) {
                try {
                    languageCode = configuration.locale.getLanguage();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (languageCode == null || languageCode.trim().isEmpty()) {
                return;
            }
            Locale locale = new Locale(languageCode.toLowerCase());
            Locale.setDefault(locale);
            Configuration configuration2 = activity.getResources().getConfiguration();
            configuration2.setLocale(locale);
            configuration.setLayoutDirection(locale);
            activity.getResources().updateConfiguration(configuration2, activity.getApplicationContext().getResources().getDisplayMetrics());
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public static void tintMenuIcon(Context context, MenuItem menuItem, int i) {
        Drawable wrap = DrawableCompat.wrap(menuItem.getIcon());
        DrawableCompat.setTint(wrap, context.getResources().getColor(i));
        menuItem.setIcon(wrap);
    }

    public static void setIsPrivacyPolicyAccepted(Context context, boolean z) {
        SharedPreferences.Editor edit = context.getSharedPreferences(PREF_TAG, 0).edit();
        edit.putBoolean(PREF_PRIVACY_TAG, z);
        edit.apply();
    }

    public static boolean isPrivacyPolicyAccepted(Context context) {
        return context.getSharedPreferences(PREF_TAG, 0).getBoolean(PREF_PRIVACY_TAG, false);
    }

    public static void saveLanguageCode(Context context, String str) {
        SharedPreferences.Editor edit = context.getSharedPreferences(PREF_TAG, 0).edit();
        edit.putString(PREF_LANG_TAG, str);
        edit.apply();
    }

    public static String getLanguageCode(Context context) {
        return context.getSharedPreferences(PREF_TAG, 0).getString(PREF_LANG_TAG, "en");
    }

    public static void setLanguageSelected(Context context, boolean z) {
        SharedPreferences.Editor edit = context.getSharedPreferences(PREF_TAG, 0).edit();
        edit.putBoolean(PREF_IS_LANG_TAG, z);
        edit.apply();
    }

    public static boolean isLanguageSelected(Context context) {
        return context.getSharedPreferences(PREF_TAG, 0).getBoolean(PREF_IS_LANG_TAG, false);
    }

//    public static boolean isAllPermissionsGranted(Activity activity) {
//        if (Build.VERSION.SDK_INT >= 23) {
//            return ActivityCompat.checkSelfPermission(activity, "android.permission.READ_EXTERNAL_STORAGE") == 0 && ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE") == 0;
//        }
//        return true;
//    }

    public static Uri saveBitmapToMediaStore(Context context, Bitmap bitmap, String fileName) {
        Uri imageUri = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM
                        + File.separator + context.getResources().getString(R.string.app_name)
                        + File.separator + "Photo");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try (OutputStream out = context.getContentResolver().openOutputStream(imageUri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    context.getContentResolver().update(imageUri, values, null, null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageUri;
    }

}
