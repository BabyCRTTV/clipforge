#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "quickjs.h"
typedef struct { double deadline; char *output; size_t length; JNIEnv *env; jobject cancel; jmethodID get; int ticks; } State;
static double now(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return t.tv_sec+t.tv_nsec/1e9;}
static int interrupt(JSRuntime *rt,void *opaque){State *s=opaque;if(now()>s->deadline)return 1;return s->cancel && (++s->ticks%32==0) && (*s->env)->CallBooleanMethod(s->env,s->cancel,s->get);}
static JSValue log_json(JSContext *ctx,JSValueConst self,int argc,JSValueConst *argv){State *s=JS_GetContextOpaque(ctx);if(!argc)return JS_UNDEFINED;size_t n;const char *text=JS_ToCStringLen(ctx,&n,argv[0]);if(!text)return JS_EXCEPTION;if(n>4*1024*1024){JS_FreeCString(ctx,text);return JS_ThrowRangeError(ctx,"JavaScript output exceeds limit");}free(s->output);s->output=malloc(n+1);if(!s->output){JS_FreeCString(ctx,text);return JS_ThrowOutOfMemory(ctx);}memcpy(s->output,text,n);s->output[n]=0;s->length=n;JS_FreeCString(ctx,text);return JS_UNDEFINED;}
JNIEXPORT jbyteArray JNICALL Java_com_eightbittrade_clipforge_QuickJs_run(JNIEnv *env,jclass cls,jbyteArray input,jobject cancel){
 State s={.deadline=now()+30,.env=env,.cancel=cancel};if(cancel){jclass c=(*env)->GetObjectClass(env,cancel);s.get=(*env)->GetMethodID(env,c,"get","()Z");(*env)->DeleteLocalRef(env,c);}
 JSRuntime *rt=JS_NewRuntime();if(!rt){(*env)->ThrowNew(env,(*env)->FindClass(env,"java/lang/IllegalStateException"),"Could not start JavaScript engine");return NULL;}
 JS_SetMemoryLimit(rt,256*1024*1024);JS_SetMaxStackSize(rt,2*1024*1024);JS_SetInterruptHandler(rt,interrupt,&s);JSContext *ctx=JS_NewContext(rt);if(!ctx){JS_FreeRuntime(rt);(*env)->ThrowNew(env,(*env)->FindClass(env,"java/lang/IllegalStateException"),"Could not create JavaScript context");return NULL;}JS_SetContextOpaque(ctx,&s);
 JSValue global=JS_GetGlobalObject(ctx),console=JS_NewObject(ctx);JS_SetPropertyStr(ctx,console,"log",JS_NewCFunction(ctx,log_json,"log",1));JS_SetPropertyStr(ctx,global,"console",console);JS_FreeValue(ctx,global);
 jsize n=(*env)->GetArrayLength(env,input);char *code=malloc((size_t)n+1);if(!code){JS_FreeContext(ctx);JS_FreeRuntime(rt);(*env)->ThrowNew(env,(*env)->FindClass(env,"java/lang/IllegalStateException"),"JavaScript input exceeds available memory");return NULL;}(*env)->GetByteArrayRegion(env,input,0,n,(jbyte*)code);code[n]=0;
 JSValue result=JS_Eval(ctx,code,n,"youtube-player.js",JS_EVAL_TYPE_GLOBAL);free(code);jbyteArray output=NULL;
 if(JS_IsException(result)){JSValue error=JS_GetException(ctx);const char *message=JS_ToCString(ctx,error);(*env)->ThrowNew(env,(*env)->FindClass(env,"java/lang/IllegalStateException"),message?message:"JavaScript processing failed");if(message)JS_FreeCString(ctx,message);JS_FreeValue(ctx,error);}else if(s.output){output=(*env)->NewByteArray(env,s.length);if(output)(*env)->SetByteArrayRegion(env,output,0,s.length,(jbyte*)s.output);}else{(*env)->ThrowNew(env,(*env)->FindClass(env,"java/lang/IllegalStateException"),"JavaScript produced no result");}
 JS_FreeValue(ctx,result);free(s.output);JS_FreeContext(ctx);JS_FreeRuntime(rt);return output;
}
