package yuku.alkitabintegration.display;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Launcher {
	public static final String TAG = Launcher.class.getSimpleName();
	
	public enum Product {
		ALKITAB("Alkitab", "yuku.alkitab"),
		QUICK_BIBLE("Quick Bible", "yuku.alkitab.kjv"),
		;
		
		private final String displayName;
		private final String packageName;

		private Product(String displayName, String packageName) {
			this.displayName = displayName;
			this.packageName = packageName;
		}
		
		public String getDisplayName() {
			return displayName;
		}

		public String getPackageName() {
			return packageName;
		}
	}
	
	/**
	 * Returns an intent that can be used to open the app at the specific book, chapter, and verse. 
	 * Call {@link Context#startActivity(Intent)} with the returned intent from your activity to open it.
	 * @param bookId 0 for Genesis, up to 65 for Revelation
	 * @param chapter_1 Chapter number starting from 1
	 * @param verse_1 Verse number starting from 1
	 */
	public static Intent openAppAtBibleLocation(int bookId, int chapter_1, int verse_1) {
		int ari = (bookId << 16) | (chapter_1 << 8) | verse_1;
		return openAppAtBibleLocation(ari);
	}

	/**
	 * Returns an intent that can be used to open the app at the specific ari. 
	 * Call {@link Context#startActivity(Intent)} with the returned intent from your activity to open it.
	 */
	public static Intent openAppAtBibleLocation(int ari) {
		Intent res = new Intent("yuku.alkitab.action.VIEW");
		res.putExtra("ari", ari);
		res.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		return res;
	}
	
	/**
	 * Returns an intent that can be used to open the Google Play app on the page for the user to download the app. 
	 * Call {@link Context#startActivity(Intent)} with the returned intent from your activity to open it.
	 * @param context any context of your app
	 * @param product one of the product to open
	 */
	public static Intent openGooglePlayDownloadPage(Context context, Product product) {
		Uri uri = Uri.parse("market://details?id=" 
		+ product.getPackageName()
		+ "&referrer=utm_source%3Dintegration%26utm_medium%3D"
		+ context.getPackageName());
		Intent res = new Intent(Intent.ACTION_VIEW);
		res.setData(uri);
		res.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		return res;
	}
}
