package yuku.alkitab.base;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import yuku.afw.V;
import yuku.afw.storage.Preferences;
import yuku.alkitab.R;
import yuku.alkitab.base.IsiActivity.FakeContextMenu.Item;
import yuku.alkitab.base.ac.AboutActivity;
import yuku.alkitab.base.ac.AudioActivity;
import yuku.alkitab.base.ac.BookmarkActivity;
import yuku.alkitab.base.ac.DevotionActivity;
import yuku.alkitab.base.ac.GotoActivity;
import yuku.alkitab.base.ac.HelpActivity;
import yuku.alkitab.base.ac.Search2Activity;
import yuku.alkitab.base.ac.SettingsActivity;
import yuku.alkitab.base.ac.ShareActivity;
import yuku.alkitab.base.ac.SongViewActivity;
import yuku.alkitab.base.ac.VersionsActivity;
import yuku.alkitab.base.ac.VersionsActivity.MVersion;
import yuku.alkitab.base.ac.VersionsActivity.MVersionInternal;
import yuku.alkitab.base.ac.VersionsActivity.MVersionPreset;
import yuku.alkitab.base.ac.VersionsActivity.MVersionYes;
import yuku.alkitab.base.ac.base.BaseActivity;
import yuku.alkitab.base.compat.Api8;
import yuku.alkitab.base.config.BuildConfig;
import yuku.alkitab.base.dialog.TypeBookmarkDialog;
import yuku.alkitab.base.dialog.TypeHighlightDialog;
import yuku.alkitab.base.dialog.TypeNoteDialog;
import yuku.alkitab.base.media.AlkitabAudio;
import yuku.alkitab.base.model.Ari;
import yuku.alkitab.base.model.Book;
import yuku.alkitab.base.model.PericopeBlock;
import yuku.alkitab.base.model.Version;
import yuku.alkitab.base.rpc.SimpleHttpConnection;
import yuku.alkitab.base.storage.Db;
import yuku.alkitab.base.util.History;
import yuku.alkitab.base.util.IntArrayList;
import yuku.alkitab.base.util.Jumper;
import yuku.alkitab.base.util.LidToAri;
import yuku.alkitab.base.util.Search2Engine.Query;
import yuku.alkitab.base.widget.CallbackSpan;
import yuku.alkitab.base.widget.VerseAdapter;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class IsiActivity extends BaseActivity implements OnClickListener,
		OnTouchListener, OnCompletionListener, OnBufferingUpdateListener,
		OnPreparedListener {
	public static final String TAG = IsiActivity.class.getSimpleName();

	// The followings are for instant_pref
	private static final String PREFKEY_lastBook = "kitabTerakhir"; //$NON-NLS-1$
	private static final String PREFKEY_lastChapter = "pasalTerakhir"; //$NON-NLS-1$
	private static final String PREFKEY_lastVerse = "ayatTerakhir"; //$NON-NLS-1$
	private static final String PREFKEY_lastVersion = "edisiTerakhir"; //$NON-NLS-1$
	private static final String PREFKEY_devotion_name = "renungan_nama"; //$NON-NLS-1$

	public static final int RESULT_pindahCara = RESULT_FIRST_USER + 1;

	private static final int REQCODE_goto = 1;
	private static final int REQCODE_bookmark = 2;
	private static final int REQCODE_devotion = 3;
	private static final int REQCODE_settings = 4;
	private static final int REQCODE_version = 5;
	private static final int REQCODE_search = 6;
	private static final int REQCODE_share = 7;
	private static final int REQCODE_songs = 8;
	private static final int INTIAL_KB_BUFFER = 64;

	private static final String EXTRA_verseUrl = "urlAyat"; //$NON-NLS-1$
	private int mediaFileLength;
	private int totalKbRead = 0;
	private int counter = 0;
	private int nread = 0;
	private long mediaLengthInKb;
	private boolean isInterrupted = false;
	private boolean mpSet = false;
	private File downloadFile;
	private Handler handler = new Handler();

	ListView lsText;
	Button bGoto;
	ImageButton bLeft;
	ImageButton bRight;
	ImageButton playPause;
	View titleContainer;
	TextView lTitle, tProgress;
	View bContextMenu;
	View root;
	View aPlayer;
	SeekBar seek;
	MediaPlayer mediaPlayer;

	int chapter_1 = 0;
	SharedPreferences instant_pref;

	VerseAdapter verseAdapter_;
	History history;
	NfcAdapter nfcAdapter;

	// # state storage for search2
	Query search2_query = null;
	IntArrayList search2_results = null;
	int search2_selectedPosition = -1;

	// temporary states
	Animation fadeInAnimation;
	Animation fadeOutAnimation;
	boolean showingContextMenuButton = false;
	Boolean hasEsvsbAsal;

	CallbackSpan.OnClickListener parallel_click = new CallbackSpan.OnClickListener() {
		@Override
		public void onClick(View widget, Object data) {
			int ari = jumpTo((String) data);
			if (ari != 0) {
				history.add(ari);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_isi);

		lsText = V.get(this, R.id.lsIsi);
		bGoto = V.get(this, R.id.bTuju);
		bLeft = V.get(this, R.id.bKiri);
		bRight = V.get(this, R.id.bKanan);
		titleContainer = V.get(this, R.id.tempatJudul);
		lTitle = V.get(this, R.id.lJudul);
		bContextMenu = V.get(this, R.id.bContext);
		root = V.get(this, R.id.root);
		// playPause = V.get(this, R.id.ButtonPlayPause);
		// seek = V.get(this, R.id.SeekBarPlay);
		aPlayer = V.get(this, R.id.audioPlayer);
		tProgress = V.get(this, R.id.tProgress);

		// audioPlayer
		playPause = (ImageButton) findViewById(R.id.ButtonPlayPause);
		// playPause.setOnClickListener(this);

		seek = (SeekBar) findViewById(R.id.SeekBarPlay);
		seek.setMax(99); // It means 100% .0-99
		seek.setOnTouchListener(this);

		applyPreferences(false);

		lsText.setOnItemClickListener(lsText_itemClick);
		lsText.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		bGoto.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bGoto_click();
			}
		});
		bGoto.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				bGoto_longClick();
				return true;
			}
		});

		lTitle.setOnClickListener(new View.OnClickListener() { // pinjem bTuju
			@Override
			public void onClick(View v) {
				bGoto_click();
			}
		});
		lTitle.setOnLongClickListener(new View.OnLongClickListener() { // pinjem
																		// bTuju
			@Override
			public boolean onLongClick(View v) {
				bGoto_longClick();
				return true;
			}
		});

		bLeft.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bLeft_click();
			}
		});
		bRight.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bRight_click();
			}
		});

		bContextMenu.setOnClickListener(bContextMenu_click);

		lsText.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				int action = event.getAction();
				if (action == KeyEvent.ACTION_DOWN) {
					return press(keyCode);
				} else if (action == KeyEvent.ACTION_MULTIPLE) {
					return press(keyCode);
				}
				return false;
			}
		});

		// adapter
		verseAdapter_ = new VerseAdapter(this, parallel_click,
				new AttributeListener());
		lsText.setAdapter(verseAdapter_);

		// muat preferences_instan, dan atur renungan
		instant_pref = App.getPreferencesInstan();
		history = new History(instant_pref);
		{
			String devotion_name = instant_pref.getString(
					PREFKEY_devotion_name, null);
			if (devotion_name != null) {
				for (String nama : DevotionActivity.AVAILABLE_NAMES) {
					if (devotion_name.equals(nama)) {
						S.temporary.devotion_name = devotion_name;
					}
				}
			}
		}

		// restore the last (version; book; chapter and verse).
		String lastVersion = instant_pref.getString(PREFKEY_lastVersion, null);
		int lastBook = instant_pref.getInt(PREFKEY_lastBook, 0);
		int lastChapter = instant_pref.getInt(PREFKEY_lastChapter, 0);
		int lastVerse = instant_pref.getInt(PREFKEY_lastVerse, 0);
		Log.d(TAG,
				"Going to the last: version=" + lastVersion + " book=" + lastBook + " chapter=" + lastBook + " verse=" + lastVerse); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		loadLastVersion(lastVersion);

		{ // load book
			Book book = S.activeVersion.getBook(lastBook);
			if (book != null) {
				S.activeBook = book;
			}
		}

		// load chapter and verse
		display(lastChapter, lastVerse);

		if (Build.VERSION.SDK_INT >= 14) {
			initNfcIfAvailable();
		}

		processIntent(getIntent(), "onCreate");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		processIntent(intent, "onNewIntent");
	}

	private void processIntent(Intent intent, String via) {
		Log.d(TAG, "Got intent via " + via);
		Log.d(TAG, "  action: " + intent.getAction());
		Log.d(TAG, "  data uri: " + intent.getData());
		Log.d(TAG, "  component: " + intent.getComponent());
		Log.d(TAG, "  flags: 0x" + Integer.toHexString(intent.getFlags()));
		Log.d(TAG, "  mime: " + intent.getType());
		Bundle extras = intent.getExtras();
		Log.d(TAG, "  extras: " + (extras == null ? "null" : extras.size()));
		if (extras != null) {
			for (String key : extras.keySet()) {
				Log.d(TAG, "    " + key + " = " + extras.get(key));
			}
		}

		if (Build.VERSION.SDK_INT >= 14) {
			checkAndProcessBeamIntent(intent);
		}

		checkAndProcessViewIntent(intent);
	}

	/** did we get here from VIEW intent? */
	private void checkAndProcessViewIntent(Intent intent) {
		if (!U.equals(intent.getAction(), "yuku.alkitab.action.VIEW"))
			return;

		if (intent.hasExtra("ari")) {
			int ari = intent.getIntExtra("ari", 0);
			if (ari != 0) {
				jumpToAri(ari);
				return;
			} else {
				new AlertDialog.Builder(this).setMessage("Invalid ari: " + ari)
						.setPositiveButton(R.string.ok, null).show();
			}
		}

		if (intent.hasExtra("lid")) {
			int lid = intent.getIntExtra("lid", 0);
			int ari = LidToAri.lidToAri(lid);
			if (ari != 0) {
				jumpToAri(ari);
				return;
			} else {
				new AlertDialog.Builder(this).setMessage("Invalid lid: " + lid)
						.setPositiveButton(R.string.ok, null).show();
			}
		}
	}

	@TargetApi(14)
	private void initNfcIfAvailable() {
		nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
		if (nfcAdapter != null) {
			nfcAdapter.setNdefPushMessageCallback(
					new CreateNdefMessageCallback() {
						@Override
						public NdefMessage createNdefMessage(NfcEvent event) {
							JSONObject obj = new JSONObject();
							try {
								obj.put("ari", Ari.encode(S.activeBook.bookId, IsiActivity.this.chapter_1, IsiActivity.this.getVerseBasedOnScroll())); //$NON-NLS-1$
							} catch (JSONException e) { // won't happen
							}
							byte[] payload = obj.toString().getBytes();
							NdefRecord record = new NdefRecord(
									NdefRecord.TNF_MIME_MEDIA,
									"application/vnd.yuku.alkitab.nfc.beam".getBytes(), new byte[0], payload); //$NON-NLS-1$
							NdefMessage msg = new NdefMessage(
									new NdefRecord[] {
											record,
											NdefRecord
													.createApplicationRecord(getPackageName()), });
							return msg;
						}
					}, this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (Build.VERSION.SDK_INT >= 14) {
			disableNfcForegroundDispatchIfAvailable();
		}
	}

	@TargetApi(14)
	private void disableNfcForegroundDispatchIfAvailable() {
		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Build.VERSION.SDK_INT >= 14) {
			enableNfcForegroundDispatchIfAvailable();
		}
	}

	@TargetApi(14)
	private void enableNfcForegroundDispatchIfAvailable() {
		if (nfcAdapter != null) {
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, getClass())
							.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			IntentFilter ndef = new IntentFilter(
					NfcAdapter.ACTION_NDEF_DISCOVERED);
			try {
				ndef.addDataType("application/vnd.yuku.alkitab.nfc.beam"); //$NON-NLS-1$
			} catch (MalformedMimeTypeException e) {
				throw new RuntimeException("fail mime type", e); //$NON-NLS-1$
			}
			IntentFilter[] intentFiltersArray = new IntentFilter[] { ndef, };
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					intentFiltersArray, null);
		}
	}

	@TargetApi(14)
	private void checkAndProcessBeamIntent(Intent intent) {
		String action = intent.getAction();
		if (U.equals(action, NfcAdapter.ACTION_NDEF_DISCOVERED)) {
			Parcelable[] rawMsgs = intent
					.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			// only one message sent during the beam
			if (rawMsgs != null && rawMsgs.length > 0) {
				NdefMessage msg = (NdefMessage) rawMsgs[0];
				// record 0 contains the MIME type, record 1 is the AAR, if
				// present
				NdefRecord[] records = msg.getRecords();
				if (records != null && records.length > 0) {
					String json = new String(records[0].getPayload());
					try {
						JSONObject obj = new JSONObject(json);
						int ari = obj.optInt("ari", -1); //$NON-NLS-1$
						if (ari != -1) {
							IsiActivity.this.jumpToAri(ari);
						}
					} catch (JSONException e) {
						Log.e(TAG, "Malformed json from nfc", e); //$NON-NLS-1$
					}
				}
			}
		}
	}

	private void loadLastVersion(String lastVersion) {
		if (lastVersion == null
				|| MVersionInternal.getVersionInternalId().equals(lastVersion)) {
			return; // we are now already on internal, no need to do anything!
		}

		BuildConfig c = BuildConfig.get(this);

		// coba preset dulu!
		for (MVersionPreset preset : c.presets) { // 2. preset
			if (preset.getVersionId().equals(lastVersion)) {
				if (preset.hasDataFile()) {
					loadVersion(preset);
				} else {
					return; // this is the one that should have been chosen, but
							// the data file is not available, so let's
							// fallback.
				}
			}
		}

		// masih belum cocok, mari kita cari di daftar yes
		List<MVersionYes> yeses = S.getDb().listAllVersions();
		for (MVersionYes yes : yeses) {
			if (yes.getVersionId().equals(lastVersion)) {
				if (yes.hasDataFile()) {
					loadVersion(yes);
				} else {
					return; // this is the one that should have been chosen, but
							// the data file is not available, so let's
							// fallback.
				}
			}
		}
	}

	protected void loadVersion(final MVersion mv) {
		// for rollback
		Version oldActiveVersion = S.activeVersion;
		String oldActiveVersionId = S.activeVersionId;

		boolean success = false;
		try {
			Version version = mv.getVersion(getApplicationContext());

			if (version != null) {
				S.activeVersion = version;
				S.activeVersionId = mv.getVersionId();
				S.prepareBook();

				Book book = S.activeVersion.getBook(S.activeBook.bookId);
				if (book != null) {
					// assign active book with the new one, no need to consider
					// the pos
					S.activeBook = book;
				} else {
					S.activeBook = S.activeVersion.getFirstBook(); // too bad,
																	// it was
																	// not found
				}

				display(chapter_1, getVerseBasedOnScroll());
			} else {
				new AlertDialog.Builder(IsiActivity.this)
						.setMessage(
								getString(
										R.string.ada_kegagalan_membuka_edisiid,
										mv.getVersionId()))
						.setPositiveButton(R.string.ok, null).show();
			}
			success = true;
		} catch (Throwable e) { // so we don't crash on the beginning of the app
			Log.e(TAG, "failed in loadVersion", e); //$NON-NLS-1$
		} finally {
			if (!success) {
				S.activeVersion = oldActiveVersion;
				S.activeVersionId = oldActiveVersionId;
				S.prepareBook();
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		hideOrShowContextMenuButton();
	}

	void showContextMenuButton() {
		if (!showingContextMenuButton) {
			if (fadeInAnimation == null) {
				fadeInAnimation = AnimationUtils.loadAnimation(this,
						android.R.anim.fade_in);
			}
			bContextMenu.setVisibility(View.VISIBLE);
			bContextMenu.startAnimation(fadeInAnimation);
			bContextMenu.setEnabled(true);
			showingContextMenuButton = true;
		}
	}

	void hideContextButton() {
		if (showingContextMenuButton) {
			if (fadeOutAnimation == null) {
				fadeOutAnimation = AnimationUtils.loadAnimation(this,
						android.R.anim.fade_out);
			}
			fadeOutAnimation.setAnimationListener(fadeOutAnimation_animation);
			bContextMenu.startAnimation(fadeOutAnimation);
			bContextMenu.setEnabled(false);
			showingContextMenuButton = false;
		}
	}

	private AnimationListener fadeOutAnimation_animation = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			bContextMenu.setVisibility(View.INVISIBLE);
		}
	};

	protected boolean press(int keyCode) {
		String volumeButtonsForNavigation = Preferences.getString(
				getString(R.string.pref_tombolVolumeBuatPindah_key),
				getString(R.string.pref_tombolVolumeBuatPindah_default));
		if (U.equals(volumeButtonsForNavigation, "pasal" /* chapter */)) { //$NON-NLS-1$
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				keyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				keyCode = KeyEvent.KEYCODE_DPAD_LEFT;
		} else if (U.equals(volumeButtonsForNavigation, "ayat" /* verse */)) { //$NON-NLS-1$
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				keyCode = KeyEvent.KEYCODE_DPAD_DOWN;
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				keyCode = KeyEvent.KEYCODE_DPAD_UP;
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			bLeft_click();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			bRight_click();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			int oldPos = getPositionBasedOnScroll();
			if (oldPos < verseAdapter_.getCount() - 1) {
				lsText.setSelectionFromTop(oldPos + 1,
						lsText.getVerticalFadingEdgeLength());
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			int oldPos = getPositionBasedOnScroll();
			if (oldPos >= 1) {
				int newPos = oldPos - 1;
				while (newPos > 0) { // cek disabled, kalo iya, mundurin lagi
					if (verseAdapter_.isEnabled(newPos))
						break;
					newPos--;
				}
				lsText.setSelectionFromTop(newPos,
						lsText.getVerticalFadingEdgeLength());
			} else {
				lsText.setSelectionFromTop(0,
						lsText.getVerticalFadingEdgeLength());
			}
			return true;
		}
		return false;
	}

	private synchronized void keepScreenOnWhenRequested() {
		if (Preferences.getBoolean(
				getString(R.string.pref_nyalakanTerusLayar_key), getResources()
						.getBoolean(R.bool.pref_nyalakanTerusLayar_default))) {
			lsText.setKeepScreenOn(true);
		}
	}

	private synchronized void turnScreenOffIfAllowedToTurnOff() {
		lsText.setKeepScreenOn(false);
	}

	/**
	 * Jump to a given verse reference in string format.
	 * 
	 * @return ari of the parsed reference
	 */
	int jumpTo(String reference) {
		if (reference.trim().length() == 0) {
			return 0;
		}

		Log.d(TAG, "going to jump to " + reference); //$NON-NLS-1$

		Jumper jumper = new Jumper();
		boolean success = jumper.parse(reference);
		if (!success) {
			Toast.makeText(this,
					getString(R.string.alamat_tidak_sah_alamat, reference),
					Toast.LENGTH_SHORT).show();
			return 0;
		}

		int bookId = jumper.getBookId(S.activeVersion.getConsecutiveBooks());
		Book selected;
		if (bookId != -1) {
			Book book = S.activeVersion.getBook(bookId);
			if (book != null) {
				selected = book;
			} else {
				// not avail, just fallback
				selected = S.activeBook;
			}
		} else {
			selected = S.activeBook;
		}

		// set book
		S.activeBook = selected;

		int chapter = jumper.getChapter();
		int verse = jumper.getVerse();
		int ari_cv;
		if (chapter == -1 && verse == -1) {
			ari_cv = display(1, 1);
		} else {
			ari_cv = display(chapter, verse);
		}

		return Ari.encode(selected.bookId, ari_cv);
	}

	/**
	 * Jump to a given ari
	 */
	void jumpToAri(int ari) {
		if (ari == 0)
			return;

		Log.d(TAG, "will jump to ari 0x" + Integer.toHexString(ari)); //$NON-NLS-1$

		int bookId = Ari.toBook(ari);
		Book book = S.activeVersion.getBook(bookId);

		if (book != null) {
			S.activeBook = book;
		} else {
			Log.w(TAG, "bookId=" + bookId + " not found for ari=" + ari); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}

		display(Ari.toChapter(ari), Ari.toVerse(ari));
	}

	private OnItemClickListener lsText_itemClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			verseAdapter_.notifyDataSetChanged();
			hideOrShowContextMenuButton();
		}
	};

	void hideOrShowContextMenuButton() {
		SparseBooleanArray checkedPositions = lsText.getCheckedItemPositions();
		boolean anyChecked = false;
		for (int i = 0; i < checkedPositions.size(); i++)
			if (checkedPositions.valueAt(i)) {
				anyChecked = true;
				break;
			}

		if (anyChecked) {
			showContextMenuButton();
		} else {
			hideContextButton();
		}
	}

	private IntArrayList getSelectedVerses_1() {
		// count how many are selected
		SparseBooleanArray positions = lsText.getCheckedItemPositions();
		if (positions == null) {
			return new IntArrayList(0);
		}

		IntArrayList res = new IntArrayList(positions.size());
		for (int i = 0, len = positions.size(); i < len; i++) {
			if (positions.valueAt(i)) {
				int position = positions.keyAt(i);
				int verse_1 = verseAdapter_.getVerseFromPosition(position);
				if (verse_1 >= 1)
					res.add(verse_1);
			}
		}
		return res;
	}

	private CharSequence referenceFromSelectedVerses(IntArrayList selectedVerses) {
		if (selectedVerses.size() == 0) {
			// should not be possible. So we don't do anything.
			return S.reference(S.activeBook, this.chapter_1);
		} else if (selectedVerses.size() == 1) {
			return S.reference(S.activeBook, this.chapter_1,
					selectedVerses.get(0));
		} else {
			return S.reference(S.activeBook, this.chapter_1, selectedVerses);
		}
	}

	static class FakeContextMenu {
		static class Item {
			String label;

			Item(String label) {
				this.label = label;
			}
		}

		Item menuCopyVerse;
		Item menuAddBookmark;
		Item menuAddNote;
		Item menuAddHighlight;
		Item menuShare;
		Item menuEsvsbasal;
		List<Item> items;

		String[] getLabels() {
			String[] res = new String[items.size()];
			for (int i = 0, len = items.size(); i < len; i++) {
				res[i] = items.get(i).label;
			}
			return res;
		}
	}

	FakeContextMenu getFakeContextMenu() {
		FakeContextMenu res = new FakeContextMenu();
		res.menuCopyVerse = new Item(getString(R.string.salin_ayat));
		res.menuAddBookmark = new Item(getString(R.string.tambah_pembatas_buku));
		res.menuAddNote = new Item(getString(R.string.tulis_catatan));
		res.menuAddHighlight = new Item(getString(R.string.highlight_stabilo));
		res.menuShare = new Item(getString(R.string.bagikan));

		/*
		 * The following "esvsbasal" thing is a personal thing by yuku that
		 * doesn't matter to anyone else. Please ignore it and leave it intact.
		 */
		if (hasEsvsbAsal == null) {
			try {
				getPackageManager().getApplicationInfo("yuku.esvsbasal", 0); //$NON-NLS-1$
				hasEsvsbAsal = true;
			} catch (NameNotFoundException e) {
				hasEsvsbAsal = false;
			}
		}

		if (hasEsvsbAsal) {
			res.menuEsvsbasal = new Item("ESV Study Bible"); //$NON-NLS-1$
		}

		ArrayList<Item> items = new ArrayList<Item>(6);
		items.add(res.menuCopyVerse);
		items.add(res.menuAddBookmark);
		items.add(res.menuAddNote);
		items.add(res.menuAddHighlight);
		items.add(res.menuShare);

		if (hasEsvsbAsal) {
			items.add(res.menuEsvsbasal);
		}
		res.items = items;

		return res;
	};

	public void showFakeContextMenu() {
		IntArrayList selected = getSelectedVerses_1();
		if (selected.size() == 0)
			return;

		// creating the menu manually
		final FakeContextMenu menu = getFakeContextMenu();

		new AlertDialog.Builder(this)
				.setTitle(referenceFromSelectedVerses(selected))
				.setItems(menu.getLabels(),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (which >= 0 && which < menu.items.size()) {
									onFakeContextMenuSelected(menu,
											menu.items.get(which));
								}
							}
						}).show();
	}

	public void onFakeContextMenuSelected(FakeContextMenu menu, Item item) {
		IntArrayList selected = getSelectedVerses_1();
		if (selected.size() == 0)
			return;

		CharSequence reference = referenceFromSelectedVerses(selected);

		// the main verse (0 if not exist), which is only when only one verse is
		// selected
		int mainVerse_1 = 0;
		if (selected.size() == 1) {
			mainVerse_1 = selected.get(0);
		}

		if (item == menu.menuCopyVerse) { // copy, can be multiple
			StringBuilder textToCopy = new StringBuilder();
			textToCopy.append(reference).append("  "); //$NON-NLS-1$

			// append each selected verse
			for (int i = 0; i < selected.size(); i++) {
				int verse_1 = selected.get(i);
				if (i != 0)
					textToCopy.append('\n');
				textToCopy.append(U.removeSpecialCodes(verseAdapter_
						.getVerse(verse_1)));
			}

			U.copyToClipboard(textToCopy);
			uncheckAll();

			Toast.makeText(this,
					getString(R.string.alamat_sudah_disalin, reference),
					Toast.LENGTH_SHORT).show();
		} else if (item == menu.menuAddBookmark) {
			if (mainVerse_1 == 0) {
				// no main verse, scroll to show the relevant one!
				mainVerse_1 = selected.get(0);

				scrollToShowVerse(mainVerse_1);
			}

			final int ari = Ari.encode(S.activeBook.bookId, this.chapter_1,
					mainVerse_1);

			TypeBookmarkDialog dialog = new TypeBookmarkDialog(this,
					S.reference(S.activeBook, this.chapter_1, mainVerse_1), ari);
			dialog.setListener(new TypeBookmarkDialog.Listener() {
				@Override
				public void onOk() {
					uncheckAll();
					verseAdapter_.loadAttributeMap();
				}
			});
			dialog.show();
		} else if (item == menu.menuAddNote) {
			if (mainVerse_1 == 0) {
				// no main verse, scroll to show the relevant one!
				mainVerse_1 = selected.get(0);

				scrollToShowVerse(mainVerse_1);
			}

			TypeNoteDialog dialog = new TypeNoteDialog(IsiActivity.this,
					S.activeBook, this.chapter_1, mainVerse_1,
					new TypeNoteDialog.RefreshCallback() {
						@Override
						public void onDone() {
							uncheckAll();
							verseAdapter_.loadAttributeMap();
						}
					});
			dialog.show();
		} else if (item == menu.menuAddHighlight) {
			final int ariKp = Ari
					.encode(S.activeBook.bookId, this.chapter_1, 0);
			int warnaRgb = S.getDb().getWarnaRgbStabilo(ariKp, selected);

			new TypeHighlightDialog(this, ariKp, selected,
					new TypeHighlightDialog.JenisStabiloCallback() {
						@Override
						public void onOk(int warnaRgb) {
							uncheckAll();
							verseAdapter_.loadAttributeMap();
						}
					}, warnaRgb, reference).bukaDialog();
		} else if (item == menu.menuShare) {
			StringBuilder sb = new StringBuilder();
			sb.append(reference).append("  "); //$NON-NLS-1$

			// append each selected verse
			for (int i = 0; i < selected.size(); i++) {
				int verse_1 = selected.get(i);
				if (i != 0)
					sb.append('\n');
				sb.append(U.removeSpecialCodes(verseAdapter_.getVerse(verse_1)));
			}

			String verseUrl;
			if (selected.size() == 1) {
				verseUrl = S.createVerseUrl(S.activeBook, this.chapter_1,
						String.valueOf(selected.get(0)));
			} else {
				StringBuilder sb2 = new StringBuilder();
				S.writeVerseRange(selected, sb2);
				verseUrl = S.createVerseUrl(S.activeBook, this.chapter_1,
						sb2.toString()); // use verse range
			}

			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain"); //$NON-NLS-1$
			intent.putExtra(Intent.EXTRA_SUBJECT, reference);
			intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
			intent.putExtra(EXTRA_verseUrl, verseUrl);
			startActivityForResult(
					ShareActivity.createIntent(intent,
							getString(R.string.bagikan_alamat, reference)),
					REQCODE_share);

			uncheckAll();
		} else if (item == menu.menuEsvsbasal) {
			final int ari = Ari.encode(S.activeBook.bookId, this.chapter_1,
					mainVerse_1);

			try {
				Intent intent = new Intent("yuku.esvsbasal.action.GOTO"); //$NON-NLS-1$
				intent.putExtra("ari", ari); //$NON-NLS-1$
				startActivity(intent);
			} catch (Exception e) {
				Log.e(TAG, "ESVSB starting", e); //$NON-NLS-1$
			}
		}
	}

	private void scrollToShowVerse(int mainVerse_1) {
		int position = verseAdapter_
				.getPositionOfPericopeBeginningFromVerse(mainVerse_1);
		if (Build.VERSION.SDK_INT >= 8) {
			Api8.ListView_smoothScrollToPosition(lsText, position);
		} else {
			lsText.setSelectionFromTop(position,
					lsText.getVerticalFadingEdgeLength());
		}
	}

	private void applyPreferences(boolean languageToo) {
		// appliance of background color
		{
			root.setBackgroundColor(S.applied.backgroundColor);
			lsText.setCacheColorHint(S.applied.backgroundColor);

			// on Holo theme, the button background is quite transparent, so we
			// need to adjust button text color
			// to dark one if user chooses to use a light background color.
			if (Build.VERSION.SDK_INT >= 11) {
				if (S.applied.backgroundBrightness > 0.7f) {
					bGoto.setTextColor(0xff000000); // black
				} else {
					bGoto.setTextColor(0xfff3f3f3); // default button text color
													// on Holo
				}
			}
		}

		// appliance of hide navigation
		{
			View navigationPanel = findViewById(R.id.panelNavigasi);
			if (Preferences.getBoolean(
					getString(R.string.pref_tanpaNavigasi_key), getResources()
							.getBoolean(R.bool.pref_tanpaNavigasi_default))) {
				navigationPanel.setVisibility(View.GONE);
				titleContainer.setVisibility(View.VISIBLE);
			} else {
				navigationPanel.setVisibility(View.VISIBLE);
				titleContainer.setVisibility(View.GONE);
			}
		}

		if (languageToo) {
			S.applyLanguagePreference(null, 0);
		}

		// necessary
		lsText.invalidateViews();
	}

	@Override
	protected void onStop() {
		super.onStop();

		Editor editor = instant_pref.edit();
		editor.putInt(PREFKEY_lastBook, S.activeBook.bookId);
		editor.putInt(PREFKEY_lastChapter, chapter_1);
		editor.putInt(PREFKEY_lastVerse, getVerseBasedOnScroll());
		editor.putString(PREFKEY_devotion_name, S.temporary.devotion_name);
		editor.putString(PREFKEY_lastVersion, S.activeVersionId);
		history.simpan(editor);
		editor.commit();

		turnScreenOffIfAllowedToTurnOff();
	}

	@Override
	protected void onStart() {
		super.onStart();

		keepScreenOnWhenRequested();
	}

	/**
	 * @return 1-based verse
	 */
	int getVerseBasedOnScroll() {
		return verseAdapter_.getVerseFromPosition(getPositionBasedOnScroll());
	}

	int getPositionBasedOnScroll() {
		int pos = lsText.getFirstVisiblePosition();

		// check if the top one has been scrolled
		View child = lsText.getChildAt(0);
		if (child != null) {
			int top = child.getTop();
			if (top == 0) {
				return pos;
			}
			int bottom = child.getBottom();
			if (bottom > lsText.getVerticalFadingEdgeLength()) {
				return pos;
			} else {
				return pos + 1;
			}
		}

		return pos;
	}

	void bGoto_click() {
		startActivityForResult(GotoActivity.createIntent(S.activeBook.bookId,
				this.chapter_1, getVerseBasedOnScroll()), REQCODE_goto);
	}

	void bGoto_longClick() {
		if (history.getN() > 0) {
			new AlertDialog.Builder(this)
					.setAdapter(historyAdapter,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									int ari = history.getAri(which);
									jumpToAri(ari);
									history.add(ari);
								}
							}).setNegativeButton(R.string.cancel, null).show();
		} else {
			Toast.makeText(this, R.string.belum_ada_sejarah, Toast.LENGTH_SHORT)
					.show();
		}
	}

	private ListAdapter historyAdapter = new BaseAdapter() {
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView res = (TextView) convertView;
			if (res == null) {
				res = (TextView) LayoutInflater.from(IsiActivity.this).inflate(
						android.R.layout.select_dialog_item, null);
			}
			int ari = history.getAri(position);
			res.setText(S.reference(S.activeVersion, ari));
			return res;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Integer getItem(int position) {
			return history.getAri(position);
		}

		@Override
		public int getCount() {
			return history.getN();
		}
	};

	public void openDonationDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.donasi_judul)
				.setMessage(R.string.donasi_keterangan)
				.setPositiveButton(R.string.donasi_tombol_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String donation_url = getString(R.string.alamat_donasi);
								Intent intent = new Intent(Intent.ACTION_VIEW,
										Uri.parse(donation_url));
								startActivity(intent);
							}
						})
				.setNegativeButton(R.string.donasi_tombol_gamau, null).show();
	}

	public void buildMenu(Menu menu) {
		menu.clear();
		getSupportMenuInflater().inflate(R.menu.activity_isi, menu);

		BuildConfig c = BuildConfig.get(this);

		if (c.menuGebug) {
			// SubMenu menuGebug = menu.addSubMenu(R.string.gebug);
			// menuGebug.add(0, 0x985801, 0, "gebug 1: dump p+p"); //$NON-NLS-1$
		}

		// # build config
		menu.findItem(R.id.menuRenungan).setVisible(c.menuDevotion);
		menu.findItem(R.id.menuEdisi).setVisible(c.menuVersions);
		menu.findItem(R.id.menuBantuan).setVisible(c.menuHelp);
		menu.findItem(R.id.menuDonasi).setVisible(c.menuDonation);
		menu.findItem(R.id.menuSongs).setVisible(c.menuSongs);
		menu.findItem(R.id.menuAudio).setVisible(c.menuAudio);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		buildMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (menu != null) {
			buildMenu(menu);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuBukmak:
			startActivityForResult(new Intent(this, BookmarkActivity.class),
					REQCODE_bookmark);
			return true;
		case R.id.menuSearch2:
			menuSearch2_click();
			return true;
		case R.id.menuEdisi:
			bukaDialogEdisi();
			return true;
		case R.id.menuRenungan:
			startActivityForResult(new Intent(this, DevotionActivity.class),
					REQCODE_devotion);
			return true;
		case R.id.menuSongs:
			startActivityForResult(SongViewActivity.createIntent(),
					REQCODE_songs);
			return true;
		case R.id.menuAudio:
			if (aPlayer.isShown())
				aPlayer.setVisibility(View.GONE);
			else
				aPlayer.setVisibility(View.VISIBLE);

			Log.d("Audio", "" + S.activeVersionId);
			/*
			 * String url = AlkitabAudio.getAudioURL(S.activeBook, chapter_1);
			 * 
			 * Intent intent = new Intent(this, AudioActivity.class);
			 * intent.putExtra("url", url); startActivity(intent);
			 */
			return true;
		case R.id.menuTentang:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		case R.id.menuPengaturan:
			startActivityForResult(new Intent(this, SettingsActivity.class),
					REQCODE_settings);
			return true;
		case R.id.menuBantuan:
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		case R.id.menuDonasi:
			openDonationDialog();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void bukaDialogEdisi() {
		// populate with
		// 1. internal
		// 2. presets that have been DOWNLOADED and ACTIVE
		// 3. yeses that are ACTIVE

		BuildConfig c = BuildConfig.get(this);
		final List<String> options = new ArrayList<String>(); // sync with below
																// line
		final List<MVersion> data = new ArrayList<MVersion>(); // sync with
																// above line

		options.add(c.internalLongName); // 1. internal
		data.add(new MVersionInternal());

		for (MVersionPreset preset : c.presets) { // 2. preset
			if (preset.hasDataFile() && preset.getActive()) {
				options.add(preset.longName);
				data.add(preset);
			}
		}

		// 3. active yeses
		List<MVersionYes> yeses = S.getDb().listAllVersions();
		for (MVersionYes yes : yeses) {
			if (yes.hasDataFile() && yes.getActive()) {
				options.add(yes.longName);
				data.add(yes);
			}
		}

		int selected = -1;
		if (S.activeVersionId == null) {
			selected = 0;
		} else {
			for (int i = 0; i < data.size(); i++) {
				MVersion mv = data.get(i);
				if (mv.getVersionId().equals(S.activeVersionId)) {
					selected = i;
					break;
				}
			}
		}

		new AlertDialog.Builder(this)
				.setTitle(R.string.pilih_edisi)
				.setSingleChoiceItems(
						options.toArray(new String[options.size()]), selected,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final MVersion mv = data.get(which);

								loadVersion(mv);
								dialog.dismiss();
							}
						})
				.setPositiveButton(R.string.versi_lainnya,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Intent intent = new Intent(
										getApplicationContext(),
										VersionsActivity.class);
								startActivityForResult(intent, REQCODE_version);
							}
						}).setNegativeButton(R.string.cancel, null).show();
	}

	private void menuSearch2_click() {
		startActivityForResult(
				Search2Activity.createIntent(search2_query, search2_results,
						search2_selectedPosition, S.activeBook.bookId),
				REQCODE_search);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQCODE_goto) {
			if (resultCode == RESULT_OK) {
				GotoActivity.Result result = GotoActivity.obtainResult(data);
				if (result != null) {
					// change book
					Book book = S.activeVersion.getBook(result.bookId);
					if (book != null) {
						S.activeBook = book;
					} else { // no book, just chapter and verse.
						result.bookId = S.activeBook.bookId;
					}

					int ari_cv = display(result.chapter_1, result.verse_1);
					history.add(Ari.encode(result.bookId, ari_cv));
				}
			}
		} else if (requestCode == REQCODE_bookmark) {
			verseAdapter_.loadAttributeMap();

			if (resultCode == RESULT_OK) {
				int ari = data.getIntExtra(BookmarkActivity.EXTRA_ariTerpilih,
						0);
				if (ari != 0) { // 0 means nothing, because we don't have
								// chapter 0 verse 0
					jumpToAri(ari);
					history.add(ari);
				}
			}
		} else if (requestCode == REQCODE_search) {
			if (resultCode == RESULT_OK) {
				Search2Activity.Result result = Search2Activity
						.obtainResult(data);
				if (result != null) {
					if (result.ariTerpilih != -1) {
						jumpToAri(result.ariTerpilih);
						history.add(result.ariTerpilih);
					}

					search2_query = result.query;
					search2_results = result.hasilCari;
					search2_selectedPosition = result.posisiTerpilih;
				}
			}
		} else if (requestCode == REQCODE_devotion) {
			if (data != null) {
				String alamat = data
						.getStringExtra(DevotionActivity.EXTRA_alamat);
				if (alamat != null) {
					int ari = jumpTo(alamat);
					if (ari != 0) {
						history.add(ari);
					}
				}
			}
		} else if (requestCode == REQCODE_songs) {
			if (resultCode == SongViewActivity.RESULT_gotoScripture
					&& data != null) {
				String ref = data.getStringExtra(SongViewActivity.EXTRA_ref);
				if (ref != null) { // TODO
					int ari = jumpTo(ref);
					if (ari != 0) {
						history.add(ari);
					}
				}
			}
		} else if (requestCode == REQCODE_settings) {
			// MUST reload preferences
			S.calculateAppliedValuesBasedOnPreferences();

			applyPreferences(true);
		} else if (requestCode == REQCODE_share) {
			if (resultCode == RESULT_OK) {
				ShareActivity.Result result = ShareActivity.obtainResult(data);
				if (result != null && result.chosenIntent != null) {
					Intent chosenIntent = result.chosenIntent;
					if (U.equals(chosenIntent.getComponent().getPackageName(),
							"com.facebook.katana")) { //$NON-NLS-1$
						String verseUrl = chosenIntent
								.getStringExtra(EXTRA_verseUrl);
						if (verseUrl != null) {
							chosenIntent.putExtra(Intent.EXTRA_TEXT, verseUrl); // change
																				// text
																				// to
																				// url
						}
					}
					startActivity(chosenIntent);
				}
			}
		}
	}

	/**
	 * @return Ari that contains only chapter and verse. Book always set to 0.
	 */
	int display(int chapter_1, int verse_1) {
		if (chapter_1 < 1)
			chapter_1 = 1;
		if (chapter_1 > S.activeBook.nchapter)
			chapter_1 = S.activeBook.nchapter;

		if (verse_1 < 1)
			verse_1 = 1;
		if (verse_1 > S.activeBook.nverses[chapter_1 - 1])
			verse_1 = S.activeBook.nverses[chapter_1 - 1];

		// loading data no need to use async. // 20100417 updated to not use
		// async, it's not useful.
		{
			int[] pericope_aris;
			PericopeBlock[] pericope_blocks;
			int nblock;

			String[] verses = S.loadChapterText(S.activeVersion, S.activeBook,
					chapter_1);

			// # max is set to 30 (one chapter has max of 30 blocks. Already
			// almost impossible)
			int max = 30;
			pericope_aris = new int[max];
			pericope_blocks = new PericopeBlock[max];
			nblock = S.activeVersion.reader.loadPericope(S.activeVersion,
					S.activeBook.bookId, chapter_1, pericope_aris,
					pericope_blocks, max);

			// # fill adapter with new data. make sure all checked states are
			// reset
			uncheckAll();
			verseAdapter_.setData(S.activeBook, chapter_1, verses,
					pericope_aris, pericope_blocks, nblock);
			verseAdapter_.loadAttributeMap();

			// tell activity
			this.chapter_1 = chapter_1;

			final int position = verseAdapter_
					.getPositionOfPericopeBeginningFromVerse(verse_1);

			if (position == -1) {
				Log.w(TAG, "could not find verse=" + verse_1 + ", weird!"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				lsText.setSelectionFromTop(position,
						lsText.getVerticalFadingEdgeLength());
			}
		}

		String title = S.reference(S.activeBook, chapter_1);
		lTitle.setText(title);
		bGoto.setText(title);

		// reset mediaplayer
		playPause.setEnabled(true);
		if (mediaPlayer == null) {
			Log.d("info", "MediaPlayer Null!!");
			// createMP();
		} else {
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.stop();
			}
			isInterrupted = true;
			seek.setSecondaryProgress(0);
			seek.setProgress(0);

			tProgress.setText("0.0 Kb");
			playPause.setImageResource(R.drawable.button_play);
			mediaPlayer.reset();
			// mediaPlayer.release();
			mpSet = false;

		}

		return Ari.encode(0, chapter_1, verse_1);
	}

	void uncheckAll() {
		SparseBooleanArray checkedPositions = lsText.getCheckedItemPositions();
		if (checkedPositions != null && checkedPositions.size() > 0) {
			for (int i = checkedPositions.size() - 1; i >= 0; i--) {
				if (checkedPositions.valueAt(i)) {
					lsText.setItemChecked(checkedPositions.keyAt(i), false);
				}
			}
		}
		hideContextButton();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (press(keyCode))
			return true;
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		if (press(keyCode))
			return true;
		return super.onKeyMultiple(keyCode, repeatCount, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		String volumeButtonsForNavigation = Preferences.getString(
				getString(R.string.pref_tombolVolumeBuatPindah_key),
				getString(R.string.pref_tombolVolumeBuatPindah_default));
		if (!U.equals(volumeButtonsForNavigation, "default")) { // consume here //$NON-NLS-1$
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				return true;
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
				return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	void bLeft_click() {
		Book currentBook = S.activeBook;
		if (chapter_1 == 1) {
			// we are in the beginning of the book, so go to prev book
			int tryBookId = currentBook.bookId - 1;
			while (tryBookId >= 0) {
				Book newBook = S.activeVersion.getBook(tryBookId);
				if (newBook != null) {
					S.activeBook = newBook;
					int newChapter_1 = newBook.nchapter; // to the last chapter
					display(newChapter_1, 1);
					break;
				}
				tryBookId--;
			}
			// whileelse: now is already Genesis 1. No need to do anything
		} else {
			int newChapter = chapter_1 - 1;
			display(newChapter, 1);
		}
	}

	void bRight_click() {
		Book currentBook = S.activeBook;
		if (chapter_1 >= currentBook.nchapter) {
			int maxBookId = S.activeVersion.getMaxBookIdPlusOne();
			int tryBookId = currentBook.bookId + 1;
			while (tryBookId < maxBookId) {
				Book newBook = S.activeVersion.getBook(tryBookId);
				if (newBook != null) {
					S.activeBook = newBook;
					display(1, 1);
					break;
				}
				tryBookId++;
			}
			// whileelse: now is already Revelation (or the last book) at the
			// last chapter. No need to do anything
		} else {
			int newChapter = chapter_1 + 1;
			display(newChapter, 1);
		}
	}

	private OnClickListener bContextMenu_click = new OnClickListener() {
		@Override
		public void onClick(View v) {
			showFakeContextMenu();
		}
	};

	@Override
	public boolean onSearchRequested() {
		menuSearch2_click();

		return true;
	}

	public class AttributeListener {
		public void onClick(Book book, int chapter_1, int verse_1, int kind) {
			if (kind == Db.Bukmak2.kind_bookmark) {
				final int ari = Ari.encode(book.bookId, chapter_1, verse_1);
				String alamat = S.reference(S.activeVersion, ari);
				TypeBookmarkDialog dialog = new TypeBookmarkDialog(
						IsiActivity.this, alamat, ari);
				dialog.setListener(new TypeBookmarkDialog.Listener() {
					@Override
					public void onOk() {
						verseAdapter_.loadAttributeMap();
					}
				});
				dialog.show();
			} else if (kind == Db.Bukmak2.kind_note) {
				TypeNoteDialog dialog = new TypeNoteDialog(IsiActivity.this,
						book, chapter_1, verse_1,
						new TypeNoteDialog.RefreshCallback() {
							@Override
							public void onDone() {
								verseAdapter_.loadAttributeMap();
							}
						});
				dialog.show();
			}
		}
	}

	// audio player
	/**
	 * Method which updates the SeekBar primary progress by current song playing
	 * position
	 */
	private void primarySeekBarProgressUpdater() {
		if (mediaPlayer != null) {
			mediaFileLength = mediaPlayer.getDuration();

			seek.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLength) * 100));
		}

		if (mediaPlayer.isPlaying()) {
			Runnable notification = new Runnable() {
				public void run() {
					primarySeekBarProgressUpdater();
				}
			};
			handler.postDelayed(notification, 1000);
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		/**
		 * Method which updates the SeekBar secondary progress by current song
		 * loading from URL position
		 */
		seek.setSecondaryProgress(percent);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		/**
		 * MediaPlayer onCompletion event handler. Method which calls then song
		 * playing is complete
		 */
		playPause.setImageResource(R.drawable.button_play);
		seek.setSecondaryProgress(0);
		seek.setProgress(0);
		primarySeekBarProgressUpdater();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.SeekBarPlay) {
			/**
			 * Seekbar onTouch event handler. Method which seeks MediaPlayer to
			 * seekBar primary progress position
			 */
			// if(mediaPlayer.isPlaying()){
			SeekBar sb = (SeekBar) v;
			int playPositionInMillisecconds = (mediaFileLength / 100)
					* sb.getProgress();
			mediaPlayer.seekTo(playPositionInMillisecconds);
			// }
		}
		return false;
	}

	private class mAsync extends AsyncTask<String, Void, Boolean>{		

		@Override
		protected Boolean doInBackground(String... url) {
			// TODO Auto-generated method stub
			boolean stat;
			try {	
				long startTime = System.currentTimeMillis();
				
				URL u = new URL(url[0]);
				HttpURLConnection huc = (HttpURLConnection) u.openConnection();
				huc.setRequestMethod("GET");
				huc.connect();
				int resp = huc.getResponseCode();
				if (resp == HttpURLConnection.HTTP_OK) {
					stat = true;
				} else {
					stat = false;
				}
				Log.d("info", "resp " + resp + " " + stat);
				long endTime = System.currentTimeMillis() - startTime;	
				Log.d("info", "time " + endTime);
				
				return stat;										
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}			
		}
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			final String title = S.reference(S.activeBook, chapter_1);
			
			if (result == true){
				Log.d("info", "true");
				startStream();
			} else {
				Log.d("info", "Tidak Bisa");
				new AlertDialog.Builder(IsiActivity.this).setTitle("Audio")
						.setMessage("Audio tidak tersedia untuk " + title)
						.setPositiveButton("OK", null).show();
			}
		}				
	}

	public void startStream() {
		final String url = AlkitabAudio.getAudioURL(S.activeBook, chapter_1);
		
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.d("info", "Running!");
				createFile(url);
			}
		};
		new Thread(r).start();

	}	

	public void bStart_onClick(View v) throws IOException {
		Log.d("info", "" + mpSet);
		if (mpSet == false) {
			String url = AlkitabAudio.getAudioURL(S.activeBook, chapter_1);
			new mAsync().execute(url);						
			isInterrupted = false;
			playPause.setEnabled(false);
		} else {
			if (!mediaPlayer.isPlaying()) {
				mediaPlayer.start();
				playPause.setImageResource(R.drawable.button_pause);
			} else {
				mediaPlayer.pause();
				playPause.setImageResource(R.drawable.button_play);
			}
		}
	}

	public void createFile(String url) {
		try {
			SimpleHttpConnection conn = new SimpleHttpConnection(url);

			InputStream content = conn.load();

			if (content == null) {
				Log.e(getClass().getName(),
						"Unable to create InputStream for mediaUrl:" + url);

			}
			// long fileSize = entity.getContentLength();

			mediaLengthInKb = conn.getContentLength() / 1024;

			// create download file
			downloadFile = new File(Environment.getExternalStorageDirectory(),
					"bible/new/downloaded" + (counter++) + ".mp3");

			// if (downloadFile.exists()) downloadFile.delete();

			FileOutputStream out = new FileOutputStream(downloadFile);

			// read from URL
			byte buf[] = new byte[16384];
			int totalBytesRead = 0;
			int incrementBytes = 0;

			mpSet = true;
			
			while (notInterrupted()) {
				nread = content.read(buf);
				if (nread <= 0)
					break;
				out.write(buf, 0, nread);

				totalBytesRead += nread;
				incrementBytes += nread;
				totalKbRead = totalBytesRead / 1024;

				testMediaBuffer();
				fireDataLoadUpdate();
			}

			// fireDataFullyLoaded();
			content.close();
			out.close();
			if (notInterrupted()) {
				fireDataFullyLoaded();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d("Audio", "" + e);
		}

	}

	private boolean notInterrupted() {
		if (isInterrupted) {
			if (mediaPlayer != null) {
				mediaPlayer.pause();
				// mediaPlayer.release();
			}
			Log.d("info", "interrupted!!");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Test whether we need to transfer buffered data to the MediaPlayer.
	 * Interacting with MediaPlayer on non-main UI thread can causes crashes to
	 * so perform this using a Handler.
	 */
	private void testMediaBuffer() {
		Runnable updater = new Runnable() {
			public void run() {
				Log.d("info ", "" + totalKbRead);				
				if (mediaPlayer == null) {
					// Only create the MediaPlayer once we have the minimum
					// buffered data
					if (totalKbRead >= INTIAL_KB_BUFFER) {
						try {
							startMediaPlayer();
						} catch (Exception e) {
							Log.e("info", "Error copying buffered conent.", e);
						}
					}
				} else if (mediaPlayer.getDuration()
						- mediaPlayer.getCurrentPosition() <= 1000) {
					// NOTE: The media player has stopped at the end so transfer
					// any existing buffered data
					// We test for < 1second of data because the media player
					// can stop when there is still
					// a few milliseconds of data left to play
					transferBufferToMediaPlayer();
				} else {
					
					Log.d("info ", "" + totalKbRead);
				}
			}
		};
		handler.post(updater);
	}

	private void startMediaPlayer() {
		try {
			File bufferedFile = new File(getExternalCacheDir(), "buf_"
					+ (counter++) + ".mp3");

			moveFile(downloadFile, bufferedFile);

			Log.e(getClass().getName(),
					"Buffered File path: " + bufferedFile.getAbsolutePath());
			Log.e(getClass().getName(),
					"Buffered File length: " + bufferedFile.length() + "");

			// We have pre-loaded enough content and started the MediaPlayer so
			// update the buttons & progress meters.

			mediaPlayer = createMediaPlayer(bufferedFile.getAbsolutePath());

			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.prepare();

			// mediaPlayer.start();

			// primarySeekBarProgressUpdater();

			// fireDataPreloadComplete();

		} catch (IOException e) {
			Log.e(getClass().getName(), "Error initializing the MediaPlayer.",
					e);
			return;
		}
	}

	private void fireDataLoadUpdate() {
		Runnable updater = new Runnable() {
			public void run() {
				tProgress
						.setText((totalKbRead + " of " + mediaLengthInKb + " Kb"));
				float loadProgress = ((float) totalKbRead / (float) mediaLengthInKb);
				seek.setSecondaryProgress((int) (loadProgress * 100));
			}
		};
		handler.post(updater);
	}

	private void fireDataPreloadComplete() {
		Runnable updater = new Runnable() {
			public void run() {
				mediaPlayer.start();
				Log.d("testdownload", "Its Starting!!");
				primarySeekBarProgressUpdater();
			}
		};
		handler.post(updater);
	}

	private void fireDataFullyLoaded() {
		Runnable updater = new Runnable() {
			public void run() {
				transferBufferToMediaPlayer();

				// Delete the downloaded File as it's now been transferred to
				// the currently playing buffer file.
				downloadFile.delete();
				tProgress.setText(totalKbRead + " of " + mediaLengthInKb
						+ " Kb");
			}
		};
		handler.post(updater);
	}

	private void transferBufferToMediaPlayer() {
		try {
			Log.d("testdownload", "Its Transferring!!");
			// First determine if we need to restart the player after
			// transferring data...e.g. perhaps the user pressed pause
			boolean wasPlaying = mediaPlayer.isPlaying();
			int curPosition = mediaPlayer.getCurrentPosition();

			// Pause the current player now as we are about to create and start
			// a new one.
			mediaPlayer.pause();

			// Copy the currently downloaded content to a new buffered File.
			// Store the old File for deleting later.

			File bufferedFile = new File(getExternalCacheDir(), "buf2_"
					+ (counter++) + ".mp3");
			// bufferedFile = File.createTempFile("buf_" + (counter++), ".mp3",
			// bufferedFile);

			moveFile(downloadFile, bufferedFile);

			mediaPlayer = createMediaPlayer(bufferedFile.getAbsolutePath());
			mediaPlayer.setOnPreparedListener(this);
			
			/*
			 * mediaPlayer = new MediaPlayer(); FileInputStream fis = new
			 * FileInputStream(bufferedFile); // createMP();
			 * mediaPlayer.setDataSource(bufferedFile.getAbsolutePath());
			 */
			mediaPlayer.prepare();

			mediaPlayer.seekTo(curPosition);

			// getDirSize(oldBufferedFile);
			// getDirSize(bufferedFile);

			// Restart if at end of prior buffered content or mediaPlayer was
			// previously playing.
			// NOTE: We test for < 1second of data because the media player can
			// stop when there is still
			// a few milliseconds of data left to play
			boolean atEndOfFile = mediaPlayer.getDuration()
					- mediaPlayer.getCurrentPosition() <= 1000;
			if (wasPlaying || atEndOfFile) {
				mediaPlayer.start();
				Log.d("testdownload", "Its Starting!!");
			}

			// Lastly delete the previously playing buffered File as it's no
			// longer needed.

			bufferedFile.delete();

		} catch (Exception e) {
			Log.e(getClass().getName(),
					"Error updating to newly loaded content.", e);
		}
	}

	private MediaPlayer createMediaPlayer(String mediaFile) throws IOException {
		MediaPlayer mPlayer = new MediaPlayer();
		mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.e(getClass().getName(), "Error in MediaPlayer: (" + what
						+ ") with extra (" + extra + ")");
				return false;
			}
		});
		mPlayer.setOnBufferingUpdateListener(this);
		mPlayer.setOnCompletionListener(this);
		// FileInputStream fis = new FileInputStream(mediaFile);
		mPlayer.setDataSource(mediaFile);
		return mPlayer;
	}

	/**
	 * Move the file in oldLocation to newLocation.
	 */
	public void moveFile(File oldLocation, File newLocation) throws IOException {

		if (oldLocation.exists()) {
			BufferedInputStream reader = new BufferedInputStream(
					new FileInputStream(oldLocation));
			BufferedOutputStream writer = new BufferedOutputStream(
					new FileOutputStream(newLocation, false));
			try {
				byte[] buff = new byte[8192];
				int numChars;
				while ((numChars = reader.read(buff, 0, buff.length)) != -1) {
					writer.write(buff, 0, numChars);
				}
			} catch (IOException ex) {
				throw new IOException("IOException when transferring "
						+ oldLocation.getPath() + " to "
						+ newLocation.getPath());
			} finally {
				try {
					if (reader != null) {
						writer.close();
						reader.close();
					}
				} catch (IOException ex) {
					Log.e(getClass().getName(),
							"Error closing files when transferring "
									+ oldLocation.getPath() + " to "
									+ newLocation.getPath());
				}
			}
		} else {
			throw new IOException(
					"Old location does not exist when transferring "
							+ oldLocation.getPath() + " to "
							+ newLocation.getPath());
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mediaPlayer != null)
			mediaPlayer.release();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.d("Audio", "prepared!!");
		mp.start();
		playPause.setEnabled(true);
		playPause.setImageResource(R.drawable.button_pause);
		primarySeekBarProgressUpdater();
	}

	// gak dipake
	private void getDirSize(File tdir) {
		long result = 0;

		Stack<File> dirlist = new Stack<File>();
		dirlist.clear();

		dirlist.push(tdir);

		while (!dirlist.isEmpty()) {
			File dirCurrent = dirlist.pop();

			File[] fileList = dirCurrent.listFiles();
			for (int i = 0; i < fileList.length; i++) {

				if (fileList[i].isDirectory())
					dirlist.push(fileList[i]);
				else
					result += fileList[i].length();
			}
		}
		Log.d("downloads", "dir: " + tdir + " result: " + result);
	}

	// gak dipake
	public void createMP() {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnCompletionListener(this);
	}

	// gak dipake
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		/*
		 * Log.d("ABC", "start " + System.currentTimeMillis()); String url =
		 * AlkitabAudio.getAudioURL(S.activeBook, chapter_1); String title =
		 * S.reference(S.activeBook, chapter_1);
		 * 
		 * Log.d("Audio", url); Log.d("Audio", title);
		 * 
		 * if (v.getId() == R.id.ButtonPlayPause) { /** ImageButton onClick
		 * event handler. Method which start/pause mediaplayer playing
		 */

		// if (checkURL(url)) {
		// getDataSource(url);
		// play(url);

		// try {
		// createFile(url);
		/*
		 * if (mediaPlayer == null) { mediaPlayer = new MediaPlayer(); } else {
		 * mediaPlayer.stop(); mediaPlayer.reset(); }
		 * mediaPlayer.setDataSource(url); // Go to Initialized state
		 * mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		 * mediaPlayer.setOnPreparedListener(this);
		 * mediaPlayer.setOnBufferingUpdateListener(this);
		 * 
		 * mediaPlayer.prepareAsync();
		 * 
		 * if (mediaPlayer.isPlaying()) { mediaPlayer.pause();
		 * playPause.setImageResource(R.drawable.button_play); }else
		 * playPause.setImageResource(R.drawable.button_pause);
		 * 
		 * 
		 * 
		 * try {
		 */
		// setup song from
		// URL to mediaplayer data source
		// if (mpSet == false){

		/*
		 * long fileSize = entity.getContentLength();
		 * 
		 * File tmp = new File(Environment.getExternalStorageDirectory(),
		 * "bible/tmp/");
		 * 
		 * //create download file downloadFile =
		 * File.createTempFile("downloaded", ".mp3", tmp);
		 * 
		 * byte buf[] = new byte[16384]; int totalBytesRead = 0; int
		 * incrementBytes = 0;
		 * 
		 * while (notInterrupted()){ int nread = content.read(buf); if (nread <=
		 * 0) break; else { out.write(buf, 0, nread);
		 * 
		 * totalBytesRead += nread; incrementBytes += nread; totalKbRead =
		 * totalBytesRead/1024;
		 * 
		 * testMediaBuffer(); fireDataLoadUpdate(); } // Log.d("download", "" +
		 * totalBytesRead + " " + incrementBytes); getDirSize(tmp); }
		 * content.close();
		 */

		/*
		 * mediaPlayer.setDataSource(url);
		 * mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		 * mediaPlayer .setOnPreparedListener((OnPreparedListener) this); // you
		 * must call this method after setup the datasource in // setDataSource
		 * method. After calling prepare() the // instance of MediaPlayer starts
		 * load data from URL to // internal buffer. mediaPlayer.prepareAsync();
		 * 
		 * // gets the song length in milliseconds from URL mediaFileLength =
		 * mediaPlayer.getDuration(); mpSet = true; }else { if
		 * (!mediaPlayer.isPlaying()) { mediaPlayer.start();
		 * playPause.setImageResource(R.drawable.button_pause); } else {
		 * mediaPlayer.pause();
		 * playPause.setImageResource(R.drawable.button_play); } }
		 * 
		 * 
		 * primarySeekBarProgressUpdater();
		 * 
		 * } catch (Exception e) { e.printStackTrace(); }
		 */
		/*
		 * } else { Log.d("Audio", "Tidak Bisa"); new
		 * AlertDialog.Builder(IsiActivity.this).setTitle("Audio")
		 * .setMessage("Audio tidak tersedia untuk " + title)
		 * .setPositiveButton("OK", null).show(); } }
		 */
	}

}
