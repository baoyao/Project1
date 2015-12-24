package com.meinan.pad1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.winplus.serial.utils.SerialPort;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 用于显示对话的主Activity
 */
public class MainActivity extends Activity {

	private static final String TAG = "tt";

	private EditText outEt;// 布局中的控件引用
	private Button sendBtn, getProtDataBtn;
	private String connectedNameStr = null;// 已连接的设备名称
	private StringBuffer outSb;// 发送的字符信息
	private BluetoothAdapter btAdapter = null;// 本地蓝牙适配器
	private MyService myService = null;// Service引用
	private Toast mToast;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
		// 获取本地蓝牙适配器
		btAdapter = BluetoothAdapter.getDefaultAdapter();

		initPort();// 串口
		initRead();// 串口
	}

	@Override
	public void onStart() {
		super.onStart();
		// 如果蓝牙没有开启，提示开启蓝牙，并退出Activity
		if (!btAdapter.isEnabled()) {
			showShortMessage(getResources().getString(R.string.open_bluetooth));
			btAdapter.enable();
		} else {
			// 否则初始化聊天的控件
			initChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (myService != null) {// 创建并开启Service
			// 如果Service为空状态
			if (myService.getState() == MyService.STATE_NONE) {
				myService.start();// 开启Service
			}
		}

		if (!isRegist) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
			registerReceiver(bluetoothReceiver, filter);
			isRegist = true;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isRegist) {
			unregisterReceiver(bluetoothReceiver);
			isRegist = false;
		}
	}

	private void initChat() {
		if (myService != null) {
			return;
		}
		outEt = (EditText) findViewById(R.id.edit_text_out);// 获取编辑文本框的引用
		// 获取发送按钮引用，并为其添加监听
		sendBtn = (Button) findViewById(R.id.button_send);
		sendBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// 获取编辑文本框中的文本内容，并发送消息
				TextView view = (TextView) findViewById(R.id.edit_text_out);
				String message = view.getText().toString();
				sendMessage(message);
			}
		});

		getProtDataBtn = (Button) findViewById(R.id.button_get_prot_data);
		getProtDataBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getDataFromPort();
			}
		});

		myService = new MyService(this, mHandler);// 创建Service对象
		// 初始化存储发送消息的StringBuffer
		outSb = new StringBuffer("");
	}

	// 发送消息的方法
	private void sendMessage(String message) {
		// 先检查是否已经连接到设备
		if (myService.getState() != MyService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}
		if (message.length() > 0) {// 如果消息不为空再发送消息
			byte[] send = message.getBytes();// 获取发送消息的字节数组，并发送
			myService.write(send);
			// 消除StringBuffer和编辑文本框的内容
			outSb.setLength(0);
			outEt.setText(outSb);
		}
	}

	// 处理从Service发来的消息的Handler
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constant.MSG_READ: {
				byte[] readBuf = (byte[]) msg.obj;
				// 创建要发送的信息的字符串
				String readMessage = new String(readBuf, 0, msg.arg1);
				showLongMessage(connectedNameStr + ":  " + readMessage);
				setDataToPort(readMessage);// 发送数据给串口
				break;
			}
			case Constant.MSG_DEVICE_NAME:
				// 获取已连接的设备名称，并弹出提示信息
				connectedNameStr = msg.getData()
						.getString(Constant.DEVICE_NAME);
				showShortMessage("已连接到 " + connectedNameStr);
				break;
			case Constant.MSG_INIT_CHAT:
				initChat();
				break;
			case Constant.MSG_READ_PORT_DATA: {
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				protBufferData = readMessage;
				showLongMessage("接到串口的数据： " + readMessage);
				MainActivity.this.sendMessage(readMessage == null ? ""
						: readMessage);
				break;
			}

			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 1:
			// 如果设备列表Activity返回一个连接的设备
			if (resultCode == Activity.RESULT_OK) {
				// 获取设备的MAC地址
				String address = data.getExtras().getString(
						MyDeviceListActivity.EXTRA_DEVICE_ADDR);
				// 获取BLuetoothDevice对象
				BluetoothDevice device = btAdapter.getRemoteDevice(address);
				myService.connect(device);// 连接该设备
			}
			break;
		}
	}

	private void showShortMessage(String mess) {
		mToast.setDuration(Toast.LENGTH_SHORT);
		mToast.setText(mess);
		mToast.show();
	}

	private void showLongMessage(String mess) {
		mToast.setDuration(Toast.LENGTH_LONG);
		mToast.setText(mess);
		mToast.show();
	}

	private boolean isRegist = false;
	BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				if (btAdapter.isEnabled()) {
					showShortMessage(MainActivity.this.getResources()
							.getString(R.string.bluetooth_opened));
					mHandler.sendEmptyMessage(Constant.MSG_INIT_CHAT);
				}
			}
		}
	};

	// 串口@{
	private String path = "/dev/ttyS3";
	private int baudrate = 9600;
	private SerialPort mSerialPort;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;

	private String protBufferData = "";

	private void initPort() {
		try {
			mSerialPort = new SerialPort(new File(path), baudrate, 0);
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			Log.v(TAG, "can not creat SerialPort: " + e.toString());
		}
	}

	private void closePort() {
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
	}

	private void initRead() {
		mReadThread = new ReadThread();
		mReadThread.isRun = true;
		mReadThread.start();
	}

	private void closeRead() {
		if (mReadThread != null) {
			mReadThread.isRun = false;
		}
	}

	private void setDataToPort(String data) {
		Log.d(TAG, "send data is " + data);
		if (mOutputStream != null && data != null && !"".equals(data.trim())) {
			try {
				mOutputStream.write(data.getBytes());
				Log.d(TAG, "send data success");
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "write error " + e.toString());
			}
		}
	}

	private void getDataFromPort() {
		showShortMessage(protBufferData);
	}

	private class ReadThread extends Thread {
		public boolean isRun = true;

		@Override
		public void run() {
			while (isRun) {
				int size;
				try {
					byte[] buffer = new byte[1024];
					for (int i = 0; i < buffer.length; i++) {
						buffer[i] = 0;
					}
					if (mInputStream == null)
						return;
					size = mInputStream.read(buffer);
					if (size > 0) {
						Log.v(TAG, "ReadThread size: " + size);
						for (int i = 0; i < size; i++) {
							Log.v(TAG, "buffer = " + buffer[i]);
						}
						mHandler.obtainMessage(Constant.MSG_READ_PORT_DATA,
								size, -1, buffer).sendToTarget();
					}
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			Log.v(TAG, "Thread stop");
		}
	}

	// @}

	@Override
	public void onDestroy() {
		super.onDestroy();
		closeRead();// 串口
		closePort();// 串口

		if (myService != null) {// 停止Service
			myService.stop();
		}
	}

}