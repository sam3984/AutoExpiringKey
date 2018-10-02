package com.autoexpiringkey.AutoExpiringStore;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class AutoExpiringMapImpl<K, V> implements AutoExpiringMap<K, V> {
	
	//ConcurrentHash Map to hold actual key value pair
	private final Map<K, V> internalMap;
	
	//WeakHashMap to hold AutoExpiringKey objects
	private final Map<K, AutoExpiringKey<K>> expiringKeyMap;
	
	private final long maxLifeTime;
	private final int cacheSize;
	
	private static AutoExpiringMapImpl expiringCache;
	
	//DelayedQueue to hold the keys until maxLifeTime is expired.
	private final DelayQueue<AutoExpiringKey> delayQueue = new DelayQueue<AutoExpiringKey>();
	
	//Singleton implementation of AutoExpiringMap
	public static synchronized AutoExpiringMapImpl getInstance(int initialCapacity, long maxLifeTime){
		
		if (expiringCache == null)
			expiringCache = new AutoExpiringMapImpl(initialCapacity, maxLifeTime);
				
		return expiringCache;
	}
	
	/*private AutoExpiringMapImpl(long maxLifeTime) {
		internalMap = new ConcurrentHashMap();
		expiringKeyMap = new WeakHashMap<K, AutoExpiringKey<K>>();
		this.maxLifeTime=maxLifeTime;
	}
	
	public AutoExpiringMapImpl() {
		internalMap = new ConcurrentHashMap();
		expiringKeyMap = new WeakHashMap<K, AutoExpiringKey<K>>();
		this.maxLifeTime=Long.MAX_VALUE;
	}*/
	
	private AutoExpiringMapImpl(int initialCapacity, long maxLifeTime) {
		internalMap = new ConcurrentHashMap(initialCapacity);
		expiringKeyMap = new WeakHashMap<K, AutoExpiringKey<K>>(initialCapacity);
		this.maxLifeTime=maxLifeTime;
		this.cacheSize = initialCapacity;
	}
	
	public void clear() {
		internalMap.clear();
	}

	public boolean containsKey(Object key) {
		cleanUp();
		return internalMap.containsKey(key);
	}

	public boolean containsValue(Object value) {
		cleanUp();
		return internalMap.containsValue(value);
	}
	
	public V get(Object key) {
		cleanUp();
		renewKey((K) key);
		return internalMap.get(key);
	}

	public boolean isEmpty() {
		cleanUp();
		return internalMap.isEmpty();
	}

	public Set<K> keySet() {
		cleanUp();
		return internalMap.keySet();
	}

	public V put(K key, V value) {
		return this.put(key, value, maxLifeTime);
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
		
	}

	public V remove(Object key) {
		V removedKeyValue = internalMap.remove(key);
		expireKey(expiringKeyMap.remove(key));
		return removedKeyValue;
	}

	public int size() {
		cleanUp();
		return internalMap.size();
	}

	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	public V put(K key, V value, long maxLifeTime) {
		
		//Remove all the expired keys
		cleanUp();
		
		//Add the new key to the delayedQueue
		AutoExpiringKey<K> delayedKey = new AutoExpiringKey<K>(key, maxLifeTime);
		
		//Add the DelayedKey to WeakHashMap so that it is garbage collected as soon as it is expired.
		AutoExpiringKey oldKey = expiringKeyMap.put(key, delayedKey);
		if( oldKey!= null )	{
			expireKey(oldKey);
			expiringKeyMap.put(key, delayedKey);
		}
		
		delayQueue.offer(delayedKey);
		return internalMap.put(key, value);
	}


	private void expireKey(AutoExpiringKey delayedKey) {
		if(delayedKey != null) {
			delayedKey.expire();
			cleanUp();
		}
	}

	private void cleanUp() {
		AutoExpiringKey<K> delayedKey = delayQueue.poll();
		while(delayedKey != null) {
			internalMap.remove(delayedKey.getKey());
			expiringKeyMap.remove(delayedKey.getKey());
			delayedKey=delayQueue.poll();
		}

	}

	public boolean renewKey(K key) {
		AutoExpiringKey delayedKey = expiringKeyMap.get(key);
		if(delayedKey != null) {
			delayedKey.renewKey();
			return true;
		}
		
		return false;
	}

	public Set<java.util.Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
	
	private class AutoExpiringKey<K> implements Delayed {

		private long startTime = System.currentTimeMillis();
		private final long maxLifeTimeInMillisecs;
		private final K key;
		
		public AutoExpiringKey(K key, long delayInMillisecs) {
			this.maxLifeTimeInMillisecs = delayInMillisecs;
			this.key = key;
		}
		
		public void renewKey() {
			this.startTime = System.currentTimeMillis();			
		}

		public void expire() {
			this.startTime = Long.MIN_VALUE;
		}

		public K getKey() {
			return key;
		}

		public int compareTo(Delayed that) {
			return Long.compare(this.getDelayInMillesconds(), ((AutoExpiringKey<K>) that).getDelayInMillesconds());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			
			if (this == obj)
				return true;
			
			if (obj == null)
				return false;
			
			if (getClass() != obj.getClass())
				return false;
		
			AutoExpiringKey other = (AutoExpiringKey) obj;
		
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			
			return true;
		}

		public long getDelay(TimeUnit unit) {
			return unit.convert(getDelayInMillesconds(), TimeUnit.MILLISECONDS);
		}
		
		public long getDelayInMillesconds() {
			return ( (startTime + maxLifeTimeInMillisecs) - System.currentTimeMillis());
		}

	}
}
