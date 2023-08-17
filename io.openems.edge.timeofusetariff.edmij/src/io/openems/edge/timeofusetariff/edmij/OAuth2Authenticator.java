package io.openems.edge.timeofusetariff.edmij;

import okhttp3.Request;
import okhttp3.Route;
import okhttp3.Response;
import okhttp3.Authenticator;
import java.io.IOException;

public class OAuth2Authenticator implements Authenticator {
  private final EdmijAuthRepository repository;

  public OAuth2Authenticator(EdmijAuthRepository repository) {
    this.repository = repository;
  }


  @Override
  public Request authenticate(Route route, Response response) throws IOException {
    this.repository.refresh();
    return response.request().newBuilder()
        .header("Authorization", "Bearer " + this.repository.getAccessToken())
        .build();
  }
}
