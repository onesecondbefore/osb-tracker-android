package com.onesecondbefore.tracker.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.onesecondbefore.tracker.OSB;

import java.util.HashMap;
import java.util.Map;

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
        if (accountId.isEmpty()) {
            accountId = "development";
        }

        String serverUrl = mEditServerUrl.getText().toString();
        if ( serverUrl.isEmpty()) {
            serverUrl = "https://c.onesecondbefore.com";
        }

        Log.i(TAG, "AccountId = " + accountId);
        Log.i(TAG, "ServerUrl = " + serverUrl);

        OSB osb = OSB.getInstance();
        osb.create(this, accountId, serverUrl, "osbdemo.app");

        HashMap<String, Object> extraData = new HashMap<>();
        extraData.put("extra1", "value1");
        extraData.put("extra2", "value2");
        osb.set("extra", extraData);

        HashMap<String, Object> hitsData = new HashMap<>();
        hitsData.put("hit1", "value1");
        hitsData.put("hit2", "value2");
        osb.set(hitsData);

        String type = mEditType.getText().toString();
        String action = mEditAction.getText().toString();
        OSB.EventType eventType = getEventType(type);

        Log.i(TAG, "type = " + type);
        Log.i(TAG, "action = " + action);
        Log.i(TAG, "eventType = " + eventType);

        try {
            Map<String, Object> eventData = getEventData(OSB.EventType.valueOf(type.toUpperCase()));

            if (eventType == OSB.EventType.ACTION) {
                if (action.isEmpty()) {
                    showActionError();
                } else {
                    osb.send(OSB.EventType.ACTION, action, eventData);
                }
            } else if (eventType == OSB.EventType.PAGEVIEW) {
                osb.sendPageView("/homepage", "Homepage", null, eventData);
            } else if (eventType == OSB.EventType.SCREENVIEW) {
                osb.sendScreenView("Homepage", eventData);
            } else if (eventType == OSB.EventType.EVENT) {
                osb.sendEvent("event category", "event action", "event label", "1.00");
            }
        } catch (IllegalArgumentException ex) {
            showEventTypeError();
        }
    }

    /* Private Functions */
    private void initializeFields() {
        mEditType = findViewById(R.id.editType);
        mEditAction = findViewById(R.id.editAction);
        mEditAccountId = findViewById(R.id.editAccountId);
        mEditServerUrl = findViewById(R.id.editServerUrl);
        mSwitchLocation = findViewById(R.id.switchLocation);
    }

    private void showActionError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enter a valid action");
        builder.show();
    }

    private void showEventTypeError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Invalid event type");
        builder.show();
    }

    private OSB.EventType getEventType(String type) {
        OSB.EventType eventType = OSB.EventType.EVENT;
        if (type.equalsIgnoreCase("ids")) {
            eventType = OSB.EventType.IDS;
        } else if (type.equalsIgnoreCase("screenview")) {
            eventType = OSB.EventType.SCREENVIEW;
        } else if (type.equalsIgnoreCase("pageview")) {
            eventType = OSB.EventType.PAGEVIEW;
        } else if (type.equalsIgnoreCase("action")) {
            eventType = OSB.EventType.ACTION;
        } else if (type.equalsIgnoreCase("exception")) {
            eventType = OSB.EventType.EXCEPTION;
        } else if (type.equalsIgnoreCase("social")) {
            eventType = OSB.EventType.SOCIAL;
        } else if (type.equalsIgnoreCase("timing")) {
            eventType = OSB.EventType.TIMING;
        }

        return eventType;
    }

    private Map<String, Object> getEventData(OSB.EventType type) {
        Map<String, Object> eventData = new HashMap<>();
        if (type == OSB.EventType.IDS) {
            eventData.put("key", "login");
            eventData.put("value", "demouser@OSB.com");
            eventData.put("label", "single sign-on");
            eventData.put("is-signed-user", true);
            eventData.put("password", "OSBdemo123");
        } else if (type == OSB.EventType.SCREENVIEW) {
            eventData.put("id", "ink001");
            eventData.put("title", "Welcome to the profileScreen");
        }  else if (type == OSB.EventType.EVENT) {
            eventData.put("category", "category1");
            eventData.put("action", "action1");
            eventData.put("value", 30.0);
            eventData.put("extra_item", true);
            eventData.put("video_finished", false);
        } else if (type == OSB.EventType.ACTION) {
            eventData.put("id", "ink001");
            eventData.put("revenue", 29.20);
            eventData.put("shipping", 3.50);
            eventData.put("coupon", "ABC123");
        }  else if (type == OSB.EventType.EXCEPTION) {
            eventData.put("category", "JS Error");
            eventData.put("label", "ReferenceError: bla is not defined");
            eventData.put("userId", "test@demo.com");
        } else if (type == OSB.EventType.SOCIAL) {
            eventData.put("category", "Facebook");
            eventData.put("action", "like");
            eventData.put("label", "http://foo.com");
            eventData.put("addComment", true);
        } else if (type == OSB.EventType.TIMING) {
            eventData.put("category", "Page load");
            eventData.put("action", "onDomLoad");
            eventData.put("value", 30);
            eventData.put("isfavVideo", true);
        }

        return eventData;
    }
}
