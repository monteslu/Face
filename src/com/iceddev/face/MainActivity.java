package com.iceddev.face;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codebutler.android_websockets.SocketIOClient;

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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import org.json.*;

public class MainActivity extends Activity implements SensorEventListener {
	
	//which reflector socket server to post to? 
    private static final String SOCKET_SERVER = "http://10.0.2.2:8080";
	//private static final String SOCKET_SERVER = "http://192.168.0.101:8080";
    
    //allow remote taking of pictures from a broadcast?
    private static final boolean ALLOW_REMOTE_CAMERA_SHOT =  true;
    
    //min time between each broadcast of sensor data
    private static final long MIN_TIME_BETWEEN_SENSOR_BROADCAST = 100;

    
    private static final String LOG_TAG = "SensorTest";
    
	private TextView text;

	private TextView chanText;

	private SensorManager mSensorManager;

	private Sensor mOrientation;

	private LocationManager mLocationManager;
	protected long lastSensorSend = System.currentTimeMillis();
	
	String chanId = null;
	SocketIOClient client = null;

	
	float azimuth_angle;
	float pitch_angle;
	float roll_angle;
    String lastMessage = "";
    Object lastError = null;
	

	IntentFilter winkIntentFilter;
	
	protected BroadcastReceiver winkReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("event", "wink");	
	    	broadcastWebsocket(map);
		
		}
	};
	
    public void onClick_send(String btn) {
    	Map<String, Object> map = new HashMap<String, Object>();
		map.put("event", btn);	
    	broadcastWebsocket(map);

    }
    
    public void onClick_A(View view) {
    	onClick_send("A");

    }
    
    public void onClick_B(View view) {
    	onClick_send("B");

    }
   
    
    public void broadcastWebsocket(Map<String, Object> map){
    	try{
    		JSONObject jsObj = new JSONObject();
    		for (String key : map.keySet()) {
    		    jsObj.put(key, map.get(key));
    		}
    		jsObj.put("id", chanId);				
			JSONArray arguments = new JSONArray();
			arguments.put(jsObj);
			client.emit("broadcast", arguments); //Event
		}catch(Exception jse){
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

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		for(Sensor sensor : sensors) {
			Log.i(LOG_TAG, "Found sensor: " + sensor.getName());
		}
		
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		
		winkIntentFilter = new IntentFilter();
	    winkIntentFilter.addAction("com.google.glass.action.EYE_GESTURE");
		
		
	}
	
    @Override
    public void onStart(){
    	super.onStart();
    	Log.v(LOG_TAG, "started");
    	initSocket();
    }
    
    public void initSocket(){

        URI uri = URI.create(SOCKET_SERVER);
		
		client = new SocketIOClient(uri, new SocketIOClient.Handler() {
		    @Override
		    public void onConnect() {
		        Log.d(LOG_TAG, "Connected! to socket");
				try{
					Log.d(LOG_TAG, "getting chan id");
					JSONArray arguments = new JSONArray();
					client.emit("getChan", arguments); //Message
				
				}catch(Exception jsonE){
					Log.w(LOG_TAG, "error emiting first messags: "+jsonE.toString());
				}
		    }

		    @Override
		    public void on(String event, JSONArray arguments) {
		    	
		        Log.d(LOG_TAG, String.format("Got event %s: %s", event, arguments.toString() ));
		        if(event.equals("chanId")){
		        	try{
                        chanId = arguments.getString(0);
		        	}catch(JSONException jse){
		        		Log.w(LOG_TAG, "error getting chanId: " + jse.toString());
		        	}
		        	

		        }else if(event.equals("broadcast")){
                    try{
                        Log.w(LOG_TAG, "broadcast: " + arguments.get(0).toString());
                        JSONObject payload = arguments.getJSONObject(0);
                        
                        if(payload.has("msg")){
                        	lastMessage = payload.get("msg").toString();
                        }
                        
                        if(payload.has("action")){
                        	String action = payload.get("action").toString();
                        	if("camera".equals(action) && ALLOW_REMOTE_CAMERA_SHOT){
	                    		//The sloppy way of launching the default camera taking app
	                            Intent i = new Intent("android.intent.action.CAMERA_BUTTON");
	                            sendBroadcast(i);
	                            
                        	}
                        }
                    }catch(JSONException jse){
                        Log.w(LOG_TAG, "error getting broadcast: " + jse.toString());
                    }


                }
		    }

		    @Override
		    public void onJSON(JSONObject json) {
		        //try {
		            Log.d(LOG_TAG, String.format("Got JSON Object: %s", json.toString()));
		        //} catch(JSONException e) {
		       // }
		    }

		    @Override
		    public void onMessage(String message) {
		        Log.d(LOG_TAG, String.format("Got message: %s", message));
		    }

		    @Override
		    public void onDisconnect(int code, String reason) {
		        Log.d(LOG_TAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
		    }

		    @Override
		    public void onError(Exception error) {
		        Log.e(LOG_TAG, "Error!", error);
                lastError = error;
		    }

		    @Override
		    public void onConnectToEndpoint(String endpoint) {
		        Log.d(LOG_TAG, "Connected to endpoint");
		    }
		});

		client.connect();


    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mOrientation,
				SensorManager.SENSOR_DELAY_NORMAL);
		registerReceiver(winkReceiver, winkIntentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		unregisterReceiver(winkReceiver);
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

	@Override
	public void onSensorChanged(SensorEvent event) {
		azimuth_angle = event.values[0];
		pitch_angle = event.values[1];
		roll_angle = event.values[2];
		
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				text.setText("msg:" + displayString(lastMessage) +
                        "\n" + "a,p,r:" + new Float(azimuth_angle).intValue() + "," +  new Float(pitch_angle).intValue() + "," + new Float(roll_angle).intValue() +
                        "\n" + "err:" + displayString(lastError));
				chanText.setText("chanId: " + chanId);
				
			}
		});

	    
	
		
		//dumb throttling
		if( (System.currentTimeMillis() - lastSensorSend) > MIN_TIME_BETWEEN_SENSOR_BROADCAST){
			
			
			if(client != null && chanId != null){

				Map<String, Object> map = new HashMap<String, Object>();
				JSONObject sensors = new JSONObject();	
				
				
				try {
					sensors.put("pitch", pitch_angle);
					sensors.put("azimuth", azimuth_angle);
					sensors.put("roll", roll_angle);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				map.put("sensors", sensors);
				
		    	broadcastWebsocket(map);
		    				
				lastSensorSend = System.currentTimeMillis();
			}
		}
	}




}

