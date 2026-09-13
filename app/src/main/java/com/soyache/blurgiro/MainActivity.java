/*
 * Setup UI adapted from DuoFold-Android MainActivity
 * https://github.com/jcx396905-gif/DuoFold-Android
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 *
 * CristalGiro: Spanish first-run, navy/ice branding, Shizuku install step.
 */
package com.soyache.blurgiro;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.soyache.blurgiro.bridge.BridgeClient;
import com.soyache.blurgiro.motion.EffectStyle;

public final class MainActivity extends Activity {
    private static final int FG=0xffe8f2ff,MUTED=0xff8aa0b8,ACCENT=0xff8ec8ff,BG=0xff0b1020,CARD=0xff12182a;
    private static final String SHIZUKU="moe.shizuku.privileged.api";
    private final Handler handler=new Handler(android.os.Looper.getMainLooper());
    private BridgeClient client;
    private SharedPreferences prefs;
    private TextView bridgeStatus,accessStatus,liveStatus,progressText;
    private Button start,trial;
    private boolean pendingStart,pendingTrial;
    private final Runnable update=new Runnable() {
        @Override public void run() { refresh();handler.postDelayed(this,400); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences(CrystalService.PREFS,0);client=BridgeClient.get(this);
        int motionVersion=prefs.getInt("motionVersion",0);
        if(motionVersion<3) {
            SharedPreferences.Editor migration=prefs.edit().putInt("motionVersion",3).putBoolean("zAxis",false);
            if(motionVersion<2)migration.putFloat("range",80).putFloat("intensity",1);
            migration.apply();
        }
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(18),dp(24),dp(28));
        scroll.addView(body);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)-> {
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout setup=card(body);
        setup.addView(text("CristalGiro  ·  "+BuildConfig.VERSION_NAME+"  ·  "+BuildConfig.FLAVOR_LABEL,13,MUTED));
        add(setup,text("Efecto en todo el teléfono",22,FG),8,0);
        add(setup,text("Inclina en X: de frente nítido; el lado que se aleja se pone blando. "
                +"Hace falta Shizuku + accesibilidad. Sin root y sin diálogo de grabar pantalla.",13,MUTED),8,0);
        bridgeStatus=text("Esperando Shizuku",13,MUTED);add(setup,bridgeStatus,16,0);
        button(setup,"01   Instalar e iniciar Shizuku",this::openShizuku,false);
        button(setup,"02   Autorizar Shizuku",()->client.request(),false);
        accessStatus=text("Esperando accesibilidad",13,MUTED);add(setup,accessStatus,12,0);
        button(setup,"03   Activar accesibilidad",this::openAccessibility,false);
        add(setup,text("La accesibilidad pone la capa y corrige el toque (Android 14+). "
                +"La imagen se trata solo en este teléfono: no se guarda ni se sube.",12,MUTED),10,0);
        liveStatus=text("Cuando Shizuku y la accesibilidad estén listos, prueba 10 segundos",14,FG);add(setup,liveStatus,16,0);
        progressText=text("",12,MUTED);add(setup,progressText,6,0);
        trial=button(setup,"04   Probar 10 segundos",()->requestStart(true),true);
        start=button(setup,"05   Activar efecto global",()-> {
            CrystalService s=CrystalService.current();
            if(s!=null && s.isActive()) s.stopSession("Lo detuviste a mano");else requestStart(false);
        },false);
        button(setup,"Recalibrar la presa actual",()-> {
            CrystalService s=CrystalService.current();
            if(s!=null && s.isActive()) {s.calibrate();Toast.makeText(this,"Calibrado",Toast.LENGTH_SHORT).show();}
        },false);
        add(setup,text(Build.VERSION.SDK_INT>=34
                ?"Tres dedos a la vez o el botón de la notificación detienen el efecto."
                :"El botón de la notificación detiene el efecto.",12,ACCENT),12,0);

        LinearLayout tuning=card(body);
        tuning.addView(text("Ajuste al tacto",19,FG));
        slider(tuning,"Ángulo máximo",15,80,prefs.getFloat("range",80),value->prefs.edit().putFloat("range",value).apply(),"°");
        slider(tuning,"Intensidad espacial",20,100,prefs.getFloat("intensity",1)*100,value->prefs.edit().putFloat("intensity",value/100).apply(),"%");
        Switch zAxis=new Switch(this);zAxis.setText("Eje Z · inclinar adelante / atrás");zAxis.setTextSize(14);zAxis.setTextColor(FG);
        zAxis.setChecked(prefs.getBoolean("zAxis",false));zAxis.setPadding(0,dp(8),0,dp(4));
        zAxis.setOnCheckedChangeListener((button,checked)->prefs.edit().putBoolean("zAxis",checked).apply());
        tuning.addView(zAxis,new LinearLayout.LayoutParams(-1,dp(52)));
        add(tuning,text("Apagado: solo izquierda / derecha. Encendido: también adelante / atrás. "
                +"Los parámetros se aplican al siguiente arranque del efecto.",12,MUTED),8,0);

        LinearLayout effects=card(body);
        effects.addView(text("Estilo del cristal",19,FG));
        add(effects,text("Se aplica al momento y se recuerda.",12,MUTED),6,10);
        RadioGroup choices=new RadioGroup(this);
        EffectStyle selected=EffectStyle.fromId(prefs.getString("effectStyle","classic"));
        for(EffectStyle style:EffectStyle.values()) {
            RadioButton choice=new RadioButton(this);
            choice.setId(View.generateViewId());choice.setTag(style);
            choice.setText(style.title);choice.setTextSize(14);choice.setTextColor(FG);
            choice.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
            choice.setMinHeight(dp(48));
            choices.addView(choice,new RadioGroup.LayoutParams(-1,-2));
            if(style==selected)choices.check(choice.getId());
        }
        effects.addView(choices);
        TextView effectDescription=text(selected.description,12,MUTED);
        add(effects,effectDescription,8,0);
        choices.setOnCheckedChangeListener((group,id)-> {
            RadioButton choice=group.findViewById(id);if(choice==null)return;
            EffectStyle style=(EffectStyle)choice.getTag();
            prefs.edit().putString("effectStyle",style.id).apply();
            effectDescription.setText(style.description);
            CrystalService service=CrystalService.current();
            if(service!=null)service.setEffectStyle(style);
        });

        TextView credit=text("Captura y proyección adaptadas de DuoFold (MIT) · jcx396905-gif",11,MUTED);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0,dp(12),0,dp(8));
        credit.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/jcx396905-gif/DuoFold-Android"))));
        add(body,credit,10,0);

        refresh();
    }
    @Override protected void onResume() {super.onResume();client.connect();handler.post(update);}
    @Override protected void onPause() {handler.removeCallbacks(update);super.onPause();}
    private void refresh() {
        if(bridgeStatus==null)return;
        bridgeStatus.setText(client.message());bridgeStatus.setTextColor(client.service()!=null?ACCENT:MUTED);
        CrystalService s=CrystalService.current();boolean active=s!=null&&s.isActive();
        accessStatus.setText(s!=null?"Servicio de accesibilidad conectado":"Falta activar el servicio de accesibilidad");
        accessStatus.setTextColor(s!=null?ACCENT:MUTED);
        liveStatus.setText(s!=null?s.status():prefs.getString("lastStatus","Cuando Shizuku y la accesibilidad estén listos, prueba 10 segundos"));
        progressText.setText(active?"Apertura "+Math.round(s.progress()*100)+"%  ·  "+s.frames()+" fotogramas":"");
        start.setText(active?"Detener y restaurar la pantalla":"05   Activar efecto global");
        trial.setEnabled(!active && s!=null && client.service()!=null);
        start.setEnabled(active || s!=null && client.service()!=null);
    }
    private void requestStart(boolean isTrial) {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            pendingStart=true;pendingTrial=isTrial;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},51);return;
        }
        startSession(isTrial);
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==51 && pendingStart){pendingStart=false;startSession(pendingTrial);}
    }
    private void startSession(boolean isTrial) {
        CrystalService s=CrystalService.current();
        if(s==null){openAccessibility();return;}
        s.startSession(isTrial);
        if(s.isActive()) moveTaskToBack(true);
    }
    private void openShizuku() {
        Intent launch=getPackageManager().getLaunchIntentForPackage(SHIZUKU);
        if(launch!=null) {startActivity(launch);return;}
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/download/")));
        } catch(Exception e) {
            Toast.makeText(this,"Instala Shizuku y ábrelo para iniciarlo (depuración inalámbrica o ADB).",Toast.LENGTH_LONG).show();
        }
    }
    private void openAccessibility() {
        try {startActivity(new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
                .putExtra("android.intent.extra.COMPONENT_NAME",new ComponentName(this,CrystalService.class)));}
        catch(Exception e){startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(20),dp(20),dp(20));
        GradientDrawable shape=new GradientDrawable();shape.setColor(CARD);shape.setCornerRadius(dp(24));shape.setStroke(dp(1),0xff1e2a40);card.setBackground(shape);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);parent.addView(card,lp);return card;
    }
    private TextView text(String text,int size,int color) {
        TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);return t;
    }
    private void add(LinearLayout p,View v,int top,int bottom) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);lp.bottomMargin=dp(bottom);p.addView(v,lp);
    }
    private Button button(LinearLayout p,String title,Runnable action,boolean filled) {
        Button b=new Button(this);b.setText(title);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(filled?BG:ACCENT);
        GradientDrawable bg=new GradientDrawable();bg.setColor(filled?ACCENT:0xff1a2438);bg.setCornerRadius(dp(16));b.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.topMargin=dp(10);p.addView(b,lp);b.setOnClickListener(v->action.run());return b;
    }
    private interface ValueListener {void changed(float value);}
    private void slider(LinearLayout parent,String label,int min,int max,float initial,ValueListener listener,String unit) {
        TextView value=text(label+"  "+Math.round(initial)+unit,13,MUTED);add(parent,value,18,4);
        SeekBar seek=new SeekBar(this);seek.setMax(max-min);seek.setProgress(Math.round(initial)-min);parent.addView(seek,new LinearLayout.LayoutParams(-1,dp(36)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int p,boolean user){value.setText(label+"  "+(min+p)+unit);if(user)listener.changed(min+p);}
            @Override public void onStartTrackingTouch(SeekBar b){} @Override public void onStopTrackingTouch(SeekBar b){}
        });
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
