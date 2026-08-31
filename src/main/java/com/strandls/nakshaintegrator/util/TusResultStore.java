package com.strandls.nakshaintegrator.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TusResultStore {

	public static class Entry {
		public volatile boolean complete = false;
		public volatile List<MyUpload> result;
		public volatile String error;
		public final long createdAt = System.currentTimeMillis();
	}

	private final Map<String, Entry> results = new ConcurrentHashMap<>();

	public Entry getOrCreate(String uploadUri) {
		return results.computeIfAbsent(uploadUri, k -> new Entry());
	}

	public Entry get(String uploadUri) {
		return results.get(uploadUri);
	}

	public void remove(String uploadUri) {
		results.remove(uploadUri);
	}

	public void cleanupOlderThan(long maxAgeMs) {
		long cutoff = System.currentTimeMillis() - maxAgeMs;
		results.entrySet().removeIf(e -> e.getValue().createdAt < cutoff);
	}

}
