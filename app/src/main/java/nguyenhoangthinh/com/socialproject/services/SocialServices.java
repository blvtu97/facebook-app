package nguyenhoangthinh.com.socialproject.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import nguyenhoangthinh.com.socialproject.models.Comment;
import nguyenhoangthinh.com.socialproject.models.Post;
import nguyenhoangthinh.com.socialproject.models.User;

public class SocialServices extends Service {

    //constants
    public static final String VIEW_TYPE = "VIEW_TYPE";

    public static final String VIEW_PROFILE = "VIEW_PROFILE";

    public static final String VIEW_COMMENT_POST = "VIEW_COMMENT_POST";

    public static final String COMMENT_FOR_POST = "COMMENT_FOR_POST";

    public static final String LIKE_FOR_POST = "LIKE_FOR_POST";

    public static final String DATA_CHANGE = "DATA_CHANGE";
    // Nhóm fire base
    private FirebaseUser mUser;

    private final List<User> userListCurrent = new ArrayList<>();

    private final List<Post> postListCurrent = new ArrayList<>();

    private final List<Comment> commentListCurrent = new ArrayList<>();
    //

    private boolean isHaveUsers = false;

    private boolean isHavePosts = false;

    private boolean isHaveComments = false;

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public class LocalBinder extends Binder {
        public SocialServices getService() {
            return SocialServices.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getDatabaseFromFirebase();
        return super.onStartCommand(intent, flags, startId);
    }

    private void getDatabaseFromFirebase() {
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        getAllUsers();
        getAllComments();
        getAllPosts();
    }

    public void senBroadcastToNavigateUid(String uid) {
        Intent intent = new Intent();
        intent.putExtra(VIEW_TYPE, VIEW_PROFILE);
        intent.putExtra("uid", uid);
        intent.setAction("metaChanged.Broadcast");
        sendBroadcast(intent);
    }

    public void senBroadcastToNavigateCommentOf(Post post) {
        Intent intent = new Intent();
        intent.putExtra(VIEW_TYPE, VIEW_COMMENT_POST);
        intent.putExtra("uid", post.getUid());
        intent.putExtra("pId", post.getpId());
        intent.setAction("metaChanged.Broadcast");
        sendBroadcast(intent);
    }

    public void senBroadcastToCommentForPost(String pId, String cContent) {
        Intent intent = new Intent();
        intent.putExtra(VIEW_TYPE, COMMENT_FOR_POST);
        intent.putExtra("pId", pId);
        intent.putExtra("cContent", cContent);
        intent.setAction("metaChanged.Broadcast");
        sendBroadcast(intent);
    }

    public void senBroadcastToLikeForPost(String pId) {
        Intent intent = new Intent();
        intent.putExtra(VIEW_TYPE, LIKE_FOR_POST);
        intent.putExtra("pId", pId);
        intent.setAction("metaChanged.Broadcast");
        sendBroadcast(intent);
    }

    public void senBroadcastToUpdateData() {
        Intent intent = new Intent();
        intent.putExtra(VIEW_TYPE, DATA_CHANGE);
        intent.setAction("metaChanged.Broadcast");
        sendBroadcast(intent);
    }

    public FirebaseUser getCurrentUser() {
        return mUser;
    }

    public boolean isReceiveDataSuccessfully() {
        return isHaveUsers && isHaveComments && isHavePosts;
    }

    private void getAllUsers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("User");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userListCurrent.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    userListCurrent.add(user);

                }
                isHaveUsers = true;
                senBroadcastToUpdateData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public List<User> getUserListCurrent() {
        return userListCurrent;
    }

    public User findUserById(String uid) {
        for (User us : userListCurrent) {
            if (us.getUid().equals(uid)) return us;
        }
        return null;
    }

    public void addUser(User user) {
        if (findUserById(user.getUid()) == null) {
            userListCurrent.add(user);
        }
    }

    public String findName(String uid) {
        for (User us : userListCurrent) {
            if (us.getUid().equals(uid)) return us.getName();
        }
        return "";
    }

    public String findImage(String uid) {
        for (User us : userListCurrent) {
            if (us.getUid().equals(uid)) return us.getImage();
        }
        return "";
    }

    public void clearUserList() {
        userListCurrent.clear();
    }

    private void getAllPosts() {
        // Đường dẫn tới tất cả các post
        FirebaseDatabase.getInstance()
                .getReference("Posts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        postListCurrent.clear();
                        // Load tất cả bài viết từ fire base, đồng thời lắng nghe sự thay đổi
                        // để cập nhật lại post
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            Post post = ds.getValue(Post.class);
                            postListCurrent.add(post);
                        }
                        isHavePosts = true;
                        senBroadcastToUpdateData();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    public List<Post> getPostListCurrent() {
        return postListCurrent;
    }

    private void getAllComments() {
        // Đường dẫn tới tất cả các post
        FirebaseDatabase.getInstance()
                .getReference("Comments")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        commentListCurrent.clear();
                        // Load tất cả bài viết từ fire base, đồng thời lắng nghe sự thay đổi
                        // để cập nhật lại post
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            Comment comment = ds.getValue(Comment.class);
                            commentListCurrent.add(comment);
                        }
                        isHaveComments = true;
                        senBroadcastToUpdateData();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    public List<Comment> getCommentListCurrent() {
        return commentListCurrent;
    }

}
