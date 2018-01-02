package com.lwh8762.wolframalpha;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    
    private int width = 0;
    
    private InputMethodManager imm = null;
    
    private WolframClient client = null;
    private ResultListener resultListener = null;
    
    private EditText queryInput = null;
    private ListView resultView = null;
    private ArrayList<ResultData> resultImgList = null;
    private ResultArrayAdapter resultArrayAdapter = null;
    private ProgressBar progressBar1 = null;
    private ProgressBar progressBar2 = null;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        width = point.x / 2;
        
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        
        client = new WolframClient(this, (LinearLayout) findViewById(R.id.l));
        client.setClientListener(new WolframClient.ClientListener() {
            @Override
            public void onProgressChanged(int progress) {
                progressBar1.setProgress(progress);
                if (progress == 100) {
                    progressBar1.setVisibility(View.INVISIBLE);
                    progressBar2.setVisibility(View.VISIBLE);
                }
            }
    
            @Override
            public void onLoad(String html) {
                handleResults(html);
            }
        });
    
        queryInput = (EditText) findViewById(R.id.queryInput);
        queryInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Toast.makeText(MainActivity.this, "" + v.getText().toString(), Toast.LENGTH_SHORT).show();
                    client.go(v.getText().toString());
                    progressBar1.setVisibility(View.VISIBLE);
                    imm.hideSoftInputFromWindow(queryInput.getWindowToken(), 0);
                }
                return false;
            }
        });
        
        resultView = (ListView) findViewById(R.id.resultView);
        resultImgList = new ArrayList<>();
        resultArrayAdapter = new ResultArrayAdapter(this, resultImgList);
        resultView.setAdapter(resultArrayAdapter);
        
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar2 = (ProgressBar) findViewById(R.id.progressBar2);
    }
    
    private void handleResults(String html) {
        try {
            FileOutputStream fos = new FileOutputStream("/sdcard/out.html");
            fos.write(html.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new HandlerThread(html).start();
    }
    
    private class ResultArrayAdapter extends ArrayAdapter<ResultData> {
        private LayoutInflater li = null;
        private Context context = null;
        private ArrayList<ResultData> arrayList = null;
        
        public ResultArrayAdapter(Context context, ArrayList<ResultData> arrayList) {
            super(context, 0, arrayList);
            this.context = context;
            this.arrayList = arrayList;
            li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
    
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ResultData data = arrayList.get(position);
            convertView = li.inflate(R.layout.result_view, null);
            
            TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
            titleView.setText(data.getTitle());
            
            LinearLayout subLayout = (LinearLayout) convertView.findViewById(R.id.subLayout);
            Bitmap[] bitmaps = data.getBitmaps();
            for (Bitmap bmp : bitmaps) {
                int bmpWidth = bmp.getWidth() * 5;
                bmpWidth = bmpWidth > width ? width : bmpWidth;
                
                ImageView imageView = new ImageView(context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(bmpWidth, bmpWidth * bmp.getHeight() / bmp.getWidth()));
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                imageView.setImageBitmap(bmp);
                subLayout.addView(imageView);
            }
            
            return convertView;
        }
    }
    
    private class HandlerThread extends Thread {
        private String html = null;
        
        public HandlerThread(String html) {
            this.html = html;
        }
        
        @Override
        public void run() {
            super.run();
            try {
                resultImgList.clear();
                Document document = Jsoup.parse(html);
                Log.i("html", html);
                Elements pods = document.select(".pod");
                Log.i("Podcnd", "t: " + pods.size());
                
                for (Element pod : pods) {
                    Elements subs = pod.select(".sub");
                    String title = pod.select("h2").text();
                    if (pod.attr("display").equals("none") || pod.tagName().equals("section") || title.isEmpty()) continue;
                    Log.i("Title", "t: " + title);
                    int subCount = subs.size();
                    Bitmap[] bitmaps = new Bitmap[subCount];
                    for (int i = 0;i < subCount;i ++) {
                        String url = subs.get(i).select("img").attr("src");
                        bitmaps[i] = BitmapFactory.decodeStream(new URL(url).openStream());
                    }
                    
                    resultImgList.add(new ResultData(title, bitmaps));
                }
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultArrayAdapter.notifyDataSetChanged();
                        progressBar2.setVisibility(View.INVISIBLE);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private interface ResultListener {
        public void onParsed(Bitmap bitmap);
    }
    
    private class ResultData {
        private String title = null;
        private Bitmap[] bitmaps = null;
    
        public ResultData(String title, Bitmap[] bitmaps) {
            this.title = title;
            this.bitmaps = bitmaps;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    
        public void setBitmaps(Bitmap[] bitmaps) {
            this.bitmaps = bitmaps;
        }
    
        public String getTitle() {
            return title;
        }
    
        public Bitmap[] getBitmaps() {
            return bitmaps;
        }
    }
}
