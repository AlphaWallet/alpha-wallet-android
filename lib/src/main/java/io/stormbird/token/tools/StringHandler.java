package io.stormbird.token.tools;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class StringHandler implements RequestHandler<String, String> {
  public String handleRequest(String myCount, Context context) {
    return "you said " + myCount;
  }
}