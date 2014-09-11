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
package org.talend.dataprofiler.core.service;

import net.sourceforge.sqlexplorer.service.ISqlExplorerService;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.talend.commons.bridge.ReponsitoryContextBridge;
import org.talend.core.model.properties.Item;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.manager.DQStructureManager;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.resource.EResourceConstant;

/**
 * DOC qiongli class global comment. Detailled comment
 */
public class SqlExplorerService implements ISqlExplorerService {

    /**
     * DOC qiongli SqlExploerSaveAsService constructor comment.
     */
    public SqlExplorerService() {
    }

    /**
     * create item and property for sql resource.
     */
    public Item createFile(String content, IPath path, String label, String extension) {
        Item item = DQStructureManager.getInstance().createSourceFileItem(content, path, label, extension);
        // Added TDQ-7532, 20130719 yyin: to lock the editor when creating the sql file from "preview table"
        ProxyRepositoryFactory.getInstance().isEditableAndLockIfPossible(item);// ~
        CorePlugin.getDefault().refreshDQView(RepositoryNodeHelper.getLibrariesFolderNode(EResourceConstant.SOURCE_FILES));
        return item;

    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.sqlexplorer.service.ISaveAsService#getRootProject()
     */
    public IProject getRootProject() {
        return ReponsitoryContextBridge.getRootProject();
    }

}
