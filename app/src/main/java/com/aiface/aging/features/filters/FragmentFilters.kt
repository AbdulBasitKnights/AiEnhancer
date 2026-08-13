package com.aiface.aging.features.filters

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import com.aiface.aging.features.filters.model.ModelFilterPack
import com.snaptune.ai.photoeditor.collagemaker.presentation.fragments.filters.adapters.AdapterFilterPack
import com.snaptune.ai.photoeditor.collagemaker.presentation.fragments.filters.adapters.FilterPacksCallback
import com.aiface.aging.R
import com.aiface.aging.shared.hide
import com.aiface.aging.shared.safePopSupportBackStack
import com.aiface.aging.shared.show
import com.aiface.aging.data.LocalFiltersDataSource
import com.aiface.aging.databinding.FragmentFiltersBinding
import com.aiface.aging.features.editor.ViewModelEditorActivity
import com.aiface.aging.features.filters.adapters.AdapterFilterHeader
import com.aiface.aging.features.filters.adapters.FiltersCallback
import com.aiface.aging.features.filters.model.ModelFilters
import com.aiface.aging.utils.ToastUtils

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wysaid.common.SharedContext
import org.wysaid.nativePort.CGEImageHandler


interface FilterUpdateCallback {
    fun onFilterClick(position: Int, modelFilterPack: ModelFilterPack)
    fun onFilterDone()
    fun onFilterCancel()
}

@AndroidEntryPoint
class FragmentFilters : Fragment(), FiltersCallback, FilterPacksCallback {

    companion object {
        private var config: ModelFilters? = null
        private var filterUpdateCallback: FilterUpdateCallback? = null
        fun newInstance(
            config: ModelFilters,
            filterUpdateCallback: FilterUpdateCallback
        ): FragmentFilters {
            val fragment = FragmentFilters()
            Companion.config = config
            Companion.filterUpdateCallback = filterUpdateCallback
            return fragment
        }
    }


    private var mActivity: FragmentActivity? = null
    private lateinit var adapterPack: AdapterFilterPack
    private lateinit var adapterHeader: AdapterFilterHeader
    private val viewModel: ViewModelFilters by viewModels()
    private val viewModelEditor: ViewModelEditorActivity by lazy {
        ViewModelProvider(requireActivity())[ViewModelEditorActivity::class.java]
    }
    private lateinit var binding: FragmentFiltersBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFiltersBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            config?.let { config ->
                onclickListeners()
                binding.headingText.text = getString(R.string.basic)
                setUpRecyclerviewHeaders(binding, config, activity)
                setUpRecyclerviewPacks(binding, activity)
            }
        }
    }

    private fun onclickListeners() {
        binding.tick.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            try {
                filterUpdateCallback?.onFilterDone()
                // Host activities (Editor/BGRemover) close panel via callback; avoid double pop.
                if (!isHostManagedPanel()) {
                    mActivity?.safePopSupportBackStack()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.cross.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            try {
                filterUpdateCallback?.onFilterCancel()
                if (!isHostManagedPanel()) {
                    mActivity?.safePopSupportBackStack()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isHostManagedPanel(): Boolean {
        val host = mActivity ?: return false
        return host is com.aiface.aging.features.editor.EditorActivity ||
            host is com.aiface.aging.features.bgremover.BGRemoverActivity
    }

    private fun setUpRecyclerviewHeaders(
        binding: FragmentFiltersBinding,
        modelFilters: ModelFilters,
        activity: FragmentActivity
    ) {
        adapterHeader = AdapterFilterHeader(activity, this)
        binding.headersRecycler.apply {
            adapter = adapterHeader
            itemAnimator = DefaultItemAnimator()
        }
        subscribeUi(adapterHeader, modelFilters.parent, activity)
    }

    private fun subscribeUi(adapter: AdapterFilterHeader, stickerType: String?, activity: FragmentActivity) {
        lifecycleScope.launchWhenStarted {
            val filterDataSource=  LocalFiltersDataSource(activity)
            viewModel.getFilterHeaders(stickerType!!, filterDataSource)
            viewModel.filterHeaders.observe(viewLifecycleOwner, Observer{
                if (it != null){
                    adapter.submitList(it)
                    it.firstOrNull()?.id?.let { id ->
                        mActivity?.let { fragmentActivity ->
                            subscribePacksUi(
                                id,
                                fragmentActivity
                            )
                        }
                    }


                }
            })


        }
    }

    override fun onFilterCtgClick(position: Int, modelFilters: ModelFilters) {
        mActivity?.let { activity ->
            if (adapterHeader.getSelectedPosition() != position) {
                binding.headingText.text = modelFilters.title
                try {
                    adapterHeader.selectMode(position)
                    setUpRecyclerviewPacks(binding, activity)
                    subscribePacksUi(modelFilters.id, activity)
                } catch (e: Exception) {
                    ToastUtils.showErrorToast(activity)
                }
            }


        }
    }

    private fun setUpRecyclerviewPacks(
        binding: FragmentFiltersBinding,
        activity: FragmentActivity
    ) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    adapterPack = AdapterFilterPack(this@FragmentFilters, activity)
                    binding.packsRecycler.apply {
                        adapter = adapterPack
                        itemAnimator = null
                    }
                }
            }
        } catch (e: Exception) {
            ToastUtils.showErrorToast(activity)
        }
    }

    private fun subscribePacksUi(
        id: Int,
        activity: FragmentActivity
    ) {
        lifecycleScope.launchWhenStarted {
            val filterDataSource=  LocalFiltersDataSource(activity)
            viewModel.getFilterPacks(id, filterDataSource)
            viewModel.filterPacks.observe(viewLifecycleOwner, Observer{


                if (it?.isNotEmpty() == true) {
                    binding.textLoading.show()
                    applyAllFiltersAndGetImagesAsync(it) { filteredImages ->
                        if (filteredImages.size == it.size) {
                            val updatedFilterPacks = it.mapIndexed { index, modelFilterPack ->
                                modelFilterPack.copy(bitmap = filteredImages[index])
                            }
                            binding.textLoading.hide()
                            binding.textNoInternet.hide()
                            adapterPack.submitList(updatedFilterPacks)
                        }
                    }

                } else {
//                    if (NetworkUtils.isOnline(activity)) {
//                        binding.textNoInternet.hide()
//                        binding.textLoading.show()
//                    } else {
//                        binding.textLoading.hide()
//                        binding.textNoInternet.show()
//                    }
                }




            })
//            viewModel.filterPackState.collectLatest {
//                if (it.filterPack?.isNotEmpty() == true) {
//                    binding.textLoading.show()
//                    applyAllFiltersAndGetImagesAsync(it.filterPack) { filteredImages ->
//                        if (filteredImages.size == it.filterPack.size) {
//                            val updatedFilterPacks = it.filterPack.mapIndexed { index, modelFilterPack ->
//                                modelFilterPack.copy(bitmap = filteredImages[index])
//                            }
//                            binding.textLoading.hide()
//                            binding.textNoInternet.hide()
//                            adapterPack.submitList(updatedFilterPacks)
//                        }
//                    }
//
//                } else {
//                    if (NetworkUtils.isOnline(activity)) {
//                        binding.textNoInternet.hide()
//                        binding.textLoading.show()
//                    } else {
//                        binding.textLoading.hide()
//                        binding.textNoInternet.show()
//                    }
//                }
//            }

        }
    }

    private fun applyAllFiltersAndGetImagesAsync(
        filterPacks: List<ModelFilterPack>,
        callback: (List<Bitmap>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val filteredImages = mutableListOf<Bitmap>()

            viewModelEditor.userImageBitmap?.let { originalBitmap ->
                filterPacks.forEach { modelFilterPack ->
                    val rule = modelFilterPack.rule
                    val intensity = modelFilterPack.intensity?.toFloatOrNull() ?: 0f

                    // Create a lightweight copy of the original bitmap
                    val userImageCopy = getScaledBitmap(originalBitmap, 1.0f / 8.0f)

                    val filteredBitmap = applyCustomFilter(userImageCopy, rule, intensity)
                    filteredBitmap?.let { filteredImages.add(it) }
                }
            }

            // Return the result to the UI thread
            withContext(Dispatchers.Main) {
                callback(filteredImages)
            }
        }
    }

    private fun getScaledBitmap(originalBitmap: Bitmap, scaleFactor: Float): Bitmap {
        val width = (originalBitmap.width * scaleFactor).toInt()
        val height = (originalBitmap.height * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(originalBitmap, width, height, true)
    }

    private fun applyCustomFilter(
        userImageBitmap: Bitmap,
        ruleString: String?,
        intensity: Float
    ): Bitmap? {
        val glContext = SharedContext.create()
        val handler = CGEImageHandler()

        return try {
            glContext.makeCurrent()

            // Apply the filter directly to the input bitmap to save memory.
            handler.initWithBitmap(userImageBitmap)
            handler.setFilterWithConfig(ruleString)
            handler.setFilterIntensity(intensity)
            handler.processFilters()

            // Reuse the input bitmap for the result to save memory.
            handler.resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                handler.release() // Release the handler resources if there's a release method
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                glContext.release() // Always release the GL context
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onFilterClick(position: Int, modelFilterPack: ModelFilterPack) {
        adapterPack.selectMode(position)
        filterUpdateCallback?.onFilterClick(position, modelFilterPack)
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