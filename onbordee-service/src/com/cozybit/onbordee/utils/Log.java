package com.cozybit.onbordee.utils;

public class Log
{
    public static int LEVEL = android.util.Log.DEBUG;

    /**
     * This static method will simple log the callers method name if the logging level is greater than or equal
     * to DEBUG
     * @param tag The log tag
     */
    static public void mark(String tag)
    {
        if (LEVEL <= android.util.Log.DEBUG)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.d(tag, caller);
        }
    }

    static public void d(String tag, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.DEBUG)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.d(tag, caller + " : " + String.format(msgFormat, args));
        }
    }

    static public void d(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.DEBUG)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.d(tag, caller + " : " + String.format(msgFormat, args), t);
        }
    }

    static public void i(String tag, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.INFO)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.i(tag, caller + " : " + String.format(msgFormat, args));
        }
    }

    static public void i(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.INFO)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.i(tag, caller + " : " + String.format(msgFormat, args), t);
        }
    }

    static public void w(String tag, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.WARN)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.w(tag, caller + " : " + String.format(msgFormat, args));
        }
    }

    static public void w(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.WARN)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.w(tag, caller + " : " + String.format(msgFormat, args), t);
        }
    }

    static public void e(String tag, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.ERROR)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.e(tag, caller + " : " + String.format(msgFormat, args));
        }
    }

    static public void e(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.ERROR)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.e(tag, caller + " : " + String.format(msgFormat, args), t);
        }
    }

    static public void v(String tag, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.VERBOSE)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.v(tag, caller + " : " + String.format(msgFormat, args));
        }
    }

    static public void v(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL <= android.util.Log.VERBOSE)
        {
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            android.util.Log.v(tag, caller + " : " + String.format(msgFormat, args), t);
        }
    }
}