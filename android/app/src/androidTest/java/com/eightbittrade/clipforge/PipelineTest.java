package com.eightbittrade.clipforge;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.media3.common.*;
import androidx.media3.effect.*;
import androidx.media3.transformer.*;
import com.google.common.collect.ImmutableList;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class PipelineTest {
 private Context ctx;private File source,model;
 @Before public void fixture()throws Exception{ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();source=asset("fixture.mp4");model=asset("ggml-base.en.bin");}
 private File asset(String name)throws Exception{File f=new File(ctx.getCacheDir(),name);if(!f.exists())try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name);OutputStream out=new FileOutputStream(f)){byte[] copyBuffer=new byte[65536];int copied;while((copied=in.read(copyBuffer))!=-1)out.write(copyBuffer,0,copied);}return f;}
 @Test public void recognizesSpeechExportsPortraitAndSavesGallery()throws Exception{
  List<Word> words=Speech.transcribe(source,0,11,model,new AtomicBoolean(),m->{});StringBuilder transcript=new StringBuilder();for(Word w:words){transcript.append(w.text).append(' ');assertTrue(w.start>=0);assertTrue(w.end>w.start);assertTrue(w.start<11.1);}String text=transcript.toString().toLowerCase(Locale.ROOT);assertTrue("Recognized: "+text,text.contains("country"));assertTrue(text.contains("ask"));assertTrue(words.size()>8);
  android.app.Activity activity=InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(ctx,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{try{set(activity,"source",source);set(activity,"meta",VideoTools.inspect(source));set(activity,"title","Speech test · guided preview");set(activity,"start",0.0);set(activity,"end",11.0);set(activity,"words",words);set(activity,"picks",Highlights.recommend(11,Collections.emptyList(),words));}catch(Exception e){throw new RuntimeException(e);}});
  for(int step=0;step<4;step++){final int screen=step;InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{try{set(activity,"step",screen);java.lang.reflect.Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);render.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(OutputStream out=new FileOutputStream(new File(ctx.getFilesDir(),"qa-step-"+step+".png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
  CountDownLatch previewReady=new CountDownLatch(1);AtomicReference<Exception> previewError=new AtomicReference<>();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{try{java.lang.reflect.Method refresh=MainActivity.class.getDeclaredMethod("refreshPreview");refresh.setAccessible(true);refresh.invoke(activity);java.lang.reflect.Field f=MainActivity.class.getDeclaredField("player");f.setAccessible(true);androidx.media3.exoplayer.ExoPlayer p=(androidx.media3.exoplayer.ExoPlayer)f.get(activity);p.addListener(new Player.Listener(){@Override public void onRenderedFirstFrame(){previewReady.countDown();}@Override public void onPlayerError(PlaybackException e){previewError.set(e);previewReady.countDown();}});p.setVolume(0);p.play();}catch(Exception e){previewError.set(e);previewReady.countDown();}});
  assertTrue("Preview never rendered a frame",previewReady.await(45,TimeUnit.SECONDS));if(previewError.get()!=null)throw previewError.get();
  InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.graphics.Bitmap previewImage=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(OutputStream out=new FileOutputStream(new File(ctx.getFilesDir(),"qa-step-3.png"))){previewImage.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}previewImage.recycle();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);

  CountDownLatch finished=new CountDownLatch(1);AtomicReference<Exception> error=new AtomicReference<>();File output=new File(ctx.getCacheDir(),"pipeline-portrait.mp4");output.delete();
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{float ratio=(360*9f/16)/640;List<Effect> effects=Arrays.asList(new Crop(-ratio,ratio,-1,1),Presentation.createForWidthAndHeight(198,352,Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP),new OverlayEffect(ImmutableList.of(new CaptionOverlay(words,0,198,352))));EditedMediaItem item=new EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(source))).setEffects(new Effects(ImmutableList.of(),effects)).build();Transformer t=new Transformer.Builder(ctx).setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC).addListener(new Transformer.Listener(){@Override public void onCompleted(Composition c,ExportResult r){finished.countDown();}@Override public void onError(Composition c,ExportResult r,ExportException e){error.set(e);finished.countDown();}}).build();t.start(item,output.getPath());});
  assertTrue("Export timed out",finished.await(5,TimeUnit.MINUTES));if(error.get()!=null)throw error.get();assertTrue(output.length()>1000);VideoTools.Meta m=VideoTools.inspect(output);assertEquals(198,m.width);assertEquals(352,m.height);assertEquals(11,m.duration,.2);assertEquals(15,m.fps,1);
  Uri gallery=VideoTools.save(ctx,output);try(InputStream in=ctx.getContentResolver().openInputStream(gallery)){assertNotNull(in);assertTrue(in.read()!=-1);}finally{ctx.getContentResolver().delete(gallery,null,null);}assertTrue(CaptionOverlay.phrases(words).size()>=3);
 }
 private void set(Object target,String name,Object value)throws Exception{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
 @Test public void embeddedDownloaderStartsWithoutPermissions()throws Exception{dev.ffmpegkit_maintained.ytdlp.YtDlp.init(ctx);assertTrue(com.chaquo.python.Python.getInstance().getModule("sys").get("version").toString().startsWith("3.13"));assertNotNull(com.chaquo.python.Python.getInstance().getModule("yt_dlp").get("YoutubeDL"));}
 @Test public void declaredPermissionsAreMinimal()throws Exception{String[] permissions=ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;Set<String>p=new HashSet<>(Arrays.asList(permissions));assertTrue(p.contains("android.permission.INTERNET"));for(String forbidden:Arrays.asList("android.permission.RECORD_AUDIO","android.permission.READ_MEDIA_VIDEO","android.permission.READ_EXTERNAL_STORAGE","android.permission.MANAGE_EXTERNAL_STORAGE","android.permission.ACCESS_FINE_LOCATION","android.permission.READ_CONTACTS"))assertFalse(forbidden,p.contains(forbidden));}
}
