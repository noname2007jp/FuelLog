package com.fuellog.app.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fuellog.app.data.FuelRecord
import com.fuellog.app.data.FuelRepository
import kotlinx.coroutines.launch

class EditViewModel(
    private val repository: FuelRepository,
    private val recordId: Long
) : ViewModel() {

    var existing: FuelRecord? by mutableStateOf(null)
        private set

    init {
        if (recordId > 0) {
            viewModelScope.launch {
                existing = repository.findById(recordId)
            }
        }
    }

    fun save(record: FuelRecord, onDone: () -> Unit) {
        viewModelScope.launch {
            if (record.id == 0L) repository.insert(record) else repository.update(record)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            existing?.let { repository.delete(it) }
            onDone()
        }
    }

    companion object {
        fun factory(repository: FuelRepository, recordId: Long) = viewModelFactory {
            initializer { EditViewModel(repository, recordId) }
        }
    }
}
