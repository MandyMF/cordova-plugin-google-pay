An Android only plugin for processing Google Pay payments from your app.

### Installation:

For stable relases type:

```shell
cordova plugin add @bkamenov/cordova-plugin-google-pay
```

For latest releases type:

```shell
cordova plugin add https://github.com/bkamenov/cordova-plugin-google-pay
```

After install, the plugin will be using the **TEST** environment of GooglePay.

To make it more obvious what environment you are currently using edit your app's **config.xml** as follows:

For **PRODUCTION**:

```xml
<platform name="android">
    ...
    <preference name="GooglePayEnvironment" value="PRODUCTION"/>
</platform>
```

For **TEST**:

```xml
<platform name="android">
    ...
    <preference name="GooglePayEnvironment" value="TEST"/>
</platform>
```

### Usage:

```js
//You can adjust it as you wish (see google.payments.api.PaymentDataRequest)
const paymentDataRequest = {
  apiVersion: 2,
  apiVersionMinor: 0,
  allowedPaymentMethods: [
    {
      type: 'CARD',
      parameters: {
        allowedAuthMethods: ['PAN_ONLY', 'CRYPTOGRAM_3DS'],
        allowedCardNetworks: ['AMEX', 'MASTERCARD', 'VISA']
      },
      tokenizationSpecification: {
        type: 'PAYMENT_GATEWAY',
        parameters: {
          gateway: 'example', //This is your payment service provider e.g. "stripe"
          gatewayMerchantId: 'exampleGatewayMerchantId' //Your merchant ID within "stripe
        }
      }
    }
  ],
  merchantInfo: {
    //merchantId: '01234567890123456789', //Your Google merchant ID. Uncomment in PRODUCTION with real ID.
    merchantName: 'Example Merchant' //Your Google merchant name
  },
  transactionInfo: {
    totalPriceStatus: 'FINAL',
    totalPrice: '12.34',
    currencyCode: 'USD',
    countryCode: 'US'
  }
};

//Just make a copy of some portions of the above request (see google.payments.api.IsReadyToPayRequest)
const canMakePaymentRequest = {
  apiVersion: paymentDataRequest.apiVersion,
  apiVersionMinor: paymentDataRequest.apiVersionMinor,
  allowedPaymentMethods: [
    {
      type: paymentDataRequest.allowedPaymentMethods[0].type,
      parameters: {
        allowedCardNetworks: paymentDataRequest.allowedPaymentMethods[0].parameters.allowedCardNetworks,
        allowedAuthMethods: paymentDataRequest.allowedPaymentMethods[0].parameters.allowedAuthMethods
      }
    }
  ]
};

//Call this when you want to display the button.
cordova.plugins.GooglePayPlugin.canMakePayment(canMakePaymentRequest,
(result) => {
  if(result.canMakePayments) {

    const gpayButton = document.getElementById("myGooglePayButton");
    if(!gpayButton)
      return; //This should not happen.

     gpayButton.onclick = () => {
      cordova.plugins.GooglePayPlugin.requestPayment(paymentDataRequest,
      (paymentData) {
        //Pass this to the payment gateway (e.g. your backend) and from there to the payment service provider
        const paymentToken = paymentData.paymentMethodData.tokenizationData.token;
        //console.log(JSON.stringify(paymentData)); //Uncomment to take a look what is inside
        //...or see google.payments.api.PaymentData
      },
      (error) => {
        if(error === "Payment cancelled") {
          return; //Payment was cancelled
        }

        //Show to the user that there was a payment problem
        ...

        console.error("There was an error while processing your payment. Details: " + error);
      });
    };

    //Make your button visible
    gpayButton.style.visibility = "visible";
  }
  else {
    //Show that this device does not support the desired payment methods.
    ...
  }
},
(error) => {
  console.error("An error occured while chcking if payment methods are supported by GooglePay. Details: " + error);
});


```

```html
...
<button id="myGooglePayButton" style="visibility:hidden">Place order</button>
...
```
