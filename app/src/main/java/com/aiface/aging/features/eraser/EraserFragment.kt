package com.aiface.aging.features.eraser

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.databinding.ActivityEraserNewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.math.ln
import kotlin.math.pow

class EraserFragment : Fragment(){
    private var binding: ActivityEraserNewBinding? = null

    private var imagePath: String? = null
    private var intent: Intent? = null
    private var mContentResolver: ContentResolver? = null
    private var mBitmap: Bitmap? = null

    var mHoverView: HoverView? = null
    var mDensity: Double = 0.0

    var viewWidth: Int = 0
    var viewHeight: Int = 0
    var bmWidth: Int = 0
    var bmHeight: Int = 0

    var actionBarHeight: Int = 0
    var bottombarHeight: Int = 0
    var bmRatio: Double = 0.0
    var viewRatio: Double = 0.0

    var path = ""

    private var canvasReady = false

    private fun hoverView(): HoverView? = mHoverView?.takeIf { canvasReady && isAdded && view != null }

    private inline fun withHoverView(block: (HoverView) -> Unit) {
        val hover = hoverView() ?: return
        block(hover)
    }

    private fun setToolsEnabled(enabled: Boolean) {
        binding?.clTools?.isEnabled = enabled
        binding?.clTools?.alpha = if (enabled) 1f else 0.5f
        binding?.clToolbar?.isEnabled = enabled
    }

    companion object {
        private const val ARG_PATH = "path"

        fun newInstance(p: String): EraserFragment {
            val fragment = EraserFragment()
            val args = Bundle()
            args.putString(ARG_PATH, p)
            fragment.arguments = args
            return fragment
        }
    }



    interface OnFragmentInteractionListener {
        fun onErasedImage(path : String?)
    }
    private var listener: OnFragmentInteractionListener? = null

    private var mActivity: FragmentActivity? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=ActivityEraserNewBinding.inflate(inflater,container,false)
        // binding?.lifecycleOwner=this -- not a data-binding layout
        setToolsEnabled(false)
        clickListeners()

        try {
            mActivity?.let {activty->
                mContentResolver = activty.contentResolver

                if (activty.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(
                    activty, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
                )

                path = arguments?.getString(ARG_PATH) ?: ""
               // path = activty.getIntent()?.getStringExtra("path") ?: ""
             //   mBitmap = getBitmap(path)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val bitmap: Bitmap = Glide.with(activty)
                            .asBitmap()
                            .load(File(path))
                            .override(600,600)
                            .centerInside()
                            .submit()
                            .get()

                        // Use the bitmap on the main thread
                        withContext(Dispatchers.Main) {
                            // Handle the bitmap here
                            mBitmap=bitmap

                            mDensity = resources.displayMetrics.density.toDouble()
                            actionBarHeight = (110 * mDensity).toInt()
                            bottombarHeight = (60 * mDensity).toInt()

                            viewWidth = resources.displayMetrics.widthPixels
                            viewHeight = resources.displayMetrics.heightPixels - actionBarHeight - bottombarHeight
                            viewRatio = viewHeight.toDouble() / viewWidth.toDouble()

                            bmRatio = mBitmap!!.height.toDouble() / mBitmap!!.width.toDouble()
                            if (bmRatio < viewRatio) {
                                bmWidth = viewWidth
                                bmHeight =
                                    ((viewWidth.toDouble()) * (mBitmap!!.height.toDouble() / mBitmap!!.width.toDouble())).toInt()
                            } else {
                                bmHeight = viewHeight
                                bmWidth =
                                    ((viewHeight.toDouble()) * (mBitmap!!.width.toDouble() / mBitmap!!.height.toDouble())).toInt()
                            }
                            mBitmap = Bitmap.createScaledBitmap(mBitmap!!, bmWidth, bmHeight, false)

                            mHoverView = HoverView(activty, mBitmap, bmWidth, bmHeight, viewWidth, viewHeight,this@EraserFragment)
                            mHoverView!!.layoutParams = ViewGroup.LayoutParams(viewWidth, viewHeight)

                            binding?.mainLayout?.addView(mHoverView)

                            canvasReady = true
                            setToolsEnabled(true)
                            initButton()
                            setSelectedButton("zoom")
                        }
                    } catch (e : OutOfMemoryError){
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(activty, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                            listener?.onErasedImage(null)
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(activty, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                            listener?.onErasedImage(null)
                        }
                    }
                }



            }



        } catch (e: Exception) {
            e.printStackTrace()
        }

        return binding?.root
    }


    private fun clickListeners(){
        binding?.eraseButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                hover.switchMode(HoverView.ERASE_MODE)
                if (binding?.eraserLayout?.visibility == View.VISIBLE) {
                    binding?.eraserLayout?.visibility = View.GONE
                } else {
                    binding?.eraserLayout?.visibility = View.VISIBLE
                }
                binding?.magicWandLayout?.visibility = View.GONE
                resetMainButtonState()
                resetSubEraserButtonState()
                binding?.eraseSubButton?.setSelected(true)
                binding?.eraseButton?.setSelected(true)
                setSelectedButton("eraser")
            }
        }

        binding?.magicButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                hover.switchMode(HoverView.MAGIC_MODE)
                binding?.eraserLayout?.visibility = View.GONE
                resetMainButtonState()
                resetSubMagicButtonState()
                binding?.magicRemoveButton?.setSelected(true)
                binding?.magicButton?.setSelected(true)
                binding?.magicRemoveButton?.setVisibility(View.GONE)
                binding?.magicRestoreButton?.setVisibility(View.GONE)
                setSelectedButton("magic_eraser")
            }
        }
        binding?.ivMagicBrush?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                hover.switchMode(HoverView.MAGIC_MODE_RESTORE)
                binding?.eraserLayout?.visibility = View.GONE
                resetMainButtonState()
                resetSubMagicButtonState()
                binding?.magicRemoveButton?.setSelected(true)
                binding?.magicButton?.setSelected(true)
                binding?.magicRemoveButton?.setVisibility(View.GONE)
                binding?.magicRestoreButton?.setVisibility(View.GONE)
                setSelectedButton("magic_brush")
            }
        }
        binding?.positionButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                hover.switchMode(HoverView.MOVING_MODE)
                binding?.magicWandLayout?.visibility = View.GONE
                binding?.eraserLayout?.visibility = View.GONE
                resetMainButtonState()
                binding?.positionButton?.setSelected(true)
                setSelectedButton("zoom")
            }
        }
        binding?.eraseSubButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                hover.switchMode(HoverView.ERASE_MODE)
                resetSubEraserButtonState()
                binding?.eraseSubButton?.setSelected(true)
            }
        }
        binding?.uneraseSubButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                hover.switchMode(HoverView.UNERASE_MODE)
                resetSubEraserButtonState()
                binding?.uneraseSubButton?.setSelected(true)
            }
        }
        binding?.brushSize1Button?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                resetBrushButtonState()
                hover.setEraseOffset(40)
                binding?.brushSize1Button?.setSelected(true)
            }
        }
        binding?.brushSize2Button?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                resetBrushButtonState()
                hover.setEraseOffset(60)
                binding?.brushSize2Button?.setSelected(true)
            }
        }
        binding?.brushSize3Button?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                resetBrushButtonState()
                hover.setEraseOffset(80)
                binding?.brushSize3Button?.setSelected(true)
            }
        }
        binding?.brushSize4Button?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                resetBrushButtonState()
                hover.setEraseOffset(100)
                binding?.brushSize4Button?.setSelected(true)
            }
        }
        binding?.magicRemoveButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                resetSubMagicButtonState()
                binding?.magicRemoveButton?.setSelected(true)
                hover.switchMode(HoverView.MAGIC_MODE)
            }
        }
        binding?.magicRestoreButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                resetSubMagicButtonState()
                binding?.magicRestoreButton?.setSelected(true)
                hover.switchMode(HoverView.MAGIC_MODE_RESTORE)
            }
        }
        binding?.undoButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                binding?.eraserLayout?.visibility = View.GONE
                binding?.magicWandLayout?.visibility = View.GONE
                hover.undo()
                if (hover.checkUndoEnable()) {
                    binding?.undoButton?.setEnabled(true)
                    binding?.undoButton?.setAlpha(1.0f)
                } else {
                    binding?.undoButton?.setEnabled(false)
                    binding?.undoButton?.setAlpha(0.3f)
                }
                updateRedoButton()
            }
        }
        binding?.redoButton?.setOnClickListener {
            withHoverView { hover ->
                updateDoButtons()
                binding?.eraserLayout?.visibility = View.GONE
                binding?.magicWandLayout?.visibility = View.GONE
                hover.redo()
                updateUndoButton()
                updateRedoButton()
            }
        }
        binding?.back?.setOnClickListener {
            withHoverView { hover ->
                hover.switchMode(HoverView.MOVING_MODE)
                setSelectedButton("zoom")
            }
            listener?.onErasedImage(null)
        }
        binding?.btnSave?.setOnClickListener {
            mActivity?.let {activity->
                val save=hoverView()?.saveDrawnBitmap()
                lifecycleScope.launch {
                    val erasedBitmapPath = withContext(Dispatchers.IO) {
                        save?.let { it1 ->
                            AppUtils.convertBitmapToImagePath(
                                activity,
                                it1
                            )
                        }
                    }
                    withContext(Dispatchers.Main){
                        listener?.onErasedImage(erasedBitmapPath)
                    }
                }
            }
        }
    }

    private fun resetBrushButtonState() {
        binding?.brushSize1Button?.setSelected(false)
        binding?.brushSize2Button?.setSelected(false)
        binding?.brushSize3Button?.setSelected(false)
        binding?.brushSize4Button?.setSelected(false)
    }
    private fun resetSubMagicButtonState() {
        binding?.magicRemoveButton?.setSelected(false)
        binding?.magicRestoreButton?.setSelected(false)
    }
    private fun resetMainButtonState() {
        binding?.eraseButton?.setSelected(false)
        binding?.magicButton?.setSelected(false)
        binding?.mirrorButton?.setSelected(false)
        binding?.positionButton?.setSelected(false)
    }
    private fun resetSubEraserButtonState() {
        binding?.eraseSubButton?.setSelected(false)
        binding?.uneraseSubButton?.setSelected(false)
    }
    private fun updateDoButtons(){
        updateUndoButton()
        updateRedoButton()
    }
    private fun initButton(){
        updateRedoButton()
        binding?.eraseButton?.isSelected = true
    }

    private fun setSelectedButton(which: String) {
        when (which) {
            "zoom" -> {
                setButtonsColor(binding?.positionButton, binding?.tvZoom, R.color.colorHighlightBlueDark)
                setButtonsColor(binding?.magicButton, binding?.tvMagicEraser, R.color.white)
                setButtonsColor(binding?.ivMagicBrush, binding?.tvMagicBrush, R.color.white)
                setButtonsColor(binding?.eraseButton, binding?.tvEraser, R.color.white)
                hoverView()?.switchMode(HoverView.MOVING_MODE)
            }
            "magic_eraser" -> {
                setButtonsColor(binding?.positionButton, binding?.tvZoom, R.color.white)
                setButtonsColor(binding?.magicButton, binding?.tvMagicEraser, R.color.colorHighlightBlueDark)
                setButtonsColor(binding?.ivMagicBrush, binding?.tvMagicBrush, R.color.white)
                setButtonsColor(binding?.eraseButton, binding?.tvEraser, R.color.white)
            }
            "magic_brush" -> {
                setButtonsColor(binding?.positionButton, binding?.tvZoom, R.color.white)
                setButtonsColor(binding?.magicButton, binding?.tvMagicEraser, R.color.white)
                setButtonsColor(binding?.ivMagicBrush, binding?.tvMagicBrush, R.color.colorHighlightBlueDark)
                setButtonsColor(binding?.eraseButton, binding?.tvEraser, R.color.white)
            }
            "eraser" -> {
                setButtonsColor(binding?.positionButton, binding?.tvZoom, R.color.white)
                setButtonsColor(binding?.magicButton, binding?.tvMagicEraser, R.color.white)
                setButtonsColor(binding?.ivMagicBrush, binding?.tvMagicBrush, R.color.white)
                setButtonsColor(binding?.eraseButton, binding?.tvEraser, R.color.colorHighlightBlueDark)
            }
            else -> {
                setButtonsColor(binding?.positionButton, binding?.tvZoom, R.color.colorHighlightBlueDark)
                setButtonsColor(binding?.magicButton, binding?.tvMagicEraser, R.color.white)
                setButtonsColor(binding?.ivMagicBrush, binding?.tvMagicBrush, R.color.white)
                setButtonsColor(binding?.eraseButton, binding?.tvEraser, R.color.white)
            }
        }
    }
    private fun setButtonsColor(iv: ImageView?, tv: TextView?, color: Int) {
        iv?.setColorFilter(resources.getColor(color, null), PorterDuff.Mode.SRC_IN)
        tv?.setTextColor(resources.getColor(color, null))
    }


    private fun getBitmap(path: String): Bitmap? {
        val uri = getImageUri(path)
        var inputStream: InputStream?
        try {
            val IMAGE_MAX_SIZE = 1024
            inputStream = mContentResolver!!.openInputStream(uri)

            // Decode image size
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true

            BitmapFactory.decodeStream(inputStream, null, o)
            inputStream!!.close()

            var scale = 1
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = 2.0.pow(
                    Math.round(
                        ln(
                            IMAGE_MAX_SIZE / o.outHeight.coerceAtLeast(o.outWidth).toDouble()
                        ) / ln(0.5)
                    ).toInt().toDouble()
                ).toInt()
            }

            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = mContentResolver!!.openInputStream(uri)
            var b = BitmapFactory.decodeStream(inputStream, null, o2)
            inputStream!!.close()

            b = Bitmap.createBitmap(
                (b)!!, 0, 0, o2.outWidth, o2.outHeight, getOrientationMatrix(path), true
            )

            return b
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getOrientationMatrix(path: String): Matrix {
        val matrix = Matrix()
        val exif: ExifInterface
        try {
            exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.setRotate(180f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }


        return matrix
    }

    private fun getImageUri(path: String): Uri {
        return Uri.fromFile(File(path))
    }


    fun updateUndoButton() {
        val hover = hoverView() ?: return
        if (hover.checkUndoEnable()) {
            binding?.undoButton?.setEnabled(true)
            binding?.undoButton?.setAlpha(1.0f)
        } else {
            binding?.undoButton?.setEnabled(false)
            binding?.undoButton?.setAlpha(0.3f)
        }
    }

    fun updateRedoButton() {
        val hover = hoverView() ?: return
        if (hover.checkRedoEnable()) {
            binding?.redoButton?.setEnabled(true)
            binding?.redoButton?.setAlpha(1.0f)
        } else {
            binding?.redoButton?.setEnabled(false)
            binding?.redoButton?.setAlpha(0.3f)
        }
    }

    override fun onDestroyView() {
        canvasReady = false
        mHoverView = null
        binding = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
        listener = null
    }

}