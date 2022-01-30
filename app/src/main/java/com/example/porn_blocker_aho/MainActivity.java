package com.example.porn_blocker_aho;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;


import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import static android.content.ContentValues.TAG;
import static java.lang.Character.isLetter;
import static java.lang.Character.isLowerCase;



public class MainActivity extends AppCompatActivity{
    private EditText url_n;
    private ProgressBar prepro;
    private Button go_butt;
    private Button url_butt;
    private ImageView Porn_signal;
    private WebView webView;
    private TextView text_desc;
    private TextView expected_class;
    public static int SDK_INT = android.os.Build.VERSION.SDK_INT;
    private String porn_keywords[]={"horny","bbc","orgasm","nude","naked","dildo","threesome","bitch","gangbang","xxx","cumshot","cum","blowjob","bimbo",
            "squirt","ebony","tits","busty","whore","slut","cunt","anal", "gay","fuck","lesbian","porn","porno","sex","sexy","boobs","pussy","dick",
            "handjob","fingering","booty","creampie","butt","chick","milf","cougar","cuckold","deepthroat","hentai","doggystyle","milfy","bondage",
            "bbw","escort","erotic","incest","hotmom","orgy","puba","stepmom","stepsis","spanking","sissy","shemale","taboo","virgin","cfnm","cmnf"};
    HashMap<Integer,String> index_2_string=new HashMap<Integer,String>();
    Set<String> found_porn_words = new HashSet<String>();


    int K = 26;

    public class vertex{
        int[] next=new int[K];
        Boolean leaf=false;
        int p=-1;
        char pch;
        int green_link=-1;
        int link=-1;
        int[] go=new int[K];
        public vertex(int pp,char ch){
            p=pp; pch=ch;
            for(int i=0;i<K;i++){
                next[i]=-1; go[i]=-1;
            }
        }
    };

    ArrayList<vertex> tree=new ArrayList<>();

    void add_string(String s){
        int v=0;
        for(int i=0;i<s.length();i++){
            int c=s.charAt(i)-'a';
            if(c<0){
                Log.e(TAG, String.valueOf(s.charAt(i))+String.valueOf(c));  return;}
            if(tree.get(v).next[c]==-1){
                tree.get(v).next[c]=tree.size(); vertex tmp=new vertex(v,s.charAt(i));
                tree.add(tmp);
            }
            v=tree.get(v).next[c];
        }
        index_2_string.put(v,s);
        tree.get(v).leaf=true;
    }

    int get_link(int v){
        if(tree.get(v).link==-1){
            if(v==0||tree.get(v).p==0){
                tree.get(v).link=0;
            }else{
                tree.get(v).link=go(get_link(tree.get(v).p),tree.get(v).pch);
            }
        }
        return tree.get(v).link;
    }

    int go(int v,int ch){
        int c=ch-'a';

        if(tree.get(v).go[c]==-1){
            if(tree.get(v).next[c]!=-1){
                tree.get(v).go[c]=tree.get(v).next[c];
            }else{
                if(v==0){ tree.get(v).go[c]=0; }else{
                    tree.get(v).go[c]=go(get_link(v),ch);
                }
            }
        }
        return tree.get(v).go[c];
    }

    void initialise_suffix(){
        Queue<Integer> q = new LinkedList<>(); q.add(0);
        while (!q.isEmpty()){
            int cur=q.remove(); get_link(cur);
            int ct=0;
            for(int i=0;i<26;i++){
                if(tree.get(cur).next[i]!=-1){ q.add(tree.get(cur).next[i]);  }
                go(cur,i+'a');
            }

            if(tree.get(cur).link>0){
                if(tree.get(tree.get(cur).link).leaf){ tree.get(cur).green_link=tree.get(cur).link; }
                else{
                    tree.get(cur).green_link=tree.get(tree.get(cur).link).green_link;
                }
            }

        }
    }

    String porn_hai="";
    void print_green_links(int v){
        if(v>0){
            if(tree.get(v).leaf){ found_porn_words.add(index_2_string.get(v)); //porn_hai=porn_hai+" "+index_2_string.get(v);
            }
            print_green_links(tree.get(v).green_link);
        }
    }

    void occurence_print(String str){
        int v=0;
        for(int c=0;c<str.length();c++){
            int i=str.charAt(c)-'a';
            //  Log.e("TAG3",String.valueOf("DEKH "+str.charAt(c))+" "+String.valueOf(i)+" "+String.valueOf(v));
            if(i<0||i>25||v<0){ Log.e("TAG2",String.valueOf(str.charAt(c))+" "+String.valueOf(i)+" "+String.valueOf(v)); v=0; continue;}
            print_green_links(tree.get(v).go[i]); v=tree.get(v).go[i];
        }
    }


    boolean error_thi;
    String filtered_FULL_page_doc="";
    String Total_errors="";

    final Boolean[] load_again_webview = {true};

    public MainActivity(){
        vertex top= new vertex(-1,'$');
        tree.add(top);
        for(String str:porn_keywords){
            add_string(str);
        }
        initialise_suffix();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Simple_browser);
        setContentView(R.layout.activity_main);
        url_n=(EditText) findViewById(R.id.textInputEditText);
        go_butt=(Button) findViewById(R.id.button);
        text_desc=(TextView)findViewById(R.id.textView3);
        text_desc.setMovementMethod(new ScrollingMovementMethod());
        webView = (WebView) findViewById(R.id.webView);
        expected_class=(TextView) findViewById(R.id.textView);
        expected_class.setMovementMethod(new ScrollingMovementMethod());
        url_butt=(Button)findViewById(R.id.button2);
        prepro=(ProgressBar)findViewById(R.id.progressBar);
        prepro.setVisibility(View.INVISIBLE);
        Porn_signal=(ImageView)findViewById(R.id.imageView);


        webView.setWebViewClient(new myWebViewClient());


        go_butt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Total_errors="";

                try {
                    switch (view.getId()) {
                        case R.id.button:
                            new MyTask().execute(10);
                            break;
                    }
                }catch (Exception ex){
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                }

                String url=url_n.getText().toString();

                Boolean isUrl=false;
                for(int i=0;i<url.length();i++){
                    if(url.charAt(i)=='.'){ isUrl=true;}
                }

                if(!isUrl){
                    url = "www.google.com/search?q="+url.replace(" ", "%20");
                }





                if(load_again_webview[0]) {
                    webView.loadUrl(url);
                    webView.getSettings().setLoadsImagesAutomatically(true);
                    webView.getSettings().setJavaScriptEnabled(true);
                }
                go_butt.setClickable(true);
                load_again_webview[0] =true;

            }
        });

        url_butt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    go_butt.setClickable(false);
                    String cur_url = webView.getUrl().toString();
                    String[] arrStr = cur_url.split("//", 0);
                    Total_errors="";
                    url_n.setText(arrStr[1]);
                    load_again_webview[0]=false;
                    go_butt.performClick();
                }catch (Exception ex){
                    text_desc.setText(" First load the webpage correctly then  fetch current url\n"+ex.getMessage().toString());
                    go_butt.setClickable(true);
                }
            }
        });

    }

    class MyTask extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {


            String url=url_n.getText().toString();


            Document doc=null;
            String userAgent = System.getProperty("http.agent");
            String FULL_page_doc="";


            try {
                if (SDK_INT >= 10) {
                    StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
                    StrictMode.setThreadPolicy(tp);
                }
                String nrl="http://"+url;
                doc = Jsoup.connect(nrl).userAgent(userAgent).get();
            } catch (IOException e) {
                Total_errors=Total_errors+"\na: "+e.getMessage().toString();
                try{
                    if (SDK_INT >= 10) {
                        StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;
                        StrictMode.setThreadPolicy(tp);
                    }
                    String nrl="https://"+url;
                    doc = Jsoup.connect(nrl).userAgent(userAgent).get();
                }catch(Exception ex){
                    Total_errors=Total_errors+"\nb: "+e.getMessage().toString();
                }
                e.printStackTrace();
            }
            String str1="";
            error_thi=false;
            try{
                str1=str1+ doc.select("meta[name=description]").get(0)
                        .attr("content").toString();
                FULL_page_doc=doc.body().text();
            }
            catch(Exception e1) {
                try {
                    str1 = doc.body().text();
                    int len=str1.length();
                    str1= str1.substring(Math.min(len,50),Math.min(10050,len));
                    FULL_page_doc=doc.body().text();
                } catch (Exception ex) {
                    // Toast.makeText(getApplicationContext(), "1: " + ex.getMessage().toString(), Toast.LENGTH_LONG).show();
                    Total_errors=Total_errors+"\nc: "+ex.getMessage().toString();
                    error_thi = true;

                }
            }


            FULL_page_doc=FULL_page_doc.toLowerCase();
            int len_FULL=FULL_page_doc.length();
            FULL_page_doc=FULL_page_doc.substring(Math.min(len_FULL,0),Math.min(100000,len_FULL));


            filtered_FULL_page_doc=FULL_page_doc;
            for(int i=0;i<FULL_page_doc.length();i++){
                if(isLowerCase(FULL_page_doc.charAt(i))||FULL_page_doc.charAt(i)==' ') filtered_FULL_page_doc=filtered_FULL_page_doc+FULL_page_doc.charAt(i);
            }

            return "Task Completed.";
        }
        @Override
        protected void onPostExecute(String result) {
            occurence_print(filtered_FULL_page_doc);
            if(!found_porn_words.isEmpty()){
                Porn_signal.setImageDrawable(getApplicationContext().getDrawable(R.mipmap.red_button_2));
                expected_class.setText("PORN CONTENT FOUND");
            }else{
                Porn_signal.setImageDrawable(getApplicationContext().getDrawable(R.mipmap.green_button));
                expected_class.setText("NO PORN CONTENT");
            }
            for(String s:found_porn_words){
                porn_hai=porn_hai+" "+s;
            } found_porn_words.clear();
            text_desc.setText("Porn_keywords: "+porn_hai);

            //expected_class.setBackgroundDrawable(getApplicationContext().getDrawable(R.mipmap.green_button));
            if(error_thi){
                text_desc.setText("There is some error in loading the page or getting inference on text.\nErrors:"+Total_errors);
                Toast.makeText(getApplicationContext(),"There is some error in loading the page or getting inference on text",Toast.LENGTH_LONG).show();

            }
            prepro.setVisibility(View.INVISIBLE);
        }
        @Override
        protected void onPreExecute() {
            prepro.setVisibility(View.VISIBLE);
            Total_errors="";
            porn_hai="";
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            try{
            }catch (Exception ex){
                Toast.makeText(getApplicationContext(), "progress error", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public static String getAssetJsonData(Context context,String name) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(name);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        Log.e("data", json);
        return json;

    }

    private class myWebViewClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                view.loadUrl(url);
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            return true;
        }
    }

}
