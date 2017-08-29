package com.jinchim.jbind;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jinchim.jbind.annotations.Bind;
import com.jinchim.jbind.annotations.Click;
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

        textView.setText("activity1");
        textView2.setText("activity2");
    }

    @Click({R.id.btn, R.id.btn2})
    void onClickBtn(View v) {
        switch (v.getId()) {
            case R.id.btn:
                Toast.makeText(MainActivity.this, "你点击了我哦！", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn2:
                Toast.makeText(MainActivity.this, "你点击了我哦2！", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }


}
