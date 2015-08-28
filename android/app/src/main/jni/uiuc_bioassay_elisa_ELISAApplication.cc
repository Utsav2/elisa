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

JNIEXPORT jdoubleArray JNICALL 
Java_uiuc_bioassay_elisa_ELISAApplication_readRGBSpec
  (JNIEnv *env, jclass, jstring jstr) {
  const char *path = env->GetStringUTFChars(jstr, nullptr);
  std::vector<double> rgb_spec = elisa::read_rgb_spec(path);
  env->ReleaseStringUTFChars(jstr, path);
  jdoubleArray res;
  res = env->NewDoubleArray(rgb_spec.size());
  if (res == nullptr) {
    return nullptr;
  }
  env->SetDoubleArrayRegion(res, 0, rgb_spec.size(), &rgb_spec[0]);
  return res;
}

JNIEXPORT jdoubleArray JNICALL 
Java_uiuc_bioassay_elisa_ELISAApplication_readBBResNormalized
  (JNIEnv *env, jclass, jstring jstr) {
  const char *path = env->GetStringUTFChars(jstr, nullptr);
  std::vector<double> bb_res = elisa::read_res(path, true);
  env->ReleaseStringUTFChars(jstr, path);
  jdoubleArray res;
  res = env->NewDoubleArray(bb_res.size());
  if (res == nullptr) {
    return nullptr;
  }
  env->SetDoubleArrayRegion(res, 0, bb_res.size(), &bb_res[0]);
  return res; 
}

JNIEXPORT jint JNICALL 
Java_uiuc_bioassay_elisa_ELISAApplication_processSample(JNIEnv *env, jclass,
                                                             jstring jstr) {
  const char *path = env->GetStringUTFChars(jstr, nullptr);
  int ret = elisa::process_sample(path);
  env->ReleaseStringUTFChars(jstr, path);
  return ret;
}

JNIEXPORT jdoubleArray JNICALL 
Java_uiuc_bioassay_elisa_ELISAApplication_readSampleResNormalized
  (JNIEnv *env, jclass, jstring jstr) {
  const char *path = env->GetStringUTFChars(jstr, nullptr);
  std::vector<double> sample_res = elisa::read_res(path, false);
  env->ReleaseStringUTFChars(jstr, path);
  jdoubleArray res;
  res = env->NewDoubleArray(sample_res.size());
  if (res == nullptr) {
    return nullptr;
  }
  env->SetDoubleArrayRegion(res, 0, sample_res.size(), &sample_res[0]);
  return res;   
}
