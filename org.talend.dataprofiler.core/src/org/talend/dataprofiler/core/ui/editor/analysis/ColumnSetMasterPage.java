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
package org.talend.dataprofiler.core.ui.editor.analysis;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.jfree.chart.JFreeChart;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.cwm.helper.ColumnHelper;
import org.talend.cwm.helper.DataProviderHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.relational.TdColumn;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.helper.ModelElementIndicatorHelper;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.model.ModelElementIndicator;
import org.talend.dataprofiler.core.ui.action.actions.RunAnalysisAction;
import org.talend.dataprofiler.core.ui.chart.ChartDecorator;
import org.talend.dataprofiler.core.ui.dialog.ColumnsSelectionDialog;
import org.talend.dataprofiler.core.ui.editor.composite.AnalysisColumnSetTreeViewer;
import org.talend.dataprofiler.core.ui.editor.composite.DataFilterComp;
import org.talend.dataprofiler.core.ui.editor.composite.IndicatorsComp;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartTypeStatesOperator;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartWithData;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.IChartTypeStates;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.ExecutionLanguage;
import org.talend.dataquality.exception.DataprofilerCoreException;
import org.talend.dataquality.helpers.MetadataHelper;
import org.talend.dataquality.indicators.CompositeIndicator;
import org.talend.dataquality.indicators.DataminingType;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.IndicatorsFactory;
import org.talend.dataquality.indicators.RegexpMatchingIndicator;
import org.talend.dataquality.indicators.columnset.AllMatchIndicator;
import org.talend.dataquality.indicators.columnset.ColumnsetFactory;
import org.talend.dataquality.indicators.columnset.SimpleStatIndicator;
import org.talend.dq.analysis.ColumnSetAnalysisHandler;
import org.talend.dq.helper.ProxyRepositoryViewObject;
import org.talend.dq.helper.resourcehelper.AnaResourceFileHelper;
import org.talend.dq.indicators.definitions.DefinitionHandler;
import org.talend.dq.indicators.preview.EIndicatorChartType;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;
import org.talend.utils.sugars.ReturnCode;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * @author yyi 2009-12-16
 * 
 */
public class ColumnSetMasterPage extends AbstractAnalysisMetadataPage implements PropertyChangeListener {

    private static Logger log = Logger.getLogger(ColumnSetMasterPage.class);

    AnalysisEditor currentEditor;

    AnalysisColumnSetTreeViewer treeViewer;

    IndicatorsComp indicatorsViewer;

    DataFilterComp dataFilterComp;

    ColumnSetAnalysisHandler columnSetAnalysisHandler;

    private SimpleStatIndicator simpleStatIndicator;

    private AllMatchIndicator allMatchIndicator;

    protected String execLang;

    private String stringDataFilter;

    private Composite chartComposite;

    private ScrolledForm form;

    private static final int TREE_MAX_LENGTH = 300;

    private static final int INDICATORS_SECTION_HEIGHT = 300;

    protected Composite[] previewChartCompsites;

    private EList<ModelElement> analyzedColumns;

    private Section analysisColSection;

    private Section dataFilterSection;

    private Section previewSection;

    private Section indicatorsSection;

    private ModelElementIndicator[] currentModelElementIndicators;

    public ColumnSetMasterPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        currentEditor = (AnalysisEditor) editor;
    }

    public void initialize(FormEditor editor) {
        super.initialize(editor);
        recomputeIndicators();

    }

    public void recomputeIndicators() {
        columnSetAnalysisHandler = new ColumnSetAnalysisHandler();
        columnSetAnalysisHandler.setAnalysis((Analysis) this.currentModelElement);
        stringDataFilter = columnSetAnalysisHandler.getStringDataFilter();
        analyzedColumns = columnSetAnalysisHandler.getAnalyzedColumns();
        if (columnSetAnalysisHandler.getSimpleStatIndicator() == null) {
            ColumnsetFactory columnsetFactory = ColumnsetFactory.eINSTANCE;
            simpleStatIndicator = columnsetFactory.createSimpleStatIndicator();
            simpleStatIndicator.setRowCountIndicator(IndicatorsFactory.eINSTANCE.createRowCountIndicator());
            simpleStatIndicator.setDistinctCountIndicator(IndicatorsFactory.eINSTANCE.createDistinctCountIndicator());
            simpleStatIndicator.setDuplicateCountIndicator(IndicatorsFactory.eINSTANCE.createDuplicateCountIndicator());
            simpleStatIndicator.setUniqueCountIndicator(IndicatorsFactory.eINSTANCE.createUniqueCountIndicator());
        } else {
            simpleStatIndicator = (SimpleStatIndicator) columnSetAnalysisHandler.getSimpleStatIndicator();
        }
        if (columnSetAnalysisHandler.getAllmatchIndicator() == null) {
            ColumnsetFactory columnsetFactory = ColumnsetFactory.eINSTANCE;
            allMatchIndicator = columnsetFactory.createAllMatchIndicator();
            DefinitionHandler.getInstance().setDefaultIndicatorDefinition(allMatchIndicator);
        } else {
            allMatchIndicator = (AllMatchIndicator) columnSetAnalysisHandler.getAllmatchIndicator();
        }

        initializeIndicator(simpleStatIndicator);
        List<ModelElementIndicator> meIndicatorList = new ArrayList<ModelElementIndicator>();
        ModelElementIndicator currentIndicator;
        for (ModelElement element : analyzedColumns) {
            TdColumn tdColumn = SwitchHelpers.COLUMN_SWITCH.doSwitch(element);
            if (tdColumn == null) {
                continue;
            }
            MetadataHelper.setDataminingType(DataminingType.NOMINAL, tdColumn);

            currentIndicator = ModelElementIndicatorHelper.createModelElementIndicator(element);
            Collection<Indicator> indicatorList = columnSetAnalysisHandler.getRegexMathingIndicators(element);
            currentIndicator.setIndicators(indicatorList.toArray(new Indicator[indicatorList.size()]));
            meIndicatorList.add(currentIndicator);
        }
        currentModelElementIndicators = meIndicatorList.toArray(new ModelElementIndicator[meIndicatorList.size()]);

    }

    public ModelElementIndicator[] getCurrentModelElementIndicators() {
        return this.currentModelElementIndicators;
    }

    private void initializeIndicator(Indicator indicator) {
        if (indicator.getIndicatorDefinition() == null) {
            DefinitionHandler.getInstance().setDefaultIndicatorDefinition(indicator);
        }
        if (indicator instanceof CompositeIndicator) {
            for (Indicator child : ((CompositeIndicator) indicator).getChildIndicators()) {
                initializeIndicator(child); // recurse
            }
        }
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        this.form = managedForm.getForm();
        Composite body = form.getBody();

        body.setLayout(new GridLayout());
        final SashForm sForm = new SashForm(body, SWT.NULL);
        sForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        topComp = toolkit.createComposite(sForm);
        topComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        topComp.setLayout(new GridLayout());
        metadataSection = creatMetadataSection(form, topComp);
        metadataSection.setText(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.analysisMeta")); //$NON-NLS-1$
        metadataSection.setDescription(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.setPropOfAnalysis")); //$NON-NLS-1$

        form.setText(DefaultMessagesImpl.getString("ColumnSetMasterPage.title")); //$NON-NLS-1$

        createAnalysisColumnsSection(form, topComp);

        createIndicatorsSection(form, topComp);

        createDataFilterSection(form, topComp);

        // createAnalysisParamSection(form, topComp);

        Composite previewComp = toolkit.createComposite(sForm);
        previewComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        previewComp.setLayout(new GridLayout());
        // add by hcheng for 0007290: Chart cannot auto compute it's size in
        // DQRule analsyis Editor
        previewComp.addControlListener(new ControlAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse .swt.events.ControlEvent)
             */
            @Override
            public void controlResized(ControlEvent e) {
                super.controlResized(e);
                sForm.redraw();
                form.reflow(true);
            }
        });
        // ~
        createPreviewSection(form, previewComp);
    }

    /**
     * DOC yyi Comment method "createIndicatorsSection".
     * 
     * @param topComp
     * @param form
     */
    private void createIndicatorsSection(ScrolledForm form, Composite topComp) {
        indicatorsSection = createSection(form, topComp, "Indicators", null); //$NON-NLS-1$

        Composite indicatorsComp = toolkit.createComposite(indicatorsSection, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(indicatorsComp);
        indicatorsComp.setLayout(new GridLayout());
        ((GridData) indicatorsComp.getLayoutData()).heightHint = INDICATORS_SECTION_HEIGHT;

        indicatorsViewer = new IndicatorsComp(indicatorsComp, this);
        indicatorsViewer.setDirty(false);
        indicatorsViewer.addPropertyChangeListener(this);
        indicatorsViewer.setInput(simpleStatIndicator, allMatchIndicator);
        indicatorsSection.setClient(indicatorsComp);
    }

    void createAnalysisColumnsSection(final ScrolledForm form, Composite anasisDataComp) {
        analysisColSection = createSection(form, anasisDataComp, DefaultMessagesImpl
                .getString("ColumnMasterDetailsPage.analyzeColumn"), null); //$NON-NLS-1$

        Composite topComp = toolkit.createComposite(analysisColSection);
        topComp.setLayout(new GridLayout());
        // ~ MOD mzhao 2009-05-05,Bug 6587.
        createConnBindWidget(topComp);
        // ~
        Hyperlink clmnBtn = toolkit.createHyperlink(topComp, DefaultMessagesImpl
                .getString("ColumnMasterDetailsPage.selectColumn"), SWT.NONE); //$NON-NLS-1$
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(clmnBtn);
        clmnBtn.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                openColumnsSelectionDialog();
            }

        });

        Composite tree = toolkit.createComposite(topComp, SWT.None);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tree);
        tree.setLayout(new GridLayout());
        ((GridData) tree.getLayoutData()).heightHint = TREE_MAX_LENGTH;

        treeViewer = new AnalysisColumnSetTreeViewer(tree, this);
        treeViewer.addPropertyChangeListener(this);
        treeViewer.setInput(analyzedColumns.toArray());
        treeViewer.setDirty(false);
        analysisColSection.setClient(topComp);

    }

    /**
     * 
     */
    public void openColumnsSelectionDialog() {
        List<TdColumn> columnList = treeViewer.getColumnSetMultiValueList();
        if (columnList == null) {
            columnList = new ArrayList<TdColumn>();
        }
        ColumnsSelectionDialog dialog = new ColumnsSelectionDialog(
                this,
                null,
                DefaultMessagesImpl.getString("ColumnMasterDetailsPage.columnSelection"), columnList, DefaultMessagesImpl.getString("ColumnMasterDetailsPage.columnSelections")); //$NON-NLS-1$ //$NON-NLS-2$
        if (dialog.open() == Window.OK) {
            Object[] columns = dialog.getResult();
            treeViewer.setInput(columns);
            indicatorsViewer.setInput(simpleStatIndicator, allMatchIndicator);
            return;
        }
    }

    void createPreviewSection(final ScrolledForm form, Composite parent) {

        previewSection = createSection(
                form,
                parent,
                DefaultMessagesImpl.getString("ColumnMasterDetailsPage.graphics"), DefaultMessagesImpl.getString("ColumnMasterDetailsPage.space")); //$NON-NLS-1$ //$NON-NLS-2$
        previewSection.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite sectionClient = toolkit.createComposite(previewSection);
        sectionClient.setLayout(new GridLayout());
        sectionClient.setLayoutData(new GridData(GridData.FILL_BOTH));

        ImageHyperlink refreshBtn = toolkit.createImageHyperlink(sectionClient, SWT.NONE);
        refreshBtn.setText(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.refreshGraphics")); //$NON-NLS-1$
        refreshBtn.setImage(ImageLib.getImage(ImageLib.SECTION_PREVIEW));
        final Label message = toolkit.createLabel(sectionClient, DefaultMessagesImpl
                .getString("ColumnMasterDetailsPage.spaceWhite")); //$NON-NLS-1$
        message.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
        message.setVisible(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(sectionClient);

        chartComposite = toolkit.createComposite(sectionClient);
        chartComposite.setLayout(new GridLayout());
        chartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Analysis analysis = columnSetAnalysisHandler.getAnalysis();

        refreshBtn.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {

                for (Control control : chartComposite.getChildren()) {
                    control.dispose();
                }

                boolean analysisStatue = analysis.getResults().getResultMetadata() != null
                        && analysis.getResults().getResultMetadata().getExecutionDate() != null;

                if (!analysisStatue) {
                    boolean returnCode = MessageDialog.openConfirm(null, DefaultMessagesImpl
                            .getString("ColumnMasterDetailsPage.ViewResult"), //$NON-NLS-1$
                            DefaultMessagesImpl.getString("ColumnMasterDetailsPage.RunOrSeeSampleData")); //$NON-NLS-1$

                    if (returnCode) {
                        new RunAnalysisAction().run();
                        message.setVisible(false);
                    } else {
                        createPreviewCharts(form, chartComposite, false);
                        message.setText(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.warning")); //$NON-NLS-1$
                        message.setVisible(true);
                    }
                } else {
                    createPreviewCharts(form, chartComposite, true);
                }

                chartComposite.layout();
                form.reflow(true);
            }

        });

        previewSection.setClient(sectionClient);
    }

    public void createPreviewCharts(final ScrolledForm form, final Composite parentComp, final boolean isCreate) {
        Section previewSimpleStatSection = createSection(
                form,
                parentComp,
                DefaultMessagesImpl.getString("ColumnSetResultPage.SimpleStatistics"), DefaultMessagesImpl.getString("ColumnMasterDetailsPage.space")); //$NON-NLS-1$
        previewSimpleStatSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite sectionClient = toolkit.createComposite(previewSimpleStatSection);
        sectionClient.setLayout(new GridLayout());
        sectionClient.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite simpleComposite = toolkit.createComposite(sectionClient);
        simpleComposite.setLayout(new GridLayout(1, true));
        simpleComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        createSimpleStatistics(form, simpleComposite);
        previewSimpleStatSection.setClient(sectionClient);

        // match
        if (0 < allMatchIndicator.getCompositeRegexMatchingIndicators().size()) {

            Section previewMatchSection = createSection(form, parentComp, "All Match", ""); //$NON-NLS-1$
            previewMatchSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Composite sectionMatchClient = toolkit.createComposite(previewMatchSection);
            sectionMatchClient.setLayout(new GridLayout());
            sectionMatchClient.setLayoutData(new GridData(GridData.FILL_BOTH));

            Composite matchComposite = toolkit.createComposite(sectionMatchClient);
            matchComposite.setLayout(new GridLayout(1, true));
            matchComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

            createAllMatch(form, matchComposite);
            previewMatchSection.setClient(sectionMatchClient);
        }
    }

    private void createAllMatch(final ScrolledForm form, final Composite composite) {

        List<IndicatorUnit> units = new ArrayList<IndicatorUnit>();
        units.add(new IndicatorUnit(IndicatorEnum.AllMatchIndicatorEnum, allMatchIndicator, null));

        EIndicatorChartType matchingType = EIndicatorChartType.PATTERN_MATCHING;
        IChartTypeStates chartTypeState = ChartTypeStatesOperator.getChartState(matchingType, units);
        ChartWithData chartData = new ChartWithData(matchingType, chartTypeState.getChart(), chartTypeState.getDataEntity());

        JFreeChart chart = chartTypeState.getChart();
        ChartDecorator.decorate(chart);
        if (chart != null) {
            ChartComposite cc = new ChartComposite(composite, SWT.NONE, chart, true);

            GridData gd = new GridData();
            gd.widthHint = PluginConstant.CHART_STANDARD_WIDHT;
            gd.heightHint = PluginConstant.CHART_STANDARD_HEIGHT;
            cc.setLayoutData(gd);
        }
    }

    private void createSimpleStatistics(final ScrolledForm form, final Composite composite) {

        List<IndicatorUnit> units = new ArrayList<IndicatorUnit>();
        units.add(new IndicatorUnit(IndicatorEnum.RowCountIndicatorEnum, simpleStatIndicator.getRowCountIndicator(), null));
        units.add(new IndicatorUnit(IndicatorEnum.DistinctCountIndicatorEnum, simpleStatIndicator.getDistinctCountIndicator(),
                null));
        units.add(new IndicatorUnit(IndicatorEnum.DuplicateCountIndicatorEnum, simpleStatIndicator.getDuplicateCountIndicator(),
                null));
        units.add(new IndicatorUnit(IndicatorEnum.UniqueIndicatorEnum, simpleStatIndicator.getUniqueCountIndicator(), null));

        IChartTypeStates chartTypeState = ChartTypeStatesOperator.getChartState(EIndicatorChartType.SIMPLE_STATISTICS, units);

        // create chart
        JFreeChart chart = chartTypeState.getChart();
        ChartDecorator.decorate(chart);
        if (chart != null) {
            ChartComposite cc = new ChartComposite(composite, SWT.NONE, chart, true);

            GridData gd = new GridData();
            gd.widthHint = PluginConstant.CHART_STANDARD_WIDHT;
            gd.heightHint = PluginConstant.CHART_STANDARD_HEIGHT;
            cc.setLayoutData(gd);
        }
    }

    @Override
    public void refresh() {
        if (chartComposite != null) {
            try {
                for (Control control : chartComposite.getChildren()) {
                    control.dispose();
                }

                createPreviewCharts(form, chartComposite, true);
                chartComposite.layout();
                getForm().reflow(true);
            } catch (Exception ex) {
                log.error(ex, ex);
            }

        }
    }

    /**
     * @param form
     * @param toolkit
     * @param anasisDataComp
     */
    void createDataFilterSection(final ScrolledForm form, Composite anasisDataComp) {
        dataFilterSection = createSection(
                form,
                anasisDataComp,
                DefaultMessagesImpl.getString("ColumnMasterDetailsPage.dataFilter"), DefaultMessagesImpl.getString("ColumnMasterDetailsPage.editDataFilter")); //$NON-NLS-1$ //$NON-NLS-2$

        Composite sectionClient = toolkit.createComposite(dataFilterSection);
        dataFilterComp = new DataFilterComp(sectionClient, stringDataFilter);
        dataFilterComp.addPropertyChangeListener(this);
        dataFilterSection.setClient(sectionClient);
    }

    /**
     * @param outputFolder
     * @throws DataprofilerCoreException
     */

    public void saveAnalysis() throws DataprofilerCoreException {
        columnSetAnalysisHandler.clearAnalysis();
        simpleStatIndicator.getAnalyzedColumns().clear();
        allMatchIndicator.getAnalyzedColumns().clear();
        // set execute engine
        Analysis analysis = columnSetAnalysisHandler.getAnalysis();
        analysis.getParameters().setExecutionLanguage(ExecutionLanguage.JAVA);

        // set data filter
        columnSetAnalysisHandler.setStringDataFilter(dataFilterComp.getDataFilterString());

        // save analysis
        List<TdColumn> columnList = treeViewer.getColumnSetMultiValueList();

        Connection tdProvider = null;
        if (columnList != null && columnList.size() != 0) {
            tdProvider = DataProviderHelper.getTdDataProvider(SwitchHelpers.COLUMN_SWITCH.doSwitch(columnList.get(0)));
            if (tdProvider.eIsProxy()) {
                // Resolve the connection again
                tdProvider = ((ConnectionItem) ProxyRepositoryViewObject.getRepositoryViewObject(tdProvider).getProperty()
                        .getItem()).getConnection();
            }
            analysis.getContext().setConnection(tdProvider);
            simpleStatIndicator.getAnalyzedColumns().addAll(columnList);
            columnSetAnalysisHandler.addIndicator(columnList, simpleStatIndicator);
            // ~ MOD mzhao feature 13040. 2010-05-21
            allMatchIndicator.getCompositeRegexMatchingIndicators().clear();
            ModelElementIndicator[] modelElementIndicator = treeViewer.getModelElementIndicator();
            if (modelElementIndicator != null) {
                for (ModelElementIndicator modelElementInd : modelElementIndicator) {
                    Indicator[] inds = modelElementInd.getPatternIndicators();
                    for (Indicator ind : inds) {
                        if (ind instanceof RegexpMatchingIndicator) {
                            ind.setAnalyzedElement(modelElementInd.getModelElement());
                            allMatchIndicator.getCompositeRegexMatchingIndicators().add((RegexpMatchingIndicator) ind);
                        }
                    }
                }

            }
            if (allMatchIndicator.getCompositeRegexMatchingIndicators().size() > 0) {
                allMatchIndicator.getAnalyzedColumns().addAll(columnList);
                columnSetAnalysisHandler.addIndicator(columnList, allMatchIndicator);
            }
            // ~
        } else {
            analysis.getContext().setConnection(null);
            analysis.getClientDependency().clear();
        }

        String urlString = analysis.eResource() != null ? analysis.eResource().getURI().toFileString()
                : PluginConstant.EMPTY_STRING;
        // ADD xqliu 2010-07-19 bug 14014
        this.updateAnalysisClientDependency();
        // ~ 14014
        ReturnCode saved = AnaResourceFileHelper.getInstance().save(analysis);
        if (saved.isOk()) {
            if (tdProvider != null) {
                ProxyRepositoryViewObject.fetchAllDBRepositoryViewObjects(Boolean.TRUE);
                ProxyRepositoryViewObject.save(tdProvider);
            }

            if (log.isDebugEnabled()) {
                log.debug("Saved in  " + urlString + " successful");
            }
        } else {
            throw new DataprofilerCoreException(DefaultMessagesImpl.getString(
                    "ColumnMasterDetailsPage.problem", analysis.getName(), urlString, saved.getMessage())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        treeViewer.setDirty(false);
        dataFilterComp.setDirty(false);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (PluginConstant.ISDIRTY_PROPERTY.equals(evt.getPropertyName())) {
            currentEditor.firePropertyChange(IEditorPart.PROP_DIRTY);
        } else if (PluginConstant.DATAFILTER_PROPERTY.equals(evt.getPropertyName())) {
            this.columnSetAnalysisHandler.setStringDataFilter((String) evt.getNewValue());
        }

    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || (treeViewer != null && treeViewer.isDirty())
                || (dataFilterComp != null && dataFilterComp.isDirty())
                || (indicatorsViewer != null && indicatorsViewer.isDirty());
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.treeViewer != null) {
            this.treeViewer.removePropertyChangeListener(this);
        }
        if (dataFilterComp != null) {
            this.dataFilterComp.removePropertyChangeListener(this);
        }
    }

    /**
     * Getter for treeViewer.
     * 
     * @return the treeViewer
     */
    public AnalysisColumnSetTreeViewer getTreeViewer() {
        return this.treeViewer;
    }

    public ScrolledForm getForm() {
        return form;
    }

    public void setForm(ScrolledForm form) {
        this.form = form;
    }

    public ColumnSetAnalysisHandler getColumnSetAnalysisHandler() {
        return columnSetAnalysisHandler;
    }

    public SimpleStatIndicator getSimpleStatIndicator() {
        // simpleStatIndicator.getListRows();
        return simpleStatIndicator;
    }

    public Composite[] getPreviewChartCompsites() {
        return previewChartCompsites;
    }

    public Composite getChartComposite() {
        return chartComposite;
    }

    @Override
    protected ReturnCode canSave() {
        String message = null;
        List<TdColumn> columnSetMultiValueList = getTreeViewer().getColumnSetMultiValueList();

        if (!columnSetMultiValueList.isEmpty()) {
            if (!ColumnHelper.isFromSameTable(columnSetMultiValueList)) {
                message = DefaultMessagesImpl.getString("ColumnSetMasterPage.CannotCreateAnalysis"); //$NON-NLS-1$
            }
        }
        if (message == null) {
            resetResultPageData();
            return new ReturnCode(true);
        }

        return new ReturnCode(message, false);
    }

    @Override
    protected ReturnCode canRun() {
        List<TdColumn> columnSetMultiValueList = getTreeViewer().getColumnSetMultiValueList();
        if (columnSetMultiValueList.isEmpty()) {
            return new ReturnCode(DefaultMessagesImpl.getString("ColumnSetMasterPage.NoColumnsAssigned"), false); //$NON-NLS-1$
        }
        resetResultPageData();
        return new ReturnCode(true);

    }

    public AllMatchIndicator getAllMatchIndicator() {
        return allMatchIndicator;
    }

    public void updateIndicatorSection() {
        if (null != indicatorsViewer)
            indicatorsViewer.setInput(simpleStatIndicator, allMatchIndicator);
    }

    private void resetResultPageData() {
        ColumnSetResultPage theResultPage = null;
        if (this.currentEditor.getResultPage() instanceof ColumnSetResultPage) {
            theResultPage = (ColumnSetResultPage) this.currentEditor.getResultPage();
        }
        if (theResultPage.getTableFilterResult() != null) {
            theResultPage.setTableFilterResult(null);
        }
    }
}
