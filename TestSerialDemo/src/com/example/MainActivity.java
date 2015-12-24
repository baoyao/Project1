package com.example;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.winplus.serial.utils.SerialPort;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private String path = "/dev/ttyS3";
	private int baudrate  = 9600;
	private SerialPort mSerialPort = null;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
	
	private TextView mTextView;
	private Button mButton1;
	private EditText mETData;
	private Button mBTNSend;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mETData = (EditText)findViewById(R.id.ET_data);
		mBTNSend = (Button) findViewById(R.id.BTN_send);
		mButton1 = (Button)findViewById(R.id.button1);
		
		try {
			mSerialPort = new SerialPort(new File(path), baudrate, 0);
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();
		} catch (Exception e) {
			Log.v("yzh","can not creat SerialPort");
			e.printStackTrace();
		} 
		
//		mButton1.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
				mReadThread = new ReadThread();
				mReadThread.start();
				Log.v("yzh","thread start");
//			}
//		});
				
		mBTNSend.setOnClickListener(new View.OnClickListener() {
			String str;
			@Override
			public void onClick(View v) {
				str = mETData.getText().toString();
				Log.d("yzh","send data is " + str);
				if(mOutputStream != null && str != null) {
					try {
						mOutputStream.write(str.getBytes());
					} catch (IOException e) {
						e.printStackTrace();
						Log.e("yzh","write error " + e.toString());
					}
				}
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mReadThread != null) {
			mReadThread.isRun = false;
		}
		if(mSerialPort !=  null) {
			mSerialPort.close();
			mSerialPort = null;
		}
	}
	
	
	private class ReadThread extends Thread {
		public boolean isRun = true;
		@Override
		public void run() {
			while (isRun) {
				int size;
				try {
					byte[] buffer = new byte[10];
					for(int i=0; i<10; i++) {
						buffer[i] = 0 ;
					}
//					buffer[0] = 1;
//					buffer[1] = 2;
//					buffer[2] = 3;
					
					if (mInputStream == null)
						return;
					size = mInputStream.read(buffer);
					if (size > 0) {
						for(int i=0;i<size;i++) {
							Log.v("yzhh", "buffer="+ buffer[i]);
							Message msg = new Message();
							msg.what = 1111;
							msg.obj = buffer;
							mHandler.sendMessage(msg);
						}
						mHandler.obtainMessage(222, buffer).sendToTarget();
					}
					
//					if(buffer.length >0) {
//						Log.v("yzh", "buffer.length="+ buffer.length);
//						Message msg = new Message();
//						msg.what = 1111;
//						msg.obj = buffer;
//						mHandler.sendMessage(msg);
//					}
					
					Thread.sleep(1000);
//					isRun =  false;
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			Log.v("yzh", "Thread stop");
		}
	}
	
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 1111:{
				String str = new String();
				byte[] buf =  ((byte[])msg.obj);
				Log.v("yzh","buf.length="+buf.length);
				for(int i=0; i<10; i++) {
					Log.v("yzh","buf="+buf[i]);
					str += new Byte(buf[i]).toString();
				}
				Log.v("yzh","str="+str);
				//mTextView.setText(str);
				break;}
			case 222:{
				String str = new String();
				byte[] buf =  ((byte[])msg.obj);
				for(int i=0; i<10; i++) {
					str += new Byte(buf[i]).toString();
				}
				Toast.makeText(MainActivity.this, "收到的数据: "+str, Toast.LENGTH_SHORT).show();
				break;}
			default:
				break;
			}
		};
	};
}
