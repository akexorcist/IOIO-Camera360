package com.inex.ioiocamera360;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;


public class OrientationManager {
	public final static int PORTRAIT_NORMAL = 0;
	public final static int PORTRAIT_REVERSE = 1;
	public final static int LANDSCAPE_NORMAL = 2;
	public final static int LANDSCAPE_REVERSE = 3;
	
	Context context;
	Activity activity;
	int orientation = 0;
	String device_orientation = "";

	private OrientationEventListener mOrientationEventListener;
	private OrientationManagerListener mOrientationManagerListener;
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public OrientationManager(Activity activity) {
		this.activity = activity;
		this.context = activity.getApplicationContext();

        int xres = 0, yres = 0;
        Method mGetRawH;
        Display display = activity.getWindowManager().getDefaultDisplay(); 
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        	try {
    			mGetRawH = Display.class.getMethod("getRawHeight");
    	        Method mGetRawW = Display.class.getMethod("getRawWidth");
    	        xres = (Integer) mGetRawW.invoke(display);
    	        yres = (Integer) mGetRawH.invoke(display);
    		} catch (Exception e) {
    			xres = display.getWidth();
    			yres = display.getHeight();
    		}
    	} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    		DisplayMetrics outMetrics = new DisplayMetrics ();
    		display.getRealMetrics(outMetrics);
			xres = outMetrics.widthPixels;
			yres = outMetrics.heightPixels;
    	}
        
        int hdp = (int)(yres * (1f / dm.density));
        int wdp = (int)(xres * (1f / dm.density));
        int sw = (hdp < wdp) ? hdp : wdp;
        device_orientation = (sw >= 720) ? "landscape" : "portrait";
		
		mOrientationEventListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL){
			int orientation = -1;
			public void onOrientationChanged(int arg0) {
				this.orientation = arg0;
				if(orientation == -1) {
					orientation = getOrientation();
				} else {
					if(orientation != getOrientation()) {
						if((orientation == PORTRAIT_NORMAL && getOrientation() == PORTRAIT_REVERSE)
							|| (orientation == PORTRAIT_REVERSE && getOrientation() == PORTRAIT_NORMAL)
							|| (orientation == LANDSCAPE_NORMAL && getOrientation() == LANDSCAPE_REVERSE)
							|| (orientation == LANDSCAPE_REVERSE && getOrientation() == LANDSCAPE_NORMAL)) {
			    			mOrientationManagerListener.onMirrorRotatation(getOrientation());
		    				mOrientationManagerListener.onOrientationChanged(getOrientation(), true);
						} else {
		    				mOrientationManagerListener.onOrientationChanged(getOrientation(), false);
						}
		    			orientation = getOrientation();
					}
				}
        	}
        };
	}
	
	public void setOnOrientationListener (OrientationManagerListener listener) {
		mOrientationManagerListener = listener;
    }

	public void enable() {
		mOrientationEventListener.enable();
	}
	
	public int getAOrientation() {
		return this.orientation;
	}

	public void disable() {
		mOrientationEventListener.disable();
	}
	
	@SuppressWarnings("static-access")
	public int getOrientation() {		
		WindowManager wm = (WindowManager)context.getSystemService(context.WINDOW_SERVICE);
		int rotation = wm.getDefaultDisplay().getRotation();

        if(device_orientation.equals("portrait")) {
        	if(rotation == Surface.ROTATION_0) {
				return PORTRAIT_NORMAL;
			} else if(rotation == Surface.ROTATION_90) {
				return LANDSCAPE_NORMAL;
			} else if(rotation == Surface.ROTATION_180) {
				return PORTRAIT_REVERSE;
			} else if(rotation == Surface.ROTATION_270) {
				return LANDSCAPE_REVERSE;
			}
        } else if(device_orientation.equals("landscape")) {
        	if(rotation == Surface.ROTATION_0) {
				return LANDSCAPE_NORMAL;
			} else if(rotation == Surface.ROTATION_90) {
				return PORTRAIT_REVERSE;
			} else if(rotation == Surface.ROTATION_180) {
				return LANDSCAPE_REVERSE;
			} else if(rotation == Surface.ROTATION_270) {
				return PORTRAIT_NORMAL;
			}
        }
		return -1;
	}
	
	public void disableRotation() { 
    	disable();     
    	
        int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
        int SCREEN_ORIENTATION_REVERSE_PORTRAIT = 9;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO){
            SCREEN_ORIENTATION_REVERSE_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            SCREEN_ORIENTATION_REVERSE_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }

        int rotation = getOrientation();
        switch(rotation) {
        case OrientationManager.PORTRAIT_NORMAL:
        	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        	break;
        case OrientationManager.PORTRAIT_REVERSE:
        	activity.setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        	break;
        case OrientationManager.LANDSCAPE_NORMAL:
        	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        	break;
        case OrientationManager.LANDSCAPE_REVERSE:
        	activity.setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        	break;
        }
    }

    public void enableRotation() {
    	enable();
    	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
	
	public interface OrientationManagerListener{
	    public void onOrientationChanged(int orientation, boolean isMirror);
	    public void onMirrorRotatation(int orientation);
	}

}
