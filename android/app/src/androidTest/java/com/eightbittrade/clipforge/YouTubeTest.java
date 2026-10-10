package com.eightbittrade.clipforge;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import static org.junit.Assert.*;
/** Explicit live-network check; enabled by CI argument, never mocks YouTube. */
@RunWith(AndroidJUnit4.class)
public class YouTubeTest {
 @Test public void realPublicVideoDownloadsPlayableBytes()throws Exception{
  org.junit.Assume.assumeTrue("Live YouTube check is opt-in", "true".equals(InstrumentationRegistry.getArguments().getString("youtubeLive")));
  Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
  YouTube.Callback callback=new YouTube.Callback(new AtomicBoolean(),m->android.util.Log.i("ClipForgeYouTubeTest",m));
  for(String link:new String[]{"https://youtu.be/3BbW95fQ35Y?is=lDKfTr3nNaS5pmRx","https://www.youtube.com/watch?v=jNQXAC9IVRw","https://www.youtube.com/shorts/jNQXAC9IVRw"})assertFalse(new JSONObject(YouTube.metadata(ctx,link,callback)).getString("title").isEmpty());
  File video=new File(ctx.getCacheDir(),"youtube-live-video.mp4"),audio=new File(ctx.getCacheDir(),"youtube-live-audio.m4a"),merged=new File(ctx.getCacheDir(),"youtube-live-source.mp4");
  YouTube.download(ctx,"https://youtu.be/jNQXAC9IVRw","worstvideo[ext=mp4][vcodec^=avc1]/worst[ext=mp4]",video,callback);
  YouTube.download(ctx,"https://youtu.be/jNQXAC9IVRw","bestaudio[ext=m4a]/best[ext=mp4]",audio,callback);
  assertTrue(video.length()>1000);assertTrue(audio.length()>1000);VideoTools.mux(video,audio,merged);VideoTools.Meta meta=VideoTools.inspect(merged);assertTrue(meta.duration>1);assertTrue(meta.width>0);video.delete();audio.delete();merged.delete();
 }
}
