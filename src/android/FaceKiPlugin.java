package com.geekay.plugin.faceki;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.faceki.android.FaceKi;
import com.faceki.android.KycResponseHandler;
import com.faceki.android.VerificationResult;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class FaceKiPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("startKycVerification")) {
            String verificationLink = args.getString(0);
            String recordIdentifier = args.getString(1);
            this.startKycVerification(verificationLink, recordIdentifier, callbackContext);
            return true;
        } else if (action.equals("setCustomColors")) {
            JSONObject colorMapJson = args.getJSONObject(0);
            this.setCustomColors(colorMapJson, callbackContext);
            return true;
        } else if (action.equals("setCustomIcons")) {
            JSONObject iconMapJson = args.getJSONObject(0);
            this.setCustomIcons(iconMapJson, callbackContext);
            return true;
        }
        return false;
    }

    private void startKycVerification(String verificationLink, String recordIdentifier, final CallbackContext callbackContext) {
        final Activity activity = this.cordova.getActivity();
        
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FaceKi.startKycVerification(
                    activity,
                    verificationLink,
                    recordIdentifier,
                    new KycResponseHandler() {
                        @Override
                        public void handleKycResponse(String json, VerificationResult result) {
                            JSONObject response = new JSONObject();
                            try {
                                response.put("json", json);
                                if (result instanceof VerificationResult.ResultOk) {
                                    response.put("status", "ResultOk");
                                    callbackContext.success(response);
                                } else if (result instanceof VerificationResult.ResultCanceled) {
                                    response.put("status", "ResultCanceled");
                                    callbackContext.error(response);
                                } else {
                                    response.put("status", "Unknown");
                                    callbackContext.error(response);
                                }
                            } catch (JSONException e) {
                                callbackContext.error("JSON Error: " + e.getMessage());
                            }
                        }
                    }
                );
            }
        });
    }

    private void setCustomColors(JSONObject colorMapJson, CallbackContext callbackContext) {
        HashMap<FaceKi.ColorElement, FaceKi.ColorValue> colorMap = new HashMap<>();
        Iterator<String> keys = colorMapJson.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                String colorHex = colorMapJson.getString(key);
                FaceKi.ColorElement element = FaceKi.ColorElement.valueOf(key);
                colorMap.put(element, new FaceKi.ColorValue.StringColor(colorHex));
            } catch (Exception e) {
                // Skip invalid elements or colors
            }
        }

        FaceKi.setCustomColors(colorMap);
        callbackContext.success("Colors set successfully");
    }

    private void setCustomIcons(JSONObject iconMapJson, CallbackContext callbackContext) {
        HashMap<FaceKi.IconElement, FaceKi.IconValue> iconMap = new HashMap<>();
        Iterator<String> keys = iconMapJson.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                String iconValue = iconMapJson.getString(key);
                FaceKi.IconElement element = FaceKi.IconElement.valueOf(key);
                
                if (iconValue.startsWith("http")) {
                    iconMap.put(element, new FaceKi.IconValue.Url(iconValue));
                } else if (iconValue.startsWith("R.drawable.")) {
                    String resName = iconValue.replace("R.drawable.", "");
                    int resId = cordova.getActivity().getResources().getIdentifier(resName, "drawable", cordova.getActivity().getPackageName());
                    if (resId != 0) {
                        iconMap.put(element, new FaceKi.IconValue.Resource(resId));
                    }
                }
            } catch (Exception e) {
                // Skip invalid elements or icons
            }
        }

        FaceKi.setCustomIcons(iconMap);
        callbackContext.success("Icons set successfully");
    }
}
