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

/**
 * 
 */
package org.javarosa.cases.view;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.List;

import org.javarosa.core.services.locale.Localization;

/**
 * @author Clayton Sims
 * @date Mar 19, 2009 
 *
 */
public class CaseManagementScreen extends List {
	public final static Command BACK = new Command(Localization.get("command.back"), Command.BACK, 0);
	public final static Command SELECT = new Command(Localization.get("command.select"), Command.SCREEN, 0);
	
	public CaseManagementScreen(String title) {
		super(title, List.IMPLICIT);
		this.addCommand(BACK);
		this.addCommand(SELECT);
	}
}
