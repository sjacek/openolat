/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.nodes.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;

/**
 * 
 * Initial date: 22.12.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SelectMembersController extends FormBasicController {

	private MultipleSelectionElement ownerEl, coachEl, participantEl;
	private final List<Member> ownerList, coachList, participantList;
	
	private List<Member> selectedMembers = new ArrayList<>();
	
	public SelectMembersController(UserRequest ureq, WindowControl wControl,
			List<Member> ownerList, List<Member> coachList, List<Member> participantList) {
		super(ureq, wControl, LAYOUT_VERTICAL);
		this.ownerList = ownerList;
		this.coachList = coachList;
		this.participantList = participantList;
		initForm(ureq);
	}
	
	public List<Member> getSelectedMembers() {
		return selectedMembers;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(ownerList != null && ownerList.size() > 0) {
			ownerEl = makeSelection("owners", ownerList, formLayout);
		}
		
		if(coachList != null && coachList.size() > 0) {
			coachEl = makeSelection("coaches", coachList, formLayout);
		}
		
		if(participantList != null && participantList.size() > 0) {
			participantEl = makeSelection("participants", participantList, formLayout);
		}

		FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
		formLayout.add(buttonGroupLayout);
		uifactory.addFormSubmitButton("select", buttonGroupLayout);
		uifactory.addFormCancelButton("cancel", buttonGroupLayout, ureq, getWindowControl());
	}
	
	private MultipleSelectionElement makeSelection(String name, List<Member> members, FormItemContainer formLayout) {
		String[] keys = new String[members.size()];
		String[] values = new String[members.size()];
		for(int i=members.size(); i-->0; ) {
			keys[i] = members.get(i).getKey().toString();
			values[i] = members.get(i).getFullName();
		}
		return uifactory.addCheckboxesVertical(name, name, formLayout, keys, values, 2);
	}
	
	@Override
	protected void doDispose() {
		//
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		selectedMembers.clear();
		selectMembers(ownerEl, ownerList);
		selectMembers(coachEl, coachList);
		selectMembers(participantEl, participantList);
		fireEvent(ureq, Event.DONE_EVENT);
	}
	
	private void selectMembers(MultipleSelectionElement memberEl, List<Member> members) {
		if(memberEl != null && memberEl.isAtLeastSelected(1)) {
			Collection<String> selectedKeys = memberEl.getSelectedKeys();
			for(Member member:members) {
				if(selectedKeys.contains(member.getKey().toString())) {
					selectedMembers.add(member);
				}
			}
		}
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
}
