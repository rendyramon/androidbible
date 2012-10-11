package yuku.alkitab.base.ac;

import yuku.alkitab.R;
import yuku.alkitab.base.ac.base.BaseActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;

public class AudioActivity extends BaseActivity implements OnClickListener, OnTouchListener, OnCompletionListener, OnBufferingUpdateListener{
	
	private ImageButton buttonPlayPause;
	private SeekBar seekBarProgress;
	public EditText editTextSongURL;
	
	private MediaPlayer mediaPlayer;
	private int mediaFileLengthInMilliseconds; // this value contains the song duration in milliseconds. Look at getDuration() method in MediaPlayer class
	private String URL;
	
	private final Handler handler = new Handler();
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        
        buttonPlayPause = (ImageButton)findViewById(R.id.ButtonTestPlayPause);
		buttonPlayPause.setOnClickListener(this);
		
		seekBarProgress = (SeekBar)findViewById(R.id.SeekBarTestPlay);	
		seekBarProgress.setMax(99); // It means 100% .0-99
		seekBarProgress.setOnTouchListener(this);
		editTextSongURL = (EditText)findViewById(R.id.EditTextSongURL);		
		
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnCompletionListener(this);
		
        Intent intent = getIntent();
        URL = intent.getStringExtra("url");
        
        Log.d("Audio", "url: " + URL);        
    }
     
	/** Method which updates the SeekBar primary progress by current song playing position*/
    private void primarySeekBarProgressUpdater() {
    	seekBarProgress.setProgress((int)(((float)mediaPlayer.getCurrentPosition()/mediaFileLengthInMilliseconds)*100)); // This math construction give a percentage of "was playing"/"song length"
		if (mediaPlayer.isPlaying()) {
			Runnable notification = new Runnable() {
		        public void run() {
		        	primarySeekBarProgressUpdater();
				}
		    };
		    handler.postDelayed(notification,1000);
    	}
    }

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.ButtonTestPlayPause){
			 /** ImageButton onClick event handler. Method which start/pause mediaplayer playing */
			try {
				mediaPlayer.setDataSource(URL); // setup song from http://www.hrupin.com/wp-content/uploads/mp3/testsong_20_sec.mp3 URL to mediaplayer data source
				mediaPlayer.prepare(); // you must call this method after setup the datasource in setDataSource method. After calling prepare() the instance of MediaPlayer starts load data from URL to internal buffer.
			} catch (Exception e) {
				new AlertDialog.Builder(AudioActivity.this)
				.setTitle("Audio")
				.setMessage("Audio tidak tersedia untuk pasal tersebut")
				.setPositiveButton("OK", null);
				e.printStackTrace();				
			}
			
			try {
				mediaFileLengthInMilliseconds = mediaPlayer.getDuration(); // gets the song length in milliseconds from URL
				
				
				if(!mediaPlayer.isPlaying()){
					mediaPlayer.start();
//					bPlayPause.setText("Pause");
					buttonPlayPause.setImageResource(R.drawable.button_pause);
				}else {
					mediaPlayer.pause();
//					bPlayPause.setText("Play");
					buttonPlayPause.setImageResource(R.drawable.button_play);
				}
				
				primarySeekBarProgressUpdater();
				
			} catch (Exception e){
				new AlertDialog.Builder(AudioActivity.this)
				.setTitle("Audio")
				.setMessage("Audio tidak tersedia untuk pasal tersebut")
				.setPositiveButton("OK", null);
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(v.getId() == R.id.SeekBarTestPlay){
			/** Seekbar onTouch event handler. Method which seeks MediaPlayer to seekBar primary progress position*/
//			if(mediaPlayer.isPlaying()){
		    	SeekBar sb = (SeekBar)v;
				int playPositionInMillisecconds = (mediaFileLengthInMilliseconds / 100) * sb.getProgress();
				mediaPlayer.seekTo(playPositionInMillisecconds);
//			}
		}
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		 /** MediaPlayer onCompletion event handler. Method which calls then song playing is complete*/
		buttonPlayPause.setImageResource(R.drawable.button_play);		
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		/** Method which updates the SeekBar secondary progress by current song loading from URL position*/
		seekBarProgress.setSecondaryProgress(percent);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mediaPlayer.release();
	}

}
