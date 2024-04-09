package com.example.voiceai;

import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

public class OCRManager {

    private final TessBaseAPI tessBaseAPI;

    public OCRManager(String dataPath, String language) {
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(dataPath, language);
    }

    public String extractTextFromBitmap(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        return tessBaseAPI.getUTF8Text();
    }

    public void onDestroy() {
        if (tessBaseAPI != null) {
            tessBaseAPI.end();
        }
    }
}