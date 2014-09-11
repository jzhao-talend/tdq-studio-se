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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.model.TableIndicator;
import org.talend.dataprofiler.core.model.dynamic.DynamicIndicatorModel;
import org.talend.dataprofiler.core.ui.editor.composite.AbstractPagePart;
import org.talend.dataprofiler.core.ui.editor.composite.AnalysisTableTreeViewer;
import org.talend.dataprofiler.core.ui.editor.preview.CompositeIndicator;
import org.talend.dataprofiler.core.ui.editor.preview.TableIndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableFactory;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableMenuGenerator;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTypeStatesOperator;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartWithData;
import org.talend.dataprofiler.core.ui.editor.preview.model.MenuItemEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.IChartTypeStates;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.WhereRuleStatisticsStateTable;
import org.talend.dataprofiler.core.ui.events.DynamicChartEventReceiver;
import org.talend.dataprofiler.core.ui.events.EventEnum;
import org.talend.dataprofiler.core.ui.events.EventManager;
import org.talend.dataprofiler.core.ui.events.EventReceiver;
import org.talend.dataprofiler.core.ui.events.IEventReceiver;
import org.talend.dataprofiler.core.ui.events.TableDynamicChartEventReceiver;
import org.talend.dataprofiler.core.ui.pref.EditorPreferencePage;
import org.talend.dataprofiler.core.ui.utils.AnalysisUtils;
import org.talend.dataprofiler.core.ui.utils.SqlExplorerUtils;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dq.analysis.AnalysisHandler;
import org.talend.dq.analysis.explore.DataExplorer;
import org.talend.dq.analysis.explore.IDataExplorer;
import org.talend.dq.indicators.preview.EIndicatorChartType;
import org.talend.dq.indicators.preview.table.ChartDataEntity;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;
import orgomg.cwm.resource.relational.NamedColumnSet;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public class TableAnalysisResultPage extends AbstractAnalysisResultPage implements PropertyChangeListener {

    protected static Logger log = Logger.getLogger(TableAnalysisResultPage.class);

    private Composite resultComp;

    TableMasterDetailsPage masterPage;

    AnalysisTableTreeViewer tableTreeViewer;

    private Section resultSection = null;

    // Added TDQ-8787 20140617 yyin : store the temp indicator and its related dataset between one running
    private List<DynamicIndicatorModel> dynamicList = new ArrayList<DynamicIndicatorModel>();

    private Map<Indicator, EventReceiver> eventReceivers = new IdentityHashMap<Indicator, EventReceiver>();

    private EventReceiver registerDynamicRefreshEvent;

    private Composite sectionClient;

    // Added TDQ-9241
    private EventReceiver switchBetweenPageEvent;

    /**
     * DOC xqliu TableAnalysisResultPage constructor comment.
     * 
     * @param editor
     * @param id
     * @param title
     */
    public TableAnalysisResultPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        AnalysisEditor analysisEditor = (AnalysisEditor) editor;
        this.masterPage = (TableMasterDetailsPage) analysisEditor.getMasterPage();
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);

        resultComp = toolkit.createComposite(topComposite);
        resultComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        resultComp.setLayout(new GridLayout());
        createResultSection(resultComp);
        form.reflow(true);
    }

    @Override
    protected AnalysisHandler getAnalysisHandler() {
        return this.masterPage.getAnalysisHandler();
    }

    @Override
    protected void createResultSection(Composite parent) {

        // ADD gdbu 2011-3-4 bug 19242
        AbstractPagePart treeViewer = masterPage.getTreeViewer();
        if (treeViewer != null && treeViewer instanceof AnalysisTableTreeViewer) {
            tableTreeViewer = (AnalysisTableTreeViewer) treeViewer;
        }
        // ~

        resultSection = createSection(form, parent, DefaultMessagesImpl.getString("TableAnalysisResultPage.analysisResult"), null); //$NON-NLS-1$
        sectionClient = toolkit.createComposite(resultSection);
        sectionClient.setLayout(new GridLayout());
        sectionClient.setLayoutData(new GridData(GridData.FILL_BOTH));

        dynamicList.clear();
        for (final TableIndicator tableIndicator : tableTreeViewer.getTableIndicator()) {

            ExpandableComposite exComp = toolkit.createExpandableComposite(sectionClient, ExpandableComposite.TWISTIE
                    | ExpandableComposite.CLIENT_INDENT | ExpandableComposite.EXPANDED);
            // bug 10541 fix by zshen,Change some character set to be proper to add view in the table anasys
            if (tableIndicator.isTable()) {
                exComp.setText(DefaultMessagesImpl.getString(
                        "TableAnalysisResultPage.table", tableIndicator.getColumnSet().getName())); //$NON-NLS-1$
            } else {
                exComp.setText(DefaultMessagesImpl.getString(
                        "TableAnalysisResultPage.view", tableIndicator.getColumnSet().getName())); //$NON-NLS-1$
            }
            exComp.setLayout(new GridLayout());
            exComp.setLayoutData(new GridData(GridData.FILL_BOTH));

            // MOD xqliu 2009-06-23 bug 7481
            exComp.setExpanded(EditorPreferencePage.isUnfoldingAnalyzedEelements());
            // ~

            final Composite comp = toolkit.createComposite(exComp);
            comp.setLayout(new GridLayout());
            comp.setLayoutData(new GridData(GridData.FILL_BOTH));
            exComp.setClient(comp);

            createResultDataComposite(comp, tableIndicator);

            exComp.addExpansionListener(new ExpansionAdapter() {

                @Override
                public void expansionStateChanged(ExpansionEvent e) {

                    form.reflow(true);
                }

            });
        }

        resultSection.setClient(sectionClient);
    }

    private void createResultDataComposite(final Composite comp, final TableIndicator tableIndicator) {
        if (tableIndicator.getIndicators().length != 0) {

            final NamedColumnSet set = tableIndicator.getColumnSet();
            IRunnableWithProgress rwp = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

                    monitor.beginTask(
                            DefaultMessagesImpl.getString("TableAnalysisResultPage.createPreview", set.getName()), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {

                            Map<EIndicatorChartType, List<TableIndicatorUnit>> indicatorComposite = CompositeIndicator
                                    .getInstance().getTableIndicatorComposite(tableIndicator);
                            for (EIndicatorChartType chartType : indicatorComposite.keySet()) {
                                List<TableIndicatorUnit> units = indicatorComposite.get(chartType);
                                if (!units.isEmpty()) {
                                    IChartTypeStates chartTypeState = ChartTypeStatesOperator.getChartStateTable(chartType,
                                            units, tableIndicator);

                                    ChartWithData chartData = new ChartWithData(chartType, chartTypeState.getChart(),
                                            chartTypeState.getDataEntity());

                                    // create UI
                                    ExpandableComposite subComp = toolkit.createExpandableComposite(comp,
                                            ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT
                                                    | ExpandableComposite.EXPANDED);
                                    subComp.setText(chartData.getChartType().getLiteral());
                                    subComp.setLayoutData(new GridData(GridData.FILL_BOTH));

                                    // MOD xqliu 2009-06-23 bug 7481
                                    subComp.setExpanded(EditorPreferencePage.isUnfoldingIndicators());
                                    // ~

                                    final Composite composite = toolkit.createComposite(subComp, SWT.NULL);
                                    composite.setLayout(new GridLayout(2, false));
                                    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

                                    Composite tableTopComp = toolkit.createComposite(composite, SWT.NULL);
                                    tableTopComp.setLayout(new GridLayout(1, false));
                                    tableTopComp.setLayoutData(new GridData(GridData.FILL_BOTH));

                                    Analysis analysis = masterPage.getAnalysisHandler().getAnalysis();

                                    // create table for RownCountIndicator
                                    if (chartTypeState instanceof WhereRuleStatisticsStateTable) {
                                        WhereRuleStatisticsStateTable chartTypeStateWhereRule = (WhereRuleStatisticsStateTable) chartTypeState;
                                        ChartWithData chartDataRowCount = new ChartWithData(chartType, chartTypeStateWhereRule
                                                .getChart(), chartTypeStateWhereRule.getDataEntityRowCount());

                                        TableViewer tableviewerRowCount = chartTypeStateWhereRule
                                                .getTableFormRowCount(tableTopComp);
                                        tableviewerRowCount.setInput(chartDataRowCount);
                                        DataExplorer dataExplorerRownCount = chartTypeState.getDataExplorer();
                                        ChartTableFactory.addMenuAndTip(tableviewerRowCount, dataExplorerRownCount, analysis);

                                        // Added TDQ-8787 20140707 yyin: create and store the dynamic model for row
                                        // count's table
                                        List<Indicator> rowCount = new ArrayList<Indicator>();
                                        rowCount.add(chartTypeStateWhereRule.getRownCountUnit(units).getIndicator());
                                        DynamicIndicatorModel dyModel = AnalysisUtils.createDynamicModel(chartType, rowCount,
                                                null);
                                        dyModel.setTableViewer(tableviewerRowCount);

                                        dynamicList.add(dyModel);
                                        // ~
                                    }

                                    // create table for WhereRuleIndicator
                                    TableViewer tableviewer = chartTypeState.getTableForm(tableTopComp);
                                    tableviewer.setInput(chartData);
                                    DataExplorer dataExplorer = chartTypeState.getDataExplorer();
                                    ChartTableFactory.addMenuAndTip(tableviewer, dataExplorer, analysis);

                                    // Added TDQ-8787 20140707 yyin: create and store the dynamic model for all dq
                                    // rules's table
                                    if (chartTypeState instanceof WhereRuleStatisticsStateTable) {
                                        List<Indicator> allRules = new ArrayList<Indicator>();
                                        List<TableIndicatorUnit> removeRowCountUnit = ((WhereRuleStatisticsStateTable) chartTypeState)
                                                .removeRowCountUnit(units);
                                        for (TableIndicatorUnit indUnit : removeRowCountUnit) {
                                            allRules.add(indUnit.getIndicator());
                                        }
                                        DynamicIndicatorModel dyModel = AnalysisUtils.createDynamicModel(chartType, allRules,
                                                null);
                                        dyModel.setTableViewer(tableviewer);

                                        dynamicList.add(dyModel);
                                    }
                                    // ~

                                    Composite chartTopComp = toolkit.createComposite(composite, SWT.NULL);
                                    chartTopComp.setLayout(new GridLayout(1, false));
                                    chartTopComp.setLayoutData(new GridData(GridData.FILL_BOTH));

                                    if (!EditorPreferencePage.isHideGraphics()) {
                                        // get all indicator lists separated by chart, and only
                                        // WhereRuleStatisticsStateTable can get not-null charts
                                        List<List<Indicator>> pagedIndicators = ((WhereRuleStatisticsStateTable) chartTypeState)
                                                .getPagedIndicators();
                                        // Added TDQ-9241: for each list(for each chart), check if the current
                                        // list has been registered dynamic event
                                        List<DefaultCategoryDataset> datasets = new ArrayList<DefaultCategoryDataset>();
                                        for (List<Indicator> oneChart : pagedIndicators) {
                                            IEventReceiver event = EventManager.getInstance().findRegisteredEvent(
                                                    oneChart.get(0), EventEnum.DQ_DYMANIC_CHART, 0);
                                            if (event != null) {
                                                // get the dataset from the event
                                                DefaultCategoryDataset dataset = ((TableDynamicChartEventReceiver) event)
                                                        .getDataset();
                                                // if there has the dataset for the current rule, use it to replace,
                                                // (only happen when first switch from master to result page, during
                                                // one running)
                                                if (dataset != null) {
                                                    datasets.add(dataset);
                                                }

                                            }// ~
                                        }
                                        // create chart
                                        List<JFreeChart> charts = null;
                                        if (datasets.size() > 0) {
                                            charts = chartTypeState.getChartList(datasets);
                                        } else {
                                            charts = chartTypeState.getChartList();
                                        }
                                        if (charts != null) {

                                            int index = 0;
                                            for (JFreeChart chart2 : charts) {

                                                ChartComposite chartComp = new ChartComposite(chartTopComp, SWT.NONE, chart2,
                                                        true);
                                                // Added TDQ-8787 20140707 yyin: create and store the dynamic model for
                                                // each chart
                                                DynamicIndicatorModel dyModel = AnalysisUtils.createDynamicModel(chartType,
                                                        pagedIndicators.get(index++), chart2);
                                                dynamicList.add(dyModel);
                                                // ~
                                                GridData gd = new GridData();
                                                gd.widthHint = 550;
                                                gd.heightHint = 250;
                                                chartComp.setLayoutData(gd);

                                                addMouseListenerForChart(chartComp, dataExplorer, analysis);
                                            }
                                        }
                                    }

                                    subComp.setClient(composite);
                                    subComp.addExpansionListener(new ExpansionAdapter() {

                                        @Override
                                        public void expansionStateChanged(ExpansionEvent e) {
                                            form.reflow(true);
                                        }

                                    });
                                }
                            }
                        }

                    });

                    monitor.done();
                }

            };

            try {
                new ProgressMonitorDialog(this.getEditorSite().getShell()).run(true, false, rwp);
            } catch (Exception ex) {
                log.error(ex, ex);
            }
        }
    }

    @Override
    public void setDirty(boolean isDirty) {
        // no implementation
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (PluginConstant.ISDIRTY_PROPERTY.equals(evt.getPropertyName())) {
            ((AnalysisEditor) this.getEditor()).firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisResultPage#refresh(org.talend.dataprofiler.core
     * .ui.editor.analysis.AbstractAnalysisMetadataPage)
     */
    @Override
    public void refresh(AbstractAnalysisMetadataPage masterPage1) {
        this.masterPage = (TableMasterDetailsPage) masterPage1;

        disposeComposite();

        createFormContent(getManagedForm());
        masterPage1.refresh();

    }

    /**
     * DOC yyin Comment method "disposeComposite".
     */
    private void disposeComposite() {
        if (summaryComp != null && !summaryComp.isDisposed()) {
            summaryComp.dispose();
        }

        if (resultComp != null && !resultComp.isDisposed()) {
            resultComp.dispose();
        }
    }

    @Override
    protected void addMouseListenerForChart(final ChartComposite chartComp, final IDataExplorer explorer, final Analysis analysis) {
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
                    ICustomerDataset dataEntity = (ICustomerDataset) cateEntity.getDataset();

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
                        // create menu
                        Menu menu = new Menu(chartComp.getShell(), SWT.POP_UP);
                        chartComp.setMenu(menu);

                        final Indicator currentIndicator = currentDataEntity.getIndicator();
                        MenuItemEntity[] itemEntities = ChartTableMenuGenerator.generate(explorer, analysis, currentDataEntity);
                        for (final MenuItemEntity itemEntity : itemEntities) {
                            MenuItem item = new MenuItem(menu, SWT.PUSH);
                            item.setText(itemEntity.getLabel());
                            item.addSelectionListener(new SelectionAdapter() {

                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    Display.getDefault().asyncExec(new Runnable() {

                                        public void run() {
                                            Connection tdDataProvider = SwitchHelpers.CONNECTION_SWITCH.doSwitch(analysis
                                                    .getContext().getConnection());
                                            String query = itemEntity.getQuery();
                                            String editorName = currentIndicator.getName();
                                            SqlExplorerUtils.getDefault().runInDQViewer(tdDataProvider, query, editorName);
                                        }

                                    });
                                }
                            });
                        }

                        ChartTableFactory.addJobGenerationMenu(menu, analysis, currentIndicator);

                        menu.setVisible(true);
                    }
                }
            }

            public void chartMouseMoved(ChartMouseEvent event) {
                // no implementation
            }

        });
    }

    /**
     * Added TDQ-8787 20140613 yyin: create all charts before running, register each chart with its related indicator.
     */
    public void registerDynamicEvent() {
        disposeComposite();
        createFormContent(getManagedForm());
        // register dynamic event,for the indicator (for each column)
        for (DynamicIndicatorModel oneCategoryIndicatorModel : dynamicList) {
            CategoryDataset categoryDataset = oneCategoryIndicatorModel.getDataset();
            TableViewer tableViewer = oneCategoryIndicatorModel.getTableViewer();
            int index = 0;
            for (Indicator oneIndicator : oneCategoryIndicatorModel.getIndicatorList()) {
                TableDynamicChartEventReceiver eReceiver = new TableDynamicChartEventReceiver();
                eReceiver.setDataset(categoryDataset);
                eReceiver.setIndexInDataset(index++);
                eReceiver.setIndicatorName(oneIndicator.getName());
                eReceiver.setIndicator(oneIndicator);
                eReceiver.setIndicatorType(IndicatorEnum.findIndicatorEnum(oneIndicator.eClass()));
                eReceiver.setChartComposite(sectionClient);
                eReceiver.setTableViewer(tableViewer);
                // clear data
                eReceiver.clearValue();

                registerIndicatorEvent(oneIndicator, eReceiver);
            }
        }
        reLayoutChartComposite();

        registerRefreshDynamicChartEvent();
    }

    private void registerIndicatorEvent(Indicator oneIndicator, DynamicChartEventReceiver eReceiver) {
        eventReceivers.put(oneIndicator, eReceiver);
        EventManager.getInstance().register(oneIndicator, EventEnum.DQ_DYMANIC_CHART, eReceiver);
    }

    public void reLayoutChartComposite() {
        sectionClient.getParent().layout();
        sectionClient.layout();
    }

    /**
     * refresh the composite of the chart, to show the changes on the chart.
     */
    private void registerRefreshDynamicChartEvent() {
        registerDynamicRefreshEvent = new EventReceiver() {

            @Override
            public boolean handle(Object data) {
                reLayoutChartComposite();
                return true;
            }
        };
        EventManager.getInstance().register(sectionClient, EventEnum.DQ_DYNAMIC_REFRESH_DYNAMIC_CHART,
                registerDynamicRefreshEvent);

        // register a event to handle switch between master and result page
        switchBetweenPageEvent = new EventReceiver() {

            int times = 0;

            @Override
            public boolean handle(Object data) {
                if (times == 0) {
                    times++;
                    masterPage.refresh();
                }
                return true;
            }
        };
        EventManager.getInstance().register(masterPage.getAnalysis(), EventEnum.DQ_DYNAMIC_SWITCH_MASTER_RESULT_PAGE,
                switchBetweenPageEvent);
    }

    /**
     * unregister every dynamic events which registered before executing analysis
     * 
     * @param eventReceivers
     */
    public void unRegisterDynamicEvent() {
        // Added TDQ-9241
        EventManager.getInstance().unRegister(masterPage.getAnalysis(), EventEnum.DQ_DYNAMIC_SWITCH_MASTER_RESULT_PAGE,
                switchBetweenPageEvent);

        for (Indicator oneIndicator : eventReceivers.keySet()) {
            DynamicChartEventReceiver eventReceiver = (DynamicChartEventReceiver) eventReceivers.get(oneIndicator);
            eventReceiver.clear();
            EventManager.getInstance().clearEvent(oneIndicator, EventEnum.DQ_DYMANIC_CHART);
        }
        eventReceivers.clear();
        EventManager.getInstance().unRegister(sectionClient, EventEnum.DQ_DYNAMIC_REFRESH_DYNAMIC_CHART,
                registerDynamicRefreshEvent);

        for (DynamicIndicatorModel dyModel : dynamicList) {
            dyModel.clear();
        }
        dynamicList.clear();

        masterPage.clearDynamicDatasets();
    }

}
