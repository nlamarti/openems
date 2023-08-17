package io.openems.edge.timeofusetariff.edmij;

import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class EdmijAuthRepository {
  private static final String EDMIJ_API_URL = "https://trading.edmij.nl";
  private static final String TOKEN_ENDPOINT = "/connect/token";
  private Authentication authentication;
  private String email;
  private String password;
  private OkHttpClient client;

  public void refresh() throws IOException {
    if (!this.authentication.refreshToken().isEmpty()) {
      this.executeRequest(new FormBody.Builder()
          .add("grant_type", "refresh_token")
          .add("refresh_token", this.authentication.refreshToken())
          .add("scope", "openid offline_access roles")
          .add("resource", EDMIJ_API_URL)
          .build());
    } else {
      this.login();
    }
  }

  private void executeRequest(FormBody requestBody) throws IOException {
    Request request = new Request.Builder()
        .url(EDMIJ_API_URL + TOKEN_ENDPOINT)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .post(requestBody)
        .build();

    try (Response response = this.client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected response code: " + response);
      }
      Gson gson = new Gson();
      this.authentication = gson.fromJson(response.body().string(), Authentication.class);
    }
  }

  private void login() throws IOException {
    this.executeRequest(new FormBody.Builder()
        .add("grant_type", "password")
        .add("username", this.email)
        .add("password", this.password)
        .add("scope", "openid offline_access roles")
        .add("resource", EDMIJ_API_URL)
        .build());
  }

  public EdmijAuthRepository(String email, String password) {
    this.email = email;
    this.password = password;
    client = new OkHttpClient.Builder().build();
  }

  public String getAccessToken() {
    return accessToken;
  }
}