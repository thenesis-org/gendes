package org.thenesis.gendes.awt;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessControlException;

public class HttpFileDownloader {
	private String urlString;
	private URL url;
	private HttpURLConnection connection;
	private InputStream inputStream;
	
	private int fileLength;
	private byte fileData[];
	
	private volatile boolean stopFlag;
	private volatile int progress;
	private DownloadThread downloadThread;
	private EndOfDownloadListener endOfDownloadListener;
	
	static public interface EndOfDownloadListener {
		void onEndOfDownload(byte data[], int length, int lengthReceived, boolean stopped);
	}
	
	public synchronized void start(String name, EndOfDownloadListener listener) {
		urlString=name;
		endOfDownloadListener=listener;
		downloadThread=new DownloadThread();
		downloadThread.start();
	}
	
	public synchronized boolean isStarted() { return downloadThread!=null; }
	
	public synchronized void stop() {
		if (downloadThread!=null) {
    		stopFlag=true;
    		downloadThread.interrupt();
    		downloadThread=null;
		}
	}
	
	public synchronized int getProgress() { return progress; }
	
	private void download() {
		int offset=0;
		
		try {
    		url=new URL(urlString);
    		connection=(HttpURLConnection)url.openConnection();
    		fileLength=connection.getContentLength();
    		stopFlag=false; progress=0;
    		if (fileLength>0) {
	    		fileData=new byte[fileLength];
	    		inputStream=connection.getInputStream();    		
	    		while (offset<fileLength && !stopFlag) {
	    			int step=fileLength-offset;
	    			if (step>65536) step=65536;
	    			int n=inputStream.read(fileData, offset, step);
	    			offset+=n;
	    			progress=offset*100/fileLength;
	    		}
    		}
    		synchronized (this) { 	        		
    			endOfDownloadListener.onEndOfDownload(fileData, fileLength, offset, stopFlag);
    		}
		} catch (IOException e) {
    		synchronized (this) { 	        		
    			endOfDownloadListener.onEndOfDownload(null, 0, 0, false);
    		}
		} catch (AccessControlException e) {
    		synchronized (this) { 	        		
    			endOfDownloadListener.onEndOfDownload(null, 0, 0, false);
    		}			
		}

		stopFlag=false; progress=0;
		fileData=null; fileLength=0;
		if (inputStream!=null) { try { inputStream.close(); } catch (IOException e) {} inputStream=null; }
		if (connection!=null) { connection.disconnect(); connection=null; }
		url=null;
		urlString=null;
	}
	
	private class DownloadThread extends Thread {
    	public void run() { download(); }
	}
}
