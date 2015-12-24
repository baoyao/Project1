package com.meinan.pad1;

//用于管理公共常量的常量类
public class Constant {
	// 由Service中的Handler发送的消息类型
	public static final int MSG_READ = 2;
	public static final int MSG_DEVICE_NAME = 4;
	public static final int MSG_INIT_CHAT = 5;
	public static final int MSG_READ_PORT_DATA = 6;

	// 从Service中的Handler发来的主键名
	public static final String DEVICE_NAME = "device_name";
}
