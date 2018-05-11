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
package org.olat.modules.curriculum.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.modules.curriculum.CurriculumElement;
import org.olat.modules.curriculum.CurriculumElementRef;

/**
 * 
 * Initial date: 11 mai 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CurriculumTreeModel extends GenericTreeModel {

	private static final long serialVersionUID = 2911319509933144413L;

	public static final String LEVEL_PREFIX = "cur-el-lev-";

	public CurriculumTreeModel() {
		GenericTreeNode root = new GenericTreeNode();
		root.setTitle("ROOT");
		setRootNode(root);
	}
	
	public void loadTreeModel(List<CurriculumElement> elements) {
		Map<Long,GenericTreeNode> fieldKeyToNode = new HashMap<>();
		for(CurriculumElement element:elements) {
			Long key = element.getKey();
			GenericTreeNode node = fieldKeyToNode.computeIfAbsent(key, k -> {
				GenericTreeNode newNode = new GenericTreeNode(nodeKey(element));
				newNode.setTitle(element.getDisplayName());
				newNode.setIconCssClass("o_icon_curriculum_element");
				newNode.setUserObject(element);
				return newNode;
			});

			CurriculumElement parentElement = element.getParent();
			if(parentElement == null) {
				//this is a root
				getRootNode().addChild(node);
			} else {
				Long parentKey = parentElement.getKey();
				GenericTreeNode parentNode = fieldKeyToNode.computeIfAbsent(parentKey, k -> {
					GenericTreeNode newNode = new GenericTreeNode(nodeKey(parentElement));
					newNode.setTitle(parentElement.getDisplayName());
					newNode.setIconCssClass("o_icon_curriculum_element");
					newNode.setUserObject(parentElement);
					return newNode;
				});
				
				if(parentNode == null) {
					fieldKeyToNode.put(parentKey, parentNode);
				} else {
					parentNode.addChild(node);
				}
			}
		}
	}
	
	public static final String nodeKey(CurriculumElementRef element) {
		return LEVEL_PREFIX + element.getKey();
	}
}
