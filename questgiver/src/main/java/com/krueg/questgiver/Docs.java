package com.krueg.questgiver;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

@EActivity
public class Docs extends Activity {

    @ViewById
    WebView webView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docs);
        
        String data = getString(R.string.docs_html);
        webView.loadData(data, "text/html; charset=UTF-8", null);
        
    }
}
