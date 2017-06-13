package signalgo.client;

import signalgo.client.annotations.GoServiceName;
import signalgo.client.models.GoKeyValue;
import signalgo.client.models.MethodCallInfo;
import signalgo.client.models.MethodCallbackInfo;
import signalgo.client.util.GoAsyncHelper;
import signalgo.client.util.GoAutoResetEvent;
import signalgo.client.util.GoClientHelper;
import signalgo.client.models.GoCompressMode;
import signalgo.client.util.GoConvertorHelper;
import signalgo.client.models.GoDataType;
import signalgo.client.util.GoBackStackHelper;
import signalgo.client.GoSocketListener.SocketState;
import sun.rmi.runtime.Log;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Mehdi Akbarian on 2016-08-04.
 */
public class Connector {

    private static String CHARSET = "UTF-8";
    private String mHostName;
    private Socket socket;
    private boolean onRecievedExeption = false, isAlive;
    private Selector selector;
    private int mPort, timeoutMills = 20000, pingpongTimeout = 10000, pingPongPeriod = 2 * 10000;
    private ConcurrentHashMap<String, ClientDuplex> mPendingServices;
    private InputStream inputStream;
    private OutputStream outputStream;
    private GoCallbackHandler callbackHandler;
    private GoStreamReader goStreamReader;
    private GoStreamWriter goStreamWriter;
    private GoClientHelper clientHelper;
    private GoConvertorHelper convertorHelper;
    private GoSocketListener socketListener;
    private SocketState currentState;
    private SocketState lastState;
    private Thread t;
    private Timer timer;
    private TimerTask timerTask;
    Runnable listener;

    public Connector() {
        System.out.println("signalGo   new connector instant create");
        this.currentState = SocketState.Disconnected;
        this.lastState = SocketState.Disconnected;
        convertorHelper = new GoConvertorHelper();
        goStreamReader = new GoStreamReader();
        goStreamWriter = new GoStreamWriter();
        clientHelper = new GoClientHelper();
        autoPingPong();
    }

    /**
     * make new connection to server
     *
     * @return
     */
    public Connector connectAsync(final String url) {
        GoAsyncHelper.run(new Runnable() {
            public void run() {
                try {
                    connect(url);
                } catch (Exception ex) {
                    exceptionHandler(ex);
                    notifyListener(SocketState.Disconnected);
                }
            }
        }, false);
        return this;
    }

    public Connector connect(String url) throws Exception {
        URI uri = URI.create(url);
        Connector connector = connect(uri.getHost(), uri.getPort());
        firstInitial();
        goStreamWriter.typeAuthentication(outputStream);
        if (!goStreamReader.onTypeAuthenticationResponse(inputStream))
            throw new Exception("server can't authenticate client type!");
        connectData(uri.getPath());
        listen();
        syncAllServices();
        this.notifyListener(SocketState.Connected);
        return connector;
    }

    private void notifyListener(SocketState currentState) {
        if (this.currentState == currentState)
            return;
        this.lastState = this.currentState;
        this.currentState = currentState;
        if (socketListener != null)
            this.socketListener.onSocketChange(this.lastState, currentState);
    }


    private Connector connect(String hostName, int port) throws IOException {
        this.notifyListener(SocketState.connecting);
        this.mHostName = hostName;
        this.mPort = port;
        socket = new Socket();
        socket.connect(new InetSocketAddress(mHostName, mPort), timeoutMills);
        return this;
    }

    private void connectData(String url) {
        ArrayList<String> list = new ArrayList<String>();
        list.add(url);
        try {
            byte[] b = convertorHelper.byteConvertor(list);
            byte[] len = goStreamWriter.getSize(b.length);
            byte[] result = new byte[b.length + len.length];
            System.arraycopy(len, 0, result, 0, len.length);
            System.arraycopy(b, 0, result, len.length, b.length);
            try {
                outputStream.write(result);
            } catch (IOException exception) {
                exceptionHandler(exception);
            }

            MethodCallInfo methodCallInfo = new MethodCallInfo();
            methodCallInfo.setGuid(UUID.randomUUID().toString());
            methodCallInfo.setServiceName("/CheckConnection");

        } catch (Exception ex) {
            exceptionHandler(ex);
        }

    }

    /**
     * listen to specified Socket until finish connection or get exception
     */
    private void listen() {
        listener = new Runnable() {
            public void run() {
                while (!onRecievedExeption && socket.isConnected()) {
                    try {
                        GoDataType type = goStreamReader.readType(inputStream);
                        isAlive = true;
                        if (type == GoDataType.Ping_Pong) {
                            System.out.println("signalGo   get pingpong");
                            continue;
                        }
                        GoCompressMode compressMode = goStreamReader.readCompressMode(inputStream);
                        if (type == GoDataType.CallMethod) {
                            callMethodParser(inputStream);
                        } else if (type == GoDataType.ResponseCallMethod) {
                            callMethodResponse(inputStream);
                        }
                    } catch (Exception e) {
                        exceptionHandler(e);
                        clientHelper.dispose();
                        notifyListener(SocketState.Disconnected);
                    }
                }
            }
        };
        t = new Thread(listener);
        t.start();
    }

    private void autoPingPong() {
        if (timer == null) {
            timer = new Timer();
            timerTask = getTimerTask();
            timer.schedule(timerTask, pingPongPeriod, pingPongPeriod);
        }
    }

    private TimerTask getTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                pingPong();
            }
        };
    }

    public void pingPong() {
        isAlive = false;
        if (currentState == SocketState.Connected) {
            System.out.println("signalGo   send pingpong");

            goStreamWriter.send(outputStream, new byte[]{(byte) GoDataType.Ping_Pong.getValue()});
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!isAlive) {
                        try {
                            Connector.this.close();
                        } catch (Exception e) {
                            exceptionHandler(e);
                        }
                    }
                    this.cancel();
                }
            }, pingpongTimeout, 100);
        }
    }

    private void firstInitial() throws IOException {
        isAlive = true;
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    public boolean initForCallback(Object o) {
        if (callbackHandler == null) {
            callbackHandler = new GoCallbackHandler(this);
        }
        return callbackHandler.registerCallback(o);
    }

    public boolean destroyCallBack(Object o) {
        if (callbackHandler == null) {
            return false;
        }
        return callbackHandler.removeCallback(o);
    }

    public void registerService(ClientDuplex cd) {
        try {
            Class s = cd.getClass();
            AnnotatedElement serviceClass = (AnnotatedElement) s;
            Annotation[] annotations = serviceClass.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof GoServiceName) {
                    if (mPendingServices == null) {
                        mPendingServices = new ConcurrentHashMap<String, ClientDuplex>();
                    }
                    if (socket != null && socket.isConnected()) {
                        syncService(((GoServiceName) annotation).name());
                    } else {
                        mPendingServices.put(((GoServiceName) annotation).name(), cd);
                    }
                    if (((GoServiceName) annotation).usage() != GoServiceName.GoUsageType.invoke) {
                        initForCallback(cd);
                    }
                }
            }
        } catch (Exception e) {
            exceptionHandler(e);
        }
    }

    private void syncService(String name) {
        try {
            Object o = invoke("/RegisterService", name, Object.class);
            if (mPendingServices != null && mPendingServices.containsKey(name)) {
                ((ClientDuplex) this.mPendingServices.get(name)).getConnector(this);
                mPendingServices.remove(name);
            }
        } catch (Exception ex) {
            exceptionHandler(ex);
        }
    }

    private void syncAllServices() {
        for (Map.Entry<String, ClientDuplex> entry : mPendingServices.entrySet()) {
            syncService(entry.getKey());
        }
        currentState = GoSocketListener.SocketState.Connected;
        if (socketListener != null) {
            socketListener.onSocketChange(lastState, currentState);
        }
    }

    /**
     * @param MethodName
     */
    public Object invoke(String MethodName, String ServiceName, Type responseType, Object... param) throws Exception {
        if (socket == null || !socket.isConnected()) {
            return null;
        }

        MethodCallInfo mci = new MethodCallInfo();
        mci.setGuid(UUID.randomUUID().toString());
        mci.setMethodName(MethodName);
        mci.setServiceName(ServiceName);
        mci.setParameters(clientHelper.getListOfParam(param));
        System.out.println("signalGo   invoke method " + MethodName + " " + mci.getGuid());
        Object raw = send(mci, responseType);
        if (raw == null) {
            return raw;
        }
        try {
            Object o = convertorHelper.deserialize((String) raw, responseType);
            return o;
        } catch (Exception e) {
            exceptionHandler(e);
            return raw;
        }
    }

    public Object autoInvoke(Type responseType, Object... param) throws Exception {
        //String serviceName, methodName;
        Object result = null;
        try {
            final String serviceName = GoBackStackHelper.getServiceName();
            final String methodName = GoBackStackHelper.getMethodName();
            result = invoke(methodName, serviceName, responseType, param);
        } catch (ClassNotFoundException ex) {
            exceptionHandler(ex);
        }
        return result;
    }

    public Object send(MethodCallInfo callInfo, Type responseType) throws Exception {
        if (clientHelper.isDisposed()) {
            return null;
        }
        clientHelper.addInvokedMethod(callInfo.getGuid(), new GoKeyValue(new GoAutoResetEvent(), null));
        GoKeyValue gkv = clientHelper.getGoKeyValue(callInfo.getGuid());
        goStreamWriter.send(outputStream, callInfo);
        synchronized (gkv) {
            gkv.wait(timeoutMills);
        }
        clientHelper.removeInvokedMethod(callInfo.getGuid());
        return gkv.getValue();
    }

    public void sendDeliveryNotify(MethodCallbackInfo mci) {
        try {
            goStreamWriter.sendDeliveryNotify(outputStream, mci);
        } catch (IOException e) {
            exceptionHandler(e);
        }
    }

    public void invokeAsync(final String methodName, final String serviceName, final GoResponseHandler goResponseHandler, final Object... param) {
        GoAsyncHelper.run(new Runnable() {
            public void run() {
                try {
                    Object o = invoke(methodName, serviceName, goResponseHandler.getType(), param);
                    goResponseHandler.onResponse(o);
                } catch (Exception ex) {
                    exceptionHandler(ex);
                }
            }
        });
    }

    public void autoInvokeAsync(final GoResponseHandler goResponseHandler, final Object... param) {
        try {
            final String serviceName = GoBackStackHelper.getServiceName();
            final String methodName = GoBackStackHelper.getMethodName();

            GoAsyncHelper.run(new Runnable() {
                public void run() {
                    try {
                        Object o = invoke(methodName, serviceName, goResponseHandler.getType(), param);
                        goResponseHandler.onResponse(o);
                    } catch (Exception ex) {
                        exceptionHandler(ex);
                    }
                }
            });
        } catch (Exception ex) {
            exceptionHandler(ex);
        }
    }

    public void exceptionHandler(Exception e) {
        onRecievedExeption = true;
        if (socketListener != null) {
            //socketListener.onSocketChange(lastState, currentState);
            notifyListener(currentState);
            socketListener.socketExeption(e);
        } else {
            e.printStackTrace();
        }
    }

    private void callMethodParser(InputStream inputStream) throws Exception {
        byte[] recievedData = goStreamReader.readBlockToEnd(inputStream);
        String convertedData = new String(recievedData, CHARSET);
        MethodCallInfo mci = convertorHelper.deserialize(convertedData, MethodCallInfo.class);
        System.out.println("signalGo   call from server method " + mci.getMethodName());

        if (callbackHandler != null) {
            callbackHandler.onServerCallBack(mci);
        }
    }

    private void callMethodResponse(InputStream inputStream) throws Exception {
        byte[] recievedData = goStreamReader.readBlockToEnd(inputStream);
        String convertedData = new String(recievedData, CHARSET);
        MethodCallbackInfo mci = convertorHelper.deserialize(convertedData, MethodCallbackInfo.class);
        System.out.println("signalGo   get response method " + mci.getGuid());

        clientHelper.setValue(mci.getGuid(), mci.getData());
        clientHelper.endWait(mci.getGuid());
    }

    public void onSocketChangeListener(GoSocketListener listener) {
        this.socketListener = listener;
    }

    public Connector setTimeout(int mills) {
        if (mills > 0) {
            this.timeoutMills = mills;
        }
        return this;
    }

    public Connector setPingpongTimeout(int mills) {
        if (mills > 0) {
            this.pingpongTimeout = mills;
        }
        return this;
    }

    public Connector setPingpongPeriod(int mills) {
        if (mills > 0) {
            this.pingPongPeriod = mills;
            if (timerTask != null) {
                timerTask.cancel();
                timer.cancel();
                timer.purge();
                timer = null;
                timer = new Timer();
                timerTask = getTimerTask();
                timer.schedule(timerTask, pingPongPeriod, pingPongPeriod);
            }
        }
        return this;
    }

    public void close() throws Exception {
        System.out.println("signalGo   close");

        if (socketListener != null)
            notifyListener(SocketState.Disconnected);
        isAlive = false;
        timerTask.cancel();
        timer.cancel();
        timer.purge();
        timerTask = null;
        timer = null;
        socket.close();
    }

    public void forceClose() throws Exception {
        System.out.println("signalGo   force close");

        isAlive = false;
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (socket != null) {
            socket.close();
            notifyListener(SocketState.Disconnected);
        }
    }

    public boolean socketIsConnected() {
        return socket.isConnected();
    }

}
