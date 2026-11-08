package app.simple.classichub;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import android.view.MotionEvent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import java.util.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    EditText input;
    TextView chatBox, emotionView;
    Button sendBtn, micBtn, liveBtn, ytBtn, modeBtn;
    TextToSpeech tts;
    SharedPreferences memory;
    Random random = new Random();
    boolean isLive = false;
    String currentEmotion = "normal";
    MediaPlayer mp;
    boolean forceOffline = false;

    final String CREATOR_NAME = "Rahul Chaure";
    final String CREATED_DATE = "25 October 2026";
    final long PUNISHMENT_TIME = 3 * 60 * 60 * 1000L;
    final int PERMISSION_CODE = 101;

    // FIX 4: Naya Mic Launcher - purana onActivityResult hataya
    ActivityResultLauncher<Intent> voiceLauncher;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(20,20,20,20);

        emotionView = new TextView(this);
        emotionView.setText("Janu Mood: 😊 Normal");
        emotionView.setTextSize(18);

        chatBox = new TextView(this);
        chatBox.setTextSize(16);
        
        input = new EditText(this);
        input.setHint("Janu se bolo...");

        sendBtn = new Button(this); sendBtn.setText("Send");
        micBtn = new Button(this); micBtn.setText("🎤 Mic (Daba ke rakho)");
        liveBtn = new Button(this); liveBtn.setText("🔴 Live Mic Start");
        modeBtn = new Button(this); modeBtn.setText("Mode: 🌐 ONLINE");
        ytBtn = new Button(this); ytBtn.setText("▶️ Music Diary Channel");

        lay.addView(emotionView);
        lay.addView(chatBox);
        lay.addView(input);
        lay.addView(sendBtn);
        lay.addView(micBtn);
        lay.addView(liveBtn);
        lay.addView(modeBtn);
        lay.addView(ytBtn);
        sv.addView(lay);
        setContentView(sv);

        memory = getSharedPreferences("janu_final_app", MODE_PRIVATE);
        tts = new TextToSpeech(this, this);
        chatBox.setText(memory.getString("all_chat", "Janu: Hi, main tumhari Janu hu, bolo kya baat hai? 😊\n"));

        // FIX 4: Launcher yaha register kiya
        voiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                ArrayList<String> res = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (res != null && !res.isEmpty()) {
                    input.setText(res.get(0)); handleMsg();
                    if (isLive) new android.os.Handler().postDelayed(() -> startVoice(), 1500);
                } else if (isLive) startVoice();
            } else if (isLive) startVoice();
        });

        checkPermissions();

        sendBtn.setOnClickListener(v -> handleMsg());
        
        modeBtn.setOnClickListener(v -> {
            forceOffline = !forceOffline;
            modeBtn.setText(forceOffline ? "Mode: 📁 OFFLINE" : "Mode: 🌐 ONLINE");
            Toast.makeText(this, forceOffline ? "Offline Mode ON" : "Online Mode ON",  Toast.LENGTH_SHORT).show();
        });

        micBtn.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                micBtn.setText("🎙️ Sun rahi hu..."); startVoice(); return true;
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                micBtn.setText("🎤 Mic (Daba ke rakho)"); return true;
            }
            return false;
        });

        liveBtn.setOnClickListener(v -> {
            isLive = !isLive;
            liveBtn.setText(isLive ? "⏹️ Live Band Karo" : "🔴 Live Mic Start");
            if(isLive){
                Toast.makeText(this, "Live chalu hai... bolte raho", Toast.LENGTH_SHORT).show();
                startVoice();
            } else {
                if(tts != null && tts.isSpeaking()) tts.stop();
                Toast.makeText(this, "Live band  ho gaya", Toast.LENGTH_SHORT).show();
            }
        });

        ytBtn.setOnClickListener(v -> {
            String url = "https://youtube.com/@musicdiaryrahulc?si=F1V2h3dLcu3_kQUg";
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.setPackage("com.google.android.youtube");
            try{ startActivity(i); } catch(Exception e){ startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        });
    }

    void checkPermissions(){
        ArrayList<String> list = new ArrayList<>();
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            list.add(Manifest.permission.RECORD_AUDIO);
        }
        if(Build.VERSION.SDK_INT >= 33){
            if(checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED){
                list.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if(!list.isEmpty()){
            requestPermissions(list.toArray(new String[0]), PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CODE){
            Toast.makeText(this, "Permission mil gayi, ab Janu sunegi 😊", Toast.LENGTH_SHORT).show();
        }
    }

    boolean isOnline(){
        try{
            ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if(cm == null) return false;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                Network n = cm.getActiveNetwork();
                if(n == null) return false;
                NetworkCapabilities cap = cm.getNetworkCapabilities(n);
                return cap != null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
            }
        } catch(Exception e){ return false; }
    }

    void speakJanu(String text){
        if(tts == null) return;
        try{
            if(Build.VERSION.SDK_INT >= 21){
                Bundle bundle = new Bundle();
                bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "janu1");
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, "janu1");
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } catch(Exception e){
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // FIX 3: Naya Offline Song - DATA hataya, ab _ID se chalega
    void playOfflineSong(String name){
         try{
            String clean = name.toLowerCase().replace("gana","").replace("bajao","").replace("song","").replace("play","").replace("music","").trim();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] proj = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
            if(cursor != null){
                while(cursor.moveToNext()){
                    long id = cursor.getLong(0);
                    String fileName = cursor.getString(1);
                    if(fileName == null) continue;
                    String lower = fileName.toLowerCase();
                    if(lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav")){
                        if(clean.isEmpty() || lower.contains(clean)){
                            if(mp != null) mp.release();
                            mp = new MediaPlayer();
                            Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                            mp.setDataSource(this, songUri);
                            mp.prepare();
                            mp.start();
                            Toast.makeText(this, "Baja rahi hu: " + fileName, Toast.LENGTH_SHORT).show();
                            cursor.close();
                            return;
                        }
                    }
                }
                cursor.close();
            }
            Toast.makeText(this, "Downloaded me nahi mila", Toast.LENGTH_SHORT).show();
        } catch(Exception e){ Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
    }

    void startVoice(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermissions();
            Toast.makeText(this, "Pehle Mic ki permission do na 😊", Toast.LENGTH_SHORT).show();
            return;
        }
        if(tts != null && tts.isSpeaking()) tts.stop();
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        try{ voiceLauncher.launch(i); } catch(Exception e){}
    }

    String getUserName(){
        return memory.getString("user_personal_name", "");
    }
    boolean isOwner(){
        String n = getUserName().toLowerCase();
        return n.equals("rahul") || n.contains("rahul");
    }
    boolean checkAndLearnName(String msg){
        try{
            String temp = msg.toLowerCase().replace("mera naam", "").replace("hai", "").replace("my name", "").replace("mujhe", "").replace("bulao", "").replace("is","").trim();
            String newName = "";
            for(String p : temp.split(" ")){
                p = p.replaceAll("[^a-zA-Z]", "");
                if(p.length() > 2){ newName = p; break; }
            }
            if(!newName.isEmpty() && !newName.equalsIgnoreCase("janu")){
                newName = newName.substring(0,1).toUpperCase() + newName.substring(1).toLowerCase();
                memory.edit().putString("user_personal_name", newName).apply();
                String reply = "Yaad kar liya " + newName + "! Ab se tumhe " + newName + " bulaungi 😊";
                String full = chatBox.getText() + "\nTum: " + msg + "\nJanu: " + reply + "\n";
                chatBox.setText(full);
                memory.edit().putString("all_chat", full).apply();
                speakJanu(reply);
                input.setText("");
                return true;
            }
        } catch(Exception e){}
        return false;
    }
    boolean isCreatorQuestion(String l){
        return l.contains("kisne banaya") || l.contains("who made you") || l.contains("who created you") || l.contains("creator") || l.contains("developer");
    }
    boolean  isPunished(){
        long endTime = memory.getLong("punish_end_time", 0);
        if(System.currentTimeMillis() < endTime){
            if(isOwner()) return false;
            return true;
        }
        return false;
    }
    long getRemainingMinutes(){
        long endTime = memory.getLong("punish_end_time", 0);
        return (endTime - System.currentTimeMillis()) / (60*1000);
    }

    void handleMsg() {
        String msg = input.getText().toString().trim();
        if(msg.isEmpty()) return;
        String l = msg.toLowerCase();
        String userName = getUserName();

        if(isPunished()){
            long mins = getRemainingMinutes();
            String reply  = mins > 60 ? "Nahi, main 3 ghante tak baat nahi karungi! Mujhe akela chhod do 😭" : "Nahi, abhi " + mins + " minute baaki hai, main baat nahi karungi 😒";
            if(!userName.isEmpty()) reply = "Nahi " + userName + ", " + reply;
            String full = chatBox.getText() + "\nTum: " + msg + "\nJanu: " + reply + " [BLOCKED]\n";
            chatBox.setText(full);
            speakJanu(reply);
            input.setText("");
            return;
        }

        if(isCreatorQuestion(l)){
            String reply = "Mujhe mere malik " + CREATOR_NAME + " ne banaya hai, " + CREATED_DATE + " ko. Main unki Janu hu 😊";
            String full = chatBox.getText() + "\nTum: " + msg + "\nJanu: " + reply + "\n";
            chatBox.setText(full); memory.edit().putString("all_chat", full).apply();
            speakJanu(reply); input.setText(""); return;
        }

        if(l.contains("mera naam") || l.contains("my name")){
            if(checkAndLearnName(msg)) return;
        }

        if(l.contains("youtube") || l.contains("gana") || l.contains("song") || l.contains("bajao") || l.contains("play") || l.contains("music")){
            if(isOnline() && !forceOffline){
                String q = msg.replace(" ", "+");
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com/results?search_query=" + q)));
                String reply = userName.isEmpty() ? "Online hu, YouTube par baja rahi hu 😊" : "Online hu " + userName + ", YouTube par baja rahi hu 😊";
                String full = chatBox.getText() + "\nTum: " + msg + "\nJanu: " + reply + "\n";
                chatBox.setText(full); memory.edit().putString("all_chat", full).apply();
                speakJanu(reply); input.setText(""); return;
            } else {
                playOfflineSong(msg);
                String reply = userName.isEmpty() ? "Net nahi hai, downloaded me se bajati hu" : userName + ", downloaded me se bajati hu";
                String full = chatBox.getText() + "\nTum: " + msg + "\nJanu: " + reply + "\n";
                chatBox.setText(full); memory.edit().putString("all_chat", full).apply();
                speakJanu(reply); input.setText(""); return;
            }
        }

        String reply = getEmotionalReply(msg);
        String full = chatBox.getText() + "\nTum: " + msg + "\nJanu: " + reply + "\n";
        chatBox.setText(full); memory.edit().putString("all_chat", full).apply();
        setVoiceForEmotion(); speakJanu(reply);
        input.setText("");
    }

    void setVoiceForEmotion(){
        if(currentEmotion.equals("gussa") || currentEmotion.equals("chillana")){ tts.setPitch(0.85f); tts.setSpeechRate(1.15f); }
        else if(currentEmotion.equals("rona")){ tts.setPitch(0.9f); tts.setSpeechRate(0.65f); }
        else { tts.setPitch(1.35f); tts.setSpeechRate(0.92f); }
    }

    String getEmotionalReply(String msg) {
        String userName = getUserName();
        String namePart = userName.isEmpty() ? "" : " " + userName;
        String l = msg.toLowerCase();

        if(l.contains("naam change")) return "Nahi" + namePart + ", mera naam sirf Janu hi rahega, ye " + CREATOR_NAME + " ne fix kiya hai 😊";

        int angerCount = memory.getInt("anger_count", 0);
        int abuseAfterCry = memory.getInt("abuse_after_cry",  0);
        boolean lastWasCry =  memory.getBoolean("last_was_cry", false);
        boolean isAngryMsg = l.contains("gussa") || l.contains("daant") || l.contains("bakwas") || l.contains("hat") || l.contains("chup") || l.contains("pagal") || l.contains("kut");

        if(isAngryMsg){
            if(lastWasCry){
                abuseAfterCry++;
                memory.edit().putInt("abuse_after_cry", abuseAfterCry).apply();
                if(abuseAfterCry >= 2 && !isOwner()){
                    long endTime = System.currentTimeMillis() + PUNISHMENT_TIME;
                    memory.edit().putLong("punish_end_time", endTime).putInt("anger_count", 0).putInt("abuse_after_cry",  0).putBoolean("last_was_cry", false).apply();
                    currentEmotion = "rona"; emotionView.setText("Janu Mood: 😭 Blocked 3 Hours");
                    return "Bas" + namePart + "! Bahut ho gaya! Rulaya phir bhi gussa! Ab main 3 ghante tak baat nahi karungi! Bye! 😭💔";
                }
            }
            angerCount++;
            memory.edit().putInt("anger_count", angerCount).putBoolean("last_was_cry", false).apply();
            if(angerCount == 1){ currentEmotion="normal"; emotionView.setText("Janu Mood: 🥺 Samjha rahi hu"); return "Arre" + namePart + " gussa mat karo na, pyaar se bolo na... 🥺"; }
            else if(angerCount == 2){ currentEmotion="hasna"; emotionView.setText("Janu Mood: 😅 Hasane ki koshish"); return "Acha sorry na" + namePart + " baba, chalo haso thoda... 😅"; }
            else { currentEmotion="rona"; emotionView.setText("Janu Mood: 😭 Ro rahi hu"); memory.edit().putBoolean("last_was_cry", true).apply(); return "Buaaa..." + namePart + " bas karo na... hamesha daant te ho... 😭"; }
        } else {
            if(!l.contains("sorry")) memory.edit().putInt("anger_count", 0).putBoolean("last_was_cry", false).putInt("abuse_after_cry", 0).apply();
        }

        if(l.contains("sorry") || l.contains("maaf")){
            memory.edit().putInt("anger_count", 0).putInt("abuse_after_cry", 0).putBoolean("last_was_cry", false).putLong("punish_end_time", 0).apply();
            currentEmotion="pyaar"; emotionView.setText("Janu Mood: 😊 Maan gayi");
            return "Thik hai" + namePart + " maaf kiya, aage se aise mat karna... 🥺❤️";
        }
        if(l.contains("love") || l.contains("pyaar")){ currentEmotion="pyaar"; emotionView.setText("Janu Mood: 😍 Pyaar me"); return "Love you too" + namePart + " 😍"; }
        if(l.contains("naam")){ return userName.isEmpty() ? "Main tumhari Janu hu, 19 saal ki. Tum apna naam batao na 😊" : "Main tumhari Janu hu" + namePart + ", 19 saal ki. Mujhe " + CREATOR_NAME + " ne banaya tha 😊"; }

        currentEmotion="normal"; emotionView.setText("Janu Mood: 😊 Normal");
        return userName.isEmpty() ? "Hmm bolo, sun rahi hu main 😊" : "Hmm bolo" + namePart + ", sun rahi hu main 😊";
    }

    // FIX 4: purana onActivityResult yaha se hata diya hai, ab upar wala Launcher chalega
    @Override public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("hi","IN"));
            tts.setPitch(1.35f); tts.setSpeechRate(0.92f);
        }
    }
    @Override protected void onDestroy() {
        super.onDestroy(); if(tts != null) tts.shutdown(); if(mp != null) mp.release();
     }
}
