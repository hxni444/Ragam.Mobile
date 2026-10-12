package com.ragam.music.sync

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ragam.music.constants.AccountEmailKey
import com.ragam.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

val CustomSyncEmailKey = stringPreferencesKey("custom_sync_email")
val CustomSyncIdKey = stringPreferencesKey("custom_sync_id")
val PersistentDeviceIdKey = stringPreferencesKey("ragam_device_sync_id")

@Singleton
class MusicSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private val _cloudNowPlaying = MutableStateFlow<CloudNowPlayingState?>(null)
    val cloudNowPlaying: StateFlow<CloudNowPlayingState?> = _cloudNowPlaying.asStateFlow()

    private var activeNowPlayingListener: ListenerRegistration? = null
    private var lastObservedSyncId: String? = null

    init {
        // Automatically start observing cloud playback on app startup
        scope.launch {
            ensureAuthenticated()
            startObservingCloudNowPlaying()
        }
    }

    /**
     * Normalizes an email or UID to match Ragam Desktop's deterministic sync ID
     */
    fun normalizeSyncId(email: String?, uid: String): String {
        return if (!email.isNullOrBlank()) {
            "u_" + email.trim().lowercase().replace(Regex("[^a-z0-9]"), "_")
        } else {
            uid
        }
    }

    /**
     * Ensures Firebase Auth has an active session (signs in anonymously if needed)
     * so that Firestore security rules (`request.auth != null`) allow access.
     */
    suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
                Timber.tag("MusicSync").d("Signed into Firebase anonymously: ${auth.currentUser?.uid}")
            } catch (e: Exception) {
                Timber.tag("MusicSync").e(e, "Failed to sign into Firebase anonymously")
            }
        }
    }

    /**
     * Resolves the deterministic sync ID matching Ragam Desktop:
     * 1. Custom Sync ID / Pair code (e.g. "guest_yzzwtqfz" or custom email)
     * 2. Custom Sync Email preference
     * 3. Firebase Auth user email (if logged in with Email or Google)
     * 4. YouTube Music account email stored in DataStore
     * 5. Persistent device sync ID (formatted like Desktop: "guest_...")
     */
    suspend fun getActiveSyncId(): String {
        ensureAuthenticated()

        val prefs = try {
            context.dataStore.data.first()
        } catch (_: Exception) {
            null
        }

        // 1. Manually specified custom sync ID or device code
        val customId = prefs?.get(CustomSyncIdKey)?.trim()?.takeIf { it.isNotBlank() }
        if (!customId.isNullOrBlank()) {
            val resolved = if (customId.contains("@")) normalizeSyncId(customId, "") else customId
            Timber.tag("MusicSync").d("Resolved sync ID from CustomSyncIdKey: $resolved")
            return resolved
        }

        // 2. Custom sync email preference
        val customEmail = prefs?.get(CustomSyncEmailKey)?.trim()?.takeIf { it.isNotBlank() }
        if (!customEmail.isNullOrBlank()) {
            val resolved = normalizeSyncId(customEmail, "")
            Timber.tag("MusicSync").d("Resolved sync ID from CustomSyncEmail: $resolved")
            return resolved
        }

        // 3. Firebase Auth user (if authenticated with real email)
        val authUser = auth.currentUser
        if (authUser != null && !authUser.isAnonymous && !authUser.email.isNullOrBlank()) {
            val resolved = normalizeSyncId(authUser.email, authUser.uid)
            Timber.tag("MusicSync").d("Resolved sync ID from Firebase Auth email: $resolved")
            return resolved
        }

        // 4. YouTube Music account email
        val accountEmail = prefs?.get(AccountEmailKey)?.trim()?.takeIf { it.isNotBlank() }
        if (!accountEmail.isNullOrBlank()) {
            val resolved = normalizeSyncId(accountEmail, authUser?.uid ?: "")
            Timber.tag("MusicSync").d("Resolved sync ID from AccountEmailKey: $resolved")
            return resolved
        }

        // 5. Firebase Auth UID (if non-anonymous)
        if (authUser != null && !authUser.isAnonymous && authUser.uid.isNotBlank()) {
            return authUser.uid
        }

        // 6. Persistent guest device ID (matching Ragam Desktop's persistent fallback)
        val savedDeviceId = prefs?.get(PersistentDeviceIdKey)?.trim()?.takeIf { it.isNotBlank() }
        if (!savedDeviceId.isNullOrBlank()) {
            return savedDeviceId
        }

        val newDeviceId = "guest_" + UUID.randomUUID().toString().replace("-", "").take(8)
        try {
            context.dataStore.edit { it[PersistentDeviceIdKey] = newDeviceId }
        } catch (_: Exception) {}
        Timber.tag("MusicSync").d("Generated new persistent device sync ID: $newDeviceId")
        return newDeviceId
    }

    /**
     * Starts or restarts real-time observation of the currently active sync ID
     */
    suspend fun startObservingCloudNowPlaying() {
        val syncId = getActiveSyncId()
        if (syncId == lastObservedSyncId && activeNowPlayingListener != null) {
            return
        }

        activeNowPlayingListener?.remove()
        lastObservedSyncId = syncId

        val docRef = firestore.collection("users")
            .document(syncId)
            .collection("playback")
            .document("current")

        Timber.tag("MusicSync").d("Listening to cloud now-playing at users/$syncId/playback/current")

        activeNowPlayingListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.tag("MusicSync").e(error, "Error listening to cloud playback for syncId=$syncId")
                _cloudNowPlaying.value = null
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val state = parseNowPlayingFromSnapshot(snapshot)
                Timber.tag("MusicSync").d("Cloud playback update received: ${state?.track?.title} (isPlaying=${state?.isPlaying})")
                _cloudNowPlaying.value = state
            } else {
                _cloudNowPlaying.value = null
            }
        }
    }

    /**
     * Saves a custom email for sync
     */
    suspend fun setCustomSyncEmail(email: String) {
        context.dataStore.edit { prefs ->
            if (email.isBlank()) {
                prefs.remove(CustomSyncEmailKey)
            } else {
                prefs[CustomSyncEmailKey] = email.trim()
            }
        }
        startObservingCloudNowPlaying()
    }

    /**
     * Saves a custom sync ID (like Desktop's "guest_..." ID)
     */
    suspend fun setCustomSyncId(syncId: String) {
        context.dataStore.edit { prefs ->
            if (syncId.isBlank()) {
                prefs.remove(CustomSyncIdKey)
            } else {
                prefs[CustomSyncIdKey] = syncId.trim()
            }
        }
        startObservingCloudNowPlaying()
    }

    fun observeNowPlaying(): Flow<CloudNowPlayingState?> = cloudNowPlaying

    suspend fun getEffectiveSyncEmail(): String? {
        val user = getCurrentFirebaseUser()
        if (user != null && !user.email.isNullOrBlank()) return user.email

        val prefs = try {
            context.dataStore.data.first()
        } catch (_: Exception) {
            null
        }

        val customEmail = prefs?.get(CustomSyncEmailKey)?.takeIf { it.isNotBlank() }
        if (!customEmail.isNullOrBlank()) return customEmail

        val customId = prefs?.get(CustomSyncIdKey)?.takeIf { it.isNotBlank() }
        if (!customId.isNullOrBlank()) return customId

        val accountEmail = prefs?.get(AccountEmailKey)?.takeIf { it.isNotBlank() }
        if (!accountEmail.isNullOrBlank()) return accountEmail

        return null
    }

    suspend fun getEffectiveSyncId(): String {
        return getActiveSyncId()
    }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        val user = auth.currentUser
        return if (user != null && !user.isAnonymous) user else null
    }

    /**
     * Signs in with Firebase email & password
     */
    suspend fun loginWithFirebase(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("Firebase user was null after sign-in")
            startObservingCloudNowPlaying()
            Result.success(user)
        } catch (e: Exception) {
            Timber.tag("MusicSync").e(e, "Firebase login failed")
            Result.failure(e)
        }
    }

    /**
     * Registers a new account with Firebase email, password, displayName & avatar
     */
    suspend fun registerWithFirebase(
        email: String,
        pass: String,
        displayName: String,
        avatarUrl: String? = null
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("Firebase user was null after registration")

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim().ifBlank { email.split("@")[0] })
                .apply {
                    if (!avatarUrl.isNullOrBlank()) {
                        setPhotoUri(Uri.parse(avatarUrl))
                    }
                }
                .build()

            user.updateProfile(profileUpdates).await()
            startObservingCloudNowPlaying()
            Result.success(user)
        } catch (e: Exception) {
            Timber.tag("MusicSync").e(e, "Firebase registration failed")
            Result.failure(e)
        }
    }

    /**
     * Signs out of Firebase Auth and falls back to anonymous session
     */
    suspend fun logoutFromFirebase() {
        try {
            auth.signOut()
            ensureAuthenticated()
            startObservingCloudNowPlaying()
        } catch (e: Exception) {
            Timber.tag("MusicSync").e(e, "Failed to sign out of Firebase")
        }
    }

    /**
     * Sends local Android Now Playing state to Firestore `users/{syncId}/playback/current`
     */
    suspend fun sendNowPlaying(
        track: TrackDto?,
        isPlaying: Boolean,
        positionSec: Double,
        deviceName: String = "Ragam Mobile (${android.os.Build.MODEL})"
    ) {
        val syncId = getActiveSyncId()
        val docRef = firestore.collection("users")
            .document(syncId)
            .collection("playback")
            .document("current")

        val stateMap = hashMapOf<String, Any?>(
            "userId" to syncId,
            "track" to if (track != null) hashMapOf(
                "id" to track.id,
                "title" to track.title,
                "artist" to track.artist,
                "album" to track.album,
                "duration" to track.duration,
                "thumbnailUrl" to track.thumbnailUrl
            ) else null,
            "isPlaying" to isPlaying,
            "positionSec" to positionSec,
            "timestampMs" to System.currentTimeMillis(),
            "deviceName" to deviceName,
            "volume" to 1.0,
            "repeatMode" to "off",
            "isShuffle" to false,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(stateMap, SetOptions.merge()).await()
            Timber.tag("MusicSync").d("Synced now playing to Firestore: ${track?.title} (isPlaying=$isPlaying, device=$deviceName, syncId=$syncId)")
        } catch (e: Exception) {
            Timber.tag("MusicSync").e(e, "Failed to sync now playing to Firestore for syncId=$syncId")
        }
    }

    /**
     * Sends played track to Firestore `users/{syncId}/history`
     */
    suspend fun sendTrackHistory(
        track: TrackDto,
        deviceName: String = "Ragam Mobile (${android.os.Build.MODEL})"
    ) {
        val syncId = getActiveSyncId()
        val historyCol = firestore.collection("users")
            .document(syncId)
            .collection("history")
        val newDocRef = historyCol.document()

        val historyMap = hashMapOf(
            "userId" to syncId,
            "track" to hashMapOf(
                "id" to track.id,
                "title" to track.title,
                "artist" to track.artist,
                "album" to track.album,
                "duration" to track.duration,
                "thumbnailUrl" to track.thumbnailUrl
            ),
            "playedAtMs" to System.currentTimeMillis(),
            "playedAt" to FieldValue.serverTimestamp(),
            "device" to deviceName
        )

        try {
            newDocRef.set(historyMap).await()
            Timber.tag("MusicSync").d("Synced history track to Firestore: ${track.title} for syncId=$syncId")
        } catch (e: Exception) {
            Timber.tag("MusicSync").e(e, "Failed to sync history to Firestore for syncId=$syncId")
        }
    }

    /**
     * Clears now playing state on cloud
     */
    suspend fun clearNowPlaying() {
        val syncId = getActiveSyncId()
        val docRef = firestore.collection("users")
            .document(syncId)
            .collection("playback")
            .document("current")

        val stateMap = hashMapOf<String, Any?>(
            "userId" to syncId,
            "isPlaying" to false,
            "positionSec" to 0.0,
            "timestampMs" to System.currentTimeMillis(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(stateMap, SetOptions.merge()).await()
            Timber.tag("MusicSync").d("Cleared now playing in Firestore for syncId=$syncId")
        } catch (e: Exception) {
            Timber.tag("MusicSync").e(e, "Failed to clear now playing in Firestore")
        }
    }

    /**
     * Observes real-time Listening History in descending order
     */
    fun observeHistory(limit: Long = 50): Flow<List<CloudHistoryItem>> = callbackFlow {
        val syncId = getActiveSyncId()

        val colRef = firestore.collection("users")
            .document(syncId)
            .collection("history")
            .orderBy("playedAtMs", Query.Direction.DESCENDING)
            .limit(limit)

        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.tag("MusicSync").e(error, "Error observing History for syncId=$syncId")
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    parseHistoryItemFromSnapshot(doc)
                }
                trySend(items)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Sends playback command to Desktop (Play/Pause, Seek, etc.)
     */
    suspend fun updateRemotePlayback(isPlaying: Boolean? = null, positionSec: Double? = null) {
        val syncId = getActiveSyncId()
        val docRef = firestore.collection("users")
            .document(syncId)
            .collection("playback")
            .document("current")

        val updates = mutableMapOf<String, Any>()
        if (isPlaying != null) {
            updates["isPlaying"] = isPlaying
            updates["timestampMs"] = System.currentTimeMillis()
        }
        if (positionSec != null) {
            updates["positionSec"] = positionSec
            updates["timestampMs"] = System.currentTimeMillis()
        }

        if (updates.isNotEmpty()) {
            try {
                docRef.update(updates).await()
                Timber.tag("MusicSync").d("Updated remote playback in Firestore: $updates for syncId=$syncId")
            } catch (e: Exception) {
                Timber.tag("MusicSync").e(e, "Failed to update remote playback")
            }
        }
    }

    /**
     * Robust snapshot parser that safely coerces numbers across Long, Double, Int
     */
    private fun parseNowPlayingFromSnapshot(snapshot: DocumentSnapshot): CloudNowPlayingState? {
        val data = snapshot.data ?: return null
        val trackData = data["track"] as? Map<*, *>
        val track = if (trackData != null) {
            TrackDto(
                id = trackData["id"]?.toString().orEmpty(),
                title = trackData["title"]?.toString().orEmpty(),
                artist = trackData["artist"]?.toString().orEmpty(),
                album = trackData["album"]?.toString().orEmpty(),
                duration = (trackData["duration"] as? Number)?.toLong() ?: 0L,
                thumbnailUrl = trackData["thumbnailUrl"]?.toString().orEmpty()
            )
        } else null

        return CloudNowPlayingState(
            userId = data["userId"]?.toString().orEmpty(),
            track = track,
            isPlaying = data["isPlaying"] as? Boolean ?: false,
            positionSec = (data["positionSec"] as? Number)?.toDouble() ?: 0.0,
            timestampMs = (data["timestampMs"] as? Number)?.toLong() ?: 0L,
            deviceName = data["deviceName"]?.toString().orEmpty(),
            volume = (data["volume"] as? Number)?.toDouble() ?: 1.0,
            repeatMode = data["repeatMode"]?.toString().orEmpty(),
            isShuffle = data["isShuffle"] as? Boolean ?: false
        )
    }

    private fun parseHistoryItemFromSnapshot(doc: DocumentSnapshot): CloudHistoryItem? {
        val data = doc.data ?: return null
        val trackData = data["track"] as? Map<*, *>
        val track = if (trackData != null) {
            TrackDto(
                id = trackData["id"]?.toString().orEmpty(),
                title = trackData["title"]?.toString().orEmpty(),
                artist = trackData["artist"]?.toString().orEmpty(),
                album = trackData["album"]?.toString().orEmpty(),
                duration = (trackData["duration"] as? Number)?.toLong() ?: 0L,
                thumbnailUrl = trackData["thumbnailUrl"]?.toString().orEmpty()
            )
        } else null

        return CloudHistoryItem(
            docId = doc.id,
            userId = data["userId"]?.toString().orEmpty(),
            track = track,
            playedAtMs = (data["playedAtMs"] as? Number)?.toLong() ?: 0L,
            device = data["device"]?.toString().orEmpty()
        )
    }
}
