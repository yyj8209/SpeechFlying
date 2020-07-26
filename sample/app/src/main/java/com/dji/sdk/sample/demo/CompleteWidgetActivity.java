package com.dji.sdk.sample.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJILatLng;

import java.util.Timer;
import java.util.TimerTask;

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

    private FlightController mFlightController;

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
                        onViewClick(mapWidget);
                    }
                });
            }
        });
        mapWidget.onCreate(savedInstanceState);

        parentView = (ViewGroup) findViewById(R.id.root_view);

        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewClick(fpvWidget);
            }
        });
        primaryVideoView = (RelativeLayout) findViewById(R.id.fpv_container);
        secondaryVideoView = (FrameLayout) findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
        secondaryFPVWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapVideoSource();
            }
        });
        updateSecondaryVideoVisibility();

        // 语音控制部分的初始化
        switchFlyMethod = (Switch)findViewById(R.id.fly_method);
        switchFlyMethod.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    enableSpeechFlying();
                else
                    disableSpeechFlying();
            }
        });
    }

    private void onViewClick(View view) {
        if (view == fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;
        } else if (view == mapWidget && isMapMini) {
            hidePanels();
            resizeFPVWidget(width, height, margin, 12);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
        }
    }

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
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

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
        mapWidget.onDestroy();

        disableSpeechFlying();
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

    private void enableSpeechFlying(){
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

    private void disableSpeechFlying(){
        if(mFlightController == null){
            Toast.makeText(getApplicationContext(), "飞机未连接", Toast.LENGTH_LONG).show();
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

}
