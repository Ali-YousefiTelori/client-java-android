package ir.atitec.signalgoApp;

import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.EmptyStackException;

import ir.atitec.signalgo.HttpCore;
import ir.atitec.signalgo.annotations.GoError;
import ir.atitec.signalgo.interfaces.MonitorableMessage;
import ir.atitec.signalgo.util.GoResponseHandler;

public class Constants {


    public final static String serverUrl = "http://192.168.1.5:6578";


    public static void initServerApi() {
        HttpCore.instance()
                .setCookieEnabled(true)
                .setSetUtf8(true)
                // if every response from server have pattern and just different from one param you can set this
                .setResponseClass(MessageContract2.class)
                // when your server support web cookie enable this
                // you can add deserilizer for convert data from server your class
                .addDeserializer(DateTime.class, new StdDeserializer<DateTime>((Class) null) {
                    @Override
                    public DateTime deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                        String s = (String) jsonParser.readValueAs(String.class);
                        long l = Long.parseLong(s);
                        DateTime dateTime = new DateTime().withMillis(l * 1000);
                        return dateTime;
                    }

                })
                // you can add serializer for any class what can't detect from your server like this
                .addSerializer(DateTime.class, new StdSerializer<DateTime>((Class) null) {
                    @Override
                    public void serialize(DateTime dateTime, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
                        jsonGenerator.writeString(dateTime.getMillis() / 1000 + "");
                    }
                })
                // if set this method, every call method from any where you can monitor them and do something you like
                .setMonitorableMessage(new MonitorableMessage() {

                    @Override
                    public void onMonitor(Object response, GoError[] goErrors) {
                        if (response == null) {

                        }
                    }

                    // you must overrid this method when you have not any spcefic response class
                    @Override
                    public void onServerResultWithoutResponse(Object response, GoResponseHandler responseHandler) {
                        if (response == null) {
                            responseHandler.onConnectionError();
                        } else {
                            responseHandler.onSuccess(response);
                        }
//                        if (response != null) {
//                            responseHandler.onSuccess(response);
//                        } else {
//                            responseHandler.onConnectionError();
//                        }
                    }

                    // or you must overrid this method when you haveany spcefic response class like
                    @Override
                    public void onServerResponse(Object response, GoResponseHandler responseHandler) {
                        try {
                            responseHandler.onSuccess(response);

                        } catch (Exception e) {


                        }

                      /*  MessageContract mc = (MessageContract) response;
                        if (mc != null) {

                        } else {
                            responseHandler.onConnectionError();
                        }*/
                    }
                })
                // set server url
                .withUrl(serverUrl).init();

    }

}