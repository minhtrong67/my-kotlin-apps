package com.expenser.app.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed interface SignInResult {
    data object Success : SignInResult
    data object Cancelled : SignInResult
    data class Failure(val message: String) : SignInResult
}

class AuthRepository(private val app: Context) {
    /** False until google-services.json has been added to the project. */
    val configured: Boolean = FirebaseApp.getApps(app).isNotEmpty()
    private val auth: FirebaseAuth? = if (configured) FirebaseAuth.getInstance() else null

    val currentUser: FirebaseUser? get() = auth?.currentUser

    fun profile(u: FirebaseUser?): UserProfile? = u?.let { UserProfile(it.uid, it.displayName.orEmpty(), it.email.orEmpty(), it.photoUrl?.toString()) }

    val userFlow: Flow<UserProfile?> = callbackFlow {
        val a = auth
        if (a == null) { trySend(null); awaitClose { }; return@callbackFlow }
        val l = FirebaseAuth.AuthStateListener { trySend(profile(it.currentUser)) }
        a.addAuthStateListener(l)
        awaitClose { a.removeAuthStateListener(l) }
    }

    /** Google sign-in through Credential Manager, then exchanged for a Firebase credential. */
    suspend fun signInWithGoogle(activityContext: Context): SignInResult {
        val a = auth ?: return SignInResult.Failure("Firebase is not configured")
        val id = app.resources.getIdentifier("default_web_client_id", "string", app.packageName)
        if (id == 0) return SignInResult.Failure("default_web_client_id missing: add the SHA-1 fingerprint in Firebase and re-download google-services.json")
        return try {
            val option = GetSignInWithGoogleOption.Builder(app.getString(id)).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
            val google = GoogleIdTokenCredential.createFrom(result.credential.data)
            a.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await()
            SignInResult.Success
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: Exception) {
            SignInResult.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun signOut(activityContext: Context) {
        auth?.signOut()
        try { CredentialManager.create(activityContext).clearCredentialState(ClearCredentialStateRequest()) } catch (_: Exception) {}
    }
}
