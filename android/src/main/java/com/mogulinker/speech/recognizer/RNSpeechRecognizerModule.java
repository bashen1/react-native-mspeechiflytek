
package com.mogulinker.speech.recognizer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;

import org.json.JSONException;
import org.json.JSONObject;

public class RNSpeechRecognizerModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private SpeechRecognizer mIat;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private boolean mTranslateEnable = false;
    private String resultType = "json";
    private Toast mToast;

    private boolean cyclic = false;// 音频流识别是否循环调用
    private String result = "";
    private String AppId = "";

    public RNSpeechRecognizerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    public void init_AppId(String AppId) {
        if (mIat != null) {
            return;
        }
        SpeechUtility.createUtility(getCurrentActivity(), SpeechConstant.APPID + "=" + AppId);
        mIat = SpeechRecognizer.createRecognizer(getCurrentActivity(), mInitListener);
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("aaa", "SpeechRecognizer init() code = " + code);
            // if (code != ErrorCode.SUCCESS) {
            // showTip("初始化失败，错误码：" + code +
            // ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            // }
        }
    };

    /**
     * 参数设置
     *
     * @return
     */
    public void setParam(final ReadableMap param) {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);

        //
        // 此处用于设置dialog中不显示错误码信息
        // mIat.setParameter("view_tips_plain","false");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        String VADBOS = "5000";
        if (param.hasKey("vadbos")) {
            if (param.getString("vadbos") != "") {
                VADBOS = param.getString("vadbos");
            }
        }
        mIat.setParameter(SpeechConstant.VAD_BOS, VADBOS);

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        String VADEOS = "5000";
        if (param.hasKey("vadeos")) {
            if (param.getString("vadeos") != "") {
                VADEOS = param.getString("vadeos");
            }
        }
        mIat.setParameter(SpeechConstant.VAD_EOS, VADEOS);

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        String ASRPTT = "1";
        if (param.hasKey("asrptt")) {
            if (param.getString("asrptt") != "") {
                ASRPTT = param.getString("asrptt");
            }
        }
        mIat.setParameter(SpeechConstant.ASR_PTT, ASRPTT);

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    @Override
    public String getName() {
        return "RNSpeechRecognizer";
    }

    @ReactMethod
    public void stop() {
        if (mIat != null) {
            mIat.stopListening();
        }
    }

    @ReactMethod
    public void init(String AppId) {
        this.AppId = AppId;
    }

    @ReactMethod
    public void start(final ReadableMap param) {
        try {
            /** add by david 2019-6-13 start  */
            //添加权限判断
            String[] perms = {Manifest.permission.RECORD_AUDIO};
            int permission_result = PermissionChecker.checkPermission(getCurrentActivity(), perms[0], android.os.Process.myPid(), android.os.Process.myUid(), getCurrentActivity().getPackageName());
            boolean isTip = ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(), perms[0]);
            /** add by david 不再提醒后处理  start */
            SharedPreferences sharedPreferences = getCurrentActivity().getSharedPreferences("PermissionsResult", Context.MODE_PRIVATE);
            int mPermissionsResult = sharedPreferences.getInt(Manifest.permission.RECORD_AUDIO, -2);

            if (isTip == false && permission_result != PermissionChecker.PERMISSION_GRANTED && mPermissionsResult == -1) {
                WritableMap params = Arguments.createMap();
                params.putString("text", "");
                params.putBoolean("isLast", true);
                params.putString("errorMsg", "permission_error");
                params.putString("result", "");
                this.onJSEvent(getReactApplicationContext(), "onRecognizerResult", params);
                return;
            }
            /** add by david 不再提醒后处理  end */

            if (permission_result != PermissionChecker.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) getCurrentActivity(), perms, 100);
                return;
            }
            /** add by david 2019-6-13 end  */

            init_AppId(AppId);
            // 设置参数
            setParam(param);
            mIat.startListening(mRecognizerListener);
        } catch (Exception e) {
            Log.d("aaa", e.toString());
        }
    }

    private void showTip(final String str) {
        if (mToast == null) {
            mToast = Toast.makeText(getCurrentActivity(), "", Toast.LENGTH_SHORT);
        }
        try {
            mToast.setText(str);
            mToast.show();
        } catch (Exception e) {
        }

    }

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        boolean isLast = false;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
            isLast = resultJson.optBoolean("ls");
            result = result + text;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("aaa", sn);
        Log.d("aaa", text);

        WritableMap params = Arguments.createMap();
        params.putString("text", text);
        params.putBoolean("isLast", isLast);
        params.putString("result", result);

        this.onJSEvent(getReactApplicationContext(), "onRecognizerResult", params);

        if (isLast) {
            result = "";
        }

        // mIatResults.put(sn, text);
        //
        // StringBuffer resultBuffer = new StringBuffer();
        // for (String key : mIatResults.keySet()) {
        // resultBuffer.append(mIatResults.get(key));
        // }
        //
        // mResultText.setText(resultBuffer.toString());
        // mResultText.setSelection(mResultText.length());
    }

    private void onJSEvent(ReactContext reactContext, String eventName, WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            // showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
//            showTip(error.getPlainDescription(false));
            /** add by david  at 2019-8-28 start */
            // 结束说话
            showTip(error.getPlainDescription(false));
            if (error.getErrorCode() == 10118) {
                WritableMap params = Arguments.createMap();
                params.putString("text", "");
                params.putBoolean("isLast", true);
                params.putString("result", "");
                onJSEvent(getReactApplicationContext(), "onRecognizerResult", params);
            }
            /** add by david  at 2019-8-28 end */

        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//            showTip("结束说话");
            /** add by david  at 2019-8-28 start */
            // 结束说话
            WritableMap params = Arguments.createMap();
            params.putString("text", "");
            params.putBoolean("isLast", true);
            params.putString("result", "");
            onJSEvent(getReactApplicationContext(), "onRecognizerResult", params);
            /** add by david  at 2019-8-28 end */

        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d("aaa", results.getResultString());
            if (resultType.equals("json")) {
                // if( mTranslateEnable ){
                // printTransResult( results );
                // }else{
                printResult(results);

            }

        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            // showTip("当前正在说话，音量大小：" + volume);
            // Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            // if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            // String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            // Log.d(TAG, "session id =" + sid);
            // }
        }
    };
}