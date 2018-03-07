package com.lwh8762.wolframalpha;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

/**
 * Created by W on 2017-12-17.
 */

public class WolframClient {
    
    private WebView webView = null;
    
    private ClientListener clientListener = null;
    
    public WolframClient(Context context, LinearLayout linearLayout) {
        webView = new WebView(context);
        webView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                clientListener.onProgressChanged(newProgress);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.loadUrl("javascript:var a = setInterval(function () {  if (document.getElementsByClassName(\"pod loading\").length == 0 && (document.getElementsByClassName(\"pod imgLoading\").length == 0 || document.getElementsByClassName(\"pod imgLoading\")[0].style.display == \"none\")) {  window.AndroidInterface.onHtml(document.getElementById(\"content\").innerHTML);  clearInterval(a);  }  }, 100); ");
                if (url.startsWith("http")) {
                
                }
            }
        });
        webView.addJavascriptInterface(new Object() {
            
            @JavascriptInterface
            public void onHtml(String html) {
                clientListener.onLoad(html);
            }
            
        }, "AndroidInterface");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        //linearLayout.addView(webView);
    }
    
    public void go(String query) {
        query = query.replaceAll("[+]", "%2B");
        webView.loadUrl("http://m.wolframalpha.com/input/?i=" + query);
    }
    
    public void setClientListener(ClientListener clientListener) {
        this.clientListener = clientListener;
    }
    
    public interface ClientListener {
        public void onProgressChanged(int progress);
        public void onLoad(String html);
    }
}
