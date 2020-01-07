package net.webilisim.webplayer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Webilisim.net on 24/12/2019.
 */
public abstract class WEBPlayer extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

    public static final String TAG = "WEBPLAYER";
    public static WEBPlayer currentWEBPlayer;
    public static LinkedList<ViewGroup> CONTAINER_LIST = new LinkedList<>();

    public static final int SCREEN_NORMAL = 0;
    public static final int SCREEN_FULLSCREEN = 1;
    public static final int SCREEN_TINY = 2;

    public static final int STATE_IDLE = -1;
    public static final int STATE_NORMAL = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARING_CHANGING_URL = 2;
    public static final int STATE_PREPARED = 3;
    public static final int STATE_PLAYING = 4;
    public static final int STATE_PAUSE = 5;
    public static final int STATE_AUTO_COMPLETE = 6;
    public static final int STATE_ERROR = 7;

    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER = 0;//DEFAULT
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT = 1;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP = 2;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ORIGINAL = 3;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    public static boolean SAVE_PROGRESS = false;
    public static boolean WIFI_TIP_DIALOG_SHOWED = false;
    public static int VIDEO_IMAGE_DISPLAY_TYPE = 0;
    public static long lastAutoFullscreenTime = 0;
    public static final int THRESHOLD = 80;
    public static int ON_PLAY_PAUSE_TMP_STATE = 0;//这个考虑不放到库里，去自定义

    public int state = -1;
    public int screen = -1;
    public WEBPlayerDataSource WEBPlayerDataSource;
    public int widthRatio = 0;
    public int heightRatio = 0;
    public Class mediaInterfaceClass;
    public WEBPlayerMInterface mediaInterface;
    public int positionInList = -1;//很想干掉它
    public int videoRotation = 0;
    protected long gobakFullscreenTime = 0;//这个应该重写一下，刷新列表，新增列表的刷新，不打断播放，应该是个flag

    public int seekToManulPosition = -1;
    public long seekToInAdvance = 0;

    public ImageView startButton;
    public SeekBar progressBar;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup topContainer, bottomContainer;
    public WEBPlayerTexture textureView;


    protected Timer UPDATE_PROGRESS_TIMER;
    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected ProgressTimerTask mProgressTimerTask;
    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected long mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected long mSeekTimePosition;

    public WEBPlayer(Context context) {
        super(context);
        init(context);
    }

    public WEBPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public abstract int getLayoutId();

    public void init(Context context) {
        View.inflate(context, getLayoutId(), this);
        startButton = findViewById(R.id.start);
        fullscreenButton = findViewById(R.id.fullscreen);
        progressBar = findViewById(R.id.bottom_seek_progress);
        currentTimeTextView = findViewById(R.id.current);
        totalTimeTextView = findViewById(R.id.total);
        bottomContainer = findViewById(R.id.layout_bottom);
        textureViewContainer = findViewById(R.id.surface_container);
        topContainer = findViewById(R.id.layout_top);

        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);
        textureViewContainer.setOnTouchListener(this);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;

        state = STATE_IDLE;
    }

    public void setUp(String url, String title) {
        setUp(new WEBPlayerDataSource(url, title), SCREEN_NORMAL);
    }

    public void setUp(String url, String title, int screen) {
        setUp(new WEBPlayerDataSource(url, title), screen);
    }

    public void setUp(WEBPlayerDataSource WEBPlayerDataSource, int screen) {
        setUp(WEBPlayerDataSource, screen, WEBPlayerMediaSystem.class);
    }

    public void setUp(String url, String title, int screen, Class mediaInterfaceClass) {
        setUp(new WEBPlayerDataSource(url, title), screen, mediaInterfaceClass);
    }

    public void setUp(WEBPlayerDataSource WEBPlayerDataSource, int screen, Class mediaInterfaceClass) {
        if ((System.currentTimeMillis() - gobakFullscreenTime) < 200) return;

        this.WEBPlayerDataSource = WEBPlayerDataSource;
        this.screen = screen;
        onStateNormal();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }

    public void setMediaInterface(Class mediaInterfaceClass) {
        reset();
        this.mediaInterfaceClass = mediaInterfaceClass;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            Log.i(TAG, "onClick start [" + this.hashCode() + "] ");
            if (WEBPlayerDataSource == null || WEBPlayerDataSource.urlsMap.isEmpty() || WEBPlayerDataSource.getCurrentUrl() == null) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (state == STATE_NORMAL) {
                if (!WEBPlayerDataSource.getCurrentUrl().toString().startsWith("file") && !
                        WEBPlayerDataSource.getCurrentUrl().toString().startsWith("/") &&
                        !WEBPlayerUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {//这个可以放到std中
                    showWifiDialog();
                    return;
                }
                startVideo();
            } else if (state == STATE_PLAYING) {
                Log.d(TAG, "pauseVideo [" + this.hashCode() + "] ");
                mediaInterface.pause();
                onStatePause();
            } else if (state == STATE_PAUSE) {
                mediaInterface.start();
                onStatePlaying();
            } else if (state == STATE_AUTO_COMPLETE) {
                startVideo();
            }
        } else if (i == R.id.fullscreen) {
            Log.i(TAG, "onClick fullscreen [" + this.hashCode() + "] ");
            if (state == STATE_AUTO_COMPLETE) return;
            if (screen == SCREEN_FULLSCREEN) {
                //quit fullscreen
                backPress();
            } else {
                Log.d(TAG, "toFullscreenActivity [" + this.hashCode() + "] ");
                gotoScreenFullscreen();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "onTouch surfaceContainer actionDown [" + this.hashCode() + "] ");
                    mTouchingProgressBar = true;

                    mDownX = x;
                    mDownY = y;
                    mChangeVolume = false;
                    mChangePosition = false;
                    mChangeBrightness = false;

                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i(TAG, "onTouch surfaceContainer actionMove [" + this.hashCode() + "] ");
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (screen == SCREEN_FULLSCREEN) {
                        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                cancelProgressTimer();
                                if (absDeltaX >= THRESHOLD) {
                                    if (state != STATE_ERROR) {
                                        mChangePosition = true;
                                        mGestureDownPosition = getCurrentPositionWhenPlaying();
                                    }
                                } else {
                                    if (mDownX < mScreenWidth * 0.5f) {
                                        mChangeBrightness = true;
                                        WindowManager.LayoutParams lp = WEBPlayerUtils.getWindow(getContext()).getAttributes();
                                        if (lp.screenBrightness < 0) {
                                            try {
                                                mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                                                Log.i(TAG, "current system brightness: " + mGestureDownBrightness);
                                            } catch (Settings.SettingNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            mGestureDownBrightness = lp.screenBrightness * 255;
                                            Log.i(TAG, "current activity brightness: " + mGestureDownBrightness);
                                        }
                                    } else {
                                        mChangeVolume = true;
                                        mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    }
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        long totalTimeDuration = getDuration();
                        mSeekTimePosition = (int) (mGestureDownPosition + deltaX * totalTimeDuration / mScreenWidth);
                        if (mSeekTimePosition > totalTimeDuration)
                            mSeekTimePosition = totalTimeDuration;
                        String seekTime = WEBPlayerUtils.stringForTime(mSeekTimePosition);
                        String totalTime = WEBPlayerUtils.stringForTime(totalTimeDuration);

                        showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY;
                        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);

                        int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);
                        showVolumeDialog(-deltaY, volumePercent);
                    }

                    if (mChangeBrightness) {
                        deltaY = -deltaY;
                        int deltaV = (int) (255 * deltaY * 3 / mScreenHeight);
                        WindowManager.LayoutParams params = WEBPlayerUtils.getWindow(getContext()).getAttributes();
                        if (((mGestureDownBrightness + deltaV) / 255) >= 1) {//这和声音有区别，必须自己过滤一下负值
                            params.screenBrightness = 1;
                        } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
                            params.screenBrightness = 0.01f;
                        } else {
                            params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
                        }
                        WEBPlayerUtils.getWindow(getContext()).setAttributes(params);

                        int brightnessPercent = (int) (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight);
                        showBrightnessDialog(brightnessPercent);
//                        mDownY = y;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "onTouch surfaceContainer actionUp [" + this.hashCode() + "] ");
                    mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumeDialog();
                    dismissBrightnessDialog();
                    if (mChangePosition) {
                        mediaInterface.seekTo(mSeekTimePosition);
                        long duration = getDuration();
                        int progress = (int) (mSeekTimePosition * 100 / (duration == 0 ? 1 : duration));
                        progressBar.setProgress(progress);
                    }
                    if (mChangeVolume) {
                        //change volume event
                    }
                    startProgressTimer();
                    break;
            }
        }
        return false;
    }

    public void onStateNormal() {
        Log.i(TAG, "onStateNormal " + " [" + this.hashCode() + "] ");
        state = STATE_NORMAL;
        cancelProgressTimer();
        if (mediaInterface != null) mediaInterface.release();
    }

    public void onStatePreparing() {
        Log.i(TAG, "onStatePreparing " + " [" + this.hashCode() + "] ");
        state = STATE_PREPARING;
        resetProgressAndTime();
    }

    public void onPrepared() {
        Log.i(TAG, "onPrepared " + " [" + this.hashCode() + "] ");
        state = STATE_PREPARED;
        if (!preloading) {
            mediaInterface.start();
            preloading = false;
        }
        if (WEBPlayerDataSource.getCurrentUrl().toString().toLowerCase().contains("mp3") ||
                WEBPlayerDataSource.getCurrentUrl().toString().toLowerCase().contains("wma") ||
                WEBPlayerDataSource.getCurrentUrl().toString().toLowerCase().contains("aac") ||
                WEBPlayerDataSource.getCurrentUrl().toString().toLowerCase().contains("m4a") ||
                WEBPlayerDataSource.getCurrentUrl().toString().toLowerCase().contains("wav")) {
            onStatePlaying();
        }
    }

    public boolean preloading = false;

    public void startPreloading() {
        preloading = true;
        startVideo();
    }


    public void startVideoAfterPreloading() {
        if (state == STATE_PREPARED) {
            mediaInterface.start();
        } else {
            preloading = false;
            startVideo();
        }
    }

    public void onStatePlaying() {
        Log.i(TAG, "onStatePlaying " + " [" + this.hashCode() + "] ");
        if (state == STATE_PREPARED) {//如果是准备完成视频后第一次播放，先判断是否需要跳转进度。
            if (seekToInAdvance != 0) {
                mediaInterface.seekTo(seekToInAdvance);
                seekToInAdvance = 0;
            } else {
                long position = WEBPlayerUtils.getSavedProgress(getContext(), WEBPlayerDataSource.getCurrentUrl());
                if (position != 0) {
                    mediaInterface.seekTo(position);//这里为什么区分开呢，第一次的播放和resume播放是不一样的。 这里怎么区分是一个问题。然后
                }
            }
        }
        state = STATE_PLAYING;
        startProgressTimer();
    }

    public void onStatePause() {
        Log.i(TAG, "onStatePause " + " [" + this.hashCode() + "] ");
        state = STATE_PAUSE;
        startProgressTimer();
    }

    public void onStateError() {
        Log.i(TAG, "onStateError " + " [" + this.hashCode() + "] ");
        state = STATE_ERROR;
        cancelProgressTimer();
    }

    public void onStateAutoComplete() {
        Log.i(TAG, "onStateAutoComplete " + " [" + this.hashCode() + "] ");
        state = STATE_AUTO_COMPLETE;
        cancelProgressTimer();
        progressBar.setProgress(100);
        currentTimeTextView.setText(totalTimeTextView.getText());
    }

    public void onInfo(int what, int extra) {
        Log.d(TAG, "onInfo what - " + what + " extra - " + extra);
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            if (state == WEBPlayer.STATE_PREPARED
                    || state == WEBPlayer.STATE_PREPARING_CHANGING_URL) {
                onStatePlaying();
            }
        }
    }

    public void onError(int what, int extra) {
        Log.e(TAG, "onError " + what + " - " + extra + " [" + this.hashCode() + "] ");
        if (what != 38 && extra != -38 && what != -38 && extra != 38 && extra != -19) {
            onStateError();
            mediaInterface.release();
        }
    }

    public void onAutoCompletion() {
        Runtime.getRuntime().gc();
        Log.i(TAG, "onAutoCompletion " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateAutoComplete();
        mediaInterface.release();
        WEBPlayerUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WEBPlayerUtils.saveProgress(getContext(), WEBPlayerDataSource.getCurrentUrl(), 0);
    }


    public void reset() {
        Log.i(TAG, "reset " + " [" + this.hashCode() + "] ");
        if (state == STATE_PLAYING || state == STATE_PAUSE) {
            long position = getCurrentPositionWhenPlaying();
            WEBPlayerUtils.saveProgress(getContext(), WEBPlayerDataSource.getCurrentUrl(), position);
        }
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateNormal();
        textureViewContainer.removeAllViews();

        AudioManager mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        }
        WEBPlayerUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mediaInterface != null) mediaInterface.release();
    }

    public void setState(int state) {
        setState(state, 0, 0);
    }

    public void setState(int state, int urlMapIndex, int seekToInAdvance) {//后面两个参数干嘛的
        switch (state) {
            case STATE_NORMAL:
                onStateNormal();
                break;
            case STATE_PREPARING:
                onStatePreparing();
                break;
            case STATE_PREPARING_CHANGING_URL:
                changeUrl(urlMapIndex, seekToInAdvance);
                break;
            case STATE_PLAYING:
                onStatePlaying();
                break;
            case STATE_PAUSE:
                onStatePause();
                break;
            case STATE_ERROR:
                onStateError();
                break;
            case STATE_AUTO_COMPLETE:
                onStateAutoComplete();
                break;
        }
    }

    public void setScreen(int screen) {
        switch (screen) {
            case SCREEN_NORMAL:
                setScreenNormal();
                break;
            case SCREEN_FULLSCREEN:
                setScreenFullscreen();
                break;
            case SCREEN_TINY:
                setScreenTiny();
                break;
        }
    }

    public void startVideo() {
        Log.d(TAG, "startVideo [" + this.hashCode() + "] ");
        setCurrentWEBPlayer(this);
        try {
            Constructor<WEBPlayerMInterface> constructor = mediaInterfaceClass.getConstructor(WEBPlayer.class);
            this.mediaInterface = constructor.newInstance(this);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        addTextureView();

        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        WEBPlayerUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        onStatePreparing();
    }

    public void changeUrl(String url, String title, long seekToInAdvance) {
        changeUrl(new WEBPlayerDataSource(url, title), seekToInAdvance);
    }

    public void changeUrl(int urlMapIndex, long seekToInAdvance) {
        state = STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        WEBPlayerDataSource.currentUrlIndex = urlMapIndex;
        mediaInterface.setSurface(null);
        mediaInterface.release();
        mediaInterface.prepare();
    }

    public void changeUrl(WEBPlayerDataSource WEBPlayerDataSource, long seekToInAdvance) {
        state = STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        this.WEBPlayerDataSource = WEBPlayerDataSource;
        mediaInterface.setSurface(null);
        mediaInterface.release();
        mediaInterface.prepare();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (screen == SCREEN_FULLSCREEN || screen == SCREEN_TINY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthRatio != 0 && heightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) heightRatio) / widthRatio);
            setMeasuredDimension(specWidth, specHeight);

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    public void addTextureView() {
        Log.d(TAG, "addTextureView [" + this.hashCode() + "] ");
        if (textureView != null) textureViewContainer.removeView(textureView);
        textureView = new WEBPlayerTexture(getContext().getApplicationContext());
        textureView.setSurfaceTextureListener(mediaInterface);

        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        textureViewContainer.addView(textureView, layoutParams);
    }

    public void clearFloatScreen() {
        WEBPlayerUtils.showStatusBar(getContext());
        WEBPlayerUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
        WEBPlayerUtils.showSystemUI(getContext());

        ViewGroup vg = (ViewGroup) (WEBPlayerUtils.scanForActivity(getContext())).getWindow().getDecorView();
        vg.removeView(this);
        if (mediaInterface != null) mediaInterface.release();
        currentWEBPlayer = null;
    }

    public void onVideoSizeChanged(int width, int height) {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (textureView != null) {
            if (videoRotation != 0) {
                textureView.setRotation(videoRotation);
            }
            textureView.setVideoSize(width, height);
        }
    }

    public void startProgressTimer() {
        Log.i(TAG, "startProgressTimer: " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public void onProgress(int progress, long position, long duration) {
//        Log.d(TAG, "onProgress: progress=" + progress + " position=" + position + " duration=" + duration);
        if (!mTouchingProgressBar) {
            if (seekToManulPosition != -1) {
                if (seekToManulPosition > progress) {
                    return;
                } else {
                    seekToManulPosition = -1;
                }
            } else {
                if (progress != 0) progressBar.setProgress(progress);
            }
        }
        if (position != 0) currentTimeTextView.setText(WEBPlayerUtils.stringForTime(position));
        totalTimeTextView.setText(WEBPlayerUtils.stringForTime(duration));
    }

    public void setBufferProgress(int bufferProgress) {
        if (bufferProgress != 0) progressBar.setSecondaryProgress(bufferProgress);
    }

    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(WEBPlayerUtils.stringForTime(0));
        totalTimeTextView.setText(WEBPlayerUtils.stringForTime(0));
    }

    public long getCurrentPositionWhenPlaying() {
        long position = 0;
        if (state == STATE_PLAYING ||
                state == STATE_PAUSE) {
            try {
                position = mediaInterface.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    public long getDuration() {
        long duration = 0;
        try {
            duration = mediaInterface.getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (state != STATE_PLAYING &&
                state != STATE_PAUSE) return;
        long time = seekBar.getProgress() * getDuration() / 100;
        seekToManulPosition = seekBar.getProgress();
        mediaInterface.seekTo(time);
        Log.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            long duration = getDuration();
            currentTimeTextView.setText(WEBPlayerUtils.stringForTime(progress * duration / 100));
        }
    }

    public void closePlayer(ViewGroup vg) {
        try {
            Constructor<WEBPlayer> constructor = (Constructor<WEBPlayer>) WEBPlayer.this.getClass().getConstructor(Context.class);
            WEBPlayer WEBPlayer = constructor.newInstance(getContext());
            WEBPlayer.setId(getId());
            vg.addView(WEBPlayer);
            WEBPlayer.setUp(WEBPlayerDataSource.cloneMe(), SCREEN_NORMAL, mediaInterfaceClass);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void gotoScreenFullscreen() {
        ViewGroup vg = (ViewGroup) getParent();
        vg.removeView(this);
        closePlayer(vg);
        CONTAINER_LIST.add(vg);
        vg = (ViewGroup) (WEBPlayerUtils.scanForActivity(getContext())).getWindow().getDecorView();
        vg.addView(this, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setScreenFullscreen();
        WEBPlayerUtils.hideStatusBar(getContext());
        WEBPlayerUtils.setRequestedOrientation(getContext(), FULLSCREEN_ORIENTATION);
        WEBPlayerUtils.hideSystemUI(getContext());

    }

    public void gotoScreenNormal() {
        gobakFullscreenTime = System.currentTimeMillis();
        ViewGroup vg = (ViewGroup) (WEBPlayerUtils.scanForActivity(getContext())).getWindow().getDecorView();
        vg.removeView(this);
        CONTAINER_LIST.getLast().removeAllViews();
        CONTAINER_LIST.getLast().addView(this, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        CONTAINER_LIST.pop();

        setScreenNormal();
        WEBPlayerUtils.showStatusBar(getContext());
        WEBPlayerUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
        WEBPlayerUtils.showSystemUI(getContext());
    }

    public void setScreenNormal() {//TODO 这块不对呀，还需要改进，设置flag之后要设置ui，不设置ui这么写没意义呀
        screen = SCREEN_NORMAL;
    }

    public void setScreenFullscreen() {
        screen = SCREEN_FULLSCREEN;
    }

    public void setScreenTiny() {
        screen = SCREEN_TINY;
    }

    public void autoFullscreen(float x) {
        if (currentWEBPlayer != null
                && (state == STATE_PLAYING || state == STATE_PAUSE)
                && screen != SCREEN_FULLSCREEN
                && screen != SCREEN_TINY) {
            if (x > 0) {
                WEBPlayerUtils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                WEBPlayerUtils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
            gotoScreenFullscreen();
        }
    }

    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
                && state == STATE_PLAYING
                && screen == SCREEN_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    public void onSeekComplete() {

    }

    public void showWifiDialog() {
    }

    public void showProgressDialog(float deltaX,
                                   String seekTime, long seekTimePosition,
                                   String totalTime, long totalTimeDuration) {
    }

    public void dismissProgressDialog() {

    }

    public void showVolumeDialog(float deltaY, int volumePercent) {

    }

    public void dismissVolumeDialog() {

    }

    public void showBrightnessDialog(int brightnessPercent) {

    }

    public void dismissBrightnessDialog() {

    }

    public Context getApplicationContext() {
        Context context = getContext();
        if (context != null) {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext != null) {
                return applicationContext;
            }
        }
        return context;
    }

    public static class AutoFullscreenListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            if (x < -12 || x > 12) {
                if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
                    if (WEBPlayer.currentWEBPlayer != null) WEBPlayer.currentWEBPlayer.autoFullscreen(x);
                    lastAutoFullscreenTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (state == STATE_PLAYING || state == STATE_PAUSE) {
//                Log.v(TAG, "onProgressUpdate " + "[" + this.hashCode() + "] ");
                post(() -> {
                    long position = getCurrentPositionWhenPlaying();
                    long duration = getDuration();
                    int progress = (int) (position * 100 / (duration == 0 ? 1 : duration));
                    onProgress(progress, position, duration);
                });
            }
        }
    }

    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {//是否新建个class，代码更规矩，并且变量的位置也很尴尬
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    Log.d(TAG, "AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        WEBPlayer player = currentWEBPlayer;
                        if (player != null && player.state == WEBPlayer.STATE_PLAYING) {
                            player.startButton.performClick();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    public static void goOnPlayOnResume() {
        if (currentWEBPlayer != null) {
            if (currentWEBPlayer.state == WEBPlayer.STATE_PAUSE) {
                if (ON_PLAY_PAUSE_TMP_STATE == STATE_PAUSE) {
                    currentWEBPlayer.onStatePause();
                    currentWEBPlayer.mediaInterface.pause();
                } else {
                    currentWEBPlayer.onStatePlaying();
                    currentWEBPlayer.mediaInterface.start();
                }
                ON_PLAY_PAUSE_TMP_STATE = 0;
            }
        }
    }

    public static void goOnPlayOnPause() {
        if (currentWEBPlayer != null) {
            if (currentWEBPlayer.state == WEBPlayer.STATE_AUTO_COMPLETE ||
                    currentWEBPlayer.state == WEBPlayer.STATE_NORMAL ||
                    currentWEBPlayer.state == WEBPlayer.STATE_PREPARING ||
                    currentWEBPlayer.state == WEBPlayer.STATE_ERROR) {
                WEBPlayer.releaseAllVideos();
            } else {
                ON_PLAY_PAUSE_TMP_STATE = currentWEBPlayer.state;
                currentWEBPlayer.onStatePause();
                currentWEBPlayer.mediaInterface.pause();
            }
        }
    }

    public static void startFullscreenDirectly(Context context, Class _class, String url, String title) {
        startFullscreenDirectly(context, _class, new WEBPlayerDataSource(url, title));
    }

    public static void startFullscreenDirectly(Context context, Class _class, WEBPlayerDataSource WEBPlayerDataSource) {
        WEBPlayerUtils.hideStatusBar(context);
        WEBPlayerUtils.setRequestedOrientation(context, FULLSCREEN_ORIENTATION);
        WEBPlayerUtils.hideSystemUI(context);

        ViewGroup vp = (ViewGroup) WEBPlayerUtils.scanForActivity(context).getWindow().getDecorView();
        try {
            Constructor<WEBPlayer> constructor = _class.getConstructor(Context.class);
            final WEBPlayer WEBPlayer = constructor.newInstance(context);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(WEBPlayer, lp);
            WEBPlayer.setUp(WEBPlayerDataSource, WEBPlayerStd.SCREEN_FULLSCREEN);
            WEBPlayer.startVideo();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void releaseAllVideos() {
        Log.d(TAG, "releaseAllVideos");
        if (currentWEBPlayer != null) {
            currentWEBPlayer.reset();
            currentWEBPlayer = null;
        }
    }

    public static boolean backPress() {
        Log.i(TAG, "backPress");
        if (CONTAINER_LIST.size() != 0 && currentWEBPlayer != null) {
            currentWEBPlayer.gotoScreenNormal();
            return true;
        } else if (CONTAINER_LIST.size() == 0 && currentWEBPlayer != null && currentWEBPlayer.screen != SCREEN_NORMAL) {//退出直接进入的全屏
            currentWEBPlayer.clearFloatScreen();
            return true;
        }
        return false;
    }

    public static void setCurrentWEBPlayer(WEBPlayer WEBPlayer) {
        if (currentWEBPlayer != null) currentWEBPlayer.reset();
        currentWEBPlayer = WEBPlayer;
    }

    public static void clearSavedProgress(Context context, String url) {
        WEBPlayerUtils.clearSavedProgress(context, url);
    }

    public static void setTextureViewRotation(int rotation) {
        if (currentWEBPlayer != null && currentWEBPlayer.textureView != null) {
            currentWEBPlayer.textureView.setRotation(rotation);
        }
    }

    public static void setVideoImageDisplayType(int type) {
        WEBPlayer.VIDEO_IMAGE_DISPLAY_TYPE = type;
        if (currentWEBPlayer != null && currentWEBPlayer.textureView != null) {
            currentWEBPlayer.textureView.requestLayout();
        }
    }

}
