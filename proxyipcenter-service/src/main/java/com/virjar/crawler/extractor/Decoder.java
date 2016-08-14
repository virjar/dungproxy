package com.virjar.crawler.extractor;

public class Decoder {

	public static String decode(String str,String decoder){
		if(decoder == null)
			return str;
		try {
			if(decoder.toLowerCase().equals("base64")){
				return new String(BaseSixtyfour.decode(str));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return str;
	}
}
