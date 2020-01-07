# WEBPlayer
[![license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/webilisim/WEBPlayer/master/LICENSE)
[![platform](https://img.shields.io/badge/platform-Android-yellow.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-16%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=16)
[ ![Download](https://api.bintray.com/packages/netwebilisim/WEBPlayer/net.webilisim.webplayer/images/download.svg?version=1.0.11) ](https://bintray.com/netwebilisim/WEBPlayer/net.webilisim.webplayer/1.0.11/link)

 Android video player supports mp4, m3u8, 3gp and more format.
 
 ## QuickStart

1. Implementation
```gradle
implementation 'net.webilisim.webplayer:webplayer:1.0.11'
```

2. Set player in your XML
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="200dp">
    <net.webilisim.webplayer.WEBPlayerStd
        android:id="@+id/webplayer"
        android:layout_width="match_parent"
        android:layout_height="200dp" />
</LinearLayout>
```

3. Set video to webplayer
```java
WEBPlayerStd webPlayerStd = findViewById(R.id.webplayer);
webPlayerStd.setUp("YOUR VIDEO URL", "YOUR VIDEO TITLE");
webPlayerStd.thumbImageView.setImage("YOUR VIDEO THUMB IMAGE");
```

4. Some activity tools
```java
@Override
public void onBackPressed() {
    if (WEBPlayer.backPress()) {
        return;
    }
    super.onBackPressed();
}
@Override
protected void onPause() {
    super.onPause();
    WEBPlayer.releaseAllVideos();
}
```

5. Manifest Settings
```
<activity
    android:name=".MainActivity"
    android:configChanges="orientation|screenSize|keyboardHidden"
    android:screenOrientation="portrait" /> <!-- or android:screenOrientation="landscape"-->
```

6.`proguard-rules.pro` settings
```
-keep public class net.webilisim.webplayer.WEBPlayerMediaSystem {*; }
```
