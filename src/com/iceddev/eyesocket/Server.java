package com.iceddev.eyesocket;

import android.util.Log;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.stream.ByteBufferListInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.koushikdutta.async.http.server.AsyncHttpServer.*;

/**
 * Created by phated on 8/6/13.
 */
public class Server {

  List<WebSocket> sockets;

  AsyncHttpServer server;

  public Server(int port){
    sockets = new ArrayList<WebSocket>();

    server = new AsyncHttpServer();
    server.websocket("/", new WebSocketRequestCallback() {
      @Override
      public void onConnected(final WebSocket webSocket, RequestHeaders requestHeaders) {
        Log.e("SERVER", "Connected " + webSocket.toString());
        webSocket.send("Connected");

        webSocket.setStringCallback(new WebSocket.StringCallback() {
          @Override
          public void onStringAvailable(String s) {
            Log.e("SERVER", "StringAvailable " + webSocket.toString());
            webSocket.send("echo: " + s);
          }
        });

//        webSocket.setDataCallback(new DataCallback() {
//          @Override
//          public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
//            ByteBufferListInputStream stream = new ByteBufferListInputStream(byteBufferList);
//            try {
//              Log.e("DATA", );
//            } catch (IOException e) {
//              e.printStackTrace();
//            }
//          }
//        });

        webSocket.setClosedCallback(new CompletedCallback() {
          @Override
          public void onCompleted(Exception e) {
            Log.e("SERVER", "Disconnected " + webSocket.toString());
            sockets.remove(webSocket);
          }
        });

        sockets.add(webSocket);
      }
    });
    server.listen(port);
  }

  public void sendData(Object data) {
    for (WebSocket socket : sockets) {
      socket.send(data.toString());
    }
  }

  public void close() {
    for (WebSocket socket : sockets){
      socket.close();
    }
    server.stop();
  }

}
