package com.jinchim.jbind;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jinchim.jbind.annotations.Bind;
import com.jinchim.jbind.annotations.Click;
import com.jinchim.jbind_sdk.JBindSDK;
import com.jinchim.jbind_sdk.Unbinder;


public class MainFragment extends Fragment {

    @Bind(R.id.tv) TextView textView;
    @Bind(R.id.tv2) TextView textView2;

    Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = JBindSDK.bind(this, view);

        textView.setText("fragment1");
        textView2.setText("fragment2");
    }

    @Click({R.id.btn, R.id.btn2})
    void onClickBtn(View v) {
        switch (v.getId()) {
            case R.id.btn:
                Toast.makeText(getActivity(), "你点击了我哦", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn2:
                Toast.makeText(getActivity(), "你点击了我哦2", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

}
