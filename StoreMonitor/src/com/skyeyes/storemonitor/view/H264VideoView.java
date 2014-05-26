package com.skyeyes.storemonitor.view;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.View;

import com.skyeyes.base.h264.H264DecoderException;
import com.skyeyes.base.h264.JavaH264Decoder;
import com.skyeyes.base.h264.JavaH264Decoder.DecodeSuccCallback;

public class H264VideoView extends View implements Runnable{
	private int videoViewStartX = 0;
	private int videoViewStartY = 0;
	private int videoViewEndX = 0;
	private int videoViewEndY = 0;
	private Bitmap videoBitmap;  
	private JavaH264Decoder decoder;
	private LinkedList<byte[]> dataBufferList = new LinkedList<byte[]>();
	
	private Context mContext;
	private Display mDisplay;
	private DecodeSuccCallback mDecodeSuccCallback;
	
	private Thread decodeThread;
	
	private boolean play = false;
	
	private Handler handler;
	
	
	
	public H264VideoView(Context context,Display display,DecodeSuccCallback decodeSuccCallback) {
		super(context);
		// TODO Auto-generated constructor stub
		mContext = context;
		mDisplay = display;
		mDecodeSuccCallback = decodeSuccCallback;
		handler = new Handler(Looper.getMainLooper());
	    try {
			decoder = new JavaH264Decoder(new DecodeSuccCallback(){
				@Override
				public void onDecodeSucc(final JavaH264Decoder decoder ,final Bitmap bitmap) {
					// TODO Auto-generated method stub
					Log.i("DecoderCallback", "onDecodeSucc================");
					if(videoBitmap == null){
						setVideoDisplay(bitmap.getWidth(),bitmap.getHeight());
					}
					videoBitmap = bitmap;
					handler.post(new Runnable(){
						@Override
						public void run() {
							// TODO Auto-generated method stub
							postInvalidate();
							if(mDecodeSuccCallback!=null){
								mDecodeSuccCallback.onDecodeSucc(decoder, bitmap);
							}
						}
					});
				}
				
			});
		} catch (H264DecoderException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	public void toStopPlay(){
		play = false;
		if(decoder!=null)
			decoder.toStop();
		if(decodeThread!=null){
			decodeThread.interrupt();
			dataBufferList.clear();
		}

	}
	
	public void toStartPlay(){
		play = true;
		if(decodeThread==null){
			decodeThread = new Thread(this);
			decodeThread.setDaemon(true);
			decodeThread.start();
		}
	}
	
	
	public void sendStream(byte[] videoData){
		if(!play){
			return;
		}
		dataBufferList.offer(videoData);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		byte[] tempData = null;
		while(play){
			if(dataBufferList.size()==0){
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			tempData = dataBufferList.poll();
			decoder.sendStream(tempData);
		}
	}
	
	
	
    private void setVideoDisplay(int bitmapWidth,int bitmapHeight){
		videoViewStartY = 0;
		videoViewEndY = mDisplay.getHeight();
		
		float zoom = (videoViewEndY*1.0f)/bitmapHeight;
		int tmepWidth =  (int)(zoom*bitmapWidth);
		if(mDisplay.getWidth() - tmepWidth>0){
			videoViewStartX = (mDisplay.getWidth() - tmepWidth) / 2;
			videoViewEndX = videoViewStartX + tmepWidth;
		}else{
			videoViewStartX = 0;
			videoViewEndX = mDisplay.getWidth();
			
    		zoom = (videoViewEndX*1.0f)/bitmapWidth;
    		int tmepHeight =  (int)zoom*bitmapHeight;
    		
    		videoViewStartY = (mDisplay.getHeight()-tmepHeight)/2;
    		videoViewEndY = videoViewStartY+tmepHeight;
		}
    }

	@Override 
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); 
        if(videoBitmap!=null){
            RectF rectF = new RectF(videoViewStartX, videoViewStartY, videoViewEndX, videoViewEndY); 
            //w和h分别是屏幕的宽和高，也就是你想让图片显示的宽和高  
          canvas.drawBitmap(videoBitmap, null, rectF, null);
        }
    }

	


}
