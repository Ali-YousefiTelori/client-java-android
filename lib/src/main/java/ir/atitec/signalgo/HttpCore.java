package ir.atitec.signalgo;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.joda.time.DateTimeZone;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import ir.atitec.signalgo.annotations.GoMethodName;
import ir.atitec.signalgo.interfaces.MonitorableMessage;
import ir.atitec.signalgo.models.Response;
import ir.atitec.signalgo.util.GoBackStackHelper;
import ir.atitec.signalgo.util.GoConvertorHelper;
import ir.atitec.signalgo.util.GoResponseHandler;

/**
 * Created by hamed on 12/13/2017.
 */

public class HttpCore extends Core {

    private boolean cookieEnabled = false;
    private RestTemplate restTemplate;
    private List<String> cookie;


    private HttpCore() {


//
//        objectMapper = new ObjectMapper();
//        objectMapper.configure(MapperFeature.USE_ANNOTATIONS, true);
//        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
//        objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
//        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//        //mapper.configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true);
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        objectMapper.setTimeZone(DateTimeZone.getDefault().toTimeZone());

    }

    public synchronized static HttpCore instance() {
        Core c = Core.map.get(HttpCore.class);
        if (c == null) {
            c = new HttpCore();
            Core.map.put(HttpCore.class, c);
        }
        return (HttpCore) c;
    }


    private void post(String url, GoResponseHandler responseHandler, Object... params) {

        if (Build.VERSION.SDK_INT > 16)
            new MyAsync(getUrl() + url, responseHandler, HttpMethod.POST).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            new MyAsync(getUrl() + url, responseHandler, HttpMethod.POST).execute(params);
    }

    private void postMultipart(String url, String[] keys, GoResponseHandler responseHandler, Object... params) {

        if (Build.VERSION.SDK_INT > 16)
            new MyAsync(getUrl() + url, responseHandler, HttpMethod.POST).setKeys(keys).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            new MyAsync(getUrl() + url, responseHandler, HttpMethod.POST).setKeys(keys).execute(params);
    }

    private void get(String url, GoResponseHandler responseHandler) {
        if (Build.VERSION.SDK_INT > 16)
            new MyAsync(getUrl() + url, responseHandler, HttpMethod.GET).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            new MyAsync(getUrl() + url, responseHandler, HttpMethod.GET).execute();
    }

    private void uploadFile(String url, GoResponseHandler responseHandler, File file) {
        if (Build.VERSION.SDK_INT > 16)
            new MyAsync(getUrl() + url, responseHandler, HttpMethod.POST).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file);
        else
            new MyAsync(getUrl() + url, responseHandler, HttpMethod.POST).execute(file);
    }


    private GoMethodName findMethod(GoResponseHandler responseHandler) {
        try {
            GoMethodName methodName = GoBackStackHelper.getHttpMethodName();
            responseHandler.setCore(this);
            responseHandler.setGoMethodName(methodName);
            return methodName;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class MyAsync extends AsyncTask<Object, Void, Object> {
        GoResponseHandler responseHandler;
        String url;
        HttpMethod httpMethod;
        String[] keys;

        public MyAsync(String url, GoResponseHandler responseHandler, HttpMethod httpMethod) {
            this.responseHandler = responseHandler;
            this.url = url;
            this.httpMethod = httpMethod;
        }

        public MyAsync setKeys(String[] keys) {
            this.keys = keys;
            return this;
        }

        @Override
        protected Object doInBackground(Object... objects) {
            try {

                ResponseEntity responseEntity =
                        restTemplate.exchange(url, httpMethod, getEntuty(objects, keys), String.class);
                Object response = getObjectMapper().readValue((String) responseEntity.getBody(), getObjectMapper().constructType(responseHandler.getType()));
                if (cookieEnabled)
                    cookie = responseEntity.getHeaders().get("Set-Cookie");
                //if (response != null)
                //  Log.e("Core", "response : " + url + "  " + response.message + " " + response.stack);
                return response;
            } catch (Exception e) {
                Log.e("Core", "exception : " + url + "  " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object response) {
            super.onPostExecute(response);
            responseHandler.onServerResponse(response);
        }

        private HttpEntity getEntuty(Object[] objects, String[] keys) {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (cookie != null && cookieEnabled) {
                httpHeaders.put("Cookie", cookie);
            }
            HttpEntity httpEntity = null;
            if (objects != null && objects.length > 0) {
                if (objects.length == 1) {
                    if (objects[0] instanceof File) {
                        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
                        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
                        FileSystemResource value = new FileSystemResource((File) objects[0]);
                        map.add("file", value);
                        httpEntity = new HttpEntity(map, httpHeaders);
                    } else {
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        httpEntity = new HttpEntity(objects[0], httpHeaders);
                    }
                } else if (keys != null && keys.length == objects.length) {
                    httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
                    LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
                    for (int i = 0; i < objects.length; i++) {
                        map.add(keys[i], objects[i]);
                    }
                    httpEntity = new HttpEntity(map, httpHeaders);
                } else {
                    httpEntity = new HttpEntity(httpHeaders);
                }
            } else {
                httpEntity = new HttpEntity(httpHeaders);
            }
            return httpEntity;
        }
    }


    public boolean isCookieEnabled() {
        return cookieEnabled;
    }

    public Core setCookieEnabled(boolean cookieEnabled) {
        this.cookieEnabled = cookieEnabled;
        return this;
    }

    @Override
    public void init() {
        super.init();
        restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter m = new MappingJackson2HttpMessageConverter();
        m.setObjectMapper(getObjectMapper());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        restTemplate.getMessageConverters().add(m);
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                Log.d("Core", response.toString());
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                Log.d("Core", response.toString());
            }
        });
    }

    @Override
    public void callMethod(GoResponseHandler responseHandler, Object... params) {
        GoMethodName methodName = findMethod(responseHandler);
        if (methodName == null) {
            throw new RuntimeException("can't find Annotaion GoMethodName on method");
        }
        if (methodName.type().getId() < GoMethodName.MethodType.httpGet.getId()) {
            throw new RuntimeException("method type is wrong");
        }
        int i = 0;
        String url = methodName.name();
        do {
            int index = 0, index2 = 0;
            index = url.indexOf("{", index + 1);
            index2 = url.indexOf("}", index2 + 1);
            if (index == -1 || index2 == -1) {
                break;
            }
            url = url.replace("{" + url.substring(index+1, index2) + "}", params[i] + "");
            i++;
        } while (true);

        Object[] pa = {};

        if (params.length > i) {
            pa = new Object[params.length - i];
            for (int j = i; j < params.length; j++) {
                pa[j - i] = params[j];
            }
        }

        if (methodName.type().getId() == GoMethodName.MethodType.httpGet.getId()) {
            get(url, responseHandler);
        } else if (methodName.type().getId() == GoMethodName.MethodType.httpPost.getId()) {
            if (pa.length <=1)
                post(url, responseHandler, pa);
            else
                postMultipart(url, methodName.multipartKeys(), responseHandler, pa);
        } else if (methodName.type().getId() == GoMethodName.MethodType.httpUploadFile.getId()) {
            File f = (File) params[i];
            uploadFile(url, responseHandler, f);
        }
    }


}