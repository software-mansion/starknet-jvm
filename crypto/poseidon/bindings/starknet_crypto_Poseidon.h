#include <jni.h>

#ifndef _Included_com_swmansion_starknet_crypto_Poseidon
#define _Included_com_swmansion_starknet_crypto_Poseidon
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobjectArray JNICALL Java_com_swmansion_starknet_crypto_Poseidon_hades
  (JNIEnv *env, jclass obj, jobjectArray values);

#ifdef __cplusplus
}
#endif
#endif
