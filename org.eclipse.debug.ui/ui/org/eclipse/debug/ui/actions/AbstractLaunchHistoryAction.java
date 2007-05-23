/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

 
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.ILaunchHistoryChangedListener;
import org.eclipse.debug.internal.ui.ILaunchLabelChangedListener;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

import com.ibm.icu.text.MessageFormat;

/**
 * Abstract implementation of an action that displays a drop-down launch
 * history for a specific launch group.
 * 
 * @see LaunchingResourceManager
 * @see ILaunchLabelChangedListener
 * 
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 2.1
 */
public abstract class AbstractLaunchHistoryAction implements IWorkbenchWindowPulldownDelegate2, ILaunchHistoryChangedListener {

	/**
	 * The menu created by this action
	 */
	private Menu fMenu;
		
	/**
	 * The action used to render this delegate.
	 */
	private IAction fAction;
	
	/**
	 * The associated <code>ILaunchGroup</code>
	 * @since 3.3
	 */
	private ILaunchGroup fLaunchGroup = null;
	
	/**
	 * Indicates whether the launch history has changed and
	 * the sub menu needs to be recreated.
	 */
	protected boolean fRecreateMenu = false;
	
	/**
	 * Constructs a launch history action.
	 * 
	 * @param launchGroupIdentifier unique identifier of the launch group
	 * extension that this action displays a launch history for.
	 */
	public AbstractLaunchHistoryAction(String launchGroupIdentifier) {
		fLaunchGroup = getLaunchConfigurationManager().getLaunchGroup(launchGroupIdentifier);
	}
	
	/**
	 * A listener to be notified of launch label updates
	 * @since 3.3
	 */
	private ILaunchLabelChangedListener fLabelListener = new ILaunchLabelChangedListener() {
		public ILaunchGroup getLaunchGroup() {
			return fLaunchGroup;
		}
		public void labelChanged() {
			updateTooltip();
		}
	};
	
	/**
	 * Sets the action used to render this delegate.
	 * 
	 * @param action the action used to render this delegate
	 */
	private void setAction(IAction action) {
		fAction = action;
	}

	/**
	 * Returns the action used to render this delegate.
	 * 
	 * @return the action used to render this delegate
	 */
	protected IAction getAction() {
		return fAction;
	}
	
	/**
	 * Adds the given action to the specified menu with an accelerator specified
	 * by the given number.
	 * 
	 * @param menu the menu to add the action to
	 * @param action the action to add
	 * @param accelerator the number that should appear as an accelerator
	 */
	protected void addToMenu(Menu menu, IAction action, int accelerator) {
		StringBuffer label= new StringBuffer();
		if (accelerator >= 0 && accelerator < 10) {
			//add the numerical accelerator
			label.append('&');
			label.append(accelerator);
			label.append(' ');
		}
		label.append(action.getText());
		action.setText(label.toString());
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	/**
	 * Initialize this action so that it can dynamically set its tool-tip.  Also set the enabled state
	 * of the underlying action based on whether there are any registered launch configuration types that 
	 * understand how to launch in the mode of this action.
	 */
	private void initialize(IAction action) {
		getLaunchConfigurationManager().addLaunchHistoryListener(this);
		setAction(action);
		updateTooltip(); 		
		action.setEnabled(existsConfigTypesForMode());	
	}
	
	/**
	 * Return whether there are any registered launch configuration types for
	 * the mode of this action.
	 * 
	 * @return whether there are any registered launch configuration types for
	 * the mode of this action
	 */
	private boolean existsConfigTypesForMode() {
		ILaunchConfigurationType[] configTypes = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationTypes();
		for (int i = 0; i < configTypes.length; i++) {
			if (configTypes[i].supportsMode(getMode())) {
				return true;
			}
		}		
		return false;
	}
	
	/**
	 * Updates this action's tool-tip. The tooltip is based on user preference settings
	 * for launching - either the previous launch, or based on the selection and which
	 * configuration will be launched.
	 * <p>
	 * Subclasses may override as required.
	 * </p>
	 */
	protected void updateTooltip() {
		getAction().setToolTipText(getToolTip());
	}
	
	/**
	 * Returns the tooltip specific to a configuration.
	 * 
	 * @param configuration a <code>ILauncConfiguration</code>
	 * @return the string for the tool tip
	 */
	protected String getToolTip(ILaunchConfiguration configuration) {
		String launchName= configuration.getName();
		String mode= getMode();
		String label;
		if (mode.equals(ILaunchManager.RUN_MODE)) {
			label= ActionMessages.AbstractLaunchHistoryAction_1; 
		} else if (mode.equals(ILaunchManager.DEBUG_MODE)){
			label= ActionMessages.AbstractLaunchHistoryAction_2; 
		} else if (mode.equals(ILaunchManager.PROFILE_MODE)){
			label= ActionMessages.AbstractLaunchHistoryAction_3; 
		} else {
			label= ActionMessages.AbstractLaunchHistoryAction_4; 
		}
		return MessageFormat.format(ActionMessages.AbstractLaunchHistoryAction_0, new String[] {label, launchName}); 
	}
	
	/**
	 * Returns this action's tooltip. The tooltip is retrieved from the launch resource manager
	 * which builds tool tips asynchronously for context launching support.
	 * 
	 * @return the string for the tool tip
	 */
	private String getToolTip() {
		String launchName = getLaunchingResourceManager().getLaunchLabel(fLaunchGroup);
		if(launchName == null) {
			return DebugUIPlugin.removeAccelerators(internalGetHistory().getLaunchGroup().getLabel());
		}
		String label = null;
		String mode = getMode();
		if (mode.equals(ILaunchManager.RUN_MODE)) {
			label = ActionMessages.AbstractLaunchHistoryAction_1; 
		} else if (mode.equals(ILaunchManager.DEBUG_MODE)){
			label = ActionMessages.AbstractLaunchHistoryAction_2; 
		} else if (mode.equals(ILaunchManager.PROFILE_MODE)){
			label = ActionMessages.AbstractLaunchHistoryAction_3; 
		} else {
			label = ActionMessages.AbstractLaunchHistoryAction_4; 
		}
		if("".equals(launchName)) { //$NON-NLS-1$
			return MessageFormat.format(ActionMessages.AbstractLaunchHistoryAction_5, new String[] {label});
		}
		else {
			return MessageFormat.format(ActionMessages.AbstractLaunchHistoryAction_0, new String[] {label, launchName});
		}
	}

	/**
	 * @see ILaunchHistoryChangedListener#launchHistoryChanged()
	 */
	public void launchHistoryChanged() {
		fRecreateMenu = true;
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		setMenu(null);
		getLaunchConfigurationManager().removeLaunchHistoryListener(this);
		getLaunchingResourceManager().removeLaunchLabelChangedListener(fLabelListener);
	}
	
	/**
	 * Return the last launch in this action's launch history.
	 * 
	 * @return the most recent configuration that was launched from this
	 *  action's launch history that is not filtered from the menu
	 */
	protected ILaunchConfiguration getLastLaunch() {
		return getLaunchConfigurationManager().getFilteredLastLaunch(getLaunchGroupIdentifier());
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowPulldownDelegate#getMenu(org.eclipse.swt.widgets.Control)
	 */
	public Menu getMenu(Control parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}
	
	/**
	 * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
	 */
	public Menu getMenu(Menu parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}
	
	/**
	 * Creates the menu for the action
	 */
	private void initMenu() {
		// Add listener to re-populate the menu each time
		// it is shown because of dynamic history list
		fMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				if (fRecreateMenu) {
					Menu m = (Menu)e.widget;
					MenuItem[] items = m.getItems();
					for (int i=0; i < items.length; i++) {
						items[i].dispose();
					}
					fillMenu(m);
					fRecreateMenu= false;
				}
			}
		});
	}

	/**
	 * Sets this action's drop-down menu, disposing the previous menu.
	 * 
	 * @param menu the new menu
	 */
	private void setMenu(Menu menu) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = menu;
	}

	/**
	 * Fills the drop-down menu with favorites and launch history
	 * 
	 * @param menu the menu to fill
	 */
	protected void fillMenu(Menu menu) {	
		ILaunchConfiguration[] historyList= getHistory();
		ILaunchConfiguration[] favoriteList = getFavorites();
		
		// Add favorites
		int accelerator = 1;
		for (int i = 0; i < favoriteList.length; i++) {
			ILaunchConfiguration launch= favoriteList[i];
			LaunchAction action= new LaunchAction(launch, getMode());
			addToMenu(menu, action, accelerator);
			accelerator++;
		}		
		
		// Separator between favorites and history
		if (favoriteList.length > 0 && historyList.length > 0) {
			addSeparator(menu);
		}
		
		// Add history launches next
		for (int i = 0; i < historyList.length; i++) {
			ILaunchConfiguration launch= historyList[i];
			LaunchAction action= new LaunchAction(launch, getMode());
			addToMenu(menu, action, accelerator);
			accelerator++;
		}
	}
	
	/**
	 * Adds a separator to the given menu
	 * 
	 * @param menu 
	 */
	protected void addSeparator(Menu menu) {
		new MenuItem(menu, SWT.SEPARATOR);
	}
	
	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// do nothing - this is just a menu
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection){
		if (fAction == null) {
			initialize(action);
		} 
	}
	
	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		if (this instanceof AbstractLaunchToolbarAction) {
			getLaunchingResourceManager().addLaunchLabelUpdateListener(fLabelListener);
		}
	}
	
	/**
	 * Returns the launch history associated with this action's launch group.
	 * 
	 * @return the launch history associated with this action's launch group
	 * @deprecated this method returns a class that is not API and is not intended
	 *  for clients of the debug platform. Instead, use <code>getHistory()</code>,
	 *  <code>getFavorites()</code>, and <code>getLastLaunch()</code>.
	 */
	protected LaunchHistory getLaunchHistory() {
		return getLaunchConfigurationManager().getLaunchHistory(getLaunchGroupIdentifier());
	} 
	
	/**
	 * Returns the launch history associated with this action's launch group.
	 * 
	 * @return the launch history associated with this action's launch group
	 * @since 3.3
	 */
	private LaunchHistory internalGetHistory() {
		return getLaunchConfigurationManager().getLaunchHistory(getLaunchGroupIdentifier());
	}
	
	/**
	 * Returns the launch history associated with this action's launch mode and group in most
	 * recently launched order. Configurations associated with disabled activities are not included
	 * in the list. As well, configurations are filtered based on workspace preference settings
	 * to filter configurations from closed projects, deleted projects, working sets and to filter
	 * specific launch configuration types.
	 *  
	 * @return launch history
	 * @since 3.3
	 */
	protected ILaunchConfiguration[] getHistory() {
		return LaunchConfigurationManager.filterConfigs(internalGetHistory().getHistory());
	}
	
	/**
	 * Returns the launch favorites associated with this action's launch mode and group in user
	 * preference order. Configurations associated with disabled activities are not included
	 * in the list. As well, configurations are filtered based on workspace preference settings
	 * to filter configurations from closed projects, deleted projects, working sets and to filter
	 * specific launch configuration types.
	 * 
	 * @return favorite launch configurations
	 * @since 3.3
	 */
	protected ILaunchConfiguration[] getFavorites() {
		return LaunchConfigurationManager.filterConfigs(internalGetHistory().getFavorites());
	}
		
	/**
	 * Returns the mode (e.g., 'run' or 'debug') of this drop down.
	 * 
	 * @return the mode of this action
	 */
	protected String getMode() {
		return internalGetHistory().getLaunchGroup().getMode();
	}
	
	/**
	 * Returns the launch configuration manager.
	 * 
	 * @return launch configuration manager
	 */
	private LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}
	
	/**
	 * Returns the <code>ContextualLaunchingResourceManager</code>
	 * 
	 * @return <code>ContextualLaunchingResourceManager</code>
	 */
	private LaunchingResourceManager getLaunchingResourceManager() {
		return DebugUIPlugin.getDefault().getLaunchingResourceManager();
	}
	
	/**
	 * Returns the identifier of the launch group this action is associated
	 * with.
	 * 
	 * @return the identifier of the launch group this action is associated
	 * with
	 */
	protected String getLaunchGroupIdentifier() {
		return fLaunchGroup.getIdentifier();
	}
}
