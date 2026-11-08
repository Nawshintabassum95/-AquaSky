package com.aquasky.navigation.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Firebase Authentication Helper (Email + Password + Email Verification):
 * signIn, signUp, signOut and session persistence (Firebase keeps the user logged in).
 * A new account must verify its email (link sent by Firebase) before it can sign in.
 * The user's profile / role is stored in Cloud Firestore (see FirestoreUserProfile).
 */
public class FirebaseAuthHelper {

    private static final String TAG = "FirebaseAuthHelper";
    private static final String NOT_CONFIGURED =
            "Firebase is not configured. Add google-services.json to the app folder and rebuild.";
    private static FirebaseAuth mAuth;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user, String role);
        void onFailure(String errorMessage);
    }

    private static FirebaseAuth getAuth() {
        if (mAuth == null) {
            try {
                mAuth = FirebaseAuth.getInstance();
            } catch (Exception e) {
                Log.w(TAG, "Firebase Auth unavailable: " + e.getMessage());
            }
        }
        return mAuth;
    }

    public static FirebaseUser getCurrentUser() {
        try {
            FirebaseAuth auth = getAuth();
            if (auth != null) return auth.getCurrentUser();
        } catch (Exception ignored) {}
        return null;
    }

    public static void signIn(String email, String password, @NonNull AuthCallback callback) {
        final FirebaseAuth auth = getAuth();
        if (auth == null) {
            callback.onFailure(NOT_CONFIGURED);
            return;
        }
        try {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        final FirebaseUser user = auth.getCurrentUser();
                        if (task.isSuccessful() && user != null) {

                            // NEW: block login until the email is verified
                            if (!user.isEmailVerified()) {
                                user.sendEmailVerification().addOnCompleteListener(t -> {
                                    auth.signOut();
                                    callback.onFailure("Email not verified. We sent a new verification link to "
                                            + email + ". Please verify, then sign in.");
                                });
                                return;
                            }

                            // role comes from the Firestore "users" document
                            FirestoreUserProfile.loadRole(user.getUid(), role -> callback.onSuccess(user, role));
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Sign in failed";
                            callback.onFailure(err);
                        }
                    });
        } catch (Exception e) {
            callback.onFailure(NOT_CONFIGURED);
        }
    }

    public static void signUp(String email, String password, String role, String fullName,
                              @NonNull AuthCallback callback) {
        final FirebaseAuth auth = getAuth();
        if (auth == null) {
            callback.onFailure(NOT_CONFIGURED);
            return;
        }
        try {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        final FirebaseUser user = auth.getCurrentUser();
                        if (task.isSuccessful() && user != null) {

                            // 1) save profile, and only when it is finished ...
                            FirestoreUserProfile.saveProfile(user.getUid(), fullName, email, role, () -> {
                                // 2) ... send the verification link ...
                                user.sendEmailVerification().addOnCompleteListener(t -> {
                                    // 3) ... then sign out so the user must verify before using the app
                                    auth.signOut();
                                    // (shown as a Toast in LoginActivity)
                                    callback.onFailure("Account created! Verification link sent to " + email
                                            + ". Please verify your email, then sign in.");
                                });
                            });

                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                            callback.onFailure(err);
                        }
                    });
        } catch (Exception e) {
            callback.onFailure(NOT_CONFIGURED);
        }
    }

    public static void signOut() {
        try {
            FirebaseAuth auth = getAuth();
            if (auth != null) auth.signOut();
        } catch (Exception ignored) {}
    }
}