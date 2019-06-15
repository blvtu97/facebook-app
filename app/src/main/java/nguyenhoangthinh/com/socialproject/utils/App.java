package nguyenhoangthinh.com.socialproject.utils;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

public class App extends Application {

    //constants
    public static final String VIEW_TYPE = "VIEW_TYPE";

    public static final String VIEW_PROFILE = "VIEW_PROFILE";

    public static final String VIEW_COMMENT_POST = "VIEW_COMMENT_POST";

    public static final String COMMENT_FOR_POST = "COMMENT_FOR_POST";

    public static final String LIKE_FOR_POST = "LIKE_FOR_POST";

    public static final String DATA_CHANGE_COMMENT = "DATA_CHANGE_COMMENT";

    public static final String NO_DATA_CHANGE = "NON_DATA_CHANGE";

    public static final String DATA_CHANGE_ADD_NEW_USER = "DATA_CHANGE_ADD_NEW_USER";

    public static final String DATA_CHANGE_ADD_NEW_POST = "DATA_CHANGE_ADD_NEW_POST";

    public static final String DATA_CHANGE_FRIEND_LIST = "DATA_CHANGE_FRIEND_LIST";

    public static final String DATA_CHANGE_FOLLOW_LIST = "DATA_CHANGE_FOLLOW_LIST";

    public static final String DATA_CHANGE_NAME_PROFILE = "DATA_CHANGE_NAME_PROFILE";

    public static final String DATA_CHANGE_PHONE_PROFILE = "DATA_CHANGE_PHONE_PROFILE";

    public static final String DATA_CHANGE_PHOTO_AVATAR = "DATA_CHANGE_PHOTO_AVATAR";

    public static final String DATA_CHANGE_PHOTO_COVER = "DATA_CHANGE_PHOTO_COVER";

    public static final String DATA_CHANGE_NUM_LIKE_POST = "DATA_CHANGE_NUM_LIKE_POST";

    public static final String DATA_CHANGE_NUM_COMMENT_POST = "DATA_CHANGE_NUM_COMMENT_POST";

    public static final String DATA_CHANGE_STATUS_POST = "DATA_CHANGE_STATUS_POST";

    public static final String DATA_CHANGE_DELETE_POST = "DATA_CHANGE_DELETE_POST";

    public static final String DATA_CHANGE_MODE_POST = "DATA_CHANGE_MODE_POST";

    public static final String OBJECT_TYPE = "OBJECT_TYPE";

    private static Context context;

    @Override
    public void onCreate() {
        this.context = this;
        super.onCreate();

    }
    public static Context getContext() {
        return context;
    }

    public static int getNumCommentOfPost(String pComment) {
        if(TextUtils.isEmpty(pComment))return 0;
        String[] nums = pComment.split(",");
        return nums.length;
    }

    /**
     * @param pLike ,chuỗi chứa số lượng uid like
     * @return số like
     */
    public static int getNumLikeOfPost(String pLike) {
        if(TextUtils.isEmpty(pLike))return 0;
        String[] nums = pLike.split(",");
        return nums.length;
    }
}
