package nguyenhoangthinh.com.socialproject.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nguyenhoangthinh.com.socialproject.R;
import nguyenhoangthinh.com.socialproject.adapters.AdapterComment;
import nguyenhoangthinh.com.socialproject.adapters.ViewPagerAdapter;
import nguyenhoangthinh.com.socialproject.fragments.NotificationFragment;
import nguyenhoangthinh.com.socialproject.fragments.HomeFragment;
import nguyenhoangthinh.com.socialproject.fragments.ProfileFragment;
import nguyenhoangthinh.com.socialproject.fragments.UsersFragment;
import nguyenhoangthinh.com.socialproject.models.Comment;
import nguyenhoangthinh.com.socialproject.notifications.Token;
import nguyenhoangthinh.com.socialproject.services.SocialNetwork;
import nguyenhoangthinh.com.socialproject.services.SocialServices;
import nguyenhoangthinh.com.socialproject.services.SocialStateListener;

public class DashboardActivity extends AppCompatActivity
        implements SocialStateListener{

    // Nhóm firebase
    private FirebaseAuth mAuth;

    private FirebaseUser mUser;

    private DatabaseReference referencePost;

    private String mUID;

    //
    private BottomSheetBehavior bottomSheetBehavior;

    private RecyclerView recyclerViewComments;

    private AdapterComment adapterComment;

    private List<Comment> commentList;

    private TabLayout tabLayout;

    private ViewPager viewPager;

    private ViewPagerAdapter viewPagerAdapter;

    private SwitchCompat switchCompat;

    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard2);
        initializeUI();
        registerBroadcast();
    }


    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }

    /**
     * Hàm ánh xạ vắt bắt sự kiện cho views
     */
    private void initializeUI() {
        toolbar = findViewById(R.id.toolbarDashboard);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        tabLayout    = findViewById(R.id.tabLayoutOptions);
        viewPager    = findViewById(R.id.viewPager);
        switchCompat = findViewById(R.id.switchCompat);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    onDarkMode(true);
                }else{
                    onDarkMode(false);
                }
            }
        });

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        referencePost = FirebaseDatabase.getInstance().getReference("Comments");

        commentList = new ArrayList<>();
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        recyclerViewComments.setHasFixedSize(true);
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));

        bottomSheetBehavior = BottomSheetBehavior.from(recyclerViewComments);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new HomeFragment(),"");
        viewPagerAdapter.addFragment(new ProfileFragment(),"");
        viewPagerAdapter.addFragment(new UsersFragment(),"");
        viewPagerAdapter.addFragment(new NotificationFragment(),"");
        viewPager.setAdapter(viewPagerAdapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_tab_newsfeed);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_tab_user);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_tab_friends);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_bell);


        checkUserStatus();
        //update token
        updateToken(FirebaseInstanceId.getInstance().getToken());
    }

    public void updateToken(String token){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        reference.child(mUID).setValue(mToken);
    }

    /**
     * Hàm kiểm tra tài khoản người dùng đang được sử dụng hay là đăng xuất
     */
    private void checkUserStatus(){
        //Nhận user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            //user đã đăng nhập
            mUID = user.getUid();
            //lưu lại id của người dùng đăng nhập vào shared preferences
            SharedPreferences sp = getSharedPreferences("SP_USER",MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID",mUID);
            editor.apply();
        }else {
            //User chưa đăng nhập, quay về main activity
            startActivity(new Intent(DashboardActivity.this,MainActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void navigateProfile(String uid) {
        onNavigate(SocialServices.VIEW_PROFILE,uid);
        viewPager.setCurrentItem(1);

    }


    //------------------------------------------SERVICES--------------------------------------//


    private List<SocialStateListener> socialStateListeners = new ArrayList<>();

    private BroadcastListener broadcastListener;

    @Override
    public void onMetaChanged() {
        for (final SocialStateListener listener : socialStateListeners) {
            if (listener != null) {
                listener.onMetaChanged();
            }
        }
    }

    @Override
    public void onNavigate(String type, String idType) {
        for (final SocialStateListener listener : socialStateListeners) {
            if (listener != null) {
                listener.onNavigate(type,idType);
            }
        }
    }

    @Override
    public void onDarkMode(boolean change) {
        if(change) {
            SocialNetwork.isDarkMode = true;
            changeViewToDarkMode();
        }else{
            SocialNetwork.isDarkMode = false;
            changeViewToLightMode();
        }
        for (final SocialStateListener listener : socialStateListeners) {
            if (listener != null) {
                listener.onDarkMode(change);
            }
        }
    }

    private void changeViewToLightMode() {
        toolbar.setBackgroundColor(R.drawable.custom_background_profile2);
        tabLayout.setBackgroundColor(Color.WHITE);
        viewPager.setBackgroundColor(Color.WHITE);
    }

    private void changeViewToDarkMode() {
        recyclerViewComments.setBackgroundColor(R.drawable.custom_background_dark_mode_main);
        toolbar.setBackgroundColor(R.drawable.custom_background_dark_mode_main);
        tabLayout.setBackgroundColor(R.drawable.custom_background_dark_mode_main);
        viewPager.setBackgroundColor(R.drawable.custom_background_dark_mode_main);
    }

    public void setSocialStateListener(SocialStateListener stateListener){
        if(stateListener == this) return;
        if(stateListener != null){
            socialStateListeners.add(stateListener);
        }
    }

    /**
     * Phương thức đăng kí nhận thông báo từ dịch vụ để xử lý
     */
    private void registerBroadcast(){
        broadcastListener = new BroadcastListener();
        IntentFilter intentFilter = new IntentFilter("metaChanged.Broadcast");
        registerReceiver(broadcastListener,intentFilter);
    }

    /**
     * Lớp broadcast để đăng kí xử lý thông báo từ dịch vụ
     */
    public class BroadcastListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(SocialServices.VIEW_TYPE);
            if(type.equals(SocialServices.VIEW_PROFILE)) {
                String uid = intent.getStringExtra("uid");
                navigateProfile(uid);
            }else  if(type.equals(SocialServices.VIEW_COMMENT_POST)){
                String uid = intent.getStringExtra("uid");
                String pId = intent.getStringExtra("pId");
                navigateComment(uid,pId);
            }else if(type.equals(SocialServices.COMMENT_FOR_POST)){
                String pId = intent.getStringExtra("pId");
                String cContent = intent.getStringExtra("cContent");
                commentForPostByUser(pId,cContent);
            }else  if(type.equals(SocialServices.LIKE_FOR_POST)){
                String pId = intent.getStringExtra("pId");
                likeForPostByUser(pId);
            }else if(type.equals(SocialServices.DATA_CHANGE)){
                onMetaChanged();
            }
        }
    }

    /**
     * @param pId ,ID của bài viết
     *            Hàm thêm người dùng vào danh sách like của bài viết
     */
    private void likeForPostByUser(String pId) {

     FirebaseDatabase.getInstance()
             .getReference("Posts")
             .orderByChild("pId").equalTo(pId)
             .addListenerForSingleValueEvent(new ValueEventListener() {
                 @Override
                 public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                     for(DataSnapshot ds:dataSnapshot.getChildren()){
                         String pLike = ds.child("pLike").getValue().toString();
                         //Người dùng đã like, ta unlike
                         if(pLike.contains(mUser.getUid())){
                             pLike = pLike.replace(mUser.getUid()+",","");
                         }else{
                             pLike +=mUser.getUid()+",";
                         }
                         HashMap<String,Object> hashMap = new HashMap<>();
                         hashMap.put("pLike",pLike);
                         ds.getRef().updateChildren(hashMap);
                     }
                 }

                 @Override
                 public void onCancelled(@NonNull DatabaseError databaseError) {

                 }
             });
    }

    /**
     * @param pId ,id của bài viết
     *
     */
    private void commentForPostByUser(final String pId, final String cContent) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("cContent", cContent);
        hashMap.put("pId", pId);
        hashMap.put("uid", mUser.getUid());
        hashMap.put("cTime", timestamp);

        //Lưu vào đường dẫn có tên là comment;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Comments");

        //Tạo đường dẫn pId, đặt dữ liệu vào database
        ref.child(timestamp).setValue(hashMap);


        Toast.makeText(this, "New Comment...", Toast.LENGTH_SHORT).show();

        //Thêm người comment vào danh sách comment
        FirebaseDatabase.getInstance().getReference("Posts")
                .orderByChild("pId").equalTo(pId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            String pComment = ds.child("pComment").getValue().toString();
                            pComment += mUser.getUid() + ",";
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("pComment", pComment);
                            ds.getRef().updateChildren(hashMap);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    /**
     * @param uid
     * @param pId,
     *            Hàm xem comment của bài viết
     */
    private void navigateComment(String uid, String pId) {
        loadComments(uid,pId);
    }

    private void loadComments(String uid, final String pId) {
        referencePost.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    Comment comment = ds.getValue(Comment.class);

                    // Comment này chứa pId và uid của người dùng hiện tại thì thêm vào commentList
                    if(comment.getpId().equals(pId)){
                        commentList.add(comment);
                    }
                }

                // Thêm một comment ảo để làm nơi comment row của người dùng
                Comment comment = new Comment("","","","");
                commentList.add(comment);
                adapterComment = new AdapterComment(DashboardActivity.this,commentList);
                adapterComment.setpId(pId);
                adapterComment.setCommentList(commentList);
                recyclerViewComments.setAdapter(adapterComment);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
