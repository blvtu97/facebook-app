package nguyenhoangthinh.com.socialproject.services;

public interface SocialStateListener {
    void onMetaChanged();

    void onNavigate(String type, String idType);

    void onDarkMode(boolean change);
}
