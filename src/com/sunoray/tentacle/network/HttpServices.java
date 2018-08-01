package com.sunoray.tentacle.network;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunoray.tentacle.tasks.GetPendingCall;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpServices {
	
	private static final Logger log = LoggerFactory.getLogger(GetPendingCall.class);
	//private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private OkHttpClient client = new OkHttpClient();
	
	public Response post(String url, Map<String, String> formBody) throws IOException {
		//Response response = null;
		//try {
			log.info("Making request to: "+url);
			RequestBody body = formBodyBuilder(formBody).build();
			Request request = new Request.Builder()
			.header("User-Agent", System.getProperty("http.agent"))
			.url(url)
			.post(body)
			.build();
			//response = client.newCall(request).execute();
			return client.newCall(request).execute();
		/*} catch (java.net.UnknownHostException e) {
			log.debug("No Internet Conection");
		} catch (java.net.ConnectException e) {
			log.debug("Error - Server is not reachable");
		} catch (Exception e) {
			log.debug("Exception in HttpServices POST: "+e);
		}
		return response;*/
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
			log.debug("Error in formBodyBuilder: "+e);
		}
		return bodyBuilder;
	}
	
}