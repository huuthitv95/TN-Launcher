package vn.tn.launcher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tn_launcher", Context.MODE_PRIVATE)

    /** auto | light | dark */
    var themeMode: String
        get() = sp.getString("theme", "auto") ?: "auto"
        set(v) = sp.edit().putString("theme", v).apply()

    // Thông tin xe do người dùng tự nhập, không đưa dữ liệu cá nhân vào mã nguồn.
    var vehicleName by mutableStateOf(sp.getString("vehicle", "VinFast Limo Green") ?: "VinFast Limo Green")
        private set
    var plate by mutableStateOf(sp.getString("plate", "") ?: "")
        private set

    fun updateVehicle(name: String, plate: String) {
        vehicleName = name
        this.plate = plate
        sp.edit().putString("vehicle", name).putString("plate", plate).apply()
    }

    var lat by mutableStateOf(sp.getFloat("lat", 21.0285f))
        private set
    var lon by mutableStateOf(sp.getFloat("lon", 105.8542f))
        private set

    fun setCity(la: Float, lo: Float) {
        lat = la
        lon = lo
        sp.edit().putFloat("lat", la).putFloat("lon", lo).apply()
    }

    /** Ứng dụng người dùng gán cho từng nút: camera, adas, tpms, map, music. Rỗng = tự nhận diện. */
    val slots = mutableStateMapOf<String, String>().apply {
        listOf("camera", "adas", "tpms", "map", "music").forEach { put(it, sp.getString("slot_$it", "") ?: "") }
    }

    fun setSlot(key: String, pkg: String) {
        slots[key] = pkg
        sp.edit().putString("slot_$key", pkg).apply()
    }

    /** Dock tùy chọn (tối đa 5). Rỗng = dùng mặc định. */
    val dock = mutableStateListOf<String>().apply {
        addAll((sp.getString("dock", "") ?: "").split(",").filter { it.isNotBlank() })
    }

    fun saveDock() = sp.edit().putString("dock", dock.joinToString(",")).apply()

    var cameraPkg: String
        get() = slots["camera"] ?: ""
        set(v) = setSlot("camera", v)
    var adasPkg: String
        get() = slots["adas"] ?: ""
        set(v) = setSlot("adas", v)
}
