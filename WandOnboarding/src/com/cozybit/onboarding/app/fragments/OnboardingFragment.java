package com.cozybit.onboarding.app.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.cozybit.onboarding.R;
import com.cozybit.onboarding.app.OnboardingActivity;

public class OnboardingFragment extends Fragment {
		
	private ViewFlipper mFlipper;
	private Handler mHandler;
	private TextView mTextView;
	private TextView mTextView2;
	private EditText mStatus;
	private EditText mLongStatus;
	private Animation mAnimationFadeIn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.scanning_fragment, container, false);

        mAnimationFadeIn = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
				android.R.anim.fade_in);
		       
        //Getting View Flipper from main.xml and assigning to flipper reference variable
        mFlipper = (ViewFlipper)v.findViewById(R.id.viewFlipper1);
        mTextView = (TextView)v.findViewById(R.id.textView);
        mTextView2 = (TextView)v.findViewById(R.id.textView2);
        
        mStatus = (EditText)v.findViewById(R.id.status_edittext);
        mLongStatus = (EditText)v.findViewById(R.id.longstatus_edittext);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
        	mHandler.sendEmptyMessage(OnboardingActivity.START_SCANNING);
        	startFlipping();
        } else {
        	mHandler.sendEmptyMessage(OnboardingActivity.STOP_SCANNING);
        	stopFlipping();
        }
    }
    
	private void startFlipping() {
		mFlipper.setFlipInterval(500);	//setting the interval 500 milliseconds
		mFlipper.setInAnimation(AnimationUtils.loadAnimation(getActivity(),
								android.R.anim.fade_in));
        mFlipper.startFlipping();    //views flipping starts.
	}
    
    private void stopFlipping() {
		if (mFlipper != null)
			mFlipper.stopFlipping();
		if (mTextView != null)
			mTextView.setText(R.string.identifying);
		if (mTextView2 != null) {
			mTextView2.setAnimation(null);
			mTextView2.setVisibility(TextView.INVISIBLE);
		}
	}

	public void setHandler(Handler handler) {
		mHandler = handler;
	}

	public void detectedDevice() {
		mFlipper.stopFlipping();
		mFlipper.setDisplayedChild(0);
		mTextView.startAnimation(mAnimationFadeIn);
		mTextView.setText(R.string.detected);
        mTextView2.setAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
				R.anim.fade_in_out));
        mTextView2.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				mHandler.sendEmptyMessage(OnboardingActivity.CONNECT_DEVICE);
			}
		});
	}

	public void updateStatus(String status) {
		mStatus.setText(status);
	}

	public void updateLongStatus(String longStatus) {
		mLongStatus.setText(longStatus);
	}
}