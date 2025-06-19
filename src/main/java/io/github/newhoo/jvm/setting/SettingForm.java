package io.github.newhoo.jvm.setting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import io.github.newhoo.jvm.i18n.JvmParameterBundle;
import io.github.newhoo.jvm.util.AppUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SettingForm
 *
 * @author huzunrong
 * @since 1.0
 */
public class SettingForm {

    public JPanel mainPanel;
    private static final DataFlavor NODE_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "Node");

    private JLabel previewLabel;
    private JTextField jvmParameterText;
    private JPanel decorationPanel;

    private final Project project;

    private final MyJvmTableModel dataModel = new MyJvmTableModel();
    private TreeTable treeTable;
    private ListTreeTableModelOnColumns treeModel;
    private DefaultMutableTreeNode rootNode;

    public SettingForm(Project project) {
        this.project = project;

        init();
    }

    private void init() {
        previewLabel.setText(JvmParameterBundle.getMessage("label.jvm.parameter.preview"));
        previewLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 生成快捷按钮
                generateButton(e);
            }
        });
        jvmParameterText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    generateButton2(e);
                }
            }
        });

        initTreeTable();
        
        ToolbarDecorator decorationToolbar = ToolbarDecorator.createDecorator(treeTable)
                                                             .setAddAction(button -> {
                                                                 addNode();
                                                             })
                                                             .setRemoveAction(button -> {
                                                                 removeJvmParameter();
                                                             })
                                                             .addExtraAction(new AnAction(() -> JvmParameterBundle.getMessage("jvm.export.msg"), AllIcons.ToolbarDecorator.Export) {
                                                                 @Override
                                                                 public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
//                                                                     Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create();
//                                                                     String json = gson.toJson(dataModel.list);
//                                                                     CopyPasteManager.getInstance().setContents(new StringSelection(json));
                                                                 }
                                                             });
        decorationPanel.add(decorationToolbar.createPanel(), BorderLayout.CENTER);
    }

    private void initTreeTable() {
        ColumnInfo<DefaultMutableTreeNode, Boolean> enabledColumn = new ColumnInfo<DefaultMutableTreeNode, Boolean>("") {
            @Override
            public Class<?> getColumnClass() {
                return Boolean.class;
            }

            @Override
            public boolean isCellEditable(DefaultMutableTreeNode node) {
                return node.getUserObject() instanceof JvmParameter;
            }

            @Override
            public Boolean valueOf(DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof JvmParameter) {
                    return ((JvmParameter) node.getUserObject()).getEnabled();
                }
                return null;
            }

            @Override
            public void setValue(DefaultMutableTreeNode node, Boolean value) {
                if (node.getUserObject() instanceof JvmParameter) {
                    ((JvmParameter) node.getUserObject()).setEnabled(value);
                }
            }
        };

        ColumnInfo<DefaultMutableTreeNode, String> nameColumn = new ColumnInfo<DefaultMutableTreeNode, String>("Name") {
            @Override
            public boolean isCellEditable(DefaultMutableTreeNode node) {
                return true;
            }

            @Override
            public String valueOf(DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof JvmParameterGroup) {
                    return ((JvmParameterGroup) node.getUserObject()).getName();
                }
                if (node.getUserObject() instanceof JvmParameter) {
                    return ((JvmParameter) node.getUserObject()).getName();
                }
                return "";
            }

            @Override
            public void setValue(DefaultMutableTreeNode node, String value) {
                if (node.getUserObject() instanceof JvmParameterGroup) {
                    ((JvmParameterGroup) node.getUserObject()).setName(value);
                }
                if (node.getUserObject() instanceof JvmParameter) {
                    ((JvmParameter) node.getUserObject()).setName(value);
                }
            }
        };

        ColumnInfo<DefaultMutableTreeNode, String> valueColumn = new ColumnInfo<DefaultMutableTreeNode, String>("Value") {
            @Override
            public boolean isCellEditable(DefaultMutableTreeNode node) {
                return node.getUserObject() instanceof JvmParameter;
            }

            @Override
            public String valueOf(DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof JvmParameter) {
                    return ((JvmParameter) node.getUserObject()).getValue();
                }
                return "";
            }

            @Override
            public void setValue(DefaultMutableTreeNode node, String value) {
                if (node.getUserObject() instanceof JvmParameter) {
                    ((JvmParameter) node.getUserObject()).setValue(value);
                }
            }
        };

        ColumnInfo<DefaultMutableTreeNode, Boolean> scopeColumn = new ColumnInfo<DefaultMutableTreeNode, Boolean>("Global") {
            @Override
            public Class<?> getColumnClass() {
                return Boolean.class;
            }

            @Override
            public boolean isCellEditable(DefaultMutableTreeNode node) {
                return node.getUserObject() instanceof JvmParameterGroup;
            }

            @Override
            public Boolean valueOf(DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof JvmParameterGroup) {
                    return ((JvmParameterGroup) node.getUserObject()).getGlobal();
                }
                return null;
            }

            @Override
            public void setValue(DefaultMutableTreeNode node, Boolean value) {
                if (node.getUserObject() instanceof JvmParameterGroup) {
                    JvmParameterGroup group = (JvmParameterGroup) node.getUserObject();
                    group.setGlobal(value);
                    // update children
                    for (int i = 0; i < node.getChildCount(); i++) {
                        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                        if (childNode.getUserObject() instanceof JvmParameter) {
                            ((JvmParameter) childNode.getUserObject()).setGlobal(value);
                        }
                    }
                }
            }
        };

        ColumnInfo[] columns = {enabledColumn, nameColumn, valueColumn, scopeColumn};

        rootNode = new DefaultMutableTreeNode();
        treeModel = new ListTreeTableModelOnColumns(rootNode, columns);
        treeTable = new TreeTable(treeModel);
        treeTable.setDragEnabled(true);
        treeTable.setDropMode(DropMode.INSERT);
        treeTable.setTransferHandler(new TreeTransferHandler());
        treeTable.setRootVisible(false);

        // Render group name bold
        treeTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(table.getFont()); // Reset font to default
                TreePath path = treeTable.getTree().getPathForRow(row);
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.getUserObject() instanceof JvmParameterGroup) {
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        });

        treeTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        treeTable.getColumnModel().getColumn(0).setMaxWidth(50);
        treeTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        treeTable.getColumnModel().getColumn(3).setMaxWidth(70);
    }

    private void generateButton(MouseEvent e) {
        DefaultActionGroup generateActionGroup = new DefaultActionGroup(
                new AnAction(JvmParameterBundle.getMessage("generate.btn.jvmMem")) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        dataModel.addRow(true, "-Xms512m -Xmx512m", "");
                    }
                },
                new AnAction(JvmParameterBundle.getMessage("generate.btn.apollo")) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        dataModel.addRow(true, "env", "DEV");
                        dataModel.addRow(false, "idc", "default");
                    }
                },
                new AnAction(JvmParameterBundle.getMessage("generate.btn.doubleLocal")) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        dataModel.addRow(true, "dubbo.registry.register", "false");
                        dataModel.addRow(true, "dubbo.service.group", System.getProperty("user.name"));
                    }
                },
                new AnAction(JvmParameterBundle.getMessage("generate.btn.doubleServiceGroup")) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        ApplicationManager.getApplication().runReadAction(() -> {
                            AppUtils.findDubboService(project).forEach(s -> {
                                dataModel.addRow(true, "dubbo.service." + s + ".group", System.getProperty("user.name"));
                            });
                        });
                    }
                }
        );

        DataContext dataContext = DataManager.getInstance().getDataContext(e.getComponent());
        final ListPopup popup = JBPopupFactory.getInstance()
                                              .createActionGroupPopup(
                                                      JvmParameterBundle.getMessage("label.jvm.parameter.generate"),
                                                      generateActionGroup,
                                                      dataContext,
                                                      JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                                      true);
        popup.showInBestPositionFor(dataContext);
    }

    private void generateButton2(MouseEvent e) {
        DefaultActionGroup generateActionGroup = new DefaultActionGroup(
                new AnAction("Copy as line string") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        String text = jvmParameterText.getText();
                        if (StringUtils.isNotEmpty(text)) {
                            String s = text.replace("\\", "\\\\");
                            CopyPasteManager.getInstance().setContents(new StringSelection(s));
                        }
                    }
                },
                new AnAction("Copy as multiline string") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        String s = dataModel.list.stream()
                                                 .map(JvmParameter::toRunParameter)
                                                 .filter(StringUtils::isNotEmpty)
                                                 .collect(Collectors.joining("\n"));
                        if (StringUtils.isNotEmpty(s)) {
                            s = s.replace("\\", "\\\\");
                            CopyPasteManager.getInstance().setContents(new StringSelection(s));
                        }
                    }
                },
                new AnAction("Copy as string array") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        String s = dataModel.list.stream()
                                                 .map(JvmParameter::toRunParameter2)
                                                 .filter(StringUtils::isNotEmpty)
                                                 .collect(Collectors.joining(",\n    "));
                        if (StringUtils.isNotEmpty(s)) {
                            s = s.replace("\\", "\\\\");
                            CopyPasteManager.getInstance().setContents(new StringSelection("[\n    " + s + "\n]"));
                        }
                    }
                }
        );
        DataContext dataContext = DataManager.getInstance().getDataContext(jvmParameterText);
        final ListPopup popup = JBPopupFactory.getInstance()
                                              .createActionGroupPopup(
                                                      null,
                                                      generateActionGroup,
                                                      dataContext,
                                                      JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                                      true);
        popup.showInBestPositionFor(dataContext);
    }

    public Pair<JvmParameterSetting, JvmParameterSetting> getModifiedSetting() {
        JvmParameterSetting globalJvmParameterSetting = new JvmParameterSetting();
        JvmParameterSetting projectJvmParameterSetting = new JvmParameterSetting();
        saveTo(globalJvmParameterSetting, projectJvmParameterSetting);
        return Pair.of(globalJvmParameterSetting, projectJvmParameterSetting);
    }

    public void saveTo(JvmParameterSetting globalJvmParameterSetting, JvmParameterSetting projectJvmParameterSetting) {
        List<JvmParameterGroup> globalGroups = new ArrayList<>();
        List<JvmParameterGroup> projectGroups = new ArrayList<>();

        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            if (!(groupNode.getUserObject() instanceof JvmParameterGroup)) continue;

            JvmParameterGroup groupData = (JvmParameterGroup) groupNode.getUserObject();
            groupData.setItems(new ArrayList<>()); // Clear and rebuild

            for (int j = 0; j < groupNode.getChildCount(); j++) {
                DefaultMutableTreeNode paramNode = (DefaultMutableTreeNode) groupNode.getChildAt(j);
                if (paramNode.getUserObject() instanceof JvmParameter) {
                    JvmParameter param = (JvmParameter) paramNode.getUserObject();
                    param.setGlobal(groupData.getGlobal()); // Enforce consistency from group
                    groupData.getItems().add(param);
                }
            }

            if (BooleanUtils.isTrue(groupData.getGlobal())) {
                globalGroups.add(groupData);
            } else {
                projectGroups.add(groupData);
            }
        }
        globalJvmParameterSetting.setJvmParameterGroup(globalGroups);
        projectJvmParameterSetting.setJvmParameterGroup(projectGroups);
    }

    public void reset(JvmParameterSetting globalJvmParameterSetting, JvmParameterSetting projectJvmParameterSetting) {
        dataModel.clear();
        rootNode.removeAllChildren();

        for (JvmParameterGroup group : globalJvmParameterSetting.getJvmParameterGroup()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
            group.setGlobal(true);
            for (JvmParameter jvmParameter : group.getItems()) {
                jvmParameter.setGlobal(true);
                groupNode.add(new DefaultMutableTreeNode(jvmParameter));
            }
            rootNode.add(groupNode);
        }
        for (JvmParameterGroup group : projectJvmParameterSetting.getJvmParameterGroup()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
            group.setGlobal(false);
            for (JvmParameter jvmParameter : group.getItems()) {
                jvmParameter.setGlobal(false);
                groupNode.add(new DefaultMutableTreeNode(jvmParameter));
            }
            rootNode.add(groupNode);
        }
        treeModel.reload();
        // expand all
        for (int i = 0; i < treeTable.getRowCount(); i++) {
            treeTable.getTree().expandRow(i);
        }
    }

    private void addNode() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) treeTable.getTree().getLastSelectedPathComponent();

        // if group selected, add parameter to it
        if (selectedNode != null) {
            DefaultMutableTreeNode groupNode = selectedNode;
            if (groupNode.getUserObject() instanceof JvmParameter) {
                groupNode = (DefaultMutableTreeNode) selectedNode.getParent();
            }

            if (groupNode.getUserObject() instanceof JvmParameterGroup) {
                JvmParameterGroup group = (JvmParameterGroup) groupNode.getUserObject();
                JvmParameter jvmParameter = new JvmParameter(true, "", "", group.getGlobal());
                DefaultMutableTreeNode paramNode = new DefaultMutableTreeNode(jvmParameter);
                groupNode.add(paramNode);

                treeModel.reload(groupNode);
                treeTable.getTree().expandPath(new TreePath(groupNode.getPath()));
                return;
            }
        }

        // add new group
        addJvmParameterGroup();
    }

    private void addJvmParameterGroup() {
        String name = JOptionPane.showInputDialog(mainPanel, "Group Name");
        if (StringUtils.isNotEmpty(name)) {
            JvmParameterGroup group = new JvmParameterGroup(name);
            group.setGlobal(false); // default to project
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
            rootNode.add(groupNode);

            treeModel.reload();
            treeTable.getTree().expandPath(new TreePath(groupNode.getPath()));
            treeTable.getSelectionModel().setSelectionInterval(0, treeTable.getRowCount() - 1);
        }
    }

    private void removeJvmParameter() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) treeTable.getTree().getLastSelectedPathComponent();
        if (selectedNode != null) {
            if (selectedNode.getUserObject() instanceof JvmParameterGroup && selectedNode.getChildCount() > 0) {
                int result = JOptionPane.showConfirmDialog(mainPanel, "Group is not empty, continue?", "Delete", JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            if (selectedNode.getParent() != null) {
                ((DefaultTreeModel) treeTable.getTree().getModel()).removeNodeFromParent(selectedNode);
            }
        }
    }

    class TreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            TreeTable table = (TreeTable) c;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) table.getTree().getLastSelectedPathComponent();
            if (node != null) {
                return new TransferableNode(node);
            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }
            TreePath path;
            if (support.getComponent() instanceof JTree) {
                path = ((JTree.DropLocation) support.getDropLocation()).getPath();
            } else if (support.getComponent() instanceof JTable) {
                path = treeTable.getTree().getPathForRow(((JTable.DropLocation) support.getDropLocation()).getRow());
            } else {
                return false;
            }

            if (path == null) {
                return false;
            }
            DefaultMutableTreeNode dropNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            return dropNode.getUserObject() instanceof JvmParameterGroup;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                Transferable transferable = support.getTransferable();
                DefaultMutableTreeNode transferableNode = (DefaultMutableTreeNode) transferable.getTransferData(NODE_FLAVOR);

                if (transferableNode.getUserObject() instanceof JvmParameterGroup) {
                    return false;
                }

                TreePath path;
                if (support.getComponent() instanceof JTree) {
                    path = ((JTree.DropLocation) support.getDropLocation()).getPath();
                } else if (support.getComponent() instanceof JTable) {
                    path = treeTable.getTree().getPathForRow(((JTable.DropLocation) support.getDropLocation()).getRow());
                } else {
                    return false;
                }
                DefaultMutableTreeNode dropNode = (DefaultMutableTreeNode) path.getLastPathComponent();

                // remove from old parent
                ((DefaultTreeModel) treeTable.getTree().getModel()).removeNodeFromParent(transferableNode);
                // add to new parent
                dropNode.add(transferableNode);

                ((DefaultTreeModel) treeTable.getTree().getModel()).reload();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    class TransferableNode implements Transferable {
        private DefaultMutableTreeNode node;

        public TransferableNode(DefaultMutableTreeNode node) {
            this.node = node;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{NODE_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return NODE_FLAVOR.equals(flavor);
        }

        @NotNull
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return node;
        }
    }
}