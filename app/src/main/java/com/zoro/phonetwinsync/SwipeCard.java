package com.zoro.phonetwinsync;
import android.content.*;import android.graphics.drawable.ColorDrawable;import android.view.*;import android.widget.*;
public class SwipeCard extends FrameLayout {
 interface Listener { void decision(boolean keep); }
 float downX; Listener listener; TextView label;
 public SwipeCard(Context c){
  super(c); setPadding(12,12,12,12); setBackground(new ColorDrawable(0xffeeeeee));
  label=new TextView(c); label.setTextSize(22); label.setGravity(Gravity.CENTER);
  addView(label,new LayoutParams(-1,460));
  setOnTouchListener((v,e)->{
   if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();return true;}
   if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-downX;
    if(Math.abs(dx)>140){boolean keep=dx>0;
     animate().translationX(keep?900:-900).alpha(0).setDuration(180).withEndAction(()->{
      setTranslationX(0);setAlpha(1);if(listener!=null)listener.decision(keep);}).start();}
    return true;} return true;});
 }
 void show(MainActivity.FileItem f,Listener l){listener=l;label.setText((f.mime!=null&&f.mime.startsWith("video/")?"🎬 VIDEO":"🖼 PHOTO")+
 "\\n\\n"+f.name+"\\n\\n← SKIP        KEEP →");}
}
