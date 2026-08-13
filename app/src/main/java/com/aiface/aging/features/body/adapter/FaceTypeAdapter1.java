package com.aiface.aging.features.body.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;


import com.aiface.aging.R;
import com.aiface.aging.features.body.model.FaceData;

import java.util.ArrayList;
import java.util.List;


public class FaceTypeAdapter1 extends RecyclerView.Adapter<FaceTypeAdapter1.ItemViewHolder> {
    Context context;
    public List<FaceData> faceDataList = new ArrayList();
    boolean isBody;
    ItemClick itemClick;


    public interface ItemClick {
        void onItemClick(String str);
    }

    public List<FaceData> getDataList() {
        return this.faceDataList;
    }

    public FaceTypeAdapter1(Context context) {
        this.isBody = false;
        this.context = context;
        this.isBody = false;
    }

    public void setData(List<FaceData> list) {
        this.faceDataList.clear();
        this.faceDataList.addAll(list);
        notifyDataSetChanged();
    }

    public void setItemClick(ItemClick itemClick) {
        this.itemClick = itemClick;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ItemViewHolder(LayoutInflater.from(this.context).inflate(R.layout.item_face1, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ItemViewHolder itemViewHolder, final int i) {
        TextView textView = itemViewHolder.txt_itemName;
        textView.setText(this.faceDataList.get(i).getItem_name() + "");
        itemViewHolder.item_icon.setImageDrawable(this.context.getResources().getDrawable(this.faceDataList.get(i).getIcon().intValue()));
        if (this.faceDataList.get(i).isSelected()) {
            itemViewHolder.txt_itemName.setTextColor(this.context.getResources().getColor(R.color.primaryColor));
            itemViewHolder.item_icon.setColorFilter(this.context.getResources().getColor(R.color.primaryColor));
            itemViewHolder.txt_itemName.setTypeface(ResourcesCompat.getFont(this.context, R.font.inter_semibold));
        } else {
            itemViewHolder.txt_itemName.setTextColor(this.context.getResources().getColor(R.color.text_secondary));
            itemViewHolder.item_icon.setColorFilter(ResourcesCompat.getColor(this.context.getResources(), R.color.icon_primary, null));
            itemViewHolder.txt_itemName.setTypeface(ResourcesCompat.getFont(this.context, R.font.inter_regular));
        }
        itemViewHolder.root_layout.setOnClickListener(view -> FaceTypeAdapter1.this.itemClick.onItemClick(FaceTypeAdapter1.this.faceDataList.get(i).getItem_name()));
    }

    @Override
    public int getItemCount() {
        return this.faceDataList.size();
    }


    public class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView item_icon;
        LinearLayout root_layout;
        TextView txt_itemName;

        public ItemViewHolder(View view) {
            super(view);
            this.item_icon = (ImageView) view.findViewById(R.id.item_icon);
            this.txt_itemName = (TextView) view.findViewById(R.id.txt_itemName);
            this.root_layout = (LinearLayout) view.findViewById(R.id.root_layout);
        }
    }
}
