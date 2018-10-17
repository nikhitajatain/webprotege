package edu.stanford.bmir.protege.web.client.viz;

import com.google.gwt.user.client.ui.IsWidget;
import edu.stanford.bmir.protege.web.client.action.UIAction;
import edu.stanford.bmir.protege.web.client.graphlib.Graph;
import edu.stanford.bmir.protege.web.client.graphlib.NodeDetails;
import edu.stanford.protege.gwt.graphtree.client.SelectionChangeEvent;
import edu.stanford.protege.gwt.graphtree.client.SelectionChangeEvent.SelectionChangeHandler;
import elemental.dom.Element;

import javax.annotation.Nonnull;

import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 11 Oct 2018
 */
public interface VizView extends IsWidget {

    Element getSvgElement();

    void clearGraph();

    Optional<NodeDetails> getMostRecentTargetNode();

    interface DownloadHandler {
        void handleDownload();
    }

    void setGraph(Graph graph);

    void setLoadHandler(Runnable handler);

    void setDownloadHandler(@Nonnull DownloadHandler handler);

    boolean isVisible();

    interface SettingsChangedHandler {
        void handleSettingsChanged();
    }

    double getRankSpacing();

    void setSettingsChangedHandler(@Nonnull SettingsChangedHandler handler);

    @Nonnull
    TextMeasurer getTextMeasurer();

    double getScaleFactor();

    void setScaleFactor(double scaleFactor);

    void setNodeClickHandler(@Nonnull Consumer<NodeDetails> nodeClickHandler);

    void setNodeDoubleClickHandler(@Nonnull Consumer<NodeDetails> nodeDoubleClickHandler);

    void setNodeContextMenuClickHandler(@Nonnull Consumer<NodeDetails> nodeContextMenuClickHandler);

    void addContextMenuAction(@Nonnull UIAction uiAction);
}
