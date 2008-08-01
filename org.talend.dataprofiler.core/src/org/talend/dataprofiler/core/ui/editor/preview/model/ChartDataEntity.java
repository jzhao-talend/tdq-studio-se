// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.editor.preview.model;

import java.text.DecimalFormat;

import org.talend.dataquality.indicators.FrequencyIndicator;
import org.talend.dataquality.indicators.Indicator;

/**
 * DOC zqin class global comment. Detailled comment
 */
public class ChartDataEntity {

    private String label;

    private String value;

    private Indicator indicator;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPersent() {
        if (indicator != null) {

            DecimalFormat format = (DecimalFormat) DecimalFormat.getPercentInstance();
            format.applyPattern("0.00%");

            double persent;

            if (indicator instanceof FrequencyIndicator) {
                FrequencyIndicator freIndicator = (FrequencyIndicator) indicator;
                persent = Double.parseDouble(getValue()) / freIndicator.getValueToFreq().size();
            } else {
                persent = Double.parseDouble(getValue()) / indicator.getCount().doubleValue();
            }

            return persent == 0 ? "0" : format.format(persent);
        }

        return "";
    }

    public Indicator getIndicator() {
        return indicator;
    }

    public void setIndicator(Indicator indicator) {
        this.indicator = indicator;
    }

}
