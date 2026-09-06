package com.zoro.phonetwinsync;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout content;
    TextView status, selection;
    ArrayList<FileItem> items = new ArrayList<>();
    ArrayList<FileItem> selected = new ArrayList<>();
    int viewMode = 0; // list, grid, swipe
    String filter = "Photos";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        build();
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8);
        scan("Photos");
    }

    TextView tv(String s,float z) {
        TextView v=new TextView(this); v.setText(s); v.setTextSize(z);
        v.setPadding(10,10,10,10); return v;
    }
    Button btn(String s) { Button b=new Button(this); b.setText(s); return b; }

    void build() {
        ScrollView sv=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,18,18,18);
        sv.addView(root); setContentView(sv);

        root.addView(tv("Phone Twin Sync",28));
        status=tv("STATUS: Ready — no sync or deletion is running.",14);
        root.addView(status);
        selection=tv("SELECTED: 0",14); root.addView(selection);

        LinearLayout actions=new LinearLayout(this);
        Button sync=btn("🔄 SYNC REVIEW");
        Button del=btn("🗑 DELETE REVIEW");
        sync.setOnClickListener(v->syncReview());
        del.setOnClickListener(v->deleteReview());
        actions.addView(sync,new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(del,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(actions);

        root.addView(tv("CHOOSE WHAT TO SYNC",16));
        LinearLayout filters=new LinearLayout(this);
        String[] fs={"📷 Photos","🎬 Videos","📷🎬 Both"};
        for(String s:fs) {
            Button b=btn(s);
            b.setOnClickListener(v->scan(s.contains("Videos")&&!s.contains("Both")?"Videos":s.contains("Both")?"Both":"Photos"));
            filters.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(filters);

        Button pick=btn("☑ SELECT SPECIFIC FILES");
        pick.setOnClickListener(v->pickFiles()); root.addView(pick);

        root.addView(tv("VIEW MODE",16));
        LinearLayout modes=new LinearLayout(this);
        Button list=btn("☰ List"), grid=btn("▦ Grid"), swipe=btn("↔ Swipe");
        list.setOnClickListener(v->{viewMode=0;render();});
        grid.setOnClickListener(v->{viewMode=1;render();});
        swipe.setOnClickListener(v->{viewMode=2;render();});
        modes.addView(list,new LinearLayout.LayoutParams(0,-2,1));
        modes.addView(grid,new LinearLayout.LayoutParams(0,-2,1));
        modes.addView(swipe,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(modes);

        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);
    }

    void scan(String f) {
        filter=f; items.clear(); selected.clear();
        String[] p={MediaStore.Downloads._ID,MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,MediaStore.Downloads.MIME_TYPE};
        try(Cursor c=getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,p,null,null,
                MediaStore.Downloads.DATE_MODIFIED+" DESC")) {
            if(c!=null) while(c.moveToNext()) {
                String mime=c.getString(3);
                boolean ph=mime!=null&&mime.startsWith("image/");
                boolean vi=mime!=null&&mime.startsWith("video/");
                if(f.equals("Photos")&&!ph) continue;
                if(f.equals("Videos")&&!vi) continue;
                if(f.equals("Both")&&!ph&&!vi) continue;
                items.add(new FileItem(c.getString(0),c.getString(1),c.getLong(2),mime));
            }
        } catch(Exception e) { status.setText("STATUS: Scan error — "+e.getMessage()); }
        selected.addAll(items);
        status.setText("STATUS: Ready for review — "+items.size()+" item(s).");
        updateCount(); render();
    }

    void pickFiles() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,55);
    }

    @Override protected void onActivityResult(int r,int c,Intent d) {
        super.onActivityResult(r,c,d);
        if(r!=55||c!=RESULT_OK||d==null)return;
        items.clear(); selected.clear();
        if(d.getClipData()!=null)
            for(int i=0;i<d.getClipData().getItemCount();i++) addUri(d.getClipData().getItemAt(i).getUri());
        else if(d.getData()!=null) addUri(d.getData());
        selected.addAll(items); status.setText("STATUS: Files selected — ready for review.");
        updateCount(); render();
    }

    void addUri(Uri u) {
        String n=u.getLastPathSegment();
        items.add(new FileItem(u.toString(),n==null?u.toString():n,0,getContentResolver().getType(u)));
    }

    void updateCount() { selection.setText("SELECTED FOR SYNC: "+selected.size()+" / "+items.size()); }

    Bitmap thumbnail(FileItem f) {
        try {
            if(Build.VERSION.SDK_INT>=29 && f.id.matches("\\d+")) {
                long id=Long.parseLong(f.id);
                if(f.mime!=null&&f.mime.startsWith("video/"))
                    return MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(),id,MediaStore.Video.Thumbnails.MINI_KIND,null);
                if(f.mime!=null&&f.mime.startsWith("image/"))
                    return MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(),id,MediaStore.Images.Thumbnails.MINI_KIND,null);
            }
        } catch(Exception ignored) {}
        return null;
    }

    void render() {
        content.removeAllViews();
        if(viewMode==0) listView();
        else if(viewMode==1) gridView();
        else swipeView();
    }

    void listView() {
        content.addView(tv("LIST VIEW — every item has its own SYNC switch.",15));
        for(FileItem f:items) content.addView(row(f,110));
    }

    View row(FileItem f,int size) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView iv=new ImageView(this);
        iv.setLayoutParams(new LinearLayout.LayoutParams(size,size)); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bm=thumbnail(f);
        if(bm!=null) iv.setImageBitmap(bm); else iv.setImageResource(android.R.drawable.ic_menu_gallery);
        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
        info.addView(tv((f.mime!=null&&f.mime.startsWith("video/"))?"🎬 VIDEO":"🖼 PHOTO",11));
        info.addView(tv(f.name,15)); info.addView(tv(f.size>0?human(f.size):"Selected file",12));
        CheckBox cb=new CheckBox(this); cb.setText("SYNC"); cb.setChecked(selected.contains(f));
        cb.setOnCheckedChangeListener((b,on)->{if(on&&!selected.contains(f))selected.add(f);if(!on)selected.remove(f);updateCount();});
        row.addView(iv); row.addView(info,new LinearLayout.LayoutParams(0,-2,1)); row.addView(cb);
        return row;
    }

    void gridView() {
        content.addView(tv("GRID VIEW — visual previews for photos and videos.",15));
        GridLayout g=new GridLayout(this); g.setColumnCount(2);
        for(FileItem f:items) {
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(5,5,5,5);
            ImageView iv=new ImageView(this); iv.setLayoutParams(new LinearLayout.LayoutParams(240,240)); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bm=thumbnail(f); if(bm!=null)iv.setImageBitmap(bm); else iv.setImageResource(android.R.drawable.ic_menu_gallery);
            card.addView(iv); card.addView(tv(f.name,13));
            CheckBox cb=new CheckBox(this); cb.setText("SYNC"); cb.setChecked(selected.contains(f));
            cb.setOnCheckedChangeListener((b,on)->{if(on&&!selected.contains(f))selected.add(f);if(!on)selected.remove(f);updateCount();});
            card.addView(cb); g.addView(card);
        }
        content.addView(g);
    }

    void swipeView() {
        content.addView(tv("SWIPE REVIEW",20));
        content.addView(tv("← LEFT = SKIP / DON'T SYNC     RIGHT = KEEP / SYNC →",14));
        if(items.isEmpty()){content.addView(tv("No files in this queue.",15));return;}
        SwipeCard card=new SwipeCard(this);
        card.show(items.get(0),keep->{
            FileItem f=items.remove(0); selected.remove(f);
            if(keep)selected.add(f);
            render(); updateCount();
        });
        content.addView(card);
    }

    void syncReview() {
        status.setText("STATUS: SYNC REVIEW — nothing is being copied yet.");
        if(selected.isEmpty()){Toast.makeText(this,"No files selected.",Toast.LENGTH_SHORT).show();return;}
        StringBuilder s=new StringBuilder("SEND THESE FILES TO THE OTHER PHONE?\\n\\n");
        for(FileItem f:selected)s.append("✓ ").append(f.name).append("\\n");
        s.append("\\nThis approval is only for syncing. Deletion is NOT included.");
        new AlertDialog.Builder(this).setTitle("SYNC — FINAL REVIEW").setMessage(s.toString())
            .setPositiveButton("APPROVE SYNC",(d,w)->{
                status.setText("STATUS: SYNC APPROVED — "+selected.size()+" file(s).");
                Toast.makeText(this,"Sync approved. Secure transfer engine will use only these files.",Toast.LENGTH_LONG).show();
            }).setNegativeButton("CANCEL",null).show();
    }

    void deleteReview() {
        status.setText("STATUS: DELETE REVIEW — no files are being deleted.");
        new AlertDialog.Builder(this).setTitle("DELETE — SEPARATE REVIEW")
            .setMessage("Deletion is intentionally separate from syncing.\\n\\nThe final two-phone engine will list individual deletion candidates here and require a separate approval for each selected deletion.")
            .setPositiveButton("UNDERSTOOD",null).show();
    }

    String human(long n){String[] u={"B","KB","MB","GB"};double x=n;int i=0;while(x>1024&&i<3){x/=1024;i++;}return String.format(Locale.US,"%.1f %s",x,u[i]);}

    static class FileItem {
        String id,name,mime; long size;
        FileItem(String i,String n,long s,String m){id=i;name=n;size=s;mime=m;}
        public boolean equals(Object o){return o instanceof FileItem && id.equals(((FileItem)o).id);}
        public int hashCode(){return id.hashCode();}
    }
}
