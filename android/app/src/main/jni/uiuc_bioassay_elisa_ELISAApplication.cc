#include "uiuc_bioassay_elisa_ELISAApplication.h"
#include "elisa.h"

JNIEXPORT void JNICALL
Java_uiuc_bioassay_elisa_ELISAApplication_cleanFolder(JNIEnv *env, jclass,
                                                      jstring jstr) {
  const char *path = env->GetStringUTFChars(jstr, nullptr);
  std::string cmd("exec rm -r ");
  cmd += path;
  cmd += "/*";
  LOGD("%s", cmd.c_str());
  system(cmd.c_str());
  env->ReleaseStringUTFChars(jstr, path);
}

JNIEXPORT jdoubleArray JNICALL
Java_uiuc_bioassay_elisa_ELISAApplication_processBBELISA(JNIEnv *env, jclass,
                                                         jstring jstr) {
  const char *path = env->GetStringUTFChars(jstr, nullptr);
  int ret = elisa::process_bb(path);
  LOGD("%s", path);
  env->ReleaseStringUTFChars(jstr, path);
  return nullptr;
}

JNIEXPORT jdoubleArray JNICALL
Java_uiuc_bioassay_elisa_ELISAApplication_processSampleELISA(JNIEnv *, jclass,
                                                             jstring) {
  return nullptr;
}
