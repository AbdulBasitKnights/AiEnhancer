package com.aiface.aging.shared

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AppOpenManager
import com.aiface.aging.features.noti.ExitNotification.onPauseNotification
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.DialogUtils

fun FragmentActivity.changeStatusBarColor(
    @ColorRes colorRes: Int, darkIcons: Boolean = false
) {
    val window = window
    val color = ContextCompat.getColor(this, colorRes)
    window.statusBarColor = color
    window.navigationBarColor = color
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = darkIcons
        isAppearanceLightNavigationBars = darkIcons
    }
}

fun FragmentActivity.applyLightSystemBars() {
    changeStatusBarColor(R.color.bgColor, darkIcons = true)
}

 fun AppCompatActivity.applyWindowInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        insets
    }
}

/** Edge-to-edge: pad root for status/nav bars. */
fun View.applySystemBarInsets(
    applyTop: Boolean = true,
    applyBottom: Boolean = false,
    applyHorizontal: Boolean = false,
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(
            if (applyHorizontal) bars.left else v.paddingLeft,
            if (applyTop) bars.top else v.paddingTop,
            if (applyHorizontal) bars.right else v.paddingRight,
            if (applyBottom) bars.bottom else v.paddingBottom,
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun FragmentActivity.findAppNavController(): NavController {
    return try {
        findNavController(R.id.result_host_nav)
    } catch (_: Exception) {
        findNavController(R.id.nav_host_main)
    }
}

fun FragmentActivity.nextNavigateWithId(id: Int) {
    try {
        findAppNavController().navigate(id)
    } catch (e: Exception) {
        Log.e("TAG" , "nextNavigateTo: ")
    }
}

fun FragmentActivity.nextNavigateTo(navDirections: NavDirections) {
    try {
        findAppNavController().navigate(navDirections)
    } catch (e: Exception) {
        Log.e("TAG" , "nextNavigateTo: ")
    }
}

fun View.show() {
    if (!isVisible) visibility = View.VISIBLE
}

fun View.hide() {
    if (isVisible) visibility = View.GONE
}

fun View.invisible() {
    if (isVisible) visibility = View.INVISIBLE
}


fun Fragment.showResultDiscardDialog(onDiscard: () -> Unit) {
    val dialogue = DialogueUtils.getDialogue(requireContext(), R.layout.dialog_exit_editing)
    dialogue.findViewById<TextView>(R.id.titleText)?.text = getString(R.string.discard_changes)
    dialogue.findViewById<TextView>(R.id.inputEditText)?.text =
        getString(R.string.result_discard_image_message)
    dialogue.findViewById<TextView>(R.id.buttonCancel)?.apply {
        text = getString(R.string.cancel)
        setOnClickListener { dialogue.dismiss() }
    }
    dialogue.findViewById<TextView>(R.id.buttonDiscard)?.setOnClickListener {
        dialogue.dismiss()
        onDiscard()
    }
    if (isAdded) {
        dialogue.show()
    }
}

fun FragmentActivity.closeAppCompletely() {
    if (isFinishing) return
    finishAffinity()
    finish()
}

fun FragmentActivity.showAppExitFlow(
    onFlowStarted: () -> Unit = {},
    onFlowEnded: () -> Unit = {},
) {
    if (isFinishing || isDestroyed) return
    onFlowStarted()
    startActivity(
        Intent(this, com.aiface.aging.features.exit.ExitActivity::class.java)
    )
}

fun FragmentActivity.showExitEditingDialogue(dialogue: Dialog, actionListener: () -> Unit = {}) {
    if (isFinishing || isDestroyed) return
    if (dialogue.isShowing) return

    val cancel = dialogue.findViewById<TextView>(R.id.buttonCancel)
    val discard = dialogue.findViewById<TextView>(R.id.buttonDiscard)
    val flAdPlace = dialogue.findViewById<FrameLayout>(R.id.flAdplace)


    cancel.setOnClickListener {
        if (!isFinishing && !isDestroyed) {
            dialogue.dismiss()
        }
    }
//    save.setOnClickListener {
//        if (!isFinishing && !isDestroyed) {
//            dialogue.dismiss()
//            actionListener()
//        }
//    }

    discard.setOnClickListener {
        if (!isFinishing && !isDestroyed) {
            try {
                dialogue.dismiss()
                goHomeFresh()
            } catch (e: Exception) {
                e.printStackTrace()
                goHomeFresh()
            }
        }
    }

    if (!isFinishing && !isDestroyed) {
        dialogue.show()
    }
}

fun EditText.showSoftKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    post {
        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun FragmentActivity.hideNavigationBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.hide(android.view.WindowInsets.Type.navigationBars())
        window.insetsController?.systemBarsBehavior =
            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

    }
}

fun FragmentActivity.hideSystemUI() {
    hideNavigationBar()
}

fun NavController.safeNavigate(directions: NavDirections): Boolean {
    return try {
        if (currentDestination?.getAction(directions.actionId) == null) return false
        navigate(directions)
        true
    } catch (e: Exception) {
        Log.e("NavSafe", "safeNavigate directions failed", e)
        false
    }
}

fun NavController.safeNavigate(actionId: Int, args: Bundle? = null): Boolean {
    return try {
        if (currentDestination?.getAction(actionId) == null) {
            Log.w(
                "NavSafe",
                "skip action=$actionId current=${currentDestination?.id}",
            )
            return false
        }
        if (args != null) {
            navigate(actionId, args)
        } else {
            navigate(actionId)
        }
        true
    } catch (e: Exception) {
        Log.e("NavSafe", "safeNavigate action=$actionId failed", e)
        false
    }
}

fun Fragment.safeNavigate(actionId: Int, args: Bundle? = null): Boolean {
    if (!isAdded || view == null) return false
    if (Looper.myLooper() != Looper.getMainLooper()) {
        Handler(Looper.getMainLooper()).post {
            if (!isAdded || view == null) return@post
            try {
                findNavController().safeNavigate(actionId, args)
            } catch (e: Exception) {
                Log.e("NavSafe", "Fragment.safeNavigate action=$actionId failed (posted)", e)
            }
        }
        return true
    }
    return try {
        findNavController().safeNavigate(actionId, args)
    } catch (e: Exception) {
        Log.e("NavSafe", "Fragment.safeNavigate action=$actionId failed", e)
        false
    }
}

fun Fragment.safeNavigate(directions: NavDirections): Boolean {
    if (!isAdded || view == null) return false
    if (Looper.myLooper() != Looper.getMainLooper()) {
        Handler(Looper.getMainLooper()).post {
            if (!isAdded || view == null) return@post
            try {
                findNavController().safeNavigate(directions)
            } catch (e: Exception) {
                Log.e("NavSafe", "Fragment.safeNavigate directions failed (posted)", e)
            }
        }
        return true
    }
    return try {
        findNavController().safeNavigate(directions)
    } catch (e: Exception) {
        Log.e("NavSafe", "Fragment.safeNavigate directions failed", e)
        false
    }
}

fun NavController.safeNavigateUp(): Boolean {
    return try {
        navigateUp()
    } catch (e: Exception) {
        Log.e("NavSafe", "safeNavigateUp failed", e)
        false
    }
}

fun NavController.safePopBackStack(): Boolean {
    return try {
        popBackStack()
    } catch (e: Exception) {
        Log.e("NavSafe", "safePopBackStack failed", e)
        false
    }
}

fun Fragment.safeNavigateUp(): Boolean {
    if (!isAdded || view == null) return false
    return try {
        findNavController().safeNavigateUp()
    } catch (e: Exception) {
        Log.e("NavSafe", "Fragment.safeNavigateUp failed", e)
        false
    }
}

fun Fragment.safePopBackStack(): Boolean {
    if (!isAdded || view == null) return false
    return try {
        findNavController().safePopBackStack()
    } catch (e: Exception) {
        Log.e("NavSafe", "Fragment.safePopBackStack failed", e)
        false
    }
}

fun FragmentActivity.safePopSupportBackStack(): Boolean {
    return try {
        if (supportFragmentManager.isStateSaved) return false
        if (supportFragmentManager.backStackEntryCount <= 0) return false
        supportFragmentManager.popBackStack()
        true
    } catch (e: Exception) {
        Log.e("NavSafe", "safePopSupportBackStack failed", e)
        false
    }
}

fun Activity.safeFinish() {
    try {
        if (!isFinishing && !isDestroyed) {
            finish()
        }
    } catch (e: Exception) {
        Log.e("NavSafe", "safeFinish failed", e)
    }
}

/**
 * Fresh Home start — clears task so nav-graph back stack cannot crash.
 */
fun Context.goHomeFresh() {
    try {
        startActivity(
            Intent(this, com.aiface.aging.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
    } catch (e: Exception) {
        Log.e("NavSafe", "goHomeFresh failed", e)
    }
}

fun Activity.goHomeFresh() {
    (this as Context).goHomeFresh()
}

fun Fragment.goHomeFresh() {
    try {
        val ctx = context ?: return
        ctx.goHomeFresh()
    } catch (e: Exception) {
        Log.e("NavSafe", "Fragment.goHomeFresh failed", e)
    }
}

/**
 * Run back-press work once with debounce + exception swallow.
 * Prefer [BackPressGuard.begin]/[BackPressGuard.end] for animated panel dismiss.
 */
inline fun runSafeBack(
    debounce: Boolean = true,
    crossinline block: () -> Unit,
) {
    if (debounce && !BackPressGuard.tryHandle()) return
    try {
        block()
    } catch (e: Exception) {
        Log.e("NavSafe", "runSafeBack failed", e)
        BackPressGuard.end()
    }
}

fun Activity.privacyPolicy() {
    try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
             //   Uri.parse("https://signatureexp.co/privacy-policy.html")
                Uri.parse("https://tflsignatureapps.terafort.com/privacy-policy.html")
            )
        )
    } catch (e: Exception) {
    }
}

fun Activity.termsOfServices() {
    try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
              //  Uri.parse("https://signatureexp.co/terms-of-service.html")
                Uri.parse("https://www.terafort.com/T&C.html")
            )
        )
    } catch (e: Exception) {
    }
}

fun Activity.shareApp() {
    try {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            "Check out the App at: https://play.google.com/store/apps/details?id=$packageName"
        )
        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    }catch (e: Exception){

    }
}
fun Activity.rateUs() {
    try {
        AppOpenManager.disableAppOpen = true
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        )
        startActivity(intent)
    }catch (e: Exception){
    }
}

fun Activity.goUTM(pkgName : String) {
    try {
        AppOpenManager.disableAppOpen = true
        onPauseNotification = false
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$pkgName")
        )
        startActivity(intent)
    }catch (e: Exception){
    }
}

