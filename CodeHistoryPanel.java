package com.example.code_versioning_plugin;


import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;


public class CodeHistoryPanel extends JPanel {

    private JTree leftTree;
    private Project project;
    private DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("历史记录");
    private Timer timer;
    private List expandedPaths = new List();
    /**
     * 展示文件的差异比较窗口
     */
    public void showFileDifference(String currentContent, String originalContent,String TitleName) {

        // 创建 DiffContent 工厂
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DiffContent originalDiffContent = contentFactory.create(project, originalContent);
        DiffContent currentDiffContent = contentFactory.create(project, currentContent);

        // 构建一个差异请求对象
        String title = TitleName;         // 窗口的主标题
        String originalTitle = "Last Version"; // 上一文件版本的标题
        String currentTitle = "Current Version";   // 当前文件版本的标题

        SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                title,
                originalDiffContent,
                currentDiffContent,
                originalTitle,
                currentTitle
        );

        // 显示差异窗口
        DiffManager.getInstance().showDiff(project, diffRequest);

    }

    public CodeHistoryPanel(Project project, ToolWindow toolWindow) throws IOException {
        this.project = project;

        setLayout(new BorderLayout());
        // 在左侧最上方添加按钮工具栏
        SwingUtilities.invokeLater(() -> addToolbar(toolWindow));

        // 左侧目录树
        leftTree = new JTree(rootNode);
        leftTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // 设置JTree背景为深色，文字颜色为浅色
        leftTree.setBackground(new Color(43, 43, 43));
        leftTree.setForeground(new Color(169, 183, 198));

        populateTree(rootNode); // 填充树数据
        add(leftTree, BorderLayout.CENTER);

        // 添加监听器，当左侧节点选中时更新右侧内容
        leftTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) leftTree.getLastSelectedPathComponent();
            if (selectedNode == null) {
                return;
            }
            if (!selectedNode.isLeaf()){
                // 非叶节点点击后展开内容
                TreePath path = new TreePath(selectedNode.getPath());
                boolean isExpanded = leftTree.isExpanded(path);
                if (isExpanded) {
                    // 如果已经展开，则收回
                    leftTree.collapsePath(path);
                } else {
                    // 如果未展开，则展开
                    leftTree.expandPath(path);
                }
                // 清除当前选中状态，允许重新选择
                leftTree.clearSelection();
            }
            else{
                //这里传入选中文本对象内容
                if(selectedNode.getUserObject() == "当前版本修改")
                    return;
                MyFileNode selectedNodeContent = (MyFileNode) selectedNode.getUserObject();
                Path filePath = selectedNodeContent.entry.getKey();
                Integer version = selectedNodeContent.version;
                String TitleName = selectedNode.getUserObject().toString();
                VersionControl.FileCompare newContent = null;
                //根据version特判调用哪个方法来获取文件内容
                if(version != -1){
                    newContent = VersionControl.getInstance().getFileOfCertainVersion(filePath,version);
                }
                else{
                    newContent = VersionControl.getInstance().getFileOfCurrentVersion(filePath);
                }
                //将List<String>转换为String
                String currentContentString = String.join("\n", newContent.after);
                String originalContentString = String.join("\n", newContent.before);
                showFileDifference(currentContentString,originalContentString,TitleName);
                // 清除当前选中状态，允许重新选择
                leftTree.clearSelection();
            }
        });

        // 启动定时器定期刷新树的数据（每隔1秒）
        startAutoRefresh();
    }

    /**
     * 添加顶部工具栏并创建按钮
     */
    private void addToolbar(ToolWindow toolWindow) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        // 创建自定义按钮动作
        AnAction myAction = new AnAction("Push Current Code") {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                onMyButtonClick();
            }
        };

        // 将按钮动作添加到 ActionGroup
        actionGroup.add(myAction);

        // 使用 ActionManager 获取并创建工具栏
        ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("CustomToolbar", actionGroup, true);

        // 设置目标组件（可以设置为当前窗口或其他相关组件）
        actionToolbar.setTargetComponent(this); // 或者其他合适的组件

        // 将工具栏组件添加到面板顶部
        add(actionToolbar.getComponent(), BorderLayout.NORTH);

        // 创建并设置 ToolWindow 的内容
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(this, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * 按钮点击时执行的操作
     */
    private void onMyButtonClick() {
        try {
            if (VersionControl.getInstance().push()) {
                // 使用NotificationGroupManager进行成功通知
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("listener")
                    .createNotification("推送成功", "推送操作已完成", NotificationType.INFORMATION)
                    .notify(project);
            } else {
                // 使用NotificationGroupManager进行失败通知
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("listener")
                    .createNotification("推送失败", "当前无修改，无需推送", NotificationType.ERROR)
                    .notify(project);
            }
            //refreshTree();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    private void startAutoRefresh() {
        timer = new Timer(100, e -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    saveExpandedState();
                    refreshTree();
                    restoreExpandedState();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        });
        timer.start();
    }

    private void saveExpandedState() {
        expandedPaths.removeAll(); // 确保清空之前保存的状态
        // 保存当前展开的路径
        Enumeration<TreePath> paths = leftTree.getExpandedDescendants(new TreePath(rootNode));
        if(paths != null){
            while (paths.hasMoreElements()) {
                TreePath path = paths.nextElement();
                expandedPaths.add(pathToString(path));
            }
        }
    }

    private String pathToString(TreePath path) {
        StringBuilder sb = new StringBuilder();
        for (Object component : path.getPath()) {
            if (sb.length() > 0) {
                sb.append("/");
            }
            sb.append(component.toString());
        }
        return sb.toString();
    }

    private void restoreExpandedState() {
        // 恢复之前保存的展开路径
        if(expandedPaths!= null){
            //循环处理将List类型的expandedPaths中的每个元素从String转换为TreePath并添加到TreePath列表中
            for (int i = 0; i < expandedPaths.getItemCount(); i++) {
                String pathString = expandedPaths.getItem(i);
                // 使用斜杠分隔路径元素
                String[] pathElements = pathString.split("/");

                // 创建一个空的TreePath，以root作为起点
                TreePath path = new TreePath(rootNode);

                // 循环遍历路径元素，查找对应的树节点并添加到TreePath中
                for (String element : pathElements) {
                    TreeNode currentNode = (TreeNode) path.getLastPathComponent();
                    int childCount = currentNode.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        TreeNode node = ((DefaultMutableTreeNode) path.getLastPathComponent()).getChildAt(j);
                        if (node.toString().equals(element)) { // 根据节点名称匹配
                            path = path.pathByAddingChild(node);
                            break;
                        }
                    }
                }
                leftTree.expandPath(path);
            }
        }
    }

    private void refreshTree() throws IOException {
        // 清空树的数据
        rootNode.removeAllChildren();
        // 重新填充树
        populateTree(rootNode);
        // 通知模型进行更新
        ((DefaultTreeModel) leftTree.getModel()).reload();
    }


    // 动态填充树数据
    private void populateTree(DefaultMutableTreeNode rootNode) throws IOException {
        //当前版本的versionNode
        DefaultMutableTreeNode currentVersionNode = new DefaultMutableTreeNode("当前版本修改");
        rootNode.add(currentVersionNode);
        var currentResultMap = VersionControl.getInstance().getChangeDirOfCurrentVersion();
        for (Map.Entry<Path, FileStatus> entry : currentResultMap.entrySet()) {
            DefaultMutableTreeNode changeNode = new DefaultMutableTreeNode(new MyFileNode(entry,-1));
            currentVersionNode.add(changeNode);
        }

        //获取之前已保存的所有versionNode
        var versionNodeInfoList = VersionControl.getInstance().getProjectVersionInfo();
        int index = 1;
        for (Map<String, String> map : versionNodeInfoList) {
            String version = map.get("version");
            String time = map.get("time");
            //if(Integer.parseInt(version) != 0){
            DefaultMutableTreeNode versionNode = new DefaultMutableTreeNode("第" + version + "版-"+time);
            rootNode.add(versionNode);
            //获取指定版本的文件修改记录目录信息
            var resultMap = VersionControl.getInstance().getChangeDirOfDesVersion(version);
            //添加文件修改记录
            for (Map.Entry<Path, FileStatus> entry : resultMap.entrySet()) {
                DefaultMutableTreeNode changeNode = new DefaultMutableTreeNode(new MyFileNode(entry,index));
                versionNode.add(changeNode);
            }
            index++;
        }
    }
}









