package com.fuellog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fuellog.app.data.FuelEntry
import com.fuellog.app.data.FuelRecord
import com.fuellog.app.data.FuelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordsViewModel(private val repository: FuelRepository) : ViewModel() {

    val entries: StateFlow<List<FuelEntry>> = repository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(record: FuelRecord) {
        viewModelScope.launch { repository.delete(record) }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }

    companion object {
        fun factory(repository: FuelRepository) = viewModelFactory {
            initializer { RecordsViewModel(repository) }
        }
    }
}
