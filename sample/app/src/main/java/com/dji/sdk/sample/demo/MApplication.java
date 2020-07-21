package com.dji.sdk.sample.demo;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.secneo.sdk.Helper;

import static com.dji.sdk.sample.demo.DJIConnectionControlActivity.ACCESSORY_ATTACHED;

public class MApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 实时监测飞行器的连接状态。
        BroadcastReceiver br = new OnDJIUSBAttachedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACCESSORY_ATTACHED);
        registerReceiver(br, filter);

        // 讯飞语音的命令词识别，在这里初始化
        // 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true"
        // 参数间使用“,”分隔。
        // 设置你申请的应用appid
        // 注意： appid 必须和下载的SDK保持一致，否则会出现10407错误
        StringBuffer param = new StringBuffer();
        param.append("appid="+getString(R.string.app_id));
        param.append(",");
        // 设置使用v5+
        param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(MApplication.this, param.toString());
    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
    }
}
