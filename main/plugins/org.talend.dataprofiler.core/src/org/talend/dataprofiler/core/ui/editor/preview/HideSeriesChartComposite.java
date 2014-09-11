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
package org.talend.dataprofiler.core.ui.editor.preview;

import java.awt.Color;
import java.awt.Paint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryTextAnnotation;
import org.jfree.chart.axis.CategoryAnchor;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.TextAnchor;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.cwm.helper.ColumnHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dataprofiler.common.ui.editor.preview.chart.ChartDatasetUtils;
import org.talend.dataprofiler.common.ui.editor.preview.chart.ChartDatasetUtils.ValueAggregator;
import org.talend.dataprofiler.common.ui.editor.preview.chart.ChartDecorator;
import org.talend.dataprofiler.common.ui.editor.preview.chart.TopChartFactory;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.chart.ChartUtils;
import org.talend.dataprofiler.core.ui.utils.SqlExplorerUtils;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.indicators.columnset.ColumnSetMultiValueIndicator;
import org.talend.dataquality.indicators.columnset.ColumnsetPackage;
import org.talend.dq.analysis.explore.MultiColumnSetValueExplorer;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * DOC bzhou class global comment. Detailled comment
 */
public class HideSeriesChartComposite extends ChartComposite {

    private static Logger log = Logger.getLogger(HideSeriesChartComposite.class);

    private ColumnSetMultiValueIndicator indicator;

    private ModelElement column;

    private JFreeChart chart;

    private boolean isNeedUtility;

    private static final String SERIES_KEY_ID = "SERIES_KEY"; //$NON-NLS-1$

    private Map<String, RowColumPair> hightlightSeriesMap = new HashMap<String, RowColumPair>();

    private Analysis analysis = null;

    public HideSeriesChartComposite(Composite comp, Analysis ana, ColumnSetMultiValueIndicator indicator, ModelElement column,
            boolean isNeedUtility) {
        super(comp, SWT.NONE);
        this.analysis = ana;
        this.indicator = indicator;
        this.column = column;
        this.isNeedUtility = isNeedUtility;
        this.setCursor(new Cursor(Display.getDefault(), SWT.CURSOR_HAND));
        this.setToolTipText("sdfsdf"); //$NON-NLS-1$

        createHideSeriesArea();

        addSpecifiedListeners();
        // by zshen for bug 14173: make the height to incream by itself when have more than 8 column to be choosed in
        // the analysis.
        GridData gd = new GridData();
        gd.heightHint = indicator.getAnalyzedColumns().size() * 30 < 230 ? 230 : indicator.getAnalyzedColumns().size() * 30;
        gd.widthHint = 460;
        this.setLayoutData(gd);
        // ~14173
    }

    private void addSpecifiedListeners() {

        // MOD mzhao 2009-03-30, Feature 6530, add view rows menu item.
        this.addChartMouseListener(new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent event) {

                setRangeZoomable(event.getTrigger().getButton() == 1);
                setDomainZoomable(event.getTrigger().getButton() == 1);

                if (event.getTrigger().getButton() != 3) {
                    return;
                }
                // create menu
                Menu menu = new Menu(getShell(), SWT.POP_UP);
                setMenu(menu);
                MenuItem itemShowInFullScreen = new MenuItem(menu, SWT.PUSH);
                itemShowInFullScreen.setText(DefaultMessagesImpl.getString("HideSeriesChartComposite.ShowInFullScreen")); //$NON-NLS-1$
                itemShowInFullScreen.addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Display.getDefault().asyncExec(new Runnable() {

                            public void run() {
                                ChartUtils.showChartInFillScreen(createChart(), indicator);
                            }
                        });
                    }
                });

                ChartEntity chartEntity = event.getEntity();
                if (chartEntity != null) {
                    if (ColumnsetPackage.eINSTANCE.getCountAvgNullIndicator().equals(indicator.eClass())) {
                        addMenuOnBubbleChart(chartEntity, menu);
                    } else if (ColumnsetPackage.eINSTANCE.getMinMaxDateIndicator().equals(indicator.eClass())) {
                        addMenuOnGantChart(chartEntity, menu);
                    }
                }
                menu.setVisible(true);
            }

            private void addMenuOnBubbleChart(ChartEntity chartEntity, Menu menu) {

                if (chartEntity instanceof XYItemEntity) {

                    XYItemEntity xyItemEntity = (XYItemEntity) chartEntity;

                    DefaultXYZDataset xyzDataSet = (DefaultXYZDataset) xyItemEntity.getDataset();
                    final Comparable<?> seriesKey = xyzDataSet.getSeriesKey(xyItemEntity.getSeriesIndex());
                    final String seriesK = String.valueOf(seriesKey);

                    try {
                        final Map<String, ValueAggregator> createXYZDatasets = ChartDatasetUtils.createXYZDatasets(indicator,
                                column);

                        final ValueAggregator valueAggregator = createXYZDatasets.get(seriesKey);

                        valueAggregator.addSeriesToXYZDataset(xyzDataSet, seriesK);
                        String seriesLabel = valueAggregator.getLabels(seriesK).get(xyItemEntity.getItem());
                        EList<ModelElement> nominalList = indicator.getNominalColumns();
                        final String queryString = MultiColumnSetValueExplorer.getInstance().getQueryStirng(column, analysis,
                                nominalList, seriesK, seriesLabel);

                        MenuItem item = new MenuItem(menu, SWT.PUSH);
                        item.setText(DefaultMessagesImpl.getString("HideSeriesChartComposite.ViewRow")); //$NON-NLS-1$
                        item.addSelectionListener(new SelectionAdapter() {

                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                Display.getDefault().asyncExec(new Runnable() {

                                    public void run() {
                                        Connection tdDataProvider = SwitchHelpers.CONNECTION_SWITCH.doSwitch(analysis
                                                .getContext().getConnection());
                                        String query = queryString;
                                        String editorName = ColumnHelper.getColumnSetOwner(column).getName();
                                        SqlExplorerUtils.getDefault().runInDQViewer(tdDataProvider, query, editorName);
                                    }

                                });
                            }

                        });

                    } catch (Throwable e) {
                        log.error(e, e);
                    }
                }
            }

            public void chartMouseMoved(ChartMouseEvent event) {

            }

        });
    }

    private void addMenuOnGantChart(ChartEntity chartEntity, Menu menu) {

        if (chartEntity instanceof CategoryItemEntity) {
            CategoryItemEntity itemEntity = (CategoryItemEntity) chartEntity;

            String seriesK = itemEntity.getRowKey().toString();
            String seriesLabel = itemEntity.getColumnKey().toString();
            EList<ModelElement> nominalList = indicator.getNominalColumns();
            final String sql = MultiColumnSetValueExplorer.getInstance().getQueryStirng(column, analysis, nominalList, seriesK,
                    seriesLabel);
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText(DefaultMessagesImpl.getString("HideSeriesChartComposite.ViewRows")); //$NON-NLS-1$
            item.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    Display.getDefault().asyncExec(new Runnable() {

                        public void run() {
                            Connection tdDataProvider = SwitchHelpers.CONNECTION_SWITCH.doSwitch(analysis.getContext()
                                    .getConnection());
                            String query = sql;
                            String editorName = ColumnHelper.getColumnSetOwner(column).getName();
                            SqlExplorerUtils.getDefault().runInDQViewer(tdDataProvider, query, editorName);
                        }

                    });
                }

            });
        }
    }

    private void createHideSeriesArea() {
        this.chart = createChart();

        if (chart != null) {
            setChart(chart);

            if (isNeedUtility) {

                createUtilityControl(this);
            }
        }
    }

    /**
     * DOC bzhou Comment method "createChart".
     * 
     * @return
     */
    private JFreeChart createChart() {
        JFreeChart jchart = null;

        if (ColumnsetPackage.eINSTANCE.getCountAvgNullIndicator().equals(indicator.eClass())) {
            jchart = TopChartFactory.createBubbleChart(indicator, column);
            ChartDecorator.decorate(jchart, null);
        }

        if (ColumnsetPackage.eINSTANCE.getMinMaxDateIndicator().equals(indicator.eClass())) {
            final int nbNominalColumns = indicator.getNominalColumns().size();
            final int nbDateFunctions = indicator.getDateFunctions().size();
            final int indexOfDateCol = indicator.getDateColumns().indexOf(column);
            assert indexOfDateCol != -1;

            jchart = TopChartFactory.createGanttChart(indicator, column);

            createAnnotOnGantt(jchart, indicator.getListRows(), nbNominalColumns + nbDateFunctions * indexOfDateCol + 3,
                    nbNominalColumns);

            CategoryPlot plot = (CategoryPlot) jchart.getPlot();
            CustomHideSeriesGanttRender renderer = new CustomHideSeriesGanttRender();
            renderer.setBaseToolTipGenerator(toolTipGenerator);
            plot.setRenderer(renderer);
            plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(10.0f);

            ChartDecorator.decorate(jchart, null);
        }

        if (ColumnsetPackage.eINSTANCE.getWeakCorrelationIndicator().equals(indicator.eClass())) {
            // create the chart
        }

        return jchart;
    }

    private void createUtilityControl(Composite parent) {
        Composite comp = new Composite(getParent(), SWT.BORDER);
        comp.setLayout(new GridLayout());
        comp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
        comp.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));

        if (ColumnsetPackage.eINSTANCE.getCountAvgNullIndicator().equals(indicator.eClass())) {
            XYDataset dataset = chart.getXYPlot().getDataset();
            int count = dataset.getSeriesCount();

            for (int i = 0; i < count; i++) {

                Button checkBtn = new Button(comp, SWT.CHECK);
                checkBtn.setText(dataset.getSeriesKey(i).toString());
                checkBtn.setSelection(true);
                checkBtn.addSelectionListener(listener);
                checkBtn.setData(SERIES_KEY_ID, i);
            }
        }

        if (ColumnsetPackage.eINSTANCE.getMinMaxDateIndicator().equals(indicator.eClass())) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            CategoryDataset dataset = plot.getDataset();
            int count = dataset.getRowCount();

            for (int i = 0; i < count; i++) {

                Button checkBtn = new Button(comp, SWT.CHECK);
                checkBtn.setText(dataset.getRowKey(i).toString());
                checkBtn.setSelection(true);
                checkBtn.addSelectionListener(listener);
                checkBtn.setData(SERIES_KEY_ID, i);
            }
        }

    }

    SelectionAdapter listener = new SelectionAdapter() {

        @Override
        public void widgetSelected(SelectionEvent e) {

            Button checkBtn = (Button) e.getSource();
            int seriesid = (Integer) checkBtn.getData(SERIES_KEY_ID);

            if (ColumnsetPackage.eINSTANCE.getCountAvgNullIndicator().equals(indicator.eClass())) {
                XYPlot plot = chart.getXYPlot();
                XYItemRenderer xyRenderer = plot.getRenderer();
                xyRenderer.setSeriesVisible(seriesid, checkBtn.getSelection());
            }

            if (ColumnsetPackage.eINSTANCE.getMinMaxDateIndicator().equals(indicator.eClass())) {
                CategoryPlot plot = (CategoryPlot) chart.getPlot();
                CategoryItemRenderer render = plot.getRenderer();
                render.setSeriesVisible(seriesid, checkBtn.getSelection());
            }
        }
    };

    CategoryToolTipGenerator toolTipGenerator = new CategoryToolTipGenerator() {

        public String generateToolTip(CategoryDataset dataset, int row, int column) {
            TaskSeriesCollection taskSeriesColl = (TaskSeriesCollection) dataset;
            List<Task> taskList = new ArrayList<Task>();
            for (int i = 0; i < taskSeriesColl.getSeriesCount(); i++) {
                for (int j = 0; j < taskSeriesColl.getSeries(i).getItemCount(); j++) {
                    taskList.add(taskSeriesColl.getSeries(i).get(j));
                }
            }
            Task task = taskList.get(column);
            // Task task = taskSeriesColl.getSeries(row).get(column);
            String taskDescription = task.getDescription();

            Date startDate = task.getDuration().getStart();
            Date endDate = task.getDuration().getEnd();
            return taskDescription + ",     " + startDate + "---->" + endDate; //$NON-NLS-1$ //$NON-NLS-2$
            // return "this is a tooltip";
        }
    };

    /**
     * 
     * DOC zhaoxinyi Comment method "createAnnotOnGantt".
     * 
     * @param chart
     * @param rowList
     * @param multiDateColumn
     */
    public void createAnnotOnGantt(JFreeChart chart, List<Object[]> rowList, int multiDateColumn, int nominal) {
        CategoryPlot xyplot = (CategoryPlot) chart.getPlot();
        CategoryTextAnnotation an;
        for (int seriesCount = 0; seriesCount < ((TaskSeriesCollection) xyplot.getDataset()).getSeriesCount(); seriesCount++) {
            int indexOfRow = 0;
            int columnCount = 0;
            for (int itemCount = 0; itemCount < ((TaskSeriesCollection) xyplot.getDataset()).getSeries(seriesCount)
                    .getItemCount(); itemCount++, columnCount++) {
                Task task = ((TaskSeriesCollection) xyplot.getDataset()).getSeries(seriesCount).get(itemCount);
                String taskDescription = task.getDescription();
                String[] taskArray = taskDescription.split("\\|"); //$NON-NLS-1$
                boolean isSameTime = task.getDuration().getStart().getTime() == task.getDuration().getEnd().getTime();
                if (!isSameTime && (rowList.get(indexOfRow))[multiDateColumn - 3] != null
                        && (rowList.get(indexOfRow))[multiDateColumn - 2] != null
                        && !((rowList.get(indexOfRow))[multiDateColumn]).equals(new BigDecimal(0L))) {
                    RowColumPair pair = new RowColumPair();
                    pair.setRow(seriesCount);
                    pair.setColumn(columnCount);
                    hightlightSeriesMap.put(String.valueOf(seriesCount) + String.valueOf(columnCount), pair);
                    an = new CategoryTextAnnotation("#nulls = " + (rowList.get(indexOfRow))[multiDateColumn], //$NON-NLS-1$
                            taskDescription, task.getDuration().getStart().getTime());
                    an.setTextAnchor(TextAnchor.CENTER_LEFT);
                    an.setCategoryAnchor(CategoryAnchor.MIDDLE);
                    xyplot.addAnnotation(an);
                }
                if (taskArray.length == nominal) {
                    indexOfRow++;

                    if (rowList.size() != indexOfRow
                            && ((rowList.get(indexOfRow))[multiDateColumn - 3] == null || (rowList.get(indexOfRow))[multiDateColumn - 2] == null)) {
                        indexOfRow++;
                    }
                }
            }
        }
    }

    /**
     * 
     * DOC zhaoxinyi HideSeriesPanel class global comment. Detailled comment
     */
    class CustomHideSeriesGanttRender extends HideSeriesGanttRenderer {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        /**
         * 
         * DOC zhaoxinyi CustomHideSeriesGantt constructor comment.
         * 
         * @param colors
         */
        @Override
        public Paint getItemPaint(int row, int column) {
            Paint itemPaint = super.getItemPaint(row, column);
            String key = String.valueOf(row) + String.valueOf(column);
            if (hightlightSeriesMap.get(key) != null && hightlightSeriesMap.get(key).getRow() == row
                    && hightlightSeriesMap.get(key).getColumn() == column) {
                return ((Color) itemPaint).brighter().brighter();
            }
            return itemPaint;
        }
    }

    /**
     * 
     * DOC zhaoxinyi class global comment. Detailled comment
     * 
     * FIXME this inner class should be static. Confirm and fix the error.
     */
    class RowColumPair {

        private int row;

        private int column;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getColumn() {
            return column;
        }

        public void setColumn(int column) {
            this.column = column;
        }

        @Override
        public String toString() {
            return "row = " + row + ", column = " + column; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

}
