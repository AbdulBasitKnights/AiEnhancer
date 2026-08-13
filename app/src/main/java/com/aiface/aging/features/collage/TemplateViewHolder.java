package com.aiface.aging.features.collage;

import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.aiface.aging.R;
import com.aiface.aging.features.collage.model.TemplateItem;


public class TemplateViewHolder extends RecyclerView.ViewHolder {
    public interface OnTemplateItemClickListener {
        void onTemplateItemClick(final TemplateItem item);
    }

    private ImageView mImageView;

    TemplateViewHolder(View view) {
        super(view);
        mImageView = (ImageView) view.findViewById(R.id.imageView);
    }

    public void bindItem(final TemplateItem item, final OnTemplateItemClickListener listener) {
        ImageUtils.loadImageWithPicasso(mImageView.getContext(), mImageView, item.getPreview());
        mImageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTemplateItemClick(item);
            }
        });
    }
}