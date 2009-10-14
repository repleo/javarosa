/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.media.image.activity;

import java.io.IOException;
import java.util.Hashtable;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;

import org.javarosa.core.Context;
import org.javarosa.core.JavaRosaServiceProvider;
import org.javarosa.core.api.Constants;
import org.javarosa.core.api.IActivity;
import org.javarosa.core.api.ICommand;
import org.javarosa.core.api.IDisplay;
import org.javarosa.core.api.IShell;
import org.javarosa.core.services.UnavailableServiceException;
import org.javarosa.j2me.view.DisplayViewFactory;
//import org.javarosa.media.audio.service.J2MEAudioCaptureService;
import org.javarosa.media.image.model.FileDataPointer;
//import org.javarosa.media.image.utilities.FileUtility;
import org.javarosa.media.image.view.CameraCanvas;
import org.javarosa.utilities.file.*;
import org.javarosa.utilities.file.services.*;



/**
 * An Activity that represents the capture of a single Image.  This will talk to the
 * native device camera and return the selected image.
 * 
 * @author Cory Zue
 *
 */
public class ImageCaptureActivity implements IActivity, CommandListener
{

	public static final String IMAGE_KEY = "IMAGE_KEY";
	private Context context;
	private IShell shell;
	

	// camera needed variables
	
	private Player mPlayer;
	private VideoControl mVideoControl;
	private Command mBackCommand;
	private Command mCaptureCommand;
	private IDisplay display;
	private byte[] imageData;
	private int width;
	private int height;
	private String fullName;
	
	private IFileService fileService;
	
	public ImageCaptureActivity(IShell shell)
	{
		this.shell = shell;
		display = JavaRosaServiceProvider.instance().getDisplay();
		width = 640;
		height = 480;
		try
		{
			fileService = getFileService();
		}
		catch(UnavailableServiceException ue)
		{
			serviceUnavailable(ue);
		}
	}

	
	public void contextChanged(Context globalContext) {
		// TODO Auto-generated method stub
		
	}

	public void destroy() {
		mPlayer.close();
		mPlayer = null;
		mVideoControl = null;
	}

	public Context getActivityContext() {
		return context;
	}

	public void halt() {
		// no need to modify the default behavior
	}

	public void resume(Context globalContext) {
		// no need to modify the default behavior
	}

	public void setShell(IShell shell) {
		this.shell = shell;
	}

	public void start(Context context) {
		// initialize GUI
		// take a pointer to the context and shell
		this.context = context;
		showCamera();
		try
		{
			fileService = getFileService();
		}
		catch(UnavailableServiceException ue)
		{
			serviceUnavailable(ue);
		}
	}
	
	public void setResolution(int width, int height) {
		this.width = width;
		this.height = height;
		
	}
	
	
	/**
	 * takes the selected image return it (and control) to the shell
	 * Other images are deleted?
	 */
	private void doFinish() {
		Hashtable args = buildReturnArgs();
		shell.returnFromActivity(this, Constants.ACTIVITY_COMPLETE, args);
	}

	private void doError() {
		shell.returnFromActivity(this, Constants.ACTIVITY_ERROR, null);
	}

	private Hashtable buildReturnArgs() {
		// stick the picture in here. 
		Hashtable table = new Hashtable();
		FileDataPointer p = new FileDataPointer(fullName);
		//BasicDataPointer p = new BasicDataPointer("Image", imageData);
		table.put(IMAGE_KEY, p);
		return table;
	}
	
	private void showCamera() {
		try {
			mPlayer = Manager.createPlayer("capture://video");
			mPlayer.realize();

			mVideoControl = (VideoControl) mPlayer.getControl("VideoControl");

			//	Command mExitCommand = new Command("Exit", Command.EXIT, 0);
			//	Command mCameraCommand = new Command("Camera", Command.SCREEN, 0);
			mBackCommand = new Command("Back", Command.BACK, 0);
			mCaptureCommand = new Command("Capture", Command.SCREEN, 0);

			Canvas canvas = new CameraCanvas(null, mVideoControl);
			canvas.addCommand(mBackCommand);
			canvas.addCommand(mCaptureCommand);
			canvas.setCommandListener(this);
			
			display.setView(DisplayViewFactory.createView(canvas));
			mPlayer.start();
		} catch (IOException ioe) {
			handleException(ioe);
		} catch (MediaException me) {
			handleException(me);
		}
	}

	private void handleException(Exception e) {
//		Alert a = new Alert(e.toString(), e.toString(), null, null);
//		a.setTimeout(Alert.FOREVER);
//		JavaRosaServiceProvider.instance().getDisplay().setCurrent(a);
//		throw new RuntimeException(e.getMessage());
		System.out.println(e.getMessage());
		e.printStackTrace();
		String toLog = e.getMessage();
		toLog += e.toString();
		try
		{
			saveFile("log" + System.currentTimeMillis() + ".txt", toLog.getBytes());
		}
		catch(FileException fe)
		{
			System.err.println("The was an error saving the file.");
			fe.printStackTrace();
		}
		
		doError();
	}

	
	public void commandAction(Command cmd, Displayable display) {
		if (cmd.equals(this.mBackCommand)) {
			goBack();
		}
		else if (cmd.equals(this.mCaptureCommand)) {
			doCapture();
			//doCaptureLoop();
		}
	}

	private void goBack() {
		this.shell.returnFromActivity(this, Constants.ACTIVITY_CANCEL, null);
		
	}

	private void doCapture() {
		try {
			// Get the image.
			imageData = mVideoControl.getSnapshot("encoding=jpeg&quality=100&width=" + width + "&height=" + height);
			//image = Image.createImage(jpg, 0, jpg.length);
			// Save to file no longer
			String fileName = "test" + System.currentTimeMillis();
			fullName = saveFile(fileName + ".jpg", imageData);
			doFinish();
			
		}
		catch(FileException fe)
		{
			System.err.println("The was an error saving the file.");
			fe.printStackTrace();
		}
		catch(Exception me) 
		{
			handleException(me);
		}
	}
	
	
	/**
	 * This method was used in memory profiling to loop take images at different resolutions until it fails
	
	private void doCaptureLoop() {
		byte[] jpg;
		// add a loop to do this a lot and write them to individual files so we know when we fail
		int width = 640;
		int height = 480;
		int failures = 0;
		String text = "";
		while (failures < 3 && width < 3000) {
		try {
			text += width + "x" + height + ": ";
			// Get the image.
			//jpg = mVideoControl.getSnapshot("encoding=jpeg&quality="+ quality);
			jpg = mVideoControl.getSnapshot("encoding=jpeg&quality=100&width=" + width + "&height=" + height);
			String fileName = "test" + System.currentTimeMillis();
			boolean saved = saveFile(fileName + ".jpg", jpg) == "";
			if (saved) {
				text += "Success!";
				
			}
			//jpg = mVideoControl.getSnapshot("encoding=jpeg&quality=100&width=2048&height=1536");
			//jpg = mVideoControl.getSnapshot("encoding=jpeg&quality=100&width=1280&height=960");
		} 
		catch(FileException fe)
		{
			System.err.println("The was an error saving the file.");
			fe.printStackTrace();
		}
		catch (MediaException me) 
		{
			handleException(me);
			failures++;
			jpg = null;
			text += "Fail!";
		}
		text += "\n";
			width += 80;
			height += 60;
		}
		try
		{
			saveFile("photo_log" + System.currentTimeMillis() + ".txt", text.getBytes());
		}
		catch(FileException fe)
		{
			System.err.println("The was an error saving the file.");
			fe.printStackTrace();
		}
	}
	
	 */
	private String saveFile(String filename, byte[] image) throws FileException 
	{
		String rootName = fileService.getDefaultRoot();
		String restorepath = "file:///" + rootName + "JRImages";				
		fileService.createDirectory(restorepath);
		String fullName = restorepath + "/" + filename;
		if (fileService.createFile(fullName, image)) {
			System.out.println("Image saved.");	
			return fullName;	
		} else {
			return "";
		}
		
	}
	/* (non-Javadoc)
	 * @see org.javarosa.core.api.IActivity#annotateCommand(org.javarosa.core.api.ICommand)
	 */
	public void annotateCommand(ICommand command) 
	{
		throw new RuntimeException("The Activity Class " + this.getClass().getName() + " Does Not Yet Implement the annotateCommand Interface Method. Please Implement It.");
	}
	
	private IFileService getFileService() throws UnavailableServiceException
	{
		//#if app.usefileconnections
		//# JavaRosaServiceProvider.instance().registerService(new J2MEFileService());
		//# IFileService service = (J2MEFileService)JavaRosaServiceProvider.instance().getService(J2MEFileService.serviceName);
		//# return service;
		//#else
		throw new UnavailableServiceException("Unavailable service: " +  J2MEFileService.serviceName);
		//#endif
	}
	
	private void serviceUnavailable(Exception e)
	{
		System.err.println("The File Service is unavailable.\n QUITTING!");			
		System.err.println(e.getMessage());
	}
}
