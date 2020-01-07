package net.webilisim.webplayer;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;

/**
 * Created by Webilisim.net on 24/12/2019.
 */
public abstract class WEBPlayerMInterface implements TextureView.SurfaceTextureListener {

    public static SurfaceTexture SAVED_SURFACE;
    public HandlerThread mMediaHandlerThread;
    public Handler mMediaHandler;
    public Handler handler;
    public WEBPlayer WEBPlayer;


    public WEBPlayerMInterface(WEBPlayer WEBPlayer) {
        this.WEBPlayer = WEBPlayer;
    }

    public abstract void start();

    public abstract void prepare();

    public abstract void pause();

    public abstract boolean isPlaying();

    public abstract void seekTo(long time);

    public abstract void release();

    public abstract long getCurrentPosition();

    public abstract long getDuration();

    public abstract void setVolume(float leftVolume, float rightVolume);

    public abstract void setSpeed(float speed);

    public abstract void setSurface(Surface surface);
}
