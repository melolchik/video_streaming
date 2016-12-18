package net.majorkernelpanic.streaming;

import android.text.TextUtils;
import android.util.Log;

import com.instabug.library.Instabug;


/**
 * Created by Olga Melekhina on 02.06.2016.
 */
public class AppLogger {
    /**
     * The constant TAG.
     */
    public static final String TAG = "CARPO";
    public static final String TAG_FCM = "CARPO";


    /**
     * Log.
     *
     * @param text the text
     */
    public  static void log(String text){
        Log.d(TAG,"" + text);
    }

    public  static void log_fcm(String text){
         Log.d(TAG_FCM,"" + text);
    }

    public static void log_instabug(String text){
        if(TextUtils.isEmpty(text)) return;
        if (CarpoApplication.allowIntabugAndHockeyAppUpdate) {
            Instabug.log(text);
        }
    }

}
