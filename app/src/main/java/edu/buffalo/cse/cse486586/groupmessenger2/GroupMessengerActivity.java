package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

import static android.content.ContentValues.TAG;
import static android.net.wifi.WifiConfiguration.Status.strings;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String[] ports = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
    static final int SERVER_PORT = 10000;
//   HashMap<Integer, Integer> map = new HashMap<>(0,0);
    PriorityQueue<Storage> pq = new PriorityQueue<Storage>(10, new StorageComparator());
    ReentrantLock lock = new ReentrantLock();

    static Integer seq_counter = 0;
    boolean deliverable=false;
    float final_seq=0;
    int idn=0;
    ArrayList<Float> lst = new ArrayList<Float>();

    //put lock on all global variables
    String myPort = null;

    class StorageComparator implements Comparator<Storage> {

        // Overriding compare()method of Comparator
        // for descending order of cgpa
        public int compare(Storage s1, Storage s2) {
            if (s1.seq_counter > s2.seq_counter)
                return 1;
            else if (s1.seq_counter < s2.seq_counter)
                return -1;
            return 0;
        }
    }

    class Storage {
        public String msgStored;
        public float seq_counter;
        public boolean deliverable;
        public  int idn;
        // A parameterized constructor
        public Storage(String msgStored, float seq_counter, boolean deliverable, int idn) {

            this.msgStored = msgStored;
            this.seq_counter = seq_counter;
            this.deliverable=deliverable;
            this.idn=idn;
        }


    }


        Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Entered in OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        try
        {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.d(TAG, "Server Socket made");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e)
        {
            Log.e(TAG, e.getMessage());
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(tv, getContentResolver()));

        Button button = (Button) findViewById(R.id.button4);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                    Log.e("insert", "Send pressed");
                press_send(v);
            }
        });
    }


    public void press_send(View View) {

        Log.e("insert", "In press send");

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        //PA1
        EditText et = (EditText) findViewById(R.id.editText1);
//        TextView tv = (TextView) findViewById(R.id.textView1);
        String msg = et.getText().toString() + "\n";
        et.setText(""); // This is one way to reset the input box.
//        tv.append(msg+"\n"); // This is one way to display a string.
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        //Read data from file
        DataInputStream ds;

        String message;

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];
            Integer proposal=0;


            //implement file storage wali cheez here
            while(true) {


                try {
                    //create new Socket
//                    Log.d(TAG, "Socket created " + soc);

                    //accept thru serversocket
                    Socket soc = serverSocket.accept();

                    //new DataOutputStream
                    BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
//                    Log.d(TAG, "Data stream created ");

                    message = br.readLine();
                    Log.d("server", message);


                    DataOutputStream out = new DataOutputStream(soc.getOutputStream());
                    out.writeUTF(String.valueOf(proposal));
                    out.flush();
                    Log.d("server", ""+ proposal);

                    proposal=proposal+1;

                    DataInputStream in = new DataInputStream(soc.getInputStream());
                    String finalProposal = in.readUTF();
                    Log.d("server", "Final proposal" + finalProposal);



//
                    String[] str = new String[]{finalProposal, message};
                    publishProgress(str);
//                    Log.d(TAG, "passed message to publish progress ");



                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

//             return null;
        }




        protected void onProgressUpdate(String...strings) {

            Log.d(TAG, "In progress update");
            String fname = strings[0].trim();
            String fdata = strings[1].trim();

            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(fname +"--" + fdata+"\n");

            Log.d(TAG, "Received message - Message: \'"+fname+"\'");
            ContentValues mContentValues = new ContentValues();
            mContentValues.put("key",String.valueOf(seq_counter));
            seq_counter++;
            mContentValues.put("value",fdata);
            getContentResolver().insert(mUri, mContentValues);

            return;
        }
    }



    private class ClientTask extends AsyncTask<String, Void, Void> {
        int pId = 0;
        ArrayList<Integer> lst = new ArrayList<Integer>();


        @Override
        protected Void doInBackground(String... msgs) {

           try{
               //Make 5 sockets
               Socket[] socket = new Socket[5];

               //Socket 0
               try{
                   //INITIAL MESSAGE
                   //establish connection
                   socket[0] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                           Integer.parseInt(ports[0]));

                   //construct message
                   String msgTosend = msgs[0];

                   //Data streams to WRITE
                   BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket[0].getOutputStream()));

                   //Write to the file
                   bw.write(msgTosend);
                   Log.d(TAG, "0th message written to file : "+msgTosend);

                   //Flush the stream
                   bw.flush();


                   //PROPOSAL
                   DataInputStream br = new DataInputStream(socket[0].getInputStream());

                   String pro = br.readUTF();
                   Log.d(TAG, "Reading proposal from 0th"+pro);

                   lst.add(Integer.valueOf(pro));

               }
               catch (Exception e)
               {
                   Log.e(TAG, "Socket 0 Exception");
               }

               //Socket 1
               try{
                   //INITIAL MESSAGE
                   //establish connection
                   socket[1] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                           Integer.parseInt(ports[1]));

                   //construct message
                   String msgTosend = msgs[0];

                   //Data streams to WRITE
                   BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket[1].getOutputStream()));

                   //Write to the file
                   bw.write(msgTosend);
                   Log.d(TAG, "1st message written to file : "+msgTosend);

                   //Flush the stream
                   bw.flush();


                   //PROPOSAL
                   DataInputStream br = new DataInputStream(socket[1].getInputStream());

                   String pro = br.readUTF();
                   Log.d(TAG, "Reading proposal from 1st"+pro);

                   lst.add(Integer.valueOf(pro));

               }
               catch (Exception e)
               {
                   Log.e(TAG, "Socket 1 Exception");
               }

               //Socket 2
               try{
                   //INITIAL MESSAGE
                   //establish connection
                   socket[2] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                           Integer.parseInt(ports[2]));

                   //construct message
                   String msgTosend = msgs[0];

                   //Data streams to WRITE
                   BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket[2].getOutputStream()));

                   //Write to the file
                   bw.write(msgTosend);
                   Log.d(TAG, "2nd message written to file : "+msgTosend);

                   //Flush the stream
                   bw.flush();


                   //PROPOSAL
                   DataInputStream br = new DataInputStream(socket[2].getInputStream());

                   String pro = br.readUTF();
                   Log.d(TAG, "Reading proposal from 2nd"+pro);

                   lst.add(Integer.valueOf(pro));

               }
               catch (Exception e)
               {
                   Log.e(TAG, "Socket 2 Exception");
               }

               //Socket 3
               try{
                   //INITIAL MESSAGE
                   //establish connection
                   socket[3] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                           Integer.parseInt(ports[3]));

                   //construct message
                   String msgTosend = msgs[0];

                   //Data streams to WRITE
                   BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket[3].getOutputStream()));

                   //Write to the file
                   bw.write(msgTosend);
                   Log.d(TAG, "3rd message written to file : "+msgTosend);

                   //Flush the stream
                   bw.flush();


                   //PROPOSAL
                   DataInputStream br = new DataInputStream(socket[3].getInputStream());

                   String pro = br.readUTF();
                   Log.d(TAG, "Reading proposal from 3rd"+pro);

                   lst.add(Integer.valueOf(pro));

               }
               catch (Exception e)
               {
                   Log.e(TAG, "Socket 3 Exception");
               }

               //Socket 4
               try{
                   //INITIAL MESSAGE
                   //establish connection
                   socket[4] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                           Integer.parseInt(ports[4]));

                   //construct message
                   String msgTosend = msgs[0];

                   //Data streams to WRITE
                   BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket[4].getOutputStream()));

                   //Write to the file
                   bw.write(msgTosend);
                   Log.d(TAG, "4th message written to file : "+msgTosend);

                   //Flush the stream
                   bw.flush();


                   //PROPOSAL
                   DataInputStream br = new DataInputStream(socket[4].getInputStream());

                   String pro = br.readUTF();
                   Log.d(TAG, "Reading proposal from 4th"+pro);

                   lst.add(Integer.valueOf(pro));

               }
               catch (Exception e)
               {
                   Log.e(TAG, "Socket 4 Exception");
               }

               //WRITING MAXIMUM PROPOSAL TO ALL
               Integer final_proposal = Collections.max(lst);
               Log.e(TAG, "Final proposal accepted : "+final_proposal);

               for(int i=0; i<5; i++)
               {
                   DataOutputStream bw = new DataOutputStream(socket[i].getOutputStream());
                   bw.writeUTF(String.valueOf(final_proposal));
                   bw.flush();
               }
           }
           catch (Exception e)
           {
               Log.e(TAG, "ClientTask Exception");
           }
            return null;

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
    }
