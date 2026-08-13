package com.aiface.aging.features.blender

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.aiface.aging.R
import com.aiface.aging.shared.showExitEditingDialogue
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.SaveProgressHelper

var blendAlertDialog: AlertDialog? = null

fun Activity.checkBlendStoragePermission(): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

fun Activity.showBlendLoadingDialog() {
    try {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.layout_progress_dialog_loading)
        blendAlertDialog = builder.create()
        blendAlertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        blendAlertDialog?.show()
    } catch (_: Exception) {
    }
}

fun Activity.dismissBlendLoadingDialog() {
    blendAlertDialog?.dismiss()
}

fun Activity.showBlendProgressDialog() {
    SaveProgressHelper.show(this)
}

fun Activity.dismissBlendProgressDialog() {
    SaveProgressHelper.hide(this)
}

fun FragmentActivity.goBackWarningDialog() {
    val exitDialogue = DialogueUtils.getDialogue(this, R.layout.dialog_exit_editing)
    showExitEditingDialogue(exitDialogue) { finish() }
}

fun Activity.showDialogForDontAskAgain() {
    AlertDialog.Builder(this)
        .setTitle("Permission Alert")
        .setMessage("The permission has been blocked. You can unlock it in Settings.")
        .setPositiveButton("Go to Settings") { dialog, _ ->
            val intent = android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
            dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        .show()
}

fun Activity.assetsToDrawable(filePath: String): Drawable {
    val desiredPath = filePath.replace("file:///android_asset/", "")
    val inputStream = assets.open(desiredPath)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    return BitmapDrawable(resources, bitmap)
}

fun Activity.mirrorImage(originalBitmap: Bitmap): Bitmap {
    val matrix = Matrix()
    matrix.setScale(-1f, 1f)
    return Bitmap.createBitmap(
        originalBitmap,
        0,
        0,
        originalBitmap.width,
        originalBitmap.height,
        matrix,
        true
    )
}

fun Activity.sideblur(bit: Bitmap?, br: Int): Bitmap? {
    if (bit == null) return null
    return try {
        val sourceBitmap = if (bit.config == Bitmap.Config.HARDWARE) {
            bit.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bit
        } ?: return null
        val resultingImage = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultingImage)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.maskFilter = BlurMaskFilter(br.toFloat(), BlurMaskFilter.Blur.NORMAL)
        val path = Path()
        path.moveTo(br.toFloat(), br.toFloat())
        path.lineTo((canvas.width - br).toFloat(), br.toFloat())
        path.lineTo((canvas.width - br).toFloat(), (canvas.height - br).toFloat())
        path.lineTo(br.toFloat(), (canvas.height - br).toFloat())
        path.close()
        canvas.drawPath(path, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
        resultingImage
    } catch (_: OutOfMemoryError) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

fun AppCompatActivity.getThumbnail(uri: Uri): Bitmap? {
    return try {
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }
}

fun Context.isInternetConnected(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
