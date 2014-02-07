package com.github.wolfie.history.tabledemo;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Table;

public class TableView extends CustomComponent {

    public interface TableSelectionListener {
        void tableSelectionChanged(MyPojo selectedPojo);
    }

    private final HorizontalSplitPanel splitpanel = new HorizontalSplitPanel();
    private final Table table = new Table();
    private final DetailsView detailsView = new DetailsView();

    private final TableSelectionListener listener;

    private final ValueChangeListener tableValueChangeListener = new ValueChangeListener() {
        @Override
        public void valueChange(final ValueChangeEvent event) {
            final MyPojo newSelection = (MyPojo) event.getProperty().getValue();
            detailsView.display(newSelection);

            listener.tableSelectionChanged(newSelection);
        }
    };

    public TableView(TableSelectionListener listener) {
        this.listener = listener;

        setSizeFull();

        splitpanel.setSizeFull();
        splitpanel.setSplitPosition(400f, Unit.PIXELS, true);
        setCompositionRoot(splitpanel);

        table.setSizeFull();
        table.setSelectable(true);
        table.addValueChangeListener(tableValueChangeListener);
        table.setContainerDataSource(DemoData.generate());
        table.setImmediate(true);
        table.setColumnWidth("id", 20);
        table.setColumnWidth("name", 100);
        table.setVisibleColumns("id", "name", "description");
        splitpanel.setFirstComponent(table);

        splitpanel.setSecondComponent(detailsView);
    }

    private MyPojo findPojoById(final int id) {
        @SuppressWarnings("unchecked")
        final BeanItemContainer<MyPojo> container = ((BeanItemContainer<MyPojo>) table
                .getContainerDataSource());
        for (final MyPojo bean : container.getItemIds()) {
            if (bean.getId() == id) {
                return bean;
            }
        }
        return null;
    }

    public void display(int pojoId) {
        detailsView.display(findPojoById(pojoId));
    }

    public MyPojo getSelected() {
        return (MyPojo) table.getValue();
    }

    public void select(int pojoId) {
        MyPojo pojo = findPojoById(pojoId);
        table.select(pojo);
    }
}
