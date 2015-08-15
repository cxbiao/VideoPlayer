package com.bryan.widget;

import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import cn.wjq.onlinevideoplay.R;

public class MediaController extends FrameLayout {

	private MediaPlayerControl mPlayer;
	private PlayerCallback playerCb;
	private Context mContext;
	private ProgressBar mProgress;
	private TextView mEndTime, mCurrentTime;
	private TextView mTitle, mDescription;
	private boolean mShowing;
	private boolean mDragging;
	private static final int sDefaultTimeout = 3000;
	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;
	private static final int SHOW_LOADIGN   = 3;
	private static final int HIDE_LOADIGN   = 4;
	StringBuilder mFormatBuilder;
	Formatter mFormatter;
	private ImageButton mTurnButton;// 开启暂停按钮
	private ImageButton mScaleButton;
	private ImageButton mSoundButton;
	private View mCloseVdButton;// 关闭按钮
	private View loading;

	private AudioManager mAudioManager = null;
	private int maxVolume = 0;
	private int currentVolume = 0;
	private View soundSeekbarLayout;
	private VerticalSeekBar seekSound;
	private boolean fullScreen = false;
	private View titleLayout;

	public MediaController(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public MediaController(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.player_controller, this);
		view.setOnTouchListener(mTouchListener);
		initControllerView(view);

		initSoundView();
	}

	private void initSoundView() {
		mAudioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		currentVolume = mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		seekSound.setOnSeekBarChangeListener(soundSeekListener);

		seekSound.setThumb(mContext.getResources().getDrawable(
				R.drawable.fanxing_star_seek_dot));
		seekSound.setProgressDrawable(mContext.getResources().getDrawable(
				R.drawable.fanxing_star_play_progress_seek));
		seekSound.setThumbOffset(0);
		seekSound.setProgress(currentVolume * 100 / maxVolume);
	}

	private void updateVolume(int index) {
		if (mAudioManager != null) {
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
			currentVolume = index;
		}
	}

	private OnSeekBarChangeListener soundSeekListener = new OnSeekBarChangeListener() {
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			show(3600000);
			mHandler.removeMessages(SHOW_PROGRESS);
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			int index = progress * maxVolume / 100;
			updateVolume(index);
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			seekSound.setProgress(currentVolume * 100 / maxVolume);
			show(sDefaultTimeout);

			// Ensure that progress is properly updated in the future,
			// the call to show() does not guarantee this because it is a
			// no-op if we are already showing.
			mShowing = true;
			mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}
	};
	
	public void showLoading() {
		mHandler.sendEmptyMessage(SHOW_LOADIGN);
	}


	public void hideLoading() {
		mHandler.sendEmptyMessage(HIDE_LOADIGN);
		//loading.setVisibility(View.GONE);
	}

	private void initControllerView(View v) {
		titleLayout = v.findViewById(R.id.title_part);
		loading = v.findViewById(R.id.loading_layout);
		mTurnButton = (ImageButton) v.findViewById(R.id.turn_button);
		mScaleButton = (ImageButton) v.findViewById(R.id.scale_button);
		mSoundButton = (ImageButton) v.findViewById(R.id.sound_button);
		mCloseVdButton = v.findViewById(R.id.back_btn);
		loading.setVisibility(View.GONE);
		
		if (mTurnButton != null) {
			mTurnButton.requestFocus();
			mTurnButton.setOnClickListener(mPauseListener);
		}
		
		// 是否全屏
		mScaleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismissSoundWindow();
				if (playerCb != null) {
					fullScreen = playerCb.zoom();
					updateScaleButton(fullScreen);
				}
			}
		});
		mSoundButton.setOnClickListener(soundClickListener);
		mCloseVdButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(fullScreen && playerCb!=null){
					fullScreen = playerCb.zoom();
					updateScaleButton(fullScreen);
				}
				else if (playerCb != null) {
					onDestroy();
					playerCb.exit();
				}
			}
		});
		View bar = v.findViewById(R.id.seekbar);
		if (bar != null) {
            int max = 1000;
			if (bar instanceof SeekBar) {
				SeekBar seeker = (SeekBar) bar;
				seeker.setOnSeekBarChangeListener(mSeekListener);
                seeker.setMax(max);
			} else if(bar instanceof  ProgressBar) {
                mProgress = (ProgressBar)bar;
                mProgress.setMax(max);
            }
		}

		mEndTime = (TextView) v.findViewById(R.id.duration);
		mCurrentTime = (TextView) v.findViewById(R.id.has_played);
		mTitle = (TextView) v.findViewById(R.id.title);
		mDescription = (TextView) v.findViewById(R.id.txt_right);
		mFormatBuilder = new StringBuilder();
		mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

		soundSeekbarLayout = v.findViewById(R.id.sound_seek_layout);
		seekSound = (VerticalSeekBar) v.findViewById(R.id.sound_seek);
	}

	private OnClickListener soundClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (soundSeekbarLayout.getVisibility() == View.VISIBLE) {
				soundSeekbarLayout.setVisibility(View.INVISIBLE);
			} else {
				soundSeekbarLayout.setVisibility(View.VISIBLE);
				// mSoundWindow.showAsDropDown(v, v.getWidth() / 2,
				// -soundViewHeight - v.getHeight() - 10);
				// mSoundWindow.update(v, soundViewWidth, soundViewHeight);
				// mSoundWindow.showAtLocation(MediaController.this,
				// Gravity.RIGHT | Gravity.BOTTOM, -15, 0);
				// mSoundWindow.update(15, 0, soundViewWidth, soundViewHeight);

			}
		}
	};

	private OnTouchListener mTouchListener = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				if (mShowing) {
					hide();
					return true;
				}
			}
			return false;
		}
	};

	public void setMediaPlayer(MediaPlayerControl player) {
		mPlayer = player;
		updatePausePlay();
	}

	public void show() {
		show(sDefaultTimeout);
	}

	private void disableUnsupportedButtons() {
		try {
			if (mTurnButton != null && mPlayer != null && !mPlayer.canPause()) {
				mTurnButton.setEnabled(false);
			}
		} catch (IncompatibleClassChangeError ex) {
		}
	}

	public void show(int timeout) {
		if (!mShowing) {
			setProgress();
			if (mTurnButton != null) {
				mTurnButton.requestFocus();
			}
			disableUnsupportedButtons();
		}
		updatePausePlay();

		// cause the progress bar to be updated even if mShowing
		// was already true. This happens, for example, if we're
		// paused with the progress bar showing the user hits play.
		mShowing = true;
		mHandler.sendEmptyMessage(SHOW_PROGRESS);
	
		Message msg = mHandler.obtainMessage(FADE_OUT);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, timeout);
		}
	}

	public boolean isShowing() {
		return mShowing;
	}

	private void dismissSoundWindow() {
		// if (mSoundWindow != null && mSoundWindow.isShowing()) {
		// mSoundWindow.dismiss();
		// }
		if (soundSeekbarLayout != null)
			soundSeekbarLayout.setVisibility(View.INVISIBLE);
	}

	/**
	 * Remove the controller from the screen.
	 */
	public void hide() {
		if (mShowing) {
			mHandler.removeMessages(SHOW_PROGRESS);
			dismissSoundWindow();
			this.setVisibility(View.GONE);
			mShowing = false;
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			 int pos;
			switch (msg.what) {
			case FADE_OUT:
				hide();
				break;
			case SHOW_PROGRESS:
				pos = setProgress();
				setVisibility(View.VISIBLE);
				if (!mDragging && mShowing && mPlayer != null && mPlayer.isPlaying()) {
                    msg = obtainMessage(SHOW_PROGRESS);
                    sendMessageDelayed(msg, 1000 - (pos % 1000));
                }
				
				break;
			case SHOW_LOADIGN:
				loading.setVisibility(View.VISIBLE);
				break;
			case HIDE_LOADIGN:
				loading.setVisibility(View.GONE);
				break;
			}
		}
	};
	
	public void reset(){
		mCurrentTime.setText("00:00");
		mEndTime.setText("00:00");
		mProgress.setProgress(0);
		mTurnButton.setImageResource(R.drawable.fanxing_player_player_btn_selector);
		setVisibility(View.VISIBLE);
		hideLoading();
	}

	private String stringForTime(int timeMs) {
		int totalSeconds = timeMs / 1000;

		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
					.toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	private int setProgress() {
		if (mPlayer == null || mDragging) {
			return 0;
		}
		int position = mPlayer.getCurrentPosition();
		int duration = mPlayer.getDuration();
		if (mProgress != null) {
			if (duration > 0) {
				// use long to avoid overflow
				long pos = 1000L * position / duration;
				mProgress.setProgress((int) pos);
			}
			int percent = mPlayer.getBufferPercentage();
			mProgress.setSecondaryProgress(percent * 10);
		}

		if (mEndTime != null)
			mEndTime.setText(stringForTime(duration));
		if (mCurrentTime != null)
			mCurrentTime.setText(stringForTime(position));

		return position;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// show(sDefaultTimeout);
		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		show(sDefaultTimeout);
		return false;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		final boolean uniqueDown = event.getRepeatCount() == 0
				&& event.getAction() == KeyEvent.ACTION_DOWN;
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
				|| keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
				|| keyCode == KeyEvent.KEYCODE_SPACE) {
			if (uniqueDown) {
				doPauseResume();
				show(sDefaultTimeout);
				if (mTurnButton != null) {
					mTurnButton.requestFocus();
				}
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
			if (uniqueDown && !mPlayer.isPlaying()) {
				mPlayer.start();
				updatePausePlay();
				show(sDefaultTimeout);
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
				|| keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
			if (uniqueDown && mPlayer.isPlaying()) {
				mPlayer.pause();
				updatePausePlay();
				show(sDefaultTimeout);
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_UP
				|| keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
				|| keyCode == KeyEvent.KEYCODE_CAMERA) {
			// don't show the controls for volume adjustment
			return super.dispatchKeyEvent(event);
		} else if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_MENU) {
			if (uniqueDown) {
				hide();
			}
			return true;
		}

		show(sDefaultTimeout);
		return super.dispatchKeyEvent(event);
	}

	private View.OnClickListener mPauseListener = new View.OnClickListener() {
		public void onClick(View v) {
			if(mPlayer == null) {
				if(playerCb != null) {
					playerCb.firstPlay();
				}
				return;
			}
			doPauseResume();
			show(sDefaultTimeout);
		}
	};

	private void updatePausePlay() {
		if (mPlayer != null && mPlayer.isPlaying()) {
			mTurnButton
					.setImageResource(R.drawable.fanxing_player_stop_btn_selector);
		} else {
			mTurnButton
					.setImageResource(R.drawable.fanxing_player_player_btn_selector);
		}
	}

	public void updateScaleButton(boolean fullscreen) {
		this.fullScreen = fullscreen; 
		if (fullscreen) {
			mScaleButton
					.setImageResource(R.drawable.fanxing_player_zoom_in_selector);
			titleLayout.setVisibility(View.VISIBLE);
		} else {
			mScaleButton
					.setImageResource(R.drawable.fanxing_player_scale_btn_selector);
			titleLayout.setVisibility(View.GONE);
		}
	}

	private void doPauseResume() {
		if (mPlayer != null && mPlayer.isPlaying()) {
			mPlayer.pause();
		} else {
			mPlayer.start();
		}
		updatePausePlay();
	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			if(mPlayer == null) {
				return;
			}
			show(3600000);

			mDragging = true;
			mHandler.removeMessages(SHOW_PROGRESS);
		}

		public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
			if(mPlayer == null) {
				return;
			}
			if (!fromuser) {
				// We're not interested in programmatically generated changes to
				// the progress bar's position.
				return;
			}

			long duration = mPlayer.getDuration();
			long newposition = (duration * progress) / 1000L;
			mPlayer.seekTo((int) newposition);
			if (mCurrentTime != null)
				mCurrentTime.setText(stringForTime((int) newposition));
		}

		public void onStopTrackingTouch(SeekBar bar) {
			if(mPlayer == null) {
				return;
			}
			mDragging = false;
			setProgress();
			updatePausePlay();
			show(sDefaultTimeout);

			// Ensure that progress is properly updated in the future,
			// the call to show() does not guarantee this because it is a
			// no-op if we are already showing.
			mShowing = true;
			mHandler.sendEmptyMessage(SHOW_PROGRESS);
			mHandler.sendEmptyMessage(SHOW_LOADIGN);
		}
	};

	@Override
	public void setEnabled(boolean enabled) {
		if (mTurnButton != null) {
			mTurnButton.setEnabled(enabled);
		}
		if (mProgress != null) {
			mProgress.setEnabled(enabled);
		}
		disableUnsupportedButtons();
		super.setEnabled(enabled);
	}

	public static interface MediaPlayerControl {
		void start();

		void pause();

		int getDuration();

		int getCurrentPosition();

		void seekTo(int pos);

		boolean isPlaying();

		int getBufferPercentage();

		boolean canPause();

		boolean canSeekBackward();

		boolean canSeekForward();
	}

	public void setPlayerCallback(PlayerCallback cb) {
		this.playerCb = cb;
	}

	public static interface PlayerCallback {
		boolean zoom(); // 放大缩小

		boolean exit();// 退出播放
		
		boolean firstPlay();// 第一次点击播放
	}

	public void onDestroy() {
		dismissSoundWindow();
		seekSound = null;
		// mSoundWindow = null;
	}

	public void setTitle(String titile) {
		mTitle.setText(titile);
	}

	public void setDescription(String des) {
		mDescription.setText(des);
	}
}
