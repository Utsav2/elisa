#include "uiuc_bioassay_elisa_ELISAApplication.h"
#include "elisa.h"

JNIEXPORT void JNICALL
Java_uiuc_bioassay_elisa_ELISAApplication_cleanFolder(JNIEnv *env, jclass,
                                                      jstring jstr) {
  const char *path = env->GetStringUTFChars(jstr, nullptr);
  std::string cmd("exec rm -r ");
  cmd += path;
  cmd += "/*";
  system(cmd.c_str());
  env->ReleaseStringUTFChars(jstr, path);
}

JNIEXPORT jint JNICALL 
Java_uiuc_bioassay_elisa_ELISAApplication_processBB(JNIEnv *env, jclass,
                                                         jstring jstr) {
  const char *path = env->GetStringUTFChars(jstr, nullptr);
  int ret = elisa::process_bb(path);
  env->ReleaseStringUTFChars(jstr, path);
  return ret;
}

JNIEXPORT jint JNICALL 
Java_uiuc_bioassay_elisa_ELISAApplication_processSample(JNIEnv *, jclass,
                                                             jstring) {
  return 0;
}
