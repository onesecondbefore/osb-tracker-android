package com.onesecondbefore.tracker.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.onesecondbefore.tracker.OSB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        new Thread( new Runnable() { @Override public void run() {
            sendEventBackground(view);
        } } ).start();
    }

    public void sendEventBackground(View view) {
        String accountId = mEditAccountId.getText().toString();
        if (accountId.isEmpty()) {
            accountId = "development";
        }

        String serverUrl = mEditServerUrl.getText().toString();
        if (serverUrl.isEmpty()) {
            serverUrl = "https://c.onesecondbefore.com";
        }

        Log.i(TAG, "AccountId = " + accountId);
        Log.i(TAG, "ServerUrl = " + serverUrl);

        OSB osb = OSB.getInstance();
        osb.config(this, accountId, serverUrl, "osbdemo.app");

        osb.setConsent(new String[]{"marketing", "social", "functional", "advertising"});

        Log.i(TAG, "consent: " + Arrays.toString(osb.getConsent()));


        String type = mEditType.getText().toString();
        String action = mEditAction.getText().toString();
        OSB.HitType hitType = getHitType(type);

        Log.i(TAG, "type = " + type);
        Log.i(TAG, "action = " + action);
        Log.i(TAG, "hitType = " + hitType);

        // TEST 1: Set page & send viewable_impression
        HashMap<String, Object> pageData = new HashMap<>();
        pageData.put("id", "1234");
        pageData.put("title", "The Great Escape");
        pageData.put("url", "https://www.binge.nl");
        osb.set(OSB.SetType.PAGE, pageData);

        HashMap<String, Object> data1 = new HashMap<>();
        data1.put("page_id", "5678");
        data1.put("campaign_id", "2");
        try {
            osb.send(OSB.HitType.VIEWABLE_IMPRESSION, data1);
        } catch (IllegalArgumentException ex) {
            showHitTypeError();
        }

        // TEST 2: Send event, still with page data from previous set
//        data.clear(); //Clear hashmap
        HashMap<String, Object> data2 = new HashMap<>();
        data2.put("category", "unit_test");
        data2.put("action", "unit_action");
        data2.put("label", "unit_label");
        data2.put("value", 8.9);
        try {
            osb.send(OSB.HitType.EVENT, data2);
        } catch (IllegalArgumentException ex) {
            showHitTypeError();
        }

        // TEST 3: Send ids with an event
        Map<String, Object> ids1 = new HashMap<>();
        ids1.put("key", "a3");
        ids1.put("value", "12345");

        Map<String, Object> ids2 = new HashMap<>();
        ids2.put("key", "b4");
        ids2.put("value", "6789");

        ArrayList<Map<String, Object>> idsList = new ArrayList<>();
        idsList.add(ids1);
        idsList.add(ids2);

        osb.setIds(idsList);

        HashMap<String, Object> data3 = new HashMap<>();
//        data.clear(); //Clear hashmap
        data3.put("category", "unit_test");
        data3.put("action", "ids");
        data3.put("label", "send");
        try {
            osb.send(OSB.HitType.EVENT, data3);
        } catch (IllegalArgumentException ex) {
            showHitTypeError();
        }

        //TEST 4: Action
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "sku123");
        item1.put("name", "Apple iPhone 14 Pro");
        item1.put("category", "mobile");
        item1.put("price", 1234.56);
        item1.put("quantity", 1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", "sku234");
        item2.put("name", "Samsung Galaxy S22");
        item2.put("category", "mobile");
        item2.put("price", 1034.56);
        item2.put("quantity", 1);

        List<Map<String, Object>> itemData = new ArrayList<>();
        itemData.add(item1);
        itemData.add(item2);

        osb.set(OSB.SetType.ITEM, itemData);

//        data.clear(); //Clear hashmap
        HashMap<String, Object> data4 = new HashMap<>();
        data4.put("id", "abcd1234");
        data4.put("revenue", 2269.12);
        data4.put("tax", (2269.12 * 0.21));
        data4.put("shipping", 100);
        data4.put("affiliation", "partner_funnel"); // Custom data item

        try {
            osb.send(OSB.HitType.ACTION, "purchase", data4);
        } catch (IllegalArgumentException ex) {
            showHitTypeError();
        }

        // TEST 6: Aggregate
        Map<String, Object> data6 = new HashMap<>();
        data6.put("scope", "pageview");
        data6.put("name", "scrolldepth");
        data6.put("aggregate", OSB.AggregateType.MAX);
        data6.put("value", 0.8);

        // Without helper
        try {
            osb.send(OSB.HitType.AGGREGATE, data6);
        } catch (IllegalArgumentException ex) {
            showHitTypeError();
        }
        // With helper
        try {
            osb.sendAggregate("pageview", "scrolldepth", OSB.AggregateType.MAX, 0.8);
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
        this.runOnUiThread(new Runnable() {
            public void run() {
                builder.show();
            }
        });
    }

    private void showHitTypeError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Invalid hit type");
        this.runOnUiThread(new Runnable() {
            public void run() {
                builder.show();
            }
        });
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
        } else if (type.equalsIgnoreCase("viewable_impression"))
            hitType = OSB.HitType.VIEWABLE_IMPRESSION;
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
        } else if (type == OSB.HitType.EVENT) {
            eventData.put("category", "category1");
            eventData.put("action", "action1");
            eventData.put("value", 30.0);
            eventData.put("extra_item", true);
            eventData.put("video_finished", false);
        } else if (type == OSB.HitType.ACTION) {
            eventData.put("id", "abcd1234");
            eventData.put("revenue", 2269.12);
            eventData.put("tax", (2269.12 * 0.21));
            eventData.put("shipping", 100);
            eventData.put("affiliation", "partner_funnel");
        } else if (type == OSB.HitType.EXCEPTION) {
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
        } else if (type == OSB.HitType.VIEWABLE_IMPRESSION) {
            eventData.put("a", 1);
            eventData.put("b", 2);
        }
        return eventData;
    }
}
