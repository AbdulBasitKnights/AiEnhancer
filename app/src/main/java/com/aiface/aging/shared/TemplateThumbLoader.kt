package com.aiface.aging.shared

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import com.aiface.aging.R
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Home / see-all template thumbs via Coil + OkHttp (CDN-safe).
 *
 * GIF: needs [coil-gif] decoders — plain Coil shows first frame only.
 * Flow: paint static immediately → load animated gif over it when ready.
 *
 * Logcat filter: [TemplateThumb]
 */
object TemplateThumbLoader {

    const val TAG = "TemplateThumb"

    private const val OVERRIDE_W = 480
    private const val OVERRIDE_H = 640
    /** Cap Coil RAM — unbounded GIF/static cache was a major OOM source on home. */
    private const val MEMORY_CACHE_PERCENT = 0.20

    @Volatile
    private var imageLoader: ImageLoader? = null

    private fun imageLoader(context: Context): ImageLoader {
        imageLoader?.let { return it }
        return synchronized(this) {
            imageLoader ?: ImageLoader.Builder(context.applicationContext)
                .okHttpClient { buildOkHttp() }
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(MEMORY_CACHE_PERCENT)
                        .build()
                }
                .components {
                    // Required for animated GIFs (otherwise first frame only).
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .crossfade(false)
                .respectCacheHeaders(false)
                .build()
                .also { imageLoader = it }
        }
    }

    /** Release drawable + stop GIF when list items recycle. */
    fun clear(imageView: ImageView) {
        try {
            (imageView.drawable as? Animatable)?.stop()
            imageView.setImageDrawable(null)
        } catch (t: Throwable) {
            Log.w(TAG, "clear failed", t)
        }
    }

    /** Drop hot template/GIF cache when system is under memory pressure. */
    fun trimMemory(context: Context) {
        try {
            imageLoader(context.applicationContext).memoryCache?.clear()
        } catch (t: Throwable) {
            Log.w(TAG, "trimMemory failed", t)
        }
    }

    private fun buildOkHttp(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                    )
                    .header("Accept", "image/gif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    private fun normalizeUrl(url: String?): String? =
        url?.trim()?.takeIf { it.isNotEmpty() && (it.startsWith("http://") || it.startsWith("https://")) }

    @JvmOverloads
    fun load(
        imageView: ImageView,
        thumbnailUrl: String?,
        mediaUrl: String? = null,
        context: FragmentActivity? = null,
        gifUrl: String? = null,
    ) {
        val staticPrimary = normalizeUrl(thumbnailUrl)
        val staticFallback = normalizeUrl(mediaUrl)?.takeIf { it != staticPrimary }
        val staticUrl = staticPrimary ?: staticFallback
        val gif = normalizeUrl(gifUrl)

        val loader = imageLoader(context ?: imageView.context)

        if (gif != null) {
            Log.d(TAG, "GIF path gif=$gif static=$staticUrl")
            if (staticUrl != null) {
                loadStaticThenGif(imageView, loader, staticUrl, staticFallback, gif)
            } else {
                loadAnimatedGif(imageView, loader, gif, placeholder = null)
            }
            return
        }

        val url = staticUrl
        if (url == null) {
            Log.e(TAG, "SKIP null/blank url")
            imageView.setImageResource(R.drawable.placeholder_icon)
            return
        }

        Log.d(TAG, "LOAD start url=$url")
        imageView.load(url, loader) {
            size(OVERRIDE_W, OVERRIDE_H)
            scale(Scale.FILL)
            placeholder(R.drawable.placeholder_icon)
            error(R.drawable.placeholder_icon)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
            listener(
                onSuccess = { _, result: SuccessResult ->
                    Log.d(TAG, "LOAD OK source=${result.dataSource} url=$url")
                },
                onError = { _, result: ErrorResult ->
                    Log.e(TAG, "LOAD FAIL url=$url", result.throwable)
                    if (staticPrimary != null && staticFallback != null) {
                        imageView.load(staticFallback, loader) {
                            size(OVERRIDE_W, OVERRIDE_H)
                            scale(Scale.FILL)
                            placeholder(R.drawable.placeholder_icon)
                            error(R.drawable.placeholder_icon)
                        }
                    }
                },
            )
        }
    }

    private fun loadStaticThenGif(
        imageView: ImageView,
        loader: ImageLoader,
        staticUrl: String,
        staticFallback: String?,
        gifUrl: String,
    ) {
        imageView.load(staticUrl, loader) {
            size(OVERRIDE_W, OVERRIDE_H)
            scale(Scale.FILL)
            placeholder(R.drawable.placeholder_icon)
            error(R.drawable.placeholder_icon)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
            listener(
                onSuccess = { _, _ ->
                    Log.d(TAG, "STATIC OK url=$staticUrl → gif")
                    loadAnimatedGif(imageView, loader, gifUrl, placeholder = imageView.drawable)
                },
                onError = { _, _ ->
                    Log.e(TAG, "STATIC FAIL url=$staticUrl")
                    if (staticFallback != null) {
                        imageView.load(staticFallback, loader) {
                            size(OVERRIDE_W, OVERRIDE_H)
                            scale(Scale.FILL)
                            placeholder(R.drawable.placeholder_icon)
                            error(R.drawable.placeholder_icon)
                            listener(
                                onSuccess = { _, _ ->
                                    loadAnimatedGif(imageView, loader, gifUrl, placeholder = imageView.drawable)
                                },
                                onError = { _, _ ->
                                    loadAnimatedGif(imageView, loader, gifUrl, placeholder = null)
                                },
                            )
                        }
                    } else {
                        loadAnimatedGif(imageView, loader, gifUrl, placeholder = null)
                    }
                },
            )
        }
    }

    /**
     * Animated GIF — decode at thumb size so full-res GIF frames do not fill the heap.
     */
    private fun loadAnimatedGif(
        imageView: ImageView,
        loader: ImageLoader,
        gifUrl: String,
        placeholder: Drawable?,
    ) {
        Log.d(TAG, "GIF LOAD start url=$gifUrl")
        val request = ImageRequest.Builder(imageView.context)
            .data(gifUrl)
            .target(imageView)
            .size(OVERRIDE_W, OVERRIDE_H)
            .precision(Precision.INEXACT)
            .scale(Scale.FILL)
            .allowHardware(false)
            .allowRgb565(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .apply {
                if (placeholder != null) {
                    placeholder(placeholder)
                    error(placeholder)
                } else {
                    placeholder(R.drawable.placeholder_icon)
                    error(R.drawable.placeholder_icon)
                }
            }
            .listener(
                onSuccess = { _, result: SuccessResult ->
                    Log.d(TAG, "GIF OK source=${result.dataSource} url=$gifUrl drawable=${result.drawable.javaClass.simpleName}")
                    val d = result.drawable
                    if (d is Animatable && !d.isRunning) {
                        d.start()
                    }
                },
                onError = { _, result: ErrorResult ->
                    Log.e(TAG, "GIF FAIL url=$gifUrl — keep static", result.throwable)
                },
            )
            .build()
        loader.enqueue(request)
    }
}
