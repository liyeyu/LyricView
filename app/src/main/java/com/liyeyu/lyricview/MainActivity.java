package com.liyeyu.lyricview;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.liyeyu.lrcview.lrc.LrcBuilder;
import com.liyeyu.lrcview.lrc.LrcInfo;
import com.liyeyu.lrcview.lrc.LrcView;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private LrcView mLrcView;
    private Button mParse;
    private MediaPlayer mMediaPlayer;
    private Button mPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLrcView = (LrcView) findViewById(R.id.lv);
        mParse = (Button) findViewById(R.id.parse);
        mPlay = (Button) findViewById(R.id.play);
        mParse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
                parse();
//                parseUTF();
            }
        });
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMediaPlayer!=null){
                    if(mMediaPlayer.isPlaying()){
                        mMediaPlayer.pause();
                    }else{
                        mMediaPlayer.start();
                    }
                }
            }
        });
        mMediaPlayer = new MediaPlayer();
        mLrcView.setOnPlayerClickListener(new LrcView.OnPlayerClickListener() {
            @Override
            public void onPlayerClicked(long progress, String content) {
                mMediaPlayer.seekTo((int) progress);
            }
        });
        mLrcView.setOnLrcViewTouchListener(new LrcView.OnPlayerTouchListener(){
            @Override
            public void onClick(View view, int line, String content) {
                Log.i("actionUp","line:"+line+" "+content);
            }

            @Override
            public void onLongClick(View view, int line, String content) {
                Log.i("actionUp","line:"+line+" "+content);
            }
        });
    }

    private void start(){
        try {
            mMediaPlayer.reset();
            AssetFileDescriptor fileDescriptor = getAssets().openFd("双笙 - 故梦.mp3");
            mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setVolume(1,1);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mp) {
                    mMediaPlayer.start();
                    HandlerThread thread = new HandlerThread("mMediaPlayer");
                    thread.start();
                    final Handler handler = new Handler(thread.getLooper(), new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            if(mMediaPlayer!=null && mMediaPlayer.isPlaying()){
                                mLrcView.setCurrentTimeMillis(mMediaPlayer.getCurrentPosition());
                                msg.getTarget().sendEmptyMessageDelayed(0,100);
                            }
                            return false;
                        }
                    });
                    handler.sendEmptyMessage(0);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mMediaPlayer!=null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void parse(){
        new LrcBuilder(new LrcBuilder.OnLrcLoadListener() {
            @Override
            public InputStream loadLrc(LrcBuilder builder) {
                InputStream open = null;
                try {
                    open = getAssets().open("双笙 - 故梦.lrc");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return open;
            }

            @Override
            public void onLoad(LrcBuilder builder, LrcInfo lrcInfo) {
                mLrcView.setLrcInfo(lrcInfo);
            }
        }).load();

    }

    public void parseUTF(){
        try {
            InputStream open = getAssets().open("双笙 - 故梦.lrc");
            LrcInfo lrcInfo = new LrcBuilder().parseLrcInfo(open);
            mLrcView.setLrcInfo(lrcInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
