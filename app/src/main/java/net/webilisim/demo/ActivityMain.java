package net.webilisim.demo;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import net.webilisim.webplayer.WEBPlayer;
import net.webilisim.webplayer.WEBPlayerStd;

public class ActivityMain extends AppCompatActivity {

    WEBPlayerStd webPlayerStd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webPlayerStd = findViewById(R.id.webplayer_standart);
        webPlayerStd.setUp("https://www.haberturk.com/dynamics/vdo/1577084595.m3u8"
                , "Video Title Goes Here");
        Glide.with(this).load("https://mo.ciner.com.tr/video/2019/12/20/ver1576838905/1576838691_360x203.jpg").into(webPlayerStd.thumbImageView);

    }

    @Override
    protected void onPause() {
        super.onPause();
        WEBPlayer.releaseAllVideos();
    }

    @Override
    public void onBackPressed() {
        if (WEBPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }

}
