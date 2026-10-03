package vn.tn.launcher

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tn_launcher", Context.MODE_PRIVATE)

    /** auto | light | dark */
    var themeMode: String
        get() = sp.getString("theme", "auto") ?: "auto"
        set(v) = sp.edit().putString("theme", v).apply()

    var vehicleName: String
        get() = sp.getString("vehicle", "VinFast Limo Green") ?: "VinFast Limo Green"
        set(v) = sp.edit().putString("vehicle", v).apply()

    /** Biển số do người dùng tự nhập; mặc định để trống (không đưa dữ liệu cá nhân vào mã nguồn). */
    var plate: String
        get() = sp.getString("plate", "") ?: ""
        set(v) = sp.edit().putString("plate", v).apply()

    var lat: Float
        get() = sp.getFloat("lat", 21.0285f)
        set(v) = sp.edit().putFloat("lat", v).apply()
    var lon: Float
        get() = sp.getFloat("lon", 105.8542f)
        set(v) = sp.edit().putFloat("lon", v).apply()

    /** Package của app camera 360 / trợ lái nếu tự nhận diện sai. */
    var cameraPkg: String
        get() = sp.getString("camera_pkg", "") ?: ""
        set(v) = sp.edit().putString("camera_pkg", v).apply()
    var adasPkg: String
        get() = sp.getString("adas_pkg", "") ?: ""
        set(v) = sp.edit().putString("adas_pkg", v).apply()
}
