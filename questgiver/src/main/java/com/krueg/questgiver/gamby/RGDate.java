package com.krueg.questgiver.gamby;

import java.util.Date;

public class RGDate extends Date {

    private static long now = 0;
    
    private static final long serialVersionUID = -5355488038733989782L;

    public static void setNow(long ms) {
        now = ms;
    }
    
    public RGDate() {
        super();
        if ( now != 0 ) {
            setTime(now);
        }
    }
    
    public RGDate(long ms) {
        super(ms);
    }
    
}
