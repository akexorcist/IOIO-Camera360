package com.inex.ioiocamera360;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.RgbToYuv420j;

import com.inex.ioiocamera360.OrientationManager.OrientationManagerListener;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Main extends IOIOActivity implements Callback, PictureCallback {
	final int IMAGE_NUM = 36;
	boolean move = false;
	boolean saved_busy = false;
    int c = IMAGE_NUM;
	String imageNameLeading = "";
	File imagesFolder;

    Camera mCamera;
    SurfaceView mPreview;
    Button btnStart, btnViewer;
    TextView tvNumber, tvStatus;
    ToggleButton tbExposureLock;
    LinearLayout layoutPreview;
    
    OrientationManager om;
        
    @SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);        
        
        tvNumber = (TextView)findViewById(R.id.tvNumber);
        tvNumber.setVisibility(View.INVISIBLE);

        tvStatus = (TextView)findViewById(R.id.tvStatus);
        
        layoutPreview = (LinearLayout)findViewById(R.id.layoutPreview);
        
        mPreview = (SurfaceView)findViewById(R.id.preview);
        mPreview.getHolder().addCallback(this);
        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        mPreview.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if(btnStart.getText().toString().equals("Start")) {
					if (arg1.getAction() == MotionEvent.ACTION_UP) {
						Camera.Parameters params = mCamera.getParameters();
						boolean autoFocusSupported = params.getSupportedFocusModes().contains("auto");
						if(autoFocusSupported)
							mCamera.autoFocus(null);
					}
				}
				return true;
			}
		});
        
        btnStart = (Button)findViewById(R.id.btnStart);
        btnStart.setEnabled(false);
        btnStart.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(c >= IMAGE_NUM) {
					c = 0;
					btnStart.setText("Stop");
	                tbExposureLock.setEnabled(false);
			        disableRotation();
				} else {
					c = IMAGE_NUM;
					btnStart.setText("Start");
	                tvNumber.setVisibility(View.INVISIBLE);
	                tbExposureLock.setEnabled(true);
	                enableRotation();
				}
			}
		});
        
        btnViewer = (Button)findViewById(R.id.btnViewer);
        btnViewer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				File dir = new File(Environment.getExternalStorageDirectory()
	            		, "DCIM/IOIO360/");
				String[] names = dir.list();
				boolean ifVideoExist = false;
				for(int i = 0 ; i < names.length ; i++) {
					File f = new File(Environment.getExternalStorageDirectory()
		            		, "DCIM/IOIO360/" + names[i] + "/");
					if(f.isDirectory()) {
						String[] file = f.list();
						for(int j = 0 ; j < file.length ; j++) {
							if(file[j].endsWith(".mp4"))
								ifVideoExist = true;
						}
					}					
				}
							
				if(ifVideoExist) {
					final  ArrayList<File> f_gif = new ArrayList<File>();
					
					for(int i = 0 ; i < names.length ; i++) {
						File f = new File(Environment.getExternalStorageDirectory()
			            		, "DCIM/IOIO360/" + names[i] + "/");
						if(f.isDirectory()) {
							String[] file = f.list();
							for(int j = 0 ; j < file.length ; j++) {
								if(file[j].endsWith(".mp4") || file[j].endsWith(".MP4"))
									f_gif.add(new File(Environment.getExternalStorageDirectory()
											, "DCIM/IOIO360/" + names[i] + "/" + file[j]));
							}
						}
					}								
					
					final Dialog d = new Dialog(Main.this);
				    d.requestWindowFeature(Window.FEATURE_NO_TITLE);
				    d.setContentView(R.layout.dialog_view);
				    d.setCancelable(true);
	
				    Bitmap bm = null;
					ArrayList<Item> arr_grid = new ArrayList<Item>();
					
					for(int i = 0 ; i < f_gif.size() ; i++) {
						
						
						int imageSize = Main.this.getResources().getInteger(R.integer.image_preview_width);
						//bm = decodeSampledBitmapFromFile(f_gif.get(i), imageSize, imageSize);
						bm = getVideoThumbnail(f_gif.get(i));
						float bitmapRatio = (float)bm.getHeight() / (float)bm.getWidth();
						bm = Bitmap.createScaledBitmap(bm, imageSize, (int)(imageSize * bitmapRatio), false);
					
						
						boolean landscape = bm.getWidth() > bm.getHeight();
						float scale_factor;
						if (landscape) scale_factor = (float)imageSize / bm.getHeight();
						else scale_factor = (float)imageSize / bm.getWidth();
						Matrix matrix = new Matrix();
						matrix.postScale(scale_factor, scale_factor);
						
						if (landscape){
			                int start = (bm.getWidth() - bm.getHeight()) / 2;
			                bm = Bitmap.createBitmap(bm, start, 0, bm.getHeight(), bm.getHeight(), matrix, true);
			            } else {
			                int start = (bm.getHeight() - bm.getWidth()) / 2;
			                bm = Bitmap.createBitmap(bm, 0, start, bm.getWidth(), bm.getWidth(), matrix, true);
			            }
						
						arr_grid.add(new Item(bm, f_gif.get(i).getName().replace(".mp4", "")));
					}
					
					bm = null;
					
					CustomGridViewAdapter adapter = new CustomGridViewAdapter(Main.this
							, R.layout.custom_gridview, arr_grid);
	
				    GridView gv = (GridView)d.findViewById(R.id.gridView1);
				    gv.setAdapter(adapter);
				    gv.setOnItemClickListener(new OnItemClickListener() {
						public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
							Intent intent = new Intent();
							intent.setAction(android.content.Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.fromFile(f_gif.get(arg2)), "video/mp4");
							Main.this.startActivity(intent);
							
							d.cancel();
						}
					});
				    
				    d.show();
				} else {
					Toast.makeText(Main.this, "Video not found", Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        tbExposureLock = (ToggleButton)findViewById(R.id.tbExposureLock);
		tbExposureLock.setVisibility(View.INVISIBLE);
		tbExposureLock.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
				if(mCamera != null) {
					Camera.Parameters params = mCamera.getParameters();
					if(params.isAutoWhiteBalanceLockSupported())
						params.setAutoExposureLock(isChecked);
					if(params.isAutoWhiteBalanceLockSupported())
				        params.setAutoWhiteBalanceLock(isChecked);
					mCamera.setParameters(params);
				}
			}
		});
		
		om = new OrientationManager(this);
		om.setOnOrientationListener(new OrientationManagerListener() {
			public void onOrientationChanged(int orientation, boolean isMirror) { 
				tbExposureLock.setChecked(false);
			}
			
			public void onMirrorRotatation(int orientation) {
				Display display = Main.this.getWindowManager().getDefaultDisplay();
				int width = mPreview.getWidth();
				int height = mPreview.getHeight();
				Main.this.surfaceChanged(mPreview.getHolder(), display.getPixelFormat(), width, height);
			}
		});
    }
    
    public void onResume() {
    	super.onResume();
    	om.enable();
    }
    
    public void onStop() {
    	super.onStop();
    	om.disable();
    }
    
    public Bitmap getVideoThumbnail(File file) {
        return ThumbnailUtils.createVideoThumbnail(file.getPath(), Thumbnails.MINI_KIND);
    }
    
    public void captureSound() {
        AudioManager meng = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        
        if (volume != 0) {
            MediaPlayer _shootMP = MediaPlayer.create(Main.this
            		, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (_shootMP != null)
                _shootMP.start();
        }
    }
    
    @SuppressWarnings("deprecation")
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
    	if (mCamera == null) { return; }
		
		Camera.Parameters params = mCamera.getParameters();
        Camera.Size pictureSize = getMaxPictureSize(params);
        Camera.Size previewSize = getBestPreviewSize(params);
        
        params.setPictureSize(pictureSize.width, pictureSize.height);		
        params.setPreviewSize(previewSize.width, previewSize.height);
        params.setPreviewFrameRate(getMaxPreviewFps(params));    
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	        if(params.isAutoExposureLockSupported() || params.isAutoWhiteBalanceLockSupported())
	        	tbExposureLock.setVisibility(View.VISIBLE);
        }
        
        LayoutParams lp = layoutPreview.getLayoutParams();
        Display display = getWindowManager().getDefaultDisplay(); 
        
        float ratio = 0;
        if(om.getOrientation() == OrientationManager.LANDSCAPE_NORMAL) {
        	ratio = (float)previewSize.width / (float)previewSize.height;
            params.set("orientation", "landscape");
            params.set("rotation", 0);
        } else if(om.getOrientation() == OrientationManager.LANDSCAPE_REVERSE) {
        	ratio = (float)previewSize.width / (float)previewSize.height;
            params.set("orientation", "landscape");
            params.set("rotation", 180);
        } else if(om.getOrientation() == OrientationManager.PORTRAIT_NORMAL) {
        	ratio = (float)previewSize.height / (float)previewSize.width;
            params.set("orientation", "landscape");
            params.set("rotation", 90);
        } else if(om.getOrientation() == OrientationManager.PORTRAIT_REVERSE) {
        	ratio = (float)previewSize.height / (float)previewSize.width;
            params.set("orientation", "portrait");
            params.set("rotation", 270);
        }
        
        if(params.getSupportedAntibanding().contains(Camera.Parameters.ANTIBANDING_AUTO)) {
        	params.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
        }
        
        //params.setExposureCompensation(2);

        //params.setPreviewFpsRange(15000, 20520);
        
        if((int)((float)mPreview.getWidth() / ratio) < display.getHeight()) {
			lp.height = (int)((float)mPreview.getWidth() / ratio);
			lp.width = mPreview.getWidth();
		} else {
			lp.height = mPreview.getHeight();
			lp.width = (int)((float)mPreview.getHeight() * ratio);
		}
        
        /*
        Camera.CameraInfo info =
        		new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        int orientation = (om.getAOrientation() + 45) / 90 * 90;
        int rotation = 0;
        rotation = (info.orientation + orientation) % 360;
        Log.i("Check","" + rotation);
         
         if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
             rotation = (info.orientation - orientation + 360) % 360;
         } else {  // back-facing camera
             rotation = (info.orientation + orientation) % 360;
         }*/
         //mParameters.setRotation(rotation);
        

        layoutPreview.setLayoutParams(lp);        
		mCamera.setParameters(params);
		
		switch(om.getOrientation()) {
		case OrientationManager.LANDSCAPE_NORMAL:
			mCamera.setDisplayOrientation(0);
			break;
		case OrientationManager.PORTRAIT_NORMAL:
			mCamera.setDisplayOrientation(90);
			break;
		case OrientationManager.LANDSCAPE_REVERSE:
			mCamera.setDisplayOrientation(180);
			break;
		case OrientationManager.PORTRAIT_REVERSE:
			mCamera.setDisplayOrientation(270);
			break;		
		}
		
		try {
			mCamera.setPreviewDisplay(mPreview.getHolder());
			mCamera.startPreview();
		} catch (Exception e){
            e.printStackTrace();
		}
	}

    public void surfaceCreated(SurfaceHolder arg0) { 
		if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
		
		try {
            mCamera = Camera.open(0);
        } catch (Exception e) {
        	e.printStackTrace();
        }
		
		if(mCamera == null) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setMessage("Can't connet to camera");
	        builder.setNeutralButton("OK", null);
	        builder.show();
		}
	}

	public void surfaceDestroyed(SurfaceHolder arg0) { 
		if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
	}
	
    public void snap() {
    	captureSound();
		mCamera.takePicture(null, null, null, this);
    }

	public void onPictureTaken(final byte[] data, Camera camera) {
		if(c == 0) {
            tvNumber.setVisibility(View.VISIBLE);
            SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd-HHmmss");
            String date = sd.format(new Date());
        	
        	imageNameLeading = "IMG_" + date;
            imagesFolder = new File(Environment.getExternalStorageDirectory()
            		, "DCIM/IOIO360/" + date);
            imagesFolder.mkdirs();
        }
		
		String imageNum = String.valueOf(c + 1);
        tvNumber.setText(imageNum);
        if(btnStart.getText().toString().equals("Stop"))
        	tvNumber.startAnimation(AnimationUtils.loadAnimation(this, R.anim.anim_snap_count));
        
        imageNum = ("000" + imageNum).substring(imageNum.length());
        
        String fileName = imageNameLeading + "_" + imageNum + ".jpg";
        final File output = new File(imagesFolder, fileName);
		
        new Thread(new Runnable() {
        	public void run() {
        		try {
                    FileOutputStream fos = new FileOutputStream(output);
                    fos.write(data);
                    fos.flush();
                    fos.close();
                    
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	                scanIntent.setData(Uri.fromFile(output));
	                sendBroadcast(scanIntent);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                	e.printStackTrace();
                }
        	}
        }).start();
		
		
        mCamera.stopPreview();
        mCamera.startPreview();
        saved_busy = false;
        
        if(c + 1 == IMAGE_NUM) {
	        new Handler().postDelayed(new Runnable() {
	        	public void run() {
        		    System.gc();
        		    
	                tvNumber.setVisibility(View.INVISIBLE);
	                Toast.makeText(Main.this, "Finish!!", Toast.LENGTH_SHORT).show();

        		    System.gc();
	                final Dialog d = new Dialog(Main.this);
				    d.requestWindowFeature(Window.FEATURE_NO_TITLE);
				    d.setContentView(R.layout.dialog_export);
				    d.setCancelable(true);
	                
				    TextView tvMessage = (TextView)d.findViewById(R.id.tvMessage);
				    tvMessage.setText("Export to video?\n(Take a few minutes)");

				    Button btnYes = (Button)d.findViewById(R.id.btnYes);
				    btnYes.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							d.cancel();
							
			                final Dialog d1 = new Dialog(Main.this);
						    d1.requestWindowFeature(Window.FEATURE_NO_TITLE);
						    d1.setContentView(R.layout.dialog_progress);
						    d1.setCancelable(false);
						    
						    final TextView tvProgress = (TextView)d1.findViewById(R.id.tvProgress);
						    tvProgress.setText("0%");
					        
							new Thread(new Runnable () {
			                	public void run() {
			                		try{
			                		    int fps = 15;
			                		    RgbToYuv420j transform = new RgbToYuv420j();
				    	                final String outFileName = imagesFolder.getPath() + "/" + imageNameLeading + ".mp4";
				    	                
			                		    FileChannelWrapper ch = NIOUtils.writableFileChannel(new File(outFileName));
			                		    final MP4Muxer muxer = new MP4Muxer(ch, Brand.MP4);

			                		    FramesMP4MuxerTrack outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, (int)fps);

			                		    H264Encoder encoder = new H264Encoder(); // not we could use a rate control in the constructor

			                		    ByteBuffer _out = ByteBuffer.allocate(720 * 720 * 6); //Not sur about RGB

			                		    ArrayList<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
			                		    ArrayList<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();

			                		    int num = 0;
			                		    Picture rgb = null;
			                		    Picture yuv = null;

			                		    int[] packed = null;
			                		    for (int i = 0; i < IMAGE_NUM ; i++) {
			                		    	final int k = i;
			                		        Bitmap bitmap = null;
			                		        try {
			                		        	String path = imagesFolder.getPath();
					    		                String imageNum = String.valueOf(i + 1);
					    		                imageNum = ("000" + imageNum).substring(imageNum.length());
					    		                String fileName = path + "/" + imageNameLeading + "_" + imageNum + ".jpg";
					    		                File f = new File(fileName);
						    	                bitmap = decodeSampledBitmapFromFile(f, 1280, 1280);
						    	                bitmap = resizeBitmap(bitmap, 1280, 1280);
			                		        } catch (Exception e) {
			                		            e.printStackTrace();
			                		        }    
			                		        
			                		        if(rgb == null) {
			                		            rgb = Picture.create((int)bitmap.getWidth(), (int)bitmap.getHeight(), ColorSpace.RGB);
			                		        }               

			                		        int[] dstData = rgb.getPlaneData(0);
			                		        if(packed==null)
			                		            packed = new int[bitmap.getWidth() * bitmap.getHeight()];

			                		        bitmap.getPixels(packed, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

			                		        for (int iu = 0, srcOff = 0, dstOff = 0; iu < bitmap.getHeight(); iu++) {
			                		            for (int j = 0; j < bitmap.getWidth(); j++, srcOff++, dstOff += 3) {
			                		                int rgbo = packed[srcOff];
			                		                dstData[dstOff] = (rgbo >> 16) & 0xff;
			                		                dstData[dstOff + 1] = (rgbo >> 8) & 0xff;
			                		                dstData[dstOff + 2] = rgbo & 0xff;
			                		            }
			                		        }               
			                		        bitmap.recycle();


			                		        if(yuv == null)
			                		            yuv = Picture.create(rgb.getWidth(), rgb.getHeight(), ColorSpace.YUV420);
			                		        transform.transform(rgb, yuv);
			                		        ByteBuffer result = encoder.encodeFrame(yuv, _out); 
			                		        _out.clear();
			                		        spsList.clear();
			                		        ppsList.clear();

			                		        H264Utils.encodeMOVPacket(result, spsList, ppsList);
			                		        outTrack.addFrame(new MP4Packet(result, num, (int)fps, 1, num, true, null, num, 0));
			                		        result = null;
			                		        System.gc();
			                		        num++;
			                		        
					    					runOnUiThread(new Runnable() {
					    						public void run() {
					    							tvProgress.setText(String.valueOf((int)((float)(k + 1) * 100 / IMAGE_NUM)) + "%");
					    						}
					    					});
			                		    }
			                		    
			                		    yuv = null;
			                		    packed = null;
			                		    System.gc();

			                		    
			                		    outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList));
			                		    muxer.writeHeader();
			                		    NIOUtils.closeQuietly(ch);

				    					runOnUiThread(new Runnable() {
				    						public void run() {
				    							d1.cancel();
				    			                enableRotation();
				    			                tbExposureLock.setEnabled(true);
				        		                Toast.makeText(Main.this, "Saved to " + outFileName, Toast.LENGTH_SHORT).show();
				        		                
				        		                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				        		                scanIntent.setData(Uri.fromFile(new File(outFileName)));
				        		                sendBroadcast(scanIntent);
				    						}
				    					});
			                		} catch (Exception e) {
			                		    e.printStackTrace();
			                		}
			                	}
			                }).start();
						    d1.show();
						}
					});
				    
				    Button btnNo = (Button)d.findViewById(R.id.btnNo);
				    btnNo.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							d.cancel();
			                enableRotation();
			                tbExposureLock.setEnabled(true);
						}
					});
				    
				    d.show();
					btnStart.setText("Start");
	        	}
	        }, 500);
        }
	}
	
	public Bitmap resizeBitmap(Bitmap bm, int maxWidth, int maxHeight) {
		int newWidth, newHeight;
		int bmWidth = bm.getWidth();
		int bmHeight = bm.getHeight();
		if(bmWidth > maxWidth && (maxWidth * bmHeight) / bmWidth < maxHeight) {
			newWidth = maxWidth;
			newHeight = (maxWidth * bmHeight) / bmWidth;
		} else {
			newWidth = (maxHeight * bmWidth) / bmHeight;
			newHeight = maxHeight;
		}
		
		return Bitmap.createScaledBitmap(bm, newWidth, newHeight, false);
	}

	@SuppressWarnings("deprecation")
	public Camera.Size getBestPreviewSize(Camera.Parameters params) {
    	List<Camera.Size> previewSize = params.getSupportedPreviewSizes();
        Display display = getWindowManager().getDefaultDisplay();  
        int screenWidth = display.getWidth();
        int c = 0, nearest = screenWidth;

        if(om.getOrientation() == OrientationManager.LANDSCAPE_NORMAL
        		|| om.getOrientation() == OrientationManager.LANDSCAPE_REVERSE) {
	        for(int i = 0 ; i < previewSize.size() ; i++) {
	        	if(Math.abs(Math.abs(screenWidth - previewSize.get(i).width)) < nearest) {
	        		c = i;
	        		nearest = Math.abs(screenWidth - previewSize.get(i).width);
	        	}
	        }
        } else if(om.getOrientation() == OrientationManager.PORTRAIT_NORMAL
        		|| om.getOrientation() == OrientationManager.PORTRAIT_REVERSE) {
	        for(int i = 0 ; i < previewSize.size() ; i++) {
	        	if(Math.abs(Math.abs(screenWidth - previewSize.get(i).height)) < nearest) {
	        		c = i;
	        		nearest = Math.abs(screenWidth - previewSize.get(i).height);
	        	}
	        }
        }
    	return previewSize.get(c);
    }
    
    public Camera.Size getMaxPictureSize(Camera.Parameters params) {
    	List<Camera.Size> pictureSize = params.getSupportedPictureSizes();    	
    	int firstPictureWidth, lastPictureWidth;
    	try {
	    	firstPictureWidth = pictureSize.get(0).width;
	    	lastPictureWidth = pictureSize.get(pictureSize.size() - 1).width;
	    	if(firstPictureWidth > lastPictureWidth) 
	    		return pictureSize.get(0);
	    	else 
	    		return pictureSize.get(pictureSize.size() - 1);
    	} catch (ArrayIndexOutOfBoundsException e) {
    		e.printStackTrace();
    		return pictureSize.get(0);
    	}
    }
    
    @SuppressWarnings("deprecation")
	public int getMaxPreviewFps(Camera.Parameters params) {
    	List<Integer> previewFps = params.getSupportedPreviewFrameRates();
    	int fps = 0;
    	for(int i = 0 ; i < previewFps.size() ; i++) {
    		if(previewFps.get(i) > fps) 
    			fps = previewFps.get(i);
    	}
    	return fps;
    }
    
    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }    
    
    public Bitmap decodeSampledBitmapFromFile(File file, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getPath(), options);
    }
    
    public void disableRotation() {       
        int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
        int SCREEN_ORIENTATION_REVERSE_PORTRAIT = 9;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO){
            SCREEN_ORIENTATION_REVERSE_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            SCREEN_ORIENTATION_REVERSE_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }

        int rotation = om.getOrientation();
        switch(rotation) {
        case OrientationManager.PORTRAIT_NORMAL:
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        	break;
        case OrientationManager.PORTRAIT_REVERSE:
            setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        	break;
        case OrientationManager.LANDSCAPE_NORMAL:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        	break;
        case OrientationManager.LANDSCAPE_REVERSE:
            setRequestedOrientation(SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        	break;
        }
    }

    public void enableRotation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

	public int dpToPx(int dp) {
	    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
	    int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));       
	    return px;
	}
    
    class Looper extends BaseIOIOLooper {
		DigitalInput in;
    	DigitalOutput d1;
    	DigitalOutput d2;                                                                                                                                                                                                                                                                                                                                                                                                         
    	PwmOutput en;
    	public void setup() throws ConnectionLostException, InterruptedException {
    		ioio_.openDigitalOutput(0, false);
        	in = ioio_.openDigitalInput(36);
        	d1 = ioio_.openDigitalOutput(1, true);
        	d2 = ioio_.openDigitalOutput(2, false);
        	en = ioio_.openPwmOutput(3, 100);
        	en.setDutyCycle(0);
            runOnUiThread(new Runnable() {
                public void run() {
                    btnStart.setEnabled(true);
	                tvStatus.setText("Connected");
                }        
            });
        }

        public void loop() throws ConnectionLostException, InterruptedException { 
        	if(c >= IMAGE_NUM) {
            	d1.write(false);
            	d2.write(false);
        		while(c == IMAGE_NUM) 
        			Thread.sleep(50);
            	d1.write(true);
            	d2.write(false);
        	}
        	
        	if(c < IMAGE_NUM) {
	        	Thread.sleep(500);
	        	en.setDutyCycle(0.1f);
	        	while(!in.read()) Thread.sleep(5);
	        	while(in.read()) Thread.sleep(5);
	        	en.setDutyCycle(0);
	        	saved_busy = true;
	        	Thread.sleep(500);
	        	snap();
	        	while(saved_busy);
	        	c++;
        	}
        }

		public void disconnected() {
			runOnUiThread(new Runnable() {
	            public void run() {
                    btnStart.setEnabled(false);
					btnStart.setText("Start");
	                tvNumber.setVisibility(View.INVISIBLE);
	                tvStatus.setText("Waiting for IOIO");
					c = IMAGE_NUM;
	            }        
			});
		}

		public void incompatible() {
			runOnUiThread(new Runnable() {
	            public void run() {
	                tvStatus.setText("Incompatible firmware version");
	            }        
	        });
		}
    }

    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }
}
