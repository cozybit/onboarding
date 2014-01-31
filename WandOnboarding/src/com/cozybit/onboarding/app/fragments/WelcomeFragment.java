package com.cozybit.onboarding.app.fragments;

import com.cozybit.onboarding.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;


public class WelcomeFragment extends Fragment {
	
	private TextView mTextView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.welcome_fragment, container, false);
        
        mTextView = (TextView) v.findViewById(R.id.welcome_swipe_text);
        mTextView.setAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
				R.anim.fade_in_out));

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
    }
}
