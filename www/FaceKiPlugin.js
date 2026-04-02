var exec = require('cordova/exec');

var FaceKiPlugin = {
    /**
     * Start KYC Verification
     * @param {string} verificationLink 
     * @param {string} recordIdentifier 
     * @param {function} successCallback 
     * @param {function} errorCallback 
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
