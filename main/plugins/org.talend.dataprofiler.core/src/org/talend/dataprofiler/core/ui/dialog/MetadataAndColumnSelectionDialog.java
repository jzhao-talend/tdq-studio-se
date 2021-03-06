// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.ui.dialog.provider.DBTablesViewLabelProvider;
import org.talend.dataprofiler.core.ui.wizard.analysis.provider.MatchAnaColumnContentProvider;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNode;
import orgomg.cwm.foundation.softwaredeployment.DataManager;

/**
 * DOC yyin class global comment. Detailled comment
 */
public class MetadataAndColumnSelectionDialog extends ColumnsSelectionDialog {

    /**
     * MetadataAndColumnSelectionDialog constructor: the last parameter:false means no need to:addConnFilterListener, in
     * the super class.
     * 
     * @param parent
     */
    public MetadataAndColumnSelectionDialog(Shell parent, String title, List<IRepositoryNode> checkedRepoNodes, String message) {
        super(null, parent, title, checkedRepoNodes, message, false);
        // set the root of the tree, must use the RepositoryNode type.
        setInput(RepositoryNodeHelper.getRootNode(ERepositoryObjectType.METADATA, true));// ResourceManager.getMetadataFolder());
    }

    // TDQ-8248: to only show the datamanager in the dialog, after just created this datamanager
    public MetadataAndColumnSelectionDialog(Shell parent, String title, DataManager dataManager, String message) {
        super(null, parent, title, new ArrayList<IRepositoryNode>(), message, false);
        // set the root of the tree, must use the RepositoryNode type.
        setInput(RepositoryNodeHelper.recursiveFind(dataManager));// ResourceManager.getMetadataFolder());
    }

    /**
     * when the user select the columns in more than one table. should make the OK status: not ok.
     */
    @Override
    protected void handleTableElementsChecked(RepositoryNode reposNode, Boolean checkedFlag) {
        super.handleTableElementsChecked(reposNode, checkedFlag);
        updateStatusBySelection();
    }

    private void updateStatusBySelection() {
        Status fCurrStatus;
        // the table node all stored in the map as key, so when the key's number >1, means there are more than one
        // table's column selected. then make the ok status disable
        if (super.modelElementCheckedMap.keySet().size() > 1) {
            fCurrStatus = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, PluginConstant.EMPTY_STRING, null);
        } else {
            fCurrStatus = new Status(IStatus.OK, PlatformUI.PLUGIN_ID, IStatus.OK, PluginConstant.EMPTY_STRING, null);
        }
        updateStatus(fCurrStatus);
    }

    @Override
    protected void handleTreeElementsChecked(RepositoryNode repNode, Boolean checkedFlag) {
        super.handleTreeElementsChecked(repNode, checkedFlag);
        updateStatusBySelection();
    }

    // no need to create "select All " buttons
    @Override
    protected Composite createSelectionButtons(Composite composite) {
        Composite buttonComposite = new Composite(composite, SWT.RIGHT);
        return buttonComposite;
    }

    @Override
    protected void initProvider() {
        fLabelProvider = new DBTablesViewLabelProvider();
        // use the contenprovider of MatchAnaColumnContentProvider for match analysis
        fContentProvider = new MatchAnaColumnContentProvider(false);
        sLabelProvider = new DBTablesViewLabelProvider();
        sContentProvider = new ModelElementContentProvider();
    }

}
