package com.jinchim.jbind;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jinchim.jbind.annotations.JBind;
import com.jinchim.jbind.annotations.JClick;
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

//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//            }
//        });
        View v = Utils.findView(getWindow().getDecorView(), R.id.btn, "method btn", getClass().getCanonicalName());
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "你点击了我哦！", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @JClick(R.id.btn)
    void onClick(View v) {

    }

    @JClick(R.id.btn)
    void onClick() {

    }

//    @JClick(R.id.btn)
//    void onClick(String v) {
//
//    }

//    @JClick(R.id.btn)
//    void onClick(String v, View v2) {
//
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }




}
