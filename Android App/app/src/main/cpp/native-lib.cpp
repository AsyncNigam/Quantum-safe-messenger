#include <jni.h>
#include <string>
#include <sstream>
#include <oqs/oqs.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_nigdroid_quantummessenger_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    // Initialize liboqs
    OQS_init();

    // Create an ML-KEM-768 key encapsulation object
    OQS_KEM *kem = OQS_KEM_new(OQS_KEM_alg_ml_kem_768);

    if (kem == nullptr) {
        return env->NewStringUTF("ERROR: ML-KEM-768 not available.");
    }

    // Allocate buffers for the keypair
    uint8_t *public_key  = new uint8_t[kem->length_public_key];
    uint8_t *private_key = new uint8_t[kem->length_secret_key];

    // Generate the keypair
    OQS_STATUS status = OQS_KEM_keypair(kem, public_key, private_key);

    std::string result;

    if (status == OQS_SUCCESS) {
        // Convert first 16 bytes of public key to hex to display
        std::ostringstream oss;
        oss << "ML-KEM-768 keypair generated!\n";
        oss << "Public key length: " << kem->length_public_key << " bytes\n";
        oss << "First 16 bytes: ";
        for (int i = 0; i < 16; i++) {
            char buf[3];
            snprintf(buf, sizeof(buf), "%02x", public_key[i]);
            oss << buf;
        }
        result = oss.str();
    } else {
        result = "ERROR: Keypair generation failed.";
    }

    // Clean up — never leave key material in memory
    OQS_MEM_secure_free(private_key, kem->length_secret_key);
    delete[] public_key;
    OQS_KEM_free(kem);

    return env->NewStringUTF(result.c_str());
}