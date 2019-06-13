package nguyenhoangthinh.com.socialproject.utils;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        this.context = this;
        super.onCreate();

    }

    public static Context getContext() {
        return context;
    }
}
