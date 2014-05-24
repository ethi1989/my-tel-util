package com.skyeyes.storemonitor.activity;

import h264.com.H264PicView;
import h264.com.H264PicView.DecodeSuccCallback;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skyeyes.base.BaseSocketHandler;
import com.skyeyes.base.activity.BaseActivity;
import com.skyeyes.base.cmd.CommandControl.REQUST;
import com.skyeyes.base.cmd.bean.ReceiveCmdBean;
import com.skyeyes.base.cmd.bean.impl.ReceivLogin;
import com.skyeyes.base.cmd.bean.impl.ReceiveChannelPic;
import com.skyeyes.base.cmd.bean.impl.ReceiveCountManu;
import com.skyeyes.base.cmd.bean.impl.ReceiveDeviceRegisterInfo;
import com.skyeyes.base.cmd.bean.impl.ReceiveReadDeviceList;
import com.skyeyes.base.cmd.bean.impl.ReceiveRealVideo;
import com.skyeyes.base.cmd.bean.impl.ReceiveVideoData;
import com.skyeyes.base.cmd.bean.impl.SendObjectParams;
import com.skyeyes.base.exception.CommandParseException;
import com.skyeyes.base.exception.NetworkException;
import com.skyeyes.base.network.impl.SkyeyeSocketClient;
import com.skyeyes.base.util.DateUtil;
import com.skyeyes.base.util.PreferenceUtil;
import com.skyeyes.base.util.StringUtil;
import com.skyeyes.base.view.TopTitleView;
import com.skyeyes.base.view.TopTitleView.OnClickListenerCallback;
import com.skyeyes.storemonitor.R;
import com.skyeyes.storemonitor.activity.adapter.ChennalPicViewAdapter;
import com.skyeyes.storemonitor.activity.bean.ChennalPicBean;
import com.skyeyes.storemonitor.process.DeviceProcessInterface.DeviceReceiveCmdProcess;
import com.skyeyes.storemonitor.process.impl.CountManuCmdProcess;
import com.skyeyes.storemonitor.service.DevicesService;

public class MainPageActivity extends BaseActivity{
	String TAG = "MainPageActivity";
	public final static int SEND_QUERY_MANU_ID = 1;
	private boolean stopQueryManu = true;
	
	//TextView store_login_id_tv = null;
	Gallery gallery = null;
	private TopTitleView topTitleView;
	private LinearLayout vp_real_time_ll;
	private LinearLayout vp_history_ll;
	private TextView count_all_manu_tv;
	private TextView count_avg_time_tv;
	
	List<ChennalPicBean> chennalPicBeanlist=new ArrayList<ChennalPicBean>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		this.setContentView(R.layout.app_video_page);
		//store_login_id_tv = (TextView)findViewById(R.id.store_login_id_tv);
		vp_real_time_ll = (LinearLayout)findViewById(R.id.vp_real_time_ll);
		vp_history_ll = (LinearLayout)findViewById(R.id.vp_history_ll);
		topTitleView = (TopTitleView)findViewById(R.id.vp_topView);
		
		count_all_manu_tv = (TextView)findViewById(R.id.count_all_manu_tv);
		count_avg_time_tv = (TextView)findViewById(R.id.count_avg_time_tv);
		
		vp_history_ll.setVisibility(View.GONE);
		vp_real_time_ll.setVisibility(View.VISIBLE);

		topTitleView.setOnRightButtonClickListener(new OnClickListenerCallback() {
			
			@Override
			public void onClick() {
				// TODO Auto-generated method stub
				vp_real_time_ll.setVisibility(View.GONE);
				vp_history_ll.setVisibility(View.VISIBLE);
			}
		});
		topTitleView.setOnLeftButtonClickListener(new OnClickListenerCallback() {
			
			@Override
			public void onClick() {
				// TODO Auto-generated method stub
				vp_history_ll.setVisibility(View.GONE);
				vp_real_time_ll.setVisibility(View.VISIBLE);

			}
		});
		
		topTitleView.setOnMenuButtonClickListener(new OnClickListenerCallback() {
			
			@Override
			public void onClick() {
				// TODO Auto-generated method stub
				try{
					HomeActivity.getInstance().toggleMenu();
				}catch(Exception e){
					e.printStackTrace();
				}
				

			}
		});
		
		gallery = (Gallery) findViewById(R.id.chennal_pic_gallery);
		
    	if(DevicesService.getInstance() == null){
    		Log.i(TAG,"onResume()");
    		String userName = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_name);
    		
    		String userPsd = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_psd);
    		
    		String ip = PreferenceUtil.getConfigString(PreferenceUtil.SYSCONFIG, PreferenceUtil.sysconfig_server_ip);
    		
    		String port = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_psd);
    		
    		if(StringUtil.isNull(userName)||StringUtil.isNull(userPsd)||StringUtil.isNull(ip)||StringUtil.isNull(port)){
    			showToast("用户数据不完整，请前往设置用户数据...............");
    		}else{
        		SkyeyeSocketClient skyeyeSocketClient = null;
        		try {
        			skyeyeSocketClient = new SkyeyeSocketClient(new SocketHandlerImpl(), true);
        		} catch (NetworkException e1) {
        			// TODO Auto-generated catch block
        			e1.printStackTrace();
        		}
        		queryEquitListNoLogin(skyeyeSocketClient);//查询设备
        		
        		showToast("正在登陆...............");
    		}
    	}


	}
    
    
    public void onResume(){
    	super.onResume();
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	stopQueryManu = true;
    }
    
	// 查询设备列表
	public void queryEquitListNoLogin(
			SkyeyeSocketClient skyeyeSocketClient) {
		SendObjectParams sendObjectParams = new SendObjectParams();
		
		String userName = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_name);
		
		String userPsd = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_psd);

		Object[] params = new Object[] { userName, userPsd };
		//params = new Object[]{};
		try {

			sendObjectParams.setParams(REQUST.cmdUserEquitListNOLogin, params);

			Log.i(TAG,"testEquitListNoLogin入参数："+ sendObjectParams.toString());
		} catch (CommandParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			skyeyeSocketClient.sendCmd(sendObjectParams);
		} catch (NetworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
    
    
	private class SocketHandlerImpl extends BaseSocketHandler {
		
		public SocketHandlerImpl(){
			super();
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onReceiveCmdEx(final ReceiveCmdBean receiveCmdBean) {
			// TODO Auto-generated method stub
			Log.i("MainPageActivity","解析报文成功:" + (receiveCmdBean!=null?receiveCmdBean.toString():"receiveCmdBean is null"));
			if (receiveCmdBean instanceof ReceiveReadDeviceList) {
				if(((ReceiveReadDeviceList) receiveCmdBean).getCommandHeader().cmdCode == 0){
					final ReceiveReadDeviceList receiveReadDeviceList = ((ReceiveReadDeviceList) receiveCmdBean);
					PreferenceUtil.setSingleConfigInfo(PreferenceUtil.DEVICE_INFO,
							PreferenceUtil.device_count, receiveReadDeviceList.deviceCodeList.size());
					PreferenceUtil.setSingleConfigInfo(PreferenceUtil.DEVICE_INFO,
							PreferenceUtil.device_code_list,  receiveReadDeviceList.deviceListString);
					showToast(receiveReadDeviceList.deviceListString);	
					
					MainPageActivity.this.startService(new Intent(MainPageActivity.this,DevicesService.class));
					final LoginReceive loginReceive = new LoginReceive();
					final DeviceRegisterInfoReceive deviceRegisterInfoReceive = new DeviceRegisterInfoReceive();

					new Thread(){
						public void run(){
							while(true){
								if(DevicesService.getInstance() != null){
									if(DevicesService.getInstance().getCurrentDeviceCode() == null){
										DevicesService.getInstance().selectDevice(receiveReadDeviceList.deviceCodeList.get(0));
										DevicesService.getInstance().registerCmdProcess(ReceivLogin.class.getSimpleName(), loginReceive);
										DevicesService.getInstance().registerCmdProcess(ReceiveDeviceRegisterInfo.class.getSimpleName(),
												deviceRegisterInfoReceive);
									}
									break;
								}
								try {
									sleep(10);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}.start();
					
					Log.i("MainPageActivity", "DevicesService.getInstance()==null:"+(DevicesService.getInstance()==null));
					

//					for(final String deviceCode:((ReceiveReadDeviceList) receiveCmdBean).deviceCodeList){
//						Button button = new Button(MainPageActivity.this);
//						button.setText(deviceCode);
//						LinearLayout.LayoutParams lp = 
//								new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
//										LinearLayout.LayoutParams.WRAP_CONTENT); 
//						((LinearLayout)MainPageActivity.this.findViewById(R.id.layout_root)).addView(button,lp);
//						button.setOnClickListener(new OnClickListener(){
//							@Override
//							public void onClick(View arg0) {
//								// TODO Auto-generated method stub
//								DevicesService.getInstance().selectDevice(deviceCode);
//								Intent intent = new Intent(MainPageActivity.this,RealTimeVideoActivity.class);
//								MainPageActivity.this.startActivity(intent);
//							}
//						});
//					}
					
				}
			}else{
				Toast.makeText(MainPageActivity.this, "查询失败："+((ReceiveReadDeviceList) receiveCmdBean).getCommandHeader().errorInfo, Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void onCmdExceptionEx(CommandParseException ex) {
			// TODO Auto-generated method stub
			Toast.makeText(MainPageActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onSocketExceptionEx(NetworkException ex) {
			// TODO Auto-generated method stub
			Toast.makeText(MainPageActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();

		}

		@Override
		public void onSocketClosedEx() {
			// TODO Auto-generated method stub
			//System.out.println("测试连接失败:onFailure");
			//Toast.makeText(MainPageActivity.this, "测试连接失败:onFailure", Toast.LENGTH_SHORT).show();

		}

	};
	
	private class LoginReceive extends DeviceReceiveCmdProcess<ReceivLogin>{

		@Override
		public void onProcess(ReceivLogin receiveCmdBean) {
			// TODO Auto-generated method stub
			if(receiveCmdBean.getCommandHeader().resultCode != 0){
				showToast(receiveCmdBean.getCommandHeader().errorInfo);

			}else{
				showToast("登陆成功...............");
				
				stopQueryManu = false;
				queryManuCountHandler.sendEmptyMessage(SEND_QUERY_MANU_ID);//统计人流
			}
				
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	int chennalCount = 0;
	int getPicCount = 0;
	private class DeviceRegisterInfoReceive extends DeviceReceiveCmdProcess<ReceiveDeviceRegisterInfo>{

		@Override
		public void onProcess(ReceiveDeviceRegisterInfo receiveCmdBean) {
			// TODO Auto-generated method stub
			Log.i(TAG, receiveCmdBean.toString());
			showToast("通道状态："+receiveCmdBean.toString());
			chennalCount = receiveCmdBean.videoChannelCount;
//			if(chennalCount>0){
//				//查询通道图片
//				SendObjectParams sendObjectParams = new SendObjectParams();
//				Object[] params = new Object[] {(byte)0x00};
//				try {
//					sendObjectParams.setParams(REQUST.cmdReqVideoChannelPic, params);
//					System.out.println("getChannelPic入参数：" + sendObjectParams.toString());
//					
//					DevicesService.sendCmd(sendObjectParams, new ChannelPicReceive());
//				} catch (CommandParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}

		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
		
	}
	

	
	private Bitmap pic;
//	private HD264Decoder mHD264Decoder = new H264PicView(512,213,new DecoderCallback());
    public class DecoderCallback implements DecodeSuccCallback{
		@Override
		public void onDecodeSucc(Bitmap bitmap) {
			// TODO Auto-generated method stub
			Log.i("DecoderCallback", "onDecodeSucc================");
			pic = bitmap;

	        
		}
    	
    }
	
	private class ChannelPicReceive extends DeviceReceiveCmdProcess<ReceiveChannelPic>{

		@Override
		public void onProcess(ReceiveChannelPic receiveCmdBean) {
			// TODO Auto-generated method stub
			Log.i("MainPageActivity", "ChannelPicReceive================");
			showToast("解码图片开始");
	        WindowManager windowManager = getWindowManager();
	        Display display = windowManager.getDefaultDisplay();
		    H264PicView h264PicView = new H264PicView(new DecoderCallback());
		    h264PicView.sendStream(receiveCmdBean.pic);
			
			if(pic!=null){
				ImageView iv = new ImageView(MainPageActivity.this);
				iv.setBackgroundDrawable(new BitmapDrawable(pic));
				iv.setOnClickListener(new OnClickListener(){
					byte chennalId = (byte)(getPicCount);
					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub
						Log.i("MainPageActivity", "iv.setOnClickListener(new OnClickListener()================");
						VideoDataReceive video = new VideoDataReceive();
						DevicesService.getInstance().registerCmdProcess(ReceiveVideoData.class.getSimpleName(), video);
						
						
						SendObjectParams sendObjectParams = new SendObjectParams();
						Object[] params = new Object[] { chennalId };
						try {
							sendObjectParams.setParams(REQUST.cmdReqRealVideo, params);
							System.out.println("cmdReqRealVideo入参数：" + sendObjectParams.toString());
							
							DevicesService.sendCmd(sendObjectParams, new RealVideoReceive());
						} catch (CommandParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				});
				
				float zoom = 1.0f*display.getWidth()/pic.getWidth();
				int imgHeight = (int)(pic.getHeight()*zoom);
				
				LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
						display.getWidth(),imgHeight);
				iv.setLayoutParams(ivLp);
	        	ChennalPicBean picBean=new ChennalPicBean();
	        	picBean.des = "";
	        	picBean.img = iv;
	            chennalPicBeanlist.add(picBean);
			}
			Log.i("MainPageActivity", "getPicCount================"+getPicCount);
			
			getPicCount++;
			if(getPicCount<chennalCount){
				//查询通道图片
				SendObjectParams sendObjectParams = new SendObjectParams();
				Object[] params = new Object[] {(byte)getPicCount};
				try {
					sendObjectParams.setParams(REQUST.cmdReqVideoChannelPic, params);
					System.out.println("getChannelPic入参数：" + sendObjectParams.toString());
					
					DevicesService.sendCmd(sendObjectParams, new ChannelPicReceive());
				} catch (CommandParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(getPicCount==chennalCount &&
					chennalPicBeanlist.size()>0){
				Log.i("MainPageActivity", "chennalPicBeanlist================"+chennalPicBeanlist.size());
				ChennalPicViewAdapter pageAdapter=new ChennalPicViewAdapter(MainPageActivity.this,chennalPicBeanlist);
				gallery.setAdapter(pageAdapter);
			}
			
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	private class RealVideoReceive extends DeviceReceiveCmdProcess<ReceiveRealVideo>{

		@Override
		public void onProcess(ReceiveRealVideo receiveCmdBean) {
			// TODO Auto-generated method stub
			//打开视频播放界面
			Intent it = new Intent(MainPageActivity.this,VideoPlayActivity.class);
			MainPageActivity.this.startActivity(it);
			Log.i("MainPageActivity", "MainPageActivity.this.startActivity(it)================");
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class VideoDataReceive extends DeviceReceiveCmdProcess<ReceiveVideoData>{
		long lastDataTime = 0;
		@Override
		public void onProcess(ReceiveVideoData receiveCmdBean) {
			Log.e("MainPageActivity", "VideoDataReceive================");
			// TODO Auto-generated method stub
			if(VideoPlayActivity.getInstance()!=null)
				VideoPlayActivity.getInstance().sendStream(receiveCmdBean.data);
			
			responseVideoData(receiveCmdBean);
			
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
		
		// 回复视频数据
		public void responseVideoData(ReceiveCmdBean receiveCmdBean) {
			if(lastDataTime == 0)
				lastDataTime = System.currentTimeMillis();
			if(System.currentTimeMillis()-lastDataTime>700){
				SendObjectParams sendObjectParams = new SendObjectParams();
				sendObjectParams.setCommandHeader(receiveCmdBean.getCommandHeader());
				Object[] params = new Object[] {};
				try {
					sendObjectParams.setParams(REQUST.cmdRevFrame, params);
					sendObjectParams.getCommandHeader().cmdCode = 0 ;
					System.out.println("testResponseVideoData入参数："+ sendObjectParams.toString());
					
					DevicesService.sendCmd(sendObjectParams,null);
				} catch (CommandParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				lastDataTime = System.currentTimeMillis();
			}
			
		}
	}
	
	private void showToast(String msg){
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
	
	

	Handler queryManuCountHandler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
				case SEND_QUERY_MANU_ID:
					if(!stopQueryManu)
						getManucountByMonth();
					break;
			}
		}
	};
	
	/**
	 * 按月统计人流
	 * @param dayTime 如：2014-05-01 00:00:00
	 */
	private void getManucountByMonth() {
		SendObjectParams sendObjectParams = new SendObjectParams();
		
		Object[] params = new Object[] {DateUtil.getDefaultTimeStringFormat(DateUtil.TIME_FORMAT_YM)+"-01 00:00:00"};
		try {
			sendObjectParams.setParams(REQUST.cmdReqAllManuByMouse, params);
			System.out.println("getManucountByMonth入参数：" + sendObjectParams.toString());
			CountManuOfDayByMonth mCountManuCmdProcess = new CountManuOfDayByMonth(REQUST.cmdReqAllManuByMouse,(String)params[0]);
			mCountManuCmdProcess.setTimeout(30*1000);
			
			DevicesService.sendCmd(sendObjectParams,mCountManuCmdProcess);
		} catch (CommandParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * 按月统计平均驻留时间
	 * @param dayTime 如：2014-05-01 00:00:00
	 */
	private void getManuAvgTimeByMonth() {
		SendObjectParams sendObjectParams = new SendObjectParams();
		Object[] params = new Object[] {DateUtil.getDefaultTimeStringFormat(DateUtil.TIME_FORMAT_YM)+"-01 00:00:00"};
		try {
			sendObjectParams.setParams(REQUST.cmdReqAvgManuStayTimeByMouse, params);
			System.out.println("getManuAvgTimeByMonth入参数：" + sendObjectParams.toString());
			CountManuOfDayByMonth mCountManuCmdProcess = new CountManuOfDayByMonth(REQUST.cmdReqAvgManuStayTimeByMouse,(String)params[0]);
			mCountManuCmdProcess.setTimeout(30*1000);
			
			DevicesService.sendCmd(sendObjectParams,mCountManuCmdProcess);
		} catch (CommandParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 按月统计人流
	 * @author Administrator
	 *
	 */
	public class CountManuOfDayByMonth extends CountManuCmdProcess<ReceiveCountManu>{

		public CountManuOfDayByMonth(REQUST requst, String beginTime) {
			super(requst, beginTime);
			// TODO Auto-generated constructor stub
		}
		public void onProcess(ReceiveCountManu receiveCmdBean) {
			super.onProcess(receiveCmdBean);
			Log.e(TAG, requst+":"+receiveCmdBean.toString());
			try{
				if(requst == REQUST.cmdReqAvgManuStayTimeByMouse){
					if(count_avg_time_tv!=null){
						count_avg_time_tv.setText(getStringZero(receiveCmdBean.countManuResultBeans.get(0).avgTime,2));
					}
					queryManuCountHandler.sendEmptyMessageDelayed(SEND_QUERY_MANU_ID,20*1000);
				}else{
					if(count_all_manu_tv!=null){
						count_all_manu_tv.setText(getStringZero(receiveCmdBean.countManuResultBeans.get(0).inManu,4));
					}
					getManuAvgTimeByMonth();
					
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		public void onResponsTimeout(){
			queryManuCountHandler.sendEmptyMessage(SEND_QUERY_MANU_ID);
		}
		
		private String getStringZero(int value,int len){
			int valueLen = String.valueOf(value).length();
			String temp = String.valueOf(value);
			if(valueLen<len){
				for(int i=len - valueLen;i>0;i--){
					temp = "0"+temp;
				}
			}
			return temp;
		}

	}

}
