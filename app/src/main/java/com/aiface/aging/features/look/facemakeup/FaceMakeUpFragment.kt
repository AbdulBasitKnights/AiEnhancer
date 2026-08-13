package com.aiface.aging.features.look.facemakeup

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.aiface.aging.databinding.FragmentFaceMakeUpBinding
import com.aiface.aging.features.look.haircolor.HairEditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue
import android.graphics.Color
import android.util.Log
import android.widget.ImageView
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.features.look.domain.UserPreferencesUseCase
import com.aiface.aging.features.look.facemakeup.OverlayView.MakeupType
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aiface.aging.features.look.LookFeatureAds
import com.aiface.aging.shared.safeFinish
import com.aiface.aging.shared.safeNavigate
import com.aiface.aging.shared.safePopBackStack
import kotlinx.coroutines.flow.first
import yuku.ambilwarna.AmbilWarnaDialog
import javax.inject.Inject

@AndroidEntryPoint
class FaceMakeUpFragment : Fragment() {

    private var mActivity: FragmentActivity? = null

    private var _binding : FragmentFaceMakeUpBinding? =null

    private val mViewModel : FaceMakeUpModel by viewModels()

    private val hairEditorViewModel : HairEditorViewModel by activityViewModels()

    @Inject
    lateinit var userPreferencesUseCase: UserPreferencesUseCase

    private var selectedColor: Int = 0xFFFF0000.toInt() // Pure Red (#FF0000)

    private val binding get() = _binding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity =  requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFaceMakeUpBinding.inflate(inflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                systemBars.top,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
        try {
            mActivity?.let { mViewModel.loadModel(it) }
        }catch (e:Exception){
            Log.d("ExceptionD","$e")
        }
        setupClickListeners()
        setupLongPressPreview()
    }

    private fun setupClickListeners() {

        binding?.imageEditorView?.setImageBitmap(hairEditorViewModel.cropedImage)

        hairEditorViewModel.cropedImage?.let {
            binding?.let { it1 -> mViewModel.runFaceLandmark(it,it1)
            }
        }

        binding?.btnBack?.setOnClickListener {
            handleBackNavigation()
               }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            handleBackNavigation()
        }

        binding?.btnUndo?.setOnClickListener {

        }

        binding?.btnRedo?.setOnClickListener {

        }

        binding?.btnSave?.setOnClickListener {
            // Get the current bitmap from the image editor view
            val currentBitmap = binding?.imageEditorView?.drawable?.toBitmap()
            hairEditorViewModel.finalBitmap = currentBitmap
//            AppUtils.firebaseUserAction("makeup_res_save_click", "FaceMakeUpScreen")
            mActivity?.let { activity ->
                LookFeatureAds.showAdThenNavigate(activity) { navigateToResultScreen() }
            }
        }

        // Bottom makeup options
        binding?.optionLipstick?.setOnClickListener {
//            AppUtils.firebaseUserAction("makeup_editor_lips_click", "FaceMakeUpScreen")
            showColorPickerBottomSheet(MakeupType.LIPSTICK) { selectedColorString, selectedOpacity ->
                mViewModel.makeupState[MakeupType.LIPSTICK] = selectedColorString.toColorInt()
                hairEditorViewModel.cropedImage?.let { bitmap ->
                    binding?.let { mViewModel.runFaceLandmark(bitmap, it) }
                }
            }
        }

        binding?.optionCheeks?.setOnClickListener {
//            AppUtils.firebaseUserAction("makeup_editor_cheeks_click", "FaceMakeUpScreen")
            showColorPickerBottomSheet(MakeupType.BLUSH) { selectedColorString, selectedOpacity ->
                mViewModel.makeupState[MakeupType.BLUSH] = selectedColorString.toColorInt()
                hairEditorViewModel.cropedImage?.let { bitmap ->
                    binding?.let { mViewModel.runFaceLandmark(bitmap, it) }
                }
            }
        }

        binding?.optionEyes?.setOnClickListener {
//            AppUtils.firebaseUserAction("makeup_editor_eye_click", "FaceMakeUpScreen")
            showColorPickerBottomSheet(MakeupType.EYEBROW) { selectedColorString, selectedOpacity ->
                mViewModel.makeupState[MakeupType.EYEBROW] = selectedColorString.toColorInt()
                hairEditorViewModel.cropedImage?.let { bitmap ->
                    binding?.let { mViewModel.runFaceLandmark(bitmap, it) }
                }
            }
        }
    }

    private fun setupLongPressPreview() {
        binding?.imageEditorView?.setOnLongClickListener {
            // TODO: Show preview of original image
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showColorPickerBottomSheet(featureType: MakeupType, onColorSelected: (String, Int) -> Unit) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_color_picker, null)
        dialog.setContentView(view)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnConfirm = view.findViewById<ImageView>(R.id.btnConfirm)
        val btnDone = view.findViewById<Button>(R.id.btnDone)
        val seekOpacity = view.findViewById<SeekBar>(R.id.seekOpacity)
        val tvOpacityValue = view.findViewById<TextView>(R.id.tvOpacityValue)
        val colorRecyclerView = view.findViewById<RecyclerView>(R.id.colorRecyclerView)
        val colorWheelContainer = view.findViewById<LinearLayout>(R.id.colorWheelContainer)
        
        // Color options
        val colors = listOf("#FFFFFF", "#FF0000", "#FF6B6B", "#FFA500", "#FFFF00", "#FFD700", "#FFC107", "#FF9800")
        var selectedColor = Color.parseColor(colors[1])
        var selectedOpacity = 30

        // Setup RecyclerView
        colorRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val colorAdapter = ColorAdapter(
            colors = colors,
            onColorSelected = { colorString ->
                selectedColor = Color.parseColor(colorString)
                // Apply makeup immediately when color is selected
                Log.d("onProgressChanged","onColorSelected=$selectedOpacity")
                val alpha = (selectedOpacity * 255) / 100 // convert % to 0–255
                val colorWithAlpha = (selectedColor and 0x00FFFFFF) or (alpha shl 24)
                mViewModel.makeupState[featureType] = colorWithAlpha
                hairEditorViewModel.cropedImage?.let { bitmap ->
                    binding?.let { mViewModel.runFaceLandmark(bitmap, it) }
                }
            },
            onColorPickerClicked = {
                val colorPicker = AmbilWarnaDialog(mActivity, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        selectedColor = color
                        // Apply makeup immediately when custom color is selected
                        Log.d("onProgressChanged","onColorPickerClicked=$selectedOpacity")
                        val alpha = (selectedOpacity * 255) / 100 // convert % to 0–255
                        val colorWithAlpha = (selectedColor and 0x00FFFFFF) or (alpha shl 24)
                        mViewModel.makeupState[featureType] = colorWithAlpha
                        hairEditorViewModel.cropedImage?.let { bitmap ->
                            binding?.let { mViewModel.runFaceLandmark(bitmap, it) }
                        }
                    }
                    override fun onCancel(dialog: AmbilWarnaDialog?) {}
                })
                colorPicker.show()
            }
        )
        colorRecyclerView.adapter = colorAdapter
        // Load last selected color and opacity from DataStore and update UI
        lifecycleScope.launch {
            val lastColor = when (featureType) {
                MakeupType.LIPSTICK -> userPreferencesUseCase.getLipstickColor().first()
                MakeupType.BLUSH -> userPreferencesUseCase.getBlushColor().first()
                MakeupType.EYESHADOW -> userPreferencesUseCase.getEyeshadowColor().first()
                MakeupType.EYEBROW -> userPreferencesUseCase.getEyebrowColor().first()
            }
            val lastOpacity = when (featureType) {
                MakeupType.LIPSTICK -> userPreferencesUseCase.getLipstickOpacity().first()
                MakeupType.BLUSH -> userPreferencesUseCase.getBlushOpacity().first()
                MakeupType.EYESHADOW -> userPreferencesUseCase.getEyeshadowOpacity().first()
                MakeupType.EYEBROW -> userPreferencesUseCase.getEyebrowOpacity().first()
            }
            selectedColor = lastColor.toColorInt()
            selectedOpacity = lastOpacity
            // Update UI with loaded values
            seekOpacity.progress = selectedOpacity
            tvOpacityValue.text = selectedOpacity.toString()
            // Set selected color in adapter
            val colorString = String.format("#%06X", 0xFFFFFF and selectedColor)
            colorAdapter.setSelectedColor(colorString)
        }
        // Opacity slider
        seekOpacity.progress = selectedOpacity
        tvOpacityValue.text = selectedOpacity.toString()
        seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedOpacity = progress
                tvOpacityValue.text = progress.toString()
                // Apply makeup with new opacity in real-time
                if (fromUser) {
                    Log.d("onProgressChanged","onProgressChanged=$selectedOpacity")
                    val alpha = (selectedOpacity * 255) / 100 // convert % to 0–255
                    val colorWithAlpha = (selectedColor and 0x00FFFFFF) or (alpha shl 24)
                    mViewModel.makeupState[featureType] = colorWithAlpha
                    hairEditorViewModel.cropedImage?.let { bitmap ->
                        binding?.let { mViewModel.runFaceLandmark(bitmap, it) }
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Confirm/Done actions - only save preferences and dismiss
        val confirmAction = {
            val colorString = String.format("#%06X", 0xFFFFFF and selectedColor)
            // Save the selected color and opacity to DataStore
            lifecycleScope.launch {
                when (featureType) {
                    MakeupType.LIPSTICK -> {
                        userPreferencesUseCase.saveLipstickColor(colorString)
                        userPreferencesUseCase.saveLipstickOpacity(selectedOpacity)
                    }
                    MakeupType.BLUSH -> {
                        userPreferencesUseCase.saveBlushColor(colorString)
                        userPreferencesUseCase.saveBlushOpacity(selectedOpacity)
                    }
                    MakeupType.EYESHADOW -> {
                        userPreferencesUseCase.saveEyeshadowColor(colorString)
                        userPreferencesUseCase.saveEyeshadowOpacity(selectedOpacity)
                    }
                    MakeupType.EYEBROW -> {
                        userPreferencesUseCase.saveEyebrowColor(colorString)
                        userPreferencesUseCase.saveEyebrowOpacity(selectedOpacity)
                    }
                }
            }
            dialog.dismiss()
        }
        btnConfirm.setOnClickListener { confirmAction() }
        btnDone.setOnClickListener { confirmAction() }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // Extension function to convert color string to int
    private fun String.toColorInt(): Int {
        return Color.parseColor(this)
    }

    private fun navigateToResultScreen() {
        try {
            safeNavigate(
                FaceMakeUpFragmentDirections.actionFaceMakeUpFragmentToResultFragment(
                    sourceFeature = "face_makeup",
                ),
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleBackNavigation() {
        if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
        try {
            if (!safePopBackStack()) {
                requireActivity().safeFinish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}