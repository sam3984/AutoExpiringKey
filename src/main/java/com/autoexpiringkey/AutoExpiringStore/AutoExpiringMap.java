package com.autoexpiringkey.AutoExpiringStore;

import java.util.Map;

public interface AutoExpiringMap<K, V> extends Map<K, V> {
	
	/**
	 * @param key
	 * @param value
	 * @param milliseconds
	 * @return
	 */
	public V put(K key,V value,long milliseconds);
	
	/**
	 * @param key
	 * @return
	 */
	public boolean renewKey(K key);
}
