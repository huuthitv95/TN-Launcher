package vn.tn.launcher

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

/** Chỉ cần tồn tại để được cấp quyền đọc phiên media (Cài đặt → Truy cập thông báo). */
class MediaListener : NotificationListenerService()

data class NowPlaying(val title: String, val artist: String, val playing: Boolean, val controller: MediaController)

/** Trả về null nếu chưa cấp quyền hoặc không có ứng dụng nào đang phát. */
fun currentMedia(ctx: Context): NowPlaying? = try {
    val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    val list = msm.getActiveSessions(ComponentName(ctx, MediaListener::class.java))
    val c = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
    c?.let {
        val md = it.metadata
        NowPlaying(
            title = md?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = md?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            playing = it.playbackState?.state == PlaybackState.STATE_PLAYING,
            controller = it,
        )
    }
} catch (e: SecurityException) {
    null
}
