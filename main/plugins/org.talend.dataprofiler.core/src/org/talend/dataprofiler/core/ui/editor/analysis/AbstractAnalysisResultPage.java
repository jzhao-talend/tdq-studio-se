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
import java.text.SimpleDateFormat;
import java.util.Date;

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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.IRuningStatusListener;
import org.talend.dataprofiler.core.ui.editor.AbstractFormPage;
import org.talend.dataprofiler.core.ui.editor.CommonFormEditor;
import org.talend.dataprofiler.core.ui.editor.analysis.drilldown.DrillDownEditorInput;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableFactory;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTableMenuGenerator;
import org.talend.dataprofiler.core.ui.editor.preview.model.MenuItemEntity;
import org.talend.dataprofiler.core.ui.utils.SqlExplorerUtils;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.AnalyzedDataSet;
import org.talend.dataquality.analysis.ExecutionLanguage;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.PatternFreqIndicator;
import org.talend.dataquality.indicators.PatternLowFreqIndicator;
import org.talend.dq.analysis.AnalysisHandler;
import org.talend.dq.analysis.explore.IDataExplorer;
import org.talend.dq.dbms.DbmsLanguageFactory;
import org.talend.dq.indicators.preview.table.ChartDataEntity;
import org.talend.dq.pattern.PatternTransformer;

/**
 * DOC rli class global comment. Detailled comment
 */
public abstract class AbstractAnalysisResultPage extends AbstractFormPage implements IRuningStatusListener {

    /**
     * width hint for tables in data area. ADDED sgandon 15/03/2010 bug 11769 : setup the size of the table to avoid
     * crash and add consistency.
     */
    // private static final int TABLE_WIDTH_HINT = 1100;

    /**
     * size in rows of a small table (that has less than TABLE_MIN_ROW_LIMIT rows). ADDED sgandon 15/03/2010 bug 11769 :
     * setup the size of the table to avoid crash and add consistency.
     */
    private static final int SMALL_TABLE_NUM_ROWS = 10;

    /**
     * size in rows of a big table (that has more than TABLE_MIN_ROW_LIMIT rows). ADDED sgandon 15/03/2010 bug 11769 :
     * setup the size of the table to avoid crash and add consistency.
     */
    private static final int BIG_TABLE_NUM_ROWS = 30;

    /**
     * limit of row to create a small table. ADDED sgandon 15/03/2010 bug 11769 : setup the size of the table to avoid
     * crash and add consistency.
     */
    private static final int TABLE_MIN_ROW_LIMIT = 10;

    protected ScrolledForm form;

    protected Composite topComposite;

    protected Composite summaryComp;

    protected Section summarySection = null;

    public AbstractAnalysisResultPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        currentEditor = (CommonFormEditor) editor;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        this.form = managedForm.getForm();
        this.form.setText(DefaultMessagesImpl.getString("AbstractAnalysisResultPage.analysisResult")); //$NON-NLS-1$
        topComposite = form.getBody();
        topComposite.setLayout(new GridLayout());
        summaryComp = toolkit.createComposite(topComposite);
        summaryComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        summaryComp.setLayout(new GridLayout());
        createSummarySection(form, summaryComp, getAnalysisHandler());
    }

    protected abstract AnalysisHandler getAnalysisHandler();

    protected void createSummarySection(ScrolledForm form, Composite parent, AnalysisHandler analysisHandler) {
        summarySection = createSection(form, parent,
                DefaultMessagesImpl.getString("AbstractAnalysisResultPage.analysisSummary"), null); //$NON-NLS-1$
        Composite sectionClient = toolkit.createComposite(summarySection);
        sectionClient.setLayout(new GridLayout(2, false));
        sectionClient.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite databaseComp = toolkit.createComposite(sectionClient);
        databaseComp.setLayout(new GridLayout(2, false));
        GridData databaseCompData = new GridData(GridData.FILL_HORIZONTAL);
        databaseCompData.verticalAlignment = GridData.BEGINNING;
        databaseComp.setLayoutData(databaseCompData);

        toolkit.createLabel(databaseComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.connection")); //$NON-NLS-1$
        toolkit.createLabel(databaseComp, analysisHandler.getConnectionName());
        if (analysisHandler.isCatalogExisting()) {
            toolkit.createLabel(databaseComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.catalog")); //$NON-NLS-1$
            toolkit.createLabel(databaseComp, analysisHandler.getCatalogNames());
        }

        if (analysisHandler.isSchemaExisting()) {
            toolkit.createLabel(databaseComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.schema")); //$NON-NLS-1$
            toolkit.createLabel(databaseComp, analysisHandler.getSchemaNames());
        }

        toolkit.createLabel(databaseComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.table")); //$NON-NLS-1$
        toolkit.createLabel(databaseComp, analysisHandler.getTableNames());

        // bug 10541 fix by zshen,Change some character set to be proper to add view in the table anasys
        toolkit.createLabel(databaseComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.view"));//$NON-NLS-1$
        toolkit.createLabel(databaseComp, analysisHandler.getViewNames());

        Composite executionComp = toolkit.createComposite(sectionClient);
        executionComp.setLayout(new GridLayout(2, false));
        GridData executionCompData = new GridData(GridData.FILL_HORIZONTAL);
        executionCompData.verticalAlignment = GridData.BEGINNING;
        executionComp.setLayoutData(executionCompData);
        toolkit.createLabel(executionComp,
                DefaultMessagesImpl.getString("ConnectionMasterDetailsPage.createionDate", PluginConstant.EMPTY_STRING)); //$NON-NLS-1$
        toolkit.createLabel(executionComp, getFormatDateStr(analysisHandler.getAnalysis().getCreationDate()));
        toolkit.createLabel(executionComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.executionDate")); //$NON-NLS-1$
        toolkit.createLabel(executionComp, getFormatDateStr(analysisHandler.getAnalysis().getResults().getResultMetadata()
                .getExecutionDate()));
        toolkit.createLabel(executionComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.executionDuration")); //$NON-NLS-1$
        toolkit.createLabel(executionComp, analysisHandler.getExecuteDuration());
        toolkit.createLabel(executionComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.executionStatus")); //$NON-NLS-1$
        if (analysisHandler.getResultMetadata().isLastRunOk()) {
            toolkit.createLabel(executionComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.success")); //$NON-NLS-1$
        } else {
            // MOD msjian TDQ-5119 2012-12-24: the "execution status" should not display an error message when the
            // analysis is not executed yet
            String errMessage = PluginConstant.EMPTY_STRING;
            if (analysisHandler.getResultMetadata().getExecutionNumber() != 0) {
                errMessage = DefaultMessagesImpl.getString("AbstractAnalysisResultPage.failure") + analysisHandler.getResultMetadata().getMessage(); //$NON-NLS-1$
            }
            toolkit.createLabel(executionComp, errMessage).setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
            // TDQ-5119~
        }

        toolkit.createLabel(executionComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.numberOfExecution")); //$NON-NLS-1$
        toolkit.createLabel(executionComp, analysisHandler.getExecuteNumber());
        toolkit.createLabel(executionComp, DefaultMessagesImpl.getString("AbstractAnalysisResultPage.lastSucessfulExecution")); //$NON-NLS-1$
        toolkit.createLabel(executionComp, analysisHandler.getLastExecutionNumberOk());

        summarySection.setClient(sectionClient);
    }

    private String getFormatDateStr(Date date) {
        if (date == null) {
            return PluginConstant.EMPTY_STRING;
        }
        String format = SimpleDateFormat.getDateTimeInstance().format(date);
        return format;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.IRuningStatusListener#fireRuningItemChanged(boolean)
     */
    public void fireRuningItemChanged(boolean status) {
        IEditorPart editor = CorePlugin.getDefault().getCurrentActiveEditor();
        if (editor instanceof AnalysisEditor && status) {
            refresh(((AnalysisEditor) editor).getMasterPage());
        }
    }

    public abstract void refresh(AbstractAnalysisMetadataPage masterPage);

    protected abstract void createResultSection(Composite parent);

    /**
     * setup the Grid Layout Data to limit the vertical size of the table according to the numOfRows. if (numOfRows <=
     * 10) then table will be 10 rows height, if (numOfRows > 10) then the table will be 30 rows heigth
     * 
     * @param table the table to set the GridData value on
     * @param numOfRows number of rows in the table ADDED sgandon 15/03/2010 bug 11769 : setup the size of the table to
     * avoid crash and add consistency.
     */
    protected void setupTableGridDataLimitedSize(Table table, int numOfRows) {
        int itemHeight = table.getItemHeight();
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = numOfRows > TABLE_MIN_ROW_LIMIT ? BIG_TABLE_NUM_ROWS * itemHeight : SMALL_TABLE_NUM_ROWS * itemHeight;

        // MOD yyi compute table width
        // int tableWidth = 0;
        // for (TableColumn column : table.getColumns()) {
        // tableWidth += column.getWidth() + 50;
        // }
        // data.widthHint = TABLE_WIDTH_HINT > tableWidth ? tableWidth : TABLE_WIDTH_HINT;
        table.setLayoutData(data);
    }

    /**
     * 
     * Add qiongli :Extract it and used by subClass(feature 19192).
     * 
     * @param chartComp
     * @param explorer
     * @param analysis
     */
    protected void addMouseListenerForChart(final ChartComposite chartComp, final IDataExplorer explorer, final Analysis analysis) {
        chartComp.addChartMouseListener(new ChartMouseListener() {

            @SuppressWarnings("unchecked")
            public void chartMouseClicked(ChartMouseEvent event) {
                boolean flag = event.getTrigger().getButton() != MouseEvent.BUTTON3;

                chartComp.setDomainZoomable(flag);
                chartComp.setRangeZoomable(flag);

                final ExecutionLanguage currentEngine = analysis.getParameters().getExecutionLanguage();
                if (flag) {
                    return;
                }

                ChartEntity chartEntity = event.getEntity();
                if (chartEntity != null && chartEntity instanceof CategoryItemEntity) {
                    CategoryItemEntity cateEntity = (CategoryItemEntity) chartEntity;
                    ICustomerDataset dataEntity = (ICustomerDataset) cateEntity.getDataset();

                    final ChartDataEntity currentDataEntity = getCurrentChartDateEntity(cateEntity, dataEntity);

                    if (currentDataEntity != null) {
                        // MOD gdbu 2011-7-12 bug : 22524
                        if (!analysis.getParameters().isStoreData()) {
                            return;
                        }
                        if (0 == analysis.getResults().getIndicToRowMap().size()) {
                            return;
                        }
                        // ~22524
                        // create menu
                        Menu menu = new Menu(chartComp.getShell(), SWT.POP_UP);
                        chartComp.setMenu(menu);

                        final Indicator currentIndicator = currentDataEntity.getIndicator();
                        AnalyzedDataSet analyDataSet = analysis.getResults().getIndicToRowMap().get(currentIndicator);
                        if (analyDataSet == null || analyDataSet.getData() == null || analyDataSet.getData().size() == 0) {
                            return;
                        }

                        int createPatternFlag = 0;
                        MenuItemEntity[] itemEntities = ChartTableMenuGenerator.generate(explorer, analysis, currentDataEntity);
                        for (final MenuItemEntity itemEntity : itemEntities) {
                            MenuItem item = new MenuItem(menu, SWT.PUSH);
                            item.setText(itemEntity.getLabel());
                            item.setImage(itemEntity.getIcon());
                            item.addSelectionListener(new SelectionAdapter() {

                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    if (ExecutionLanguage.JAVA == currentEngine) {
                                        try {
                                            CorePlugin
                                                    .getDefault()
                                                    .getWorkbench()
                                                    .getActiveWorkbenchWindow()
                                                    .getActivePage()
                                                    .openEditor(
                                                            new DrillDownEditorInput(analysis, currentDataEntity, itemEntity),
                                                            "org.talend.dataprofiler.core.ui.editor.analysis.drilldown.drillDownResultEditor");//$NON-NLS-1$
                                        } catch (PartInitException e1) {
                                            e1.printStackTrace();
                                        }
                                    } else {
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
                                }
                            });

                            if ((currentIndicator instanceof PatternFreqIndicator || currentIndicator instanceof PatternLowFreqIndicator)
                                    && createPatternFlag == 0) {
                                MenuItem itemCreatePatt = new MenuItem(menu, SWT.PUSH);
                                itemCreatePatt.setText(DefaultMessagesImpl
                                        .getString("ColumnAnalysisResultPage.GenerateRegularPattern")); //$NON-NLS-1$
                                final PatternTransformer pattTransformer = new PatternTransformer(DbmsLanguageFactory
                                        .createDbmsLanguage(analysis));
                                itemCreatePatt.addSelectionListener(new SelectionAdapter() {

                                    @Override
                                    public void widgetSelected(SelectionEvent e) {
                                        Display.getDefault().asyncExec(new Runnable() {

                                            public void run() {
                                                ChartTableFactory.createPattern(analysis, itemEntity, pattTransformer);
                                            }
                                        });
                                    }
                                });
                            }

                            createPatternFlag++;
                        }

                        ChartTableFactory.addJobGenerationMenu(menu, analysis, currentIndicator);

                        menu.setVisible(true);
                    }
                }
            }

            /**
             * DOC xqliu Comment method "getCurrentChartDateEntity". bug 15745.
             * 
             * @param cateEntity
             * @param dataEntity
             * @return
             */
            @SuppressWarnings("unchecked")
            private ChartDataEntity getCurrentChartDateEntity(CategoryItemEntity cateEntity, ICustomerDataset dataEntity) {
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
                return currentDataEntity;
            }

            public void chartMouseMoved(ChartMouseEvent event) {
                // TODO Auto-generated method stub

            }

        });
    }

}
