package nguyenhoangthinh.com.socialproject.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import nguyenhoangthinh.com.socialproject.activity.DashboardActivity;
import nguyenhoangthinh.com.socialproject.activity.MainActivity;
import nguyenhoangthinh.com.socialproject.R;
import nguyenhoangthinh.com.socialproject.adapters.AdapterPost;
import nguyenhoangthinh.com.socialproject.models.Post;
import nguyenhoangthinh.com.socialproject.services.SocialStateListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements SocialStateListener {

    //fire base
    private FirebaseAuth mAuth;

    private FirebaseUser mUser;

    private DatabaseReference mReference;

    private RecyclerView recyclerViewPosts;

    private List<Post> postList;

    private AdapterPost adapterPost;

    public HomeFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ((DashboardActivity) getActivity()).setSocialStateListener(this);

        // Init fire base
        mAuth      = FirebaseAuth.getInstance();
        mReference = FirebaseDatabase.getInstance().getReference("User");
        mUser      = mAuth.getCurrentUser();

        // Init views
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts);
        recyclerViewPosts.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewPosts.setLayoutManager(layoutManager);

        // Init post list
        postList = new ArrayList<>();
        loadPosts();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapterPost != null){
            recyclerViewPosts.setAdapter(adapterPost);
        }
    }

    private void loadPosts() {
        // Đường dẫn tới tất cả các post
       FirebaseDatabase.getInstance()
               .getReference("Posts")
               .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                // Load tất cả bài viết từ fire base, đồng thời lắng nghe sự thay đổi
                // để cập nhật lại post
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Post post = ds.getValue(Post.class);
                    newsFeedAlgorithm(post);
                }

                priorityNewsFeed();

                // Thêm một mới post ảo để set adapter thành nơi cho người dùng đăng post
                Post p = new Post();
                p.setUid(mUser.getUid());
                postList.add(p);

                if (getActivity() == null) return;
                if(adapterPost == null) {
                    adapterPost = new AdapterPost(getActivity(), postList);
                    recyclerViewPosts.setAdapter(adapterPost);
                }else {
                    adapterPost.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    /**
     * Hàm sắp xếp bài viết
     */
    private void priorityNewsFeed() {

        //Bài viết xuất hiện mới nhất

        //Bài viết của bạn bè thân nhất

        //Dự đoán thông tin

    }

    private void newsFeedAlgorithm(final Post post) {

        // Tìm kiếm bài viết ở chế độ công khai
        if (!post.getpMode().equals("Only me")) {
            // Tìm kiếm người đăng bài post này, kiểm tra xem trong danh sách friends của người
            // đăng bài post này có chứa uid của dùng đăng nhập hiện tại hay không
            mReference.orderByChild("uid")
                      .equalTo(post.getUid())
                      .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                String friendList = ds.child("friends").getValue().toString();
                                //Danh sách có thể chứa người dùng
                                if (friendList.contains(mUser.getUid())) {

                                    //Kiểm tra đó là bạn hay là lời yêu cầu kết bạn của người
                                    //dùng đăng nhập hiện tại

                                    //Nếu là bạn bè, thêm post đó vào news feed
                                    if (!isRequestAddFriend(friendList)) {
                                        postList.add(post);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }

        // Tìm kiếm bài viết do người dùng đăng
        if (post.getUid().equals(mUser.getUid())) {
            postList.add(post);
        }


    }

    private boolean isRequestAddFriend(String friends) {
        String[] friendList = friends.split(",");
        for (int i = 0; i < friendList.length; i++) {
            if (friendList[i].contains(mUser.getUid())) {
                if (friendList[i].contains("@")) return true;
            }
        }
        return false;
    }

    private void searchPosts(final String searchQuery) {
        //đường dẫn tới tất cả các post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                //Load tất cả bài viết từ fire base, đồng thời lắng nghe sự thay đổi
                //để cập nhật lại
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Post post = ds.getValue(Post.class);
                    //Tìm kiếm post theo status
                    if (post.getpStatus().toLowerCase().contains(searchQuery.toLowerCase())) {
                        postList.add(post);
                    }
                }
                adapterPost = new AdapterPost(getActivity(), postList);
                adapterPost.notifyDataSetChanged();
                recyclerViewPosts.setAdapter(adapterPost);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(),
                        databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);//Để hiện menu
        super.onCreate(savedInstanceState);
    }

    /**
     * Hàm tạo option menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.menuSearch);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //gọi khi người dùng nhấn tìm kiếm
                if (!TextUtils.isEmpty(s)) {
                    searchPosts(s);
                } else {
                    loadPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //gọi khi có sự thay đổi kí tự
                //gọi khi người dùng nhấn tìm kiếm
                if (!TextUtils.isEmpty(s)) {
                    searchPosts(s);
                } else {
                    loadPosts();
                }
                return false;
            }
        });
    }


    /**
     * Bắt sự kiện cho menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuLogout) {
            logoutUser();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Hàm logout user, quay về main activity
     */
    private void logoutUser() {
        mAuth.signOut();
        checkUserStatus();
    }

    /**
     * Hàm kiểm tra tài khoản người dùng đang được sử dụng hay là đăng xuất
     */
    private void checkUserStatus() {
        //Nhận user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            //user đã đăng nhập
        } else {
            //User chưa đăng nhập, quay về main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onMetaChanged() {

    }

    @Override
    public void onNavigate(String type, String idType) {

    }

    @Override
    public void onDarkMode(boolean change) {
        if(change){
            recyclerViewPosts.setBackgroundColor(Color.BLACK);
            adapterPost.changeDarkMode();
        }
    }

}
