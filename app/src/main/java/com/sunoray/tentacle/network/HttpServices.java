package com.sunoray.tentacle.network;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpServices {

    private static final Logger log = LoggerFactory.getLogger(HttpServices.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    public Response post(String url, Map<String, String> formBody) throws IOException {
        log.info("Making request to: " + url);
        RequestBody body = formBodyBuilder(formBody).build();
        Request request = new Request.Builder()
                .header("User-Agent", System.getProperty("http.agent"))
                .url(url)
                .post(body)
                .build();
        return client.newCall(request).execute();
    }

    public Response postJson(String url, String jsonParams) throws IOException {
        log.debug("Sending call recording to server by " + Thread.currentThread().getName());
        RequestBody body = RequestBody.create(JSON, jsonParams);
        Request request = new Request.Builder()
                .header("User-Agent", System.getProperty("http.agent"))
                .url(url)
                .post(body)
                .build();
        // Responses from the server (code and message)
        return client.newCall(request).execute();
    }

    public Response postFile(String url, File sourceFile) throws IOException {
        //log.debug("Sending file to server by " + Thread.currentThread().getName());
        if (sourceFile.isFile()) {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", sourceFile.getName(), RequestBody.create(MediaType.parse("text/plain"), sourceFile))
                    .build();
            Request request = new Request.Builder()
                    .header("User-Agent", System.getProperty("http.agent"))
                    .url(url)
                    .post(requestBody)
                    .build();
            // Responses from the server (code and message)
            return client.newCall(request).execute();
        } else {
            log.debug("recording file not exist");
            return null;
        }
    }

    private FormBody.Builder formBodyBuilder(Map<String, String> formBody) {
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        try {
            if (formBody != null) {
                for (String key : formBody.keySet()) {
                    bodyBuilder.add(key, formBody.get(key));
                }
            } else {
                bodyBuilder.add("1", "1");
            }
        } catch (Exception e) {
            log.debug("Error in formBodyBuilder: " + e);
        }
        return bodyBuilder;
    }

}