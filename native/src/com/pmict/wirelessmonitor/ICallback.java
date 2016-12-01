package com.pmict.wirelessmonitor;

public interface ICallback<T> {
	void success(T o);
	void error(Exception e);
}
