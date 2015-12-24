package com.meinan.mobile1;

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
	private EditText outEt;// �����еĿؼ�����
	private Button sendBtn, searchBtn;
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

		searchBtn = (Button) findViewById(R.id.button_search);
		searchBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// �����豸�б�Activity�����豸
				Intent serverIntent = new Intent(MainActivity.this,
						MyDeviceListActivity.class);
				startActivityForResult(serverIntent, 1);
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
			case Constant.MSG_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// ����Ҫ���͵���Ϣ���ַ���
				String readMessage = new String(readBuf, 0, msg.arg1);
				Toast.makeText(MainActivity.this,
						connectedNameStr + ":  " + readMessage,
						Toast.LENGTH_LONG).show();
				break;
			case Constant.MSG_DEVICE_NAME:
				// ��ȡ�����ӵ��豸���ƣ���������ʾ��Ϣ
				connectedNameStr = msg.getData()
						.getString(Constant.DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"�����ӵ� " + connectedNameStr, Toast.LENGTH_SHORT).show();
				break;
			case Constant.MSG_INIT_CHAT:
				initChat();
				break;

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

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (myService != null) {// ֹͣService
			myService.stop();
		}
	}

}