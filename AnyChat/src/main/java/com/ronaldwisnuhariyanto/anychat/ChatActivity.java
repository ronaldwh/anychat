package com.ronaldwisnuhariyanto.anychat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ronaldwisnuhariyanto.anychat.model.RowItem;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

public class ChatActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ChatMainFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class ChatMainFragment extends Fragment {

        SocketIO _socket;
        private Context _context;
        private ListView _chatWindow;
        private List<RowItem> rowItems;
        private ChatWindowAdapter _adapter;

        private TextView _messageTextView;
        private String _username;
        private String _password;

        public ChatMainFragment() {
            _context = getActivity();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

            ImageButton sendChatButton = (ImageButton)rootView.findViewById(R.id.sendChatButton);
            sendChatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendChat(_username, getMessage());
                }
            });

            _messageTextView = (TextView)rootView.findViewById(R.id.message);

            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            _username = sharedPreferences.getString(getString(R.string.username), "");
            _password = sharedPreferences.getString(getString(R.string.password), "");

            _chatWindow = (ListView)rootView.findViewById(R.id.chatWindow);

            rowItems = new ArrayList<RowItem>();
            _adapter = new ChatWindowAdapter(getActivity(), rowItems);
            _chatWindow.setAdapter(_adapter);
            Log.i("PASSED USERNAME AND PASSWORD", _username + " and " + _password);

            try {
                _socket = new SocketIO("http://192.168.1.13:8080/");
                _socket.connect(new IOCallback() {
                    @Override
                    public void onMessage(JSONObject json, IOAcknowledge ack) {
                        try {
                            System.out.println("Server said:" + json.toString(2));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onMessage(String data, IOAcknowledge ack) {
                        System.out.println("Server said: " + data);
                    }

                    @Override
                    public void onError(SocketIOException socketIOException) {
                        System.out.println("an Error occured");
                        socketIOException.printStackTrace();
                    }

                    @Override
                    public void onDisconnect() {
                        System.out.println("Connection terminated.");
                    }

                    @Override
                    public void onConnect() {
                        System.out.println("Connection established");
                    }

                    @Override
                    public void on(String event, IOAcknowledge ack, Object... args) {
                        if (event.equals("updatechat")) {
                            String username = (String)args[0];
                            String message = (String)args[1];
                            updateChat(username, message);
                        }
                    }
                });

                // This line is cached until the connection is establisched.
                JSONObject user = new JSONObject();
                user.putOpt("username", _username);
                _socket.emit("adduserobject", user);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return rootView;
        }

        public void updateChat(final String username, final String message) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Executing updatechat STARTED");
                    if (_adapter != null) {
                        System.out.println("ADDING ADAPTER");
                        RowItem newMessage = new RowItem(0, username, message);
                        _adapter.rowItems.add(newMessage);
                        System.out.println("NOTIFYING ADAPTER");
                        _adapter.notifyDataSetChanged();
                        System.out.println("Executing updatechat DONE");
                        setMessage("");
                    }
                }
            });
        }

        public String getMessage() {
            return _messageTextView.getText().toString();
        }

        public void setMessage(String message) {
            _messageTextView.setText(message);
        }

        private void sendChat(String userName, String message) {
            if (_socket != null) {
                _socket.emit("sendchat", getMessage());
            }
        }
    }

    private class ChatWindowAdapter extends BaseAdapter {
        Context context;
        List<RowItem> rowItems;

        public ChatWindowAdapter(Context context, List<RowItem> items) {
            this.context = context;
            this.rowItems = items;
        }

        /*private view holder class*/
        private class ViewHolder {
            ImageView imageView;
            TextView txtTitle;
            TextView txtDesc;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item, null);
                holder = new ViewHolder();
                holder.txtDesc = (TextView) convertView.findViewById(R.id.desc);
                holder.txtTitle = (TextView) convertView.findViewById(R.id.title);
                //holder.imageView = (ImageView) convertView.findViewById(R.id.icon);
                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            RowItem rowItem = (RowItem) getItem(position);

            holder.txtDesc.setText(rowItem.getDesc());
            holder.txtTitle.setText(rowItem.getTitle());
            //holder.imageView.setImageResource(rowItem.getImageId());

            return convertView;
        }

        @Override
        public int getCount() {
            return rowItems.size();
        }

        @Override
        public Object getItem(int position) {
            return rowItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return rowItems.indexOf(getItem(position));
        }
    }
}
