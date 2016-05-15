package com.krueg.questgiver.Exceptions;

public class RGException extends Exception {
	static final long serialVersionUID = 99L;
	
	public int id;
	
	public RGException ()
    {
    }

	public RGException (String message)
    {
		super (message);
    }

	public RGException (Throwable cause)
    {
		super (cause);
    }

	public RGException (String message, Throwable cause)
    {
		super (message, cause);
    }
	
	public RGException (int id) {
		
	}
}