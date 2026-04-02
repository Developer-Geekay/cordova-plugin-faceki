package com.geekay.plugin.faceki

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
        private const val HTTP_PREFIX = "http"
        private const val DRAWABLE_PREFIX = "R.drawable."
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
        val activity = cordova.activity
        cordova.activity.runOnUiThread {
            FaceKi.startKycVerification(
                context = activity,
                verificationLink = verificationLink,
                recordIdentifier = recordIdentifier,
                kycResponseHandler = object : KycResponseHandler {
                    override fun handleKycResponse(json: String?, result: VerificationResult) {
                        try {
                            val response = JSONObject().apply {
                                put("json", json)
                            }
                            when (result) {
                                is VerificationResult.ResultOk -> {
                                    response.put("status", "ResultOk")
                                    callbackContext.success(response)
                                }
                                is VerificationResult.ResultCanceled -> {
                                    response.put("status", "ResultCanceled")
                                    callbackContext.error(response)
                                }
                                else -> {
                                    response.put("status", "Unknown")
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
