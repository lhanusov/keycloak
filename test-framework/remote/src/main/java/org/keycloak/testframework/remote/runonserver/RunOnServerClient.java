package org.keycloak.testframework.remote.runonserver;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServerException;
import org.keycloak.testframework.remote.providers.runonserver.SerializationUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public class RunOnServerClient {

    private static final String RUN_ON_SERVER_ENDPOINT = "/testing-run-on-server";
    private final HttpClient httpClient;
    private final String serverUrl;
    private final String realm;

    public RunOnServerClient(HttpClient httpClient, String serverUrl, String realm) {
        this.httpClient = httpClient;
        this.serverUrl = serverUrl;
        this.realm = realm;
    }

    public <T> T fetch(FetchOnServerWrapper<T> wrapper) throws RunOnServerException {
        return fetch(wrapper.getRunOnServer(), wrapper.getResultClass());
    }

    public <T> T fetch(FetchOnServer function, Class<T> clazz) throws RunOnServerException {
        try {
            String s = fetchString(function);
            return s==null ? null : JsonSerialization.readValue(s, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String fetchString(FetchOnServer function) throws RunOnServerException {
        String encoded = SerializationUtil.encode(function);

        String result = runOnServer(encoded);
        if (result != null && !result.isEmpty() && result.trim().startsWith("EXCEPTION:")) {
            Throwable t = SerializationUtil.decodeException(result);
            if (t instanceof AssertionError) {
                throw (AssertionError) t;
            } else {
                throw new RunOnServerException(t);
            }
        } else {
            return result;
        }
    }

    public void run(RunOnServer function) throws RunOnServerException {
        String encoded = SerializationUtil.encode(function);

        String result = runOnServer(encoded);
        if (result != null && !result.isEmpty() && result.trim().startsWith("EXCEPTION:")) {
            Throwable t = SerializationUtil.decodeException(result);
            if (t instanceof AssertionError) {
                throw (AssertionError) t;
            } else {
                throw new RunOnServerException(t);
            }
        }
    }

    public String runOnServer(String encoded) throws RunOnServerException {
        try {
            HttpPost request = new HttpPost(serverUrl + "/realms/" + realm + RUN_ON_SERVER_ENDPOINT);
            request.setEntity(new StringEntity(encoded));
            request.setHeader("Content-type", "text/plain;charset=utf-8");

            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != Response.Status.OK.getStatusCode()) {
                var statusLine = response.getStatusLine();
                throw new WebApplicationException(String.format("Unexpected response status for RunOnServer: %d %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            }
            return response.getEntity().getContent().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
