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
package org.talend.dataprofiler.core.ui.utils;

import java.util.List;

import net.sourceforge.sqlexplorer.dbproduct.Alias;
import net.sourceforge.sqlexplorer.dbproduct.AliasManager;
import net.sourceforge.sqlexplorer.dbproduct.DriverManager;
import net.sourceforge.sqlexplorer.dbproduct.ManagedDriver;
import net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin;
import net.sourceforge.sqlexplorer.plugin.editors.SQLEditor;
import net.sourceforge.sqlexplorer.plugin.editors.SQLEditorInput;
import net.sourceforge.sqlexplorer.sqleditor.actions.ExecSQLAction;
import net.sourceforge.sqlexplorer.util.AliasAndManaDriverHelper;

import org.apache.log4j.Logger;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.talend.commons.bridge.ReponsitoryContextBridge;
import org.talend.core.database.EDatabaseTypeName;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.database.JavaSqlFactory;
import org.talend.core.model.metadata.builder.util.MetadataConnectionUtils;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dq.CWMPlugin;

/**
 * created by xqliu on 2014-9-9 Detailled comment
 * 
 */
public class SqlExplorerUtils {

    private static Logger log = Logger.getLogger(SqlExplorerUtils.class);

    private static SqlExplorerUtils sqlExplorerUtils;

    public static SqlExplorerUtils getDefault() {
        if (sqlExplorerUtils == null) {
            sqlExplorerUtils = new SqlExplorerUtils();
        }
        return sqlExplorerUtils;
    }

    /**
     * DOC Zqin Comment method "runInDQViewer". this method open DQ responsitory view and run the specified query.
     * 
     * @param tdDataProvider
     * @param query
     */
    public void runInDQViewer(Connection tdDataProvider, String query, String editorName) {
        SQLEditor sqlEditor = openInSqlEditor(tdDataProvider, query, editorName);
        if (sqlEditor != null) {
            new ExecSQLAction(sqlEditor).run();
        }
    }

    /**
     * DOC bZhou Comment method "openInSqlEditor".
     * 
     * @param tdDataProvider
     * @param query
     * @param editorName
     * @return the specified sql editor.
     */
    public SQLEditor openInSqlEditor(Connection tdDataProvider, String query, String editorName) {
        String lEditorName = editorName;
        if (lEditorName == null) {
            lEditorName = String.valueOf(SQLExplorerPlugin.getDefault().getEditorSerialNo());
        }

        String dbType = PluginConstant.EMPTY_STRING;
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(tdDataProvider);
        if (dbConn != null) {
            dbType = dbConn.getDatabaseType();
        }
        // MOD qiongli 2013-12-9,TDQ-8442,if the database type is not supported on DQ side,ruturn null.
        List<String> tdqSupportDBType = MetadataConnectionUtils.getTDQSupportDBTemplate();
        String username = JavaSqlFactory.getUsername(tdDataProvider);
        boolean isInvalidUserForMsSql = EDatabaseTypeName.MSSQL.getDisplayName().equalsIgnoreCase(dbType)
                && (username == null || PluginConstant.EMPTY_STRING.equals(username));
        if (isInvalidUserForMsSql || !tdqSupportDBType.contains(dbType)) {
            MessageUI.openWarning(DefaultMessagesImpl.getString("CorePlugin.cantPreview")); //$NON-NLS-1$
            return null;
        }

        SQLExplorerPlugin sqlPlugin = SQLExplorerPlugin.getDefault();
        AliasManager aliasManager = sqlPlugin.getAliasManager();

        Alias alias = aliasManager.getAlias(tdDataProvider.getName());

        if (alias == null) {
            for (Connection dataProvider : CorePlugin.getDefault().getAllDataProviders()) {
                // MOD xqliu 2010-10-13 bug 15756
                // if (dataProvider.getId().equals(tdDataProvider.getId())) {
                if (dataProvider.getName().equals(tdDataProvider.getName())) {
                    // ~ 15756
                    CWMPlugin.getDefault().addConnetionAliasToSQLPlugin(dataProvider);
                    openInSqlEditor(tdDataProvider, query, lEditorName);
                }
            }
        } else {
            try {
                Connection connection = SwitchHelpers.CONNECTION_SWITCH.doSwitch(tdDataProvider);
                if (connection != null) {
                    String userName = JavaSqlFactory.getUsername(connection);

                    String url = JavaSqlFactory.getURL(connection);
                    SQLEditorInput input = new SQLEditorInput("SQL Editor (" + alias.getName() + "." + lEditorName + ").sql"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    net.sourceforge.sqlexplorer.dbproduct.User user;
                    user = alias.getUser(userName);
                    if (PluginConstant.EMPTY_STRING.equals(userName)) {
                        // get the user both the dbtype and username are the same.
                        if (!alias.getUrl().equals(url)) {
                            String password = JavaSqlFactory.getPassword(connection);
                            user = new net.sourceforge.sqlexplorer.dbproduct.User(userName, password);
                            user.setAlias(alias);
                            alias.addUser(user);
                        }
                    } else {
                        if (user == null) {
                            user = alias.getDefaultUser();
                        }
                    }
                    alias.setDefaultUser(user);

                    // set IMetadataConnection into the user, if the db type is hive, should use IMetadataConnection to
                    // create the hive connection
                    DatabaseConnection databaseConnection = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(connection);
                    if (databaseConnection != null) {
                        user.setDatabaseConnection(databaseConnection);
                        // if ManagedDriver class is not Loaded,check if it lack jars then update the realted jar.
                        updateDriverIfClassNotLoad(databaseConnection);
                    }

                    input.setUser(user);
                    IWorkbenchPage page = SQLExplorerPlugin.getDefault().getActivePage();
                    SQLEditor editorPart = (SQLEditor) page.openEditor(input, SQLEditor.class.getName());
                    editorPart.setText(query);
                    return editorPart;
                }
            } catch (PartInitException e) {
                log.error(e, e);
            }
        }

        return null;
    }

    /**
     * if the sqlexplorer driver is unRegisted,load the driver jar by lib manage system.
     * 
     * @param sqlPlugin
     * @param connection
     * @param databaseConnection
     */
    public void updateDriverIfClassNotLoad(DatabaseConnection databaseConnection) {
        SQLExplorerPlugin sqlPlugin = SQLExplorerPlugin.getDefault();
        DriverManager driverManager = sqlPlugin.getDriverModel();
        String driverClassName = JavaSqlFactory.getDriverClass(databaseConnection);
        if (driverClassName != null) {
            String id = AliasAndManaDriverHelper.getInstance().joinManagedDriverId(databaseConnection);
            ManagedDriver manDr = driverManager.getDriver(id);
            if (manDr != null && !manDr.isDriverClassLoaded()) {
                CWMPlugin.getDefault().loadDriverByLibManageSystem(databaseConnection);
            }
        }
    }

    public void initSqlExplorerRootProject() {
        if (SQLExplorerPlugin.getDefault().getRootProject() == null) {
            SQLExplorerPlugin.getDefault().setRootProject(ReponsitoryContextBridge.getRootProject());
        }
    }

}
