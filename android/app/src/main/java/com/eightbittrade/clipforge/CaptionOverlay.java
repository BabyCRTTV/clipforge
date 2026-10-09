package com.eightbittrade.clipforge;
import android.graphics.*;
import androidx.media3.effect.BitmapOverlay;
import java.util.*;
public class CaptionOverlay extends BitmapOverlay {
 public static class Phrase {public List<Word> words=new ArrayList<>();public double start(){return words.get(0).start;}public double end(){return words.get(words.size()-1).end;}public String text(){StringBuilder s=new StringBuilder();for(Word w:words)s.append(w.text).append(' ');return s.toString().trim();}}
 public static List<Phrase> phrases(List<Word> words){List<Phrase> result=new ArrayList<>();Phrase p=new Phrase();for(Word w:words){if(!p.words.isEmpty()&&(p.words.size()>=4||w.start-p.end()>.5||p.words.get(p.words.size()-1).text.matches(".*[.!?]$"))){result.add(p);p=new Phrase();}p.words.add(w);}if(!p.words.isEmpty())result.add(p);return result;}
 private final List<Phrase> phrases;private final int style;private final Bitmap bitmap;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private int activePhrase=-2,activeWord=-2;
 public CaptionOverlay(List<Word> words,int style,int width,int height){bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);phrases=phrases(words);this.style=style;paint.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));}
 @Override public synchronized Bitmap getBitmap(long presentationTimeUs){double sec=presentationTimeUs/1e6;int pi=-1,wi=-1;for(int i=0;i<phrases.size();i++){Phrase p=phrases.get(i);if(sec>=p.start()&&sec<p.end()){pi=i;for(int j=0;j<p.words.size();j++)if(sec>=p.words.get(j).start&&sec<p.words.get(j).end)wi=j;break;}}
  if(pi==activePhrase&&wi==activeWord)return bitmap;activePhrase=pi;activeWord=wi;bitmap.eraseColor(Color.TRANSPARENT);if(pi<0)return bitmap;
  Canvas c=new Canvas(bitmap);c.scale(bitmap.getWidth()/540f,bitmap.getHeight()/960f);Phrase p=phrases.get(pi);paint.setTextSize(37);float max=430;
  // Shrink unusually long words so even a corrected name stays inside the frame.
  for(Word w:p.words){float measured=paint.measureText(w.text.toUpperCase(Locale.ROOT));if(measured>max)paint.setTextSize(paint.getTextSize()*max/measured);}
  List<List<Integer>> lines=new ArrayList<>();List<Integer> line=new ArrayList<>();float width=0;
  for(int i=0;i<p.words.size();i++){float w=paint.measureText(p.words.get(i).text.toUpperCase(Locale.ROOT)+" ");if(width+w>max&&!line.isEmpty()){lines.add(line);line=new ArrayList<>();width=0;}line.add(i);width+=w;}if(!line.isEmpty())lines.add(line);
  float y=700-(lines.size()-1)*23;for(List<Integer> row:lines){float total=0;for(int i:row)total+=paint.measureText(p.words.get(i).text.toUpperCase(Locale.ROOT)+" ");float x=(540-total)/2;
   for(int i:row){String t=p.words.get(i).text.toUpperCase(Locale.ROOT);float w=paint.measureText(t+" ");paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeWidth(7);paint.setColor(Color.BLACK);c.drawText(t,x,y,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(style==1?Color.rgb(255,223,82):(style==0&&i==wi?Color.rgb(207,240,134):Color.WHITE));c.drawText(t,x,y,paint);x+=w;}y+=46;}
  return bitmap;
 }
}
