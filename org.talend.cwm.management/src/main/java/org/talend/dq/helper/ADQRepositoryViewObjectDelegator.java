// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dq.helper;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.cwm.management.api.DqRepositoryViewService;
import org.talend.utils.sugars.ReturnCode;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * 
 * DOC mzhao Abstract class for dq repository object loading,retrieving etc.
 * 
 * @param <T> the type connection,analysis,report etc.
 */
public abstract class ADQRepositoryViewObjectDelegator<T extends ModelElement> {

    private static Logger log = Logger.getLogger(ADQRepositoryViewObjectDelegator.class);

    protected Map<T, IRepositoryViewObject> needSavedElements = new HashMap<T, IRepositoryViewObject>();

    protected void register(T modelElement, IRepositoryViewObject reposViewObj) {
        log.info(modelElement.toString());
        needSavedElements.put(modelElement, reposViewObj);
    }

    public Collection<T> getAllElements() {
        return needSavedElements.keySet();
    }

    public void clear() {
        needSavedElements.clear();
    }

    public void remove(T element) {
        needSavedElements.remove(element);
    }

    /**
     * Save all the data provider.
     */
    public void saveAllElements() {

        Iterator<T> it = needSavedElements.keySet().iterator();
        while (it.hasNext()) {
            IRepositoryViewObject reposViewObj = needSavedElements.get(it.next());
            DqRepositoryViewService.saveOpenDataProvider(reposViewObj, false);
        }
    }

    /**
     * 
     * DOC mzhao Get repository view object by connection.
     * 
     * @param element
     * @return
     */
    public IRepositoryViewObject getRepositoryViewObject(T element) {
        return needSavedElements.get(element);
    }

    /**
     * 
     * DOC qiongli Get repository view object by property.
     * 
     * @param element
     * @return
     */
    public IRepositoryViewObject getReposViewObjByProperty(Property property) {
        Iterator<T> it = needSavedElements.keySet().iterator();
        while (it.hasNext()) {
            IRepositoryViewObject reposViewObj = needSavedElements.get(it.next());
            if (property.getId().equals(reposViewObj.getId())) {
                return reposViewObj;
            }
        }
        return null;
    }

    /**
     * 
     * DOC mzhao Save specific connection.
     * 
     * @param connection
     */
    public ReturnCode saveElement(T element) {
        ReturnCode rc = new ReturnCode();
        rc = DqRepositoryViewService.saveOpenDataProvider(needSavedElements.get(element), false);
        fetchRepositoryViewObjects(Boolean.TRUE);
        return rc;
    }

    /**
     * 
     * DOC mzhao Save specific connection with.
     * 
     * @param connection
     */
    public void saveConnectionWithReloadPackage(T element) {
        DqRepositoryViewService.saveOpenDataProvider(needSavedElements.get(element), true);
        fetchRepositoryViewObjects(Boolean.TRUE);
    }

    /**
     * 
     * DOC mzhao fetch repository view object.
     * 
     * @param reload, true to reload.
     * @return
     * @deprecated
     */
    public List<IRepositoryViewObject> fetchRepositoryViewObjects(boolean reload) {
        if (!reload) {
            // if (types.length > 0) {
            // return filterArrays(types[0]);
            // }
            IRepositoryViewObject[] reposViewObjs = new IRepositoryViewObject[needSavedElements.values().size()];
            return Arrays.asList(needSavedElements.values().toArray(reposViewObjs));
        }
        // Reload
        return fetchRepositoryViewObjectsLower();
    }

    // /**
    // *
    // * DOC mzhao fetch repository view object.
    // *
    // * @param reload, true to reload.
    // * @return
    // */
    // public List<IRepositoryViewObject> fetchRepositoryViewObjectsWithFolder(boolean reload) {
    // // Reload
    // return fetchRepositoryViewObjectsLower();
    // }

    /**
     * 
     * DOC zshen fetch repository view object by folder.
     * 
     * @param reload, true to reload.
     * @return
     */
    // public List<Object> fetchRepositoryViewObjectsByFolder(boolean reload, IFolder folder) {
    // List<IRepositoryViewObject> reposViewObjs = fetchRepositoryViewObjects(reload);
    // List<Object> returnList = new ArrayList<Object>();
    // if (ResourceManager.getMetadataFolder().getFullPath().isPrefixOf(folder.getFullPath())) {
    // IResource[] folderContains = null;
    // try {
    // folderContains = folder.members();
    // } catch (CoreException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // Iterator<IRepositoryViewObject> iter = reposViewObjs.iterator();
    // while (iter.hasNext()) {
    // IRepositoryViewObject repositoryViewObject = iter.next();
    // String reposName = repositoryViewObject.getLabel();
    // for (IResource resource : folderContains) {
    // if (resource.getName().split("_")[0].equals(reposName)) {
    // returnList.add(repositoryViewObject);
    //
    // // } else if (resource.getType() == resource) {
    // //
    // // }
    // }
    // }
    // }
    // return returnList;
    // }

    protected abstract List<IRepositoryViewObject> fetchRepositoryViewObjectsLower();
}
