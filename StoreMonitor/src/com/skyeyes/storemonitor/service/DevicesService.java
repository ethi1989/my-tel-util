package com.skyeyes.storemonitor.service;

import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.SoundPool;
import android.net.Uri;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.skyeyes.base.BaseSocketHandler;
import com.skyeyes.base.cmd.CommandControl.REQUST;
import com.skyeyes.base.cmd.bean.ReceiveCmdBean;
import com.skyeyes.base.cmd.bean.SendCmdBean;
import com.skyeyes.base.cmd.bean.impl.ReceivLogin;
import com.skyeyes.base.cmd.bean.impl.ReceiveDeviceAlarm;
import com.skyeyes.base.cmd.bean.impl.ReceiveDeviceRegisterInfo;
import com.skyeyes.base.cmd.bean.impl.ReceiveDeviceStatus;
import com.skyeyes.base.cmd.bean.impl.ReceiveHeart;
import com.skyeyes.base.cmd.bean.impl.ReceiveLoginOut;
import com.skyeyes.base.cmd.bean.impl.ReceiveReadDeviceList;
import com.skyeyes.base.cmd.bean.impl.ReceiveStatusChange;
import com.skyeyes.base.cmd.bean.impl.ReceiveUserInfo;
import com.skyeyes.base.cmd.bean.impl.SendObjectParams;
import com.skyeyes.base.exception.CommandParseException;
import com.skyeyes.base.exception.NetworkException;
import com.skyeyes.base.network.impl.SkyeyeSocketClient;
import com.skyeyes.base.util.PreferenceUtil;
import com.skyeyes.base.util.StringUtil;
import com.skyeyes.storemonitor.R;
import com.skyeyes.storemonitor.StoreMonitorApplication;
import com.skyeyes.storemonitor.activity.HomeActivity;
import com.skyeyes.storemonitor.activity.VideoPlayActivity;
import com.skyeyes.storemonitor.process.DeviceProcessInterface;
import com.skyeyes.storemonitor.process.DeviceProcessInterface.DeviceReceiveCmdProcess;
import com.skyeyes.storemonitor.process.DeviceProcessInterface.DeviceStatusChangeListener;
import com.skyeyes.storemonitor.process.impl.DeviceProcess;

public class DevicesService extends Service implements DeviceStatusChangeListener{
	static String TAG = "DevicesService";
	
	public static String DeviceAlarmBroadCast = "DeviceAlarmBroadCast";
	public static String UserInfoBroadCast = "UserInfoBroadCast";
	public static String DeviceStatusChangeBroadCast = "DeviceStatusChangeBroadCast";
	public static int NOTIFICATION_ID = 123456;
	public static int ERROR_NOTIFICATION_ID = 123457;
	
	private static DevicesService instance = null;
	private HashMap<String,DeviceProcessInterface> mDeviceDeviceProcesss = new HashMap<String,DeviceProcessInterface>();
	private HashMap<String,DeviceProcessInterface> mTempDeviceDeviceProcesss = new HashMap<String,DeviceProcessInterface>();
	
	public QueryDeviceList mQueryDeviceList;
	
	private static HashMap<String,DeviceReceiveCmdProcess> mStaticCmdProcess = new HashMap<String,DeviceReceiveCmdProcess>();
	
	private HashMap<String,HashMap<String,DeviceReceiveCmdProcess>> mStaticDeviceReceiveCmdProcess = 
			new HashMap<String,HashMap<String,DeviceReceiveCmdProcess>>();
	
	private String mCurrentDeviceCode = null;
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static DevicesService getInstance(){
		return instance;
	}
	
	public String getCurrentDeviceCode(){
		return mCurrentDeviceCode;
	}
	
	public void onCreate(){
		super.onCreate();
		
		Log.d(TAG, "onCreate................");
		instance = this;
		mQueryDeviceList = new QueryDeviceList();
		
		mQueryDeviceList.queryEquitListNoLogin();
	}
	

	
	public void onDestroy(){
		super.onDestroy();
		Log.d("DevicesService", "onDestroy................");
		instance = null;
		StoreMonitorApplication.getInstance().setReceivLogin(null);
		for(DeviceProcessInterface deviceProcess:mDeviceDeviceProcesss.values()){
			deviceProcess.stop();
		}
		mDeviceDeviceProcesss.clear();
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// 取消的只是当前Context的Notification
		mNotificationManager.cancel(ERROR_NOTIFICATION_ID);
		mNotificationManager.cancel(NOTIFICATION_ID);
	}
	
	public void onStart(Intent intent, int startId){
		super.onStart(intent, startId);

	}
	
	public void initDevices(){
		String deviceListString = PreferenceUtil.getConfigString(PreferenceUtil.DEVICE_INFO,PreferenceUtil.device_code_list);
		String[] deviceCodes = deviceListString.split(";");
		Log.d("DeviceListConnectService", "start................"+deviceCodes.length+":"+deviceCodes[0]);
		for(String deviceCode:deviceCodes){
			if(!mDeviceDeviceProcesss.containsKey(deviceCode)){
				//初始化回调对象容器
				if(!instance.mStaticDeviceReceiveCmdProcess.containsKey(deviceCode)){
					instance.mStaticDeviceReceiveCmdProcess.put(deviceCode, new HashMap<String,DeviceReceiveCmdProcess>());
				}
				//初始化设备管理器,并登陆设备
				initDeviceProcess(deviceCode);
			}
		}
		mStaticCmdProcess.clear();
	}
	/**
	 * 选择设备
	 * @param deviceCode
	 */
	public void selectDevice(String deviceCode){
		PreferenceUtil.setSingleConfigInfo(PreferenceUtil.DEVICE_INFO, 
				PreferenceUtil.device_current_code, deviceCode);
		mCurrentDeviceCode = deviceCode;
	}
	
	public void reLoginDevice(){
		//初始化设备管理器,并登陆设备
		initDeviceProcess(mCurrentDeviceCode);
	}
	@Override
	public void onDeviceLogin(String deviceCode, ReceivLogin receivLogin) {
		// TODO Auto-generated method stub
		Log.i("DevicesService","onDeviceLogin:"+receivLogin.deviceCode);
		if(mTempDeviceDeviceProcesss.containsKey(deviceCode)){
			if(receivLogin.getCommandHeader().resultCode ==0){
				mDeviceDeviceProcesss.put(receivLogin.deviceCode, 
						mTempDeviceDeviceProcesss.remove(receivLogin.deviceCode));
				StoreMonitorApplication.getInstance().setReceivLogin(receivLogin);
				registerConnectListener(deviceCode);
				Log.i("DevicesService","mDeviceSocketClients:"+receivLogin.deviceCode);
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				// 取消的只是当前Context的Notification
				mNotificationManager.cancel(ERROR_NOTIFICATION_ID);

			}else{
				showErrorNotification( "登陆失败:"+receivLogin.getCommandHeader().errorInfo);
				StoreMonitorApplication.getInstance().setReceivLogin(null);
				mTempDeviceDeviceProcesss.remove(deviceCode).stop();
				new Thread(){
					public void run(){
						try {
							sleep(30000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						reLoginDevice();
					}
				}.start();
				
			}
		}
	}
	
	 // 显示一个通知   
	public void showErrorNotification(String des) {
		// 创建一个通知
		Notification mNotification = new Notification();

		// 设置属性值
		mNotification.icon = R.drawable.ic_launcher;
		mNotification.tickerText = des;
		mNotification.when = System.currentTimeMillis(); // 立即发生此通知
		// 设置setLatestEventInfo方法,如果不设置会App报错异常
		Intent notificationIntent = new Intent(DevicesService.this, HomeActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(DevicesService.this, 0,notificationIntent, 0);
		mNotification.setLatestEventInfo(DevicesService.this, "提示信息", des, contentIntent);  
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(ERROR_NOTIFICATION_ID, mNotification);

	}  

	
	@Override
	public void onDeviceStatusChange(String deviceCode,ReceiveCmdBean receiveCmdBean) {
		// TODO Auto-generated method stub
		
		if(receiveCmdBean instanceof ReceiveDeviceRegisterInfo){
			StoreMonitorApplication.getInstance().setReceiveDeviceRegisterInfo((ReceiveDeviceRegisterInfo)receiveCmdBean);
			
			queryUserInfo();//查询用户信息
			getDeviceStatus();//查询设备状态
		}
		
	}
	
	public void onSkyeyeNetworkException(final String deviceCode,NetworkException ex){
		if(mDeviceDeviceProcesss.containsKey(deviceCode)){
			mDeviceDeviceProcesss.remove(deviceCode).stop();
		}
		new Thread(){
			public void run(){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				initDeviceProcess(deviceCode);
			}
		}.start();
	} 
	
	public void registerConnectListener(String deviceCode){
		mStaticDeviceReceiveCmdProcess.get(deviceCode).put("ReceiveLoginOut",new LoginOutProcess(deviceCode));
		mStaticDeviceReceiveCmdProcess.get(deviceCode).put("ReceiveHeart",new ConnectHeart(deviceCode));
		mStaticDeviceReceiveCmdProcess.get(deviceCode).put("ReceiveDeviceAlarm",new DeviceAlarmProcess(deviceCode));
		mStaticDeviceReceiveCmdProcess.get(deviceCode).put("ReceiveStatusChange",new StatusChangeProcess(deviceCode));
		
	}
	
	LoginReceive loginProcess = new LoginReceive();
	
	private void initDeviceProcess(String deviceCode){
		DeviceProcess deviceProcess = new DeviceProcess(deviceCode,this);
		mTempDeviceDeviceProcesss.put(deviceCode, deviceProcess);
		
		mStaticDeviceReceiveCmdProcess.get(deviceCode).putAll(mStaticCmdProcess);
		deviceProcess.setCmdProcessMaps(mStaticDeviceReceiveCmdProcess.get(deviceCode));
		//登陆
		String userName = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_name);
		String userPsd = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_psd);
		
		loginProcess.setTimeout(30*1000);
		//config 0,是否接收状态变化事件	1是否接收正报事件	3是否接收提示(出入)事件
		deviceProcess.loginDevice(userName, userPsd, (byte)0x0F,loginProcess);
	}
	
	
	public static void sendCmd(SendCmdBean sendCmdBean) {
		sendCmd(sendCmdBean,null);
	}
	
	public static void sendCmd(SendCmdBean sendCmdBean,DeviceReceiveCmdProcess receiveCmdProcess) {
		if(instance == null)
			return;
		sendCmd(instance.getCurrentDeviceCode(),sendCmdBean,receiveCmdProcess);
	}
	
	public static void sendCmd(String deviceCode,SendCmdBean sendCmdBean,DeviceReceiveCmdProcess receiveCmdProcess) {
		if(instance == null || 
				instance.mDeviceDeviceProcesss.get(deviceCode) == null)
			return;
		instance.mDeviceDeviceProcesss.get(deviceCode).sendCmd(sendCmdBean,receiveCmdProcess);
	}
	
	public static void registerCmdProcess(String className,DeviceReceiveCmdProcess receiveCmdProcess){
		String deviceListString = PreferenceUtil.getConfigString(PreferenceUtil.DEVICE_INFO,PreferenceUtil.device_code_list);
		if(instance==null||"".equals(deviceListString)){
			mStaticCmdProcess.put(className, receiveCmdProcess);
			return;
		}
		String[] deviceCodes = deviceListString.split(";");
		for(String deviceCode:deviceCodes){
			Log.e(TAG, "registerCmdProcess:"+deviceCode+":"+className);
			//初始化回调对象容器
			if(!instance.mStaticDeviceReceiveCmdProcess.containsKey(deviceCode)){
				instance.mStaticDeviceReceiveCmdProcess.put(deviceCode, new HashMap<String,DeviceReceiveCmdProcess>());
			}
			instance.mStaticDeviceReceiveCmdProcess.get(deviceCode).put(className,receiveCmdProcess);
		}
	}
	
	public static void unRegisterCmdProcess(String className){
		String deviceListString = PreferenceUtil.getConfigString(PreferenceUtil.DEVICE_INFO,PreferenceUtil.device_code_list);
		if(instance==null||"".equals(deviceListString)){
			mStaticCmdProcess.remove(className);
			return;
		}
		
		String[] deviceCodes = deviceListString.split(";");
		for(String deviceCode:deviceCodes){
			instance.mStaticDeviceReceiveCmdProcess.get(deviceCode).remove(className);
		}
	}

	public HashMap<String, DeviceProcessInterface> getDeviceDeviceProcesss() {
		return mDeviceDeviceProcesss;
	}

	public HashMap<String, DeviceProcessInterface> getTempDeviceDeviceProcesss() {
		return mTempDeviceDeviceProcesss;
	}

	/**
	 * 退出登录
	 * @author Administrator
	 *
	 */
	public class LoginOutProcess extends DeviceReceiveCmdProcess<ReceiveLoginOut>{
		String deviceCode;
		public LoginOutProcess(String deviceCode){
			this.deviceCode = deviceCode;
		}

		public void onProcess(ReceiveLoginOut receiveCmdBean) {
			reLoginDevice();
		}


		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
	}
	/**
	 * 退出登录
	 * @author Administrator
	 *
	 */
	public class ConnectHeart extends DeviceReceiveCmdProcess<ReceiveHeart>{
		String deviceCode;
		public ConnectHeart(String deviceCode){
			this.deviceCode = deviceCode;
		}
		public void onProcess(ReceiveHeart receiveCmdBean) {
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
	}
	
	UserInfoQuery userInfoQuery = new UserInfoQuery();
	private void queryUserInfo(){
		//查询通道图片
		SendObjectParams sendObjectParams = new SendObjectParams();
		Object[] params = new Object[] {};
		try {
			sendObjectParams.setParams(REQUST.cmdReqUserInfo, params);
			System.out.println("cmdReqUserInfo入参数：" + sendObjectParams.toString());
			
			DevicesService.sendCmd(sendObjectParams, userInfoQuery);
		} catch (CommandParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 查询用户信息
	 * @author Administrator
	 *
	 */
	public class UserInfoQuery extends DeviceReceiveCmdProcess<ReceiveUserInfo>{

		public void onProcess(ReceiveUserInfo receiveCmdBean) {
			try{
				StoreMonitorApplication.getInstance().setReceiveUserInfo(receiveCmdBean);
				Intent in = new Intent(UserInfoBroadCast);
				DevicesService.this.sendBroadcast(in);
			}catch(Exception e){
				
			}
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
	}
	
	DeviceStatusReceive deviceStatusReceive = new DeviceStatusReceive();
	private void getDeviceStatus(){

		SendObjectParams sendObjectParams = new SendObjectParams();
		Object[] params = new Object[] {};
		try {
			sendObjectParams.setParams(REQUST.cmdGetActive, params);
			System.out.println("cmdGetActive入参数：" + sendObjectParams.toString());
			DevicesService.sendCmd(sendObjectParams,deviceStatusReceive);
		} catch (CommandParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 查询设备状态
	 * @author Administrator
	 *
	 */
	private class DeviceStatusReceive extends DeviceReceiveCmdProcess<ReceiveDeviceStatus>{
		public void onProcess(ReceiveDeviceStatus receiveCmdBean) {
			// TODO Auto-generated method stub
				if(receiveCmdBean.getCommandHeader().resultCode == 0){
					StoreMonitorApplication.getInstance().setDeviceStatus(receiveCmdBean.deviceStatus);
					Intent in = new Intent(DeviceStatusChangeBroadCast);
					DevicesService.this.sendBroadcast(in);
				}
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
		}
		
	}
	
	/**
	 * 收到设备状态变化信息
	 * @author Administrator
	 *
	 */
	public class StatusChangeProcess extends DeviceReceiveCmdProcess<ReceiveStatusChange>{
		String deviceCode;
		public StatusChangeProcess(String deviceCode){
			this.deviceCode = deviceCode;
		}
		public void onProcess(ReceiveStatusChange receiveCmdBean) {
			if (receiveCmdBean.getCommandHeader().resultCode == 0) {
				StoreMonitorApplication.getInstance().setDeviceStatus(receiveCmdBean.status);
				Intent in = new Intent(DeviceStatusChangeBroadCast);
				DevicesService.this.sendBroadcast(in);
			}
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
	}
	/**
	 * 收到报警及提示信息
	 * @author Administrator
	 *
	 */
	public class DeviceAlarmProcess extends DeviceReceiveCmdProcess<ReceiveDeviceAlarm> implements  SoundPool.OnLoadCompleteListener{
 
		String deviceCode;
//		SoundPool sndPool;
//		int id;
		public DeviceAlarmProcess(String deviceCode){
			this.deviceCode = deviceCode;
//			sndPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0 ) ;
//	        sndPool.setOnLoadCompleteListener(this);
//	        try {
//	        	id = sndPool.load(DevicesService.this.getAssets().openFd("alarm.ogg"), 1);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		public void onProcess(ReceiveDeviceAlarm receiveCmdBean) {
			if(receiveCmdBean.startType == 0){////是否报警触发，0＝提示触发，1＝报警触发
				showNoticeNotification(receiveCmdBean.des);
			}else{
				Intent in = new Intent(DeviceAlarmBroadCast);
				DevicesService.this.sendBroadcast(in);
				showNotification(receiveCmdBean.eventCode,receiveCmdBean.des,receiveCmdBean.channelId);
			}

		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
		
		public void onLoadComplete(SoundPool soundPool, int sampleId, int status)  {

		}
		
		
		 // 显示一个通知   
		public void showNoticeNotification(String des) {
			// 创建一个通知
			Notification mNotification = new Notification();
			// 设置属性值
			mNotification.icon = R.drawable.ic_launcher;
			mNotification.tickerText = des;
			mNotification.when = System.currentTimeMillis(); // 立即发生此通知
			mNotification.flags = Notification.FLAG_INSISTENT;
	        mNotification.setLatestEventInfo(DevicesService.this, "", des, null);  

			// 设置setLatestEventInfo方法,如果不设置会App报错异常
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// 注册此通知
			mNotificationManager.notify(NOTIFICATION_ID, mNotification);

		} 
		
		 // 显示一个通知   
		public void showNotification(String eventcode,String des,byte channelId) {
			// 创建一个通知
			Notification mNotification = new Notification();

			// 设置属性值
			mNotification.icon = R.drawable.ic_launcher;
			mNotification.tickerText = des;
			mNotification.when = System.currentTimeMillis(); // 立即发生此通知

			// 带参数的构造函数,属性值如上
			// Notification mNotification = = new
			// Notification(R.drawable.icon,"NotificationTest",
			// System.currentTimeMillis()));

			// 添加声音效果
			//mNotification.defaults |= Notification.DEFAULT_SOUND;
			mNotification.sound = Uri.parse("android.resource://"+DevicesService.this.getPackageName()+"/"+R.raw.alarm); ;
			

			// 添加震动,由于在我的真机上会App发生异常,估计是Android2.2里的错误,略去，不添加
			//mNotification.defaults |= Notification.DEFAULT_VIBRATE ;

			// 添加状态标志

			// FLAG_AUTO_CANCEL 该通知能被状态栏的清除按钮给清除掉
			// FLAG_NO_CLEAR 该通知能不被状态栏的清除按钮给清除掉
			// FLAG_ONGOING_EVENT 通知放置在正在运行
			// FLAG_INSISTENT 通知的音乐效果一直播放
			mNotification.flags = Notification.FLAG_INSISTENT;
			mNotification.flags |=Notification.FLAG_NO_CLEAR;
			
			Intent notificationIntent = new Intent(DevicesService.this, VideoPlayActivity.class);
			notificationIntent.putExtra("alarmId",eventcode);
			notificationIntent.putExtra("chennalId",channelId);
			notificationIntent.putExtra("videoType",2);
			notificationIntent.putExtra("isPushAlarm",1);
			
			
	        PendingIntent contentIntent = PendingIntent.getActivity(DevicesService.this, 0,notificationIntent, 0);
	       
	        mNotification.setLatestEventInfo(DevicesService.this, "设备报警", des, contentIntent);  

			// 设置setLatestEventInfo方法,如果不设置会App报错异常
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// 注册此通知
			// 如果该NOTIFICATION_ID的通知已存在，不会重复添加，只是播放相应的效果(声音等)

			mNotificationManager.notify(NOTIFICATION_ID, mNotification);

		}  
	}
	
	
	public class QueryDeviceList {
		SocketHandlerImpl socketHandlerImpl = new SocketHandlerImpl();
		// 查询设备列表
		public void queryEquitListNoLogin() {

			String userName = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_name);
			String userPsd = PreferenceUtil.getConfigString(PreferenceUtil.ACCOUNT_IFNO, PreferenceUtil.account_login_psd);
			String ip = PreferenceUtil.getConfigString(PreferenceUtil.SYSCONFIG, PreferenceUtil.sysconfig_server_ip);
			String port = PreferenceUtil.getConfigString(PreferenceUtil.SYSCONFIG, PreferenceUtil.sysconfig_server_port);
			
			if(StringUtil.isNull(userName)||
					StringUtil.isNull(userPsd)||
					StringUtil.isNull(ip)||
					StringUtil.isNull(port)){
				showErrorNotification( "信息不完整，请前往设置页面进行设置");
				return ;
			}
			try {
				SkyeyeSocketClient skyeyeSocketClient = new SkyeyeSocketClient(
						socketHandlerImpl.setTimeout(20*1000), true);
				skyeyeSocketClient.setServerAddr(ip, Integer.parseInt(port));
				SendObjectParams sendObjectParams = new SendObjectParams();
				Object[] params = new Object[] { userName, userPsd };
				sendObjectParams.setParams(REQUST.cmdUserEquitListNOLogin, params);
				Log.i(TAG,"testEquitListNoLogin入参数："+ sendObjectParams.toString());
				skyeyeSocketClient.sendCmd(sendObjectParams);
			} catch (CommandParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NetworkException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private class SocketHandlerImpl extends BaseSocketHandler {
			public static final int TIMEOUT_WHAT = 1;
			@Override
			public void onReceiveCmdEx(final ReceiveCmdBean receiveCmdBean) {
				// TODO Auto-generated method stub
				mHandler.removeMessages(TIMEOUT_WHAT);
				Log.i("SocketHandlerImpl","解析报文成功:" + (receiveCmdBean!=null?receiveCmdBean.toString():"receiveCmdBean is null"));
				if (receiveCmdBean instanceof ReceiveReadDeviceList) {
					if(((ReceiveReadDeviceList) receiveCmdBean).getCommandHeader().resultCode == 0){
						final ReceiveReadDeviceList receiveReadDeviceList = ((ReceiveReadDeviceList) receiveCmdBean);
						PreferenceUtil.setSingleConfigInfo(PreferenceUtil.DEVICE_INFO,
								PreferenceUtil.device_count, receiveReadDeviceList.deviceCodeList.size());
						PreferenceUtil.setSingleConfigInfo(PreferenceUtil.DEVICE_INFO,
								PreferenceUtil.device_code_list,  receiveReadDeviceList.deviceListString);
						initDevices();
						selectDevice(receiveReadDeviceList.deviceCodeList.get(0));
					}else{
						showErrorNotification( "查询设备失败，从新查询");
						new Thread(){
							public void run(){
								queryEquitListNoLogin();
							}
						}.start();
					}
				}
			}

			@Override
			public void onCmdExceptionEx(CommandParseException ex) {
				// TODO Auto-generated method stub
				mHandler.removeMessages(TIMEOUT_WHAT);
			}

			@Override
			public void onSocketExceptionEx(NetworkException ex) {
				// TODO Auto-generated method stub
				mHandler.removeMessages(TIMEOUT_WHAT);
				new Thread(){
					public void run(){
						queryEquitListNoLogin();
					}
				}.start();

			}

			@Override
			public void onSocketClosedEx() {
				// TODO Auto-generated method stub
				mHandler.removeMessages(TIMEOUT_WHAT);

			}
			
			/**
			 * 设置响应超时
			 * @param timeout Millis
			 */
			public synchronized SocketHandlerImpl setTimeout(long timeout){
				mHandler.sendEmptyMessageDelayed(TIMEOUT_WHAT, timeout);
				return this;
			}

			public void handleMessage(Message msg){
				if(msg.what == TIMEOUT_WHAT){
					showErrorNotification( "查询设备列表超时");
					new Thread(){
						public void run(){
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							queryEquitListNoLogin();
						}
					}.start();
					
				}
			}

		};
	}
	
	private class LoginReceive extends DeviceReceiveCmdProcess<ReceivLogin>{

		@Override
		public void onProcess(ReceivLogin receiveCmdBean) {
			// TODO Auto-generated method stub
		}
		
		public void onResponsTimeout(){
			showErrorNotification( "登陆超时");
			new Thread(){
				public void run(){
					reLoginDevice();
				}
			}.start();
		}

		@Override
		public void onFailure(String errinfo) {
			// TODO Auto-generated method stub
			
		}
		
	}
}


