package com.eightbittrade.clipforge;
import org.json.*;
public class Word {
 public String text; public double start,end,confidence;
 public Word(String t,double s,double e,double c){text=t;start=s;end=Math.max(s+.02,e);confidence=c;}
 public JSONObject json() throws JSONException { return new JSONObject().put("text",text).put("start",start).put("end",end).put("confidence",confidence); }
 public static Word from(JSONObject o){return new Word(o.optString("text"),o.optDouble("start"),o.optDouble("end"),o.optDouble("confidence",1));}
}
