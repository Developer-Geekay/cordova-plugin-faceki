package com.geekay.plugin.faceki

import android.util.Log
import com.faceki.android.FaceKi
import com.faceki.android.KycResponseHandler
import com.faceki.android.VerificationResult
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class FaceKiPlugin : CordovaPlugin() {

    companion object {
        private const val TAG = "FaceKiPlugin"
        private const val HTTP_PREFIX = "http"
        private const val DRAWABLE_PREFIX = "R.drawable."
    }

    /**
     * The SDK expects just the UUID/link-ID, not a full URL.
     * e.g. "b1031cff-4ecd-46dd-9aae-a2be2336a123" NOT "https://verification.faceki.com/b1031cff-..."
     * If the caller passes the full URL we extract the last path segment.
     */
    private fun extractLinkId(verificationLink: String): String {
        return if (verificationLink.startsWith(HTTP_PREFIX)) {
            verificationLink.substringAfterLast("/").substringBefore("?").also {
                Log.d(TAG, "Extracted link ID '$it' from full URL")
            }
        } else {
            verificationLink
        }
    }

    override fun execute(action: String, args: JSONArray, callbackContext: CallbackContext): Boolean {
        return when (action) {
            "startKycVerification" -> {
                startKycVerification(args.getString(0), args.getString(1), callbackContext)
                true
            }
            "setCustomColors" -> {
                setCustomColors(args.getJSONObject(0), callbackContext)
                true
            }
            "setCustomIcons" -> {
                setCustomIcons(args.getJSONObject(0), callbackContext)
                true
            }
            else -> false
        }
    }

    private fun startKycVerification(
        verificationLink: String,
        recordIdentifier: String,
        callbackContext: CallbackContext
    ) {
        val linkId = extractLinkId(verificationLink)
        val activity = cordova.activity

        // The FaceKi SDK v2.x does not catch JsonDataException (a RuntimeException from Moshi)
        // when the API returns an error body. Install a temporary uncaught-exception guard so
        // that SDK crashes are surfaced as JS errors instead of crashing the host app.
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Thread.setDefaultUncaughtExceptionHandler(originalHandler)
            val rootCause = generateSequence(throwable, Throwable::cause).last()
            Log.e(TAG, "Uncaught exception: ${rootCause.javaClass.name}: ${rootCause.message}")
            if (rootCause.javaClass.name.contains("JsonDataException")) {
                // SDK crashed parsing an error response — report to JS, do not crash the app
                activity.runOnUiThread {
                    callbackContext.error("Verification failed: ${rootCause.message ?: "Invalid verification link"}")
                }
            } else {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }

        cordova.activity.runOnUiThread {
            FaceKi.startKycVerification(
                context = activity,
                verificationLink = linkId,
                recordIdentifier = recordIdentifier,
                kycResponseHandler = object : KycResponseHandler {
                    override fun handleKycResponse(json: String?, result: VerificationResult) {
                        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
                        try {
                            val response = JSONObject().apply {
                                // Parse the raw JSON string into an object when possible so
                                // consumers get a structured payload rather than a raw string.
                                // Falls back to the raw string if it is not valid JSON.
                                if (json != null) {
                                    try {
                                        put("data", JSONObject(json))
                                    } catch (e: JSONException) {
                                        put("data", json)
                                    }
                                }
                            }
                            // VerificationResult is a sealed class with exactly two variants:
                            //   ResultOk       — the user completed the KYC flow
                            //   ResultCanceled — the user pressed back / cancelled
                            // Both cases carry the API response in `json`; the when is exhaustive.
                            when (result) {
                                is VerificationResult.ResultOk -> {
                                    response.put("status", "ResultOk")
                                    callbackContext.success(response)
                                }
                                is VerificationResult.ResultCanceled -> {
                                    response.put("status", "ResultCanceled")
                                    callbackContext.error(response)
                                }
                            }
                        } catch (e: JSONException) {
                            callbackContext.error("JSON Error: ${e.message}")
                        }
                    }
                }
            )
        }
    }

    private fun setCustomColors(colorMapJson: JSONObject, callbackContext: CallbackContext) {
        val colorMap = hashMapOf<FaceKi.ColorElement, FaceKi.ColorValue>()
        colorMapJson.keys().asSequence().forEach { key ->
            try {
                val element = FaceKi.ColorElement.valueOf(key)
                colorMap[element] = FaceKi.ColorValue.StringColor(colorMapJson.getString(key))
            } catch (e: Exception) {
                // Skip invalid color elements
            }
        }
        FaceKi.setCustomColors(colorMap)
        callbackContext.success("Colors set successfully")
    }

    private fun setCustomIcons(iconMapJson: JSONObject, callbackContext: CallbackContext) {
        val iconMap = hashMapOf<FaceKi.IconElement, FaceKi.IconValue>()
        iconMapJson.keys().asSequence().forEach { key ->
            try {
                val element = FaceKi.IconElement.valueOf(key)
                val value = iconMapJson.getString(key)
                val iconValue = when {
                    value.startsWith(HTTP_PREFIX) -> FaceKi.IconValue.Url(value)
                    value.startsWith(DRAWABLE_PREFIX) -> {
                        val resName = value.removePrefix(DRAWABLE_PREFIX)
                        val resId = cordova.activity.resources.getIdentifier(
                            resName, "drawable", cordova.activity.packageName
                        )
                        if (resId != 0) FaceKi.IconValue.Resource(resId) else null
                    }
                    else -> null
                }
                iconValue?.let { iconMap[element] = it }
            } catch (e: Exception) {
                // Skip invalid icon elements
            }
        }
        FaceKi.setCustomIcons(iconMap)
        callbackContext.success("Icons set successfully")
    }
}
