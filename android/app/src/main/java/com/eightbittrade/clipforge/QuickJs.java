package com.eightbittrade.clipforge;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
/** Core JavaScript only: no filesystem, process, or networking bindings. */
public final class QuickJs {
 static { System.loadLibrary("clipforge"); }
 private static native byte[] run(byte[] script,AtomicBoolean cancel);
 public static String evaluate(String script,AtomicBoolean cancel){return new String(run(script.getBytes(StandardCharsets.UTF_8),cancel),StandardCharsets.UTF_8);}
}
