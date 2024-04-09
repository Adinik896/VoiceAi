package com.example.voiceai;

import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.RECORD_AUDIO;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.Manifest;


import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.database.Cursor;
import android.provider.CallLog;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private EditText editText;
    private OCRManager ocrManager;

    private static final int MY_PERMISSIONS_REQUEST_READ_CALL_LOG = 123;
    private final int GALLERY_REQ_CODE = 1000;// Use any unique integer value

    private String stringOutput = "";

    private TextToSpeech textToSpeech;

    private SpeechRecognizer speechRecognizer;
    private Intent intent;
    private PackageManager packageManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getModel();
        ActivityCompat.requestPermissions(this,
                new String[]{RECORD_AUDIO},
                PackageManager.PERMISSION_GRANTED);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, MY_PERMISSIONS_REQUEST_READ_CALL_LOG);
        }

        packageManager = getPackageManager();
        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                textToSpeech.setSpeechRate((float) 0.8);
            }
        });


        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int i) {
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                String string = "";
                textView.setText("");
                if (matches != null) {
                    string = matches.get(0);
                    editText.setText(string);

                    if (string.toLowerCase().contains("call")) {
                        String[] nameOfContact = string.split(" ", 2);
                        String personName = nameOfContact[1];
                        callContact(toTitleCase(personName));
                    } else if (string.toLowerCase().contains("open")) {
                        String[] array = string.split(" ", 2);
                        String appName = array[1];
                        openAppByName(appName.toLowerCase());
                    } else if (string.toLowerCase().contains("alarm")) {
                        String time = null, amPm = null;
                        if (string.toLowerCase().contains("set an alarm for")) {
                            String[] array = string.split(" ");
                            time = array[4];
                            amPm = array[5];
                        } else if (string.toLowerCase().contains("set alarm for")) {
                            String[] array = string.split(" ");
                            time = array[3];
                            amPm = array[4];
                        } else if (string.toLowerCase().contains("set alarm")) {
                            String[] array = string.split(" ");
                            time = array[2];
                            amPm = array[3];
                        }
                        setAlarm(time, amPm);
                    } else {
                        editText.setText(string);
                        String stringText = requestSolution(string + " In maximum 3 sentences");
                        stringText = stringText.replaceAll("[^a-zA-Z0-9\\s]", "");
                        stringOutput += stringText;
                        textView.setText(stringOutput);
                        textToSpeech.speak(stringOutput, TextToSpeech.QUEUE_FLUSH, null,null);
                    }
                }
            }


            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        });
    }

    private void setAlarm(String time, String amPm) {
        if(time == null || amPm == null)
            return;

        Calendar calendar = Calendar.getInstance();
        int hour, minute;

        // Parse the time provided by the user
        String[] timeParts = time.split(":");
        if (timeParts.length == 2) {
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
            Toast.makeText(this, amPm + " INSIDE PM "+hour, Toast.LENGTH_SHORT).show();
            // Adjust for AM/PM
            if (amPm.equalsIgnoreCase("P.M.") && hour < 12) {
                hour += 12;
                Toast.makeText(this, "INSIDE PM "+hour, Toast.LENGTH_SHORT).show();
            }
            if (amPm.equalsIgnoreCase("A.M.") && hour == 12) {
                hour = 0;
            }

            // Set the alarm
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent alarmIntent = new Intent(this, AlarmReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
            alarmIntent.setAction(time);

            // Schedule the alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            //Toast.makeText(this, "NO PERMISSION FOR CALLLOG "+calendar.getTimeInMillis(), Toast.LENGTH_LONG).show();
            // Inform the user
            textToSpeech.speak("Alarm set for " + time + " " + amPm, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            textToSpeech.speak("Invalid time format", TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }


    private void openAppByName(String appName) {
        List<PackageInfo> installedApps = packageManager.getInstalledPackages(0);

        for (PackageInfo appInfo : installedApps) {
            String label = appInfo.applicationInfo.loadLabel(packageManager).toString().toLowerCase();
            if (label.equals(appName)) {
                String packageName = appInfo.packageName;
                Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    return;
                }
            }
        }

        // App not found
        textToSpeech.speak("App not found", TextToSpeech.QUEUE_FLUSH, null,null);
    }



    private static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Split the input into words
        String[] words = input.split(" ");

        // Create a StringBuilder to build the formatted string
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                // Capitalize the first letter of the word
                formattedName.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    // Append the rest of the word in lowercase
                    formattedName.append(word.substring(1).toLowerCase());
                }
                formattedName.append(" "); // Add a space between words
            }
        }

        // Remove the trailing space
        formattedName.deleteCharAt(formattedName.length() - 1);

        return formattedName.toString();
    }


    private void callContact(String personName) {
       // String phoneNumber = getPhoneNumberFromCallLog(personName);//from call logs
        String phoneNumber = getPhoneNumberFromContacts(personName);// from contacts

        if (phoneNumber != null) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(callIntent);
        } else {
            Toast.makeText(this, "Phone number not found.", Toast.LENGTH_SHORT).show();
        }

    }

    private String getPhoneNumberFromCallLog(String personName) {
        String phoneNumber = null;
        String[] projection = {
                CallLog.Calls.NUMBER
        };
        String selection = CallLog.Calls.CACHED_NAME + "=?";
        String[] selectionArgs = {personName};
        Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                CallLog.Calls.DATE + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
        //    Toast.makeText(this, "Phone number not found.", Toast.LENGTH_SHORT).show();
            int numberColumn = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            phoneNumber = cursor.getString(numberColumn);
            cursor.close();
        }

        return phoneNumber;

    }


    private String getPhoneNumberFromContacts(String personName) {
        String phoneNumber = null;
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String selection = ContactsContract.Data.DISPLAY_NAME + "=?";
        String[] selectionArgs = {personName};
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            phoneNumber = cursor.getString(numberColumn);
            cursor.close();
        }

        return phoneNumber;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_CALL_LOG) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with accessing the call log.
            } else {
                // Permission denied, handle this situation (e.g., show a message to the user).
            }
        }
    }

    public void buttonAssist(View view){

        if (textToSpeech.isSpeaking()){
            textToSpeech.stop();
            return;
        }
        stringOutput = "";
        speechRecognizer.startListening(intent);
    }

    public void openGallery(View view){
        Intent iGallery = new Intent(Intent.ACTION_PICK);
        iGallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(iGallery, GALLERY_REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == GALLERY_REQ_CODE && data != null) {
            // Check for both resultCode and requestCode, and also data not being null
            Uri imageUri = data.getData();
            String text = getText(imageUri);//*$$\\
            String reply = requestSolution(text+" describe this in maximum 3 sentences");
            reply.replaceAll("[^a-zA-Z0-9\\s]","");
            textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, null,null);
            textView.setText(reply);

        }
    }

    private String getText(Uri imageUri) {//map{key:0000011,key2:010102}
        String extractedText=null;
        try {
            // Convert image URI to Bitmap
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            // Extract text from Bitmap using OCRManager
            Log.d("OCR", "dataPath: " + getFilesDir().getPath());
            Log.d("OCR", "language: eng");
            extractedText = ocrManager.extractTextFromBitmap(bitmap);

            Log.d("Extracted text:",extractedText);
            // Display the extracted text
//            textView.setText(extractedText);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return extractedText;
    }

    private  void getModel(){//privatedir/tesseract/tessdata/eng.traineddata
        String dataPath = getFilesDir() + File.separator + "tesseract";
        String language = "eng"; // English language
        File dataPathDir = new File(dataPath);
        if (!dataPathDir.exists()) {
            if (!dataPathDir.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + dataPath);
            }
        }
        String tessdataPath = dataPath + File.separator + "tessdata";
        File tessdataDir = new File(tessdataPath);
        if (!tessdataDir.exists()) {
            if (!tessdataDir.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + tessdataPath);
            }
        }

        AssetManager assetManager = getApplicationContext().getAssets();
        String assetFileName = "eng.traineddata";
        String targetFilePath = getFilesDir() + File.separator + "tesseract" + File.separator + "tessdata" + File.separator + assetFileName;

        // Check if the target directory exists, if not, create it
        File targetDirectory = new File(getFilesDir() + File.separator + "tesseract" + File.separator + "tessdata");
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        try {
            InputStream inputStream = assetManager.open(assetFileName);
            OutputStream outputStream = new FileOutputStream(targetFilePath);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("AssetManager",assetManager.toString());
        ocrManager = new OCRManager(dataPath, language);
    }

    private String requestSolution(String question) {
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py = Python.getInstance();
        PyObject module = py.getModule("script");



        PyObject obj = module.callAttr("getResponse", question);
        Log.d("Returnnnn", String.valueOf(obj));

        return String.valueOf(obj);

    }

}