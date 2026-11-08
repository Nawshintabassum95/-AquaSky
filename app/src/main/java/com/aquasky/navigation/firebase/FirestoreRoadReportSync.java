package com.aquasky.navigation.firebase;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Cloud Firestore Integration:
 * - Community road / hazard reports saved permanently in the "road_reports" collection
 */
public class FirestoreRoadReportSync {

    private static final String TAG = "FirestoreRoadReport";

    public interface ReportCallback {
        void onSuccess(String documentId);
        void onFailure(String message);
    }

    public static void submitRoadReport(String title, String category, double lat, double lng,
                                        String description, ReportCallback callback) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("title", title);
            report.put("category", category);
            report.put("latitude", lat);
            report.put("longitude", lng);
            report.put("description", description);
            report.put("status", "PENDING");
            report.put("timestamp", System.currentTimeMillis());

            FirebaseUser user = FirebaseAuthHelper.getCurrentUser();
            if (user != null) {
                report.put("reportedBy", user.getEmail());
                report.put("uid", user.getUid());
            }

            FirebaseFirestore.getInstance().collection("road_reports")
                    .add(report)
                    .addOnSuccessListener(doc -> {
                        Log.i(TAG, "Report saved: " + doc.getId());
                        if (callback != null) callback.onSuccess(doc.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed: " + e.getMessage());
                        if (callback != null) callback.onFailure(e.getMessage());
                    });
        } catch (Exception e) {
            Log.w(TAG, "Firestore not available: " + e.getMessage());
            if (callback != null) callback.onFailure("Firebase is not configured (google-services.json missing).");
        }
    }
}
