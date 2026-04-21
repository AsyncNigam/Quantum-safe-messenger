/**
 * AUTHENTICATION & KEY GENERATION ARCHITECTURE
 * ==============================================
 *
 * This document outlines the complete authentication flow for Quantum Messenger,
 * including post-quantum cryptography, secure key management, and clean architecture
 * principles.
 *
 *
 * HIGH-LEVEL FLOW
 * ===============
 *
 * User → AuthScreen → AuthViewModel → AuthRepository → GenerateIdentityUseCase
 *           ↓                                              ↓
 *         Phone Input                          C++ JNI Bridge (ML-KEM/ML-DSA)
 *         Glassmorphism UI                    + Google Tink (X25519/Ed25519)
 *         Secure Button                       + Android Keystore
 *           ↓                                              ↓
 *       AuthService (Retrofit)              AuthRepositoryImpl.registerIdentity()
 *           ↓                                              ↓
 *     Express Server (/auth/register)      Response: userId + serverPublicKey
 *
 *
 * COMPONENTS OVERVIEW
 * ===================
 *
 * 1. DOMAIN LAYER
 *    ============
 *
 *    File: domain/model/AuthModels.kt
 *    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *    - Identity: Domain model containing public key material
 *    - AuthRegisterRequest: Payload to send to server (all keys Base64-encoded)
 *    - AuthRegisterResponse: Response from registration endpoint
 *    - IdentityGenerationResult: Success/Error/Cancelled sealed class
 *    - AuthenticationResult: Success/Error/InvalidInput sealed class
 *
 *    File: domain/usecase/GenerateIdentityUseCase.kt
 *    ────────────────────────────────────────────────
 *    Orchestrates cryptographic identity generation:
 *
 *    suspend operator fun invoke(phoneNumber: String): IdentityGenerationResult
 *
 *    Steps:
 *    1. Validate phone number format
 *    2. Generate ML-KEM keypair (via PostQuantumCrypto JNI)
 *       - Uses NIST FIPS 203 (ML-KEM-768)
 *       - Key Encapsulation Mechanism (KEM) for post-quantum key establishment
 *    3. Generate ML-DSA keypair (via PostQuantumCrypto JNI)
 *       - Uses NIST FIPS 204 (ML-DSA-87)
 *       - Digital signatures for post-quantum authentication
 *    4. Generate X25519 keypair (via Google Tink)
 *       - Classical elliptic curve for key exchange
 *       - Backward compatibility with non-PQ systems
 *    5. Generate Ed25519 keypair (via Google Tink)
 *       - Classical EdDSA for signature verification
 *       - Server authentication and challenge-response
 *    6. Store all private keys in Android Keystore
 *       - Device-bound, encrypted at rest
 *       - Never accessible in plaintext
 *
 *    Important: This operation runs on Dispatchers.Default (CPU-intensive)
 *
 *    File: domain/repository/AuthRepository.kt
 *    ──────────────────────────────────────────
 *    Interface defining authentication contract:
 *    - generateIdentity(phoneNumber): Generate cryptographic identity
 *    - registerIdentity(identity): Register with server
 *    - authenticateUser(userId): Establish authenticated session
 *    - isUserRegistered(phoneNumber): Check registration status
 *    - getStoredIdentity(userId): Retrieve local identity
 *    - clearAuthentication(): Logout and clear data
 *
 *
 * 2. DATA LAYER
 *    ===========
 *
 *    File: data/repository/AuthRepositoryImpl.kt
 *    ──────────────────────────────────────────
 *    Concrete implementation of AuthRepository:
 *
 *    Responsibilities:
 *    - Coordinate GenerateIdentityUseCase for key generation
 *    - Call AuthenticationService to register with server
 *    - Manage local encrypted storage (EncryptedSharedPreferences)
 *    - Handle registration response and server public key storage
 *
 *    Registration Flow:
 *    1. Receive Identity from GenerateIdentityUseCase
 *    2. Encode public keys to Base64
 *    3. Create AuthRegisterRequest
 *    4. Call authService.registerUser(request)
 *    5. On success:
 *       - Store identity locally
 *       - Store server's public key
 *       - Return AuthenticationResult.Success
 *    6. On error:
 *       - Return AuthenticationResult.Error or NetworkError
 *
 *    File: crypto/PostQuantumCrypto.kt
 *    ──────────────────────────────────
 *    JNI Bridge to C++ CRYSTALS implementation:
 *
 *    Native Functions:
 *    - generateMLKemKeypair(): Pair<ByteArray, ByteArray>
 *    - generateMLDsaKeypair(): Pair<ByteArray, ByteArray>
 *    - signWithMLDsa(message, privateKey): ByteArray
 *    - verifyMLDsaSignature(message, signature, publicKey): Boolean
 *    - encapsulateMLKem(publicKey): Pair<ByteArray, ByteArray>
 *    - decapsulateMLKem(encapsulation, privateKey): ByteArray
 *
 *    These are loaded from libquantum_crypto.so (compiled C++ code)
 *    Reference implementation: https://github.com/pq-crystals/
 *
 *    File: network/api/AuthenticationService.kt
 *    ───────────────────────────────────────────
 *    Retrofit service interface:
 *
 *    @POST("auth/register")
 *    suspend fun registerUser(@Body request: AuthRegisterRequest): Response<AuthRegisterResponse>
 *
 *    Expected Request Body:
 *    {
 *      "phoneNumber": "+1234567890",
 *      "mlKemPublicKey": "base64_encoded",
 *      "mlDsaPublicKey": "base64_encoded",
 *      "x25519PublicKey": "base64_encoded",
 *      "ed25519PublicKey": "base64_encoded",
 *      "deviceToken": "fcm_token_optional",
 *      "deviceName": "Pixel 8 Pro"
 *    }
 *
 *    Expected Response:
 *    {
 *      "success": true,
 *      "userId": "550e8400-e29b-41d4-a716-446655440000",
 *      "message": "Registration successful",
 *      "serverPublicKey": "base64_encoded_x25519_public_key",
 *      "challengeNonce": "base64_encoded_proof_of_identity"
 *    }
 *
 *
 * 3. PRESENTATION LAYER
 *    ====================
 *
 *    File: presentation/viewmodel/auth/AuthViewModel.kt
 *    ──────────────────────────────────────────────────
 *    MVVM with MVI principles (unidirectional data flow):
 *
 *    sealed class AuthState {
 *        object Idle                          // Ready for input
 *        data class GeneratingKeys(progress) // Crypto in progress
 *        data class Uploading(identity)      // Registering with server
 *        data class Success(userId, identity) // Registration complete
 *        data class Error(message, cause)    // Any step failed
 *    }
 *
 *    Public Methods:
 *    - fun secureLogin(phoneNumber: String)
 *      Entry point for authentication flow
 *      - Validates phone number
 *      - Calls authRepository.generateIdentity()
 *      - Calls authRepository.registerIdentity()
 *      - Updates authState accordingly
 *
 *    - fun retryLogin(phoneNumber: String)
 *      Retry failed register after error
 *
 *    - fun cancelLogin()
 *      Cancel ongoing operation and return to Idle
 *
 *    State Flow:
 *    Idle → GeneratingKeys → Uploading → Success
 *      ↓        ↓              ↓
 *      └────────Error──────────┘
 *
 *    All operations run in viewModelScope.launch with proper error handling.
 *    Heavy crypto (GenerateIdentityUseCase) automatically runs on Dispatchers.Default.
 *
 *
 * 4. UI LAYER
 *    =========
 *
 *    File: presentation/ui/screen/auth/AuthScreen.kt
 *    ─────────────────────────────────────────────────
 *    Premium Glassmorphism authentication UI:
 *
 *    Visual Components:
 *    1. AuthHeader
 *       - "Quantum Messenger" title
 *       - "Post-Quantum Secure Messaging" subtitle
 *
 *    2. IdleContent
 *       - Glassmorphism phone input field
 *       - GlowingButton "Secure Login"
 *       - Info text about on-device key generation
 *
 *    3. GeneratingKeysContent
 *       - CryptographicMeshAnimation (animated gradient)
 *       - "Generating Cryptographic Identity" status
 *       - CircularProgressIndicator
 *
 *    4. UploadingContent
 *       - "Registering with Server" status
 *       - Loading animation
 *
 *    5. SuccessContent
 *       - "✓ Secure Identity Created" checkmark
 *       - Success message
 *       - Auto-navigates after delay
 *
 *    6. ErrorContent
 *       - "⚠️ Authentication Error" warning
 *       - Error message
 *       - "Retry" button to try again
 *
 *    Animations:
 *    - GlowingButton: Pulsing glow effect using infiniteRepeatable tween
 *    - CryptographicMeshAnimation: Rotating radial gradient mesh
 *    - State transitions: fadeIn/slideInVertically for smooth UX
 *
 *    Edge-to-Edge Layout:
 *    - statusBarsPadding() above content
 *    - navigationBarsPadding() below content
 *    - imePadding() to avoid keyboard overlap
 *
 *
 * SECURITY FEATURES
 * =================
 *
 * Post-Quantum Cryptography (PQC):
 * - ML-KEM (NIST FIPS 203): Lattice-based key encapsulation
 * - ML-DSA (NIST FIPS 204): Lattice-based digital signatures
 * - Resistant to quantum computer attacks
 * - Larger key sizes (ML-KEM-768: 1,184 bytes public key)
 *
 * Classical Cryptography (Backward Compatibility):
 * - X25519: Elliptic curve Diffie-Hellman
 * - Ed25519: EdDSA digital signatures
 * - Hybrid approach: PQ + classical for defense in depth
 *
 * Key Management:
 * - Private keys stored in Android Keystore
 * - Device-bound to hardware-backed TEE (if available)
 * - Never exported or accessible in plaintext
 * - EncryptedSharedPreferences for local identity storage
 *
 * Network Security:
 * - TLS 1.3+ for all HTTPS connections
 * - Certificate pinning (configured in OkHttp)
 * - Base64 encoding for public keys in requests
 * - Retrofit timeout configuration
 *
 * Input Validation:
 * - Phone number format validation (E.164)
 * - Public key size verification
 * - Response signature verification (server → client)
 *
 *
 * DISPATCHER MANAGEMENT
 * ====================
 *
 * withContext(Dispatchers.Default) in GenerateIdentityUseCase
 * - ML-KEM key generation: ~50-200ms per pair
 * - ML-DSA key generation: ~10-50ms per pair
 * - X25519 key generation: ~1-5ms
 * - Ed25519 key generation: ~1-5ms
 * - Total: ~100-300ms, blocking UI if on main thread
 *
 * withContext(Dispatchers.IO) in AuthRepositoryImpl
 * - Retrofit network calls (suspend functions)
 * - Local file I/O (reading/writing keys)
 *
 * viewModelScope.launch (Main dispatcher by default in ViewModel)
 * - UI state updates
 * - Navigation events
 * - Error handling and logging
 *
 *
 * TESTING CONSIDERATIONS
 * ======================
 *
 * Unit Tests:
 * @Test
 * fun testPhoneNumberValidation() {
 *     val viewModel = AuthViewModel(mockAuthRepository)
 *     viewModel.secureLogin("")  // Should fail validation
 *     assertEquals(AuthState.Error, viewModel.authState.value)
 * }
 *
 * @Test
 * fun testKeyGenerationFlow() {
 *     val useCase = GenerateIdentityUseCase()
 *     runBlocking {
 *         val result = useCase("+1234567890")
 *         assertTrue(result is IdentityGenerationResult.Success)
 *     }
 * }
 *
 * Integration Tests:
 * - Mock AuthenticationService responses
 * - Test complete flow from input to success/error
 * - Verify state transitions in AuthState
 *
 * End-to-End Tests:
 * - Register actual user with test Express server
 * - Verify private keys stored in Keystore
 * - Verify public keys received by server
 *
 *
 * FUTURE ENHANCEMENTS
 * ====================
 *
 * 1. Biometric Authentication
 *    - Use fingerprint/face to unlock stored private keys
 *    - Require reauthentication for sensitive operations
 *
 * 2. Multi-Device Support
 *    - Synchronize identity across devices
 *    - Device-specific keying (separate key per device)
 *
 * 3. Key Rotation
 *    - Periodic MLK-KEM/ML-DSA key refresh
 *    - Secure key rotation protocol with server
 *
 * 4. Hardware Security
 *    - Leverage StrongBox Keymaster (if available)
 *    - Secure enclave utilization for crypto operations
 *
 * 5. Recovery Codes
 *    - Generate recovery codes for account recovery
 *    - Encrypted storage and offline backup option
 */


