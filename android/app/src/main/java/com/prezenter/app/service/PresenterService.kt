package com.prezenter.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.VolumeProviderCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.prezenter.app.MainActivity
import com.prezenter.app.PrezenterApplication
import com.prezenter.app.R
import com.prezenter.app.network.CommandAction
import com.prezenter.app.network.PresenterSession

/**
 * Ekran o'chiq (yoki ilova fonda) bo'lganda ham telefon tovush
 * tugmalarini slayd navigatsiyasi (NEXT/PREV) sifatida ushlab qolish
 * uchun ishlatiladi.
 *
 * NIMA UCHUN ISHLAYDI: Android odatda hardware volume tugmalarini
 * faqat ekran ochiq va ilova old planda bo'lganda ilovaga uzatadi.
 * Lekin agar ilovada FAOL (active + PLAYING holatidagi) MediaSession
 * bo'lsa va o'sha session "remote volume" (VolumeProviderCompat) rejimida
 * ishlasa, tizim tovush tugmasi bosilganda ovoz darajasini o'zi
 * o'zgartirish o'rniga onAdjustVolume() callbackini chaqiradi - bu esa
 * ekran qulflangan holatda ham ishlaydi (xuddi Bluetooth garnitura
 * tugmalari musiqa ilovasini boshqargani kabi). Shu bois quyida haqiqiy
 * audio ijro etmasdan, "soxta PLAYING" holatini ushlab turamiz.
 */
class PresenterService : Service() {

    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        setupMediaSession()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun setupMediaSession() {
        val session = MediaSessionCompat(this, "PrezenterVolumeSession")

        val volumeProvider = object : VolumeProviderCompat(
            VOLUME_CONTROL_RELATIVE,
            MAX_FAKE_VOLUME,
            CURRENT_FAKE_VOLUME
        ) {
            override fun onAdjustVolume(direction: Int) {
                // direction: +1 = tovush tugmasi yuqoriga (keyingi slayd),
                //            -1 = tovush tugmasi pastga (oldingi slayd)
                when (direction) {
                    1 -> PresenterSession.client.sendCommand(CommandAction.NEXT)
                    -1 -> PresenterSession.client.sendCommand(CommandAction.PREV)
                }
                // Ko'rsatkichni har doim o'rtada ushlab turamiz, shunda
                // tugma cheksiz bosilaversa ham NEXT/PREV yuborishda davom etadi.
                currentVolume = CURRENT_FAKE_VOLUME
            }
        }

        session.setPlaybackToRemote(volumeProvider)
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                .build()
        )
        session.isActive = true

        mediaSession = session
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, PresenterService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PrezenterApplication.PRESENTER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_media_pause, "To'xtatish", stopIntent)
            .setStyle(MediaStyle().setMediaSession(mediaSession?.sessionToken).setShowActionsInCompactView(0))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.prezenter.app.action.STOP"

        private const val MAX_FAKE_VOLUME = 100
        private const val CURRENT_FAKE_VOLUME = 50

        fun start(context: android.content.Context) {
            val intent = Intent(context, PresenterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            context.startService(Intent(context, PresenterService::class.java).setAction(ACTION_STOP))
        }
    }
}
