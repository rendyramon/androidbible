package yuku.alkitab.base.media;

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
		String chap = "";
		
		if (chapter < 10) chap = "0" + chap + chapter;		
		else chap = "" + chapter;
		
		if (shortname.equals("maz")){
			shortname = "mzm";
			if (chapter < 100) chap = "0" + chap;
		}		
		else if (shortname.equals("pen")) shortname = "pkh";
		else if (shortname.equals("mar")) shortname = "mrk";						
		
		newurl = newurl + plpb + "/mp3/mobile/" + bnum + "_" + judul + "/" + bnum + "_" + shortname + chap + ".mp3";
			
		return newurl;
	}
}
