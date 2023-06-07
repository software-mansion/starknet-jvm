#include <jni.h>

#include "./starknet_crypto_Poseidon.h"
#include "f251.h"
#include "poseidon.h"
#include "poseidon_rc.h"

JNIEXPORT jobjectArray JNICALL Java_com_swmansion_starknet_crypto_Poseidon_hades
  (JNIEnv *env, jclass obj, jobjectArray values) {

    jsize num_rows = (*env)->GetArrayLength(env, values);
    jsize num_cols = (*env)->GetArrayLength(env, (*env)->GetObjectArrayElement(env, values, 0));

    felt_t converted_values[3] = { {0} };
    for (int i = 0; i < num_rows; i++) {
        jlongArray row = (*env)->GetObjectArrayElement(env, values, i);
        jlong *row_elems = (*env)->GetLongArrayElements(env, row, NULL);
        for (int j = 0; j < num_cols; j++) {
            converted_values[i][j] = (uint64_t) row_elems[j];
        }
        (*env)->ReleaseLongArrayElements(env, row, row_elems, JNI_ABORT);
    }

    permutation_3(converted_values);

    jclass longArrayClass = (*env)->FindClass(env, "[J");
    jobjectArray result = (*env)->NewObjectArray(env, 3, longArrayClass, NULL);
    for (int i = 0; i < 3; i++) {
        jlongArray row = (*env)->NewLongArray(env, 4);
        jlong* elements = (*env)->GetLongArrayElements(env, row, NULL);
        for (int j = 0; j < 4; j++) {
            elements[j] = (jlong)converted_values[i][j];
        }
        (*env)->ReleaseLongArrayElements(env, row, elements, 0);
        (*env)->SetObjectArrayElement(env, result, i, row);
    }

    return result;
  }
