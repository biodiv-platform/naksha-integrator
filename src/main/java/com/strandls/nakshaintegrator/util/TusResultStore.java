package com.strandls.nakshaintegrator.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Singleton;

@Singleton
public class TusResultStore {

	public static class Entry {
		public volatile boolean complete = false;
		public volatile Object result;
		public volatile String error;
	}

	private final Map<String, Entry> store = new ConcurrentHashMap<>();

	public Entry getOrCreate(String uploadUri) {
		return store.computeIfAbsent(uploadUri, k -> new Entry());
	}

	public Entry get(String uploadUri) {
		return store.get(uploadUri);
	}

	public void remove(String uploadUri) {
		store.remove(uploadUri);
	}
}