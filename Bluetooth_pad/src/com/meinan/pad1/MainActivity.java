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
 * ������ʾ�Ի�����Activity
 */
public class MainActivity extends Activity {

	private static final String TAG = "tt";

	private EditText outEt;// �����еĿؼ�����
	private Button sendBtn, getProtDataBtn;
	private String connectedNameStr = null;// �����ӵ��豸����
	private StringBuffer outSb;// ���͵��ַ���Ϣ
	private BluetoothAdapter btAdapter = null;// ��������������
	private MyService myService = null;// Service����
	private Toast mToast;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
		// ��ȡ��������������
		btAdapter = BluetoothAdapter.getDefaultAdapter();

		initPort();// ����
		initRead();// ����
	}

	@Override
	public void onStart() {
		super.onStart();
		// �������û�п�������ʾ�������������˳�Activity
		if (!btAdapter.isEnabled()) {
			showShortMessage(getResources().getString(R.string.open_bluetooth));
			btAdapter.enable();
		} else {
			// �����ʼ������Ŀؼ�
			initChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (myService != null) {// ����������Service
			// ���ServiceΪ��״̬
			if (myService.getState() == MyService.STATE_NONE) {
				myService.start();// ����Service
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
		outEt = (EditText) findViewById(R.id.edit_text_out);// ��ȡ�༭�ı��������
		// ��ȡ���Ͱ�ť���ã���Ϊ����Ӽ���
		sendBtn = (Button) findViewById(R.id.button_send);
		sendBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// ��ȡ�༭�ı����е��ı����ݣ���������Ϣ
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

		myService = new MyService(this, mHandler);// ����Service����
		// ��ʼ���洢������Ϣ��StringBuffer
		outSb = new StringBuffer("");
	}

	// ������Ϣ�ķ���
	private void sendMessage(String message) {
		// �ȼ���Ƿ��Ѿ����ӵ��豸
		if (myService.getState() != MyService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}
		if (message.length() > 0) {// �����Ϣ��Ϊ���ٷ�����Ϣ
			byte[] send = message.getBytes();// ��ȡ������Ϣ���ֽ����飬������
			myService.write(send);
			// ����StringBuffer�ͱ༭�ı��������
			outSb.setLength(0);
			outEt.setText(outSb);
		}
	}

	// �����Service��������Ϣ��Handler
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constant.MSG_READ: {
				byte[] readBuf = (byte[]) msg.obj;
				// ����Ҫ���͵���Ϣ���ַ���
				String readMessage = new String(readBuf, 0, msg.arg1);
				showLongMessage(connectedNameStr + ":  " + readMessage);
				setDataToPort(readMessage);// �������ݸ�����
				break;
			}
			case Constant.MSG_DEVICE_NAME:
				// ��ȡ�����ӵ��豸���ƣ���������ʾ��Ϣ
				connectedNameStr = msg.getData()
						.getString(Constant.DEVICE_NAME);
				showShortMessage("�����ӵ� " + connectedNameStr);
				break;
			case Constant.MSG_INIT_CHAT:
				initChat();
				break;
			case Constant.MSG_READ_PORT_DATA: {
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				protBufferData = readMessage;
				showLongMessage("�ӵ����ڵ����ݣ� " + readMessage);
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
			// ����豸�б�Activity����һ�����ӵ��豸
			if (resultCode == Activity.RESULT_OK) {
				// ��ȡ�豸��MAC��ַ
				String address = data.getExtras().getString(
						MyDeviceListActivity.EXTRA_DEVICE_ADDR);
				// ��ȡBLuetoothDevice����
				BluetoothDevice device = btAdapter.getRemoteDevice(address);
				myService.connect(device);// ���Ӹ��豸
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

	// ����@{
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
		closeRead();// ����
		closePort();// ����

		if (myService != null) {// ֹͣService
			myService.stop();
		}
	}

}