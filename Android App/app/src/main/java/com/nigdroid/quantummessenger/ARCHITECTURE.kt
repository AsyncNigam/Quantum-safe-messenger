/**
 * DOMAIN AND PRESENTATION LAYER ARCHITECTURE
 * ==========================================
 *
 * This file documents the complete Domain and Presentation layer implementation
 * for the Quantum Messenger secure messaging application using MVVM with
 * Unidirectional Data Flow (UDF) principles.
 *
 * ARCHITECTURE OVERVIEW
 * ====================
 *
 * The implementation follows Clean Architecture with three distinct layers:
 *
 * 1. DATA LAYER
 *    - ChatRepository: Handles encryption/decryption and database operations
 *    - CryptoManager: Manages Tink-based AES-256-GCM encryption
 *    - SQLCipher-encrypted Room Database
 *    - Hilt dependency injection modules
 *
 * 2. DOMAIN LAYER
 *    - ChatMessage: Domain model for messages
 *    - ChatRepository interface: Defines contract for data operations
 *    - Use Cases: Orchestrate business logic using repository
 *      • SendMessageUseCase: Encrypt, save locally, serialize to Protobuf, send via WebSocket
 *      • ReceiveMessageUseCase: Observe WebSocket messages, decrypt, save locally
 *      • GetChatHistoryUseCase: Retrieve Flow<List<ChatMessage>> from database
 *
 * 3. PRESENTATION LAYER
 *    - ChatUiState: Sealed class representing all possible UI states
 *    - ChatViewModel: MVVM ViewModel with @HiltViewModel annotation
 *    - Manages UI state using StateFlow<ChatUiState>
 *    - Handles coroutines with viewModelScope
 *    - Robust exception handling prevents app crashes
 *
 *
 * DOMAIN LAYER STRUCTURE
 * ======================
 *
 * =====================================================================
 * File: domain/model/ChatMessage.kt
 * =====================================================================
 *
 * Represents a decrypted chat message in the domain layer.
 * Properties:
 *   - id: Long = 0
 *   - senderId: String
 *   - receiverId: String
 *   - content: String (plain text, already decrypted)
 *   - timestamp: Long (milliseconds)
 *   - messageType: MessageType (TEXT, IMAGE, FILE)
 *   - isRead: Boolean
 *
 * =====================================================================
 * File: domain/repository/ChatRepository.kt
 * =====================================================================
 *
 * Interface defining the contract for chat data operations:
 *
 * suspend fun sendMessage(message: ChatMessage): Long
 *   - Encrypts content via CryptoManager
 *   - Saves encrypted message to Room Database
 *   - Returns the ID of the inserted message
 *
 * fun getMessagesBetweenUsers(userId: String, otherUserId: String): Flow<List<ChatMessage>>
 *   - Returns a Flow that observes changes in real-time
 *   - Messages are already decrypted by the repository
 *   - Emits new messages whenever database is updated
 *
 * fun getUnreadMessagesForUser(userId: String): Flow<List<ChatMessage>>
 *   - Retrieves only unread messages as a Flow
 *
 * suspend fun markMessageAsRead(messageId: Long)
 *   - Updates the isRead flag in the database
 *
 * suspend fun deleteMessage(messageId: Long)
 *   - Deletes a single message
 *
 * suspend fun deleteConversation(userId: String, otherUserId: String)
 *   - Deletes all messages in a conversation
 *
 * =====================================================================
 * File: domain/usecase/GetChatHistoryUseCase.kt
 * =====================================================================
 *
 * Retrieves a real-time observable flow of chat messages.
 *
 * Usage:
 *   getChatHistoryUseCase(currentUserId, recipientId)
 *     .collect { messages: List<ChatMessage> ->
 *       // Update UI with decrypted messages
 *     }
 *
 * Key Features:
 *   - Operator function invoke() for concise syntax
 *   - Returns Flow<List<ChatMessage>> for reactive updates
 *   - Already decrypted by the repository
 *
 * =====================================================================
 * File: domain/usecase/SendMessageUseCase.kt
 * =====================================================================
 *
 * Orchestrates sending a message through encryption, storage, and network.
 *
 * Operation Flow:
 *   1. Takes plain text string as input
 *   2. Creates ChatMessage domain model
 *   3. Encrypts and saves to Room Database via ChatRepository
 *   4. Converts to Protobuf ChatMessage schema
 *   5. Sends serialized Protobuf message via WebSocket
 *
 * Usage:
 *   sendMessageUseCase(
 *     plainTextContent = "Hello, Alice!",
 *     senderId = "user_123",
 *     receiverId = "user_456",
 *     messageType = MessageType.TEXT
 *   )
 *
 * Key Features:
 *   - Runs on IO dispatcher (no main thread blocking)
 *   - Comprehensive error handling
 *   - Throws SendMessageException on failure
 *
 * Protobuf Conversion:
 *   - Plain text is wrapped in ByteString for Protobuf
 *   - Server receives serialized ChatMessage with payload field
 *   - Server-side encryption/signature applied if needed
 *
 * =====================================================================
 * File: domain/usecase/ReceiveMessageUseCase.kt
 * =====================================================================
 *
 * Observes incoming WebSocket messages and saves them locally.
 *
 * Operation Flow:
 *   1. Observes WebSocketManager.events as a Flow
 *   2. Filters for MessageReceived events
 *   3. Converts Protobuf payload to plain text string
 *   4. Creates ChatMessage domain model
 *   5. Saves to encrypted Room Database
 *   6. Emits ReceiveMessageResult (Success or Error)
 *
 * Usage:
 *   receiveMessageUseCase()
 *     .collect { result ->
 *       when (result) {
 *         is ReceiveMessageResult.Success -> {
 *           val message: ChatMessage = result.message
 *           // UI automatically updates via GetChatHistoryUseCase flow
 *         }
 *         is ReceiveMessageResult.Error -> {
 *           val error: String = result.message
 *           // Handle error gracefully
 *         }
 *       }
 *     }
 *
 * Key Features:
 *   - Error handling prevents app crashes
 *   - WebSocket connection drops are caught
 *   - Decryption failures are reported as errors
 *   - Messages are automatically saved to database
 *
 * =====================================================================
 * File: domain/di/UseCaseModule.kt
 * =====================================================================
 *
 * Hilt DI module providing all use cases as singletons.
 * Manages dependencies:
 *   - ChatRepository: injected into SendMessageUseCase, ReceiveMessageUseCase, GetChatHistoryUseCase
 *   - WebSocketManager: injected into SendMessageUseCase, ReceiveMessageUseCase
 *
 *
 * PRESENTATION LAYER STRUCTURE
 * ============================
 *
 * =====================================================================
 * File: presentation/viewmodel/ChatUiState.kt
 * =====================================================================
 *
 * Sealed class representing all possible UI states.
 * Implements Unidirectional Data Flow (UDF) pattern.
 *
 * States:
 *
 * object Loading : ChatUiState()
 *   - Used when the screen first loads
 *   - Display progress bar to user
 *
 * data class Success(
 *   val messages: List<ChatMessage> = emptyList(),
 *   val isSending: Boolean = false
 * ) : ChatUiState()
 *   - messages: Current list of decrypted messages
 *   - isSending: True while a message is being sent
 *   - Use for displaying message list and send button state
 *
 * data class Error(
 *   val message: String,
 *   val messages: List<ChatMessage> = emptyList()
 * ) : ChatUiState()
 *   - message: Error description to show user
 *   - messages: Previously loaded messages (if any) to keep them visible
 *
 * =====================================================================
 * File: presentation/viewmodel/ChatViewModel.kt
 * =====================================================================
 *
 * MVVM ViewModel managing chat screen state and behavior.
 * Annotated with @HiltViewModel for dependency injection.
 *
 * Properties:
 *   - uiState: StateFlow<ChatUiState> - Single source of truth for UI
 *
 * Key Methods:
 *
 * fun initialize(userId: String, participantId: String)
 *   - Call once when screen is first loaded
 *   - Starts observing chat history
 *   - Starts listening for incoming messages
 *   - Must be called before any other methods
 *
 * fun sendMessage(plainTextContent: String)
 *   - Public method for sending a message
 *   - Updates isSending state during transmission
 *   - Automatically called through use case
 *   - Handles errors gracefully
 *
 * fun markMessageAsRead(messageId: Long)
 *   - Marks a message as read
 *   - Updates database without affecting UI message list
 *
 * fun deleteMessage(messageId: Long)
 *   - Deletes a single message
 *   - UI automatically updates via chat history flow
 *
 * fun retryLoadingMessages()
 *   - Retries loading chat history after an error
 *
 * Internal Methods (private):
 *
 * loadChatHistory()
 *   - Loads messages via GetChatHistoryUseCase
 *   - Observes Flow for real-time updates
 *   - Handles loading, success, and error states
 *
 * observeIncomingMessages()
 *   - Observes incoming messages via ReceiveMessageUseCase
 *   - Saves them to database
 *   - Handles WebSocket errors gracefully
 *   - Automatically shows/hides errors after 3 seconds
 *
 * Coroutine Management:
 *   - All coroutines use viewModelScope
 *   - Automatically cancelled when ViewModel is destroyed
 *   - No manual cleanup needed
 *
 * Error Handling Strategy:
 *   1. Send Error: Show error message, keep messages on screen, retry after 3 seconds
 *   2. Receive Error: Show error message, keep messages on screen, retry after 3 seconds
 *   3. Load Error: Show error state with retry button
 *   4. Never throws exceptions to UI layer - always caught and converted to state
 *
 * =====================================================================
 * File: network/di/NetworkModule.kt
 * =====================================================================
 *
 * Hilt module providing WebSocketManager as a singleton.
 * Ensures only one WebSocket connection throughout the app lifetime.
 *
 *
 * DATA FLOW DIAGRAMS
 * ==================
 *
 * SENDING A MESSAGE:
 * ==================\n *   User Input (String)
 *        |\n *        v\n *   ChatViewModel.sendMessage(text)\n *        |\n *        v\n *   SendMessageUseCase.invoke()\n *        |\n *        +-> ChatRepository.sendMessage()  [Encrypt & Save]\n *        |        |\n *        |        v\n *        |   CryptoManager.encrypt()        [AES-256-GCM]\n *        |        |\n *        |        v\n *        |   Room Database                  [with SQLCipher]\n *        |\n *        +-> Convert to Protobuf\n *        |        |\n *        |        v\n *        |   ByteString.copyFrom(payload)\n *        |\n *        +-> WebSocketManager.sendMessage() [Send over network]\n *                 |\n *                 v\n *             Server receives Protobuf\n *
 * RECEIVING A MESSAGE:
 * ====================\n *   Server sends Protobuf\n *        |\n *        v\n *   WebSocketManager.events (SharedFlow)\n *        |\n *        v\n *   ReceiveMessageUseCase.invoke() (Flow)\n *        |\n *        v\n *   Decode Protobuf payload to String\n *        |\n *        v\n *   ChatRepository.sendMessage()  [Save encrypted]\n *        |\n *        v\n *   CryptoManager (already encrypted content preserved)\n *        |\n *        v\n *   Room Database                  [with SQLCipher]\n *        |\n *        v\n *   GetChatHistoryUseCase emits change\n *        |\n *        v\n *   ChatViewModel.uiState updates\n *        |\n *        v\n *   UI observes StateFlow and re-composes\n *
 * LOADING CHAT HISTORY:\n * ====================\n *   Screen loads\n *        |\n *        v\n *   ChatViewModel.initialize()\n *        |\n *        v\n *   GetChatHistoryUseCase(userId, recipientId)\n *     .collect { messages }\n *        |\n *        v\n *   ChatRepository.getMessagesBetweenUsers()\n *        |\n *        v\n *   Room Database Query\n *        |\n *        v\n *   CryptoManager.decrypt()  [For each message]\n *        |\n *        v\n *   ChatViewModel updates uiState with Success\n *        |\n *        v\n *   UI displays message list\n *
 *
 * USAGE EXAMPLE - COMPOSABLE SCREEN\n * ==================================\n *\n * @Composable\n * fun ChatScreen() {\n *   val viewModel: ChatViewModel = hiltViewModel()\n *   val uiState by viewModel.uiState.collectAsState()\n *\n *   LaunchedEffect(Unit) {\n *     // Initialize with hardcoded IDs for example\n *     viewModel.initialize(userId = \"current_user\", participantId = \"recipient\")\n *   }\n *\n *   Column(modifier = Modifier.fillMaxSize()) {\n *     when (uiState) {\n *       is ChatUiState.Loading -> {\n *         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {\n *           CircularProgressIndicator()\n *         }\n *       }\n *\n *       is ChatUiState.Success -> {\n *         val state = uiState as ChatUiState.Success\n *\n *         // Message list\n *         LazyColumn(modifier = Modifier.weight(1f)) {\n *           items(state.messages) { message ->\n *             MessageBubble(\n *               text = message.content,\n *               isOwn = message.senderId == \"current_user\",\n *               timestamp = message.timestamp\n *             )\n *           }\n *         }\n *\n *         // Message input\n *         Row(modifier = Modifier.fillMaxWidth()) {\n *           TextField(\n *             value = messageText,\n *             onValueChange = { messageText = it },\n *             modifier = Modifier.weight(1f),\n *             enabled = !state.isSending\n *           )\n *           Button(\n *             onClick = {\n *               viewModel.sendMessage(messageText)\n *               messageText = \"\"\n *             },\n *             enabled = !state.isSending && messageText.isNotBlank()\n *           ) {\n *             if (state.isSending) {\n *               CircularProgressIndicator(Modifier.size(20.dp))\n *             } else {\n *               Text(\"Send\")\n *             }\n *           }\n *         }\n *       }\n *\n *       is ChatUiState.Error -> {\n *         val state = uiState as ChatUiState.Error\n *         ErrorScreen(\n *           errorMessage = state.message,\n *           onRetry = { viewModel.retryLoadingMessages() }\n *         )\n *       }\n *     }\n *   }\n * }\n *\n *\n * SECURITY FEATURES\n * ==================\n *\n * 1. END-TO-END ENCRYPTION:\n *    - CryptoManager uses Google Tink with AES-256-GCM\n *    - Key stored securely in Android Keystore\n *    - Message content encrypted before Room storage\n *    - Decryption happens in memory only\n *\n * 2. DATABASE ENCRYPTION:\n *    - SQLCipher provides whole-database encryption\n *    - 256-bit passphrase derived from CryptoManager key\n *    - Even if device is compromised, database remains encrypted\n *\n * 3. ERROR HANDLING:\n *    - Decryption failures caught and logged\n *    - WebSocket disconnections handled gracefully\n *    - No sensitive data logged to console\n *    - Error messages show generic strings to user\n *\n * 4. COROUTINE SAFETY:\n *    - viewModelScope ensures cleanup on activity destroy\n *    - IO dispatcher prevents main thread blocking\n *    - Exception handling prevents crashes\n *\n *\n * TESTING CONSIDERATIONS\n * =======================\n *\n * 1. Unit Tests for Use Cases:\n *    - Mock ChatRepository\n *    - Mock WebSocketManager\n *    - Test encryption/decryption workflows\n *
 *    Example:\n *    @Test\n *    fun testSendMessageEncryptsContent() {\n *      coEvery { chatRepository.sendMessage(any()) } returns 1L\n *      every { webSocketManager.sendMessage(any()) } just runs\n *\n *      runBlocking {\n *        sendMessageUseCase(\n *          \"Hello\",\n *          \"user1\",\n *          \"user2\"\n *        )\n *      }\n *\n *      coVerify { chatRepository.sendMessage(any()) }\n *      verify { webSocketManager.sendMessage(any()) }\n *    }\n *\n * 2. ViewModel Tests:\n *    - Use TestDispatchers for coroutines\n *    - Mock use cases\n *    - Test state transitions\n *    - Verify error handling\n *\n * 3. Integration Tests:\n *    - Test end-to-end message flow\n *    - Verify WebSocket integration\n *    - Test database encryption\n *\n *\n * PERFORMANCE OPTIMIZATIONS\n * ==========================\n *\n * 1. Lazy Initialization:\n *    - CryptoManager key generated lazily (first access)\n *    - Database connections pooled\n *\n * 2. Flow-based Updates:\n *    - ChatHistoryFlow only emits when messages change\n *    - UI only recomposes when state actually changes\n *    - No unnecessary database queries\n *\n * 3. IO Dispatcher:\n *    - All blocking operations (encryption, DB) on IO thread\n *    - Main thread never blocked\n *    - Smooth UI animations and responsiveness\n *\n * 4. Coroutine Scope Management:\n *    - viewModelScope prevents coroutine leaks\n *    - Automatic cleanup prevents memory leaks\n *    - No manual job cancellation needed\n *\n *\n * FUTURE ENHANCEMENTS\n * ====================\n *\n * 1. Message Pagination:\n *    - Load messages in batches for large conversations\n *    - Implement lazy loading on scroll\n *\n * 2. Offline Support:\n *    - Queue failed sends locally\n *    - Retry when connection restored\n *\n * 3. Read Receipts:\n *    - Implement proper read receipt protocol\n *    - Show when recipient read message\n *\n * 4. Message Editing/Deletion:\n *    - Implement server-side message tracking\n *    - Support edit history\n *\n * 5. File/Image Sharing:\n *    - Extend MessageType enum\n *    - Handle file encryption separately\n *    - Implement upload/download with progress\n */
