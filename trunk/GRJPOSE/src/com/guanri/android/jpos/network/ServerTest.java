package com.guanri.android.jpos.network;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import com.guanri.android.exception.CommandParseException;
import com.guanri.android.exception.PacketException;
import com.guanri.android.jpos.bean.PosMessageBean;
import com.guanri.android.jpos.constant.JposConstant.MessageTypeDefine99Bill;
import com.guanri.android.jpos.constant.JposConstant.MessageTypeDefineUnionpay;
import com.guanri.android.jpos.iso.bill99.JposMessageType99Bill;
import com.guanri.android.jpos.iso.bill99.JposPackage99Bill;
import com.guanri.android.jpos.iso.bill99.JposUnPackage99Bill;
import com.guanri.android.jpos.iso.unionpay.JposMessageTypeUnionPay;
import com.guanri.android.jpos.iso.unionpay.JposPackageUnionPay;
import com.guanri.android.lib.log.Logger;
import com.guanri.android.lib.utils.TypeConversion;

public class ServerTest {
	public static Logger logger = Logger.getLogger(ServerTest.class);//日志对象
	public static void main(String[] args){
		byte[] sendData = queryMoney();
		logger.debug("请求数据:"+TypeConversion.byte2hex(sendData));
		try {
			
//			JposPackage99Bill jposPackage99Bill = new JposPackage99Bill(tree,(JposMessageType99Bill)bill.getmMessageType());
//			sendData = jposPackage99Bill.packaged();
			
			CommandControl.getInstance().connect(10000, 60*000);
			byte[] reData = CommandControl.getInstance().submit(sendData); 
			
			JposUnPackage99Bill bill = new JposUnPackage99Bill(reData);
			bill.unPacketed();
			TreeMap<Integer, Object>  tree = bill.getmReturnMap();
			Iterator<Integer> it = bill.getmReturnMap().keySet().iterator();
			while(it.hasNext()){
				int a = it.next();
				if(a==22||a==24||a==49){
					//tree.put(a, ((String)tree.get(a)).substring(1));
					logger.debug(a+"::::::::::::::::::::::"+tree.get(a));
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CommandParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PacketException e) {
			e.printStackTrace();
		}	
	}
	

	
	
	/**
	 * 余额查询
	 * @param cardNo 第二磁道数据    主账号=有效期+校验码
 	 * @param inputType  输入类型  刷卡0  手输1 
	 * @param pwdstr 密码字段
	 * @param MACK MACK签名字段
	 * @return
	 */
	public static byte[] queryMoney(){
		//构造签到所需各域
		TreeMap<Integer,Object> sendMap = new TreeMap<Integer,Object>();
		sendMap.put(2, "5264102500120211");
		// 域3 处理码
		sendMap.put(3, "310000");
		// 域11 流水号
		sendMap.put(11, "011100");
		// 域 12 本地交易时间
		sendMap.put(12, "115139");
		// 域13 本地交易日期
		sendMap.put(13, "0908");
		sendMap.put(22, "022");
				
		// 域24 NII
		sendMap.put(24,"009");
		// 域25 服务店条件码
		sendMap.put(25, "00");
		// 域35 2磁道数据
		sendMap.put(35, "5264102500120211=1508201");
		// 域41 终端代码
		sendMap.put(41, "20100601");
		// 域42 商户代码
		sendMap.put(42, "104110045110012");
		// 域49  货币代码
		sendMap.put(49, "156");
		// 域52 个人识别码
		//sendMap.put(52, "888888");
		// 域60 自定义域     60.1 交易类型码 00  60.2 批次号  000001 网络管理信息码 001
		//sendMap.put(60, "00000001001000001");
		sendMap.put(61, "000000001123553");
		
		sendMap.put(64, null);
		

		
		JposMessageType99Bill messageType = JposMessageType99Bill.getInstance();
		//设置消息类型
		messageType.setMessageType(MessageTypeDefine99Bill.REQUEST_OP_QUERY_MONEY);
		JposPackage99Bill jposPackage99Bill = new JposPackage99Bill(sendMap,messageType);
	 
		return jposPackage99Bill.packaged();
	}
	
	
	
	/**
	 * 银联签到方法
	 * @return 返回签到报文
	 */
	public static byte[] bill99longin(){
		//构造签到所需各域
		TreeMap<Integer,Object> sendMap = new TreeMap<Integer,Object>();
		// 域11 流水号
		sendMap.put(11, "000001");
		// 域41 终端代码
		sendMap.put(41, "00000001");
		// 域42 商户代码
		sendMap.put(42, "000000000000001");
		// 域60 自定义域     60.1 交易类型码 00  60.2 批次号  000001 网络管理信息码 001
		sendMap.put(60, "000000010001");
		// 域63 自定义域  63.1 操作员代码
		sendMap.put(63, "001");
		
		
		
		JposMessageType99Bill messageType = new JposMessageType99Bill();
		messageType.setPageLength((short)59);
		messageType.setId((byte)0x60);  
		messageType.setServerAddress("0000");
		messageType.setServerAddress("0000");
		messageType.setAddress("0090");
		messageType.setPagever("0100");
		
//		messageType.setServerAddress("0000");
//		messageType.setAddress("0000");
//		messageType.setAppType("60");
//		messageType.setSoftVer("22");
//		messageType.setPosstate("0");
//		messageType.setDisposal("3");
//		messageType.setPreserving("000000");
		//设置消息头类型
		messageType.setMessageType(MessageTypeDefine99Bill.REQUEST_POS_CHECK_IN);
		
		JposPackage99Bill jposPackageUnionPay = new JposPackage99Bill(sendMap,messageType);
	 
		return jposPackageUnionPay.packaged();
	}
	
	/**
	 * 银联签到方法
	 * @return 返回签到报文
	 */
	public static byte[] unionpaylongin(){
		//构造签到所需各域
		TreeMap<Integer,Object> sendMap = new TreeMap<Integer,Object>();
		// 域11 流水号
		sendMap.put(11, "000001");
		// 域41 终端代码
		sendMap.put(41, "00000001");
		// 域42 商户代码
		sendMap.put(42, "000000000000001");
		// 域60 自定义域     60.1 交易类型码 00  60.2 批次号  000001 网络管理信息码 001
		sendMap.put(60, "000000010001");
		// 域63 自定义域  63.1 操作员代码
		sendMap.put(63, "001");
		
		JposMessageTypeUnionPay messageType = new JposMessageTypeUnionPay();
		messageType.setServerAddress("0000");
		messageType.setAddress("0000");
		messageType.setAppType("60");
		messageType.setSoftVer("22");
		messageType.setPosstate("0");
		messageType.setDisposal("3");
		messageType.setPreserving("000000");
		//设置消息头类型
		messageType.setMessageType(MessageTypeDefineUnionpay.REQUEST_POS_CHECK_IN);
		
		JposPackageUnionPay jposPackageUnionPay = new JposPackageUnionPay(sendMap,messageType);
	 
		byte[] ruslt = jposPackageUnionPay.packaged();
		return ruslt;
	}
}