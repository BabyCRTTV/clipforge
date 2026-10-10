package com.eightbittrade.clipforge;
import android.content.Context;
import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.json.JSONArray;
/** App-owned options and runtime. No unknown CLI flags can be silently dropped. */
public final class YouTube {
 private static PyObject loadedEngine;
 public static class Callback {
  public final AtomicBoolean cancellation; private final Consumer<String> progress;
  public Callback(AtomicBoolean cancellation,Consumer<String> progress){this.cancellation=cancellation;this.progress=progress;}
  public boolean cancelled(){return cancellation.get();}
  public void message(String message){progress.accept(message);}
 }
 public static synchronized PyObject engine(Context context)throws Exception{
  if(loadedEngine!=null)return loadedEngine;
  if(!Python.isStarted())Python.start(new AndroidPlatform(context.getApplicationContext()));
  File dir=new File(context.getCodeCacheDir(),"youtube-2026.8.19-ejs0.8.0");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Not enough storage to initialize the downloader.");
  String[] names={"yt_dlp-2026.8.19-py3-none-any.whl","yt_dlp_ejs-0.8.0-py3-none-any.whl","clipforge_download.py"};
  String[] hashes={"1d57897e94c6665a0a6f9bc54b34e584284e32c034ffab3a7df25d8f7b24eedf","79300e5fca7f937a1eeede11f0456862c1b41107ce1d726871e0207424f4bdb4",null};
  for(int i=0;i<names.length;i++){File dest=new File(dir,names[i]);if(hashes[i]==null||!dest.exists()||!hashes[i].equals(hash(dest))){File temp=new File(dir,names[i]+".tmp");try(InputStream in=context.getAssets().open("downloader/"+names[i]);OutputStream out=new FileOutputStream(temp)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}if(hashes[i]!=null&&!hashes[i].equals(hash(temp)))throw new IOException("Downloader integrity check failed.");java.nio.file.Files.move(temp.toPath(),dest.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}
  Python py=Python.getInstance();PyObject sys=py.getModule("sys");
  // Change the real interpreter path directly: exec's frame/globals behavior differs on Android.
  for(int i=names.length-1;i>=0;i--){String path=i==2?dir.getPath():new File(dir,names[i]).getPath();sys.get("path").callAttr("insert",0,path);}
  py.getModule("importlib").callAttr("invalidate_caches");
  PyObject util=py.getModule("importlib.util");
  PyObject spec=util.callAttr("spec_from_file_location","clipforge_download",new File(dir,"clipforge_download.py").getPath());
  PyObject module=util.callAttr("module_from_spec",spec);
  sys.get("modules").callAttr("__setitem__","clipforge_download",module);
  spec.get("loader").callAttr("exec_module",module);
  loadedEngine=module;return loadedEngine;
 }
 private static String hash(File file)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
 public static String metadata(Context ctx,String link,Callback callback)throws Exception{return engine(ctx).callAttr("metadata",link,callback).toString();}
 public static void download(Context ctx,String link,String format,File dest,Callback callback)throws Exception{engine(ctx).callAttr("download",link,format,dest.getPath(),callback);if(callback.cancelled())throw new InterruptedException();if(!dest.exists()||dest.length()==0)throw new IOException("YouTube did not return a complete stream. Try again.");}
}
