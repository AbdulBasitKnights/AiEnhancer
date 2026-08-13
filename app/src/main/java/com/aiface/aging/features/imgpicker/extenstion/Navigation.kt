package com.aiface.aging.features.imgpicker.extenstion

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import com.aiface.aging.R

fun FragmentActivity.findAppNavController(): NavController {
    return try {
        findNavController(R.id.result_host_nav)
    } catch (_: Exception) {
        findNavController(R.id.nav_host_main)
    }
}

fun FragmentActivity.nextNavigateTo(navDirections: NavDirections) {
    try {
        findAppNavController().navigate(navDirections)
    } catch (e: Exception) {
        Log.e("TAG", "nextNavigateTo: ", e)
    }
}

fun FragmentActivity.nextNavigateWithId(id: Int) {
    try {
        findAppNavController().navigate(id)
    } catch (e: Exception) {
        Log.e("TAG", "nextNavigateWithId: ", e)
    }
}

fun FragmentActivity.nextNavigateWithIdBundle(id: Int, bundle: Bundle) {
    try {
        findAppNavController().navigate(id, bundle)
    } catch (e: Exception) {
        Log.e("TAG", "nextNavigateWithIdBundle: ", e)
    }
}

fun FragmentActivity.lookNextNavigateTo(navDirections: NavDirections) {
    try {
        findNavController(R.id.look_nav_host).navigate(navDirections)
    } catch (e: Exception) {
        Log.e("TAG", "lookNextNavigateTo: ", e)
    }
}

fun FragmentActivity.lookNextNavigateWithId(id: Int, bundle: Bundle? = null) {
    try {
        if (bundle != null) {
            findNavController(R.id.look_nav_host).navigate(id, bundle)
        } else {
            findNavController(R.id.look_nav_host).navigate(id)
        }
    } catch (e: Exception) {
        Log.e("TAG", "lookNextNavigateWithId: ", e)
    }
}

//fun FragmentActivity.nextNavigateWithIdBundleOnboard(id: Int, bundle: Bundle) {
//    try {
//        findNavController(R.id.nav_host_onboard).navigate(id, bundle)
//    } catch (e: Exception) {
//        Log.e("TAG" , "nextNavigateTo: ")
//    }
//}