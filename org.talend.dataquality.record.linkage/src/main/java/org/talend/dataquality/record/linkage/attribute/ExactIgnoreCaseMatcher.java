// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataquality.record.linkage.attribute;

import org.apache.commons.lang.StringUtils;
import org.talend.dataquality.record.linkage.constant.AttributeMatcherType;


/**
 * DOC scorreia  class global comment. Detailled comment
 */
public class ExactIgnoreCaseMatcher implements IAttributeMatcher {

    /* (non-Javadoc)
     * @see org.talend.dataquality.matching.attribute.IAttributeMatcher#getMatchingProba(java.lang.String, java.lang.String)
     */
    public double getMatchingWeight(String str1, String str2) {
        return StringUtils.equalsIgnoreCase(str1, str2) ? 1 : 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.record.linkage.attribute.IAttributeMatcher#getMatchType()
     */
    public AttributeMatcherType getMatchType() {
        return AttributeMatcherType.exactIgnoreCase;
    }

}
