package com.aiface.aging.features.imgpicker.builder

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.aiface.aging.features.imgpicker.builder.listener.ImageSelectCancelListener
import com.aiface.aging.features.imgpicker.builder.listener.OnErrorListener
import com.aiface.aging.features.imgpicker.builder.listener.OnMultiSelectedListener
import com.aiface.aging.features.imgpicker.builder.listener.OnSelectedListener
import com.aiface.aging.features.imgpicker.builder.type.SelectType
import java.lang.ref.WeakReference


class TedImagePicker {
    companion object {
        @JvmStatic
        fun with(context: Context, editorName: String) =
            Builder(WeakReference(context), editorName)
    }

    @SuppressLint("ParcelCreator")
    class Builder(
        private val contextWeakReference: WeakReference<Context>, private val editorName: String
    ) :
        TedImagePickerBaseBuilder<Builder>() {


        fun errorListener(onErrorListener: OnErrorListener): Builder {
            this.onErrorListener = onErrorListener
            return this
        }

        fun errorListener(action: (Throwable) -> Unit): Builder {
            this.onErrorListener = object : OnErrorListener {
                override fun onError(throwable: Throwable) {
                    action(throwable)
                }
            }
            return this
        }

        private fun cancelListener(imageSelectCancelListener: ImageSelectCancelListener): Builder {
            this.imageSelectCancelListener = imageSelectCancelListener
            return this
        }

        fun cancelListener(action: () -> Unit): Builder =
            cancelListener(object : ImageSelectCancelListener {
                override fun onImageSelectCancel() {
                    action.invoke()
                }
            })


        fun start(onSelectedListener: OnSelectedListener) {
            this.onSelectedListener = onSelectedListener
            selectType = SelectType.SINGLE
            contextWeakReference.get()?.let {
                startInternal(this, it, true, editorName)
            }

        }

        fun start(action: (Uri) -> Unit) {
            start(object : OnSelectedListener {
                override fun onSelected(uri: Uri) {
                    action(uri)
                }
            })
        }

        fun startMultiImage(action: (List<Uri>) -> Unit) {
            startMultiImage(object : OnMultiSelectedListener {
                override fun onSelected(uriList: List<Uri>) {
                    action(uriList)
                }
            })
        }

        private fun startMultiImage(onMultiSelectedListener: OnMultiSelectedListener) {
            this.onMultiSelectedListener = onMultiSelectedListener
            selectType = SelectType.MULTI
            contextWeakReference.get()?.let {
                startInternal(this, it, true, editorName)
            }
        }

        fun startMultiImageFragment() {
            selectType = SelectType.MULTI
            contextWeakReference.get()?.let {
                startInternal(this, it, false, editorName)
            }
        }

        fun initBuilder() {
          //  initBuilderNow(this)
        }

    }


}


