package com.iceddev.face;

import android.util.Log;

import com.iceddev.eyesocket.Server;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by phated on 8/6/13.
 */
public class Data {

  private Server server;

  private JSONObject buttons;

  private JSONObject gestures;

  private JSONObject orientation;

  public Data(Server server){
    this.server = server;

    buttons = new JSONObject();
    gestures = new JSONObject();
    try {
      buttons.put("A", false);
      buttons.put("B", false);

      gestures.put("tap", false);
      gestures.put("scrollLeft", false);
      gestures.put("scrollRight", false);
      gestures.put("down", false);
      gestures.put("longPress", false);
      gestures.put("doubleTap", false);
    } catch (JSONException e) {
      Log.e("JSON ERROR", "in constructor: " + e.toString());
    }
  }


  public Server getServer() {
    return server;
  }

  public void setServer(Server server) {
    this.server = server;
  }

  public JSONObject getButtons() {
    return buttons;
  }

  public void setButton(String button, boolean pressed) {
    try {
      this.buttons.put(button, pressed);
      this.server.sendData(this);
      this.buttons.put(button, !pressed);
    } catch (JSONException e) {
      Log.e("JSON ERROR", "in setButton: " + e.toString());
    }
  }

  @Override
  public String toString() {
    JSONObject data = new JSONObject();
    try {
      data.put("buttons", this.buttons);
      data.put("gestures", this.gestures);
      data.put("orientation", this.orientation);
    } catch (JSONException e) {
      Log.e("JSON ERROR", "in toString: " + e.toString());
    }
    return data.toString();
  };

  public JSONObject getGestures() {
    return gestures;
  }

  public void setGesture(String gesture, boolean made) {
    try {
      this.gestures.put(gesture, made);
      this.server.sendData(this);
      this.gestures.put(gesture, !made);
    } catch (JSONException e) {
      Log.e("JSON ERROR", "in setGesture: " + e.toString());
    }
  }

  public JSONObject getOrientation() {
    return orientation;
  }

  public void setOrientation(JSONObject orientation) {
    this.orientation = orientation;
    this.server.sendData(this);
  }
}
