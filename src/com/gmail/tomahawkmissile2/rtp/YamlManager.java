package com.gmail.tomahawkmissile2.rtp;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.YamlConfiguration;

public class YamlManager {

	private File f;
	private YamlConfiguration y;
	
	public YamlManager(File f) {
		y=YamlConfiguration.loadConfiguration(f);
		this.f=f;
	}
	public void writeYaml(String path,Object value) {
		y.set(path, value);
		try {
			y.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Object readYaml(String path) {
		return y.get(path);
	}
	public void createSection(String path) {
		y.createSection(path);
	}
}
