package com.cozybit.onboarding.app.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.cozybit.onboarding.R;
import com.cozybit.onboarding.app.OnboardingActivity;

public class OnboardingFragment extends Fragment {

	private LinearLayout mRootLayout;
	private LinearLayout mProgressLayout;
	private ViewFlipper mFlipper;
	private ImageView mDetectedDevImg;
	private Handler mHandler;
	private TextView mTextView;
	private TextView mTextView2;
	private EditText mStatus;
	private EditText mLongStatus;
	private Animation mAnimationFadeIn;
	private Button mButton;
	
	private String mVendorId;
	private String mDeviceId;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.scanning_fragment, container, false);

        mRootLayout = (LinearLayout) v.findViewById(R.id.rootLayout);
        mProgressLayout = (LinearLayout) v.findViewById(R.id.progressLayout);

        mAnimationFadeIn = AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
				android.R.anim.fade_in);
		       
        //Getting View Flipper from main.xml and assigning to flipper reference variable
        mFlipper = (ViewFlipper)v.findViewById(R.id.viewFlipper1);
        addImageViewsToFlipper();
        
        mDetectedDevImg = (ImageView) v.findViewById(R.id.detectedDev);
        
        mTextView = (TextView)v.findViewById(R.id.textView);
        mTextView2 = (TextView)v.findViewById(R.id.textView2);
        
        mStatus = (EditText)v.findViewById(R.id.status_edittext);
        mLongStatus = (EditText)v.findViewById(R.id.longstatus_edittext);
        mButton = (Button)v.findViewById(R.id.show_device_web);
        
        mButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openWebURL();
			}
		});

        return v;
    }

    private void addImageViewsToFlipper() {
    	// List of Images
    	int gallery_grid_Images[]= {R.drawable.blender, R.drawable.coffee, 
    			R.drawable.dishwasher, R.drawable.dvd, R.drawable.fan, 
    			R.drawable.fridge, R.drawable.mixer, R.drawable.oven,
    			R.drawable.radio, R.drawable.stereo, R.drawable.toaster
    	};

    	ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(
    			FrameLayout.LayoutParams.WRAP_CONTENT,
    			FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
    	
    	// Create views and add it, last one will be our toaster
    	for (int res : gallery_grid_Images) {
    		 ImageView image = new ImageView(getActivity().getApplicationContext());
    		 image.setBackgroundResource(res);
    		 image.setLayoutParams(params);
    		 mFlipper.addView(image);
		}
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
        	mDetectedDevImg.setVisibility(View.GONE);
        	mFlipper.setVisibility(View.VISIBLE);
        	startFlipping();
        } else {
        	mHandler.sendEmptyMessage(OnboardingActivity.STOP_SCANNING);
        	resetUI();
        }
    }
       
    MenuItem disc_menu_opt;
    MenuItem reset_menu_opt;
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.menu, menu);
    	disc_menu_opt = menu.findItem(R.id.disconnect_option);
    	reset_menu_opt = menu.findItem(R.id.reset_option);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected (MenuItem item) {

    	switch( item.getItemId() ) {
    		case R.id.disconnect_option:
    			mHandler.sendEmptyMessage(OnboardingActivity.DISCONNECT_TARGET);
    			break;
    		case R.id.reset_option:
    			mHandler.sendEmptyMessage(OnboardingActivity.RESET_TARGET);
    			break;
    	}

    	return super.onContextItemSelected(item);
    }
    
	private void startFlipping() {
		mFlipper.setFlipInterval(300);	//setting the interval 500 milliseconds
		mFlipper.setInAnimation(AnimationUtils.loadAnimation(getActivity(),
								android.R.anim.fade_in));
        mFlipper.startFlipping();    //views flipping starts.
	}
    
    private void resetUI() {
		if (mFlipper != null) {
			mFlipper.stopFlipping();
			mFlipper.setVisibility(View.VISIBLE);
		}
		if (mDetectedDevImg != null )
			mDetectedDevImg.setVisibility(View.GONE);
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
		mFlipper.setVisibility(View.GONE);
		mDetectedDevImg.setVisibility(View.VISIBLE);		
		mTextView.startAnimation(mAnimationFadeIn);
		mTextView.setText(R.string.detected);
        mTextView2.setAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(),
				R.anim.fade_in_out));

        mRootLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mHandler.sendEmptyMessage(OnboardingActivity.CONNECT_DEVICE);
				mProgressLayout.setVisibility(View.VISIBLE);
				mTextView2.setAnimation(null);
				mTextView2.setVisibility(View.INVISIBLE);
			}
		});
	}

	public void updateStatus(String status) {
		if (status.equals("CONNECTED")) {
			mButton.setVisibility(Button.VISIBLE);
			disc_menu_opt.setVisible(true);
			reset_menu_opt.setVisible(true);
			mProgressLayout.setVisibility(View.INVISIBLE);
		} else {
			disc_menu_opt.setVisible(false);
			reset_menu_opt.setVisible(false);
		}
		mStatus.setText(status);
	}

	public void updateLongStatus(String longStatus) {
		mLongStatus.setText(longStatus);
	}
	
	public void openWebURL() {
		String url = "https://cozyonboard.appspot.com/status/" + mDeviceId + "/"+ mVendorId;
	    Intent browse = new Intent(Intent.ACTION_VIEW , Uri.parse(url));
	    startActivity(browse);
	}

	public void setVendorId(String vendorId) {
		mVendorId = vendorId;
	}

	public void setDeviceId(String deviceId) {
		mDeviceId = deviceId;
	}
	
}