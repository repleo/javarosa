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

package org.javarosa.formmanager.view.singlequestionscreen.screen;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.formmanager.view.InputSafeTextField;
import org.javarosa.formmanager.view.InvalidInputListener;
import org.javarosa.j2me.view.J2MEDisplay;

import de.enough.polish.ui.Style;
import de.enough.polish.ui.TextField;

public class TextQuestionScreen extends SingleQuestionScreen implements InvalidInputListener {

	protected TextField tf;
	
	private boolean loaded = false;

	public TextQuestionScreen(FormEntryPrompt prompt, String groupName, Style style) {
		super(prompt,groupName,style);
	}

	public void createView() {
		setHint("Type in your answer");
		//#style textBox
		tf = new InputSafeTextField("", "", 200, TextField.ANY, this);
		if (prompt.isRequired())
			tf.setLabel("*" + prompt.getLongText());
		else
			tf.setLabel(prompt.getLongText());
		
		IAnswerData answerData = prompt.getAnswerValue();
		if (answerData != null) {
			tf.setString((String)new StringData().cast(answerData.uncast()).getValue());
		}

		this.append(tf);
		this.addNavigationWidgets();
		if (prompt.getHelpText() != null) {
			setHint(prompt.getHelpText());
		}
		//Don't use the default command infrastructure, since we're duplicating
		//it in the event of internationalized screens (with zoom-out behavior)
		//tf.setDefaultCommand(SingleQuestionScreen.nextItemCommand);
	}

	public IAnswerData getWidgetValue() {
		String s = tf.getString();
		return (s == null || s.equals("") ? null : new StringData(s));
	}

	protected boolean handleKeyPressed(int keyCode, int gameAction) {
		loaded = !super.handleKeyPressed(keyCode,gameAction);
		return !loaded;
	}
	
	protected boolean handleKeyReleased(int keyCode, int gameAction) {
		boolean handled = super.handleKeyReleased(keyCode, gameAction);
		
		//The center key should work due to setting the default command, but
		//that won't always be the case in international builds.
		//Check whether there's a hanging center key event, and fire 
		//next if so.
		if(loaded && !handled && this.isGameActionFire(keyCode, gameAction)) {
			this.callCommandListener(SingleQuestionScreen.nextItemCommand);
			return true;
		}
		loaded = false;
		return handled;
	}

	public void invalidNativeInput(String input) {
		System.out.println("loaded: " + loaded);
		J2MEDisplay.showError(null,Localization.get("formentry.invalid.input", new String[] {input}), null, new CommandListener() {
			private int count = 1;
			public void commandAction(Command arg0, Displayable arg1) {
				if(count < 1) {
					TextQuestionScreen.this.show();
				} else {
					count--;
				}
			}
		});
	}
}