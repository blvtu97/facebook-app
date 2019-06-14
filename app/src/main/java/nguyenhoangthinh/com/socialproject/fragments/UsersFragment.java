package nguyenhoangthinh.com.socialproject.fragments;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import nguyenhoangthinh.com.socialproject.adapters.AdapterUser;
import nguyenhoangthinh.com.socialproject.models.User;
import nguyenhoangthinh.com.socialproject.services.SocialNetwork;
import nguyenhoangthinh.com.socialproject.services.SocialStateListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class UsersFragment extends Fragment implements  SocialStateListener {
    private FirebaseAuth mAuth;

    private RecyclerView recyclerViewUsers;

    private AdapterUser adapterUser;

    private List<User> userList;


    public UsersFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        ((DashboardActivity)getActivity()).setSocialStateListener(this);

        mAuth = FirebaseAuth.getInstance();

        //Initialize Recyclerview
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewUsers.setLayoutManager(layoutManager);
        recyclerViewUsers.setHasFixedSize(true);

        //initialize user list
        userList = new ArrayList<>();

        // Optimize code
        if(SocialNetwork.isReceiveDataSuccessfully()){
            getAllUsers2();
        }
        return view;
    }

    private void getAllUsers2() {
        userList.clear();
        for(User us: SocialNetwork.getUserListCurrent()){
            if(!us.getUid().equals(mAuth.getCurrentUser().getUid())){
                userList.add(us);
            }
        }
        adapterUser = new AdapterUser(getActivity(),userList);
        recyclerViewUsers.setAdapter(adapterUser);
    }

    /**
     * Hàm nhận danh sách user tồn tại trên firebase
     */
    private void getAllUsers() {
        final FirebaseUser userCurrent = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference databaseReference
                = FirebaseDatabase.getInstance().getReference("User");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    User user = ds.getValue(User.class);
                    //Get all users except user current
                    if(!user.getUid().equals(userCurrent.getUid())){
                        userList.add(user);
                        SocialNetwork.addUserForListCurrent(user);
                    }
                }
                if(getActivity() == null) return;

                if(userList.size()>0){
                    recyclerViewUsers.smoothScrollToPosition(userList.size() - 1);
                }
                adapterUser = new AdapterUser(getActivity(),userList);
                recyclerViewUsers.setAdapter(adapterUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void searchUsers(final String query) {
        final FirebaseUser userCurrent = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference databaseReference
                = FirebaseDatabase.getInstance().getReference("User");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    User user = ds.getValue(User.class);
                    //Get all users except user current
                    if(!user.getUid().equals(userCurrent.getUid())){
                        //Tìm kiếm userName/email chứa đoạn text
                        if(user.getName().toLowerCase().contains(query.toLowerCase())
                           || user.getEmail().toLowerCase().contains(query.toLowerCase())){
                            userList.add(user);
                        }
                    }
                }

                if(adapterUser == null) {
                    adapterUser = new AdapterUser(getActivity(), userList);
                    recyclerViewUsers.setAdapter(adapterUser);
                }else{
                    adapterUser.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);//Để hiện menu
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapterUser != null){
            recyclerViewUsers.setAdapter(adapterUser);
        }
        if(SocialNetwork.isDarkMode){
            setDarkMode();
        }
    }

    /**
     * Hàm tạo option menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search,menu);
        //SearchView
        MenuItem item = menu.findItem(R.id.menuSearch);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //Search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //Gọi khi user nhấn search button từ bàn phím
                //Kiểm tra truy vấn khác rỗng sau đó search
                if(!TextUtils.isEmpty(s)){
                    //Tìm kiếm nội dung theo đoạn text
                    searchUsers(s);
                }else{
                    //Text rỗng, nhận tất cả user
                    getAllUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //Gọi bất cứ khi nào người dùng nhấn bất kỳ kí tự nào

                //Gọi khi user nhấn search button từ bàn phím
                //Kiểm tra truy vấn khác rỗng sau đó search
                if(!TextUtils.isEmpty(s)){
                    //Tìm kiếm nội dung theo đoạn text
                    searchUsers(s);
                }else{
                    //Text rỗng, nhận tất cả user
                    getAllUsers();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu,inflater);
    }


    /**
     * Bắt sự kiện cho menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menuLogout){
            logoutUser();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Hàm logout user, quay về main activity
     */
    private void logoutUser(){
        mAuth.signOut();
        checkUserStatus();
    }

    /**
     * Hàm kiểm tra tài khoản người dùng đang được sử dụng hay là đăng xuất
     */
    private void checkUserStatus(){
        //Nhận user hiện tại
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            //user đã đăng nhập
        }else {
            //User chưa đăng nhập, quay về main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
           getActivity().finish();
        }
    }

    @Override
    public void onMetaChanged() {
        getAllUsers2();
    }

    @Override
    public void onNavigate(String type, String idType) {

    }

    @Override
    public void onDarkMode(boolean change) {
        if(change){
            setDarkMode();
        }
    }

    private void setDarkMode(){
        recyclerViewUsers.setBackgroundResource(R.drawable.custom_background_dark_mode_main);
        adapterUser.changeDarkMode();
    }

}
