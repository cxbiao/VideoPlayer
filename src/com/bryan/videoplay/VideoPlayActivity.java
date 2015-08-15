package com.bryan.videoplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import cn.wjq.onlinevideoplay.R;

import com.bryan.widget.MediaController;
import com.bryan.widget.VideoView;
import com.bryan.widget.MediaController.PlayerCallback;

/**
 * 星乐坊预告/回顾房间
 */
public class VideoPlayActivity extends Activity {
	
	private static final String TAG = "StarInterviewPlayActivity";
	private VideoView mVideoView;
	private String strVideoPath;
	
	private final static int SCREEN_FULL = 0;
	private final static int SCREEN_DEFAULT = 1;

	RelativeLayout.LayoutParams layoutParams;

	private int defaultHeight = 0;
	private boolean fullscreen = false;

	private View bottonLayout;

	private MediaController controler = null;

//	private StarInterviewInfoEntity starItvInfo;// 星月房 房间信息
	
	private View centerPlayBtn;
	
	private View titleView;

	private OrientationEventListener mOrientationListener;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_media_playview);
		initViews();

		fullscreen = false;
		
	//	strVideoPath = "http://storagemv2.open.kugou.com/201502061129/17f1b1a657c3d1353803c5e408e8d81c/M05/1D/6E/CgEy51PMsm6hGKQqAs4MHg-LewU770.mp4";
		strVideoPath = "http://trackermv.kugou.com/interface/index?cmd=104&hash=03b19be9249388e6781ef42ccd6be28f&pid=99";
//		String coverUrl = "/fxinterview/20150121/20150121191614316919.jpg";
	}

	private void initViews() {
		titleView = findViewById(R.id.comman_title_layout);
		mVideoView = (VideoView) findViewById(R.id.videoView);
		controler = (MediaController) findViewById(R.id.video_controller);
		controler.setPlayerCallback(playerCb);
		//backImage = (ImageView) findViewById(R.id.back_image);
		centerPlayBtn = findViewById(R.id.play_btn);
		bottonLayout = findViewById(R.id.button_layout);

		centerPlayBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//backImage.setVisibility(View.GONE);
				centerPlayBtn.setVisibility(View.GONE);
				playVideo(strVideoPath);
			}
		});

		// init VideoView
		defaultHeight = getResources().getDisplayMetrics().widthPixels * 9 / 16;
		setVideoAreaSize(defaultHeight);
		//backImage.setLayoutParams(layoutParams);
		mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						controler.hideLoading();// 隐藏"视频加载中"进度
					}
				});
			}
		});
		mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				resetVideo();
				mVideoView.pause();
				centerPlayBtn.setVisibility(View.VISIBLE);
			}
		});
	}


	private void playVideo(String strPath) {
		if (strPath != "" && strPath != null) {
			mVideoView.setMediaController(controler);
			mVideoView.setVideoURI(Uri.parse(strPath));

			mVideoView.requestFocus();
			setVideoScaleType(fullscreen ? SCREEN_FULL : SCREEN_DEFAULT);
			mVideoView.start();
			controler.showLoading();
			if (mVideoView.isPlaying()) {
				Log.i(TAG, strPath);
			}
		}
	}

	private void resetVideo() {
		if (mVideoView != null) {
			mVideoView.seekTo(0);
		}
	}

	private void setVideoScaleType(int flag) {
		switch (flag) {
		case SCREEN_FULL:
			setVideoAreaSize(RelativeLayout.LayoutParams.MATCH_PARENT);
			break;

		case SCREEN_DEFAULT:
			setVideoAreaSize(defaultHeight);
			break;
		}
	}

	private void setVideoAreaSize(int h) { // width : fillparent
		if (layoutParams == null) {
			layoutParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.MATCH_PARENT, h);
		}
		layoutParams.height = h;
		mVideoView.setLayoutParams(layoutParams);
		controler.setLayoutParams(layoutParams);
	}
	
	private void switchFullScreenMode(){
		setVideoScaleType(SCREEN_FULL);
		getWindow()
				.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		bottonLayout.setVisibility(View.GONE);
		titleView.setVisibility(View.GONE);
		fullscreen = true;
	}

	private void switchDefaultScreenMode(){
		setVideoScaleType(SCREEN_DEFAULT);
		bottonLayout.setVisibility(View.VISIBLE);
		titleView.setVisibility(View.VISIBLE);
		getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		fullscreen = false;
	}
	
	PlayerCallback playerCb = new PlayerCallback() {

		@Override
		public boolean zoom() {
			if (!fullscreen) {// 设置RelativeLayout的全屏模式
				switchFullScreenMode();
				flag = false;
			} else {// 设置RelativeLayout的窗口模式
				switchDefaultScreenMode();
				flag = false;
			}
			return fullscreen;
		}

		@Override
		public boolean exit() {
			finish();
			return true;
		}
		
		public boolean firstPlay() {
			centerPlayBtn.setVisibility(View.GONE);
			playVideo(strVideoPath);
			return true;
		}
	};
	
	@Override
	public void onDestroy() {
		if (controler != null) {
			controler.onDestroy();
			controler = null;
		}
		super.onDestroy();
	};
	
	@Override
	protected void onResume() {
		super.onResume();
		startOrientationListener();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (mOrientationListener != null) {
			mOrientationListener.disable();
			mOrientationListener = null;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && fullscreen) {
			try{
				switchDefaultScreenMode();
			}catch(Exception ex){}
			controler.updateScaleButton(fullscreen);
			return true;
		}
		
		
		if(keyCode == KeyEvent.KEYCODE_BACK && (!fullscreen)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("确定退出应用吗");
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					finish();
				}
			});
			builder.show();
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}
	
    private boolean flag = false; // 是否开始监听屏幕方向变化
	
	// 开启屏幕切换检测
	protected void startOrientationListener() {
		mOrientationListener = new OrientationEventListener(this,
				SensorManager.SENSOR_DELAY_UI) {

			@Override
			public void onOrientationChanged(int rotation) {
				if (((rotation >= 0) && (rotation <= 80)) || (rotation >= 320)) {//转到竖屏位置
					if(!flag){//刚点击了屏幕切换按钮
						if(!fullscreen){
							flag = true;
						}
					}
					else if (fullscreen) {
						try{
							switchDefaultScreenMode();
							controler.updateScaleButton(fullscreen);
						}catch(Exception ex){}
					}
				} else { // 转到横屏位置
					if(!flag){
						if(fullscreen){//刚点击切换横屏
							flag = true;
						}
					}
					else if (!fullscreen) {
						try{
							switchFullScreenMode();
							controler.updateScaleButton(fullscreen);
						}catch(Exception ex){}
					}
				}
			}
		};
		flag = true;
//		mOrientationListener.enable();
	};
	
}
