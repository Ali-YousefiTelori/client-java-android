package ir.atitec.signalgoApp;


import com.google.common.reflect.TypeToken;


import java.io.File;
import java.util.Date;
import java.util.List;

import ir.atitec.signalgo.HttpCore;
import ir.atitec.signalgo.annotations.GoMethodName;
import ir.atitec.signalgo.util.GoResponseHandler;

public class ServerMethods {
    @GoMethodName(name = "/Authentication/TestUtf8", type = GoMethodName.MethodType.httpPost_formData, multipartKeys = {"userInfo"})
    public static void TestUtf8(UserInfo userInfo, GoResponseHandler<MessageContract2<UserInfo>> goResponseHandler) {
        goResponseHandler.setTypeToken(new TypeToken<MessageContract2<UserInfo>>() {
        });
        // call method -> pass params to it
        HttpCore.instance().callMethod(goResponseHandler, userInfo);
    }



}