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
package org.talend.dq.nodes;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ITDQItemService;
import org.talend.core.model.general.Project;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.dq.helper.UnitTestBuildHelper;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.IRepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode;

/**
 * created by qiongli on 2014-3-5 Detailled comment
 * 
 */
public class AnalysisFolderRepNodeTest {

    /**
     * DOC qiongli Comment method "setUp".
     * 
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        if (GlobalServiceRegister.getDefault().isServiceRegistered(ITDQItemService.class)) {
            ITDQItemService tdqService = (ITDQItemService) GlobalServiceRegister.getDefault().getService(ITDQItemService.class);
            tdqService.createDQStructor();
        }

    }

    /**
     * DOC qiongli Comment method "tearDown".
     * 
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.talend.dq.nodes.AnalysisFolderRepNode#getChildren()}. Testing Analysis's children is
     * empty or not.
     */
    @Test
    public void testGetChildren() {

        Project tProject = ProjectManager.getInstance().getCurrentProject();
        if (tProject != null && tProject.getEmfProject() != null && tProject.getAuthor() != null) {

            IRepositoryViewObject viewObject = UnitTestBuildHelper.buildRepositoryViewObjectSystemFolder(
                    tProject.getEmfProject(), tProject.getAuthor(), ERepositoryObjectType.TDQ_ANALYSIS_ELEMENT);

            RepositoryNode node = new RepositoryNode(viewObject, null, ENodeType.SYSTEM_FOLDER);
            viewObject.setRepositoryNode(node);
            AnalysisFolderRepNode AnalysisFolderRepNode = new AnalysisFolderRepNode(viewObject, null, ENodeType.SYSTEM_FOLDER);
            List<IRepositoryNode> children = AnalysisFolderRepNode.getChildren(false);
            assertTrue(children.isEmpty());
            // after creation analysis,the children should not be empty.
            UnitTestBuildHelper.createRealAnalysis("A", null, false); //$NON-NLS-1$
            children = AnalysisFolderRepNode.getChildren(false);
            assertTrue(children.size() == 1);
        } else {
            fail("Project initialize failed!"); //$NON-NLS-1$
        }

    }

}
