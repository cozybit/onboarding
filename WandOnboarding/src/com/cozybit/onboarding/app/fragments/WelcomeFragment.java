package com.cozybit.onboarding.app.fragments;

import com.cozybit.onboarding.R;
import com.cozybit.onboarding.app.OnboardingActivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;


public class WelcomeFragment extends Fragment {
	
	private TextView mTextView;
	private Handler mHandler;
	private Activity mActivity;

	public interface IUiUpdater {
		public void updateValue(byte value);
	}
	
	private IUiUpdater mUiUpdater = new IUiUpdater() {
		
		@Override
		public void updateValue(final byte value) {
			mActivity.runOnUiThread( new Runnable() {
				@Override
				public void run () {
					mTextView.setText("" + value);
				}
			});
		}
	};
	
	public IUiUpdater getUiUpdater() {
		return mUiUpdater;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this.getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.welcome_fragment, container, false);
        
        mTextView = (TextView) v.findViewById(R.id.read_value);
        mTextView.setAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
				R.anim.fade_in_out));

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
     
        } else {
        	mHandler.sendEmptyMessage(OnboardingActivity.STOP_SCANNING);
        }
    }
    
	public void setHandler(Handler handler) {
		mHandler = handler;
	}
	
	public TextView getTextView() {
		return mTextView; 
	}
}
