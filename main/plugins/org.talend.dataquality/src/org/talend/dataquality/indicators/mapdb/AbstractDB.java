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
package org.talend.dataquality.indicators.mapdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.talend.cwm.indicator.DataValidation;

/**
 * created by talend on Aug 4, 2014 Detailled comment
 * 
 */
public abstract class AbstractDB<K> {

    /**
     * the cache size used to create db.
     */
    private static final int CACHE_SIZE = 12 * 1024;

    private DB db = null;

    protected Long limitSize = 0l;

    public static final Object EMPTY = new TupleEmpty();

    // if the name of db is same as "temp" we will think that the db should be delete after close.
    public static final String TEMP_DB_NAME = "temp"; //$NON-NLS-1$

    private File dbFile = null;

    protected Logger log = Logger.getLogger(this.getClass());

    protected void initDefaultDB(String parentFullPathStr, String fileName) {
        dbFile = MapDBManager.createPath(parentFullPathStr, fileName);
        db = MapDBManager.getInstance().getDB(dbFile);
        if (db != null) {
            return;
        }
        DBMaker<?> fileDBMaker = DBMaker.newFileDB(dbFile);
        fileDBMaker = fileDBMaker.mmapFileEnable();
        // for job application the mapDB file should be remove no need to drill down after that
        if (!Platform.isRunning()) {
            fileDBMaker = fileDBMaker.deleteFilesAfterClose();
        }
        db = fileDBMaker.cacheSize(CACHE_SIZE).transactionDisable().closeOnJvmShutdown().make();

        MapDBManager.getInstance().putDB(dbFile, db);

    }

    public void clearDB() {
        for (String catalogName : db.getAll().keySet()) {
            db.delete(catalogName);
        }
        db.getEngine().clearCache();
    }

    protected void initDefaultDB() {
        db = DBMaker.newTempFileDB().sizeLimit(2).mmapFileEnable().cacheSize(1024 * 1024).closeOnJvmShutdown()
                .deleteFilesAfterClose().transactionDisable().make();
    }

    protected void initDBByDBMapParameter(DBMapParameter parameter) {
        DBMaker<?> dbMaker = DBMaker.newTempFileDB().sizeLimit(parameter.getLimitSize()).cacheSize(parameter.getCacheSize())
                .transactionDisable();
        if (!parameter.isMmapFileEnablePartial()) {
            dbMaker = dbMaker.mmapFileEnableIfSupported();
        } else {
            dbMaker = dbMaker.mmapFileEnablePartial();
        }
        if (parameter.isDeleteFilesAfterClose()) {
            dbMaker = dbMaker.deleteFilesAfterClose();
        }
        if (parameter.isAsyncWriteEnable()) {
            dbMaker = dbMaker.asyncWriteEnable();
        }
        if (parameter.isCloseOnJvmShutdown()) {
            dbMaker = dbMaker.closeOnJvmShutdown();
        }
        db = dbMaker.make();
    }

    protected DB getDB() {
        return db;
    }

    /**
     * 
     * Close the db after 5 minute
     */
    public void close() {
        MapDBManager.getInstance().closeDB(dbFile);
    }

    /**
     * Check whether input parameter is null
     * 
     * @param parentFullPathStr
     * @param fileName
     */
    protected boolean checkParameter(String parentFullPathStr, String fileName) {
        if (parentFullPathStr == null || fileName == null) {
            log.error("create DBMap fail, the path of db can not be null"); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    /**
     * 
     * Judge whether the db is closed
     * 
     * @return
     */
    public boolean isClosed() {
        return db.isClosed();
    }

    /**
     * 
     * get subList from fromIndex to toIndex
     * 
     * @param fromIndex
     * @param toIndex
     * @param indexMap
     * @return
     */
    public List<Object[]> subList(long fromIndex, long toIndex, Map<Long, K> indexMap) {
        boolean stratToRecord = false;
        List<Object[]> returnList = new ArrayList<Object[]>();
        K fromKey = indexMap.get(fromIndex);
        K toKey = indexMap.get(toIndex);
        Iterator<K> iterator = null;
        int index = 0;
        if (fromKey == null) {
            iterator = this.iterator();
        } else if (toKey == null) {
            NavigableSet<K> tailSet = tailSet(fromKey, true);
            stratToRecord = true;
            iterator = tailSet.iterator();
        } else {
            NavigableSet<K> tailSet = subSet(fromKey, toKey);
            stratToRecord = true;
            iterator = tailSet.iterator();
        }

        while (iterator.hasNext()) {
            K next = iterator.next();
            if (index == 0 && fromKey == null) {
                indexMap.put(0l, next);
            }
            if (index == fromIndex) {
                stratToRecord = true;
            }
            if (index == toIndex) {
                if (toKey == null) {
                    indexMap.put(toIndex, next);
                }
                break;
            }
            if (stratToRecord == true) {
                returnList.add(new Object[] { next });
            }
            index++;

        }

        return returnList;
    }

    /**
     * 
     * get subList from fromIndex to toIndex
     * 
     * @param fromIndex
     * @param toIndex
     * @param indexMap
     * @return
     */
    public List<Object[]> subList(long fromIndex, long toIndex, Map<Long, K> indexMap, DataValidation dataValiator) {
        boolean stratToRecord = false;
        List<Object[]> returnList = new ArrayList<Object[]>();
        K fromKey = indexMap.get(fromIndex);
        K toKey = indexMap.get(toIndex);
        Iterator<K> iterator = null;
        int index = 0;
        if (fromKey == null) {
            iterator = this.iterator();
        } else if (toKey == null) {
            NavigableSet<K> tailSet = tailSet(fromKey, true);
            iterator = tailSet.iterator();
        } else {
            NavigableSet<K> tailSet = subSet(fromKey, toKey);
            iterator = tailSet.iterator();
        }

        while (iterator.hasNext()) {
            K next = iterator.next();
            if (dataValiator != null && !dataValiator.isValid(next)) {
                continue;
            }
            if (index == 0 && fromKey == null) {
                indexMap.put(0l, next);
            }
            if (index == fromIndex) {
                stratToRecord = true;
                if (fromKey == null) {
                    indexMap.put(fromIndex, next);
                }

            }
            if (index == toIndex) {
                if (toKey == null) {
                    indexMap.put(toIndex, next);
                }
                break;
            }
            if (stratToRecord == true) {
                returnList.add(new Object[] { next });
            }
            index++;

        }

        return returnList;
    }

    /**
     * Getter for limiteSize.
     * 
     * @return the limiteSize
     */
    public Long getLimiteSize() {
        return this.limitSize;
    }

    /**
     * Sets the limiteSize.
     * 
     * @param limiteSize the limiteSize to set
     */
    public void setLimiteSize(Long limiteSize) {
        this.limitSize = limiteSize;
    }

    public abstract int size();

    public abstract NavigableSet<K> tailSet(K fromElement, boolean inclusive);

    public abstract NavigableSet<K> headSet(K toElement, boolean inclusive);

    public abstract NavigableSet<K> subSet(K fromElement, K inctoElementlusive);

    public abstract Iterator<K> iterator();

    public abstract boolean isEmpty();

}
