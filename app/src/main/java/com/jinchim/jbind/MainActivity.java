package com.jinchim.jbind;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jinchim.jbind.annotations.Bind;
import com.jinchim.jbind_sdk.JBindSDK;
import com.jinchim.jbind_sdk.Unbinder;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.tv) TextView textView;
    @Bind(R.id.tv2) TextView textView2;

    Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = JBindSDK.bind(this);

        textView.setText("tv1");
        textView2.setText("tv2");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }



}
