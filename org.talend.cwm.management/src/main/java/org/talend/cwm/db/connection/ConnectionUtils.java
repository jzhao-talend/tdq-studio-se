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
package org.talend.cwm.db.connection;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.sqlexplorer.dbproduct.ManagedDriver;
import net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin;
import net.sourceforge.sqlexplorer.util.MyURLClassLoader;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.talend.commons.emf.FactoriesUtil;
import org.talend.commons.utils.database.DB2ForZosDataBaseMetadata;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.MDMConnection;
import org.talend.cwm.dburl.SupportDBUrlType;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.management.connection.DatabaseConstant;
import org.talend.cwm.management.i18n.Messages;
import org.talend.dq.CWMPlugin;
import org.talend.dq.PluginConstant;
import org.talend.dq.helper.resourcehelper.PrvResourceFileHelper;
import org.talend.utils.sugars.ReturnCode;
import org.talend.utils.sugars.TypedReturnCode;
import orgomg.cwm.foundation.softwaredeployment.DataProvider;
import orgomg.cwm.foundation.softwaredeployment.ProviderConnection;

/**
 * Utility class for database connection handling.
 */
public final class ConnectionUtils {

    private static Logger log = Logger.getLogger(ConnectionUtils.class);

    // MOD xqliu 2009-02-02 bug 5261
    public static final int LOGIN_TEMEOUT_MILLISECOND = 20000;

    public static final int LOGIN_TIMEOUT_SECOND = 20;

    private static boolean timeout = CWMPlugin.getDefault().getPluginPreferences().getBoolean(PluginConstant.CONNECTION_TIMEOUT);

    // MOD mzhao 2009-06-05 Bug 7571
    private static final Map<String, Driver> DRIVER_CACHE = new HashMap<String, Driver>();

    public static final String DEFAULT_USERNAME = "root";

    public static final String DEFAULT_PASSWORD = "";

    public static boolean isTimeout() {
        return timeout;
    }

    public static void setTimeout(boolean timeout) {
        ConnectionUtils.timeout = timeout;
    }

    /**
     * The query to execute in order to verify the connection.
     */
    // private static final String PING_SELECT = "SELECT 1";
    /**
     * private constructor.
     */
    private ConnectionUtils() {
    }

    /**
     * Method "createConnection".
     * 
     * @param url the database url
     * @param driverClassName the Driver classname
     * @param props properties passed to the driver manager for getting the connection (normally at least a "user" and
     * "password" property should be included)
     * @return the connection
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static java.sql.Connection createConnection(String url, String driverClassName, Properties props) throws SQLException,
            InstantiationException, IllegalAccessException, ClassNotFoundException {
        Driver driver = getClassDriver(driverClassName);
        if (driver != null) {
            DriverManager.registerDriver(driver);
            if (log.isDebugEnabled()) {
                log.debug("SQL driver found and registered: " + driverClassName);
                log.debug("Enumerating all drivers:");
                Enumeration<Driver> drivers = DriverManager.getDrivers();
                while (drivers.hasMoreElements()) {
                    log.debug(drivers.nextElement());
                }
            }
            java.sql.Connection connection = null;
            if (driverClassName.equals("org.hsqldb.jdbcDriver")) { //$NON-NLS-1$getClassDriver 
                // MOD mzhao 2009-04-13, Try to load driver first as there will
                // cause exception: No suitable driver
                // found... if not load.
                try {
                    Class.forName("org.hsqldb.jdbcDriver");
                } catch (ClassNotFoundException e) {
                    log.error(e, e);
                }
                // MOD xqliu 2009-02-02 bug 5261
                if (isTimeout()) {
                    DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECOND);
                }
                connection = DriverManager.getConnection(url, props);
            } else {
                // MOD xqliu 2009-02-02 bug 5261
                connection = createConnectionWithTimeout(driver, url, props);
            }

            return connection;
        }
        return null;

    }

    /**
     * 
     * DOC xqliu Comment method "createConnectionWithTimeout".
     * 
     * @param driver
     * @param url
     * @param props
     * @return
     * @throws SQLException
     */
    public static synchronized java.sql.Connection createConnectionWithTimeout(Driver driver, String url, Properties props)
            throws SQLException {
        java.sql.Connection ret = null;
        if (isTimeout()) {
            ConnectionCreator cc = new ConnectionCreator(driver, url, props);
            new Thread(cc).start();
            long begin = System.currentTimeMillis();
            boolean isTimeout = false;
            boolean isOK = false;
            boolean isException = false;
            while (true) {
                if (cc.getConnection() != null) {
                    isOK = true;
                    ret = cc.getConnection();
                    break;
                }
                if (cc.getExecption() != null) {
                    isException = true;
                    break;
                }
                if (System.currentTimeMillis() - begin > LOGIN_TEMEOUT_MILLISECOND) {
                    isTimeout = true;
                    break;
                }
            }
            if (isTimeout) {
                cc = null;
                throw new SQLException(Messages.getString("ConnectionUtils.ConnectionTimeout")); //$NON-NLS-1$
            }
            if (isException) {
                SQLException e = cc.getExecption();
                cc = null;
                throw e;
            }
            if (isOK) {
                ret = cc.getConnection();
                cc = null;
            }
        } else {
            ret = driver.connect(url, props);
        }
        return ret;
    }

    /**
     * DOC qzhang Comment method "getClassDriver".
     * 
     * @param driverClassName
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public static Driver getClassDriver(String driverClassName) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        // MOD mzhao 2009-06-05,Bug 7571 Get driver from catch first, if not
        // exist then get a new instance.
        Driver driver = DRIVER_CACHE.get(driverClassName);
        if (driver != null) {
            return driver;
        }

        SQLExplorerPlugin sqlExplorerPlugin = SQLExplorerPlugin.getDefault();
        if (sqlExplorerPlugin != null) {
            net.sourceforge.sqlexplorer.dbproduct.DriverManager driverModel = sqlExplorerPlugin.getDriverModel();
            try {
                Collection<ManagedDriver> drivers = driverModel.getDrivers();
                for (ManagedDriver managedDriver : drivers) {
                    LinkedList<String> jars = managedDriver.getJars();
                    List<URL> urls = new ArrayList<URL>();
                    for (int i = 0; i < jars.size(); i++) {
                        File file = new File(jars.get(i));
                        if (file.exists()) {
                            urls.add(file.toURL());
                        }
                    }
                    if (!urls.isEmpty()) {
                        try {
                            MyURLClassLoader cl;
                            cl = new MyURLClassLoader(urls.toArray(new URL[0]));
                            Class clazz = cl.findClass(driverClassName);
                            if (clazz != null) {
                                driver = (Driver) clazz.newInstance();
                                // MOD mzhao 2009-06-05,Bug 7571 Get driver from
                                // catch first, if not
                                // exist then get a new instance.
                                DRIVER_CACHE.put(driverClassName, driver);
                                return driver; // driver is found
                            }
                        } catch (ClassNotFoundException e) {
                            // do nothings
                        }
                    }

                }
            } catch (MalformedURLException e) {
                // do nothings
            }
        }
        if (driver == null) {
            driver = (Driver) Class.forName(driverClassName).newInstance();
        }
        // MOD mzhao 2009-06-05,Bug 7571 Get driver from catch first, if not
        // exist then get a new instance.
        DRIVER_CACHE.put(driverClassName, driver);
        return driver;
    }

    /**
     * Method "isValid".
     * 
     * @param connection the connection to test
     * @return a return code with the appropriate message (never null)
     */
    public static ReturnCode isValid(final java.sql.Connection connection) {
        return org.talend.utils.sql.ConnectionUtils.isValid(connection);
    }

    /**
     * Method "closeConnection".
     * 
     * @param connection the connection to close.
     * @return a ReturnCode with true if ok, false if problem. {@link ReturnCode#getMessage()} gives the error message
     * when there is a problem.
     */
    public static ReturnCode closeConnection(final java.sql.Connection connection) {
        return org.talend.utils.sql.ConnectionUtils.closeConnection(connection);
    }

    /**
     * DOC xqliu Comment method "getConnectionMetadata". 2009-07-13 bug 7888.
     * 
     * @param conn
     * @return
     * @throws SQLException
     */

    public static DatabaseMetaData getConnectionMetadata(java.sql.Connection conn) throws SQLException {
        DatabaseMetaData dbMetaData = conn.getMetaData();
        // MOD xqliu 2009-11-17 bug 7888
        if (dbMetaData != null && dbMetaData.getDatabaseProductName() != null
                && dbMetaData.getDatabaseProductName().equals(DatabaseConstant.IBM_DB2_ZOS_PRODUCT_NAME)) {
            dbMetaData = createFakeDatabaseMetaData(conn);
            log.info("IBM DB2 for z/OS");
        }
        // ~
        return dbMetaData;
    }

    /**
     * only for db2 on z/os right now. 2009-07-13 bug 7888.
     * 
     * @param conn2
     * @return
     */
    private static DatabaseMetaData createFakeDatabaseMetaData(java.sql.Connection conn) {
        DB2ForZosDataBaseMetadata dmd = new DB2ForZosDataBaseMetadata(conn);
        return dmd;
    }

    // ADD xqliu 2009-11-09 bug 9403
    private static final String DEFAULT_TABLE_NAME = "TDQ_PRODUCT";

    /**
     * DOC xqliu Comment method "existTable".
     * 
     * @param url
     * @param driver
     * @param props
     * @param tableName
     * @return
     */
    public static boolean existTable(String url, String driver, Properties props, String tableName) {
        java.sql.Connection connection = null;
        if (tableName == null || "".equals(tableName.trim())) {
            tableName = DEFAULT_TABLE_NAME;
        }
        try {
            connection = ConnectionUtils.createConnection(url, driver, props);
            if (connection != null) {
                Statement stat = connection.createStatement();
                stat.executeQuery("Select * from " + tableName);
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.warn(e);
                }
            }
        }
        return true;
    }

    // ~

    /**
     * DOC xqliu Comment method "isOdbcMssql". bug 9822
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean isOdbcMssql(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null
                && connectionMetadata.getDriverName().toLowerCase().startsWith(DatabaseConstant.ODBC_DRIVER_NAME)
                && connectionMetadata.getDatabaseProductName() != null
                && connectionMetadata.getDatabaseProductName().equals(DatabaseConstant.ODBC_MSSQL_PRODUCT_NAME)) {
            return true;
        }
        return false;
    }

    /**
     * DOC zshen Comment method "isMssql".
     * 
     * @param connection
     * @return decide to whether is mssql connection
     * @throws SQLException
     */
    public static boolean isMssql(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null
                && !connectionMetadata.getDriverName().toLowerCase().startsWith(DatabaseConstant.ODBC_DRIVER_NAME)
                && connectionMetadata.getDatabaseProductName() != null
                && connectionMetadata.getDatabaseProductName().equals(DatabaseConstant.ODBC_MSSQL_PRODUCT_NAME)) {
            return true;
        }
        return false;
    }

    /**
     * DOC zshen Comment method "isSybase".
     * 
     * @param connection
     * @return decide to whether is sybase connection
     * @throws SQLException
     */
    public static boolean isSybase(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null && connectionMetadata.getDatabaseProductName() != null
                && connectionMetadata.getDatabaseProductName().equals(SupportDBUrlType.SYBASEDEFAULTURL.getLanguage())) {
            return true;
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "isMdmConnection".
     * 
     * @param dataprovider
     * @return
     */
    public static boolean isMdmConnection(DataProvider dataprovider) {
        return dataprovider instanceof MDMConnection;
    }

    /**
     * DOC xqliu Comment method "isMdmConnection".
     * 
     * @param object
     * @return
     */
    public static boolean isMdmConnection(Object object) {
        if (object != null) {
            if (object instanceof ProviderConnection) {
                return isMdmConnection((ProviderConnection) object);
            } else if (object instanceof DataProvider) {
                return isMdmConnection((DataProvider) object);
            } else if (object instanceof IFile) {
                return isMdmConnection((IFile) object);
            }
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "isMdmConnection".
     * 
     * @param file
     * @return
     */
    public static boolean isMdmConnection(IFile file) {
        if (FactoriesUtil.PROV.equals(file.getFileExtension())) {
            PrvResourceFileHelper prvHelper = PrvResourceFileHelper.getInstance();
            TypedReturnCode<Connection> findProvider = prvHelper.findProvider(file);
            if (findProvider != null && findProvider.getObject() != null) {
                if (ConnectionUtils.isMdmConnection(findProvider.getObject())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * DOC zshen Comment method "isOdbcMssql". feature 10630
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean isOdbcExcel(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null
                && connectionMetadata.getDriverName().toLowerCase().startsWith(DatabaseConstant.ODBC_DRIVER_NAME)
                && connectionMetadata.getDatabaseProductName() != null
                && connectionMetadata.getDatabaseProductName().equals(DatabaseConstant.ODBC_EXCEL_PRODUCT_NAME)) {
            return true;
        }
        return false;
    }

    /**
     * DOC zshen Comment method "isOdbcConnection". feature 10630
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean isOdbcConnection(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null
                && connectionMetadata.getDriverName().toLowerCase().startsWith(DatabaseConstant.ODBC_DRIVER_NAME)) {
            return true;
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "isPostgresql".
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean isPostgresql(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        if (metaData != null) {
            String databaseProductName = metaData.getDatabaseProductName();
            if (databaseProductName != null) {
                return databaseProductName.toLowerCase().indexOf(DatabaseConstant.POSTGRESQL_PRODUCT_NAME) > -1;
            }
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "isOdbcPostgresql".
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean isOdbcPostgresql(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null
                && connectionMetadata.getDriverName().toLowerCase().startsWith(DatabaseConstant.ODBC_DRIVER_NAME)
                && connectionMetadata.getDatabaseProductName() != null
                && connectionMetadata.getDatabaseProductName().toLowerCase().indexOf(DatabaseConstant.POSTGRESQL_PRODUCT_NAME) > -1) {
            return true;
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "isOdbcOracle".
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean isOdbcOracle(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null
                && connectionMetadata.getDriverName().toLowerCase().startsWith(DatabaseConstant.ODBC_DRIVER_NAME)
                && connectionMetadata.getDatabaseProductName() != null
                && connectionMetadata.getDatabaseProductName().toLowerCase().indexOf(DatabaseConstant.ODBC_ORACLE_PRODUCT_NAME) > -1) {
            return true;
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "isOdbcIngres".
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean isOdbcIngres(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null
                && connectionMetadata.getDriverName().toLowerCase().startsWith(DatabaseConstant.ODBC_DRIVER_NAME)
                && connectionMetadata.getDatabaseProductName() != null
                && connectionMetadata.getDatabaseProductName().toLowerCase().indexOf(DatabaseConstant.INGRES_PRODUCT_NAME) > -1) {
            return true;
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "isJdbcIngres".
     * 
     * @param connection
     * @return
     * @throws SQLException
     */
    public static boolean isJdbcIngres(java.sql.Connection connection) throws SQLException {
        DatabaseMetaData connectionMetadata = getConnectionMetadata(connection);
        if (connectionMetadata.getDriverName() != null
                && connectionMetadata.getDriverName().equals(DatabaseConstant.JDBC_INGRES_DEIVER_NAME)
                && connectionMetadata.getDatabaseProductName() != null
                && connectionMetadata.getDatabaseProductName().toLowerCase().indexOf(DatabaseConstant.INGRES_PRODUCT_NAME) > -1) {
            return true;
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "isSqlite".
     * 
     * @param connection
     * @return
     */
    public static boolean isSqlite(Connection connection) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(connection);
        if (dbConn != null) {
            return SupportDBUrlType.SQLITE3DEFAULTURL.getDBKey().equals(dbConn.getDatabaseType());
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "printResultSetColumns".
     * 
     * @param rs
     */
    public static void printResultSetColumns(ResultSet rs) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnSize = metaData.getColumnCount();
            for (int i = 0; i < columnSize; ++i) {
                System.out.println("[" + (i + 1) + "]:" + metaData.getColumnName(i + 1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * DOC xqliu Comment method "getDatabaseType".
     * 
     * @param connection
     * @return the database type string or null
     */
    public static String getDatabaseType(Connection connection) {
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(connection);
        if (mdmConn != null) {
            return SupportDBUrlType.MDM.getDBKey();
        }
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(connection);
        if (dbConn != null) {
            return dbConn.getDatabaseType();
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "getUsername".
     * 
     * @param conn
     * @return username of the connection or null
     */
    public static String getUsername(Connection conn) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            return dbConn.getUsername();
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            return mdmConn.getUsername();
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "getUsername".
     * 
     * @param conn
     * @param defaultUsername
     * @return username of the connection or given username
     */
    public static String getUsername(Connection conn, String defaultUsername) {
        String result = getUsername(conn);
        result = (result == null || "".equals(result.trim())) ? defaultUsername : result;
        return result;
    }

    /**
     * DOC xqliu Comment method "getUsernameDefault".
     * 
     * @param conn
     * @return username of the connection or default username
     */
    public static String getUsernameDefault(Connection conn) {
        return getUsername(conn, DEFAULT_USERNAME);
    }

    /**
     * DOC xqliu Comment method "setUsername".
     * 
     * @param conn
     * @param username
     */
    public static void setUsername(Connection conn, String username) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            dbConn.setUsername(username);
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            mdmConn.setUsername(username);
        }
    }

    /**
     * DOC xqliu Comment method "getPassword".
     * 
     * @param conn
     * @return password of the connection or null
     */
    public static String getPassword(Connection conn) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            return dbConn.getPassword();
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            return mdmConn.getPassword();
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "getPassword".
     * 
     * @param conn
     * @param defaultPassword
     * @return password of the connection or given password
     */
    public static String getPassword(Connection conn, String defaultPassword) {
        String result = getPassword(conn);
        result = result == null ? defaultPassword : result;
        return result;
    }

    /**
     * DOC xqliu Comment method "getPasswordDefault".
     * 
     * @param conn
     * @return password of the connection or default password
     */
    public static String getPasswordDefault(Connection conn) {
        return getPassword(conn, DEFAULT_PASSWORD);
    }

    /**
     * DOC xqliu Comment method "setPassword".
     * 
     * @param conn
     * @param password
     */
    public static void setPassword(Connection conn, String password) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            dbConn.setPassword(ConnectionHelper.getEncryptPassword(password));
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            mdmConn.setPassword(ConnectionHelper.getEncryptPassword(password));
        }
    }

    /**
     * DOC xqliu Comment method "getDriverClass".
     * 
     * @param conn
     * @return driver class name of the connection or null
     */
    public static String getDriverClass(Connection conn) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            return dbConn.getDriverClass();
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            return "";
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "setDriverClass".
     * 
     * @param conn
     * @param driverClass
     */
    public static void setDriverClass(Connection conn, String driverClass) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            dbConn.setDriverClass(driverClass);
        }
    }

    /**
     * DOC xqliu Comment method "getURL".
     * 
     * @param conn
     * @return url string of the connection or null
     */
    public static String getURL(Connection conn) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            return dbConn.getURL();
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            return mdmConn.getPathname();
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "setURL".
     * 
     * @param conn
     * @param url
     */
    public static void setURL(Connection conn, String url) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            dbConn.setURL(url);
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            mdmConn.setPathname(url);
        }
    }

    /**
     * DOC xqliu Comment method "getServerName".
     * 
     * @param conn
     * @return server name of the connection or null
     */
    public static String getServerName(Connection conn) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            return dbConn.getServerName();
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            return mdmConn.getServer();
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "setServerName".
     * 
     * @param conn
     * @param serverName
     */
    public static void setServerName(Connection conn, String serverName) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            dbConn.setServerName(serverName);
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            mdmConn.setServer(serverName);
        }
    }

    /**
     * DOC xqliu Comment method "getPort".
     * 
     * @param conn
     * @return port of the connection or null
     */
    public static String getPort(Connection conn) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            return dbConn.getPort();
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            return mdmConn.getPort();
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "setPort".
     * 
     * @param conn
     * @param port
     */
    public static void setPort(Connection conn, String port) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            dbConn.setPort(port);
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            mdmConn.setPort(port);
        }
    }

    /**
     * DOC xqliu Comment method "getSID".
     * 
     * @param conn
     * @return sid of the connection or null
     */
    public static String getSID(Connection conn) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            return dbConn.getSID();
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            return mdmConn.getContext();
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "setSID".
     * 
     * @param conn
     * @param sid
     */
    public static void setSID(Connection conn, String sid) {
        DatabaseConnection dbConn = SwitchHelpers.DATABASECONNECTION_SWITCH.doSwitch(conn);
        if (dbConn != null) {
            dbConn.setSID(sid);
        }
        MDMConnection mdmConn = SwitchHelpers.MDMCONNECTION_SWITCH.doSwitch(conn);
        if (mdmConn != null) {
            mdmConn.setContext(sid);
        }
    }
}
