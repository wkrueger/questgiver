package com.krueg.questgiver.gamby;

public class BoundedValue<T extends Comparable<T>> {

	private T lowerBound;
	private T higherBound;
	private T value;
	
	public BoundedValue(T lower, T higher, T init) {
		lowerBound = lower;
		higherBound = higher;
		set(init);
	}
	
	public T set(T val) {
		if ( val.compareTo(lowerBound) < 0 ) value = lowerBound;
		else if ( val.compareTo(higherBound) > 0 ) value = higherBound;
		else value = val;
		return value;
	}
	
	public T get() {
		return value;
	}
	
}