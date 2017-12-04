package edu.stanford.bmir.protege.web.client.hierarchy;

import com.google.gwt.view.client.SelectionChangeEvent;
import edu.stanford.bmir.protege.web.client.Messages;
import edu.stanford.bmir.protege.web.client.action.UIAction;
import edu.stanford.bmir.protege.web.client.csv.CSVImportDialogController;
import edu.stanford.bmir.protege.web.client.csv.CSVImportViewImpl;
import edu.stanford.bmir.protege.web.client.csv.DocumentId;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassAction;
import edu.stanford.bmir.protege.web.client.dispatch.actions.CreateClassesAction;
import edu.stanford.bmir.protege.web.client.dispatch.actions.DeleteEntityAction;
import edu.stanford.bmir.protege.web.client.entity.CreateEntityDialogController;
import edu.stanford.bmir.protege.web.client.entity.CreateEntityInfo;
import edu.stanford.bmir.protege.web.client.library.dlg.WebProtegeDialog;
import edu.stanford.bmir.protege.web.client.library.msgbox.MessageBox;
import edu.stanford.bmir.protege.web.client.permissions.LoggedInUserProjectPermissionChecker;
import edu.stanford.bmir.protege.web.client.portlet.AbstractWebProtegePortletPresenter;
import edu.stanford.bmir.protege.web.client.portlet.PortletAction;
import edu.stanford.bmir.protege.web.client.portlet.PortletUi;
import edu.stanford.bmir.protege.web.client.primitive.PrimitiveDataEditor;
import edu.stanford.bmir.protege.web.client.progress.ProgressMonitor;
import edu.stanford.bmir.protege.web.client.search.SearchDialogController;
import edu.stanford.bmir.protege.web.client.upload.UploadFileDialogController;
import edu.stanford.bmir.protege.web.client.upload.UploadFileResultHandler;
import edu.stanford.bmir.protege.web.client.watches.WatchPresenter;
import edu.stanford.bmir.protege.web.shared.csv.CSVImportDescriptor;
import edu.stanford.bmir.protege.web.shared.entity.OWLClassData;
import edu.stanford.bmir.protege.web.shared.event.WebProtegeEventBus;
import edu.stanford.bmir.protege.web.shared.hierarchy.EntityHierarchyModel;
import edu.stanford.bmir.protege.web.shared.hierarchy.EntityHierarchyNode;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.selection.SelectionModel;
import edu.stanford.protege.gwt.graphtree.client.TreeWidget;
import edu.stanford.protege.gwt.graphtree.shared.Path;
import edu.stanford.protege.gwt.graphtree.shared.tree.impl.GraphTreeNodeModel;
import edu.stanford.webprotege.shared.annotations.Portlet;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.bmir.protege.web.client.events.UserLoggedInEvent.ON_USER_LOGGED_IN;
import static edu.stanford.bmir.protege.web.client.events.UserLoggedOutEvent.ON_USER_LOGGED_OUT;
import static edu.stanford.bmir.protege.web.client.library.dlg.DialogButton.CANCEL;
import static edu.stanford.bmir.protege.web.client.library.dlg.DialogButton.DELETE;
import static edu.stanford.bmir.protege.web.shared.access.BuiltInAction.*;
import static edu.stanford.bmir.protege.web.shared.hierarchy.HierarchyId.CLASS_HIERARCHY;
import static edu.stanford.bmir.protege.web.shared.permissions.PermissionsChangedEvent.ON_PERMISSIONS_CHANGED;
import static edu.stanford.protege.gwt.graphtree.shared.tree.RevealMode.REVEAL_FIRST;
import static org.semanticweb.owlapi.model.EntityType.CLASS;


@SuppressWarnings("Convert2MethodRef")
@Portlet(id = "portlets.ClassHierarchy",
        title = "Class Hierarchy",
        tooltip = "Displays the class hierarchy as a tree.")
public class ClassHierarchyPortletPresenter extends AbstractWebProtegePortletPresenter {

    private final DispatchServiceManager dispatchServiceManager;

    private final LoggedInUserProjectPermissionChecker permissionChecker;

    private final WatchPresenter watchPresenter;

    private final Provider<PrimitiveDataEditor> primitiveDataEditorProvider;

    private final SearchDialogController searchDialogController;

    private final Messages messages;

    private final EntityHierarchyModel hierarchyModel;
    @Nonnull
    private final EntityHierarchyTreeNodeRenderer renderer;

    private final UIAction createClassAction;

    private final UIAction watchClassAction;

    private final UIAction deleteClassAction;

    private final UIAction searchAction;

    private final TreeWidget<EntityHierarchyNode, OWLEntity> treeWidget;

    private boolean transmittingSelectionFromTree = false;

    @Inject
    public ClassHierarchyPortletPresenter(@Nonnull final ProjectId projectId,
                                          @Nonnull SelectionModel selectionModel,
                                          @Nonnull DispatchServiceManager dispatchServiceManager,
                                          @Nonnull LoggedInUserProjectPermissionChecker permissionChecker,
                                          @Nonnull WatchPresenter watchPresenter,
                                          @Nonnull SearchDialogController searchDialogController,
                                          @Nonnull Provider<PrimitiveDataEditor> primitiveDataEditorProvider,
                                          @Nonnull Messages messages,
                                          @Nonnull EntityHierarchyModel hierarchyModel,
                                          @Nonnull TreeWidget<EntityHierarchyNode, OWLEntity> treeWidget,
                                          @Nonnull EntityHierarchyTreeNodeRenderer renderer) {
        super(selectionModel, projectId);
        this.dispatchServiceManager = checkNotNull(dispatchServiceManager);
        this.permissionChecker = checkNotNull(permissionChecker);
        this.watchPresenter = checkNotNull(watchPresenter);
        this.primitiveDataEditorProvider = checkNotNull(primitiveDataEditorProvider);
        this.searchDialogController = checkNotNull(searchDialogController);
        this.messages = checkNotNull(messages);
        this.hierarchyModel = checkNotNull(hierarchyModel);
        this.treeWidget = checkNotNull(treeWidget);
        this.renderer = checkNotNull(renderer);

        this.createClassAction = new PortletAction(messages.create(),
                                                    this::handleCreateSubClasses);

        this.deleteClassAction = new PortletAction(messages.delete(),
                                                 this::handleDeleteClass);

        this.watchClassAction = new PortletAction(messages.watch(),
                                                  this::handleEditWatches);

        this.searchAction = new PortletAction(messages.search(),
                                              this::handleSearch);

        this.treeWidget.addSelectionChangeHandler(this::transmitSelectionFromTree);
        EntityHierarchyContextMenuPresenter contextMenuPresenter = new EntityHierarchyContextMenuPresenter(messages,
                                                                                                           treeWidget,
                                                                                                           createClassAction,
                                                                                                           deleteClassAction);
        this.treeWidget.addContextMenuHandler(contextMenuPresenter::showContextMenu);


    }

    @Override
    protected void handleAfterSetEntity(Optional<OWLEntity> entityData) {
        setSelectionInTree(entityData);
    }

    @Override
    public void startPortlet(@Nonnull PortletUi portletUi,
                             @Nonnull WebProtegeEventBus eventBus) {
        portletUi.addAction(createClassAction);
        portletUi.addAction(deleteClassAction);
        portletUi.addAction(watchClassAction);
        portletUi.addAction(searchAction);
        portletUi.setWidget(treeWidget);

        eventBus.addProjectEventHandler(getProjectId(),
                                        ON_PERMISSIONS_CHANGED,
                                        event -> updateButtonStates());

        eventBus.addApplicationEventHandler(ON_USER_LOGGED_OUT,
                                            event -> updateButtonStates());

        eventBus.addApplicationEventHandler(ON_USER_LOGGED_IN,
                                            event -> updateButtonStates());

        updateButtonStates();

        hierarchyModel.start(eventBus, CLASS_HIERARCHY);
        treeWidget.setRenderer(renderer);
        treeWidget.setModel(GraphTreeNodeModel.create(hierarchyModel,
                                                      node -> node.getEntity()));


        setSelectionInTree(getSelectedEntity());
    }


    public void updateButtonStates() {
        createClassAction.setEnabled(false);
        deleteClassAction.setEnabled(false);
        watchClassAction.setEnabled(false);
        // Note that the following action handlers cause GWT compile problems if method references
        // are used for some reason
        permissionChecker.hasPermission(CREATE_CLASS,
                                        enabled -> createClassAction.setEnabled(enabled));
        permissionChecker.hasPermission(DELETE_CLASS,
                                        enabled -> deleteClassAction.setEnabled(enabled));
        permissionChecker.hasPermission(WATCH_CHANGES,
                                        enabled -> watchClassAction.setEnabled(enabled));
    }

    private void transmitSelectionFromTree(SelectionChangeEvent event) {
        try {
            transmittingSelectionFromTree = true;
            treeWidget.getFirstSelectedKey()
                      .ifPresent(sel -> getSelectionModel().setSelection(sel));
        } finally {
            transmittingSelectionFromTree = false;
        }
    }

    private void handleCreateClass(CreateClassesMode mode) {
        if (mode == CreateClassesMode.CREATE_SUBCLASSES) {
            handleCreateSubClasses();
        }
        else {
            createSubClassesByImportingCSVDocument();
        }
    }

    private void handleCreateSubClasses() {
        if (!treeWidget.getFirstSelectedKey().isPresent()) {
            showClassNotSelectedMessage();
            return;
        }
        WebProtegeDialog.showDialog(new CreateEntityDialogController(CLASS,
                                                                     this::createClasses,
                                                                     messages));
    }

    private void createClasses(CreateEntityInfo createEntityInfo) {
        final Optional<OWLClass> superCls = getSelectedTreeNodeClass();
        if (!superCls.isPresent()) {
            return;
        }
        final Set<String> browserTexts = createEntityInfo.getBrowserTexts().stream()
                                                         .filter(browserText -> !browserText.trim().isEmpty())
                                                         .collect(Collectors.toSet());
        if (browserTexts.size() > 1) {
            dispatchServiceManager.execute(new CreateClassesAction(
                                                   getProjectId(),
                                                   superCls.get(),
                                                   browserTexts),
                                           result -> {
                                               if (!result.getCreatedClasses().isEmpty()) {
                                                   List<OWLEntity> path = new ArrayList<>();
                                                   path.addAll(result.getSuperClassPathToRoot().getPath());
                                                   path.add(result.getCreatedClasses().get(0).getEntity());
                                                   Path<OWLEntity> entityPath = new Path<>(path);
                                                   selectAndExpandPath(entityPath);
                                               }
                                           });
        }
        else {
            dispatchServiceManager.execute(new CreateClassAction(
                                                   getProjectId(),
                                                   browserTexts.iterator().next(),
                                                   superCls.get()),
                                           result -> {
                                               Path<OWLEntity> path = new Path<>(new ArrayList<>(result.getPathToRoot().getPath()));
                                               selectAndExpandPath(path);
                                           });
        }
    }

    private void handleSearch() {
        searchDialogController.setEntityTypes(CLASS);
        WebProtegeDialog.showDialog(searchDialogController);
    }

    private void selectAndExpandPath(Path<OWLEntity> entityPath) {
        treeWidget.setSelected(entityPath, true, () -> treeWidget.setExpanded(entityPath));
    }

    private void createSubClassesByImportingCSVDocument() {
        final Optional<OWLClass> selCls = getSelectedTreeNodeClass();
        if (!selCls.isPresent()) {
            return;
        }
        UploadFileResultHandler uploadResultHandler = new UploadFileResultHandler() {
            @Override
            public void handleFileUploaded(final DocumentId fileDocumentId) {
                WebProtegeDialog<CSVImportDescriptor> csvImportDialog = new WebProtegeDialog<>(
                        new CSVImportDialogController(
                                getProjectId(),
                                fileDocumentId,
                                selCls.get(),
                                dispatchServiceManager,
                                new CSVImportViewImpl(
                                        primitiveDataEditorProvider)));
                csvImportDialog.setVisible(true);

            }

            @Override
            public void handleFileUploadFailed(
                    String errorMessage) {
                ProgressMonitor.get().hideProgressMonitor();
                MessageBox.showAlert("Error uploading CSV file", errorMessage);
            }
        };
        UploadFileDialogController controller = new UploadFileDialogController("Upload CSV",
                                                                               uploadResultHandler);

        WebProtegeDialog.showDialog(controller);
    }


    private Optional<OWLClassData> getSelectedTreeNodeClassData() {
        return treeWidget.getSelectedNodes().stream()
                         .map(tn -> (OWLClassData) tn.getUserObject().getEntityData())
                         .findFirst();
    }

    private void handleDeleteClass() {
        final Optional<OWLClassData> currentSelection = getSelectedTreeNodeClassData();
        if (!currentSelection.isPresent()) {
            showClassNotSelectedMessage();
            return;
        }

        final OWLClassData theClassData = currentSelection.get();
        final String displayName = theClassData.getBrowserText();
        String subMessage = messages.delete_class_msg(displayName);
        MessageBox.showConfirmBox(messages.delete_class_title(),
                                  subMessage,
                                  CANCEL, DELETE,
                                  () -> deleteCls(theClassData.getEntity()),
                                  CANCEL);
    }

    private void showClassNotSelectedMessage() {
        MessageBox.showAlert("No class selected", "Please select a class.");
    }

    private void deleteCls(final OWLClass cls) {
        dispatchServiceManager.execute(new DeleteEntityAction(cls, getProjectId()), deleteEntityResult -> {});
    }

    protected void handleEditWatches() {
        final Optional<OWLClass> sel = getSelectedTreeNodeClass();
        if (!sel.isPresent()) {
            return;
        }
        watchPresenter.showDialog(sel.get());
    }


    /**
     * Gets the selected class.
     * @return The selected class.
     */
    private Optional<OWLClass> getSelectedTreeNodeClass() {
        Optional<OWLClassData> currentSelection = getSelectedTreeNodeClassData();
        return currentSelection.map(OWLClassData::getEntity);
    }

    private void setSelectionInTree(Optional<OWLEntity> selection) {
        if (transmittingSelectionFromTree) {
            return;
        }
        selection.ifPresent(sel -> {
            if (sel.isOWLClass()) {
                treeWidget.revealTreeNodesForKey(sel, REVEAL_FIRST);
            }
        });
    }

    private enum CreateClassesMode {

        CREATE_SUBCLASSES,
        IMPORT_CSV
    }

}
