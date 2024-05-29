package com.plugin.googlepay;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GooglePayPlugin extends CordovaPlugin {

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;
    private PaymentsClient paymentsClient;
    private CallbackContext callbackContext;
    private static final String TAG = "GooglePayPlugin";

    @Override
    protected void pluginInitialize() {
        String environment = preferences.getString("GooglePayEnvironment", "TEST");
        int env = environment.equalsIgnoreCase("PRODUCTION") ? WalletConstants.ENVIRONMENT_PRODUCTION
                : WalletConstants.ENVIRONMENT_TEST;

        paymentsClient = Wallet.getPaymentsClient(this.cordova.getActivity(),
                new Wallet.WalletOptions.Builder().setEnvironment(env).build());
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("requestPayment")) {
            requestPayment(args);
            return true;
        } else if (action.equals("canMakePayment")) {
            canMakePayment(args);
            return true;
        }
        return false;
    }

    private void requestPayment(JSONArray args) {
        try {
            JSONObject paymentDataRequestJson = args.getJSONObject(0);
            PaymentDataRequest request = createPaymentDataRequest(paymentDataRequestJson);

            if (request != null) {
                Activity activity = this.cordova.getActivity();
                cordova.setActivityResultCallback(this);
                AutoResolveHelper.resolveTask(
                        paymentsClient.loadPaymentData(request),
                        activity,
                        LOAD_PAYMENT_DATA_REQUEST_CODE);
            } else {
                callbackContext.error("PaymentDataRequest is null");
            }
        } catch (JSONException e) {
            callbackContext.error("JSON Exception: " + e.getMessage());
        }
    }

    private PaymentDataRequest createPaymentDataRequest(JSONObject paymentDataRequestJson) {
        try {
            return PaymentDataRequest.fromJson(paymentDataRequestJson.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating PaymentDataRequest: ", e);
            return null;
        }
    }

    private void canMakePayment(JSONArray args) {
      Log.e(TAG, "canMakePayment called");
        try {
            JSONObject allowedPaymentMethodsJson = args.getJSONObject(0);
            IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(allowedPaymentMethodsJson.toString());
            Task<Boolean> task = paymentsClient.isReadyToPay(request);
            task.addOnCompleteListener(
                    task1 -> {
                        try {
                            boolean result = task1.getResult(ApiException.class);
                            JSONObject jsonResult = new JSONObject();
                            jsonResult.put("canMakePayments", result);
                            callbackContext.success(jsonResult);
                        } catch (ApiException e) {
                            callbackContext.error("Error checking readiness to pay: " + e.getMessage());
                        } catch (JSONException e) {
                            callbackContext.error("JSON Exception: " + e.getMessage());
                        }
                    });
        } catch (JSONException e) {
            callbackContext.error("JSON Exception: " + e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    handlePaymentSuccess(data);
                    break;
                case Activity.RESULT_CANCELED:
                    callbackContext.error("Payment cancelled");
                    break;
                case AutoResolveHelper.RESULT_ERROR:
                    handlePaymentError(data);
                    break;
                default:
                    callbackContext.error("Unknown resultCode: " + resultCode);
            }
        }
    }

    private void handlePaymentSuccess(Intent data) {
        PaymentData paymentData = PaymentData.getFromIntent(data);
        if (paymentData != null) {
            String paymentInformation = paymentData.toJson();
            if (paymentInformation != null) {
                try {
                    JSONObject paymentResponse = new JSONObject(paymentInformation);
                    callbackContext.success(paymentResponse);
                } catch (JSONException e) {
                    callbackContext.error("Error parsing payment data: " + e.getMessage());
                }
            } else {
                callbackContext.error("PaymentData toJson is null");
            }
        } else {
            callbackContext.error("PaymentData is null");
        }
    }

    private void handlePaymentError(Intent data) {
        try {
            Status status = AutoResolveHelper.getStatusFromIntent(data);
            String statusMessage = status.getStatusMessage();
            int statusCode = status.getStatusCode();
            callbackContext.error("Payment failed: " + statusCode + " - " + statusMessage);
        } catch (Exception e) {
            callbackContext.error("Error handling payment error: " + e.getMessage());
        }
    }
}
