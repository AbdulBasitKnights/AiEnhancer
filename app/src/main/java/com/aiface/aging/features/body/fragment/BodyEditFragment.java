package com.aiface.aging.features.body.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.aiface.aging.R;
import com.aiface.aging.features.body.Constant;
import com.aiface.aging.features.body.activities.ImageEditingER;
import com.aiface.aging.features.body.adapter.FaceTypeAdapter1;
import com.aiface.aging.features.body.adapter.SkinColorAdapter;
import com.aiface.aging.features.body.inerfaces.MenuClick;
import com.aiface.aging.features.body.model.FaceData;
import com.aiface.aging.features.body.vieenhanceeeData;
import com.aiface.aging.features.body.viewHeight;
import com.aiface.aging.features.body.viewRefineData;
import com.aiface.aging.features.body.viewSkinData;
import com.aiface.aging.features.body.viewWaistData;
import com.aiface.aging.features.body.viewhipsData;

import java.util.ArrayList;
import java.util.List;


public class BodyEditFragment extends Fragment {
    FaceTypeAdapter1 adapter;
    Context context;
//    ImageEditing imageEditing;
    ImageEditingER imageEditing;
    ImageView img_close_text;
    ImageView img_done_text;
    public ImageView img_draw;
    public ImageView img_eraser;
    RelativeLayout li_done;
    public LinearLayout li_draw;
    public LinearLayout li_erase;
    LinearLayout li_main_face;
    RelativeLayout li_parent;
    RelativeLayout li_seekbar;
    public LinearLayout li_subitem;
    private SkinColorAdapter mColorAdapter;
    public BackPressed mCurrentInterface;
    MenuClick menuClick;
    private OptionChoose optionChoose;
    RecyclerView rv_edittext;
    RecyclerView rv_sub_items_face;
    SeekBar seek;
    ArcSeekBar seekbar_text;
    public TextView txt_draw;
    public TextView txt_eraser;
    TextView txt_selected_title;
    View v;
    public String selected_datass = "";
    List<FaceData> faceDataList = new ArrayList();


    public interface BackPressed {
        void onBackPressed(boolean z);
    }


    public interface OptionChoose {
        void VisibleHideSeekView(boolean z);

        void onItemClick(String str);

        void showOrigin(boolean z);

        void text_done(boolean z);

        void text_done(boolean z, String str);
    }

//    public BodyEditFragment(Context context, ImageEditing imageEditing, RecyclerView recyclerView, MenuClick menuClick) {
    public BodyEditFragment(Context context, ImageEditingER imageEditing, RecyclerView recyclerView, MenuClick menuClick) {
        this.context = context;
        this.imageEditing = imageEditing;
        this.rv_sub_items_face = recyclerView;
        this.menuClick = menuClick;
    }

    public BodyEditFragment() {
    }

    public void setListener(OptionChoose optionChoose) {
        this.optionChoose = optionChoose;
    }

    public void commitPendingEditsIfNeeded() {
        if (mCurrentInterface == null) {
            return;
        }
        boolean toolPanelOpen = li_seekbar != null && li_seekbar.getVisibility() == View.VISIBLE;
        boolean skinPanelOpen = rv_sub_items_face != null && rv_sub_items_face.getVisibility() == View.VISIBLE;
        if (toolPanelOpen || skinPanelOpen) {
            mCurrentInterface.onBackPressed(true);
        }
    }

    public boolean isBackVisible() {
        return this.li_done.getVisibility() == View.VISIBLE;
    }

    public void ClickBack() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                BodyEditFragment.this.selected_datass = "";
            }
        }, 600L);
        if (this.rv_sub_items_face.getVisibility() == View.VISIBLE) {
            this.rv_sub_items_face.setVisibility(View.GONE);
            this.li_subitem.setVisibility(View.GONE);
            setAnimationGone1(this.li_seekbar);
            this.rv_edittext.setVisibility(View.VISIBLE);
            this.li_done.setVisibility(View.GONE);
            BackPressed backPressed = this.mCurrentInterface;
            if (backPressed != null) {
                backPressed.onBackPressed(false);
            }
        } else if (this.li_seekbar.getVisibility() == View.VISIBLE) {
            this.rv_edittext.setVisibility(View.VISIBLE);
            this.li_done.setVisibility(View.GONE);
            setAnimationGone1(this.li_seekbar);
            BackPressed backPressed2 = this.mCurrentInterface;
            if (backPressed2 != null) {
                backPressed2.onBackPressed(false);
            }
            OptionChoose optionChoose = this.optionChoose;
            if (optionChoose != null) {
                optionChoose.text_done(false, this.selected_datass);
            }
        } else {
            OptionChoose optionChoose2 = this.optionChoose;
            if (optionChoose2 != null) {
                optionChoose2.text_done(true);
            }
        }
    }

    public void visibleHideSeekView(boolean z) {
        if (z) {
            setAnimationVisible(this.li_seekbar);
            this.li_done.setVisibility(View.VISIBLE);
            TextView textView = this.txt_selected_title;
            textView.setText(this.selected_datass + "");
            return;
        }
        if (this.li_seekbar.getVisibility() != View.GONE) {
            this.li_seekbar.setVisibility(View.GONE);
        }
        this.li_done.setVisibility(View.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.bottom_face_edit_dialog1, viewGroup, false);
        initView(inflate);
        this.li_main_face.setVisibility(View.VISIBLE);
        this.txt_selected_title.setVisibility(View.VISIBLE);
        this.txt_selected_title.setText(Constant.Body);
        this.rv_edittext.setVisibility(View.VISIBLE);
        for (int i = 0; i < Constant.body_array.length; i++) {
            this.faceDataList.add(new FaceData(Constant.body_array[i], false, Integer.valueOf(Constant.body_array_icons[i])));
        }
        if (this.rv_sub_items_face == null) {
//            this.rv_sub_items_face = ((ImageEditing) getActivity()).rv_sub_items_face;
            this.rv_sub_items_face = ((ImageEditingER) getActivity()).rv_sub_items_face;
        }
        this.rv_sub_items_face.setLayoutManager(new LinearLayoutManager(this.context, RecyclerView.VERTICAL, false));
        final SkinColorAdapter skinColorAdapter = new SkinColorAdapter(this.context);
        this.mColorAdapter = skinColorAdapter;
        this.rv_sub_items_face.setAdapter(skinColorAdapter);
        FaceTypeAdapter1 faceTypeAdapter1 = new FaceTypeAdapter1(this.context);
        this.adapter = faceTypeAdapter1;
        faceTypeAdapter1.setItemClick(str -> {
            for (int i2 = 0; i2 < BodyEditFragment.this.adapter.faceDataList.size(); i2++) {
                if (str.equalsIgnoreCase(BodyEditFragment.this.adapter.faceDataList.get(i2).getItem_name())) {
                    BodyEditFragment.this.adapter.faceDataList.get(i2).setSelected(true);
                } else {
                    BodyEditFragment.this.adapter.faceDataList.get(i2).setSelected(false);
                }
            }
            BodyEditFragment.this.adapter.notifyDataSetChanged();
            if (BodyEditFragment.this.selected_datass.equalsIgnoreCase(str)) {
                return;
            }
            BodyEditFragment.this.selected_datass = str;
            if (BodyEditFragment.this.optionChoose != null) {
                BodyEditFragment.this.optionChoose.onItemClick(str);
            }
            BodyEditFragment.this.rv_edittext.setVisibility(View.GONE);
            if (BodyEditFragment.this.optionChoose != null) {
                BodyEditFragment.this.optionChoose.onItemClick(str);
            }
            Constant.selected_data = str;

            BodyEditFragment.this.visibleHideSeekView(true);
            if (BodyEditFragment.this.rv_sub_items_face.getVisibility() == View.VISIBLE) {
                BodyEditFragment.this.rv_sub_items_face.setVisibility(View.GONE);
                BodyEditFragment.this.li_subitem.setVisibility(View.GONE);
                BodyEditFragment bodyEditFragment = BodyEditFragment.this;
                bodyEditFragment.setAnimationGone1(bodyEditFragment.li_seekbar);
                if (BodyEditFragment.this.mCurrentInterface != null) {
                    BodyEditFragment.this.mCurrentInterface.onBackPressed(false);
                }
            }
            else if (BodyEditFragment.this.mCurrentInterface != null) {
                BodyEditFragment.this.mCurrentInterface.onBackPressed(false);
            }
            if (str.equalsIgnoreCase(Constant.Refine)) {
                if (BodyEditFragment.this.optionChoose != null) {
                    BodyEditFragment.this.optionChoose.onItemClick(Constant.selected_data);
                    BodyEditFragment.this.optionChoose.VisibleHideSeekView(true);
                }
                Bitmap bitmap = BodyEditFragment.this.imageEditing.mCurrentBitmap;
//                ImageEditing imageEditing = BodyEditFragment.this.imageEditing;
                ImageEditingER imageEditing = BodyEditFragment.this.imageEditing;

                seek.setOnSeekBarChangeListener(null);

                BodyEditFragment.this.mCurrentInterface = new viewRefineData(bitmap, imageEditing, imageEditing.img_person1, BodyEditFragment.this.seekbar_text,
                        BodyEditFragment.this.seek, BodyEditFragment.this.menuClick);
                BodyEditFragment.this.seek.setProgress(50);
                BodyEditFragment.this.seekbar_text.setProgress(100);
            }
            else if (str.equalsIgnoreCase(Constant.Enhance)) {
                if (BodyEditFragment.this.optionChoose != null) {
                    BodyEditFragment.this.optionChoose.onItemClick(Constant.selected_data);
                    BodyEditFragment.this.optionChoose.VisibleHideSeekView(true);
                }
                Bitmap bitmap2 = BodyEditFragment.this.imageEditing.mCurrentBitmap;
//                ImageEditing imageEditing2 = BodyEditFragment.this.imageEditing;
                ImageEditingER imageEditing2 = BodyEditFragment.this.imageEditing;
                seek.setOnSeekBarChangeListener(null);
                BodyEditFragment.this.mCurrentInterface = new vieenhanceeeData(bitmap2, imageEditing2, imageEditing2.img_person1, BodyEditFragment.this.seekbar_text, BodyEditFragment.this.seek, BodyEditFragment.this.menuClick);

                BodyEditFragment.this.seek.setProgress(50);
                BodyEditFragment.this.seekbar_text.setProgress(100);
            }
            else if (str.equalsIgnoreCase(Constant.Height)) {
                if (BodyEditFragment.this.optionChoose != null) {
                    BodyEditFragment.this.optionChoose.onItemClick(Constant.selected_data);
                    BodyEditFragment.this.optionChoose.VisibleHideSeekView(true);
                }
                Bitmap bitmap3 = BodyEditFragment.this.imageEditing.mCurrentBitmap;
//                ImageEditing imageEditing3 = BodyEditFragment.this.imageEditing;
                ImageEditingER imageEditing3 = BodyEditFragment.this.imageEditing;
                seek.setOnSeekBarChangeListener(null);
                BodyEditFragment.this.mCurrentInterface = new viewHeight(bitmap3, imageEditing3, imageEditing3.img_person1, BodyEditFragment.this.seekbar_text, BodyEditFragment.this.seek, BodyEditFragment.this.menuClick);
                BodyEditFragment.this.seek.setProgress(50);
                BodyEditFragment.this.seekbar_text.setProgress(100);
            }
            else if (str.equalsIgnoreCase(Constant.Waist)) {
                if (BodyEditFragment.this.optionChoose != null) {
                    BodyEditFragment.this.optionChoose.onItemClick(Constant.selected_data);
                    BodyEditFragment.this.optionChoose.VisibleHideSeekView(true);
                }
                Bitmap bitmap4 = BodyEditFragment.this.imageEditing.mCurrentBitmap;
//                ImageEditing imageEditing4 = BodyEditFragment.this.imageEditing;
                ImageEditingER imageEditing4 = BodyEditFragment.this.imageEditing;
                seek.setOnSeekBarChangeListener(null);
                BodyEditFragment.this.mCurrentInterface = new viewWaistData(bitmap4, imageEditing4, imageEditing4.img_person1, BodyEditFragment.this.seekbar_text, BodyEditFragment.this.seek, BodyEditFragment.this.menuClick);

                BodyEditFragment.this.seek.setProgress(50);
                BodyEditFragment.this.seekbar_text.setProgress(100);
            }
            else if (str.equalsIgnoreCase(Constant.Hips)) {
                if (BodyEditFragment.this.optionChoose != null) {
                    BodyEditFragment.this.optionChoose.onItemClick(Constant.selected_data);
                    BodyEditFragment.this.optionChoose.VisibleHideSeekView(true);
                }
                Bitmap bitmap5 = BodyEditFragment.this.imageEditing.mCurrentBitmap;
//                ImageEditing imageEditing5 = BodyEditFragment.this.imageEditing;
                ImageEditingER imageEditing5 = BodyEditFragment.this.imageEditing;
                seek.setOnSeekBarChangeListener(null);
                BodyEditFragment.this.mCurrentInterface = new viewhipsData(bitmap5, imageEditing5, imageEditing5.img_person1, BodyEditFragment.this.seekbar_text, BodyEditFragment.this.seek, BodyEditFragment.this.menuClick);
                BodyEditFragment.this.seek.setProgress(50);
                BodyEditFragment.this.seekbar_text.setProgress(100);
            }
            else if (str.equalsIgnoreCase(Constant.PaintSkin)) {
                if (BodyEditFragment.this.optionChoose != null) {
                    BodyEditFragment.this.optionChoose.onItemClick(Constant.selected_data);
                    BodyEditFragment.this.optionChoose.VisibleHideSeekView(true);
                }
                Bitmap bitmap6 = BodyEditFragment.this.imageEditing.mCurrentBitmap;
//                ImageEditing imageEditing6 = BodyEditFragment.this.imageEditing;
                ImageEditingER imageEditing6 = BodyEditFragment.this.imageEditing;
                BodyEditFragment.this.rv_sub_items_face.setVisibility(View.VISIBLE);
                BodyEditFragment.this.li_subitem.setVisibility(View.VISIBLE);
                BodyEditFragment bodyEditFragment2 = BodyEditFragment.this;
                seek.setOnSeekBarChangeListener(null);
                bodyEditFragment2.mCurrentInterface = new viewSkinData(bitmap6, bodyEditFragment2, imageEditing6, imageEditing6.img_person1, BodyEditFragment.this.rv_sub_items_face, BodyEditFragment.this.seekbar_text, BodyEditFragment.this.li_seekbar, skinColorAdapter, BodyEditFragment.this.seek, BodyEditFragment.this.menuClick);
                BodyEditFragment.this.seek.setProgress(50);
                BodyEditFragment.this.seekbar_text.setProgress(100);
            }
        });
        this.rv_edittext.setAdapter(this.adapter);
        this.adapter.setData(this.faceDataList);
        this.img_close_text.setOnClickListener(view -> {
            BodyEditFragment.this.ClickBack();
            BodyEditFragment.this.selected_datass = "";
            BodyEditFragment.this.img_close_text.setClickable(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    BodyEditFragment.this.img_close_text.setClickable(true);
                }
            }, 500L);
        });
        this.img_done_text.setOnClickListener(view -> {
            BodyEditFragment.this.imageEditing.updateUndoRedoIcons();
            if (BodyEditFragment.this.rv_sub_items_face.getVisibility() == View.VISIBLE) {
                BodyEditFragment bodyEditFragment = BodyEditFragment.this;
                bodyEditFragment.setAnimationGone1(bodyEditFragment.li_seekbar);
                BodyEditFragment.this.rv_sub_items_face.setVisibility(View.GONE);
                BodyEditFragment.this.li_subitem.setVisibility(View.GONE);
                BodyEditFragment.this.rv_edittext.setVisibility(View.VISIBLE);
                BodyEditFragment.this.li_done.setVisibility(View.GONE);
                if (BodyEditFragment.this.mCurrentInterface != null) {
                    BodyEditFragment.this.mCurrentInterface.onBackPressed(true);
                }
            } else if (BodyEditFragment.this.li_seekbar.getVisibility() == View.VISIBLE) {
                BodyEditFragment.this.rv_edittext.setVisibility(View.VISIBLE);
                BodyEditFragment.this.li_done.setVisibility(View.GONE);
                BodyEditFragment bodyEditFragment2 = BodyEditFragment.this;
                bodyEditFragment2.setAnimationGone1(bodyEditFragment2.li_seekbar);
                if (BodyEditFragment.this.mCurrentInterface != null) {
                    BodyEditFragment.this.mCurrentInterface.onBackPressed(true);
                }
                if (BodyEditFragment.this.optionChoose != null) {
                    BodyEditFragment.this.optionChoose.text_done(true, BodyEditFragment.this.selected_datass);
                }
            } else {
                BodyEditFragment.this.ClickBack();
            }
            BodyEditFragment.this.selected_datass = "";
            BodyEditFragment.this.img_done_text.setClickable(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    BodyEditFragment.this.img_done_text.setClickable(true);
                }
            }, 500L);
        });
        return inflate;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
    }

    public void setAnimationVisible(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void setAnimationGone1(View view) {
        if (view.getVisibility() != View.GONE) {
            view.setVisibility(View.GONE);
        }
    }

    private void initView(View view) {
        this.li_main_face = (LinearLayout) view.findViewById(R.id.li_main_face);
        this.seekbar_text = (ArcSeekBar) view.findViewById(R.id.seekbar_text);
        this.seek = (SeekBar) view.findViewById(R.id.seek);
        this.li_seekbar = (RelativeLayout) view.findViewById(R.id.li_seekbar);
        this.li_parent = (RelativeLayout) view.findViewById(R.id.li_parent);
        RelativeLayout relativeLayout = (RelativeLayout) view.findViewById(R.id.li_done);
        this.li_done = relativeLayout;
        relativeLayout.setVisibility(View.GONE);
        this.img_close_text = (ImageView) view.findViewById(R.id.img_close_text);
        this.img_done_text = (ImageView) view.findViewById(R.id.img_done_text);
        this.rv_edittext = (RecyclerView) view.findViewById(R.id.rv_edittext);
        this.txt_selected_title = (TextView) view.findViewById(R.id.txt_selected_title);
        this.li_subitem = (LinearLayout) view.findViewById(R.id.li_subitem);
        this.li_erase = (LinearLayout) view.findViewById(R.id.li_erase);
        this.li_draw = (LinearLayout) view.findViewById(R.id.li_draw);
        this.img_draw = (ImageView) view.findViewById(R.id.img_draw);
        this.img_eraser = (ImageView) view.findViewById(R.id.img_eraser);
        this.txt_eraser = (TextView) view.findViewById(R.id.txt_eraser);
        this.txt_draw = (TextView) view.findViewById(R.id.txt_draw);
        this.li_parent.bringToFront();
        this.rv_edittext.setLayoutManager(new LinearLayoutManager(this.context, RecyclerView.HORIZONTAL, false));
    }
}
