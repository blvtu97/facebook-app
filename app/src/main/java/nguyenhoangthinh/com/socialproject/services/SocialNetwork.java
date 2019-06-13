package nguyenhoangthinh.com.socialproject.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.List;

import nguyenhoangthinh.com.socialproject.models.Post;
import nguyenhoangthinh.com.socialproject.models.User;

public class SocialNetwork {

    public static boolean isDarkMode = false;

    private static Context mContext = null;

    public static SocialServices socialServices = null;

    public static Intent intentService = null;

    public static void startService(Context context, ServiceConnection serviceConnection) {
        if(mContext == null) mContext = context;
        if(intentService == null) {
            intentService = new Intent(context, SocialServices.class);
            context.startService(intentService);
        }
    }


    public static void navigateProfile(String uid){
        socialServices.senBroadcastToNavigateUid(uid);
    }

    public static void navigateCommentListOf(Post post){
        socialServices.senBroadcastToNavigateCommentOf(post);
    }

    public static void navigateSendCommentBy(Post post){
        socialServices.senBroadcastToNavigateCommentOf(post);
    }

    public static void commentForPost(String pId, String cContent){
        socialServices.senBroadcastToCommentForPost(pId,cContent);
    }

    public static void likeForPost(String pId){
        socialServices.senBroadcastToLikeForPost(pId);
    }

    public static List<User> getUserListCurrent(){
        return socialServices.getUserListCurrent();
    }

    public static List<User> getUserListCurrentExcept(String uid){
        return socialServices.getUserListCurrent();
    }

    public static User getUser(String uid){
        return socialServices.findUserById(uid);
    }

    public static void addUserForListCurrent(User user){
        socialServices.addUser(user);
    }

    public static String findNameById(String uid){
        return socialServices.findName(uid);
    }

    public static String findImageById(String uid){
        return socialServices.findImage(uid);
    }

    public static void clearUserListCurrent(){
        socialServices.clearUserList();
    }

    public static List<Post> getPostListCurrent(){
        return socialServices.getPostListCurrent();
    }

    public static boolean isReceiveDataSuccessfully(){
        return  socialServices.isReceiveDataSuccessfully();
    }
}
