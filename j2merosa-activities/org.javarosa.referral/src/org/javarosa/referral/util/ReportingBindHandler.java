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

package org.javarosa.referral.util;

import java.util.Vector;

import org.javarosa.core.JavaRosaServiceProvider;
import org.javarosa.core.model.DataBinding;
import org.javarosa.core.model.FormDef;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.referral.model.ReferralCondition;
import org.javarosa.referral.model.Referrals;
import org.javarosa.referral.storage.ReferralRMSUtility;
import org.javarosa.xform.util.IXFormBindHandler;
import org.kxml2.kdom.Element;

public class ReportingBindHandler implements IXFormBindHandler {

	/** ReferralCondition */
	Vector referrals = new Vector();
	
	/* (non-Javadoc)
	 * @see org.javarosa.xform.util.IXFormBindHandler#handle(org.kxml2.kdom.Element, org.javarosa.core.model.DataBinding)
	 */
	public void handle(Element bindElement, DataBinding bind) {
		String referralValue = bindElement.getAttributeValue("", "referralvalue");
		String referralText = bindElement.getAttributeValue("", "referraltext");
	
		if(referralValue != null) {
			if(referralText != null) {
				if (referralText.startsWith("jr:itext('") && referralText.endsWith("')")) {
					//String textRef = referralText.substring("jr:itext('".length(), referralText.indexOf("')"));				
					//is this incomplete?
				}
			}
			ReferralCondition newCondition = new ReferralCondition(referralValue, referralText, (XPathReference)bind.getReference());
			referrals.addElement(newCondition);
		}
	}
	
	public Vector getReferralConditions() {
		return referrals;
	}
	
	public void clearConditions() {
		referrals = new Vector();
	}
	
	public void init() {
		clearConditions();
	}
	
	public void postProcess(FormDef formDef) {
		ReferralRMSUtility referralRms =(ReferralRMSUtility)JavaRosaServiceProvider.instance().getStorageManager().getRMSStorageProvider().getUtility(ReferralRMSUtility.getUtilityName());
		Referrals referrals = new Referrals(formDef.getName(), this.getReferralConditions());
		referralRms.writeToRMS(referrals);
	}
}
