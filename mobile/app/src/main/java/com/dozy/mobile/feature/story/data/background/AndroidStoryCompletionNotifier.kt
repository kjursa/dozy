package com.dozy.mobile.feature.story.data.background

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.dozy.mobile.MainActivity
import com.dozy.mobile.R
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.usecase.StoryCompletionNotifier

class AndroidStoryCompletionNotifier(
    private val context: Context,
) : StoryCompletionNotifier {
    override suspend fun deliver(id: Story.Id): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                "stories",
                context.getString(R.string.generation_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        if (!manager.areNotificationsEnabled() ||
            manager.getNotificationChannel("stories").importance == NotificationManager.IMPORTANCE_NONE
        ) {
            return false
        }
        val intent =
            Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("dozy://stories/${id.value}")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification =
            Notification
                .Builder(context, "stories")
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle(context.getString(R.string.generation_notification_title))
                .setContentText(context.getString(R.string.generation_ready))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
        // Stable tag makes replay after process death replace the same notification.
        manager.notify(id.value, 1, notification)
        return true
    }
}
