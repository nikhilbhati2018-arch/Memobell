package com.memobell.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class SpokenAlarmService extends Service implements TextToSpeech.OnInitListener {
  private TextToSpeech tts;
  private String spokenText="You have a reminder";
  private boolean ready=false;
  @Override public int onStartCommand(Intent intent,int flags,int startId) {
    if(intent!=null) spokenText=intent.getStringExtra("text");
    if(spokenText==null||spokenText.trim().isEmpty())spokenText="You have a reminder";
    if(Build.VERSION.SDK_INT>=26) {
      NotificationChannel ch=new NotificationChannel("spoken_alarm","MemoBell spoken alarms",NotificationManager.IMPORTANCE_LOW);
      ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
      Notification n=new Notification.Builder(this,"spoken_alarm").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("MemoBell").setContentText("Speaking your reminder").build();
      startForeground(7402,n);
    }
    if(tts==null)tts=new TextToSpeech(this,this); else if(ready)speak();
    return START_NOT_STICKY;
  }
  @Override public void onInit(int status) {
    if(status==TextToSpeech.SUCCESS) {
      ready=true;
      if(tts!=null) {int lang=tts.setLanguage(Locale.getDefault()); if(lang==TextToSpeech.LANG_MISSING_DATA||lang==TextToSpeech.LANG_NOT_SUPPORTED)tts.setLanguage(Locale.US);}
      speak();
    } else stopSelf();
  }
  private void speak() {
    if(tts!=null&&ready) {
      if(Build.VERSION.SDK_INT>=21)tts.speak("MemoBell reminder. "+spokenText,TextToSpeech.QUEUE_FLUSH,null,"memobell_alarm");
      else tts.speak("MemoBell reminder. "+spokenText,TextToSpeech.QUEUE_FLUSH,null);
      new Handler(Looper.getMainLooper()).postDelayed(this::stopSelf,12000);
    }
  }
  @Override public void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
  @Override public android.os.IBinder onBind(Intent intent){return null;}
}
