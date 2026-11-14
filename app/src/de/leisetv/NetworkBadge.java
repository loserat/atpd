package de.leisetv;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import org.json.*;

/** Quiet Home status: no card or arrow; the focus outline appears only during navigation. */
final class NetworkBadge extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String label=UiText.t("Netzwerk"),model="",transport="";private int color=TvStyle.MUTED;
    NetworkBadge(Context context,Runnable open){super(context);setFocusable(true);setClickable(true);setDefaultFocusHighlightEnabled(false);setOnClickListener(v->open.run());setOnFocusChangeListener((v,f)->invalidate());update();}
    void update(){try{
        JSONObject state=NetworkStatus.view(getContext());String transport=state.optString("transport"),ssid=state.optString("ssid"),internet=state.optString("internet");String next=transport+"\n"+ssid+"\n"+internet;
        if(model.equals(next))return;model=next;this.transport=transport;label="WLAN".equals(transport)?(ssid.isEmpty()?UiText.t("WLAN"):ssid):state.optBoolean("connected")?UiText.t(transport):"offline".equals(internet)?"Offline":UiText.t("Netzwerk");
        color="validated".equals(internet)?0xff86D6A1:state.optBoolean("connected")?0xffE8BE82:TvStyle.MUTED;
        setContentDescription(UiText.t("Netzwerkstatus, ")+label+", "+NetworkStatus.internetLabel(internet)+("WLAN".equals(transport)&&ssid.isEmpty()?UiText.t(", WLAN-Name nicht freigegeben"):""));invalidate();
    }catch(JSONException ignored){}}
    @Override protected void onDraw(Canvas canvas){canvas.save();canvas.scale(getWidth()/440f,getHeight()/64f);paint.setStyle(Paint.Style.FILL);
        if(isFocused()){paint.setColor(0x18CBF0D9);canvas.drawRoundRect(4,4,436,60,15,15,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(TvStyle.MINT);canvas.drawRoundRect(4,4,436,60,15,15,paint);paint.setStyle(Paint.Style.FILL);}
        paint.setTextSize(23);paint.setTypeface(TvStyle.NORMAL);String text=TvStyle.fit(paint,label,354);float start=416-paint.measureText(text);transportIcon(canvas,start-42);TvStyle.text(canvas,paint,text,start,41,23,TvStyle.MUTED,false);canvas.restore();
    }
    private void transportIcon(Canvas canvas,float x){
        canvas.save();canvas.translate(x,21);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.4f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        if("WLAN".equals(transport)){
            canvas.drawArc(-3,0,35,38,225,90,false,paint);canvas.drawArc(3,6,29,32,225,90,false,paint);canvas.drawArc(9,12,23,26,225,90,false,paint);
            paint.setStyle(Paint.Style.FILL);canvas.drawCircle(16,20,2.4f,paint);
        }else if("LAN".equals(transport)){
            Path plug=new Path();plug.moveTo(3,1);plug.lineTo(29,1);plug.lineTo(29,17);plug.lineTo(23,17);plug.lineTo(23,24);plug.lineTo(9,24);plug.lineTo(9,17);plug.lineTo(3,17);plug.close();canvas.drawPath(plug,paint);
            for(int pin=10;pin<=22;pin+=4)canvas.drawLine(pin,6,pin,11,paint);
        }else{
            canvas.drawCircle(16,12,12,paint);canvas.drawOval(10,0,22,24,paint);canvas.drawLine(4,12,28,12,paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);paint.setStrokeJoin(Paint.Join.MITER);paint.setStyle(Paint.Style.FILL);canvas.restore();
    }
}
