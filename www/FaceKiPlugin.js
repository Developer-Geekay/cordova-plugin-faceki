var exec = require('cordova/exec');

var FaceKiPlugin = {
    /**
     * Start KYC Verification
     * @param {string} verificationLink - The link ID (UUID) from the 'data' field of the
     *   FaceKi link-generation API response, e.g. "b1031cff-4ecd-46dd-9aae-a2be2336a123".
     *   A full URL such as "https://verification.faceki.com/<uuid>" is also accepted;
     *   the plugin will extract the UUID automatically.
     * @param {string} recordIdentifier - Your internal record/user identifier
     * @param {function} successCallback - Called with { status: "ResultOk", data: <object> }
     *   where `data` is the parsed KYC API response object (or raw string if unparseable).
     * @param {function} errorCallback   - Called with { status: "ResultCanceled", data: <object> }
     *   when the user cancels, or with a plain string message on SDK/network error.
     */
    startKycVerification: function (verificationLink, recordIdentifier, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'FaceKiPlugin', 'startKycVerification', [verificationLink, recordIdentifier]);
    },

    /**
     * Set Custom Colors
     * @param {object} colorMap - e.g., { "BackgroundColor": "#FFFFFF" }
     * @param {function} successCallback 
     * @param {function} errorCallback 
     */
    setCustomColors: function (colorMap, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'FaceKiPlugin', 'setCustomColors', [colorMap]);
    },

    /**
     * Set Custom Icons
     * @param {object} iconMap - e.g., { "Logo": "R.drawable.logo" or "https://..." }
     * @param {function} successCallback 
     * @param {function} errorCallback 
     */
    setCustomIcons: function (iconMap, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'FaceKiPlugin', 'setCustomIcons', [iconMap]);
    }
};

module.exports = FaceKiPlugin;
