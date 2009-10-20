/**
 * 
 */
package org.javarosa.formmanager.api;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;

import org.javarosa.core.api.State;
import org.javarosa.formmanager.api.transitions.FormBatchTransportStateTransitions;
import org.javarosa.formmanager.utility.FormSender;
import org.javarosa.formmanager.view.transport.FormTransportViews;
import org.javarosa.formmanager.view.transport.MultiSubmitStatusScreen;
import org.javarosa.j2me.view.J2MEDisplay;

/**
 * @author ctsims
 *
 */
public class FormBatchTransportState implements State<FormBatchTransportStateTransitions>, CommandListener, ItemStateListener {

	MultiSubmitStatusScreen screen;
	
	FormSender sender;
	
	FormBatchTransportStateTransitions transitions;

	
	public FormBatchTransportState(Vector messages) {
		FormTransportViews views = new FormTransportViews(this, this);
		sender = new FormSender(views,messages);
		sender.setMultiple(true);
		screen = views.getMultiSubmitStatusScreen();
	}

	public void enter(FormBatchTransportStateTransitions transitions) {
		this.transitions = transitions;
	}

	public void start() {
		sender.setObserver(screen);
		sender.sendData();
		J2MEDisplay.setView(screen);
	}

	public void commandAction(Command c, Displayable arg1) {
		//It's pretty atrocious, but I don't have time to completely rewrite this right now. 
		//Any exit from the multiscreen is just a bail.
		transitions.done();
	}

	public void itemStateChanged(Item arg0) {
		//It's pretty atrocious, but I don't have time to completely rewrite this right now. 
		//Any exit from the multiscreen is just a bail.
		transitions.done();
	}

}
