package com.yasinkaralurt.utils

import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference
import android.Manifest.permission.*
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class PermissionManager constructor(private val fragment: Fragment) {

    private var dontAskAgaionCheckDuration: Long = 500

    private var callback: (Boolean) -> Unit = {}
    private var permissions = mutableListOf<String>()
    private var dontAskAgain = false

    //dialog
    private var showDialog = true
    private var dialogTitle: String? = null
    private var dialogRationale: String? = null
    private var dialogApplyButton: String? = null

    private val requestMultiplePermissions =
        fragment.registerForActivityResult(RequestMultiplePermissions()) { results ->
            if (dontAskAgain) {
                openApplicationSettings()
            } else {
                handleResult(results.all { it.value })
            }
        }

    private val settingsResult =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            handleResult(isPermissionsGranted(permissions))
        }

    private fun openApplicationSettings() {
        fragment.requireActivity().let {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", it.packageName, null)
            intent.data = uri
            settingsResult.launch(intent)
        }
    }

    fun permissions(vararg permission: String): PermissionManager {
        permissions.clear()
        permissions.addAll(permission)
        return this
    }

    fun checkPermissions(callback: (Boolean) -> Unit) {
        this.callback = callback
        when {
            isPermissionsGranted(permissions) -> this.callback.invoke(true)
            showDialog -> displayRationale()
            else -> requestPermissions()
        }
    }

    private fun isPermissionsGranted(permissions: List<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(), it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun displayRationale() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(dialogTitle ?: fragment.getString(R.string.default_permission_dialog_title))
            .setMessage(
                dialogRationale ?: fragment.getString(R.string.default_permission_dialog_message)
            )
            .setCancelable(false)
            .setPositiveButton(
                dialogApplyButton
                    ?: fragment.getString(R.string.default_permission_dialog_apply_button_text)
            ) { _, _ ->
                requestPermissions()
            }
            .show()
    }

    private fun handleResult(result: Boolean) {
        callback.invoke(result)
        dialogApplyButton = null
        dialogRationale = null
        dialogTitle = null
        callback = {}
    }

    private fun requestPermissions() {
        dontAskAgain = true
        requestMultiplePermissions.launch(permissions.toTypedArray())
        Handler(Looper.getMainLooper()).postDelayed(
            {
                dontAskAgain = false
            }, dontAskAgaionCheckDuration
        )
    }

    fun setDoNotAskAgainCheckDuration(duration: Long): PermissionManager {
        this.dontAskAgaionCheckDuration = duration
        return this
    }

    //dialog functions

    fun showDialog(show: Boolean): PermissionManager {
        this.showDialog = show
        return this
    }

    fun setDialogTitle(title: String): PermissionManager {
        this.dialogTitle = title
        return this
    }

    fun setDialogRationale(rationale: String): PermissionManager {
        this.dialogRationale = rationale
        return this
    }

    fun setDialogApplyButtonText(text: String): PermissionManager {
        this.dialogApplyButton = text
        return this
    }

}