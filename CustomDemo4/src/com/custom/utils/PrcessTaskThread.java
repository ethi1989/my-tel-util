package com.custom.utils;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.custom.client.Main;

public class PrcessTaskThread extends JDialog {
	private JProgressBar proBar;// 进度条
	private int min = 0;// 进度条的起始值
	private int max = 0;// 进度条的最大值
	private TaskThread proThread;
	private String title;
	private String msg ;
	private boolean stop = false;
	private CloseLintener closeLintener;
	public PrcessTaskThread(String title,String msg,int minValue,int maxValue,CloseLintener closeLintener) {
		this.title = title;
		this.msg = msg;
		this.min = minValue;
		this.max = maxValue;
		this.closeLintener = closeLintener;
		init();
	}
	
	public void stopBar(){
		this.stop = true;
		dispose();
	}
	/**
	 * 初始化对话框
	 */
	public void init() {
		
		setTitle(title);
		setModal(true);
		proBar = new JProgressBar();
		proBar.setOpaque(false);//背景色设为透明的了       

		proBar.setString(msg);
		proBar.setValue(0);
		proBar.setMaximum(max);
		proBar.setStringPainted(true);// 设置显示字符串
		add(proBar);
		proThread = new TaskThread();
		proThread.start();
		

		new Thread(){
			public void run(){
				setLocation(Main.mainInstance.location().x, 
						Main.mainInstance.location().y);// 定位
				setSize(250, 70);
				setLocationRelativeTo(Main.mainInstance);
				setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
				// 添加窗口关闭事件
				addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						stopBar();
						if(closeLintener!=null)
							closeLintener.close();
						dispose();
					}
				});
				PrcessTaskThread.this.setVisible(true);
			}
		}.start();
		

	}
	
	public void setValue(String msg,int minValue,int maxValue){
		this.msg = msg;
		this.min = minValue;
		this.max = maxValue;
	}

	/**
	 * 进度条线程
	 * 
	 */
	class TaskThread extends Thread {
		public void run() {
			proBar.setValue(min);
			proBar.setMinimum(min);
			proBar.setMaximum(max);
			proBar.setString(msg);

			Runnable runner = new Runnable() {
				public void run() {
					proBar.setMaximum(max);
					proBar.setString(msg);
					proBar.setValue(min);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			while(!stop){
				try {
					SwingUtilities.invokeAndWait(runner);// 组件和线程同步进行
				} catch (InvocationTargetException e) {
					break;
				} catch (InterruptedException e) {
				}
			}
			dispose();
			proThread = null;
		}
	}
	
	public interface CloseLintener{
		public void close();
	}
	
	
	public static void main(String[] args){
		
		try{
			File file = new File("g:\\\\custom\\update1.zip");
			if(file.exists())
				file.delete();
		}catch(Exception e){
			e.printStackTrace();
		}
//		new Main();
//		PrcessTaskThread prcessTaskThread = new PrcessTaskThread(
//				"复制文件","正在复制"
//				,0,100,new CloseLintener(){
//					public void close(){
//					}
//				});
//		for(int i=0;i<100;i++){
//			prcessTaskThread.setValue("正在复制("+i+"/"+100+"M)"
//					,i,100);
//			try{
//				Thread.sleep(500);
//			}catch(Exception e){
//				
//			}
//			
//		}

	}

}