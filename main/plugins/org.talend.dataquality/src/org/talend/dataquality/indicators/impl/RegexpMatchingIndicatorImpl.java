/**
 * <copyright> </copyright>
 * 
 * $Id$
 */
package org.talend.dataquality.indicators.impl;

import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.talend.dataquality.domain.Domain;
import org.talend.dataquality.domain.pattern.Pattern;
import org.talend.dataquality.helpers.DomainHelper;
import org.talend.dataquality.indicators.IndicatorsPackage;
import org.talend.dataquality.indicators.RegexpMatchingIndicator;
import org.talend.i18n.Messages;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>Regexp Matching Indicator</b></em>'. <!--
 * end-user-doc -->
 * <p>
 * </p>
 * 
 * @generated
 */
public class RegexpMatchingIndicatorImpl extends PatternMatchingIndicatorImpl implements RegexpMatchingIndicator {

    private static Logger log = Logger.getLogger(RegexpMatchingIndicatorImpl.class);

    // FIXME never use now. error message should not be specific to this indicator.
    // add klliu 2010-06-12 bug 13695
    private String javaPatternMessage;

    public String getJavaPatternMessage() {
        return javaPatternMessage;
    }

    public void setJavaPatternMessage(String javaPatternMessage) {
        this.javaPatternMessage = javaPatternMessage;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    protected RegexpMatchingIndicatorImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    protected EClass eStaticClass() {
        return IndicatorsPackage.Literals.REGEXP_MATCHING_INDICATOR;
    }

    private String regex = null;

    private java.util.regex.Pattern pattern = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.indicators.impl.IndicatorImpl#prepare()
     */
    @Override
    public boolean prepare() {
        this.regex = getRegex();
        if (regex == null) {
            return false;
        }
        // MOD klliu 2010-06-12 bug 13695
        if (regex.equals(this.getJavaPatternMessage())) {
            return false;
        }
        try {
            pattern = java.util.regex.Pattern.compile(regex);
        } catch (java.util.regex.PatternSyntaxException e) {
            log.error(Messages.getString("Using_regular_expression", this.getName(), regex), e);
            return false;
        }
        if (log.isInfoEnabled()) {
            log.info(Messages.getString("Using_regular_expression", this.getName(), regex));
        }
        return super.prepare();
    }

    /**
     * DOC scorreia Comment method "getRegex".
     * 
     * this mehtod only for job Action
     * 
     * 1) only in Java Engine will call this method 2) get Java Regex first, if don't have Java Regex then get Default
     * Regex, if don't have Default Regex then return null
     * 
     * @return
     */
    @Override
    public String getRegex() {
        // MOD klliu 2010-06-12 bug 13695
        if (this.parameters != null) {
            final Domain dataValidDomain = parameters.getDataValidDomain();
            if (dataValidDomain != null) {
                final EList<Pattern> patterns = dataValidDomain.getPatterns();
                for (Pattern p : patterns) {
                    if (p != null) {
                        // MOD yyi 2009-09-29 Feature: 9289
                        String r = DomainHelper.getJavaRegexp(p);
                        if (r == null) { // if don't have Java Regex, get the Default Regex
                            r = DomainHelper.getSQLRegexp(p);
                        }

                        if (r != null) {
                            if (r.startsWith("'") && r.endsWith("'")) {
                                // remove enclosing singles quotes which are used for SQL only (not java)
                                r = r.substring(1, r.length() - 1);
                            }
                            return r;
                        }
                    }
                }
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.indicators.impl.IndicatorImpl#finalizeComputation()
     */
    @Override
    public boolean finalizeComputation() {
        if (count != null && matchingValueCount != null) {
            this.setNotMatchingValueCount(count - matchingValueCount);
        }
        return super.finalizeComputation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.indicators.impl.IndicatorImpl#handle(java.lang.Object)
     */
    @Override
    public boolean handle(Object data) {
        this.mustStoreRow = true;
        this.setValidRow(false);
        this.setInValidRow(false);
        if (data != null) {
            String body = String.valueOf(data);
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                this.matchingValueCount++;
                this.setValidRow(true);
            } else {
                this.setInValidRow(true);
            }

        } else {
            this.setInValidRow(true);
        }
        boolean ok = super.handle(data);

        return ok;
    }

} // RegexpMatchingIndicatorImpl
