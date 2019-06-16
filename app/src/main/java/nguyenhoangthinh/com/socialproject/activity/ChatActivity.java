package nguyenhoangthinh.com.socialproject.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nguyenhoangthinh.com.socialproject.R;
import nguyenhoangthinh.com.socialproject.adapters.AdapterChat;
import nguyenhoangthinh.com.socialproject.models.Chat;
import nguyenhoangthinh.com.socialproject.models.Room;
import nguyenhoangthinh.com.socialproject.models.User;
import nguyenhoangthinh.com.socialproject.notifications.APIService;
import nguyenhoangthinh.com.socialproject.notifications.Client;
import nguyenhoangthinh.com.socialproject.notifications.Data;
import nguyenhoangthinh.com.socialproject.notifications.Response;
import nguyenhoangthinh.com.socialproject.notifications.Sender;
import nguyenhoangthinh.com.socialproject.notifications.Token;
import nguyenhoangthinh.com.socialproject.widgets.TypingVisualizer;
import retrofit2.Call;
import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_VIDEO_CALL = 100;

    private  static final int GALLERY_PICK = 1;

    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] REQUESTED_PERMISSIONS =
            {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    //Firebase
    private FirebaseAuth mAuth;

    private FirebaseDatabase firebaseDatabase;

    private DatabaseReference databaseReference;

    private StorageReference firebaseStorage;
    //

    //Kiểm tra bật tắt tính năng voice
    private boolean isVoice = true;

    //Sử dụng tính năng voice
    private MediaRecorder mediaRecorder;

    private static final String outputFileRecord =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";

    //Để kiểm tra xem người dùng có nhìn thấy tin nhắn hay không
    private ValueEventListener valueEventListener;

    private DatabaseReference referenceForSeen;
    //
    private List<Chat> chatList;

    private AdapterChat adapterChat;

    //UI
    private Toolbar toolbar;

    private RecyclerView recyclerView;

    private ImageView imgProfile;

    private TextView txtName, txtStatus;

    private EditText edtMessage;

    private ImageButton btnSend, btnVideoCall, btnSendImg;

    private LinearLayout typingLinearLayout;

    private TypingVisualizer typingVisualizer;

    //

    private String hisUid;

    private String hisImage;

    private String myUid;

    //
    private APIService apiService;

    private boolean notify = false;

    private boolean isCalling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ActivityCompat.requestPermissions(ChatActivity.this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(outputFileRecord);

        initializeUI();
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //get time stamp
        if(!isCalling) {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            //set offline with last seen time stamp
            checkOnlineStatus(timeStamp);
        }

        //set typing
        checkTypingStatus("noOne");

        //Người dùng tạm dừng chương trình thì kết thúc việc lắng nghe
        referenceForSeen.removeEventListener(valueEventListener);
    }

    @Override
    protected void onResume() {
        //set online
        checkOnlineStatus("online");
        super.onResume();
    }

    private void initializeUI(){

        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.toolBarChat);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        typingLinearLayout = findViewById(R.id.typingLayout);
        typingVisualizer   = findViewById(R.id.typingVisualizer);
        recyclerView = findViewById(R.id.recyclerViewChats);
        imgProfile   = findViewById(R.id.circularProfile);
        txtName      = findViewById(R.id.txtName);
        txtStatus    = findViewById(R.id.txtStatus);
        edtMessage   = findViewById(R.id.edtMessage);
        btnSend      = findViewById(R.id.btnSend);
        btnVideoCall = findViewById(R.id.btnVideoCall);
        btnSendImg   = findViewById(R.id.btn_Img_Send);
        //Layout linear for recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        //create API service
        apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);



        mAuth        = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("User");
        firebaseStorage = FirebaseStorage.getInstance().getReference();

        //Khi click vào một user ta có uid. Chúng ta sử dụng UID này để có được hình ảnh và bắt
        //đầu trò chuyện cùng người đó
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        //Tìm kiếm thông tin bạn chat
        Query query = databaseReference.orderByChild("uid").equalTo(hisUid);

        //Nhận tên và hình ảnh bạn chat
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Kiểm tra cho đến khi nhận được thông tin từ firebase
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    //Nhận tên, uri hình ảnh bạn chat
                    String name   = "" + ds.child("name").getValue();
                    hisImage      = "" + ds.child("image").getValue();

                    //get value of typing
                    String typing = "" + ds.child("typingTo").getValue();
                    if(typing.equals(myUid)){
                        typingVisualizer.setColor(Color.BLUE);
                        typingLinearLayout.setVisibility(View.VISIBLE);
                    }else{
                        typingLinearLayout.setVisibility(View.GONE);
                    }

                    //get value of online status
                    String onlineStatus = ""+ds.child("onlineStatus").getValue();
                    if(onlineStatus.equals("online")) {
                        btnVideoCall.setImageResource(R.drawable.ic_video_call);
                    }else if(onlineStatus.equals("Video calling")){
                        txtStatus.setText("Calling to you");
                        btnVideoCall.setImageResource(R.drawable.ic_video_call_on);
                    } else {
                        btnVideoCall.setImageResource(R.drawable.ic_video_call);
                        //format time
                        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                        calendar.setTimeInMillis(Long.parseLong(onlineStatus));
                        String dateTime =
                                DateFormat.format("yyyy/MM/dd hh:mm aa",calendar).toString();
                        txtStatus.setText("Last seen at: "+dateTime);
                    }
                    //Set data
                    txtName.setText(name);
                    try{
                        Picasso.get()
                                .load(hisImage)
                                .placeholder(R.drawable.ic_user_anonymous)
                                .into(imgProfile);
                    }catch (Exception e){
                        Picasso.get()
                                .load(R.drawable.ic_user_anonymous)
                                .into(imgProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                notify = true;
                //Nhận nội dung từ edit text
                String message = edtMessage.getText().toString().trim();

                if(TextUtils.isEmpty(message)){
                    //Handle text is empty
                }else {
                    sendMessage(message);
                }
                //Reset edit text
                edtMessage.setText("");
            }
        });

//        btnSend.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if(!isVoice) return false;
//                switch(event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        try {
//                            mediaRecorder.prepare();
//                            mediaRecorder.start();
//                        } catch (IllegalStateException ise) {
//                            // make something ...
//                        } catch (IOException ioe) {
//                            // make something ...
//                        }
//                        Toast.makeText(getApplicationContext(), "Recording...", Toast.LENGTH_LONG).show();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        mediaRecorder.stop();
//                        mediaRecorder.release();
//                        mediaRecorder = null;
//                        break;
//                }
//                sendVoiceMessage();
//                return false;
//            }
//        });

        btnSendImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] actions = {"Audio", "Picture", "Location", "Video"};

                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Option");
                builder.setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user clicked on colors[which]
                        switch (which){
                            case 0:
                                break;
                            case 1:
                                Intent galleryIntent = new Intent();
                                galleryIntent.setType("image/*");
                                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                        }
                    }
                });
                builder.show();

            }
        });

        btnVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOnlineStatus("Video calling");
                setupToCallVideo();

            }
        });

        //check edit text change listener
        edtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().equals("")){
                    isVoice = true;
                    btnSend.setImageResource(R.drawable.ic_voice);
                }else{
                    isVoice = false;
                    btnSend.setImageResource(R.drawable.ic_send);
                }
                if(s.toString().trim().length() == 0){
                    checkTypingStatus("noOne");
                }else{
                    //uid of receiver
                    checkTypingStatus(hisUid);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        readMessages();

        seenMessages();
    }


    /**
     * Hàm lắng nghe cuộc hội thoại giữa 2 người, cho biết bên nào đã xem
     */
    private void seenMessages() {
        referenceForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        valueEventListener = referenceForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    Chat chat = ds.getValue(Chat.class);
                    //Kiểm tra xem đoạn chat cuối, người nhận là chính bạn và người gửi là bạn của
                    //bạn thì bạn của bạn đã xem tin nhắn
                    if(chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)){
                        HashMap<String,Object> hashMap = new HashMap<>();
                        hashMap.put("isSeen",true);
                        ds.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    /**
     * Hàm lấy nội dung đoạn chat trên database của firebase sau khi activity chat được gọi
     * và lắng nghe cuộc hội thoại giữa 2 người, khi có sự thay đổi, dữ liệu trên firebase sẽ được
     * cập nhật
     */
    private void readMessages() {
        chatList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Chat chat = ds.getValue(Chat.class);
                    //Kiểm tra cho đến khi nhận được nội dung đoạn chat giữa người dùng và bạn chat
                    if((chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid))
                    ||(chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid))){
                        chatList.add(chat);
                    }
                }
                adapterChat = new AdapterChat(ChatActivity.this,chatList,hisImage);
                adapterChat.notifyDataSetChanged();
                recyclerView.setAdapter(adapterChat);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * @param message , nội dung cần gửi
     *                Hàm gửi nội dung tin nhắn đến bạn chat
     */
    private void sendMessage(final String message) {
        /* "Chats" node will be created that will contains all chats
         * Whenever user send message it will create new child in "Chats" note and that will contain
         * the following key values :
         * sender: UID of sender
         * receiver: UID of receiver
         * message: content of conversation
         */

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        //Lấy thời gian hiện tại
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("sender",myUid);
        hashMap.put("receiver",hisUid);
        hashMap.put("message",message);
        hashMap.put("timestamp",timestamp);
        hashMap.put("isSeen",false);
        hashMap.put("type","text");

        //Tạo node Chats và set dữ liệu
        reference.child("Chats").push().setValue(hashMap);


        String msg = message;
        DatabaseReference databaseReference =
                FirebaseDatabase.getInstance().getReference("User").child(myUid);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User  user = dataSnapshot.getValue(User.class);
                if(notify){
                    sendNotifications(hisUid,user.getName(),message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendVoiceMessage() {
        final String mCurrentUserId = mAuth.getCurrentUser().getUid();
        final String nameRandom = myUid + hisUid + SystemClock.currentThreadTimeMillis() + ".3gp";
        final StorageReference filepath = firebaseStorage.child("Message_Audio").child(nameRandom);

        filepath.putFile(Uri.parse(outputFileRecord)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()){
                    filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String timestamp = String.valueOf(System.currentTimeMillis());

                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                            Map messageMap = new HashMap();
                            messageMap.put("sender", mCurrentUserId);
                            messageMap.put("receiver", hisUid);
                            messageMap.put("message", uri.toString());
                            messageMap.put("timestamp", timestamp);
                            messageMap.put("isSeen", false);
                            messageMap.put("type", "audio");

                            reference.child("Chats").push().setValue(messageMap);

                            DatabaseReference dataRef =
                                    FirebaseDatabase.getInstance().getReference("User").child(myUid);
                            dataRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    User user = dataSnapshot.getValue(User.class);
                                    if (notify) {
                                        sendNotifications(hisUid, user.getName(), "You have an audio message");
                                    }
                                    notify = false;
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Toast.makeText(ChatActivity.this, "Error : " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void sendNotifications(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds:dataSnapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid,
                            name+":"+message,
                            "New Message",
                            hisUid,
                            R.drawable.ic_user_anonymous);
                    Sender sender = new Sender(data,token.getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<Response>() {
                                @Override
                                public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                                    Toast.makeText(ChatActivity.this,
                                            response.message(),
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Call<Response> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Hàm kiểm tra tài khoản người dùng đang được sử dụng hay là đăng xuất
     */
    private void checkUserStatus(){
        //Nhận user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            //user đã đăng nhập
            myUid = user.getUid();
        }else {
            //User chưa đăng nhập, quay về main activity
            startActivity(new Intent(ChatActivity.this, MainActivity.class));
            finish();
        }
    }

    /**
     * @param status , trạng thái của người dùng đang on hay off
     *               Hàm thay đổi trạng thái của người dùng hiện tại
     */
    private void checkOnlineStatus(String status){
        DatabaseReference reference =
                FirebaseDatabase.getInstance().getReference("User").child(myUid);
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus",status);

        //Cập nhật giá trị vào trong uid của current user(
        reference.updateChildren(hashMap);
    }

    /**
     * @param typing , trạng thái của người dùng đang gõ tin nhắn
     *               Hàm thay đổi trạng thái của người dùng đang gõ tin nhắn
     */
    private void checkTypingStatus(String typing){
        DatabaseReference reference =
                FirebaseDatabase.getInstance().getReference("User").child(myUid);
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("typingTo",typing);

        //Cập nhật giá trị vào trong uid của current user(
        reference.updateChildren(hashMap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

    }

    private void setupToCallVideo() {

        // Tìm phòng
        FirebaseDatabase.getInstance()
                .getReference("Rooms")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean findRoomSuccess = false;
                        String timestamp = String.valueOf(System.currentTimeMillis());
                        for(DataSnapshot ds:dataSnapshot.getChildren()){
                            Room room = ds.getValue(Room.class);
                            if(room.getUser1().equals(myUid) ||
                               room.getUser2().equals(myUid)){
                                //Đã có room

                                Intent intent = new Intent(ChatActivity.this,VideoCallActivity.class);
                                intent.putExtra("hisUid",hisUid);
                                intent.putExtra("myUid",myUid);
                                intent.putExtra("room",room.getRoomId());
                                startActivityForResult(intent,REQUEST_VIDEO_CALL);
                                isCalling = true;
                                findRoomSuccess = true;
                                break;
                            }
                        }
                        if(!findRoomSuccess){
                            Room room = new Room(timestamp,myUid,hisUid);
                            FirebaseDatabase.getInstance()
                                    .getReference("Rooms")
                                    .child(timestamp).setValue(room);
                            Intent intent = new Intent(ChatActivity.this,VideoCallActivity.class);
                            intent.putExtra("hisUid",hisUid);
                            intent.putExtra("myUid",myUid);
                            intent.putExtra("room",timestamp);
                            isCalling = true;
                            startActivityForResult(intent,REQUEST_VIDEO_CALL);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ChatActivity.this,"Not call",Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_VIDEO_CALL){
            // Cuộc gọi kết thúc
            FirebaseDatabase.getInstance()
                    .getReference("Rooms")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot ds:dataSnapshot.getChildren()){
                                Room room = ds.getValue(Room.class);
                                if(room.getUser1().equals(myUid)){
                                    ds.getRef().removeValue();
                                    isCalling = false;
                                    checkOnlineStatus("online");
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

        }else if(resultCode == RESULT_OK && requestCode == GALLERY_PICK){

            final String mCurrentUserId = mAuth.getCurrentUser().getUid();

            Uri imageUri = null;
            if (data != null) {
                imageUri = data.getData();

                final String nameRandom = myUid + hisUid + SystemClock.currentThreadTimeMillis() + ".jpg";

                final StorageReference filepath = firebaseStorage.child("Message_Images").child(nameRandom);
                filepath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String timestamp = String.valueOf(System.currentTimeMillis());

                                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

                                    Map messageMap = new HashMap();
                                    messageMap.put("sender", mCurrentUserId);
                                    messageMap.put("receiver", hisUid);
                                    messageMap.put("message", uri.toString());
                                    messageMap.put("timestamp", timestamp);
                                    messageMap.put("isSeen", false);
                                    messageMap.put("type", "image");

                                    reference.child("Chats").push().setValue(messageMap);

                                DatabaseReference dataRef =
                                        FirebaseDatabase.getInstance().getReference("User").child(myUid);
                                dataRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        User user = dataSnapshot.getValue(User.class);
                                        if (notify) {
                                            sendNotifications(hisUid, user.getName(), "You have a image message");
                                        }
                                        notify = false;
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    Toast.makeText(ChatActivity.this, "Error : " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });

            }else {
                Toast.makeText(ChatActivity.this, "Can't take image !", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
