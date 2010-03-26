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
package org.talend.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.talend.cwm.helper.DataProviderHelper;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.domain.pattern.Pattern;
import org.talend.dataquality.indicators.sql.UserDefIndicator;
import org.talend.dataquality.rules.DQRule;
import orgomg.cwm.foundation.softwaredeployment.DataProvider;
import orgomg.cwm.foundation.softwaredeployment.ProviderConnection;
import orgomg.cwm.objectmodel.core.ModelElement;
import orgomg.cwm.objectmodel.core.TaggedValue;
import orgomg.cwmx.analysis.informationreporting.Report;

/**
 * DOC bZhou class global comment. Detailled comment
 */
public enum EResourceConstant {

    DATA_PROFILING("TDQ_Data Profiling", "TDQ_Data Profiling", ResourceConstant.READONLY, ResourceConstant.NO_SUBFOLDER),
    LIBRARIES("TDQ_Libraries", "TDQ_Libraries", ResourceConstant.READONLY, ResourceConstant.NO_SUBFOLDER),
    METADATA("TDQ_Metadata", "TDQ_Metadata", ResourceConstant.READONLY, ResourceConstant.NO_SUBFOLDER),
    ANALYSIS("Analyses", "TDQ_Data Profiling/Analyses", ResourceConstant.READONLY),
    REPORTS("Reports", "TDQ_Data Profiling/Reports", ResourceConstant.READONLY),
    EXCHANGE("Exchange", "TDQ_Libraries/Exchange", ResourceConstant.READONLY, ResourceConstant.NO_SUBFOLDER),
    INDICATORS("Indicators", "TDQ_Libraries/Indicators", ResourceConstant.READONLY, ResourceConstant.NO_SUBFOLDER),
    JRXML_TEMPLATE("JRXML Template", "TDQ_Libraries/JRXML Template", ResourceConstant.READONLY),
    PATTERNS("Patterns", "TDQ_Libraries/Patterns", ResourceConstant.READONLY, ResourceConstant.NO_SUBFOLDER),
    RULES("Rules", "TDQ_Libraries/Rules", ResourceConstant.READONLY, ResourceConstant.NO_SUBFOLDER),
    SOURCE_FILES("Source Files", "TDQ_Libraries/Source Files", ResourceConstant.READONLY),
    USER_DEFINED_INDICATORS(
                            "User Defined Indicators",
                            "TDQ_Libraries/Indicators/User Defined Indicators",
                            ResourceConstant.READONLY),
    PATTERN_REGEX("Regex", "TDQ_Libraries/Patterns/Regex", ResourceConstant.READONLY),
    PATTERN_SQL("SQL", "TDQ_Libraries/Patterns/SQL", ResourceConstant.READONLY),
    RULES_SQL("SQL", "TDQ_Libraries/Rules/SQL", ResourceConstant.READONLY),
    DB_CONNECTIONS("DB Connections", "TDQ_Metadata/DB Connections", ResourceConstant.READONLY),
    MDM_CONNECTIONS("MDM Connections", "TDQ_Metadata/MDM Connections", ResourceConstant.READONLY),
    REPORTING_DB("TDQ_reporting_db", "REPORTING_DB", ResourceConstant.READONLY);

    private String name;

    private String path;

    private QualifiedName[] qualifications;

    EResourceConstant(String name, String path, QualifiedName... qualifications) {
        this.name = name;
        this.path = path;
        this.qualifications = qualifications;
    }

    /**
     * Getter for name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for path.
     * 
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Getter for qualifications.
     * 
     * @return the qualifications
     */
    public QualifiedName[] getQualifications() {
        return qualifications;
    }

    /**
     * DOC bzhou Comment method "getPathes".
     * 
     * @return
     */
    public static IPath[] getPathes() {
        List<IPath> allPathes = new ArrayList<IPath>();

        for (EResourceConstant constant : values()) {
            allPathes.add(new Path(constant.getPath()));
        }

        return allPathes.toArray(new Path[allPathes.size()]);
    }

    /**
     * 
     * This method is to find the matched Qualifications constant by path.
     * 
     * @param path
     * @return
     */
    public static QualifiedName[] findQualificationsByPath(String path) {
        for (EResourceConstant constant : values()) {
            if (StringUtils.equals(path, constant.getPath())) {
                return constant.getQualifications();
            }
        }

        return null;
    }

    /**
     * DOC bZhou Comment method "getTypedConstant".
     * 
     * @param element
     * @return
     */
    public static EResourceConstant getTypedConstant(ModelElement element) {

        if (element instanceof Analysis) {
            return ANALYSIS;
        }

        if (element instanceof Report) {
            return REPORTS;
        }

        if (element instanceof UserDefIndicator) {
            return USER_DEFINED_INDICATORS;
        }

        if (element instanceof Pattern) {
            return PATTERNS;
        }

        if (element instanceof DQRule) {
            return RULES_SQL;
        }

        if (element instanceof DataProvider) {
            DataProvider provider = (DataProvider) element;

            if (isMDMConnection(provider)) {
                return MDM_CONNECTIONS;
            }

            return DB_CONNECTIONS;
        }

        return null;
    }

    /**
     * DOC bZhou Comment method "isMDMConnection".
     * 
     * @param provider
     * @return
     */
    private static boolean isMDMConnection(DataProvider provider) {
        ProviderConnection connection = DataProviderHelper.getTdProviderConnection(provider).getObject();

        TaggedValue tv = TaggedValueHelper.getTaggedValue(TaggedValueHelper.DBTYPE, connection.getTaggedValue());
        if (tv != null) {
            return StringUtils.equalsIgnoreCase("MDM", tv.getValue());
        }

        return false;
    }

    /**
     * 
     * This mehtod is to get the top level folder constant of TOP.
     * 
     * @return An array of EResourceConstant.
     */
    public static EResourceConstant[] getTopConstants() {
        return new EResourceConstant[] { DATA_PROFILING, LIBRARIES, METADATA };
    }

    /**
     * 
     * This method is to see whether the path is a current workspace constant path.
     * 
     * @param path
     * @return true if it is, otherwise false.
     */
    public static boolean isConstantPath(IPath path) {
        if (path != null) {
            for (IPath tmp : getPathes()) {
                if (tmp.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }
}
