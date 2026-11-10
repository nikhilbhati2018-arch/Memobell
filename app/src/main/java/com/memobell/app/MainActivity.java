package com.memobell.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.RecognizerIntent;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
  static final String PREF="memobell_data", KEY="notes";
  LinearLayout list; EditText input; TextView dateLabel,timeLabel,repeatLabel;
  Calendar chosen=Calendar.getInstance(); long alarmTime=0; boolean daily=false,habit=false; int challenge=0;
  int ink=0xFF263B78;
  TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ink);return t;}
  Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xFFF8F7FF);channel();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);screen();render();}
  void channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("reminders","MemoBell reminders",NotificationManager.IMPORTANCE_HIGH));}
  void screen(){
    ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(22,20,22,28);root.setBackgroundColor(0xFFF8F7FF);sc.addView(root);
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView title=label("MemoBell",30);title.setTypeface(null,1);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button menu=btn("⋮");top.addView(menu);menu.setOnClickListener(v->menu(v));root.addView(top);root.addView(label("Plan  •  Remind  •  Achieve",14));
    dateLabel=label("YOUR NOTES · "+DateFormat.getDateInstance(DateFormat.FULL).format(new Date()),15);dateLabel.setPadding(0,20,0,10);root.addView(dateLabel);
    LinearLayout actions=new LinearLayout(this);Button date=btn("＋ Add date"),voice=btn("🎙 Voice");actions.addView(date,new LinearLayout.LayoutParams(0,-2,1));actions.addView(voice,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);date.setOnClickListener(v->pickDate());voice.setOnClickListener(v->voice());
    TextView h=label("Create a note",20);h.setPadding(0,20,0,8);root.addView(h);input=new EditText(this);input.setHint("What do you want to remember?");input.setMinLines(2);root.addView(input);
    Button time=btn("Choose reminder date & time");root.addView(time);timeLabel=label("No reminder selected",13);root.addView(timeLabel);time.setOnClickListener(v->pickTime());
    LinearLayout types=new LinearLayout(this);Button once=btn("One time"),hab=btn("Daily habit");types.addView(once,new LinearLayout.LayoutParams(0,-2,1));types.addView(hab,new LinearLayout.LayoutParams(0,-2,1));root.addView(types);repeatLabel=label("One-time reminder",13);root.addView(repeatLabel);
    once.setOnClickListener(v->{daily=false;habit=false;challenge=0;repeatLabel.setText("One-time reminder");});hab.setOnClickListener(v->habitOptions());
    Button save=btn("Save reminder ✓");root.addView(save);save.setOnClickListener(v->save());TextView heading=label("Your date-wise notes",21);heading.setPadding(0,24,0,8);root.addView(heading);list=new LinearLayout(this);list.setOrientation(1);root.addView(list);setContentView(sc);
  }
  void menu(View v){PopupMenu p=new PopupMenu(this,v);for(String s:new String[]{"Search notes","Habit challenges","Voice assistant","All notes"})p.getMenu().add(s);p.setOnMenuItemClickListener(i->{String s=i.getTitle().toString();if(s.equals("Search notes"))search();else if(s.equals("Habit challenges"))habitOptions();else if(s.equals("Voice assistant"))voice();else render();return true;});p.show();}

  void wakeWordSettings(){
    android.content.SharedPreferences prefs=getSharedPreferences(PREF,0);
    Switch sw=new Switch(this);sw.setText("Enable “Hey MemoBell”");sw.setTextSize(17);sw.setPadding(24,22,24,22);
    sw.setChecked(prefs.getBoolean("wake_word_enabled",false));
    LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.addView(sw);
    TextView info=label("When enabled, MemoBell is intended to listen for its wake phrase in the background. This needs a compatible on-device wake-word engine and microphone foreground-service setup. The setting is saved here, but background listening is not active in this build yet.",13);
    info.setPadding(24,8,24,18);box.addView(info);
    new AlertDialog.Builder(this).setTitle("Hands-free voice activation").setView(box)
      .setPositiveButton("Save",(d,w)->{
        boolean enabled=sw.isChecked();
        prefs.edit().putBoolean("wake_word_enabled",enabled).apply();
        Toast.makeText(this,enabled?"Wake-word preference saved. Engine setup is still required.":"Hands-free wake word turned off.",Toast.LENGTH_LONG).show();
      }).setNegativeButton("Cancel",null).show();
  }
  void pickDate(){new DatePickerDialog(this,(d,y,m,day)->{chosen.set(y,m,day);dateLabel.setText("SELECTED DATE · "+DateFormat.getDateInstance(DateFormat.FULL).format(chosen.getTime()));},chosen.get(Calendar.YEAR),chosen.get(Calendar.MONTH),chosen.get(Calendar.DAY_OF_MONTH)).show();}
  void pickTime(){Calendar now=Calendar.getInstance();new DatePickerDialog(this,(d,y,m,day)->{Calendar x=Calendar.getInstance();x.set(y,m,day);new TimePickerDialog(this,(t,h,min)->{x.set(Calendar.HOUR_OF_DAY,h);x.set(Calendar.MINUTE,min);x.set(Calendar.SECOND,0);alarmTime=x.getTimeInMillis();timeLabel.setText("🔔 "+DateFormat.getDateTimeInstance().format(x.getTime()));},now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),false).show();},now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();}
  void habitOptions(){String[] opts={"10 days","30 days","50 days","Custom duration"};new AlertDialog.Builder(this).setTitle("Daily habit challenge").setItems(opts,(d,w)->{habit=true;daily=true;if(w<3)challenge=new int[]{10,30,50}[w];else{EditText e=new EditText(this);e.setInputType(2);e.setHint("Number of days");new AlertDialog.Builder(this).setTitle("Custom duration").setView(e).setPositiveButton("Set",(a,b)->{try{challenge=Integer.parseInt(e.getText().toString());}catch(Exception ex){challenge=30;}setHabit();}).setNegativeButton("Cancel",null).show();return;}setHabit();}).show();}
  void setHabit(){repeatLabel.setText("🌱 "+challenge+"-day habit · repeats daily");}
  void voice(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},12);return;}try{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak your reminder");startActivityForResult(i,13);}catch(Exception e){Toast.makeText(this,"Voice input unavailable",Toast.LENGTH_LONG).show();}}
  @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==13&&c==RESULT_OK&&d!=null){ArrayList<String>a=d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(a!=null&&!a.isEmpty()){VoiceCommandParser.Result parsed=VoiceCommandParser.parse(a.get(0));
input.setText(parsed.note);
if(parsed.hasTime){alarmTime=parsed.when;timeLabel.setText("🔔 "+DateFormat.getDateTimeInstance().format(new Date(alarmTime)));}
if(parsed.daily){daily=true;repeatLabel.setText("Repeats daily");}
Toast.makeText(this,parsed.hasTime?"Voice reminder details filled in. Please check and save.":"Voice note filled in. Choose a time and save.",Toast.LENGTH_LONG).show();}}}
  JSONArray load(){try{return new JSONArray(getSharedPreferences(PREF,0).getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
  void save(){String s=input.getText().toString().trim();if(s.isEmpty()){input.setError("Enter a note");return;}if(alarmTime>0&&alarmTime<=System.currentTimeMillis()){Toast.makeText(this,"Choose a future time",Toast.LENGTH_LONG).show();return;}try{JSONArray a=load();int id=(int)(System.currentTimeMillis()&0x7fffffff);JSONObject n=new JSONObject();n.put("id",id);n.put("text",s);n.put("when",alarmTime);n.put("daily",daily);n.put("habit",habit);n.put("challenge",challenge);n.put("created",System.currentTimeMillis());n.put("done",false);a.put(n);getSharedPreferences(PREF,0).edit().putString(KEY,a.toString()).apply();if(alarmTime>0)schedule(id,s,alarmTime,daily);input.setText("");alarmTime=0;daily=false;habit=false;challenge=0;timeLabel.setText("No reminder selected");repeatLabel.setText("One-time reminder");render();Toast.makeText(this,"Saved in MemoBell",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Could not save",Toast.LENGTH_SHORT).show();}}
  void schedule(int id,String s,long when,boolean repeat){Intent i=new Intent(this,ReminderReceiver.class);i.putExtra("id",id);i.putExtra("text",s);i.putExtra("daily",repeat);PendingIntent pi=PendingIntent.getBroadcast(this,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);}
  void render(){list.removeAllViews();JSONArray a=load();TreeMap<String,ArrayList<JSONObject>> groups=new TreeMap<>(Collections.reverseOrder());for(int i=0;i<a.length();i++){JSONObject n=a.optJSONObject(i);if(n==null)continue;long w=n.optLong("when",n.optLong("created"));String day=DateFormat.getDateInstance(DateFormat.FULL).format(new Date(w));groups.computeIfAbsent(day,k->new ArrayList<>()).add(n);}if(groups.isEmpty()){list.addView(label("No reminders yet. Add your first note above.",15));return;}for(String day:groups.keySet()){TextView hd=label("📅 "+day,16);hd.setPadding(0,12,0,5);list.addView(hd);for(JSONObject n:groups.get(day)){int id=n.optInt("id");LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(14,12,14,12);card.setBackgroundColor(-1);list.addView(card);card.addView(label((n.optBoolean("done")?"✓ ":"")+n.optString("text"),17));long w=n.optLong("when");if(w>0)card.addView(label("🔔 "+DateFormat.getDateTimeInstance().format(new Date(w))+(n.optBoolean("daily")?" · Daily":""),13));if(n.optBoolean("habit"))card.addView(label("🌱 "+n.optInt("challenge")+"-day habit challenge",13));LinearLayout row=new LinearLayout(this);Button done=btn(n.optBoolean("done")?"Undo":"Complete"),del=btn("Delete");row.addView(done,new LinearLayout.LayoutParams(0,-2,1));row.addView(del,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);done.setOnClickListener(v->markDone(id));del.setOnClickListener(v->delete(id));}}}
  void markDone(int id){try{JSONArray a=load();for(int i=0;i<a.length();i++){JSONObject n=a.optJSONObject(i);if(n!=null&&n.optInt("id")==id)n.put("done",!n.optBoolean("done"));}getSharedPreferences(PREF,0).edit().putString(KEY,a.toString()).apply();render();}catch(Exception ignored){}}
  void delete(int id){JSONArray a=load(),b=new JSONArray();for(int i=0;i<a.length();i++){JSONObject n=a.optJSONObject(i);if(n!=null&&n.optInt("id")!=id)b.put(n);}getSharedPreferences(PREF,0).edit().putString(KEY,b.toString()).apply();Intent i=new Intent(this,ReminderReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(this,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);render();}
  void search(){EditText e=new EditText(this);e.setHint("Search notes");new AlertDialog.Builder(this).setTitle("Search notes").setView(e).setPositiveButton("Search",(d,w)->{String q=e.getText().toString().toLowerCase();list.removeAllViews();JSONArray a=load();for(int i=0;i<a.length();i++){JSONObject n=a.optJSONObject(i);if(n!=null&&n.optString("text").toLowerCase().contains(q))list.addView(label("• "+n.optString("text"),16));}}).setNegativeButton("Cancel",null).show();}
}