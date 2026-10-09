package com.eightbittrade.clipforge;
import java.util.*;
public class Highlights {
 public static class Peak { public double time,value; public Peak(double t,double v){time=t;value=v;} }
 public static class Pick { public double start,end,score;public String reason,title; public Pick(double s,double e,double q,String r,String t){start=s;end=e;score=q;reason=r;title=t;} }
 public static List<Pick> recommend(double duration,List<Peak> peaks,List<Word> words) {
  List<Pick> candidates=new ArrayList<>(),out=new ArrayList<>(); double length=Math.min(35,duration);
  for(Peak p:peaks) if(Double.isFinite(p.value)&&p.value>0&&p.time>=0&&p.time<duration){double s=Math.max(0,Math.min(duration-length,p.time-length*.35)); candidates.add(new Pick(s,s+length,p.value+10,"YouTube replay peak · relative signal, not a view count",excerpt(words,s,s+length)));}
  for(int i=0;i<words.size();i++){Word w=words.get(i);if(i>0&&!words.get(i-1).text.matches(".*[.!?]$")&&w.start-words.get(i-1).end<.65)continue;double s=Math.min(Math.max(0,duration-length),w.start),e=s+length;
   String text=excerpt(words,s,e),lower=text.toLowerCase(Locale.ROOT);double score=0;int n=0;
   for(Word x:words)if(x.start>=s&&x.start<e){score+=Math.max(0,x.confidence);n++;}score=Math.min(1,n/60.0)+score/Math.max(1,n)*.4;
   for(String hook:new String[]{"why","how","secret","mistake","never","actually","here's","finally","but","imagine"})if(lower.contains(hook))score+=.15;
   if(n>=8)candidates.add(new Pick(s,e,score,"Content suggestion · speech density and opening words",text));
  }
  candidates.sort((a,b)->Double.compare(b.score,a.score));for(Pick p:candidates){boolean distinct=true;for(Pick q:out)if(overlap(p,q)>.25)distinct=false;if(distinct){out.add(p);if(out.size()==3)break;}}
  int target=duration>=45?3:duration>=20?2:1;double sampleLength=Math.min(length,duration/target);
  for(int i=0;out.size()<target&&i<target*8;i++){double s=(duration-sampleLength)*i/Math.max(1,target*8-1);Pick p=new Pick(s,s+sampleLength,-1,"Review sample · no replay or strong transcript evidence",excerpt(words,s,s+sampleLength));boolean ok=true;for(Pick q:out)if(overlap(p,q)>.25)ok=false;if(ok)out.add(p);}
  // If a long peak occupies too much of a short video, return three shorter review windows.
  if(out.size()<target){out.clear();for(int i=0;i<target;i++){double s=duration*i/target;out.add(new Pick(s,s+duration/target,-1,"Review sample · video too short for three long highlights",excerpt(words,s,s+duration/target)));}}
  return out;
 }
 static double overlap(Pick a,Pick b){return Math.max(0,Math.min(a.end,b.end)-Math.max(a.start,b.start))/Math.min(a.end-a.start,b.end-b.start);}
 static String excerpt(List<Word>w,double s,double e){StringBuilder b=new StringBuilder();for(Word x:w)if(x.start>=s&&x.start<e){b.append(x.text).append(' ');if(b.length()>90)break;}return b.length()==0?"Preview this moment":b.toString().trim();}
}
