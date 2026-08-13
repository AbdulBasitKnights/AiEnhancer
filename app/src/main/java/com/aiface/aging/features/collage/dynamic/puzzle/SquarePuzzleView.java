package com.aiface.aging.features.collage.dynamic.puzzle;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;


public class SquarePuzzleView extends PuzzleView {
    private float increaseI = (float) 1.5;

    public SquarePuzzleView(Context context) {
        super(context);
    }

    public SquarePuzzleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquarePuzzleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getBackground();
        if (d != null) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = w * d.getIntrinsicHeight() / d.getIntrinsicWidth();
            setMeasuredDimension(w, h);
        } else super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
/////// old function
//  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//    int width = getMeasuredWidth();
//    int height = getMeasuredHeight();
//    int length = width > height ? height : width;
//
//    setMeasuredDimension(length, length);
//  }
}
