package com.eightbittrade.clipforge;
import android.media.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
public class Speech {
 static {System.loadLibrary("clipforge");}
 static native String recognize(String path,float[] pcm,int threads);
 public interface Progress {void update(String message);}
 public static File model(File dir,boolean quick,AtomicBoolean cancel,Progress progress)throws Exception{
  String name=quick?"base.en":"small.en";File finalFile=new File(dir,"ggml-"+name+".bin");if(finalFile.exists())return finalFile;dir.mkdirs();File partial=new File(dir,name+".part");
  String expected=quick?"a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002":"c6138d6d58ecc8322097e0f987c32f1be8bb0a18532a3f88f734d1bbf9c41e5d";
  HttpURLConnection connection=(HttpURLConnection)new URL("https://huggingface.co/ggerganov/whisper.cpp/resolve/5359861c739e955e79d9a303bcbc70fb988958b1/ggml-"+name+".bin").openConnection();connection.setConnectTimeout(20000);connection.setReadTimeout(30000);long size=connection.getContentLengthLong(),total=0;MessageDigest digest=MessageDigest.getInstance("SHA-256");
  try(InputStream input=connection.getInputStream();OutputStream out=new FileOutputStream(partial)){byte[] buf=new byte[131072];int n;long previous=0;while((n=input.read(buf))!=-1){if(cancel.get())throw new InterruptedException();out.write(buf,0,n);digest.update(buf,0,n);total+=n;if(total-previous>2097152){progress.update("Downloading "+name+" · "+total/1048576+" MB"+(size>0?" / "+size/1048576+" MB":""));previous=total;}}}finally{connection.disconnect();}
  StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b));if(!hash.toString().equals(expected)){partial.delete();throw new IOException("Speech model verification failed. Please try the download again.");}if(!partial.renameTo(finalFile))throw new IOException("Unable to save speech model.");return finalFile;
 }
 public static List<Word> transcribe(File source,double start,double end,File model,AtomicBoolean cancel,Progress progress)throws Exception{
  MediaExtractor extractor=new MediaExtractor();MediaCodec codec=null;List<Word> all=new ArrayList<>();
  try{extractor.setDataSource(source.getPath());MediaFormat format=null;for(int i=0;i<extractor.getTrackCount();i++){MediaFormat f=extractor.getTrackFormat(i);if(f.getString(MediaFormat.KEY_MIME).startsWith("audio/")){extractor.selectTrack(i);format=f;break;}}if(format==null)throw new IOException("This video has no audio track. You can add captions manually.");
   int rate=format.getInteger(MediaFormat.KEY_SAMPLE_RATE),channels=format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);codec=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));codec.configure(format,null,null,0);codec.start();extractor.seekTo((long)(start*1e6),MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
   boolean inputEnd=false,outputEnd=false;MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();FloatBuffer samples=FloatBuffer.allocate(16000*60);double chunkStart=start;long timeout=System.currentTimeMillis();
   while(!outputEnd){if(cancel.get())throw new InterruptedException();if(System.currentTimeMillis()-timeout>60000)throw new IOException("Audio decoder stalled. Try another video format.");
    if(!inputEnd){int idx=codec.dequeueInputBuffer(10000);if(idx>=0){ByteBuffer b=codec.getInputBuffer(idx);int n=extractor.readSampleData(b,0);long t=extractor.getSampleTime();if(n<0||t>=end*1e6){codec.queueInputBuffer(idx,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnd=true;}else{codec.queueInputBuffer(idx,0,n,t,0);extractor.advance();}}}
    int idx=codec.dequeueOutputBuffer(info,10000);if(idx==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat f=codec.getOutputFormat();rate=f.getInteger(MediaFormat.KEY_SAMPLE_RATE);channels=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);}else if(idx>=0){timeout=System.currentTimeMillis();ByteBuffer b=codec.getOutputBuffer(idx).order(ByteOrder.LITTLE_ENDIAN);b.position(info.offset);b.limit(info.offset+info.size);MediaFormat f=codec.getOutputFormat();boolean isFloat=f.containsKey(MediaFormat.KEY_PCM_ENCODING)&&f.getInteger(MediaFormat.KEY_PCM_ENCODING)==AudioFormat.ENCODING_PCM_FLOAT;int bytes=isFloat?4:2;int frames=info.size/(bytes*channels);
     double packetStart=info.presentationTimeUs/1e6,packetEnd=packetStart+frames/(double)rate,next=chunkStart+samples.position()/16000.0;
     while(next<Math.min(end,packetEnd)){if(next<packetStart){if(packetStart-next<.1){samples.put(0);next=chunkStart+samples.position()/16000.0;}else{next=packetStart;chunkStart=next-samples.position()/16000.0;}}else{double pos=(next-packetStart)*rate;int a=(int)pos,bb=Math.min(frames-1,a+1);if(a>=frames)break;float va=0,vb=0;for(int c=0;c<channels;c++){int oa=info.offset+(a*channels+c)*bytes,ob=info.offset+(bb*channels+c)*bytes;va+=isFloat?b.getFloat(oa):b.getShort(oa)/32768f;vb+=isFloat?b.getFloat(ob):b.getShort(ob)/32768f;}samples.put((float)((va+(vb-va)*(pos-a))/channels));next=chunkStart+samples.position()/16000.0;}
      if(!samples.hasRemaining()){process(samples,chunkStart-start,model,all,progress);chunkStart+=60;samples.clear();next=chunkStart;timeout=System.currentTimeMillis();}}
     outputEnd=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;codec.releaseOutputBuffer(idx,false);}
   }
   if(samples.position()>1600)process(samples,chunkStart-start,model,all,progress);return all;
  }finally{if(codec!=null){try{codec.stop();}catch(Exception ignored){}codec.release();}extractor.release();}
 }
 private static void process(FloatBuffer pcm,double offset,File model,List<Word> out,Progress progress)throws Exception{
  float[] samples=Arrays.copyOf(pcm.array(),pcm.position());double energy=0;for(float f:samples)energy+=f*f;if(energy/samples.length<.000005)return;
  progress.update("Recognizing speech · "+(int)offset+" seconds analyzed. Keep ClipForge open.");JSONArray tokens=new JSONArray(recognize(model.getPath(),samples,Math.min(6,Math.max(2,Runtime.getRuntime().availableProcessors()-2))));Word current=null;
  for(int i=0;i<tokens.length();i++){JSONObject t=tokens.getJSONObject(i);String raw=t.getString("text");double s=t.getDouble("start")+offset,e=t.getDouble("end")+offset,confidence=t.getDouble("confidence");if(raw.trim().isEmpty())continue;
   if(raw.startsWith(" ")||current==null){if(current!=null)out.add(current);current=new Word(raw.trim(),Math.max(offset,s),Math.min(offset+samples.length/16000.0,e),confidence);}else{current.text+=raw;current.end=Math.max(current.end,e);current.confidence=Math.min(current.confidence,confidence);}}if(current!=null)out.add(current);
 }
}
