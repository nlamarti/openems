package io.openems.edge.timeofusetariff.edmij;

import okhttp3.Interceptor;
import okhttp3.Chain;
import okhttp3.Response;
import okhttp3.Request;

import java.io.IOException;

class OAuth2Interceptor implements Interceptor {
  private final EdmijAuthRepository repository;

  public OAuth2Interceptor(EdmijAuthRepository repository) {
    this.repository = repository;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request originalRequest = chain.request();
    Request.Builder requestBuilder = originalRequest.newBuilder()
        .header("Authorization", "Bearer " + this.repository.getAccessToken());
    Request newRequest = requestBuilder.build();
    return chain.proceed(newRequest);
  }
}