#include <jni.h>
#include <whisper.h>
#include <sstream>
#include <algorithm>
#include <string>
static std::string escape(const char* s) { std::string o; for (;*s;++s) { unsigned char c=*s; if(c=='"'||c=='\\')o+='\\'; if(c>=32)o+=c; else if(c=='\n')o+=' '; } return o; }
extern "C" JNIEXPORT jstring JNICALL Java_com_eightbittrade_clipforge_Speech_recognize(JNIEnv* env,jclass,jstring path,jfloatArray samples,jint threads) {
 const char* p=env->GetStringUTFChars(path,nullptr);
 auto cp=whisper_context_default_params(); cp.use_gpu=false;
 auto* ctx=whisper_init_from_file_with_params(p,cp); env->ReleaseStringUTFChars(path,p);
 if(!ctx) { jclass e=env->FindClass("java/lang/IllegalStateException"); env->ThrowNew(e,"Speech model could not be opened. Delete and download it again."); return nullptr; }
 auto params=whisper_full_default_params(WHISPER_SAMPLING_BEAM_SEARCH);
 params.n_threads=std::max(1,(int)threads); params.language="en"; params.translate=false;
 params.print_progress=false; params.print_realtime=false; params.print_timestamps=false; params.print_special=false;
 params.token_timestamps=true; params.split_on_word=true; params.no_context=true;
 params.suppress_blank=true; params.suppress_nst=true; params.beam_search.beam_size=5;
 jfloat* audio=env->GetFloatArrayElements(samples,nullptr);
 int result=whisper_full(ctx,params,audio,env->GetArrayLength(samples)); env->ReleaseFloatArrayElements(samples,audio,JNI_ABORT);
 if(result!=0){whisper_free(ctx); env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),"Speech recognition failed. Try a shorter clip or the Quick model.");return nullptr;}
 std::ostringstream out; out<<"["; bool first=true;
 for(int i=0;i<whisper_full_n_segments(ctx);i++) for(int t=0;t<whisper_full_n_tokens(ctx,i);t++) {
  auto d=whisper_full_get_token_data(ctx,i,t); if(d.id>=whisper_token_eot(ctx))continue;
  const char* text=whisper_full_get_token_text(ctx,i,t); if(!text||!text[0])continue;
  auto a=d.t0>=0?d.t0:whisper_full_get_segment_t0(ctx,i); auto b=d.t1>=a?d.t1:whisper_full_get_segment_t1(ctx,i);
  if(!first)out<<",";first=false;out<<"{\"text\":\""<<escape(text)<<"\",\"start\":"<<a/100.0<<",\"end\":"<<b/100.0<<",\"confidence\":"<<d.p<<"}";
 }
 out<<"]"; whisper_free(ctx); return env->NewStringUTF(out.str().c_str());
}
