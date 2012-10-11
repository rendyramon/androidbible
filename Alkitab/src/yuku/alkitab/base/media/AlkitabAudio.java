package yuku.alkitab.base.media;

import android.util.Log;
import yuku.alkitab.base.S;
import yuku.alkitab.base.model.Book;

public class AlkitabAudio {
	
	public static String getAudioURL(Book book, int chapter){
		String newurl = "http://media.sabda.org/alkitab_audio/tb/";
		String plpb = null;
		 
		String judul = book.judul.toLowerCase();
		judul = judul.replaceAll("\\s", "");
		
		if (judul.equals("kisahpararasul")) judul = "kisah";
		
		int booknum = 0;
		String bnum;
		
		if (book.bookId >= 0 && book.bookId < 39){
			plpb = "pl";
			booknum = book.bookId + 1;			
		}else if (book.bookId >= 39 && book.bookId < 66){
			plpb = "pb";
			booknum = book.bookId + 1 - 39;
		}		
		
		if (booknum < 9) bnum = "0" + booknum;
		else bnum = "" + booknum;
		
		String shortname = judul.substring(0, 3);
		String chap;		
		
		if (chapter < 10) chap = "0" + chapter;
		else chap = "" + chapter;
		
		newurl = newurl + plpb + "/mp3/cd/" + bnum + "_" + judul + "/" + bnum + "_" + shortname + chap + ".mp3";
			
		return newurl;
	}
}
