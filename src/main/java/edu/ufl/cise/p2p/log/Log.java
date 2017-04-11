package edu.ufl.cise.p2p.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

/*
 * Log will be used by the torrent program to write events to a file. 
 */
public class Log 
{
	/******************* Class Attributes *******************/
	public final String path;
	private Timestamp time; 
	private File file;
	private FileWriter fw;
	private BufferedWriter bw;
	
	/******************* Class Methods 
	 * @throws IOException *******************/
	public Log(String path) throws IOException
	{
		this.path = path;
		Date date = new Date();
		this.time = new Timestamp(date.getTime());
		
		/* Create the file and overwrite existing file */
		file = new File(path);
		if(file.exists()) file.delete();
		file.createNewFile();
		
		/* Create the streams */
		try 
		{
			fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
		} /* remove try */
		catch (IOException e) 
		{
			// TODO nothing?
		} /* end catch */
		
	} /* end constructor */
	
	/**
	 * Thread safe method that allows the Log to write to the log.
	 * @param content	the content being written to the log
	 */
	public final synchronized void writeToFile(String content)
	{
		try 
		{
			bw.write(content);
			bw.flush();
		} /* end try */ 
		catch (IOException e) 
		{
			// TODO Do nothing?
		} /* end catch */
		
	} /* end writeToFile */
	
	/**
	 * Closes the streams used to write to the file.
	 */
	public final synchronized void close()
	{
		try 
		{
			bw.close();
			fw.close();
		} /* end try */ 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /* end catch */
		
	} /* end close method */
	
	public final synchronized String getTime()
	{
		Date date = new Date();
		this.time.setTime(date.getTime());
		return time.toString();
	} /* end getTime method */
	public static void logTCPConnection(String localPeerId, String remotePeerId)
	{
		System.out.println("Connection created between :" + localPeerId
				+ "\tand:" + remotePeerId);
	} /* end logTCPConnection method */

	public static void logUnchoking(String peerId) {
		// TODO Auto-generated method stub
		
	}

	public static void logReceivedInterested(String peerId) {
		// TODO Auto-generated method stub
		
	}

	public static void logReceivedNotInterested(String peerId) {
		// TODO Auto-generated method stub
		
	}

	public static void logReceivedHave(String string, int index) {
		// TODO Auto-generated method stub
		
	}

	public static void logChoking(String peerId) {
		// TODO Auto-generated method stub
		
	}
	
} /* end Log class */


