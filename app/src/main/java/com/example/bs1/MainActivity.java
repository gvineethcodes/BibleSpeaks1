package com.example.bs1;

import static com.example.bs1.playBackground.mediaPlayer;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    Spinner spinner;
    @SuppressLint("StaticFieldLeak")
    public static Spinner spinner2;
    TextView textView;
    @SuppressLint("StaticFieldLeak")
    public static ImageButton imageButton, imageButton2, imageButton3;
    public static AlarmManager staticAlarmManager;
    public static Intent staticIntent;
    public static PendingIntent staticPendingIntent;
    public static String text = "";
    StorageReference mStorageRef;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinner = findViewById(R.id.spinner);
        spinner2 = findViewById(R.id.spinner2);
        textView = findViewById(R.id.textview);
        imageButton = findViewById(R.id.imageButton);
        imageButton2 = findViewById(R.id.imageButton2);
        imageButton3 = findViewById(R.id.imageButton3);
        CheckBox checkBox = findViewById(R.id.checkBox);
        button = findViewById(R.id.button);
        SeekBar seekBar = findViewById(R.id.seekbar);

        disableButtons();

        sharedpreferences = getSharedPreferences("store", Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();
        checkBox.setChecked(sharedpreferences.getBoolean("onOff",true));
        mStorageRef = FirebaseStorage.getInstance().getReference();

        sendBroadcast(new Intent(getApplicationContext(), playBackground.class));

        mStorageRef.listAll()
                .addOnSuccessListener(listResult -> {
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
                    spinner.setAdapter(arrayAdapter);

                    for (StorageReference prefix : listResult.getPrefixes()) {
                        arrayAdapter.add(prefix.getName());
                    }

                    spinner.setSelection(sharedpreferences.getInt("SubjectPosition", 0));

                    })
                .addOnFailureListener(e -> textView.setText(e.toString()));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                String subjectName = spinner.getSelectedItem().toString();
                mStorageRef.child(subjectName).listAll()
                        .addOnSuccessListener(listResult -> {
                            ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
                            spinner2.setAdapter(arrayAdapter2);

                            keepInSharedPreferences("topicSize", listResult.getItems().size());
                            int j = 0;
                            for (StorageReference item : listResult.getItems()) {
                                String name = item.getName();
                                arrayAdapter2.add(name);
                                keepInSharedPreferences(name, j);
                                keepStringSharedPreferences("" + j, name);
                                j = j + 1;
                            }
                            spinner2.setSelection(sharedpreferences.getInt("TopicPosition", 0));

                            for (StorageReference prefix : listResult.getPrefixes()) {
                                String name = prefix.getName();
                                if (name.contains("https")) keepStringSharedPreferences("url",name.replaceAll("[*]","/"));
                                else keepFloatSharedPreferences(Float.parseFloat(name));
                            }
                        })
                        .addOnFailureListener(e -> textView.setText(e.toString()));
                keepInSharedPreferences("SubjectPosition", i);
                keepStringSharedPreferences("subject", subjectName);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        final String[] s = {"0 : 0"};
        final String[] t = { "0 : 0" };

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(mediaPlayer!=null) {
                    if(sharedpreferences.getBoolean("once",false)){
                        int total = mediaPlayer.getDuration();
                        t[0] =""+total % 3600000 / 60000+" : "+total % 3600000 % 60000 / 1000;
                        seekBar.setMax(total);
                        keepBoolSharedPreferences("once",false);
                    }
                    int duration = mediaPlayer.getCurrentPosition();
                    s[0] = ""+duration % 3600000 / 60000+" : "+duration % 3600000 % 60000 / 1000+" / "+ t[0] + "\n";
                    if(mediaPlayer.isPlaying()) seekBar.setProgress(duration);

                }else {
                    s[0] ="0 : 0\n";
                    t[0] ="0 : 0";
                }
                textView.setText(String.format("%s%s", s[0], text));
                handler.postDelayed(this, 1000);
            }
        }, 0);


        if (sharedpreferences.getBoolean("one", true)) {

            setAlarming(this);
            keepBoolSharedPreferences("one",false);

        }


        button.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedpreferences.getString("url","http://www.google.com")))));

        checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
            MainActivity.this.keepBoolSharedPreferences("onOff", b);
            if(b) setAlarming(MainActivity.this);
            else if (staticPendingIntent != null && staticAlarmManager != null)
                staticAlarmManager.cancel(staticPendingIntent);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b) if(mediaPlayer!=null) mediaPlayer.seekTo(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        spinner2.setSelection(sharedpreferences.getInt("TopicPosition", 0));
        imageButton2.setImageResource(sharedpreferences.getInt("image2", R.drawable.ic_baseline_play_arrow_24));
        showButton(sharedpreferences.getInt("showButton", 0));
        keepBoolSharedPreferences("activity",true);
        button.setEnabled(1.3 < sharedpreferences.getFloat("version", (float) -1.0));

    }

    @Override
    protected void onPause() {
        super.onPause();
        keepBoolSharedPreferences("activity",false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendBroadcast(new Intent(getApplicationContext(), playBackground.class).setAction("pause"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(getApplicationContext(), playBackground.class).setAction("stop"));
    }

    private void showButton(int show) {
        if (show == 1) enableButtons();
        else disableButtons();
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
    private void keepFloatSharedPreferences(float value) {
        editor.putFloat("version", value);
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

    @SuppressLint("UnspecifiedImmutableFlag")
    public static void setAlarming(Context context){

        staticIntent = new Intent(context, playBackground.class).setAction("alarm");

        staticPendingIntent = PendingIntent.getBroadcast(context, 0, staticIntent, 0);

        staticAlarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();

        if (calendar.get(Calendar.HOUR_OF_DAY) <=2) {
            calendar.set(Calendar.HOUR_OF_DAY, 4);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        } else if((calendar.get(Calendar.HOUR_OF_DAY) >= 3 && calendar.get(Calendar.HOUR_OF_DAY) <= 6) || (calendar.get(Calendar.HOUR_OF_DAY) >= 16 && calendar.get(Calendar.HOUR_OF_DAY) <= 21)) {
            calendar.set(Calendar.HOUR_OF_DAY, (calendar.get(Calendar.HOUR_OF_DAY) + 1));
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        } else if(calendar.get(Calendar.HOUR_OF_DAY) >= 7 && calendar.get(Calendar.HOUR_OF_DAY) <= 15) {
            calendar.set(Calendar.HOUR_OF_DAY, 17);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        } else{
            calendar.set(Calendar.DAY_OF_WEEK,(calendar.get(Calendar.DAY_OF_WEEK) + 1));
            calendar.set(Calendar.HOUR_OF_DAY, 4);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            staticAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), staticPendingIntent);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            staticAlarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), staticPendingIntent);
        else
            staticAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), staticPendingIntent);

    }
}
