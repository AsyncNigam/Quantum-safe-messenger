#include <jni.h>
#include <string>
#include <sstream>
#include <vector>
#include <oqs/oqs.h>

// ─── ML-KEM-768 Key Generation ───────────────────────────────────────────────
// Returns a 2-element byte[][] — [0] = public key, [1] = private key
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_nigdroid_quantummessenger_crypto_PostQuantumCrypto_jniGenerateKemKeypair(
        JNIEnv* env, jobject) {

    OQS_init();
    OQS_KEM* kem = OQS_KEM_new(OQS_KEM_alg_ml_kem_768);

    jbyteArray pubArray  = env->NewByteArray((jsize)kem->length_public_key);
    jbyteArray privArray = env->NewByteArray((jsize)kem->length_secret_key);

    auto* pub  = new uint8_t[kem->length_public_key];
    auto* priv = new uint8_t[kem->length_secret_key];

    OQS_KEM_keypair(kem, pub, priv);

    env->SetByteArrayRegion(pubArray,  0, (jsize)kem->length_public_key,
            reinterpret_cast<jbyte*>(pub));
    env->SetByteArrayRegion(privArray, 0, (jsize)kem->length_secret_key,
            reinterpret_cast<jbyte*>(priv));

    // Return as byte[][]
    jclass byteArrayClass = env->FindClass("[B");
    jobjectArray result = env->NewObjectArray(2, byteArrayClass, nullptr);
    env->SetObjectArrayElement(result, 0, pubArray);
    env->SetObjectArrayElement(result, 1, privArray);

    // Wipe private key from native memory immediately
    OQS_MEM_secure_free(priv, kem->length_secret_key);
    delete[] pub;
    OQS_KEM_free(kem);

    return result;
}

// ─── ML-KEM-768 Encapsulation ─────────────────────────────────────────────────
// Given recipient's public key → returns [0] = ciphertext, [1] = shared secret
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_nigdroid_quantummessenger_crypto_PostQuantumCrypto_jniKemEncapsulate(
        JNIEnv* env, jobject,
        jbyteArray recipientPublicKey) {

    OQS_KEM* kem = OQS_KEM_new(OQS_KEM_alg_ml_kem_768);

    auto* ciphertext   = new uint8_t[kem->length_ciphertext];
    auto* sharedSecret = new uint8_t[kem->length_shared_secret];

    jbyte* pubKeyBytes = env->GetByteArrayElements(recipientPublicKey, nullptr);

    OQS_KEM_encaps(kem,
            ciphertext,
            sharedSecret,
            reinterpret_cast<uint8_t*>(pubKeyBytes));

    env->ReleaseByteArrayElements(recipientPublicKey, pubKeyBytes, JNI_ABORT);

    jbyteArray ctArray = env->NewByteArray((jsize)kem->length_ciphertext);
    jbyteArray ssArray = env->NewByteArray((jsize)kem->length_shared_secret);

    env->SetByteArrayRegion(ctArray, 0, (jsize)kem->length_ciphertext,
            reinterpret_cast<jbyte*>(ciphertext));
    env->SetByteArrayRegion(ssArray, 0, (jsize)kem->length_shared_secret,
            reinterpret_cast<jbyte*>(sharedSecret));

    jclass byteArrayClass = env->FindClass("[B");
    jobjectArray result = env->NewObjectArray(2, byteArrayClass, nullptr);
    env->SetObjectArrayElement(result, 0, ctArray);
    env->SetObjectArrayElement(result, 1, ssArray);

    OQS_MEM_secure_free(sharedSecret, kem->length_shared_secret);
    delete[] ciphertext;
    OQS_KEM_free(kem);

    return result;
}

// ─── ML-KEM-768 Decapsulation ─────────────────────────────────────────────────
// Given our private key + ciphertext → returns shared secret bytes
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_nigdroid_quantummessenger_crypto_PostQuantumCrypto_jniKemDecapsulate(
        JNIEnv* env, jobject,
        jbyteArray ciphertext,
        jbyteArray privateKey) {

    OQS_KEM* kem = OQS_KEM_new(OQS_KEM_alg_ml_kem_768);

    auto* sharedSecret = new uint8_t[kem->length_shared_secret];

    jbyte* ctBytes   = env->GetByteArrayElements(ciphertext,  nullptr);
    jbyte* privBytes = env->GetByteArrayElements(privateKey,  nullptr);

    OQS_KEM_decaps(kem,
            sharedSecret,
            reinterpret_cast<uint8_t*>(ctBytes),
            reinterpret_cast<uint8_t*>(privBytes));

    env->ReleaseByteArrayElements(ciphertext, ctBytes,   JNI_ABORT);
    env->ReleaseByteArrayElements(privateKey, privBytes, JNI_ABORT);

    jbyteArray result = env->NewByteArray((jsize)kem->length_shared_secret);
    env->SetByteArrayRegion(result, 0, (jsize)kem->length_shared_secret,
            reinterpret_cast<jbyte*>(sharedSecret));

    OQS_MEM_secure_free(sharedSecret, kem->length_shared_secret);
    OQS_KEM_free(kem);

    return result;
}