package com.aiface.aging.features.body.controls;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;


public class undoRedoData {


    public interface PhotoLoadResponse {
        void loadResponse(Bitmap bitmap, int i, int i2);
    }


    private static String saveImage(Bitmap bitmap, String str, ContentResolver contentResolver) throws IOException {
        OutputStream outputStream = null;
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", str);
            contentValues.put("mime_type", "image/png");
            contentValues.put("relative_path", "DCIM/");
            Uri insert = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                outputStream = contentResolver.openOutputStream(insert);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return insert.toString();
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, null);
                outputStream.flush();
                outputStream.close();
                return insert.toString();
            }
        } catch (FileNotFoundException unused) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, null);
            outputStream.flush();
            outputStream.close();
            return outputStream.toString();
        }
    }

    private static Bitmap storeThumbnail(ContentResolver contentResolver, Bitmap bitmap, long j, int i) {
        Matrix matrix = new Matrix();
        matrix.setScale(50.0f / bitmap.getWidth(), 50.0f / bitmap.getHeight());
        Bitmap createBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        ContentValues contentValues = new ContentValues(4);
        contentValues.put("kind", Integer.valueOf(i));
        contentValues.put("image_id", Integer.valueOf((int) j));
        contentValues.put("height", Integer.valueOf(createBitmap.getHeight()));
        contentValues.put("width", Integer.valueOf(createBitmap.getWidth()));
        try {
            OutputStream openOutputStream = contentResolver.openOutputStream(contentResolver.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, contentValues));
            createBitmap.compress(Bitmap.CompressFormat.JPEG, 100, openOutputStream);
            openOutputStream.close();
            return createBitmap;
        } catch (IOException unused) {
            return null;
        }
    }

    public static void getBitmapFromDisk(final int i, final int i2, final String str, final PhotoLoadResponse photoLoadResponse, final Activity activity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(activity.getFilesDir(), str);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    Bitmap decodeStream = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
                    if (decodeStream == null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                photoLoadResponse.loadResponse(null, i, i2);
                            }
                        });
                        return;
                    }
                    if (!decodeStream.isMutable()) {
                        Bitmap copy = decodeStream.copy(Bitmap.Config.ARGB_8888, true);
                        decodeStream.recycle();
                        decodeStream = copy;
                    }
                    Bitmap finalDecodeStream = decodeStream;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            photoLoadResponse.loadResponse(finalDecodeStream, i, i2);
                        }
                    });
                } catch (FileNotFoundException e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            photoLoadResponse.loadResponse(null, i, i2);
                        }
                    });
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
