package com.onesecondbefore.tracker.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.onesecondbefore.tracker.OSB;

import java.util.Arrays;
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
        osb.config(this, accountId, serverUrl, "osbdemo.app");

        osb.setConsent(new String[]{"Dit is een test consent", "dit ook", "en deze ook natuurlijk"});

        Log.i(TAG, "consent: " + Arrays.toString(osb.getConsent()));

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
        OSB.HitType hitType = getHitType(type);

        Log.i(TAG, "type = " + type);
        Log.i(TAG, "action = " + action);
        Log.i(TAG, "hitType = " + hitType);

        try {
            Map<String, Object> eventData = getEventData(OSB.HitType.valueOf(type.toUpperCase()));

            if (hitType == OSB.HitType.ACTION) {
                if (action.isEmpty()) {
                    showActionError();
                } else {
                    osb.send(OSB.HitType.ACTION, action, eventData);
                }
            } else if (hitType == OSB.HitType.PAGEVIEW) {
                osb.sendPageView("/homepage", "Homepage", null, eventData);
            } else if (hitType == OSB.HitType.SCREENVIEW) {
                osb.sendScreenView("Homepage", eventData);
            } else if (hitType == OSB.HitType.EVENT) {
                osb.sendEvent("event category", "event action", "event label", "1.00");
            } else if (hitType == OSB.HitType.AGGREGATE) {
                osb.sendAggregateEvent("scope", "scrolledepth", OSB.AggregateType.MAX, 0.8);
            }
        } catch (IllegalArgumentException ex) {
            showHitTypeError();
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

    private void showHitTypeError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Invalid hit type");
        builder.show();
    }

    private OSB.HitType getHitType(String type) {
        OSB.HitType hitType = OSB.HitType.EVENT;
        if (type.equalsIgnoreCase("ids")) {
            hitType = OSB.HitType.IDS;
        } else if (type.equalsIgnoreCase("screenview")) {
            hitType = OSB.HitType.SCREENVIEW;
        } else if (type.equalsIgnoreCase("pageview")) {
            hitType = OSB.HitType.PAGEVIEW;
        } else if (type.equalsIgnoreCase("action")) {
            hitType = OSB.HitType.ACTION;
        } else if (type.equalsIgnoreCase("exception")) {
            hitType = OSB.HitType.EXCEPTION;
        } else if (type.equalsIgnoreCase("social")) {
            hitType = OSB.HitType.SOCIAL;
        } else if (type.equalsIgnoreCase("timing")) {
            hitType = OSB.HitType.TIMING;
        } else if (type.equalsIgnoreCase("aggregate")) {
            hitType = OSB.HitType.AGGREGATE;
        }
        return hitType;
    }

    private Map<String, Object> getEventData(OSB.HitType type) {
        Map<String, Object> eventData = new HashMap<>();
        if (type == OSB.HitType.IDS) {
            eventData.put("key", "login");
            eventData.put("value", "demouser@OSB.com");
            eventData.put("label", "single sign-on");
            eventData.put("is-signed-user", true);
            eventData.put("password", "OSBdemo123");
        } else if (type == OSB.HitType.SCREENVIEW) {
            eventData.put("id", "ink001");
            eventData.put("title", "Welcome to the profileScreen");
        }  else if (type == OSB.HitType.EVENT) {
            eventData.put("category", "category1");
            eventData.put("action", "action1");
            eventData.put("value", 30.0);
            eventData.put("extra_item", true);
            eventData.put("video_finished", false);
        } else if (type == OSB.HitType.ACTION) {
            eventData.put("id", "ink001");
            eventData.put("revenue", 29.20);
            eventData.put("shipping", 3.50);
            eventData.put("coupon", "ABC123");
        }  else if (type == OSB.HitType.EXCEPTION) {
            eventData.put("category", "JS Error");
            eventData.put("label", "ReferenceError: bla is not defined");
            eventData.put("userId", "test@demo.com");
        } else if (type == OSB.HitType.SOCIAL) {
            eventData.put("category", "Facebook");
            eventData.put("action", "like");
            eventData.put("label", "http://foo.com");
            eventData.put("addComment", true);
        } else if (type == OSB.HitType.TIMING) {
            eventData.put("category", "Page load");
            eventData.put("action", "onDomLoad");
            eventData.put("value", 30);
            eventData.put("isfavVideo", true);
        }

        return eventData;
    }
}
