package com.example.wisatajogja.ui.common

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.wisatajogja.R
import com.example.wisatajogja.util.PhotoUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class PhotoPickerDelegate(
    caller: ActivityResultCaller,
    private val context: Context,
    private val onPicked: (Uri) -> Unit,
    private val onError: (String) -> Unit,
    private val dialogTitleRes: Int = R.string.profile_photo_options,
) {
    private var pendingCameraFile: File? = null

    private val pickFromGallery = caller.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let(onPicked) }

    private val pickFromFiles = caller.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let(onPicked) }

    private val takePicture = caller.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null && file.exists()) {
            onPicked(PhotoUtils.uriForFile(context, file))
        } else {
            file?.delete()
        }
    }

    private val requestCameraPermission = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else onError(context.getString(R.string.profile_photo_permission))
    }

    fun showSourceDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle(dialogTitleRes)
            .setItems(
                arrayOf(
                    context.getString(R.string.profile_photo_camera),
                    context.getString(R.string.profile_photo_gallery),
                    context.getString(R.string.profile_photo_file),
                )
            ) { _, which ->
                when (which) {
                    0 -> requestCamera()
                    1 -> openGallery()
                    2 -> pickFromFiles.launch(arrayOf("image/*"))
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun requestCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        runCatching {
            val file = PhotoUtils.createCameraFile(context)
            pendingCameraFile = file
            takePicture.launch(PhotoUtils.uriForFile(context, file))
        }.onFailure {
            onError(context.getString(R.string.profile_photo_failed))
        }
    }

    private fun openGallery() {
        runCatching {
            pickFromGallery.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }.onFailure { error ->
            onError(if (error is ActivityNotFoundException) context.getString(R.string.profile_photo_failed) else error.message.orEmpty())
        }
    }
}
