package com.gmail.tomahawkmissile2.rtp;

public enum DefaultConfigValue {
	
	WORLD_TO("world","world2"),
	COOLDOWN("cooldown","60000"),
	X_MAX("x.max","2500"),
	Z_MAX("z.max","2500");
	
	private final String path;
	private final String value;
	DefaultConfigValue(String path,String value) {
		this.path=path;
		this.value=value;
	}
	public String getPath() {
		return path;
	}
	public String getValue() {
		return value;
	}
}
