package com.onesecondbefore.tracker.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.onesecondbefore.tracker.EventType;
import com.onesecondbefore.tracker.OSB;

import java.util.Dictionary;
import java.util.Hashtable;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "OSB:Demo";

    private EditText mEditType;
    private EditText mEditAction;
    private EditText mEditAccountId;
    private EditText mEditServerUrl;
    private Switch mSwitchLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initializeFields();
    }

    public void sendEvent(View view) {
        String accountId = mEditAccountId.getText().toString();
        if (accountId == null || accountId.isEmpty()) {
            accountId = "development";
        }

        String serverUrl = mEditServerUrl.getText().toString();
        if (serverUrl == null || serverUrl.isEmpty()) {
            serverUrl = "https://c.onesecondbefore.com";
        }

        Log.i(TAG, "AccountId = " + accountId);
        Log.i(TAG, "ServerUrl = " + serverUrl);

        OSB osb = OSB.getInstance(this);
        osb.initialize(accountId, serverUrl);

        String type = mEditType.getText().toString();
        String action = mEditAction.getText().toString();
        EventType eventType = getEventType(type);

        Log.i(TAG, "type = " + type);
        Log.i(TAG, "action = " + action);
        Log.i(TAG, "eventType = " + eventType);

        Dictionary<String, Object> eventData = getEventData(type);

        if (eventType == EventType.ACTION) {
            if (action.isEmpty()) {
                showActionError();
            } else {
                osb.sendEvent(EventType.ACTION, action, eventData);
            }
        } else {
            osb.sendEvent(eventType, action, eventData);
        }
    }

    /* Private Functions */
    private void initializeFields() {
        mEditType = (EditText)findViewById(R.id.editType);
        mEditAction = (EditText)findViewById(R.id.editAction);
        mEditAccountId = (EditText)findViewById(R.id.editAccountId);
        mEditServerUrl = (EditText)findViewById(R.id.editServerUrl);
        mSwitchLocation = (Switch)findViewById(R.id.switchLocation);
    }

    private void showActionError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enter a valid action");
        builder.show();
    }

    private EventType getEventType(String type) {
        EventType eventType = EventType.EVENT;
        if (type.equalsIgnoreCase("ids")) {
            eventType = EventType.IDS;
        } else if (type.equalsIgnoreCase("screenview")) {
            eventType = EventType.SCREENVIEW;
        } else if (type.equalsIgnoreCase("pageview")) {
            eventType = EventType.PAGEVIEW;
        } else if (type.equalsIgnoreCase("action")) {
            eventType = EventType.ACTION;
        } else if (type.equalsIgnoreCase("exception")) {
            eventType = EventType.EXCEPTION;
        } else if (type.equalsIgnoreCase("social")) {
            eventType = EventType.SOCIAL;
        } else if (type.equalsIgnoreCase("timing")) {
            eventType = EventType.TIMING;
        }

        return eventType;
    }

    private Dictionary<String, Object> getEventData(String type) {
        Hashtable<String, Object> eventData = new Hashtable<>();
        if (type.equalsIgnoreCase("ids")) {
            eventData.put("key", "login");
            eventData.put("value", "demouser@OSB.com");
            eventData.put("label", "single sign-on");
            eventData.put("is-signed-user", true);
            eventData.put("password", "OSBdemo123");
        } else if (type.equalsIgnoreCase("screenview")) {
            eventData.put("id", "ink001");
            eventData.put("title", "Welcome to the profileScreen");
        }  else if (type.equalsIgnoreCase( "event")) {
            eventData.put("category", "category1");
            eventData.put("action", "action1");
            eventData.put("value", 30.0);
            eventData.put("extra_item", true);
            eventData.put("video_finished", false);
        } else if (type.equalsIgnoreCase("action")) {
            eventData.put("id", "ink001");
            eventData.put("revenue", 29.20);
            eventData.put("shipping", 3.50);
            eventData.put("coupon", "ABC123");
        }  else if (type.equalsIgnoreCase("exception")) {
            eventData.put("category", "JS Error");
            eventData.put("label", "ReferenceError: bla is not defined");
            eventData.put("userId", "test@demo.com");
        } else if (type.equalsIgnoreCase("social")) {
            eventData.put("category", "Facebook");
            eventData.put("action", "like");
            eventData.put("label", "http://foo.com");
            eventData.put("addComment", true);
        } else if (type.equalsIgnoreCase("timing")) {
            eventData.put("category", "Page load");
            eventData.put("action", "onDomLoad");
            eventData.put("value", 30);
            eventData.put("isfavVideo", true);
        }

        return eventData;
    }
}
