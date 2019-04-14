package com.muvaki.samples.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudkms.v1.CloudKMS;
import com.google.api.services.cloudkms.v1.CloudKMS.Builder;
import com.google.api.services.cloudkms.v1.CloudKMSScopes;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class KMSService {

  private CloudKMS cloudKMS;

  public KMSService() throws IOException {
    init();
  }

  private void init() throws IOException {
    final HttpTransport transport = new NetHttpTransport();
    final JsonFactory jsonFactory = new JacksonFactory();

    // Authorize the client using Application Default Credentials
    // @see https://g.co/dv/identity/protocols/application-default-credentials
    GoogleCredential credential = GoogleCredential.getApplicationDefault(transport, jsonFactory);

    // Depending on the environment that provides the default credentials (e.g. Compute Engine, App
    // Engine), the credentials may require us to specify the scopes we need explicitly.
    // Check for this case, and inject the scope if required.
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(CloudKMSScopes.all());
    }

    cloudKMS = new Builder(transport, jsonFactory, credential)
        .setApplicationName("CloudKMS")
        .build();
  }

  public String encrypt(String keyName, byte[] secretKey) throws IOException {
    return cloudKMS.projects()
        .locations()
        .keyRings()
        .cryptoKeys()
        .encrypt(
            keyName,
            new com.google.api.services.cloudkms.v1.model.EncryptRequest()
                .setPlaintext(new String(
                    Base64.getEncoder().encode(secretKey), StandardCharsets.UTF_8))
        ).execute().getCiphertext();
  }
}
