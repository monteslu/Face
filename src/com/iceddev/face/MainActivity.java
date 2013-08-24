package com.iceddev.face;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.iceddev.eyesocket.Server;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.text.format.Formatter.*;
import static android.view.GestureDetector.OnGestureListener;
import static android.view.GestureDetector.SimpleOnGestureListener;

public class MainActivity extends Activity {

  private GestureDetector gestureDetector;

  private static final int PORT = 1338;
  private Server server;

  //allow remote taking of pictures from a broadcast?
  private static final boolean ALLOW_REMOTE_CAMERA_SHOT =  true;

  private static final String LOG_TAG = "SensorTest";

  private TextView text;

  private TextView chanText;

  private SensorManager mSensorManager;

  private Sensor mOrientation;

  private LocationManager mLocationManager;

  float azimuth_angle;
  float pitch_angle;
  float roll_angle;
  String lastMessage = "";
  Object lastError = null;

  private Data sensorData;

  private Map<String, Object> data;

  public void setButtons(String keyPressed){
    JSONObject buttons = (JSONObject) data.get("buttons");
    Iterator keys = buttons.keys();
    while(keys.hasNext()){
      String key = (String) keys.next();
      try {
        buttons.put(key, key.equals(keyPressed));
      } catch (JSONException jse){
        Log.w(LOG_TAG, "error serializing json" + jse.toString());
      }
    }
  }

  public void onClick_A(View view) {
    sensorData.setButton("A", true);
  }

  public void onClick_B(View view) {
    sensorData.setButton("B", true);
  }


  public void broadcastWebsocket(Map<String, Object> map){
    try{
      JSONObject jsObj = new JSONObject();
      for (String key : map.keySet()) {
        jsObj.put(key, map.get(key));
      }
      JSONArray arguments = new JSONArray();
      arguments.put(jsObj);
      server.sendData(arguments);
    }catch(Exception jse){
      server.sendData(jse);
      Log.w(LOG_TAG, "error broadcasting" + jse.toString());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(R.layout.activity_main);

    text = (TextView) findViewById(R.id.text);
    chanText = (TextView) findViewById(R.id.chan);
    WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
    WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
    int ip = wifiInfo.getIpAddress();
    final String ipAddress = formatIpAddress(ip);
    chanText.setText("IP: " + ipAddress);

    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
    for(Sensor sensor : sensorList) {
      Log.i(LOG_TAG, "Found sensor: " + sensor.getName());
    }

    gestureDetector = new GestureDetector(this, gestureListener);

    mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

    data = new HashMap<String, Object>();

    JSONObject gestures = new JSONObject();
    data.put("gestures", gestures);
    JSONObject sensors = new JSONObject();
    data.put("sensors", sensors);
    JSONObject buttons = new JSONObject();
    try{
      buttons.put("A", false);
      buttons.put("B", false);
    } catch (JSONException jse){
      Log.w(LOG_TAG, "error serializing json" + jse.toString());
    }

    data.put("buttons", buttons);
  }

  @Override
  public void onStart(){
    super.onStart();
    Log.v(LOG_TAG, "started");
  }

  @Override
  protected void onResume() {
    super.onResume();
    server = new Server(PORT);
    sensorData = new Data(server);
    mSensorManager.registerListener(orientationSensor, mOrientation, SensorManager.SENSOR_DELAY_UI);
  }

  @Override
  protected void onPause() {
    super.onPause();
    server.close();
    mSensorManager.unregisterListener(orientationSensor);
  }

  @Override
  public boolean onGenericMotionEvent(MotionEvent event) {
    gestureDetector.onTouchEvent(event);
    return false;
  }

  protected String displayString(Object obj){
    if(obj == null){
      return "";
    }else{
      String longStr = obj.toString();
      if(longStr.length() > 15){
        return longStr.substring(0,14);
      }else{
        return longStr;
      }
    }
  }

  private SensorEventListener orientationSensor = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
      azimuth_angle = sensorEvent.values[0];
      pitch_angle = sensorEvent.values[1];
      roll_angle = sensorEvent.values[2];

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          text.setText("msg:" + displayString(lastMessage) +
                  "\n" + "a,p,r:" + new Float(azimuth_angle).intValue() + "," +  new Float(pitch_angle).intValue() + "," + new Float(roll_angle).intValue() +
                  "\n" + "err:" + displayString(lastError));
        }
      });

      JSONObject orientation = new JSONObject();

      try {
        orientation.put("pitch", pitch_angle);
        orientation.put("azimuth", azimuth_angle);
        orientation.put("roll", roll_angle);
        sensorData.setOrientation(orientation);
      } catch (JSONException e) {
        Log.e("JSON ERROR", e.toString());
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
  };

  private OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

    @Override
    public boolean onDown(MotionEvent motionEvent) {
      sensorData.setGesture("down", true);
      return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
      sensorData.setGesture("doubleTap", true);
      return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
//       sensorData.setGesture("longPress", true);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
      sensorData.setGesture("tap", true);
      return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float distanceX, float distanceY) {
      Log.d("Gesture Example", "onScroll: distanceX:" + distanceX + " distanceY:" + distanceY);
      if (distanceX < -1) {
        sensorData.setGesture("scrollRight", true);
      } else if (distanceX > 1) {
        sensorData.setGesture("scrollLeft", true);
      }
      return false;
    }

    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
      return false;
    }
  };

}
