package io.stormbird.token.tools;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.joda.time.LocalDate;

import java.util.Collections;

public class LambdaProxyHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        LocalDate currentDate = LocalDate.now(); // make shadowJar include DateTime.class
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Collections.emptyMap())
                .withBody("{\"inputs\":\"" + input.getBody() + "\"}");
    }
}