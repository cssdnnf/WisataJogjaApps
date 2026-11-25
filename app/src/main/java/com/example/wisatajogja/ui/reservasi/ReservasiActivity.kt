package com.example.wisatajogja.ui.reservasi

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.wisatajogja.R
import com.example.wisatajogja.data.remote.ApiException
import com.example.wisatajogja.data.repository.ReservationRepository
import com.example.wisatajogja.databinding.ActivityReservasiBinding
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.util.formatRupiah
import com.example.wisatajogja.util.hideKeyboard
import com.example.wisatajogja.util.loadPhoto
import com.example.wisatajogja.util.snack
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReservasiActivity : BaseActivity() {

    private lateinit var binding: ActivityReservasiBinding
    private val repository = ReservationRepository()

    private var destinationId: Int = 0
    private var ticketPrice: Int = 0
    private var selectedDate: LocalDate? = null
    private var people: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservasiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        destinationId = intent.getIntExtra(EXTRA_DESTINATION_ID, 0)
        ticketPrice = intent.getIntExtra(EXTRA_TICKET_PRICE, 0)
        if (destinationId <= 0) {
            finish()
            return
        }
        people = savedInstanceState?.getInt(STATE_PEOPLE, 2) ?: 2
        savedInstanceStateDate(savedInstanceState)?.let { date ->
            selectedDate = date
            renderDate()
        }
        bindDestination()
        updateSummary()

        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonMinus.setOnClickListener { changePeople(-1) }
        binding.buttonPlus.setOnClickListener { changePeople(1) }
        binding.cardDate.setOnClickListener { showDatePicker() }
        binding.buttonSubmit.setOnClickListener { submit() }
    }

    private fun bindDestination() = with(binding) {
        textDestination.text = intent.getStringExtra(EXTRA_DESTINATION_NAME).orEmpty()
        textDestinationMeta.text = buildString {
            append(intent.getStringExtra(EXTRA_DESTINATION_AREA).orEmpty())
            append(" · ")
            append(formatRupiah(ticketPrice))
            append(" / orang")
        }
        imageDestination.loadPhoto(intent.getStringExtra(EXTRA_DESTINATION_IMAGE))
        textPeople.text = people.toString()
    }

    private fun changePeople(delta: Int) {
        people = (people + delta).coerceIn(1, 10)
        binding.textPeople.text = people.toString()
        updateSummary()
    }

    private fun updateSummary() = with(binding) {
        val total = ticketPrice * people
        textTicketLine.text = getString(R.string.reservation_ticket_price, people)
        textTicketTotal.text = formatRupiah(total)
        textGrandTotal.text = formatRupiah(total)
        buttonMinus.alpha = if (people <= 1) 0.4f else 1f
        buttonPlus.alpha = if (people >= 10) 0.4f else 1f
    }

    private fun showDatePicker() {
        val selection = selectedDate
            ?.atStartOfDay(ZoneId.of("Asia/Jakarta"))
            ?.toInstant()
            ?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.reservation_date)
            .setSelection(selection)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.of("Asia/Jakarta"))
                .toLocalDate()
            renderDate()
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun renderDate() {
        val date = selectedDate ?: return
        binding.textDate.text = date.format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
        )
    }

    private fun submit() {
        with(binding) {
        val date = selectedDate
        if (date == null) {
            binding.root.snack(getString(R.string.reservation_date_required))
            return
        }
        if (date.isBefore(LocalDate.now())) {
            binding.root.snack("Tanggal kunjungan tidak boleh di masa lalu.")
            return
        }
        binding.root.hideKeyboard()
        buttonSubmit.isEnabled = false
        buttonSubmit.text = getString(R.string.common_loading)
        lifecycleScope.launch {
            runCatching {
                repository.create(
                    destinationId = destinationId,
                    visitDate = date.toString(),
                    people = people,
                    notes = inputNotes.text?.toString().orEmpty(),
                )
            }.onSuccess { data ->
                buttonSubmit.isEnabled = true
                buttonSubmit.text = getString(R.string.reservation_action)
                val code = data.optString("kode_booking")
                MaterialAlertDialogBuilder(this@ReservasiActivity)
                    .setTitle(R.string.reservation_success_title)
                    .setMessage(getString(R.string.reservation_success_message, code))
                    .setPositiveButton(R.string.reservation_success_done) { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }.onFailure { error ->
                buttonSubmit.isEnabled = true
                buttonSubmit.text = getString(R.string.reservation_action)
                binding.root.snack((error as? ApiException)?.message ?: getString(R.string.error_generic))
            }
        }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_PEOPLE, people)
        selectedDate?.let { outState.putString(STATE_DATE, it.toString()) }
        super.onSaveInstanceState(outState)
    }

    private fun savedInstanceStateDate(state: Bundle?): LocalDate? =
        state?.getString(STATE_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    companion object {
        const val EXTRA_DESTINATION_ID = "extra_destination_id"
        const val EXTRA_DESTINATION_NAME = "extra_destination_name"
        const val EXTRA_DESTINATION_AREA = "extra_destination_area"
        const val EXTRA_DESTINATION_IMAGE = "extra_destination_image"
        const val EXTRA_TICKET_PRICE = "extra_ticket_price"
        private const val STATE_PEOPLE = "state_people"
        private const val STATE_DATE = "state_date"
    }
}
