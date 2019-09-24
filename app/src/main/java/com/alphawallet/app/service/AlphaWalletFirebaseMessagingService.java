package com.alphawallet.app.service;

//import android.util.Log;
//
//import com.amazonaws.auth.CognitoCachingCredentialsProvider;
//import com.amazonaws.regions.Region;
//import com.amazonaws.regions.Regions;
//import com.amazonaws.services.sns.AmazonSNSClient;
//import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
//import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
//import com.google.firebase.messaging.FirebaseMessagingService;

public class AlphaWalletFirebaseMessagingService
{

}
/*public class AlphaWalletFirebaseMessagingService extends FirebaseMessagingService {
    public static final String TAG = AlphaWalletFirebaseMessagingService.class.getSimpleName();
    public static final String IDENTITY_POOL_ID = "us-west-2:6d3c1431-0764-43f0-8ced-54e584fd01ad";
    public static final String PLATFORM_APPLICATION_ARN = "arn:aws:sns:us-west-2:400248756644:app/GCM/AlphaWallet-Android";

    @Override
    public void onNewToken(String token)
    {
        try
        {
            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    IDENTITY_POOL_ID,
                    Regions.US_WEST_2
            );
            AmazonSNSClient snsClient = new AmazonSNSClient(credentialsProvider);
            snsClient.setRegion(Region.getRegion(Regions.US_WEST_2));
            CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest();
            request.setPlatformApplicationArn(PLATFORM_APPLICATION_ARN);
            request.setToken(token);
            CreatePlatformEndpointResult result = snsClient.createPlatformEndpoint(request);
        }
        catch (Exception e)
        {
            Log.e(TAG, e.getMessage(), e);
        }
        super.onNewToken(token);
    }
}*/
