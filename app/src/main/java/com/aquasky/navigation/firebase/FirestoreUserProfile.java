package com.aquasky.navigation.firebase;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Cloud Firestore: one document per registered user in the "users" collection.
 * Written at sign-up, read at sign-in (this is where the user's role comes from).
 */
public class FirestoreUserProfile {

    private static final String TAG = "FirestoreUserProfile";
    public static final String DEFAULT_ROLE = "Master Navigator";

    public interface RoleCallback {
        void onRole(String role);
    }

    public interface ProfileCallback {
        void onProfile(String name, String role);
    }

    /** Reads the signed-in user's name and role from the "users" collection. */
    public static void loadProfile(String uid, ProfileCallback callback) {
        try {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        String name = "";
                        String role = DEFAULT_ROLE;
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String n = task.getResult().getString("name");
                            String r = task.getResult().getString("role");
                            if (n != null) name = n;
                            if (r != null) role = r;
                        }
                        callback.onProfile(name, role);
                    });
        } catch (Exception e) {
            callback.onProfile("", DEFAULT_ROLE);
        }
    }

    /** Same as above, but runs onDone after the write finishes (success OR failure). */
    public static void saveProfile(String uid, String name, String email, String role, Runnable onDone) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", name);
            doc.put("email", email);
            doc.put("role", role);
            doc.put("createdAt", System.currentTimeMillis());
            FirebaseFirestore.getInstance().collection("users").document(uid).set(doc)
                    .addOnCompleteListener(t -> {
                        if (t.isSuccessful()) Log.i(TAG, "Profile saved for " + uid);
                        else Log.e(TAG, "Profile save failed: " + (t.getException() != null ? t.getException().getMessage() : ""));
                        if (onDone != null) onDone.run();
                    });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
            if (onDone != null) onDone.run();
        }
    }

    public static void loadRole(String uid, RoleCallback callback) {
        try {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        String role = DEFAULT_ROLE;
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String stored = task.getResult().getString("role");
                            if (stored != null) role = stored;
                        }
                        callback.onRole(role);
                    });
        } catch (Exception e) {
            callback.onRole(DEFAULT_ROLE);
        }
    }
}
