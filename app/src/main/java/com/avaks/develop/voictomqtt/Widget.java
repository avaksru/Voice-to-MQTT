package com.avaks.develop.voictomqtt;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;
import static java.security.AccessController.checkPermission;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import android.speech.tts.TextToSpeech;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.ArrayList;



/**
 * Implementation of App Widget functionality.
 */

public class Widget extends AppWidgetProvider implements TextToSpeech.OnInitListener {

    private static String widgetMQTT= "tcp://192.168.0.10:1883";
    private static String widgetcommandTopic = "/test";
    private static String widgetstatusTopic = "/test";
    private static String widgetUser= "guest";
    private static String widgetPassword = "guest";
    boolean  widgetTTS = true;
    //------------------
    private SpeechRecognizer speechRecognizer;
    private EditText editText;
    private ImageView micButton;

    private int mic_ = 0;

    String text ="";
    private TextToSpeech TTS;
    private static final String PREF_PREFIX_KEY = "appwidget_";
    static SharedPreferences settings;

    //----------------
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
    }

    static void Tost(Context context, String msg) {
             Toast t = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
             t.show();
    }


    public final static String STOP_WIDGET_ANIMATION = "com.avaks.develop.vocetomqtt.STOP_WIDGET_ANIMATION";
    public final static String FORCE_WIDGET_UPDATE = "com.avaks.develop.vocetomqtt.FORCE_WIDGET_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.e("!!!!!!!!!!!!!!!!!!!", "Получено onUpdate");
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);

        }
    }
    Button button;

    //вызывается каждый раз, при отлове broadcast'a с установленным для виджета фильтром
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        TTS = new TextToSpeech(context, this);


//------------------

        Log.e("!!!!!!!!!!!!!!!!!!!", "Получено onReceive");

        //Проверка action и при соответствии - выполнение вашего кода
        if (FORCE_WIDGET_UPDATE.equals(intent.getAction())) {
            Log.e("!!!!!!!!!!!!!!!!!!!", "Получено FORCE_WIDGET_UPDATE");
            int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int[] allWidgetIds = { widgetID };
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            widgetMQTT = prefs.getString("mqtt_server", "");
            widgetcommandTopic=  prefs.getString("command_topic", "");
            widgetstatusTopic=  prefs.getString("status_topic", "");
            widgetUser=  prefs.getString("username", "");
            widgetPassword=  prefs.getString("pass", "");
            widgetTTS =  prefs.getBoolean("tts", false);

            // Обновляем виджет
            mic_=1;
            updateWidget(context, AppWidgetManager.getInstance(context), widgetID);
        }
        if (STOP_WIDGET_ANIMATION.equals(intent.getAction())) {
            // обновляете виджет после обновления данных
        }
    }


    PendingIntent pIntent;
    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetID) {
        Log.e("!!!!!!!!!!!!!!!!!!!", "Получено updateWidget");


        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        Intent updateIntent = new Intent(context, Widget.class);
        updateIntent.setAction(FORCE_WIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID); // id виджета, который от которого будет послан broadcast
        int[] allWidgetIds = {widgetID}; // id виджетов, которые необходимо будет обновить
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pIntent = PendingIntent.getBroadcast(context, widgetID, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }else {
            pIntent = PendingIntent.getBroadcast(context, widgetID, updateIntent, 0); // создаём PendingIntent который будет отправляться при нажатии
        }
        views.setOnClickPendingIntent(R.id.mic, pIntent); // id кнопки, по нажатию на которую будет отправлен broadcast


        if (mic_ == 1) {
            ////////////--------------------------

            new CountDownTimer(5000, 500) {
                public void onFinish() {

                    views.setInt(R.id.mic, "setBackgroundResource", R.mipmap.gray_foreground);
                    appWidgetManager.updateAppWidget(widgetID, views);
                    if (text !="") {
                        Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                        t.show();
                    }
                }  public void onTick(long millisUntilFinished) { }
            }.start();
            String topic = widgetcommandTopic;
            int qos = 0;
            String broker = widgetMQTT; //"tcp://live-control.com:1883";
            String clientId = "VoceToMqtt";
            MemoryPersistence persistence = new MemoryPersistence();

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechRecognizer.startListening(speechRecognizerIntent);
            views.setInt(R.id.mic, "setBackgroundResource", R.mipmap.read_foreground);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {

                @Override
                public void onReadyForSpeech(Bundle bundle) {

                }

                @Override
                public void onBeginningOfSpeech() {


                    //      editText.setText("");
                    //      editText.setHint("Listening...");
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

                String Speech(String text) {
                    String utteranceId = this.hashCode() + "";
                    TTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                    return text;
                }

                @Override
                public void onResults(Bundle bundle) {
                    views.setInt(R.id.mic, "setBackgroundResource", R.mipmap.gray_foreground);
                    appWidgetManager.updateAppWidget(widgetID, views);

                   ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                   if (broker !=""){
                   try {
                        MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
                        MqttConnectOptions connOpts = new MqttConnectOptions();
                        connOpts.setCleanSession(true);
                        System.out.println("Connecting to broker: " + broker);
                        connOpts.setUserName(widgetUser);
                        connOpts.setPassword(widgetPassword.toCharArray());
                        connOpts.setConnectionTimeout(60);
                        connOpts.setKeepAliveInterval(60);

                        sampleClient.connect(connOpts);
                        System.out.println("Connected");
                        views.setInt(R.id.mic, "setBackgroundResource", R.mipmap.mqtt_foreground);
                        appWidgetManager.updateAppWidget(widgetID, views);

                        System.out.println("Publishing message: " + data.get(0));
                        sampleClient.subscribe(widgetstatusTopic, qos);
                        MqttMessage message = new MqttMessage();
                        String msg = "{\"status\":\""+data.get(0)+"\"}";
                        message.setPayload(msg.getBytes());

                        // MqttMessage message = new MqttMessage(data.get(0).getBytes());
                        message.setQos(qos);
                        sampleClient.publish(widgetcommandTopic, message);
                        System.out.println("Message published");

                        //    Toast t = Toast.makeText(context, data.get(0), Toast.LENGTH_SHORT);
                        //   t.show();
                        ////////////////////
                        //  sampleClient.disconnect(3000);

                        sampleClient.setCallback(new MqttCallback() {
                            public void connectionLost(Throwable cause) {
                                System.out.println("connectionLost: " + cause.getMessage());


                            }


                            public void messageArrived(String topic, MqttMessage message) throws MqttException {
                                views.setInt(R.id.mic, "setBackgroundResource", R.mipmap.mqttcomplit_foreground);

                                appWidgetManager.updateAppWidget(widgetID, views);
                                System.out.println("topic: " + topic);
                                System.out.println("Qos: " + message.getQos());
                                System.out.println("message content: " + new String(message.getPayload()));
                                text = new String(message.getPayload());




                                try {
                                    JSONObject jObject = new JSONObject(text);
                                    text = jObject.getString("status");
                                   if (widgetTTS == true){
                                       Speech(text);

                                   }
                                } catch (JSONException e) {
                                    if (widgetTTS == true){
                                        Speech(text);

                                    }
                                }




                                sampleClient.disconnect(1000);

                            }
                            public void deliveryComplete(IMqttDeliveryToken token) {
                                System.out.println("deliveryComplete---------" + token.isComplete());

                            }
                        });



                    } catch (MqttException me) {
                        views.setInt(R.id.mic, "setBackgroundResource", R.mipmap.nomqtt_foreground);
                        appWidgetManager.updateAppWidget(widgetID, views);
                        System.out.println("reason " + me.getReasonCode());
                        System.out.println("msg " + me.getMessage());
                        Toast t = Toast.makeText(context,"MQTT: "+me.getMessage(), Toast.LENGTH_LONG);
                        t.show();
                        System.out.println("loc " + me.getLocalizedMessage());
                        System.out.println("cause " + me.getCause());
                        System.out.println("excep " + me);
                        t = Toast.makeText(context,"MQTT: "+me, Toast.LENGTH_LONG);
                        t.show();
                        me.printStackTrace();

                    }
                   }else{

                       views.setInt(R.id.mic, "setBackgroundResource", R.mipmap.nomqtt_foreground);
                       appWidgetManager.updateAppWidget(widgetID, views);
                       Toast t = Toast.makeText(context, "MQTT не настроен", Toast.LENGTH_SHORT);
                       t.show();
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


        appWidgetManager.updateAppWidget(widgetID, views);
    }





    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

    }


    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result;
            if (TTS.isLanguageAvailable(new Locale(Locale.getDefault().getLanguage()))
                    == TextToSpeech.LANG_AVAILABLE) {
                result =  TTS.setLanguage(new Locale(Locale.getDefault().getLanguage()));
                Log.e("TTS", "This Language is  supported");
            } else {
                result = TTS.setLanguage(Locale.US);
                Locale locale = new Locale(Locale.getDefault().getLanguage());
                Log.e("TTS", "This Language is not supported "+locale);

            }
            //    TTS.setPitch(1.3f);
            //    TTS.setSpeechRate(0.7f);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }
        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }



}
