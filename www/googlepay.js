var exec = require('cordova/exec');

var GooglePayPlugin = {
    requestPayment: function(paymentDataRequest, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'GooglePayPlugin', 'requestPayment', [paymentDataRequest]);
    },
    canMakePayment: function(allowedPaymentMethods, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'GooglePayPlugin', 'canMakePayment', [allowedPaymentMethods]);
    }
};

module.exports = GooglePayPlugin;
