package com.example.bs1;

import static android.content.Context.ALARM_SERVICE;
import static com.example.bs1.MainActivity.imageButton;
import static com.example.bs1.MainActivity.imageButton2;
import static com.example.bs1.MainActivity.imageButton3;
import static com.example.bs1.MainActivity.text;
import static com.example.bs1.MainActivity.spinner2;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.AdapterView;

import androidx.core.app.NotificationCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.Calendar;

public class playBackground extends BroadcastReceiver {
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    static MediaPlayer mediaPlayer = null;
    MediaSessionCompat mediaSessionCompat;
    StorageReference mStorageRef;
    static String subject = "", notify = "", topic = "";
    NotificationManager notificationManager;
    Calendar calendar;

    @Override
    public void onReceive(Context context, Intent intent) {
        sharedpreferences = context.getSharedPreferences("store", Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();
        mediaSessionCompat = new MediaSessionCompat(context, "mytagmediam9");
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        calendar = Calendar.getInstance();

        createNotificationChannel();

        try {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case "stop":
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                            mediaPlayer.release();
                            mediaPlayer = null;
                        }
                        if (notificationManager != null) notificationManager.cancelAll();
                        break;
                    case "pause":
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            showNotification(context, true, R.drawable.ic_baseline_play_arrow_24);
                            keepInSharedPreferences("image2", R.drawable.ic_baseline_play_arrow_24);
                        }
                        break;
                    case "playPause":
                        playPause(context);
                        break;
                    case "prev":
                        prev(context);
                        break;
                    case "next":
                        next(context);
                        break;
                    case "alarm":
                        if (((calendar.get(Calendar.HOUR_OF_DAY) > 3 && calendar.get(Calendar.HOUR_OF_DAY) < 8) || (calendar.get(Calendar.HOUR_OF_DAY) > 16 && calendar.get(Calendar.HOUR_OF_DAY) < 23)) && sharedpreferences.getBoolean("onOff",true))
                            context.sendBroadcast(new Intent(context, playBackground.class).setAction("playPause"));
                        break;

                    case "android.intent.action.BOOT_COMPLETED":

                        Intent notifyIntent = new Intent(context, playBackground.class).setAction("alarm");

                        @SuppressLint("UnspecifiedImmutableFlag") final PendingIntent notifyPendingIntent = PendingIntent.getBroadcast(context, 1111, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

                        long repeatInterval = AlarmManager.INTERVAL_HOUR;

                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, (calendar.get(Calendar.HOUR_OF_DAY) + 1));
                        calendar.set(Calendar.MINUTE, 0);
                        calendar.set(Calendar.SECOND, 0);

                        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), repeatInterval, notifyPendingIntent);
                        editor.putBoolean("one", false);
                        editor.commit();

                        break;
                }
            }
            imageButton2.setOnClickListener(view -> playPause(context));
            imageButton.setOnClickListener(view -> prev(context));
            imageButton3.setOnClickListener(view -> next(context));

            spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                    keepInSharedPreferences("TopicPosition", i);
                    keepStringSharedPreferences("topic", spinner2.getSelectedItem().toString());
                    if (sharedpreferences.getInt("play", 0) == 1) {
                        play(context);
                        keepInSharedPreferences("play", 0);
                    } else if (notify.contains("preparing")) {
                        disableButtons();
                        keepInSharedPreferences("showButton", 0);
                    } else {
                        enableButtons();
                        keepInSharedPreferences("showButton", 1);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playPause(Context context) {

        if (mediaPlayer != null && sharedpreferences.getString("topic", "").equals(sharedpreferences.getString("playingTopic", ""))) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                if (sharedpreferences.getBoolean("activity", true))
                    imageButton2.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                keepInSharedPreferences("image2", R.drawable.ic_baseline_play_arrow_24);
                showNotification(context, true, R.drawable.ic_baseline_play_arrow_24);
            } else {
                mediaPlayer.start();
                if (sharedpreferences.getBoolean("activity", true))
                    imageButton2.setImageResource(R.drawable.ic_baseline_pause_24);
                keepInSharedPreferences("image2", R.drawable.ic_baseline_pause_24);
                showNotification(context, true, R.drawable.ic_baseline_pause_24);
            }

        } else play(context);

    }

    public void play(Context context) {
        topic = sharedpreferences.getString("topic", "");
        subject = sharedpreferences.getString("subject", "");
        notify = "preparing " + topic;
        text = notify;
        if (sharedpreferences.getBoolean("activity", true)) disableButtons();
        keepInSharedPreferences("showButton", 0);
        showNotification(context, false, R.drawable.ic_baseline_play_arrow_24);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mStorageRef.child(subject)
                .child(topic)
                .getDownloadUrl().addOnSuccessListener(uri -> {
            try {
                mediaPlayer = new MediaPlayer();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mediaPlayer.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                    );
                }
                mediaPlayer.setDataSource(context, uri);
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mediaPlayer -> {
                    mediaPlayer.start();
                    keepBoolSharedPreferences("once",true);
                    text = topic;
                    if (sharedpreferences.getBoolean("activity", true)) {
                        enableButtons();
                        imageButton2.setImageResource(R.drawable.ic_baseline_pause_24);
                    }
                    keepInSharedPreferences("showButton", 1);
                    keepInSharedPreferences("image2", R.drawable.ic_baseline_pause_24);
                    notify = topic;
                    showNotification(context, true, R.drawable.ic_baseline_pause_24);
                    keepStringSharedPreferences("playingTopic", sharedpreferences.getString("topic", ""));
                });
                mediaPlayer.setOnCompletionListener(mediaPlayer -> {

                    if (sharedpreferences.getBoolean("activity", true))
                        imageButton2.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                    keepInSharedPreferences("image2", R.drawable.ic_baseline_play_arrow_24);
                    showNotification(context, true, R.drawable.ic_baseline_play_arrow_24);

                });
                mediaPlayer.setOnErrorListener((mediaPlayer, i, i1) -> {
                    text = "Try again " + topic;
                    if (sharedpreferences.getBoolean("activity", true)) {
                        enableButtons();
                        imageButton2.setImageResource(R.drawable.ic_baseline_play_arrow_24);

                    }
                    keepInSharedPreferences("showButton", 1);
                    keepInSharedPreferences("image2", R.drawable.ic_baseline_play_arrow_24);
                    notify = "Try again " + topic;
                    showNotification(context, true, R.drawable.ic_baseline_play_arrow_24);
                    return false;
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mStorageRef.child(subject)
                .child(topic)
                .getDownloadUrl().addOnFailureListener(e -> {
            text = e.getMessage();
            if (sharedpreferences.getBoolean("activity", true)) {
                enableButtons();
                imageButton2.setImageResource(R.drawable.ic_baseline_play_arrow_24);

            }
            keepInSharedPreferences("showButton", 1);
            keepInSharedPreferences("image2", R.drawable.ic_baseline_play_arrow_24);
            notify = e.getMessage() + "\nTry again " + topic;
            showNotification(context, true, R.drawable.ic_baseline_play_arrow_24);

        });
    }

    public void prev(Context context) {
        int prev = sharedpreferences.getInt("TopicPosition", 0) - 1;
        if (prev > -1)
            if (sharedpreferences.getBoolean("activity", true)) {
                spinner2.setSelection(prev);
                keepInSharedPreferences("play", 1);
            } else {
                keepInSharedPreferences("TopicPosition", prev);
                keepStringSharedPreferences("topic", sharedpreferences.getString("" + prev, ""));
                play(context);
            }
    }

    public void next(Context context) {
        int next = sharedpreferences.getInt("TopicPosition", 0) + 1;
        if (next < sharedpreferences.getInt("topicSize", 0))
            if (sharedpreferences.getBoolean("activity", true)) {
                spinner2.setSelection(next);
                keepInSharedPreferences("play", 1);
            } else {
                keepInSharedPreferences("TopicPosition", next);
                keepStringSharedPreferences("topic", sharedpreferences.getString("" + next, ""));
                play(context);
            }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "play", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Media controls");
            notificationManager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    public void showNotification(Context context, boolean showButtons, int playPause) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (showButtons) {
            PendingIntent playPI, prevPI, nextPI;

            Intent playI = new Intent(context, playBackground.class).setAction("playPause");
            Intent prevI = new Intent(context, playBackground.class).setAction("prev");
            Intent nextI = new Intent(context, playBackground.class).setAction("next");

            playPI = PendingIntent.getBroadcast(context, 2, playI, PendingIntent.FLAG_UPDATE_CURRENT);
            prevPI = PendingIntent.getBroadcast(context, 3, prevI, PendingIntent.FLAG_UPDATE_CURRENT);
            nextPI = PendingIntent.getBroadcast(context, 4, nextI, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new NotificationCompat.Builder(context, "1")
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_foreground))
                    .setContentTitle(subject)
                    .setContentText(notify)
                    .setContentIntent(contentIntent)
                    .addAction(R.drawable.ic_baseline_skip_previous_24, "prev", prevPI)
                    .addAction(playPause, "play", playPI)
                    .addAction(R.drawable.ic_baseline_skip_next_24, "next", nextPI)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSessionCompat.getSessionToken()))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOnlyAlertOnce(true)
                    .build();

            notificationManager.notify(1, notification);

        } else {

            Notification notification = new NotificationCompat.Builder(context, "1")
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(subject)
                    .setContentText(notify)
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOnlyAlertOnce(true)
                    .build();

            notificationManager.notify(1, notification);
        }

    }

    public void enableButtons() {
        imageButton.setEnabled(true);
        imageButton2.setEnabled(true);
        imageButton3.setEnabled(true);
    }

    public void disableButtons() {
        imageButton.setEnabled(false);
        imageButton2.setEnabled(false);
        imageButton3.setEnabled(false);
    }

    private void keepInSharedPreferences(String keyStr, int valueInt) {
        editor.putInt(keyStr, valueInt);
        editor.apply();
    }

    private void keepStringSharedPreferences(String keyStr1, String valueStr1) {
        editor.putString(keyStr1, valueStr1);
        editor.apply();
    }
    private void keepBoolSharedPreferences(String keyStr2, boolean valueBool) {
        editor.putBoolean(keyStr2, valueBool);
        editor.apply();
    }
}