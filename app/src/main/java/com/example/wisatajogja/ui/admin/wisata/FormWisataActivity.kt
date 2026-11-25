package com.example.wisatajogja.ui.admin.wisata

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.AdminRepository
import com.example.wisatajogja.data.repository.DestinationForm
import com.example.wisatajogja.data.repository.DestinationRepository
import com.example.wisatajogja.databinding.ActivityFormWisataBinding
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.ui.common.CropImageActivity
import com.example.wisatajogja.ui.common.PhotoPickerDelegate
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.loadPhoto
import com.example.wisatajogja.util.snack
import com.example.wisatajogja.util.toast
import kotlinx.coroutines.launch
import java.io.File

class FormWisataActivity : BaseActivity() {

    private lateinit var binding: ActivityFormWisataBinding
    private lateinit var photoPicker: PhotoPickerDelegate
    private lateinit var cropLauncher: ActivityResultLauncher<Intent>
    private val adminRepository = AdminRepository()
    private val destinationRepository = DestinationRepository()
    private var destinationId: Int = 0
    private var imageUrl: String = ""
    private var uploading = false
    private val categoryOptions = listOf("Pantai", "Budaya", "Alam")
    private lateinit var categoryAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormWisataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        destinationId = intent.getIntExtra(EXTRA_DESTINATION_ID, 0)
        cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val path = result.data?.getStringExtra(CropImageActivity.EXTRA_OUTPUT_PATH)
                if (!path.isNullOrBlank()) uploadImageFile(File(path))
            }
        }
        photoPicker = PhotoPickerDelegate(
            caller = this,
            context = this,
            onPicked = ::openCropEditor,
            onError = { binding.root.snack(it) },
            dialogTitleRes = R.string.form_image,
        )
        setupForm()
        if (destinationId > 0) loadDestination() else prefillDefaults()
    }

    private fun setupForm() = with(binding) {
        buttonBack.setOnClickListener { finish() }
        cardImage.setOnClickListener { photoPicker.showSourceDialog() }
        setupCategoryDropdown()
        buttonSave.setOnClickListener { save() }
    }

    private fun setupCategoryDropdown() = with(binding) {
        categoryAdapter = ArrayAdapter(
            this@FormWisataActivity,
            R.layout.item_dropdown,
            R.id.text1,
            categoryOptions,
        )
        inputCategory.setAdapter(categoryAdapter)
        inputCategory.threshold = 0
        inputCategory.setOnClickListener {
            binding.root.hideKeyboard()
            inputCategory.showDropDown()
        }
        inputCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) inputCategory.showDropDown()
        }
        inputCategory.setOnItemClickListener { _, _, position, _ ->
            inputCategory.setText(categoryOptions[position], false)
            inputCategoryLayout.error = null
        }
        inputCategoryLayout.setEndIconOnClickListener {
            binding.root.hideKeyboard()
            inputCategory.showDropDown()
        }
    }

    private fun prefillDefaults() = with(binding) {
        textScreenTitle.setText(R.string.form_add_title)
        inputCategory.setText("Alam", false)
        inputPrice.setText("0")
        inputHours.setText("07.00 - 17.00")
        inputLatitude.setText("-7.7956")
        inputLongitude.setText("110.3695")
        switchPublished.isChecked = false
    }

    private fun loadDestination() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching { destinationRepository.detail(id = destinationId) }
                .onSuccess { item ->
                    binding.textScreenTitle.setText(R.string.form_edit_title)
                    bindDestination(item)
                    binding.buttonSave.isEnabled = true
                    binding.buttonSave.text = getString(R.string.form_save)
                }
                .onFailure { error ->
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                    finish()
                }
        }
    }

    private fun bindDestination(item: Destination) = with(binding) {
        inputName.setText(item.name)
        inputCategory.setText(item.category, false)
        inputArea.setText(item.area)
        inputShort.setText(item.shortDescription)
        inputDescription.setText(item.description)
        inputPrice.setText(item.ticketPrice.toString())
        inputHours.setText(item.openHours)
        inputLatitude.setText(item.latitude.toString())
        inputLongitude.setText(item.longitude.toString())
        switchPublished.isChecked = item.status == "terbit"
        switchFeatured.isChecked = item.featured
        setImage(item.imageUrl.orEmpty())
    }

    private fun setImage(url: String) {
        imageUrl = url
        binding.imagePreview.loadPhoto(url)
        binding.imagePlaceholder.visibility = if (url.isBlank()) View.VISIBLE else View.GONE
    }

    private fun openCropEditor(uri: Uri) {
        if (uploading) return
        val intent = Intent(this, CropImageActivity::class.java)
            .putExtra(CropImageActivity.EXTRA_SOURCE_URI, uri.toString())
            .putExtra(CropImageActivity.EXTRA_ASPECT_RATIO, 16f / 9f)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        cropLauncher.launch(intent)
    }

    private fun uploadImageFile(file: File) {
        if (uploading) return
        uploading = true
        binding.root.snack(getString(R.string.form_uploading))
        lifecycleScope.launch {
            runCatching { adminRepository.uploadDestinationImage(file) }
                .onSuccess { url ->
                    uploading = false
                    file.delete()
                    setImage(url)
                }
                .onFailure { error ->
                    uploading = false
                    file.delete()
                    binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
                }
        }
    }

    private fun save() {
        with(binding) {
        val name = inputName.text?.toString()?.trim().orEmpty()
        val category = inputCategory.text?.toString()?.trim().orEmpty()
        val area = inputArea.text?.toString()?.trim().orEmpty()
        val short = inputShort.text?.toString()?.trim().orEmpty()
        val description = inputDescription.text?.toString()?.trim().orEmpty()
        val price = inputPrice.text?.toString()?.toIntOrNull() ?: 0
        val hours = inputHours.text?.toString()?.trim().orEmpty()
        val latitude = inputLatitude.text?.toString()?.toDoubleOrNull()
        val longitude = inputLongitude.text?.toString()?.toDoubleOrNull()

        var valid = true
        if (name.length < 3) { inputNameLayout.error = getString(R.string.error_name_short); valid = false } else inputNameLayout.error = null
        if (category !in categoryOptions) {
            inputCategoryLayout.error = "Pilih kategori destinasi."
            valid = false
        } else {
            inputCategoryLayout.error = null
        }
        if (area.isBlank()) { inputAreaLayout.error = getString(R.string.common_required); valid = false } else inputAreaLayout.error = null
        if (short.isBlank()) { inputShortLayout.error = getString(R.string.common_required); valid = false } else inputShortLayout.error = null
        if (description.isBlank()) { inputDescriptionLayout.error = getString(R.string.common_required); valid = false } else inputDescriptionLayout.error = null
        if (hours.isBlank()) { inputHoursLayout.error = getString(R.string.common_required); valid = false } else inputHoursLayout.error = null
        if (latitude == null) { inputLatitudeLayout.error = getString(R.string.common_required); valid = false } else inputLatitudeLayout.error = null
        if (longitude == null) { inputLongitudeLayout.error = getString(R.string.common_required); valid = false } else inputLongitudeLayout.error = null
        if (imageUrl.isBlank()) { binding.root.snack(getString(R.string.form_image_required)); valid = false }
        if (!valid) return

        binding.root.hideKeyboard()
        buttonSave.isEnabled = false
        buttonSave.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching {
                adminRepository.saveDestination(
                    DestinationForm(
                        id = destinationId,
                        name = name,
                        category = category,
                        area = area,
                        shortDescription = short,
                        description = description,
                        imageUrl = imageUrl,
                        ticketPrice = price,
                        openHours = hours,
                        latitude = latitude ?: 0.0,
                        longitude = longitude ?: 0.0,
                        published = switchPublished.isChecked,
                        featured = switchFeatured.isChecked,
                    )
                )
            }.onSuccess {
                toast(getString(R.string.form_success))
                setResult(RESULT_OK)
                finish()
            }.onFailure { error ->
                buttonSave.isEnabled = true
                buttonSave.text = getString(R.string.form_save)
                binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
            }
        }
        }
    }

    companion object {
        const val EXTRA_DESTINATION_ID = "extra_destination_id"
    }
}
