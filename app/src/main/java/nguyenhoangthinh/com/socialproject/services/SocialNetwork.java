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

    private static ServiceBinder serviceBinder = null;

    public static SocialServices socialServices = null;

    private static Intent intentService = null;

    public static void bindToService(Context context, ServiceConnection serviceConnection) {
        if(mContext == null) mContext = context;

        if(intentService == null) {

            intentService = new Intent(context, SocialServices.class);
            context.startService(intentService);

            serviceBinder = new ServiceBinder(context, serviceConnection);
            context.bindService(intentService, serviceBinder, Context.BIND_AUTO_CREATE);

        }
    }

    //Kế thừa ServiceConnection
    public static class ServiceBinder implements ServiceConnection {

        private ServiceConnection mCallback;

        private Context mContext;

        public ServiceBinder(Context context, ServiceConnection callback) {
            mCallback = callback;
            mContext = context;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            socialServices =((SocialServices.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {

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
        return socialServices.findName(uid);
    }

    public static void clearUserListCurrent(){
        socialServices.clearUserList();
    }
}
