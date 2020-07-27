package com.dji.sdk.sample.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.sdk.sample.demo.util.FucUtil;
import com.dji.sdk.sample.demo.util.JsonParser;
import com.dji.sdk.sample.demo.util.XmlParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;

import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AlertDialog;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.Limits;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.KeyManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.ux.internal.SwitchButton;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;
import dji.ux.widget.controls.CameraControlsWidget;

/**
 * Activity that shows all the UI elements together
 */
public class CompleteWidgetActivity extends Activity {

    private static final String TAG = "CompleteWidgetActivity";

    private MapWidget mapWidget;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private RelativeLayout primaryVideoView;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;

    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;

    // 虚拟遥杆飞行部分
    private FlightController mFlightController;

    // 语音识别部分
    private SpeechRecognizer mAsr;
    private Toast mToast;
    private String mLocalGrammar = null;    // 本地语法文件
    // 本地语法构建路径
    private String grmPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/test";
    private String mResultType = "json";    // 返回结果格式，支持：xml,json
//    private  final String GRAMMAR_TYPE_ABNF = "abnf";
    private  final String GRAMMAR_TYPE_BNF = "bnf";
    private String mEngineType;
    private String mLocalLexicon = null;
    private String groupName;
    private String groupInfo;

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_widgets);

        height = DensityUtil.dip2px(this, 100);
        width = DensityUtil.dip2px(this, 150);
        margin = DensityUtil.dip2px(this, 12);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

        mapWidget = findViewById(R.id.map_widget);
        mapWidget.initAMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull DJIMap map) {
                map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(DJILatLng latLng) {
//                        onViewClick(mapWidget);
                    }
                });
            }
        });
        mapWidget.onCreate(savedInstanceState);

        parentView = (ViewGroup) findViewById(R.id.root_view);

//        fpvWidget = findViewById(R.id.fpv_widget);
//        fpvWidget.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onViewClick(fpvWidget);
//            }
//        });
//        primaryVideoView = (RelativeLayout) findViewById(R.id.fpv_container);
//        secondaryVideoView = (FrameLayout) findViewById(R.id.secondary_video_view);
//        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
//        secondaryFPVWidget.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                swapVideoSource();
//            }
//        });
//        updateSecondaryVideoVisibility();

        // 语音控制部分的初始化
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        initSpeechEngine();
        switchFlyMethod = (Switch)findViewById(R.id.fly_method);
        switchFlyMethod.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    enableVirtualStickFlying();
                    listenSpeechCommand();
                }
                else
                    disableVirtualStickFlying();
            }
        });
    }

//    private void onViewClick(View view) {
//        if (view == fpvWidget && !isMapMini) {
//            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
//            reorderCameraCapturePanel();
//            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
//            mapWidget.startAnimation(mapViewAnimation);
//            isMapMini = true;
//        } else if (view == mapWidget && isMapMini) {
//            hidePanels();
//            resizeFPVWidget(width, height, margin, 12);
//            reorderCameraCapturePanel();
//            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
//            mapWidget.startAnimation(mapViewAnimation);
//            isMapMini = false;
//        }
//    }

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) primaryVideoView.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        primaryVideoView.setLayoutParams(fpvParams);

        parentView.removeView(primaryVideoView);
        parentView.addView(primaryVideoView, fpvInsertPosition);
    }

    private void reorderCameraCapturePanel() {
        View cameraCapturePanel = findViewById(R.id.CameraCapturePanel);
        parentView.removeView(cameraCapturePanel);
        parentView.addView(cameraCapturePanel, isMapMini ? 9 : 13);
    }

    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
        }
    }

    private void updateSecondaryVideoVisibility() {
        if (secondaryFPVWidget.getVideoSource() == null) {
            secondaryVideoView.setVisibility(View.GONE);
        } else {
            secondaryVideoView.setVisibility(View.VISIBLE);
        }
    }

    private void hidePanels() {
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.HISTOGRAM_ENABLED), false, null);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED), false, null);
        }

        //These panels have buttons that toggle them, so call the methods to make sure the button state is correct.
        CameraControlsWidget controlsWidget = findViewById(R.id.CameraCapturePanel);
        controlsWidget.setAdvancedPanelVisibility(false);
        controlsWidget.setExposurePanelVisibility(false);

        //These panels don't have a button state, so we can just hide them.
        findViewById(R.id.pre_flight_check_list).setVisibility(View.GONE);
        findViewById(R.id.rtk_panel).setVisibility(View.GONE);
        findViewById(R.id.spotlight_panel).setVisibility(View.GONE);
        findViewById(R.id.speaker_panel).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        mapWidget.onResume();
        initFlightController();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // 释放地图
        mapWidget.onDestroy();
        // 释放语音引擎
        if( null != mAsr ){
            mAsr.cancel();
            mAsr.destroy();
        }
        // 释放飞行控制权
        disableVirtualStickFlying();
        if (mSendVirtualStickDataTask != null) {
            mSendVirtualStickDataTask.cancel();
        }
        mSendVirtualStickDataTimer.cancel();
        mSendVirtualStickDataTimer.purge();
        mSendVirtualStickDataTimer = null;
        mSendVirtualStickDataTask = null;

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    private class ResizeAnimation extends Animation {

        private View mView;
        private int mToHeight;
        private int mFromHeight;

        private int mToWidth;
        private int mFromWidth;
        private int mMargin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }

// Virtual stick 部分
    private boolean isEnableStick = false;
    private FlightControlData mSendData;
    //  Virtual Stick 部分
    private Switch switchFlyMethod;
    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    private void initFlightController() {

        Aircraft aircraft = MApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            mFlightController = null;
            return;
        } else {
            mFlightController = aircraft.getFlightController();
        }
    }

    private void enableVirtualStickFlying(){
        if(mFlightController == null){
            Toast.makeText(getApplicationContext(), "飞机未连接", Toast.LENGTH_LONG).show();
            return;
        }
        mFlightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });

        mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (null == djiError) {
//                    isEnableStick = true;
                    Log.d("debug", "setVirtualStickModeEnabled success，准备接受Virtual Stick控制");
                    mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                    mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                    mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                    mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                    isEnableStick = true;
                } else {
                    Log.d("debug",  "error = " + djiError.getDescription());
                }
//————————————————
//                版权声明：本文为CSDN博主「风之子磊505057618」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
//                原文链接：https://blog.csdn.net/qq_26923265/article/details/82743941
            }
        });
    }

    private void disableVirtualStickFlying(){
        if(mFlightController == null){
            showTip("飞机未连接");
            return;
        }
        mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.d("debug", "setVirtualStickModeDisabled success， 回到遥控器控制");
                isEnableStick = false;
            }
        });
    }

    private class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {

            if (mFlightController != null) {
                mFlightController.sendVirtualStickFlightControlData(mSendData,
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
//                                Log.d("debug", "sendVirtualStickFlightControlData success， 指令发送正确");
                            }
                        }
                );
            }
        }
    }

    /**
     *  rockerX 杆量比[-1, 1], 前/后(+/-)
     *  rockerY 杆量比[-1, 1], 左/右(-/+)
     *  rockerZ 杆量比[-1, 1], 上/下(-/+)
     *  rockerRotate 杆量比[-1, 1], 顺时针/逆时针(+/-)旋转
     */
    private class VirtualStickPB{
        float rockerX;
        float rockerY;
        float rockerZ;
        float rockerRotation;
        public VirtualStickPB(float rockerX, float rockerY, float rockerZ, float rockerRotation){
            this.rockerX = rockerX;
            this.rockerY = rockerY;
            this.rockerZ = rockerZ;
            this.rockerRotation = rockerRotation;
        }
    }

    private void executeFlying(VirtualStickPB bean) {
        if(mFlightController == null){
            Toast.makeText(getApplicationContext(), "飞机未连接", Toast.LENGTH_LONG).show();
            return;
        }
        if (isEnableStick) {
            float rockX =  bean.rockerX;
            float rockY =  bean.rockerY;
            float rockZ =  bean.rockerZ;
            float rockerRotation =  bean.rockerRotation;
            //Log.e("dispatch", "x = " + rockX + "   y = " + rockY + "  z = " + rockZ+"  rockerRotation = "+rockerRotation);

            RollPitchControlMode rollPitchControlMode = mFlightController.getRollPitchControlMode();
            VerticalControlMode verticalControlMode = mFlightController.getVerticalControlMode();
            YawControlMode yawControlMode = mFlightController.getYawControlMode();

            if (rollPitchControlMode == RollPitchControlMode.VELOCITY) {
                // 前/后
                mRoll = rockX * Limits.ROLL_PITCH_CONTROL_MAX_VELOCITY;
                // 左/右
                mPitch = rockY * Limits.ROLL_PITCH_CONTROL_MAX_VELOCITY;
            } else if (rollPitchControlMode == RollPitchControlMode.ANGLE) {
                // 前/后
                mPitch = -rockX * Limits.ROLL_PITCH_CONTROL_MAX_ANGLE;
                // 左/右
                mRoll = rockY * Limits.ROLL_PITCH_CONTROL_MAX_ANGLE;
            } else {
                mPitch = 0;
                mRoll = 0;
            }

            if (verticalControlMode == VerticalControlMode.VELOCITY) {
                // 上/下
                mThrottle = -rockZ * Limits.VERTICAL_CONTROL_MAX_VELOCITY;
            } else if (verticalControlMode == VerticalControlMode.POSITION) {
                // 上/下
                if (rockZ >= 0) {
                    mThrottle = rockZ * Limits.VERTICAL_CONTROL_MAX_HEIGHT;
                } else {
                    mThrottle = 0;
                    //mThrottle = controller.getCurrentState().getAircraftLocation().getAltitude();
                }
            } else {
                mThrottle = 0;
            }

            if (yawControlMode == YawControlMode.ANGULAR_VELOCITY) {
                // 旋转
                mYaw = rockerRotation * Limits.YAW_CONTROL_MAX_ANGULAR_VELOCITY;
            } else if (yawControlMode == YawControlMode.ANGLE) {
                // 旋转
                mYaw = rockerRotation * Limits.YAW_CONTROL_MAX_ANGLE;
            } else {
                mYaw = 0;
            }

            mSendData = new FlightControlData(mPitch, mRoll, mYaw, mThrottle);

            if (null == mSendVirtualStickDataTimer) {
                mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                mSendVirtualStickDataTimer = new Timer();
                mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 100, 200);
            }
        } else {
            Log.d("debug", "isenablestick = false");
        }
    }

    // 语音识别部分
    private void initSpeechEngine(){
        mEngineType =  SpeechConstant.TYPE_LOCAL;
        mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);
//        mLocalLexicon = "张海羊\n刘婧\n王锋\n";        // 初始化语法、命令词
        mLocalGrammar = FucUtil.readFile(this,"command.bnf", "utf-8");
        buildGrammer();
    }

    private void buildGrammer(){
        mAsr.setParameter(SpeechConstant.PARAMS, null);
        // 设置文本编码格式
        mAsr.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
        // 设置引擎类型
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置语法构建路径
        mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        //使用8k音频的时候请解开注释
//             mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
        // 设置资源路径
        mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        int ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mLocalGrammar, grammarListener);
        if(ret != ErrorCode.SUCCESS){
            showTip("语法构建失败,错误码：" + ret);
        }
    }

    private void listenSpeechCommand(){
        if (!setParam()) {
            showTip("请先构建语法。");
            return;
        };
        int ret = mAsr.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            showTip("识别失败,错误码: " + ret);
        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码："+code);
            }
        }
    };

    /**
     * 更新词典监听器。
     */
    private LexiconListener lexiconListener = new LexiconListener() {
        @Override
        public void onLexiconUpdated(String s, SpeechError error) {
            if(error == null){
                showTip("词典更新成功");
            }else{
                showTip("词典更新失败,错误码："+error.getErrorCode());
            }
        }
    };

    /**
     * 构建语法监听器。
     */
    private GrammarListener grammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String s, SpeechError error) {
            if (error == null) {
                showTip("语法构建成功：" + s);
            } else {
                showTip("语法构建失败,错误码：" + error.getErrorCode());
            }
        }
     };

    /**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.d(TAG, "recognizer result：" + result.getResultString());
                String text = "";
                if (mResultType.equals("json")) {
                    text = JsonParser.parseGrammarResult(result.getResultString(), SpeechConstant.TYPE_LOCAL);
                } else if (mResultType.equals("xml")) {
                    text = XmlParser.parseNluResult(result.getResultString());
                }
                // 显示
                showTip(text);
            } else {
                Log.d(TAG, "recognizer result : null");
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error)  {
            if (error == null) {
                showTip("语法识别引擎成功");
            } else {
                showTip("语法识别引擎成功,错误码：" + error.getErrorCode());
            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    /**
     * 参数设置
     * @param
     * @return
     */
    public boolean setParam(){
        boolean result = false;
        // 清空参数
        mAsr.setParameter(SpeechConstant.PARAMS, null);
        // 设置识别引擎
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);

        // 设置本地识别资源
        mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        // 设置语法构建路径
        mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        // 设置返回结果格式
        mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
        // 设置本地识别使用语法id
        mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "command");
        // 设置识别的门限值
        mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
        // 使用8k音频的时候请解开注释
//       mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
        result = true;


        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mAsr.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mAsr.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/asr.wav");
        return result;
    }

    //获取识别资源路径
    private String getResourcePath(){
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet"));
        //识别8k资源-使用8k的时候请解开注释
//    tempBuffer.append(";");
//    tempBuffer.append(ResourceUtil.generateResourcePath(this, RESOURCE_TYPE.assets, "asr/common_8k.jet"));
        return tempBuffer.toString();
    }

    private void updateLexicon(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View v = inflater.inflate(R.layout.word_info_editor, null);
        final EditText wordGroupName = v.findViewById(R.id.enter_word_group_name);
        final EditText wordGroupInfo = v.findViewById(R.id.enter_word_group_info);
        Button cancleButton = v.findViewById(R.id.register_cancle);
        Button confirmButton = v.findViewById(R.id.register_confirm);
        final Dialog dialog = builder.create();
        //点击EditText弹出软键盘
        cancleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTip( "取消");
                dialog.cancel();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!wordGroupName.getText().toString().equals("")) {
                    groupName= wordGroupName.getText().toString();
                }
                if (!wordGroupInfo.getText().toString().equals("")) {
                    groupInfo = wordGroupInfo.getText().toString();
                }
                mLocalLexicon = getUpdateInfo(groupInfo);
//                ((EditText) findViewById(R.id.isr_text)).setText(mLocalLexicon);
                mAsr.setParameter(SpeechConstant.PARAMS, null);
                // 设置引擎类型
                mAsr.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
                // 设置资源路径
                mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
                // 设置语法构建路径
                mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
                // 设置语法名称
                mAsr.setParameter(SpeechConstant.GRAMMAR_LIST, "call");
                // 设置文本编码格式
                mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
                //执行更新操作
                int ret = mAsr.updateLexicon(groupName, mLocalLexicon, lexiconListener);
                if (ret != ErrorCode.SUCCESS) {
                    showTip("更新词典失败,错误码：" + ret);
                }
                else{
                    showTip("更新词典成功" );
                }
                dialog.cancel();
            }
        });

        dialog.show();
        dialog.getWindow().setContentView(v);//自定义布局应该在这里添加，要在dialog.show()的后面
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private String getUpdateInfo(String groupInfo) {
        String[] wordList=groupInfo.split("，");
        StringBuilder builder=new StringBuilder();
        for(int i=0;i<wordList.length;i++){
            if(i==wordList.length-1) {
                builder.append(wordList[i] );
                Log.d(TAG, "getUpdateInfo: "+wordList[i]);
            }else{
                builder.append(wordList[i] + "\n");
            }
        }
        return builder.toString();
    }
}
