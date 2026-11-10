package com.memobell.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Calendar;

public class MainActivity extends Activity {
    private LinearLayout remindersBox;
    private EditText reminderInput;
    private SharedPreferences prefs;
    private int nextId;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("memobell", MODE_PRIVATE);
        nextId = prefs.getInt("next_id", 1);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
        buildScreen();
        renderReminders();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24, 28, 24, 20); root.setBackgroundColor(0xFFF7F5FF);
        TextView title = new TextView(this); title.setText("MemoBell"); title.setTextSize(30); title.setTextColor(0xFF39256B); title.setGravity(Gravity.CENTER); title.setPadding(0, 12, 0, 4); root.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText("Never miss what matters."); subtitle.setTextSize(15); subtitle.setTextColor(0xFF625A70); subtitle.setGravity(Gravity.CENTER); subtitle.setPadding(0, 0, 0, 24); root.addView(subtitle);
        reminderInput = new EditText(this); reminderInput.setSingleLine(true); reminderInput.setHint("What do you need to remember?"); root.addView(reminderInput, new LinearLayout.LayoutParams(-1, -2));
        Button add = new Button(this); add.setText("Choose date & time"); add.setOnClickListener(v -> chooseDateTime()); root.addView(add);
        TextView heading = new TextView(this); heading.setText("YOUR REMINDERS"); heading.setTextSize(13); heading.setTextColor(0xFF625A70); heading.setPadding(0, 28, 0, 12); root.addView(heading);
        remindersBox = new LinearLayout(this); remindersBox.setOrientation(LinearLayout.VERTICAL); root.addView(remindersBox);
        setContentView(root);
    }

    private void chooseDateTime() {
        String text = reminderInput.getText().toString().trim();
        if (text.isEmpty()) { reminderInput.setError("Enter a reminder first"); return; }
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance(); selected.set(year, month, day);
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                selected.set(Calendar.HOUR_OF_DAY, hour); selected.set(Calendar.MINUTE, minute); selected.set(Calendar.SECOND, 0); selected.set(Calendar.MILLISECOND, 0);
                if (selected.getTimeInMillis() <= System.currentTimeMillis()) { Toast.makeText(this, "Please choose a future time", Toast.LENGTH_LONG).show(); return; }
                saveAndSchedule(text, selected);
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveAndSchedule(String text, Calendar when) {
        int id = nextId++;
        prefs.edit().putInt("next_id", nextId).apply();
        try {
            JSONArray list = new JSONArray(prefs.getString("items", "[]"));
            JSONObject item = new JSONObject(); item.put("id", id); item.put("title", text); item.put("when", when.getTimeInMillis()); list.put(item);
            prefs.edit().putString("items", list.toString()).apply();
        } catch (Exception ignored) { }
        Intent intent = new Intent(this, ReminderReceiver.class); intent.putExtra("title", text); intent.putExtra("id", id);
        PendingIntent pending = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pending);
        reminderInput.setText(""); renderReminders();
        Toast.makeText(this, "Reminder saved", Toast.LENGTH_SHORT).show();
    }

    private void renderReminders() {
        remindersBox.removeAllViews();
        try {
            JSONArray list = new JSONArray(prefs.getString("items", "[]"));
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i); int id = item.getInt("id"); String name = item.getString("title"); long time = item.getLong("when");
                LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(16, 12, 16, 12); row.setBackgroundColor(0xFFFFFFFF);
                TextView nameView = new TextView(this); nameView.setText(name); nameView.setTextSize(17); nameView.setTextColor(0xFF292333); row.addView(nameView);
                TextView dateView = new TextView(this); dateView.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(time)); dateView.setTextSize(13); dateView.setTextColor(0xFF6D6678); row.addView(dateView);
                Button delete = new Button(this); delete.setText("Delete"); delete.setOnClickListener(v -> deleteReminder(id)); row.addView(delete);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = 12; remindersBox.addView(row, lp);
            }
            if (list.length() == 0) { TextView empty = new TextView(this); empty.setText("No reminders yet. Add your first one above."); empty.setTextColor(0xFF6D6678); remindersBox.addView(empty); }
        } catch (Exception ignored) { }
    }

    private void deleteReminder(int id) {
        try {
            JSONArray old = new JSONArray(prefs.getString("items", "[]")); JSONArray fresh = new JSONArray();
            for (int i = 0; i < old.length(); i++) if (old.getJSONObject(i).getInt("id") != id) fresh.put(old.getJSONObject(i));
            prefs.edit().putString("items", fresh.toString()).apply();
            Intent intent = new Intent(this, ReminderReceiver.class); PendingIntent pending = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            ((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pending);
        } catch (Exception ignored) { }
        renderReminders();
    }
}
