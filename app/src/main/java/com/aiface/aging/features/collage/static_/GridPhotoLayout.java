package com.aiface.aging.features.collage.static_;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.RelativeLayout;


import com.aiface.aging.features.collage.ImageUtils;
import com.aiface.aging.features.collage.model.PhotoItem;
import com.aiface.aging.utils.ImageDecoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vanhu_000 on 3/11/2016.
 */
public class GridPhotoLayout extends RelativeLayout implements GridPhotoImageView.OnImageClickListener {
    private static final String TAG = GridPhotoLayout.class.getSimpleName();
    private OnQuickActionClickListener mQuickActionClickListener;
    public interface OnQuickActionClickListener {
        void onImageTouchListner(GridPhotoImageView v);
        void onDoubleTouchListner(GridPhotoImageView v);
    }

    OnDragListener mOnDragListener = new OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            int dragEvent = event.getAction();

            switch (dragEvent) {
                case DragEvent.ACTION_DRAG_ENTERED:

                    break;

                case DragEvent.ACTION_DRAG_EXITED:

                    break;

                case DragEvent.ACTION_DROP:

                    GridPhotoImageView target = (GridPhotoImageView) v;
                    GridPhotoImageView selectedView = getSelectedFrameImageView(target, event);
                    if (selectedView != null) {
                        target = selectedView;
                        GridPhotoImageView dragged = (GridPhotoImageView) event.getLocalState();
                        if (target.getPhotoItem() != null && dragged.getPhotoItem() != null) {
                            String targetPath = target.getPhotoItem().imagePath;
                            String draggedPath = dragged.getPhotoItem().imagePath;
                            if (targetPath == null) targetPath = "";
                            if (draggedPath == null) draggedPath = "";
                            if (!targetPath.equals(draggedPath))
                                target.swapImage(dragged);
                        }
                    }
                    break;
            }

            return true;
        }
    };

    //action id
    private static final int ID_EDIT = 1;
    private static final int ID_CHANGE = 2;
    private static final int ID_DELETE = 3;
    private static final int ID_CANCEL = 4;

    private List<PhotoItem> mPhotoItems;
    private List<GridPhotoImageView> mItemImageViews;
    private int mViewWidth, mViewHeight;
    private float mOutputScaleRatio = 1;

    public GridPhotoLayout(Context context, List<PhotoItem> photoItems) {
        super(context);
        mItemImageViews = new ArrayList<>();
        setLayerType(LAYER_TYPE_HARDWARE, null);
        mPhotoItems = photoItems;
    }
    public void setQuickActionClickListener(OnQuickActionClickListener quickActionClickListener) {
        mQuickActionClickListener = quickActionClickListener;
    }

    private GridPhotoImageView getSelectedFrameImageView(GridPhotoImageView target, DragEvent event) {
        GridPhotoImageView dragged = (GridPhotoImageView) event.getLocalState();
        int leftMargin = (int) (mViewWidth * target.getPhotoItem().bound.left);
        int topMargin = (int) (mViewHeight * target.getPhotoItem().bound.top);
        final float globalX = leftMargin + event.getX();
        final float globalY = topMargin + event.getY();
        for (int idx = mItemImageViews.size() - 1; idx >= 0; idx--) {
            GridPhotoImageView view = mItemImageViews.get(idx);
            float x = globalX - mViewWidth * view.getPhotoItem().bound.left;
            float y = globalY - mViewHeight * view.getPhotoItem().bound.top;
            if (view.isSelected(x, y)) {
                if (view == dragged) {
                    return null;
                } else {
                    return view;
                }
            }
        }
        return null;
    }

    public void saveInstanceState(Bundle outState) {
        if (mItemImageViews != null)
            for (GridPhotoImageView view : mItemImageViews)
                view.saveInstanceState(outState);
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        if (mItemImageViews != null)
            for (GridPhotoImageView view : mItemImageViews)
                view.restoreInstanceState(savedInstanceState);
    }
    private boolean isNotLargeThan1Gb() {
        ImageUtils.MemoryInfo memoryInfo = ImageUtils.getMemoryInfo(getContext());
        if (memoryInfo.totalMem > 0 && (memoryInfo.totalMem / 1048576.0 <= 1024)) {
            return true;
        } else {
            return false;
        }
    }

    public void build(final int viewWidth, final int viewHeight, final float outputScaleRatio, final float space, final float corner) {
        if (viewWidth < 1 || viewHeight < 1) {
            return;
        }
        //add children views
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
        mOutputScaleRatio = outputScaleRatio;
        mItemImageViews.clear();
        //A circle view always is on top
        if (mPhotoItems.size() > 4 || isNotLargeThan1Gb()) {
            ImageDecoder.SAMPLER_SIZE = 256;
        } else {
            ImageDecoder.SAMPLER_SIZE = 512;
        }
        for (PhotoItem item : mPhotoItems) {
            GridPhotoImageView imageView = addPhotoItemView(item, mOutputScaleRatio, space, corner);
            mItemImageViews.add(imageView);
        }
    }

    public void build(final int viewWidth, final int viewHeight, final float outputScaleRatio) {
        build(viewWidth, viewHeight, outputScaleRatio, 0, 0);
    }

    public void setSpace(float space, float corner) {
        for (GridPhotoImageView img : mItemImageViews)
            img.setSpace(space, corner);
    }

    private GridPhotoImageView addPhotoItemView(PhotoItem item, float outputScaleRatio, final float space, final float corner) {
        final GridPhotoImageView imageView = new GridPhotoImageView(getContext(), item);
        int leftMargin = (int) (mViewWidth * item.bound.left);
        int topMargin = (int) (mViewHeight * item.bound.top);
        int frameWidth = 0, frameHeight = 0;
        if (item.bound.right == 1) {
            frameWidth = mViewWidth - leftMargin;
        } else {
            frameWidth = (int) (mViewWidth * item.bound.width() + 0.5f);
        }

        if (item.bound.bottom == 1) {
            frameHeight = mViewHeight - topMargin;
        } else {
            frameHeight = (int) (mViewHeight * item.bound.height() + 0.5f);
        }

        imageView.init(frameWidth, frameHeight, outputScaleRatio, space, corner);
        imageView.setOnImageClickListener(this);
        if (mPhotoItems.size() > 1)
            imageView.setOnDragListener(mOnDragListener);

        LayoutParams params = new LayoutParams(frameWidth, frameHeight);
        params.leftMargin = leftMargin;
        params.topMargin = topMargin;
        imageView.setOriginalLayoutParams(params);
        addView(imageView, params);
        return imageView;
    }

    public Bitmap createImage() throws OutOfMemoryError {
        try {
            Bitmap template = Bitmap.createBitmap((int) (mOutputScaleRatio * mViewWidth), (int) (mOutputScaleRatio * mViewHeight), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(template);
            for (GridPhotoImageView view : mItemImageViews)
                if (view.getImage() != null && !view.getImage().isRecycled()) {
                    final int left = (int) (view.getLeft() * mOutputScaleRatio);
                    final int top = (int) (view.getTop() * mOutputScaleRatio);
                    final int width = (int) (view.getWidth() * mOutputScaleRatio);
                    final int height = (int) (view.getHeight() * mOutputScaleRatio);
                    //draw image
                    canvas.saveLayer(left, top, left + width, top + height, new Paint(), Canvas.ALL_SAVE_FLAG);
                    canvas.translate(left, top);
                    canvas.clipRect(0, 0, width, height);
                    view.drawOutputImage(canvas);
                    canvas.restore();
                }

            return template;
        } catch (OutOfMemoryError error) {
            throw error;
        }
    }

    public void recycleImages() {
        for (GridPhotoImageView view : mItemImageViews) {
            view.recycleImage();
        }
        System.gc();
    }

    @Override
    public void onLongClickImage(GridPhotoImageView v) {
        if (mPhotoItems.size() > 1) {
            v.setTag("x=" + v.getPhotoItem().x + ",y=" + v.getPhotoItem().y + ",path=" + v.getPhotoItem().imagePath);
            ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
            String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
            ClipData dragData = new ClipData(v.getTag().toString(), mimeTypes, item);
            DragShadowBuilder myShadow = new DragShadowBuilder(v);
            try {
                v.startDrag(dragData, myShadow, v, 0);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDoubleClickImage(GridPhotoImageView v) {
        if (mQuickActionClickListener != null) {
            mQuickActionClickListener.onDoubleTouchListner(v);
        }
    }

    @Override
    public void onSingleClickImage(GridPhotoImageView view) {
        Log.e("onSingleClickImage", "onSingleClickImage");
    }

    @Override
    public void onTouchImage(GridPhotoImageView view) {
        if (mQuickActionClickListener != null) {
            mQuickActionClickListener.onImageTouchListner(view);
        }
    }
}
