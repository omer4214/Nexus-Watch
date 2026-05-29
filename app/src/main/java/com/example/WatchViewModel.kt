package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WatchViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)

    // Clock Time State
    private val _currentTime = MutableStateFlow(LocalDateTime.now())
    val currentTime: StateFlow<LocalDateTime> = _currentTime.asStateFlow()

    // Active Watch Theme Preset Index (Staged/Previewed inside the app)
    private val _selectedThemeIndex = MutableStateFlow(
        sharedPrefs.getInt(PREF_THEME_INDEX, DEFAULT_THEME_INDEX)
    )
    val selectedThemeIndex: StateFlow<Int> = _selectedThemeIndex.asStateFlow()

    // Current Weather Condition
    private val _currentWeather = MutableStateFlow(WeatherCondition.SUNNY)
    val currentWeather: StateFlow<WeatherCondition> = _currentWeather.asStateFlow()

    // Selected Temperature Unit (true = Celsius, false = Fahrenheit)
    private val _isCelsius = MutableStateFlow(sharedPrefs.getBoolean(PREF_IS_CELSIUS, true))
    val isCelsius: StateFlow<Boolean> = _isCelsius.asStateFlow()

    // Battery Override State (useful for reviewing charging animations, low battery warnings, etc.)
    private val _isBatteryOverrideActive = MutableStateFlow(false)
    val isBatteryOverrideActive: StateFlow<Boolean> = _isBatteryOverrideActive.asStateFlow()

    private val _simulatedBatteryLevel = MutableStateFlow(85)
    val simulatedBatteryLevel: StateFlow<Int> = _simulatedBatteryLevel.asStateFlow()

    private val _simulatedIsCharging = MutableStateFlow(false)
    val simulatedIsCharging: StateFlow<Boolean> = _simulatedIsCharging.asStateFlow()

    // --- AMAZFIT COMPANION CONNECTIVITY STATES ---
    private val _connectedDevice = MutableStateFlow<String?>(null)
    val connectedDevice: StateFlow<String?> = _connectedDevice.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<String>>(emptyList())
    val discoveredDevices: StateFlow<List<String>> = _discoveredDevices.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    // This tracks the watch face index ACTUALly synced on the device
    private val _activeWatchFaceOnDeviceIndex = MutableStateFlow(
        sharedPrefs.getInt(PREF_ACTIVE_DEVICE_THEME_INDEX, DEFAULT_THEME_INDEX)
    )
    val activeWatchFaceOnDeviceIndex: StateFlow<Int> = _activeWatchFaceOnDeviceIndex.asStateFlow()

    // Null means inactive. 0 to 100 means active sync
    private val _syncProgress = MutableStateFlow<Int?>(null)
    val syncProgress: StateFlow<Int?> = _syncProgress.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    // --- APP-TO-DEVICE SIMULATIONS ---

    fun startScanningNearbyWatches() {
        if (_isScanning.value || _isConnecting.value) return
        _isScanning.value = true
        _discoveredDevices.value = emptyList()
        viewModelScope.launch {
            delay(1200)
            _discoveredDevices.value = listOf(
                "Amazfit GTR 4 Pro [7A:B4]",
                "Amazfit GTS 4 Dual [9F:2C]",
                "Amazfit Active Spark [1B:88]",
                "Zepp Balance Smart [E3:4A]"
            )
            _isScanning.value = false
        }
    }

    fun stopScanning() {
        _isScanning.value = false
        _discoveredDevices.value = emptyList()
    }

    fun connectDevice(deviceName: String) {
        _isConnecting.value = true
        _isScanning.value = false
        viewModelScope.launch {
            delay(1800)
            _connectedDevice.value = deviceName
            _isConnecting.value = false
        }
    }

    fun disconnectDevice() {
        _connectedDevice.value = null
    }

    fun syncWatchFaceToDevice(themeIndex: Int, callback: () -> Unit = {}) {
        if (_connectedDevice.value == null) return
        _syncProgress.value = 0
        _syncMessage.value = "Paket hazırlanıyor..."
        viewModelScope.launch {
            delay(800)
            _syncProgress.value = 15
            _syncMessage.value = "Bluetooth bağlantısı doğrulanıyor..."
            delay(600)
            
            _syncProgress.value = 35
            _syncMessage.value = "Arayüz dosyası aktarılıyor (bin.xml)..."
            delay(800)

            _syncProgress.value = 65
            _syncMessage.value = "Arayüz kaynakları saate yazılıyor..."
            delay(700)

            _syncProgress.value = 85
            _syncMessage.value = "Saat ekranı yeniden başlatılıyor..."
            delay(600)

            _syncProgress.value = 100
            _syncMessage.value = "Başarıyla Akatarıldı!"
            delay(800)

            // Commit sync index to device
            _activeWatchFaceOnDeviceIndex.value = themeIndex
            sharedPrefs.edit().putInt(PREF_ACTIVE_DEVICE_THEME_INDEX, themeIndex).apply()
            _syncProgress.value = null
            _syncMessage.value = ""
            callback()
        }
    }

    // Weather detailed models based on condition
    private val weatherPresets = mapOf(
        WeatherCondition.SUNNY to WeatherDetails(
            temperatureCelsius = 24,
            location = "Valletta, MT",
            windSpeed = "4 km/h S",
            humidity = 45,
            sunsetTime = "20:12",
            phrase = "Bright & Golden"
        ),
        WeatherCondition.RAINY to WeatherDetails(
            temperatureCelsius = 15,
            location = "London, UK",
            windSpeed = "22 km/h WNW",
            humidity = 88,
            sunsetTime = "21:04",
            phrase = "Soft Rainfall"
        ),
        WeatherCondition.SNOWY to WeatherDetails(
            temperatureCelsius = -2,
            location = "St. Moritz, CH",
            windSpeed = "12 km/h NE",
            humidity = 76,
            sunsetTime = "17:15",
            phrase = "Quiet Snowflake Drift"
        ),
        WeatherCondition.STORMY to WeatherDetails(
            temperatureCelsius = 19,
            location = "Tokyo, JP",
            windSpeed = "35 km/h E",
            humidity = 92,
            sunsetTime = "18:48",
            phrase = "Electric Atmosphere"
        ),
        WeatherCondition.OVERCAST to WeatherDetails(
            temperatureCelsius = 12,
            location = "Seattle, US",
            windSpeed = "9 km/h SSW",
            humidity = 84,
            sunsetTime = "20:55",
            phrase = "Rolling Mist Layer"
        )
    )

    fun updateTime() {
        _currentTime.value = LocalDateTime.now()
    }

    fun setThemeIndex(index: Int) {
        _selectedThemeIndex.value = index
        sharedPrefs.edit().putInt(PREF_THEME_INDEX, index).apply()
    }

    fun setWeatherCondition(condition: WeatherCondition) {
        _currentWeather.value = condition
    }

    fun toggleTemperatureUnit() {
        val newVal = !_isCelsius.value
        _isCelsius.value = newVal
        sharedPrefs.edit().putBoolean(PREF_IS_CELSIUS, newVal).apply()
    }

    fun setBatteryOverride(active: Boolean, level: Int = 85, isCharging: Boolean = false) {
        _isBatteryOverrideActive.value = active
        _simulatedBatteryLevel.value = level.coerceIn(0, 100)
        _simulatedIsCharging.value = isCharging
    }

    fun getWeatherDetails(condition: WeatherCondition): WeatherDetails {
        return weatherPresets[condition] ?: WeatherDetails(
            20, "Earth", "0 km/h", 50, "19:00", "Calm Vibe"
        )
    }

    companion object {
        private const val PREF_THEME_INDEX = "pref_theme_index"
        private const val PREF_ACTIVE_DEVICE_THEME_INDEX = "pref_active_device_theme_index"
        private const val PREF_IS_CELSIUS = "pref_is_celsius"
        private const val DEFAULT_THEME_INDEX = 0 // Elegant Dark is index 0
    }
}

enum class WeatherCondition {
    SUNNY, RAINY, SNOWY, STORMY, OVERCAST
}

data class WeatherDetails(
    val temperatureCelsius: Int,
    val location: String,
    val windSpeed: String,
    val humidity: Int,
    val sunsetTime: String,
    val phrase: String
) {
    fun getTemperatureString(isCelsius: Boolean): String {
        return if (isCelsius) {
            "$temperatureCelsius°C"
        } else {
            val fahrenheit = (temperatureCelsius * 9 / 5) + 32
            "$fahrenheit°F"
        }
    }
}

