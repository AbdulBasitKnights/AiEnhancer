package com.aiface.aging.features.body.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aiface.aging.R;
import com.aiface.aging.features.body.Constant;


public class SkinColorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ItemClick itemClick;
    public int selected = 0;


    public interface ItemClick {
        void onItemClick(int i);
    }

    public SkinColorAdapter(Context context) {
        this.context = context;
    }

    public void setItemClick(ItemClick itemClick) {
        this.itemClick = itemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ItemViewHolder(LayoutInflater.from(this.context).inflate(R.layout.item_color, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
        DrawableCompat.setTint(DrawableCompat.wrap(itemViewHolder.li_back.getBackground()), Constant.listOfSkinColor[i]);
        itemViewHolder.img_check.setColorFilter(this.context.getResources().getColor(R.color.white));
        if (this.selected == i) {
            itemViewHolder.img_check.setVisibility(View.VISIBLE);
        } else {
            itemViewHolder.img_check.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return Constant.listOfSkinColor.length;
    }


    public class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView img_check;
        LinearLayout li_back;

        public ItemViewHolder(View view) {
            super(view);
            this.li_back = (LinearLayout) view.findViewById(R.id.li_back);
            this.img_check = (ImageView) view.findViewById(R.id.img_check);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    int i = SkinColorAdapter.this.selected;
                    SkinColorAdapter.this.selected = ItemViewHolder.this.getAdapterPosition();
                    SkinColorAdapter.this.notifyItemChanged(i);
                    SkinColorAdapter skinColorAdapter = SkinColorAdapter.this;
                    skinColorAdapter.notifyItemChanged(skinColorAdapter.selected);
                    if (SkinColorAdapter.this.itemClick != null) {
                        SkinColorAdapter.this.itemClick.onItemClick(SkinColorAdapter.this.selected);
                    }
                    SkinColorAdapter.this.notifyDataSetChanged();
                }
            });
        }
    }
}
