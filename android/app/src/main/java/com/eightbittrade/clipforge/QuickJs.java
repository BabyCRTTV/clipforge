package com.eightbittrade.clipforge;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
/** Core JavaScript only: no filesystem, process, or networking bindings. */
public final class QuickJs {
 static { System.loadLibrary("clipforge"); }
 private static native byte[] run(byte[] script,AtomicBoolean cancel);
 public static String evaluate(String script,AtomicBoolean cancel){
  AtomicReference<byte[]> output=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();
  // Player parsers recurse more deeply than Android's default Java thread stack allows.
  Thread engine=new Thread(null,()->{try{output.set(run(script.getBytes(StandardCharsets.UTF_8),cancel));}catch(Throwable e){failure.set(e);}},"ClipForge-player",8*1024*1024);
  engine.start();boolean interrupted=false;
  for(;;){try{engine.join();break;}catch(InterruptedException e){interrupted=true;if(cancel!=null)cancel.set(true);}}
  if(interrupted)Thread.currentThread().interrupt();Throwable error=failure.get();if(error!=null){if(error instanceof RuntimeException)throw (RuntimeException)error;throw new IllegalStateException("YouTube player processing failed",error);}
  return new String(output.get(),StandardCharsets.UTF_8);
 }
}
