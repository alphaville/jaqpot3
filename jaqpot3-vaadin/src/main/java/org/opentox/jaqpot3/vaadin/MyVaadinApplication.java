package org.opentox.jaqpot3.vaadin;

import com.vaadin.Application;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.SplitPanel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class MyVaadinApplication extends Application {

    private Button newContact = new Button("Add contact");
    private Button search = new Button("Search");
    private Button share = new Button("Share");
    private Button help = new Button("Help");
    private SplitPanel horizontalSplit = new SplitPanel(
            SplitPanel.ORIENTATION_HORIZONTAL);

    @Override
    public void init() {
        buildMainLayout();
        setTheme("runo");
    }

    private void buildMainLayout() {
        setMainWindow(new Window("Address Book Demo application"));
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        layout.addComponent(createToolbar());
        layout.addComponent(horizontalSplit);
        /* Allocate all available extra space to the horizontal split panel */
        layout.setExpandRatio(horizontalSplit, 1);
        /* Set the initial split position so we can have a 200 pixel menu to the left */
        horizontalSplit.setSplitPosition(200, SplitPanel.UNITS_PIXELS);
        getMainWindow().setContent(layout);
    }

    public HorizontalLayout createToolbar() {
        HorizontalLayout lo = new HorizontalLayout();
        lo.addComponent(newContact);
        lo.addComponent(search);
        lo.addComponent(share);
        lo.addComponent(help);
        return lo;
    }
}
