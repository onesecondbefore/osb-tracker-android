package com.onesecondbefore.tracker.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.analytics.FirebaseAnalytics;
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

    private OSB mOsb;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initializeFields();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    public void sendEvent(View view) {
//        new Thread( new Runnable() { @Override public void run() {
            sendEventBackground(view);
//        } } ).start();
    }

    public void showCMP(View view) {
        showConsentWebview(false);
    }

    public void resurfaceCMP(View view) {
        showConsentWebview(true);
    }

    public void showConsentWebview(Boolean forceShow) {
        if (mOsb == null) {
            inializeOSB();
        }
        mOsb.showConsentWebview(this, forceShow);
    }

    public void inializeOSB() {
        String accountId = mEditAccountId.getText().toString();
        if (accountId.isEmpty()) {
            accountId = "demo";
        }

        String serverUrl = mEditServerUrl.getText().toString();
        if (serverUrl.isEmpty()) {
            serverUrl = "https://c.onesecondbefore.com";
        }

        String siteId = "demo.app";
        Log.i(TAG, "AccountId = " + accountId);
        Log.i(TAG, "ServerUrl = " + serverUrl);
        Log.i(TAG, "siteId = " + siteId);

        mOsb = OSB.getInstance();
        mOsb.config(this, accountId, serverUrl, siteId);

        mOsb.addGoogleConsentCallback(consent -> {
            Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> consentMap = new HashMap<>();
            consent.forEach((t,s) -> consentMap.put(FirebaseAnalytics.ConsentType.valueOf(t), FirebaseAnalytics.ConsentStatus.valueOf(s)));
            mFirebaseAnalytics.setConsent(consentMap);
        });
    }

    

    public void sendEventBackground(View view) {

        inializeOSB();

        Handler handler = new Handler();

//        mOsb.setConsent(new String[]{"marketing", "social", "functional", "advertising"});

        Log.i(TAG, "consent: " + Arrays.toString(mOsb.getConsent()));
//
        // TEST 1: Send pageview
        handler.postDelayed(new Runnable() {
            public void run() {

//                HashMap<String, Object> ids = new HashMap<>();
//                ids.put("key", "email");
//                ids.put("value", 'test@hotmail.com');
//                ids.put("hash", 1);
//                mOsb.set(OSB.SetType.IDS, ids);


                HashMap<String, Object> pageData = new HashMap<>();
                pageData.put("id", "1234");
                pageData.put("title", "Onesecondbefore");
                pageData.put("url", "https://www.onesecondbefore.com");

                //Without helper:
                try {
                    mOsb.send(OSB.HitType.PAGEVIEW, pageData);
                } catch (IllegalArgumentException ex) {
                    showHitTypeError();
                }
            }
        }, 1000);


        // TEST 2: Set page data & send viewable_impression
        handler.postDelayed(new Runnable() {
            public void run() {
                HashMap<String, Object> data1 = new HashMap<>();
                data1.put("page_id", "11111");
                data1.put("campaign_id", "1");
                mOsb.set(OSB.SetType.PAGE, data1);
                try {
                    mOsb.send(OSB.HitType.VIEWABLE_IMPRESSION, data1);
                } catch (IllegalArgumentException ex) {
                    showHitTypeError();
                }
            }
        }, 2000);


        // TEST 3: Set page data & send viewable_impression
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    mOsb.sendPageView("https://www.theurl.com", "Custom page title", "https://www.thereferrer.com", "3456");
                } catch (IllegalArgumentException ex) {
                    showHitTypeError();
                }
            }
        }, 3000);


        // TEST 4: Send event, still with page data from previous set
        handler.postDelayed(new Runnable() {
            public void run() {
                HashMap<String, Object> data2 = new HashMap<>();
                data2.put("category", "unit_test");
                data2.put("action", "unit_action");
                data2.put("label", "unit_label");
                data2.put("value", 8.9);
                try {
                    mOsb.send(OSB.HitType.EVENT, data2);
                } catch (IllegalArgumentException ex) {
                    showHitTypeError();
                }
            }
        }, 4000);

//        Test 5: screenview (moet ook een vid genereren)
        handler.postDelayed(new Runnable() {
            public void run() {
                mOsb.sendScreenView("screenName", "screenClass");
            }
        }, 5000);

        // TEST 6: Send event
        handler.postDelayed(new Runnable() {
            public void run() {
                HashMap<String, Object> data3 = new HashMap<>();
                data3.put("category", "test after screenview");
                data3.put("action", "some action");
                data3.put("label", "some label");
                data3.put("value", 8.9);
                try {
                    mOsb.send(OSB.HitType.EVENT, data3);
                } catch (IllegalArgumentException ex) {
                    showHitTypeError();
                }
            }
            }, 6000);

        //TEST 7: Action
        handler.postDelayed(new Runnable() {
            public void run() {
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

                mOsb.set(OSB.SetType.ITEM, itemData);

                HashMap<String, Object> data4 = new HashMap<>();
                data4.put("id", "abcd1234");
                data4.put("revenue", 2269.12);
                data4.put("tax", (2269.12 * 0.21));
                data4.put("shipping", 100);
                data4.put("affiliation", "partner_funnel"); // Custom data item
                mOsb.set(OSB.SetType.ACTION, data4);

                try {
                    mOsb.sendPageView("https://www.onesecondbefore.com/thankyou.html", "Thank you page", "https://www.onesecondbefore.com/payment.html", "3456");
                } catch (IllegalArgumentException ex) {
                    showHitTypeError();
                }
            }
        }, 7000);
//
        // TEST 8: Aggregate
        handler.postDelayed(new Runnable() {
            public void run() {
//                Map<String, Object> data6 = new HashMap<>();
//                data6.put("scope", "pageview");
//                data6.put("name", "scrolldepth");
//                data6.put("aggregate", OSB.AggregateType.MAX);
//                data6.put("value", 0.8);
//
//                // Without helper
//                try {
//                    mOsb.send(OSB.HitType.AGGREGATE, data6);
//                } catch (IllegalArgumentException ex) {
//                    showHitTypeError();
//                }
                try {
                    mOsb.sendAggregate("pageview", "scrolldepth", OSB.AggregateType.MAX, 0.8);
                } catch (IllegalArgumentException ex) {
                    showHitTypeError();
                }
            }
        }, 8000);
//        // With helper



//        Map<String, Object> data = new HashMap<>();
//        data.put("id", "12345");
//        data.put("title", "the matrix");
//        data.put("url", "binge.com");
//
//        mOsb.set(OSB.SetType.PAGE, data);
//
//        HashMap<String, Object> data1 = new HashMap<>();
//        data1.put("page_id", "5678");
//        data1.put("campaign_id", "2");
//        try {
//            mOsb.send(OSB.HitType.VIEWABLE_IMPRESSION, data1);
//        } catch (IllegalArgumentException ex) {
//            showHitTypeError();
//        }

//        mOsb.set(OSB.SetType.PAGE, new HashMap<>());

//
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                mOsb.sendPageView("pageview url", "pagviewTitle", "pageview referrer","screenview id");
//            }
//        }, 2000);
//
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                mOsb.send(OSB.HitType.ACTION, "purchase", data4);
//            }
//        }, 3000);

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
