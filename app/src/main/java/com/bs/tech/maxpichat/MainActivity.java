package com.bs.tech.maxpichat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.history.PNHistoryItemResult;
import com.pubnub.api.models.consumer.history.PNHistoryResult;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String ACTIVITY_TAG= "In Main Activity -> ";
    private Button sendButton;
    EditText newMsg;
    private PNConfiguration pnConfiguration;
    private PubNub pubnub;
    private ListView mListView;
    TextView msg;
    private ArrayList<String> msgList= new ArrayList<String>();
    ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //added hereforth

        newMsg= findViewById(R.id.newMsg);
        sendButton= findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);

        mListView= findViewById(R.id.mListView);

        //mListView.addView(view);


        pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey("sub-c-8120bb3e-250f-11e8-a8f3-22fca5d72012");
        pnConfiguration.setPublishKey("pub-c-ae2b3b18-b578-4a80-a2ef-76c2b98c3737");
        pnConfiguration.setSecure(false);

        pubnub = new PubNub(pnConfiguration);

        pubnub.subscribe().channels(Arrays.asList("temp")).execute();

        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {


                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // This event happens when radio / connectivity is lost
                    Toast.makeText(MainActivity.this, "Unexpected Disconnect", Toast.LENGTH_SHORT).show();
                }

                else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {

                    // Connect event. You can do stuff like publish, and know you'll get it.
                    // Or just use the connected event to confirm you are subscribed for
                    // UI / internal notifications, etc
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    /*if (status.getCategory() == PNStatusCategory.PNConnectedCategory){

                    }*/

                    pubnub.history()
                            .channel("temp") // where to fetch history from
                            .count(100) // how many items to fetch
                            .async(new PNCallback<PNHistoryResult>() {
                                @Override
                                public void onResponse(PNHistoryResult result, PNStatus status) {
                                    List<PNHistoryItemResult> objarr= result.getMessages();
                                    PNHistoryItemResult obj = null;

                                    for(int i=0; i<objarr.size();
                                        ++i)
                                    {
                                        String m="";
                                        //m= m+ objarr.get(i).getEntry().getAsString().replace("\"", "");
                                        obj= objarr.get(i);
                                        m=m+obj.getEntry().getAsJsonObject().get("nameValuePairs").getAsJsonObject()
                                                .get("text").toString().replace("\"", "");
                                        msgList.add(m);
                                        Log.d(ACTIVITY_TAG, ""+m);
                                    }
                                    adapter= new ArrayAdapter(MainActivity.this, R.layout.message_item, msgList);
                                    mListView.setAdapter(adapter);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                }
                else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {

                    // Happens as part of our regular operation. This event happens when
                    // radio / connectivity is lost, then regained.
                    Toast.makeText(MainActivity.this, "Reconnect", Toast.LENGTH_SHORT).show();
                }
                else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory) {

                    // Handle messsage decryption error. Probably client configured to
                    // encrypt messages and on live data feed it received plain text.
                }
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                // Handle new message stored in message.message
                if (message.getChannel() != null) {
                    // Message has been received on channel group stored in
                    // message.getChannel()
                    //Toast.makeText(MainActivity.this, "Message in getChannel()",
                    //        Toast.LENGTH_SHORT).show();
                }
                else {
                    // Message has been received on channel stored in
                    // message.getSubscription()
                    //Toast.makeText(MainActivity.this, "Message in getSubscription()",
                    //        Toast.LENGTH_SHORT).show();
                }

            /*
                log the following items with your favorite logger
                    - message.getMessage()
                    - message.getSubscription()
                    - message.getTimetoken()
                    */
                //msgList.add(message.getMessage().toString());
                //adapter.notifyDataSetChanged();
                Log.d(ACTIVITY_TAG, "Message: "+ message.getMessage());
                Log.d(ACTIVITY_TAG, "Subscription: "+ message.getSubscription());
                Log.d(ACTIVITY_TAG, "Timetoken: "+ message.getTimetoken());
                Log.d(ACTIVITY_TAG, "Channel: "+ message.getChannel());
                Log.d(ACTIVITY_TAG, "Publisher: "+ message.getPublisher());
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.sendButton:
                JSONObject ms= null;
                if(newMsg.getText().toString().isEmpty())
                {
                    newMsg.setError("Please enter a message first");
                }
                else
                {
                    newMsg.setError(null);
                    ms= new JSONObject();
                    try {
                        ms.put("text", newMsg.getText().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    pubnub.publish().channel("temp").message(ms)
                            .async(new PNCallback<PNPublishResult>() {
                                @Override
                                public void onResponse(PNPublishResult result, PNStatus status) {
                                    // Check whether request successfully completed or not.
                                    if (!status.isError()) {
                                        // Message successfully published to specified channel.
                                        newMsg.setText("");
                                        Toast.makeText(MainActivity.this, "Message sent",
                                                Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(MainActivity.this, MainActivity.class));
                                        finish();
                                    }
                                    // Request processing failed.
                                    else {

                                        // Handle message publish error. Check 'category' property to find out possible issue
                                        // because of which request did fail.
                                        //
                                        // Request can be resent using: [status retry];
                                        Toast.makeText(MainActivity.this, "Not sent. Try again later.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
                break;
        }
    }
}