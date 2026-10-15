package vn.tnlauncher.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

// Test APK only; never packaged in the delivered launcher APK.
class FixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Ứng dụng mẫu A đang chạy"; textSize = 28f })
    }
}
class PrivateFixtureActivity : Activity()
class ProtectedFixtureActivity : Activity()
class FallbackFixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Màn hình phụ mẫu đang chạy"; textSize = 28f })
    }
}
