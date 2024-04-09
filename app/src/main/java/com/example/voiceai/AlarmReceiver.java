package com.example.voiceai;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the alarm is triggered.
        // You can perform actions here, such as displaying a notification or playing a sound.
        // For example, you can notify the user that the alarm has gone off.
        // You can use the TextToSpeech or any other method to inform the user.
        // Implement your desired action in this method.
        System.out.println("Alarm ringing");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        MediaPlayer mediaPlayer = MediaPlayer.create(context, alarmSound);
        mediaPlayer.start();

    }
}
