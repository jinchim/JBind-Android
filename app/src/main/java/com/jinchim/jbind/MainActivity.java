package com.jinchim.jbind;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import com.jinchim.jbind.annotations.JBind;
import com.jinchim.jbind_sdk.JBindSDK;
import com.jinchim.jbind_sdk.Unbinder;
import com.jinchim.jbind_sdk.Utils;

public class MainActivity extends AppCompatActivity {

    @JBind(R.id.tv) TextView textView;
    @JBind(R.id.tv2) TextView textView2;

    Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = JBindSDK.bind(this);

        textView.setText("activity1");
        textView2.setText("activity2");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }




}
