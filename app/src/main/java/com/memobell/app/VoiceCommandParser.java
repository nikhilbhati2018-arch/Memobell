package com.memobell.app;

import java.util.*;
import java.util.regex.*;

public final class VoiceCommandParser {
  public static final class Result {
    public String note;
    public long when;
    public boolean hasTime;
    public boolean daily;
  }
  private VoiceCommandParser() {}
  public static Result parse(String spoken) {
    Result r=new Result();
    String raw=spoken==null?"":spoken.trim();
    String s=raw.toLowerCase(Locale.ROOT);
    Calendar c=Calendar.getInstance();
    if(s.matches(".*\\b(day after tomorrow|परसों)\\b.*")) c.add(Calendar.DAY_OF_YEAR,2);
    else if(s.matches(".*\\b(tomorrow|कल)\\b.*")) c.add(Calendar.DAY_OF_YEAR,1);
    else if(s.matches(".*\\b(today|आज)\\b.*")) {}
    else if(s.matches(".*\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b.*")) {
      String[] days={"sunday","monday","tuesday","wednesday","thursday","friday","saturday"};
      for(int i=0;i<days.length;i++) if(s.contains(days[i])) {
        int delta=(i-c.get(Calendar.DAY_OF_WEEK)+7)%7; if(delta==0)delta=7;
        c.add(Calendar.DAY_OF_YEAR,delta); break;
      }
    }
    Matcher rel=Pattern.compile("\\bin\\s+(\\d+)\\s+(minute|minutes|min|hour|hours)\\b").matcher(s);
    if(rel.find()) {
      int n=Integer.parseInt(rel.group(1));
      c.add(rel.group(2).startsWith("hour")?Calendar.HOUR_OF_DAY:Calendar.MINUTE,n);
      r.hasTime=true;
    } else {
      Matcher tm=Pattern.compile("\\b(?:at\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b").matcher(s);
      if(tm.find()) {
        int h=Integer.parseInt(tm.group(1)),m=tm.group(2)==null?0:Integer.parseInt(tm.group(2));
        if(tm.group(3).equals("pm")&&h<12)h+=12; if(tm.group(3).equals("am")&&h==12)h=0;
        c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
        if(c.getTimeInMillis()<=System.currentTimeMillis() && !s.contains("tomorrow") && !s.contains("कल")) c.add(Calendar.DAY_OF_YEAR,1);
        r.hasTime=true;
      } else if(s.matches(".*\\b(सुबह|दोपहर|शाम|रात)\\s*\\d{1,2}.*")) {
        Matcher hi=Pattern.compile("(सुबह|दोपहर|शाम|रात)\\s*(\\d{1,2})(?::(\\d{2}))?").matcher(s);
        if(hi.find()) {int h=Integer.parseInt(hi.group(2));if((hi.group(1).equals("शाम")||hi.group(1).equals("रात")||hi.group(1).equals("दोपहर"))&&h<12)h+=12;c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,hi.group(3)==null?0:Integer.parseInt(hi.group(3)));c.set(Calendar.SECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);r.hasTime=true;}
      }
    }
    r.when=r.hasTime?c.getTimeInMillis():0;
    r.daily=s.contains("daily")||s.contains("every day")||s.contains(" रोज")||s.contains("हर दिन");
    String cleaned=raw.replaceAll("(?i)\\b(hey\\s+memobell|memobell|remind me|reminder|please|tomorrow|today|day after tomorrow|every day|daily|at\\s+\\d{1,2}(:\\d{2})?\\s*(am|pm)|in\\s+\\d+\\s+(minutes?|hours?))\\b"," ").trim();
    cleaned=cleaned.replaceAll("(?i)\\b(to|on)\\b"," ").replaceAll("\\s+"," ").trim();
    r.note=cleaned.isEmpty()?raw:cleaned;
    return r;
  }
}
