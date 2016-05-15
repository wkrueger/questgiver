package com.krueg.questgiver.gamby;

import org.joda.time.MutableDateTime;

public class RGMutableDateTime extends MutableDateTime {

	private static final long serialVersionUID = 8225713384095600113L;

	private static long now = 0;
	
	public static void setNow(long ms) {
		now = ms;
	}
	
	public RGMutableDateTime() {
		super();
		if ( now != 0 ) {
			setDate(now);
		}
	}
	
	
	
}
