package com.cozybit.onboarding.app.fragments;

import com.cozybit.onboarding.R;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;


public class WelcomeFragment extends Fragment {
	
	private TextView mTextView;
	private ImageView mImageView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.welcome_fragment, container, false);
        
        mImageView = (ImageView) v.findViewById(R.id.welcome_img);
        mTextView = (TextView) v.findViewById(R.id.welcome_swipe_text);
        
        mImageView.setBackgroundResource(R.drawable.wand);
        AnimationDrawable anim = (AnimationDrawable) mImageView.getBackground();
        anim.start();
        
        mTextView.setAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
				R.anim.fade_in_out));

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
    }
}
