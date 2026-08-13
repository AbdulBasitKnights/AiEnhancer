package com.aiface.aging.features.text.text

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.shared.safePopSupportBackStack
import com.aiface.aging.shared.showSoftKeyboard
import com.aiface.aging.databinding.FragmentTextBinding

import com.aiface.aging.shared.editorui.BottomActionListener
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.utils.AppFonts
import com.aiface.aging.utils.AppUtils.setCustomMargins
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.DrawableClass
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.ToastUtils
import com.aiface.aging.utils.ToastUtils.showInternetWarningToast
import com.aiface.aging.utils.ToastUtils.showToast
import com.xiaopo.flying.sticker.Sticker
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.TextStickerCustom
import com.xw.repo.BubbleSeekBar
import com.xw.repo.BubbleSeekBar.OnProgressChangedListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface TextItemListener {
    fun onTextItemClick(position: Int, modelDrawableAssets: ModelDrawableAssets)
    fun onFontItemClick(position: Int)
    fun onColorItemClick(position: Int)
    fun onColorTypeClick(position: Int)
}

@AndroidEntryPoint
class TextFragment : Fragment(), TextItemListener {
    companion object {
        private var stickerView: StickerView? = null
        private var addText: Boolean = true
        private var actionListener: BottomActionListener? = null
        fun newInstance(
            stickerView: StickerView?,
            addText: Boolean,
            actionListener: BottomActionListener? = null
        ): TextFragment {
            val fragment = TextFragment()
            Companion.stickerView = stickerView
            Companion.addText = addText
            Companion.actionListener = actionListener
            return fragment
        }
    }

    private lateinit var binding: FragmentTextBinding
    private var mActivity: FragmentActivity? = null
    private lateinit var adapterTextMain: AdapterTextMain
    private val viewModel: ViewModelTextFragment by viewModels()

    private var sticker: TextStickerCustom? = null
    var textItemListener: TextItemListener? = null
    private val typefaces = ArrayList<Typeface?>()
    private var text_Color = Color.WHITE
    private var fontsAdapter: FontsAdapter? = null
    private var text_shadowDx = 0f
    private var text_shadowDy = 0f
    private var text_shadowRadius = 0f
    private var text_shadowColor = Color.BLACK
    private var isTextColor: Boolean? = null
    private var isBGColor: Boolean? = null
    private var isShadowColor: Boolean? = null
    private var BgColor = 0
    private lateinit var BgGradient: Array<String>
    private var isGradient = false
    private lateinit var solidColorsList: IntArray
    private var colorsAdapter: ColorsAdapter? = null
    private var colorTypeAdapter: ColorTypeAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            setClickListeners()
            textItemListener = this
            BgGradient = DrawableClass.gradientColors.get(0)

            setUpTextMainRecyclerview(activity)

            if (addText) {
                showDialogueForTextInput(activity, false)
            }
            showTextColorLayout()
        }
    }

    private fun setClickListeners() {
        binding.btnCrossBg.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            try {
                actionListener?.onActionCancelClick("text", null)
                // Editor Activity closes panel in onActionCancelClick → hideFragment
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.btnTickBg.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            try {
                actionListener?.onActionCancelClick("text", null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showDialogueForTextInput(
        activity: FragmentActivity,
        isEdit: Boolean
    ) {
        val dialog = DialogueUtils.getDialogue(activity, R.layout.dialog_input_text)
        val buttonDone = dialog.findViewById<AppCompatButton>(R.id.buttonOk)
        val buttonCancel = dialog.findViewById<AppCompatButton>(R.id.buttonCancel)
        val editText = dialog.findViewById<EditText>(R.id.inputEditText)

        if (isEdit) {
            getCurrentStickerText()?.let {
                editText.setText(it)
            }
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                buttonDone.background = ContextCompat.getDrawable(activity, R.drawable.rounded_filled_dark_grey)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // text changing
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                // final text after change
                if (text.isEmpty()){
                    buttonDone.isClickable = false
                    buttonDone.isEnabled = false
                    buttonDone.background = ContextCompat.getDrawable(activity, R.drawable.rounded_filled_dark_grey)
                }else{
                    buttonDone.isClickable = true
                    buttonDone.isEnabled = true
                    buttonDone.background = ContextCompat.getDrawable(activity, R.drawable.rounded_filled_dark_blue)
                }

            }
        })

        buttonDone.setOnClickListener {
            if (!activity.isFinishing) {
                dialog.dismiss()
            }

            val trimmedText = editText.text.trim()
            if (trimmedText.isNotEmpty()) {
                if (isEdit) {
                    updateText(trimmedText.toString())
                    adapterTextMain.unselectBottomItem()
                } else {
                    addTextSticker(
                        trimmedText.toString(),
                        getscaleDrawable(BgColor, 255, activity),
                        activity
                    )
                }
            }
            //  actionListener?.onActionCancelClick("text", null)
        }

        buttonCancel.setOnClickListener {
            if (!activity.isFinishing) {
                dialog.dismiss()
                if (!isEdit) {
//                    activity.onBackPressed()
                    activity.supportFragmentManager.popBackStack()
                } else {
                    adapterTextMain.unselectBottomItem()
                }
                actionListener?.onActionCancelClick("text", null)
            }
        }
        // Show the dialog
        if (!activity.isFinishing) {
            dialog.show()
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        editText.postDelayed({
            editText.showSoftKeyboard()
        }, 100)
    }

    private fun setUpTextMainRecyclerview(activity: FragmentActivity) {
        adapterTextMain = AdapterTextMain(this, activity)
        binding.textRecycler.adapter = adapterTextMain
        subscribeTextMainUi(adapterTextMain)
    }

    private fun subscribeTextMainUi(adapter: AdapterTextMain) {
        lifecycleScope.launchWhenStarted {
            mActivity?.let {
                viewModel.loadTextIcons(it).collectLatest { icons ->
                    if (icons.isNotEmpty()) {
                        adapter.submitList(icons)
                    }
                }
            }
        }
    }

    override fun onTextItemClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        adapterTextMain.selectBottomItem(position)
        when (position) {
            0 -> {
                binding.fontsLayout.visibility = View.GONE
                binding.textColorLayout.adjustTextColorLayout.visibility = View.GONE
                binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.GONE
                mActivity?.let {
                    showDialogueForTextInput(it, true)
                }
            }

            1 -> {
                binding.textColorLayout.adjustTextColorLayout.visibility = View.GONE
                binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.GONE
                binding.fontsLayout.visibility = View.VISIBLE
                mActivity?.let {
                    initFonts(it)
                }
            }

            2 -> {
                showTextColorLayout()
            }

            3 -> {
                isTextColor = false
                isShadowColor = false
                isBGColor = true
                isGradient = false
                mActivity?.let {
                    opacity_colors_seeks(it)
                    colorAdpaterSet(it)
                    colorTypeAdpaterSet(it)
                }
                binding.fontsLayout.visibility = View.GONE
                binding.textColorLayout.shadowOffsetSeek.visibility = View.GONE
                binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.GONE
                binding.textColorLayout.adjustTextColorLayout.visibility = View.VISIBLE
                binding.textColorLayout.opacitySeekbar.visibility = View.VISIBLE
            }

//            "Shadow" -> {
//                isTextColor = false
//                isBGColor = false
//                isShadowColor = true
//                isGradient = false
//                binding.fontsLayout.visibility = View.GONE
//                binding.textColorLayout.opacitySeekbar.visibility = View.GONE
//                binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.GONE
//                mActivity?.let {
//                    TextShadowChange()
//                    colorAdpaterSet(it)
//                    colorTypeAdpaterSet(it)
//                }
//                binding.textColorLayout.adjustTextColorLayout.visibility = View.VISIBLE
//                binding.textColorLayout.shadowOffsetSeek.visibility = View.VISIBLE
//            }

            4 -> {
                binding.fontsLayout.visibility = View.GONE
                binding.textColorLayout.adjustTextColorLayout.visibility = View.GONE
                binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.VISIBLE
                textAlign_seeks()
            }

            5 -> {
                binding.fontsLayout.visibility = View.GONE
                binding.textColorLayout.adjustTextColorLayout.visibility = View.GONE
                binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.VISIBLE
                val sticker = stickerView?.currentSticker
                if (sticker is TextStickerCustom) {
                    if (StickerView.isSelected == true) {
                        sticker.setTextAlign(Layout.Alignment.ALIGN_NORMAL)
                        sticker.resizeText()
                        stickerView?.invalidate()
                    } else {
                        mActivity?.let {
                            if (!NetworkUtils.isOnline(it)) {
                                showInternetWarningToast(it)
                            }
                        }
                    }
                }
            }

            6 -> {
                binding.fontsLayout.visibility = View.GONE
                binding.textColorLayout.adjustTextColorLayout.visibility = View.GONE
                binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.VISIBLE
                val sticker = stickerView?.currentSticker
                if (sticker is TextStickerCustom) {
                    if (StickerView.isSelected == true) {
                        sticker.setTextAlign(Layout.Alignment.ALIGN_CENTER)
                        sticker.resizeText()
                        stickerView?.invalidate()
                    } else {
                        mActivity?.let {
                            if (!NetworkUtils.isOnline(it)) {
                                showInternetWarningToast(it)
                            }
                        }
                    }
                }
            }

            7 -> {
                binding.fontsLayout.visibility = View.GONE
                binding.textColorLayout.adjustTextColorLayout.visibility = View.GONE
                binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.VISIBLE
                val sticker = stickerView?.currentSticker
                if (sticker is TextStickerCustom) {
                    if (StickerView.isSelected == true) {
                        sticker.setTextAlign(Layout.Alignment.ALIGN_OPPOSITE)
                        sticker.resizeText()
                        stickerView?.invalidate()
                    } else {
                        mActivity?.let {
                            if (!NetworkUtils.isOnline(it)) {
                                showInternetWarningToast(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showTextColorLayout() {
        isTextColor = true
        isBGColor = false
        isGradient = false
        isShadowColor = false
        mActivity?.let {
            opacity_colors_seeks(it)
            colorAdpaterSet(it)
            colorTypeAdpaterSet(it)
        }
        binding.textAlignLayout.adjustTextAlignmentLayout.visibility = View.GONE
        binding.fontsLayout.visibility = View.GONE
        binding.textColorLayout.shadowOffsetSeek.visibility = View.GONE
        binding.textColorLayout.adjustTextColorLayout.visibility = View.VISIBLE
        binding.textColorLayout.opacitySeekbar.visibility = View.VISIBLE
    }

    private fun getStrings(id: Int, activity: FragmentActivity): String {
        return activity.resources.getString(id)
    }

    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun addTextSticker(text: String?, drawable: Drawable?, activity: FragmentActivity) {
        /*final TextSticker */
        try {
            text_shadowRadius = 4.0f
            text_shadowDx = 0.0f
            text_shadowDy = 2.0f
            StickerView.isSelected = true
            sticker = TextStickerCustom(activity)
            drawable?.let { d->
                sticker?.let { sticker->
                    sticker.text = text
                    sticker.drawable = d
                    sticker.setTextColor(text_Color)
                    sticker.setTextAlign(Layout.Alignment.ALIGN_CENTER)
                    sticker.resizeText()
                    stickerView?.addSticker(sticker)
                }
            }
        } catch (e: Exception) {
            mActivity?.let { ToastUtils.showErrorToast(it) }
        }
    }

    private fun getGradient(colors: Array<String>): IntArray {
        val colorList = IntArray(colors.size)
        for (i in colors.indices) {
            var color: Int
            color = Color.parseColor(colors[i])
            colorList[i] = color
        }
        return colorList
    }

    private fun getGradientScaleDrawable(
        colors: Array<String>,
        alpha: Int,
        activity: FragmentActivity
    ): Drawable {
        val drawable = GradientDrawable()
        drawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        drawable.colors = getGradient(colors)
        drawable.alpha = alpha
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setSize(dip2px(activity, 200f), dip2px(activity, 40f))
        return drawable
    }

    private fun getscaleDrawable(color: Int, alpha: Int, activity: FragmentActivity): Drawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.alpha = alpha
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setSize(dip2px(activity, 200f), dip2px(activity, 40f))
        return drawable
    }

    private fun updateText(text: String?) {
        val sticker = stickerView?.currentSticker
        if (sticker is TextStickerCustom) {
            if (StickerView.isSelected == true) {
                if (text != null && sticker as TextStickerCustom? != null) {
                    sticker.text = text
                    sticker.resizeText()
                    stickerView?.invalidate()
                } else {
                    sticker.text = " "
                    sticker.resizeText()
                    stickerView?.invalidate()
                }
            } else {
                mActivity?.let {
                    showToast(it, getStrings(R.string.select_object, it))
                }
            }
        }
    }

    private fun getCurrentStickerText(): String? {
        var currentText = ""
        val sticker = stickerView?.currentSticker
        if (sticker is TextStickerCustom) {
            if (StickerView.isSelected == true) {
                currentText = sticker.text.toString()
            }
        }
        return currentText
    }


    override fun onFontItemClick(position: Int) {
        val sticker = stickerView?.currentSticker
        mActivity?.let {
            updateFont(it, sticker, position)
        }
    }

    override fun onColorItemClick(position: Int) {
        stickerView?.currentSticker?.let { sticker ->
            if (sticker is TextStickerCustom) {
                if (StickerView.isSelected == true) {
                    if (isTextColor == true) {
                        if (isGradient == false) {
                            sticker.setTextColor(solidColorsList[position])
                        } else if (isGradient == true) {
                            sticker.setTextGradient(DrawableClass.gradientColors.get(position))
                        }
                    } else if (isBGColor == true) {
                        if (isGradient == false) {
                            BgColor = solidColorsList[position]
                            mActivity?.let {
                                sticker.drawable = getscaleDrawable(BgColor, 255, it)
                            }
                        } else if (isGradient == true) {
                            BgGradient = DrawableClass.gradientColors.get(position)
                            mActivity?.let {
                                sticker.drawable = getGradientScaleDrawable(
                                    DrawableClass.gradientColors.get(position),
                                    255,
                                    it
                                )
                            }
                        }
                    } else if (isShadowColor == true) {
                        if (isGradient == false) {
                            text_shadowColor = solidColorsList[position]
                            sticker.setShadowLayer(
                                text_shadowRadius,
                                text_shadowDx,
                                text_shadowDy,
                                text_shadowColor
                            )
                        } else if (isGradient == true) {
                        }
                    }
                    stickerView?.invalidate()
                }
            }
        }

    }

    override fun onColorTypeClick(position: Int) {
        if (position == 0) {
            isGradient = false
            mActivity?.let { colorAdpaterSet(it) }
        } else if (position == 1) {
            if (isShadowColor == true) {
                mActivity?.let {
                    showToast(
                        it,
                        "Gradients are not applicable for shadow"
                    )
                }
            } else {
                isGradient = true
                mActivity?.let { colorAdpaterSet(it) }
            }
        }
    }

    private fun initFonts(activity: FragmentActivity) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                binding.fontsRecyclerview.visibility = View.GONE
                binding.loadingText.visibility = View.VISIBLE
            }
            val fonts = withContext(Dispatchers.IO) {
                getFonts(activity)
            }
            withContext(Dispatchers.Main) {
                if (fonts.isNotEmpty()) {
                    typefaces.addAll(fonts)
                    binding.loadingText.visibility = View.GONE
                    binding.fontsRecyclerview.visibility = View.VISIBLE
                    fontsAdapter = FontsAdapter(activity, fonts, textItemListener)
                    binding.fontsRecyclerview.adapter = fontsAdapter
                }
            }
        }
    }

    private fun getFonts(activity: FragmentActivity): ArrayList<Typeface?> {
        val typefaces = ArrayList<Typeface?>()
        if (!NetworkUtils.isOnline(activity)) {
            try {
                for (i in 0 until AppFonts.offlineFonts) {
                    val typeface = ResourcesCompat.getFont(activity, AppFonts.fonts.get(i))
                    typefaces.add(typeface)
                }
            } catch (e: Exception) {
            }
        } else {
            try {
                var length = 0
                length =
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O || Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
                        AppFonts.offlineFonts
                    } else {
                        AppFonts.fonts.size
                    }
                for (i in 0 until length) {
                    val typeface = ResourcesCompat.getFont(activity, AppFonts.fonts.get(i))
                    typefaces.add(typeface)
                }
            } catch (e: Exception) {
            }
        }
        return typefaces
    }

    private fun opacity_colors_seeks(activity: FragmentActivity) {
        try {
            binding.textColorLayout.opacitySeekbar.onProgressChangedListener =
                object : OnProgressChangedListener {
                    override fun onProgressChanged(
                        bubbleSeekBar: BubbleSeekBar?,
                        progress: Int,
                        progressFloat: Float
                    ) {
                        try {
                            val sticker = stickerView?.currentSticker
                            if (sticker is TextStickerCustom) {
                                if (StickerView.isSelected == true) {
                                    if (isTextColor == true) {
                                        sticker.setAlpha(progress)
                                    } else if (isBGColor == true) {
                                        if (!isGradient) {
                                            sticker.drawable =
                                                getscaleDrawable(BgColor, progress, activity)
                                        } else {
                                            sticker.drawable =
                                                getGradientScaleDrawable(
                                                    BgGradient,
                                                    progress,
                                                    activity
                                                )
                                        }
                                    }
                                    stickerView?.invalidate()
                                }
                            }
                        } catch (e: Exception) {

                        }
                    }

                    override fun getProgressOnActionUp(
                        bubbleSeekBar: BubbleSeekBar,
                        progress: Int,
                        progressFloat: Float
                    ) {
                    }

                    override fun getProgressOnFinally(
                        bubbleSeekBar: BubbleSeekBar?,
                        progress: Int,
                        progressFloat: Float
                    ) {

                    }
                }
        } catch (e: Exception) {

        }
    }

    private fun TextShadowChange() {
        //define shadowOffset
        binding.textColorLayout.shadowOffsetSeek.configBuilder
            .min(-12.0f)
            .max(12.0f)
            .progress(0.0f)
            .build()
        binding.textColorLayout.shadowOffsetSeek.onProgressChangedListener =
            object : OnProgressChangedListener {
                override fun onProgressChanged(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {
                    if (progressFloat >= 0.0) {
                        text_shadowDx = progressFloat
                        text_shadowDy = 0.0f
                    } else {
                        text_shadowDx = 0.0f
                        text_shadowDy = progressFloat
                    }
                    val sticker = stickerView?.currentSticker
                    if (sticker is TextStickerCustom) {
                        if (StickerView.isSelected == true) {
                            sticker.setShadowLayer(
                                text_shadowRadius,
                                text_shadowDx,
                                text_shadowDy,
                                text_shadowColor
                            )
                            //  updateSticker();
                            stickerView?.invalidate()
                        } else {
                            mActivity?.let {
                                showToast(it, getStrings(R.string.select_object, it))
                            }
                        }
                    }
                }

                override fun getProgressOnActionUp(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {

                }

                override fun getProgressOnFinally(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {

                }

            }
    }

    private fun textAlign_seeks() {
        //define shadowOffset
        binding.textAlignLayout.textSizeSeek.onProgressChangedListener =
            object : OnProgressChangedListener {
                override fun onProgressChanged(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {
                    val sticker = stickerView?.currentSticker
                    if (sticker is TextStickerCustom) {
                        if (StickerView.isSelected == true) {
                            sticker.setMaxTextSize(progressFloat)
                            sticker.setMinTextSize(progressFloat)
                            //  updateSticker();
                            sticker.resizeText()
                            stickerView?.invalidate()
                        } else {
                            mActivity?.let {
                                showToast(it, getStrings(R.string.select_object, it))
                            }
                        }
                    }
                }

                override fun getProgressOnActionUp(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {

                }

                override fun getProgressOnFinally(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {

                }

            }
        binding.textAlignLayout.letterSpaceSeek.onProgressChangedListener =
            object : OnProgressChangedListener {
                override fun onProgressChanged(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {
                    val sticker = stickerView?.currentSticker
                    if (sticker is TextStickerCustom) {
                        if (StickerView.isSelected == true) {
                            try {
                                sticker.setLetterSpacing(progressFloat)
                            } catch (e: Exception) {
                            }
                            //  updateSticker();
                            sticker.resizeText()
                            stickerView?.invalidate()
                        } else {
                            mActivity?.let {
                                showToast(it, getStrings(R.string.select_object, it))
                            }
                        }
                    }
                }

                override fun getProgressOnActionUp(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {

                }

                override fun getProgressOnFinally(
                    bubbleSeekBar: BubbleSeekBar?,
                    progress: Int,
                    progressFloat: Float
                ) {

                }

            }
    }

    private fun colorAdpaterSet(activity: FragmentActivity) {
        binding.textColorLayout.colorsRecycler.setHasFixedSize(true)
        solidColorsList = resources.getIntArray(R.array.listcolors)
        colorsAdapter = ColorsAdapter(
            activity,
            solidColorsList, DrawableClass.gradientColors, isGradient, textItemListener
        )
        binding.textColorLayout.colorsRecycler.adapter = colorsAdapter
    }

    private fun colorTypeAdpaterSet(activity: FragmentActivity) {
        binding.textColorLayout.colorTypeRecycler.setHasFixedSize(true)
        mActivity?.let {
            val colorTypeTexts = arrayOf(it.resources.getString(R.string.solid_color), it.resources.getString(R.string.gradient))
            colorTypeAdapter = ColorTypeAdapter(
                colorTypeTexts, activity, textItemListener
            )
            binding.textColorLayout.colorTypeRecycler.adapter = colorTypeAdapter
        }
    }

    private fun updateFont(activity: FragmentActivity, sticker: Sticker?, position: Int) {
        try {
            if (sticker is TextStickerCustom) {
                if (StickerView.isSelected == true) {
                    sticker.setTypeface(typefaces[position])
                    sticker.resizeText()
                    stickerView?.invalidate()
                } else {
                    showToast(activity, getStrings(R.string.select_object, activity))
                }
            }
        } catch (e: Exception) {

        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }
}

internal class FontsAdapter(
    context: Context,
    typefaces: ArrayList<Typeface?>,
    listener: TextItemListener?
) : RecyclerView.Adapter<FontsAdapter.MyViewHolder>() {
    var context: Context
    var listener: TextItemListener?
    var typefaces = ArrayList<Typeface?>()

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class MyViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        // each data item is just a string in this case
        var text: TextView

        init {
            text = v.findViewById(R.id.text)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            listener?.onFontItemClick(adapterPosition)
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    init {
        this.typefaces = typefaces
        this.context = context
        this.listener = listener
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_font, parent, false) as View
        return MyViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.text.typeface = typefaces[position]
        holder.text.text = "ABC"
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return typefaces.size
    }
}

internal class ColorsAdapter    // Provide a suitable constructor (depends on the kind of dataset)
    (
    var context: Context,
    private val colors: IntArray,
    private val gradientList: Array<Array<String>>,
    var isGradient: Boolean,
    var listener: TextItemListener?
) : RecyclerView.Adapter<ColorsAdapter.MyViewHolder>() {
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder

    private var selectedItem = -1

    inner class MyViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        // each data item is just a string in this case
        var image: ImageView
        var ll: LinearLayout

        init {
            image = v.findViewById<ImageView>(R.id.imageColor)
            ll = v.findViewById(R.id.ll)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val previousItem = selectedItem
            selectedItem = bindingAdapterPosition

            notifyItemChanged(previousItem)
            notifyItemChanged(selectedItem)
            listener?.onColorItemClick(adapterPosition)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_text, parent, false) as View
        return MyViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (position == 0) {
            holder.ll.setCustomMargins(45, 0, 0, 0)
            holder.image.background = context.resources.getDrawable(R.drawable.transparency_icon)
        } else {
            holder.ll.setCustomMargins(20, 0, 0, 0)
            if (isGradient == false) {
                holder.image.setBackgroundColor(colors[position])
            } else if (isGradient == true) {
                holder.image.background = getGradientScaleDrawable(gradientList[position])
            }
        }

        if (selectedItem == position) {
            holder.itemView.setBackgroundResource(R.drawable.bg_selected_card)
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        var length = 0
        if (isGradient == false) {
            length = colors.size
        } else if (isGradient == true) {
            length = gradientList.size
        }
        return length
    }

    private fun getGradientScaleDrawable(colors: Array<String>): Drawable {
        val colorList = IntArray(colors.size)
        for (i in colors.indices) {
            var color: Int
            color = Color.parseColor(colors[i])
            colorList[i] = color
        }
        val drawable = GradientDrawable()
        drawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        drawable.colors = colorList
        drawable.shape = GradientDrawable.RECTANGLE
        return drawable
    }
}

internal class ColorTypeAdapter    // Provide a suitable constructor (depends on the kind of dataset)
    (private val texts: Array<String>, var context: Context, var listener: TextItemListener?) :
    RecyclerView.Adapter<ColorTypeAdapter.MyViewHolder>() {
    var selectedItem = 0

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class MyViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        // each data item is just a string in this case
        var text: TextView

        init {
            text = v.findViewById<TextView>(R.id.highlighter)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            listener?.onColorTypeClick(adapterPosition)
            if (selectedItem == adapterPosition) {
                selectedItem = 0
                notifyDataSetChanged()
                return
            }
            selectedItem = adapterPosition
            notifyDataSetChanged()
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_type, parent, false) as View
        return MyViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        if (selectedItem == position) {
            // do your stuff here like
            //Change selected item background color and Show sub item views
//            holder.itemView.setAlpha(Constants.buttonPress);
            holder.text.setBackgroundColor(context.resources.getColor(R.color.colorHighlightBlueDark))
        } else {
//            holder.itemView.setAlpha(Constants.buttonUnpress);
            holder.text.setBackgroundColor(context.resources.getColor(R.color.text_secondary))
            // do your stuff here like
            //Change  unselected item background color and Hide sub item views
        }
        holder.text.text = texts[position]
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return texts.size
    }
}