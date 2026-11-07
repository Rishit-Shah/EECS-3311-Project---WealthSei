package wealthsei.presentation.gui;

import java.awt.BorderLayout;

/** Stands in for a screen that is not built yet. */
@SuppressWarnings({"serial", "this-escape"})
public class PlaceholderView extends View {

    private final String title;
    private final String subtitle;
    private final String[] headline;

    public PlaceholderView(String title, String[] headline, String subtitle) {
        this.title = title;
        this.headline = headline;
        this.subtitle = subtitle;
        refresh();
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String[] headline() {
        return headline;
    }

    @Override
    public String subtitle() {
        return subtitle;
    }

    @Override
    public void refresh() {
        removeAll();
        Card card = new Card("Coming up next", title, null).minHeight(260);
        add(card, BorderLayout.CENTER);
        revalidate();
    }
}
