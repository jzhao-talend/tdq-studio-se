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
package org.talend.dataprofiler.core.ui.editor.analysis;

import java.awt.event.MouseEvent;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.Section;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dataprofiler.common.ui.editor.preview.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.chart.ChartDecorator;
import org.talend.dataprofiler.common.ui.editor.preview.chart.TopChartFactory;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableFactory;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableMenuGenerator;
import org.talend.dataprofiler.core.ui.editor.preview.model.MenuItemEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.entity.TableStructureEntity;
import org.talend.dataprofiler.core.ui.pref.EditorPreferencePage;
import org.talend.dataprofiler.core.ui.utils.SqlExplorerUtils;
import org.talend.dataquality.PluginConstant;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.columnset.ColumnDependencyIndicator;
import org.talend.dq.analysis.AnalysisHandler;
import org.talend.dq.analysis.explore.ColumnDependencyExplorer;
import org.talend.dq.helper.ColumnDependencyHelper;
import org.talend.dq.indicators.preview.table.ChartDataEntity;
import org.talend.dq.indicators.preview.table.PatternChartDataEntity;
import org.talend.utils.format.StringFormatUtil;

/**
 * DOC jet class global comment. Detailled comment
 */
public class ColumnDependencyResultPage extends AbstractAnalysisResultPage {

    private Composite analyzedColumnSetsComp;

    private Composite analysisResultsComp;

    private Section resultSection = null;

    private ColumnDependencyMasterDetailsPage masterPage;

    private ColumnDependencyIndicator columnDependencyIndicator;

    public ColumnDependencyIndicator getColumnDependencyIndicator() {
        return this.columnDependencyIndicator;
    }

    public void setColumnDependencyIndicator(ColumnDependencyIndicator columnDependencyIndicator) {
        this.columnDependencyIndicator = columnDependencyIndicator;
    }

    /**
     * DOC jet ColumnDependencyResultPage constructor comment.
     * 
     * @param editor
     * @param id
     * @param title
     */
    public ColumnDependencyResultPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        AnalysisEditor analysisEditor = (AnalysisEditor) editor;
        this.masterPage = (ColumnDependencyMasterDetailsPage) analysisEditor.getMasterPage();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisResultPage#getAnalysisHandler()
     */
    @Override
    protected AnalysisHandler getAnalysisHandler() {
        // TODO Auto-generated method stub
        return this.masterPage.getAnalysisHandler();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisResultPage#refresh(org.talend.dataprofiler.core
     * .ui.editor.analysis.AbstractAnalysisMetadataPage)
     */
    @Override
    public void refresh(AbstractAnalysisMetadataPage masterPage) {

        this.masterPage = (ColumnDependencyMasterDetailsPage) masterPage;
        if (summaryComp != null && !summaryComp.isDisposed()) {
            summaryComp.dispose();
        }

        if (analyzedColumnSetsComp != null && !analyzedColumnSetsComp.isDisposed()) {
            analyzedColumnSetsComp.dispose();
        }

        if (analysisResultsComp != null && !analysisResultsComp.isDisposed()) {
            analysisResultsComp.dispose();
        }

        createFormContent(getManagedForm());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.AbstractFormPage#setDirty(boolean)
     */
    @Override
    public void setDirty(boolean isDirty) {

    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);

        // Analysis analysis = this.getAnalysisHandler().getAnalysis();

        analyzedColumnSetsComp = toolkit.createComposite(topComposite);
        analyzedColumnSetsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        analyzedColumnSetsComp.setLayout(new GridLayout());

        analysisResultsComp = toolkit.createComposite(topComposite);
        analysisResultsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        analysisResultsComp.setLayout(new GridLayout());
        createResultSection(analysisResultsComp);

        form.reflow(true);
    }

    @Override
    protected void createResultSection(Composite parent) {
        resultSection = createSection(form, parent,
                DefaultMessagesImpl.getString("ColumnsComparisonAnalysisResultPage.analysisResults"), ""); //$NON-NLS-1$ //$NON-NLS-2$
        Composite sectionClient = toolkit.createComposite(resultSection);
        sectionClient.setLayout(new GridLayout(2, false));
        sectionClient.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        resultSection.setClient(sectionClient);

        Analysis analysis = this.masterPage.getAnalysisHandler().getAnalysis();
        EList<Indicator> indicators = analysis.getResults().getIndicators();
        if (indicators.size() > 0) {
            columnDependencyIndicator = (ColumnDependencyIndicator) indicators.get(0);
        }

        createTable(sectionClient);

        if (!EditorPreferencePage.isHideGraphics()) {
            createChart(sectionClient, analysis);
        }

        resultSection.layout();
    }

    /**
     * DOC jet create result chart.
     * <p>
     * the chart must include
     * 
     * @param sectionClient
     * @param analysis
     */
    private void createChart(Composite sectionClient, Analysis analysis) {
        CustomerDefaultCategoryDataset dataset = initCustomerDataset();

        JFreeChart createChart = TopChartFactory
                .createStackedBarChart(
                        DefaultMessagesImpl.getString("ColumnDependencyResultPage.dependencyStrength"), dataset, PlotOrientation.HORIZONTAL, true); //$NON-NLS-1$
        ChartDecorator.decorateColumnDependency(createChart);

        GridData gd = new GridData();
        gd.heightHint = 180;
        gd.widthHint = 450;

        final ChartComposite chartComp = new ChartComposite(sectionClient, SWT.NONE, createChart);
        chartComp.setLayoutData(gd);
        // ADD xqliu 2009-09-11
        chartComp.addChartMouseListener(new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent event) {

                boolean flag = event.getTrigger().getButton() != MouseEvent.BUTTON3;

                chartComp.setDomainZoomable(flag);
                chartComp.setRangeZoomable(flag);

                if (flag) {
                    return;
                }

                ChartEntity chartEntity = event.getEntity();
                if (chartEntity != null && chartEntity instanceof CategoryItemEntity) {
                    CategoryItemEntity cateEntity = (CategoryItemEntity) chartEntity;
                    CustomerDefaultCategoryDataset dataEntity = (CustomerDefaultCategoryDataset) cateEntity.getDataset();

                    Menu menu = new Menu(chartComp.getShell(), SWT.POP_UP);
                    chartComp.setMenu(menu);

                    ChartDataEntity currentDataEntity = null;
                    ChartDataEntity[] dataEntities = dataEntity.getDataEntities();
                    if (dataEntities.length == 1) {
                        currentDataEntity = dataEntities[0];
                    } else {
                        for (ChartDataEntity entity : dataEntities) {
                            if (cateEntity.getColumnKey().compareTo(entity.getLabel()) == 0) {
                                currentDataEntity = entity;
                            } else {
                                if (cateEntity.getRowKey().compareTo(entity.getLabel()) == 0) {
                                    currentDataEntity = entity;
                                }
                            }
                        }
                    }

                    if (currentDataEntity != null) {
                        ColumnDependencyExplorer explorer = new ColumnDependencyExplorer();
                        final Analysis analysis = getAnalysisHandler().getAnalysis();
                        final Indicator indicator = currentDataEntity.getIndicator();

                        MenuItemEntity[] itemEntities = ChartTableMenuGenerator.generate(explorer, analysis, currentDataEntity);
                        for (final MenuItemEntity itemEntity : itemEntities) {
                            MenuItem menuItem = new MenuItem(menu, SWT.NONE);
                            menuItem.setText(itemEntity.getLabel());
                            menuItem.setImage(ImageLib.getImage(ImageLib.EXPLORE_IMAGE));
                            menuItem.addSelectionListener(new SelectionAdapter() {

                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    Display.getDefault().asyncExec(new Runnable() {

                                        public void run() {
                                            Connection tdDataProvider = SwitchHelpers.CONNECTION_SWITCH.doSwitch(analysis
                                                    .getContext().getConnection());
                                            String query = itemEntity.getQuery();
                                            String editorName = ColumnDependencyHelper.getIndicatorName(indicator);
                                            SqlExplorerUtils.getDefault().runInDQViewer(tdDataProvider, query, editorName);
                                        }

                                    });
                                }
                            });
                        }
                    }

                    menu.setVisible(true);
                }
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.jfree.chart.ChartMouseListener#chartMouseMoved(org.jfree.chart.ChartMouseEvent)
             */
            public void chartMouseMoved(ChartMouseEvent event) {

            }
        });
        // ~
    }

    /**
     * DOC xqliu Comment method "createTable".
     * 
     * @param composite
     */
    private void createTable(Composite composite) {
        final TableViewer tableViewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.BORDER);
        Table resultTable = tableViewer.getTable();

        resultTable.setLinesVisible(true);
        resultTable.setHeaderVisible(true);
        // create table headers
        TableStructureEntity tableStructure = getTableStructure();
        String[] fieldNames = tableStructure.getFieldNames();
        Integer[] fieldWidths = tableStructure.getFieldWidths();
        for (int i = 0; i < fieldNames.length; ++i) {
            TableColumn columnHeader = new TableColumn(resultTable, SWT.LEFT);
            columnHeader.setText(fieldNames[i]);
            columnHeader.setWidth(fieldWidths[i]);
        }
        // create table items
        CustomerDefaultCategoryDataset dataset = initCustomerDataset();
        ChartDataEntity[] dataEntities = dataset.getDataEntities();
        int i = 0;
        if (dataEntities != null) {
            // MOD mzhao bug 8839 There might be duplicate dependencies on left and right columnSet.
            if (dataset.getColumnCount() < dataEntities.length) {
                MessageDialog.openError(this.getEditor().getSite().getShell(), "Duplicate dependencies",//$NON-NLS-1$
                        "There might be duplicate dependencies on left and right columnSet.");//$NON-NLS-1$
            } else {
                for (ChartDataEntity dataEntity : dataEntities) {
                    TableItem item = new TableItem(resultTable, SWT.NULL);

                    Number match = dataset.getValue(0, i);
                    Number notMatch = dataset.getValue(1, i);
                    Number row = match.intValue() + notMatch.intValue();

                    item.setText(0, dataset.getColumnKey(i).toString());
                    item.setText(1, String.valueOf(match.intValue()));
                    // TDQ-8695 display "N/A" if it is infinite or NaN
                    double percentage = match.doubleValue() / row.doubleValue();
                    if (Double.isNaN(percentage) || Double.isInfinite(percentage)) {
                        item.setText(2, PluginConstant.NA_STRING);
                    } else {
                        item.setText(2, StringFormatUtil.format(String.valueOf(percentage), StringFormatUtil.PERCENT).toString());
                    }
                    item.setText(3, String.valueOf(row));

                    item.setData(dataEntity);
                    i++;
                }
            }
        }

        GridData gd = new GridData();
        gd.heightHint = 180;
        gd.widthHint = 450;

        resultTable.setLayoutData(gd);

        // add menus
        ChartTableFactory.addMenuAndTip(tableViewer, new ColumnDependencyExplorer(), this.masterPage.analysisItem.getAnalysis());
    }

    /**
     * DOC jet according to current analysis generator chart dataset "initDataset".
     * 
     * @return
     * @deprecated
     */
    // private DefaultCategoryDataset initDataset() {
    //
    // DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    //
    // Analysis analysis = this.getAnalysisHandler().getAnalysis();
    //
    // for (Iterator iterator = analysis.getResults().getIndicators().iterator(); iterator.hasNext();) {
    // ColumnDependencyIndicator indicator = (ColumnDependencyIndicator) iterator.next();
    // String label = getRowLabel(indicator);
    // if (getAnalysisHandler().getResultMetadata().getExecutionNumber() > 0) {
    // dataset.addValue(indicator.getDistinctACount(), "Dependency Strength", label);
    //                dataset.addValue(indicator.getACount() - indicator.getDistinctACount(), "", label); //$NON-NLS-1$
    // }
    // }
    // return dataset;
    // }

    /**
     * DOC xqliu Comment method "initCustomerDataset".
     * 
     * @return
     */
    private CustomerDefaultCategoryDataset initCustomerDataset() {

        CustomerDefaultCategoryDataset dataset = new CustomerDefaultCategoryDataset();

        Analysis analysis = this.getAnalysisHandler().getAnalysis();

        for (Indicator indicator2 : analysis.getResults().getIndicators()) {
            ColumnDependencyIndicator indicator = (ColumnDependencyIndicator) indicator2;
            String label = ColumnDependencyHelper.getIndicatorName(indicator);
            if (getAnalysisHandler().getResultMetadata().getExecutionNumber() > 0) {
                Long matchCount = indicator.getDistinctACount() == null ? 0 : indicator.getDistinctACount();
                Long notMatchCount = indicator.getACount() == null ? 0 : indicator.getACount() - matchCount;
                dataset.addValue(matchCount, DefaultMessagesImpl.getString("ColumnDependencyResultPage.Match"), label); //$NON-NLS-1$
                dataset.addValue(notMatchCount, DefaultMessagesImpl.getString("ColumnDependencyResultPage.NotMatch"), label); //$NON-NLS-1$

                PatternChartDataEntity dataEntity = new PatternChartDataEntity();
                dataEntity.setLabel(label);
                dataEntity.setIndicator(indicator);
                dataEntity.setNumMatch(matchCount.toString());
                dataEntity.setNumNoMatch(notMatchCount.toString());

                dataset.addDataEntity(dataEntity);
            }
        }
        return dataset;
    }

    /**
     * DOC jet Comment method "getRowLabel".
     * <p>
     * according to ColumnDependencyIndicator get chart label.
     * <p>
     * value is columnA.getName() -> columnB.getName()
     * 
     * @param indicator
     * @return
     * @deprecated
     */
    // private String getRowLabel(ColumnDependencyIndicator indicator) {
    // assert indicator.getColumnA() != null;
    // assert indicator.getColumnB() != null;
    //
    //        return indicator.getColumnA().getName() + "-->" + indicator.getColumnB().getName(); //$NON-NLS-1$
    // }

    @Deprecated
    private TableStructureEntity getTableStructure() {
        TableStructureEntity entity = new TableStructureEntity();
        entity.setFieldNames(new String[] {
                DefaultMessagesImpl.getString("ColumnDependencyState.Label"), DefaultMessagesImpl.getString("ColumnDependencyState.Match"), DefaultMessagesImpl.getString("ColumnDependencyState.%Match"), DefaultMessagesImpl.getString("ColumnDependencyState.rows") }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        entity.setFieldWidths(new Integer[] { 200, 85, 85, 85 });
        return entity;
    }

}
