package com.cxy.redisclient.presentation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cxy.redisclient.domain.ContainerKey;
import com.cxy.redisclient.domain.DataNode;
import com.cxy.redisclient.domain.Favorite;
import com.cxy.redisclient.domain.Language;
import com.cxy.redisclient.domain.Node;
import com.cxy.redisclient.domain.NodeType;
import com.cxy.redisclient.domain.Server;
import com.cxy.redisclient.dto.ContainerKeyInfo;
import com.cxy.redisclient.dto.FindInfo;
import com.cxy.redisclient.dto.HashInfo;
import com.cxy.redisclient.dto.ListInfo;
import com.cxy.redisclient.dto.Order;
import com.cxy.redisclient.dto.OrderBy;
import com.cxy.redisclient.dto.RenameInfo;
import com.cxy.redisclient.dto.SetInfo;
import com.cxy.redisclient.dto.StringInfo;
import com.cxy.redisclient.dto.ZSetInfo;
import com.cxy.redisclient.integration.ConfigFile;
import com.cxy.redisclient.integration.I18nFile;
import com.cxy.redisclient.presentation.component.DataContent;
import com.cxy.redisclient.presentation.console.Console;
import com.cxy.redisclient.presentation.favorite.AddFavoriteDialog;
import com.cxy.redisclient.presentation.favorite.OrganizeFavoriteDialog;
import com.cxy.redisclient.presentation.hash.HashDataContent;
import com.cxy.redisclient.presentation.hash.NewHashDialog;
import com.cxy.redisclient.presentation.key.DeleteContainerDialog;
import com.cxy.redisclient.presentation.key.FindKeyDialog;
import com.cxy.redisclient.presentation.key.RenameKeysDialog;
import com.cxy.redisclient.presentation.list.ListDataContent;
import com.cxy.redisclient.presentation.list.NewListDialog;
import com.cxy.redisclient.presentation.pubsub.Publish;
import com.cxy.redisclient.presentation.pubsub.Subscribe;
import com.cxy.redisclient.presentation.server.AddServerDialog;
import com.cxy.redisclient.presentation.server.PropertiesDialog;
import com.cxy.redisclient.presentation.server.UpdateServerDialog;
import com.cxy.redisclient.presentation.set.NewSetDialog;
import com.cxy.redisclient.presentation.set.SetDataContent;
import com.cxy.redisclient.presentation.string.NewStringDialog;
import com.cxy.redisclient.presentation.string.StringDataContent;
import com.cxy.redisclient.presentation.zset.NewZSetDialog;
import com.cxy.redisclient.presentation.zset.ZSetDataContent;
import com.cxy.redisclient.service.ExportService;
import com.cxy.redisclient.service.FavoriteService;
import com.cxy.redisclient.service.HashService;
import com.cxy.redisclient.service.ImportService;
import com.cxy.redisclient.service.KeyNotExistException;
import com.cxy.redisclient.service.ListService;
import com.cxy.redisclient.service.NodeService;
import com.cxy.redisclient.service.ServerService;
import com.cxy.redisclient.service.SetService;
import com.cxy.redisclient.service.ZSetService;

public class RedisClient {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);

    private Shell shell;
    private PasteBuffer pBuffer = new PasteBuffer();
    private FindBuffer fBuffer = null;
    private final NavHistory history = new NavHistory();
    private boolean flatView = ConfigFile.getFlatView();
    public static Language language = ConfigFile.getLanguage();
    public static final I18nFile i18nFile = new I18nFile();

    private TreeItem rootRedisServers;

    private Item[] itemsSelected = new Item[]{rootRedisServers};
    private TreeItem treeItemSelected;

    private static final String DB_PREFIX = "db";
    private static final String NODE_TYPE = "type";
    private static final String NODE_ID = "id";
    private static final String ITEM_OPENED = "open";
    private static final String FAVORITE = "favorite";

    private Tree tree;
    private Table table;
    private Text text;
    private CTabItem mainTabItem;
    private CTabFolder tabFolder;
    private CTabFolder tabFolder_1;
    private final DataContents openDataContent = new DataContents();
    private final Tools<Console> openConsole = new Tools<>();
    private final Tools<Publish> openPublish = new Tools<>();
    private final Tools<Subscribe> openSubscribe = new Tools<>();

    private Menu menuTreeServer;
    private Menu menuTableServer;
    private Menu menu;
    private Menu menu_null;
    private Menu menuTreeDBContainer;
    private Menu menuTableDBContainer;
    private Menu menuDataExplorer;
    private Menu menu_Multi;
    private Menu menuData; //顶级 Data菜单
    private Menu menuFavorite; //顶级 Favorite菜单
    private Menu menuServer; //顶级 Server菜单
    private Menu menuView; //顶级 View菜单
    private Menu menuTools; //顶级 Tools菜单

    //顶级View菜单 子菜单 
    private MenuItem menuViewOrderbyName;  // View => Order by => Name 菜单
    private MenuItem menuViewOrderbyType;  // View => Order by => Type 菜单
    private MenuItem menuViewOrderbySize;  // View => Order by => Size 菜单
    private MenuItem menuViewOrderAscend;  // View => Order => Ascend 菜单
    private MenuItem menuViewOrderDescend; // View => Order => Descend 菜单

    private final ServerService serverService = new ServerService();
    private final NodeService nodeService = new NodeService();
    private final FavoriteService favoriteService = new FavoriteService();
    private final ListService listService = new ListService();
    private final SetService setService = new SetService();
    private final ZSetService zsetService = new ZSetService();
    private final HashService hashService = new HashService();

    private Image redisImage;
    private Image dbImage;
    private Image containerImage;
    private Image strImage;
    private Image setImage;
    private Image listImage;
    private Image zsetImage;
    private Image hashImage;
    private Image strGrayImage;
    private Image setGrayImage;
    private Image listGrayImage;
    private Image zsetGrayImage;
    private Image hashGrayImage;
    private Image leftImage;
    private Image rightImage;
    private Image upImage;
    private Image refreshImage;
    private Image iconImage;
    private Image codeImage;
    private Image questionImage;

    private TableColumn tableColumnName;
    private TableColumn tablecolumnType;
    private TableColumn tablecolumnSize;
    private Button btnBackward; //上一个按钮  ←按钮
    private Button btnForward; //下一个按钮   →按钮

    private Order clientOrder = Order.Ascend;
    private OrderBy clientOrderBy = OrderBy.NAME;

    /**
     * @param args
     */
    public static void main(String[] args) {
        RedisClient window = new RedisClient();
        window.open();
    }

    /**
     * Open the window.
     *
     */
    public void open() {
        Display display = Display.getDefault();
        createContents();

        shell.open();
        shell.layout();

        while (!shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            } catch (Exception e) {
                MessageDialog.openError(shell, i18nFile.getText(I18nFile.ERROR), e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        display.dispose();
    }

    /**
     * Create contents of the window.
     *
     */
    protected void createContents() {
        initShell();

        initMenu();

        initImage();

        initSash();
    }

    /**
     * 初始化图片
     */
    private void initImage() {
        redisImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/redis.png"));
        dbImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/db.png"));

        containerImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/container.png"));

        strImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/string.png"));
        strGrayImage = new Image(shell.getDisplay(), strImage, SWT.IMAGE_GRAY);

        setImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/set.png"));
        setGrayImage = new Image(shell.getDisplay(), setImage, SWT.IMAGE_GRAY);

        listImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/list.png"));
        listGrayImage = new Image(shell.getDisplay(), listImage, SWT.IMAGE_GRAY);

        zsetImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/zset.png"));
        zsetGrayImage = new Image(shell.getDisplay(), zsetImage, SWT.IMAGE_GRAY);

        hashImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/hash.png"));
        hashGrayImage = new Image(shell.getDisplay(), hashImage, SWT.IMAGE_GRAY);

        leftImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/left.png"));
        rightImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/right.png"));
        upImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/up.png"));
        refreshImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/refresh.png"));

        iconImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/icon.png"));

        codeImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/code.png"));

        questionImage = new Image(shell.getDisplay(), getClass().getResourceAsStream("/question.png"));
    }

    /**
     * 
     */
    private void initShell() {
        shell = new Shell();
        shell.addListener(SWT.Close, (Event event) -> {
            String key = openDataContent.canClose();
            if (key != null) {
                boolean ok = MessageDialog.openConfirm(shell, i18nFile.getText(I18nFile.CLOSEAPP), key + ": " + i18nFile.getText(I18nFile.CLOSEAPPERROR));
                event.doit = ok;
            } else {
                event.doit = true;
            }
        });
        shell.setText("RedisClient");
        shell.setLayout(new GridLayout(1, false));
    }

    /**
     *  功能按钮初始化
     */
    private void initSash() {
        Composite composite_1 = new Composite(shell, SWT.NONE);
        GridLayout gl_composite_1 = new GridLayout(1, false);
        gl_composite_1.verticalSpacing = 0;
        gl_composite_1.marginWidth = 0;
        gl_composite_1.marginHeight = 0;
        gl_composite_1.horizontalSpacing = 0;
        composite_1.setLayout(gl_composite_1);
        composite_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        Composite composite = new Composite(composite_1, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        GridLayout gl_composite = new GridLayout(5, false);
        gl_composite.marginBottom = 5;
        gl_composite.verticalSpacing = 0;
        gl_composite.horizontalSpacing = 0;
        gl_composite.marginWidth = 0;
        gl_composite.marginHeight = 0;
        composite.setLayout(gl_composite);

        //上一个按钮 start
        btnBackward = new Button(composite, SWT.CENTER);
        btnBackward.setEnabled(false);
        btnBackward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem item = history.backward();
                if (!item.isDisposed()) {
                    tree.setSelection(item);
                    treeItemSelected(false);
                    if (!history.canBackward()) {
                        btnBackward.setEnabled(false);
                    }

                    btnForward.setEnabled(true);
                } else {
                    MessageDialog.openInformation(shell,
                            i18nFile.getText(I18nFile.ERROR),
                            i18nFile.getText(I18nFile.OBJECTDELETE));
                }
            }
        });
        btnBackward.setImage(leftImage);
        //上一个按钮 end
        
        //下一个按钮 start
        btnForward = new Button(composite, SWT.NONE);
        btnForward.setEnabled(false);
        btnForward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem item = history.forward();
                btnBackward.setEnabled(true);
                if (!item.isDisposed()) {
                    tree.setSelection(item);
                    treeItemSelected(false);
                    if (!history.canForward()) {
                        btnForward.setEnabled(false);
                    }
                } else {
                    MessageDialog.openInformation(shell, i18nFile.getText(I18nFile.ERROR), i18nFile.getText(I18nFile.OBJECTDELETE));
                }
            }
        });
        btnForward.setImage(rightImage);
        //下一层按钮 end
        
        //上一层按钮 start 
        Button btnUP = new Button(composite, SWT.CENTER);
        btnUP.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] items = tree.getSelection();
                if (items[0] != rootRedisServers) {
                    history.add(items[0].getParentItem());
                    btnBackward.setEnabled(true);
                    btnForward.setEnabled(false);
                    tree.setSelection(items[0].getParentItem());
                    treeItemSelected(false);
                }
            }
        });
        btnUP.setImage(upImage);
        //上一层按钮 end
        
        //刷新按钮 start
        Button btnRefresh = new Button(composite, SWT.CENTER);
        btnRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshOrder();
                treeItemSelected(true);
            }
        });
        btnRefresh.setImage(refreshImage);
        //刷新按钮 end
        
        //当前被选中内容展示 Text（只读） start
        text = new Text(composite, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        text.setEditable(false);
        //当前被选中内容展示 Text（只读） end
        
        final SashForm sashForm = new SashForm(composite_1, SWT.NONE);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        initTree(sashForm);

        //初始化根元素
        initRootItem();

        //初始化菜单数据
        initMenuData();

        menuTableDBContainer = initMenuTableDB();
        menuTreeDBContainer = initMenuTreeDB();

        initMenuMulti();//？？？ 不确定

        final Composite composite_2 = new Composite(sashForm, SWT.NONE);
        composite_2.setLayout(new GridLayout(1, false));

        final SashForm sashForm_1 = new SashForm(composite_2, SWT.VERTICAL);
        sashForm_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
                1, 1));
        sashForm_1.setLocation(0, 0);

        tabFolder = new CTabFolder(sashForm_1, SWT.BORDER);
        tabFolder.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                if (sashForm_1.getMaximizedControl() != tabFolder) {
                    sashForm.setMaximizedControl(composite_2);
                    sashForm_1.setMaximizedControl(tabFolder);
                } else {
                    sashForm.setMaximizedControl(null);
                    sashForm_1.setMaximizedControl(null);
                }
            }
        });
        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (tabFolder.getSelection() == mainTabItem) {
                    treeItemSelected(true);
                }
            }
        });
        tabFolder.setSelectionBackground(Display.getCurrent().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
        tabFolder.setFocus();
        tabFolder.setLayout(new GridLayout(1, false));

        mainTabItem = new CTabItem(tabFolder, SWT.NONE);
        mainTabItem.setText(i18nFile.getText(I18nFile.REDISDATAEXPLORER));

        initTable(tabFolder);

        tabFolder_1 = new CTabFolder(sashForm_1, SWT.BORDER);
        tabFolder_1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                if (sashForm_1.getMaximizedControl() != tabFolder_1) {
                    sashForm.setMaximizedControl(composite_2);
                    sashForm_1.setMaximizedControl(tabFolder_1);
                } else {
                    sashForm.setMaximizedControl(null);
                    sashForm_1.setMaximizedControl(null);
                }
            }
        });
        tabFolder_1.setBounds(0, 0, 156, 127);
        tabFolder_1.setSelectionBackground(Display.getCurrent().getSystemColor(
                SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));

        sashForm_1.setWeights(new int[]{1, 1});
        sashForm.setWeights(new int[]{1, 4});

        initMenuNull();

        menuTreeServer = initMenuTreeServer();
        menuTableServer = initMenuTableServer();

        initServers();

        tree.select(rootRedisServers);

        treeItemSelected(false);

        history.add(rootRedisServers);

        shell.setImage(iconImage);

    }

   /**
    * todo ??? 没有明白哪的菜单
    */
    private void initMenuMulti() {
        menu_Multi = new Menu(shell);

        MenuItem mntmDelete_5 = new MenuItem(menu_Multi, SWT.NONE);
        mntmDelete_5.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteKeys();
            }
        });
        mntmDelete_5.setText(i18nFile.getText(I18nFile.DELETE));

        new MenuItem(menu_Multi, SWT.SEPARATOR);

        MenuItem mntmCut_2 = new MenuItem(menu_Multi, SWT.NONE);
        mntmCut_2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cut();
            }
        });
        mntmCut_2.setText(i18nFile.getText(I18nFile.CUT));

        MenuItem mntmCopy_3 = new MenuItem(menu_Multi, SWT.NONE);
        mntmCopy_3.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copy();
            }
        });
        mntmCopy_3.setText(i18nFile.getText(I18nFile.COPY));

        new MenuItem(menu_Multi, SWT.SEPARATOR);

        MenuItem mntmExport_2 = new MenuItem(menu_Multi, SWT.NONE);
        mntmExport_2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                export();
            }
        });
        mntmExport_2.setText(i18nFile.getText(I18nFile.EXPORT));
    }

    /**
     * 初始化菜单数据 （Redis data explorer 右键菜单） todo ？？？确定是这样吗？
     */
    private void initMenuData() {
        menuDataExplorer = new Menu(shell);

        //Redis data explorer 右键菜单 => Rename start
        MenuItem menuDataExplorerRename = new MenuItem(menuDataExplorer, SWT.NONE);
        menuDataExplorerRename.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                renameKey();
            }
        });
        menuDataExplorerRename.setText(i18nFile.getText(I18nFile.RENAME));
        //Redis data explorer 右键菜单 => Rename end
        
        //Redis data explorer 右键菜单 => Delete start
        MenuItem menuDataExplorerDelete = new MenuItem(menuDataExplorer, SWT.NONE);
        menuDataExplorerDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteOneKey();
            }
        });
        menuDataExplorerDelete.setText(i18nFile.getText(I18nFile.DELETE));
        //Redis data explorer 右键菜单 => Delete end
        
        //Redis data explorer 右键菜单 => Properties start
        MenuItem menuDataExplorerProperties = new MenuItem(menuDataExplorer, SWT.NONE);
        menuDataExplorerProperties.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dataProperties();

            }
        });
        menuDataExplorerProperties.setText(i18nFile.getText(I18nFile.PROPERTIES));
        //Redis data explorer 右键菜单 => Properties end
        
        new MenuItem(menuDataExplorer, SWT.SEPARATOR);//分隔符

        //Redis data explorer 右键菜单 => Add to favorites start
        MenuItem menuDataExplorerAddFavorites = new MenuItem(menuDataExplorer, SWT.NONE);
        menuDataExplorerAddFavorites.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addFavorite();
            }
        });
        menuDataExplorerAddFavorites.setText(i18nFile.getText(I18nFile.ADDFAVORITES));
        //Redis data explorer 右键菜单 => Add to favorites end
        
        new MenuItem(menuDataExplorer, SWT.SEPARATOR);//分隔符
        
        //Redis data explorer 右键菜单 => Cut start
        MenuItem menuDataExplorerCut = new MenuItem(menuDataExplorer, SWT.NONE);
        menuDataExplorerCut.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cut();
            }
        });
        menuDataExplorerCut.setText(i18nFile.getText(I18nFile.CUT));
        //Redis data explorer 右键菜单 => Cut end
        
        //Redis data explorer 右键菜单 => Copy start
        MenuItem menuDataExplorerCopy = new MenuItem(menuDataExplorer, SWT.NONE);
        menuDataExplorerCopy.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copy();
            }
        });
        menuDataExplorerCopy.setText(i18nFile.getText(I18nFile.COPY));
        //Redis data explorer 右键菜单 => Copy start
        
        new MenuItem(menuDataExplorer, SWT.SEPARATOR);//分隔符

        //Redis data explorer 右键菜单 => Export start
        MenuItem menuDataExplorerExport = new MenuItem(menuDataExplorer, SWT.NONE);
        menuDataExplorerExport.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                export();
            }

        });
        menuDataExplorerExport.setText(i18nFile.getText(I18nFile.EXPORT));
        //Redis data explorer 右键菜单 => Export end
    }

    /**
     * 
     * @param sashForm 
     */
    private void initTree(SashForm sashForm) {
        tree = new Tree(sashForm, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        tree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectTreeItem();
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent arg0) {
                if (arg0.button == 3) {
                    Point point = new Point(arg0.x, arg0.y);
                    TreeItem selectedItem = tree.getItem(point);

                    selectTreeItem();

                    if (selectedItem == rootRedisServers
                            || selectedItem == null) {
                        tree.setMenu(menu_null);
                    } else {
                        NodeType type = (NodeType) selectedItem
                                .getData(NODE_TYPE);

                        if (null != type) {
                            switch (type) {
                                case ROOT:
                                    tree.setMenu(menu_null);
                                    break;
                                case SERVER:
                                    updateMenuServer(false, menuTreeServer);
                                    tree.setMenu(menuTreeServer);
                                    break;
                                case DATABASE:
                                case CONTAINER:
                                    updateMenuDBContainer(type, menuTreeDBContainer);
                                    tree.setMenu(menuTreeDBContainer);
                                    break;
                                default:
                                    break;
                            }
                        }

                    }

                }
            }
        });
    }

    /**
     * 初始化根元素
     */
    private void initRootItem() {
        rootRedisServers = new TreeItem(tree, SWT.NONE);
        rootRedisServers.setImage(redisImage);
        rootRedisServers.setText(i18nFile.getText(I18nFile.REDISSERVERS));
        rootRedisServers.setData(NODE_TYPE, NodeType.ROOT);
        rootRedisServers.setExpanded(true);
        rootRedisServers.setData(ITEM_OPENED, true);
    }

    private void parseContainer(TreeItem item, ContainerKeyInfo info) {
        TreeItem parent = item.getParentItem();
        if (item.getData(NODE_TYPE) == NodeType.CONTAINER) {
            String container = item.getText();
            if (info.getContainerStr() != null) {
                if (!flatView) {
                    info.setContainer(new ContainerKey(container + ":"
                            + info.getContainerStr()));
                } else {
                    info.setContainer(new ContainerKey(container
                            + info.getContainerStr()));
                }
            } else if (!flatView) {
                info.setContainer(new ContainerKey(container + ":"));
            } else {
                info.setContainer(new ContainerKey(container));
            }
            parseContainer(parent, info);
        } else if (item.getData(NODE_TYPE) == NodeType.DATABASE) {
            int db = (Integer) item.getData(NODE_ID);
            info.setDb(db);
            parseContainer(parent, info);
        } else if (item.getData(NODE_TYPE) == NodeType.SERVER) {
            int id = (Integer) item.getData(NODE_ID);
            info.setId(id);
            info.setServerName(item.getText());
        }
    }

    /**
     * 初始化 db及其db子项 右键菜单
     * @return 
     */
    private Menu initMenuTreeDB() {
        
        Menu tableDbMenu = initMenuTableDB();

        
        new MenuItem(tableDbMenu, SWT.SEPARATOR);//添加分隔符

        //db及其db子项 右键菜单 => Refresh start
        MenuItem tableDbMenuRefresh = new MenuItem(tableDbMenu, SWT.NONE);
        tableDbMenuRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] items = tree.getSelection();
                refreshOrder();
                dbContainerTreeItemSelected(items[0], true);
            }
        });
        tableDbMenuRefresh.setText(i18nFile.getText(I18nFile.REFRESH));
        //db及其db子项 右键菜单 => Refresh end
        
        return tableDbMenu;
    }

    /**
     * 初始化 db及其db子项 右键菜单
     * @return 
     */
    private Menu initMenuTableDB() {
        Menu menuDbContainer = new Menu(shell);

        //db及其db子项 右键菜单 New  start
        MenuItem menuItemDbContainerNew = new MenuItem(menuDbContainer, SWT.CASCADE);
        menuItemDbContainerNew.setText(i18nFile.getText(I18nFile.NEW));

        Menu menuDbContainerNew = new Menu(menuItemDbContainerNew);
        menuItemDbContainerNew.setMenu(menuDbContainerNew);

        //db及其db子项 右键菜单 New => String start
        MenuItem menuDbContainerNewString = new MenuItem(menuDbContainerNew, SWT.NONE);
        menuDbContainerNewString.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newString();
            }
        });
        menuDbContainerNewString.setText(i18nFile.getText(I18nFile.STRING));
        menuDbContainerNewString.setImage(strImage);
        //db及其db子项 右键菜单 New => String end

        //db及其db子项 右键菜单 New => List start
        MenuItem menuDbContainerNewList = new MenuItem(menuDbContainerNew, SWT.NONE);
        menuDbContainerNewList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newList();
            }
        });
        menuDbContainerNewList.setText(i18nFile.getText(I18nFile.LIST));
        menuDbContainerNewList.setImage(listImage);
        //db及其db子项 右键菜单 New => List end
        
        //db及其db子项 右键菜单 New => Set start
        MenuItem menuDbContainerNewSet = new MenuItem(menuDbContainerNew, SWT.NONE);
        menuDbContainerNewSet.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newSet();
            }
        });
        menuDbContainerNewSet.setText(i18nFile.getText(I18nFile.SET));
        menuDbContainerNewSet.setImage(setImage);
        //db及其db子项 右键菜单 New => Set end
        
        //db及其db子项 右键菜单 New => Sorted set start
        MenuItem menuDbContainerNewSortedSet = new MenuItem(menuDbContainerNew, SWT.NONE);
        menuDbContainerNewSortedSet.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newZSet();
            }
        });
        menuDbContainerNewSortedSet.setText(i18nFile.getText(I18nFile.ZSET));
        menuDbContainerNewSortedSet.setImage(zsetImage);
        //db及其db子项 右键菜单 New => Sorted set end

        //db及其db子项 右键菜单 New => Hash start
        MenuItem menuDbContainerNewHash = new MenuItem(menuDbContainerNew, SWT.NONE);
        menuDbContainerNewHash.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newHash();
            }
        });
        menuDbContainerNewHash.setText(i18nFile.getText(I18nFile.HASH));
        menuDbContainerNewHash.setImage(hashImage);
        //db及其db子项 右键菜单 New => Hash end
        //db及其db子项 右键菜单 New end

        //db及其db子项 右键菜单 Rename start
        MenuItem menuDbContainerRename = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerRename.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                renameContainer();
            }
        });
        menuDbContainerRename.setText(i18nFile.getText(I18nFile.RENAME));
        //db及其db子项 右键菜单 Rename end

        //db及其db子项 右键菜单 Delete start
        MenuItem menuDbContainerDelete = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteOneContainer();
            }
        });
        menuDbContainerDelete.setText(i18nFile.getText(I18nFile.DELETE));
        //db及其db子项 右键菜单 Delete end
        
        //db及其db子项 右键菜单 Properties start
        MenuItem menuDbContainerProperties = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerProperties.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dbContainerProperties();
            }
        });
        menuDbContainerProperties.setText(i18nFile.getText(I18nFile.PROPERTIES));
        //db及其db子项 右键菜单 Properties end

        new MenuItem(menuDbContainer, SWT.SEPARATOR); //添加分隔符

        //db及其db子项 右键菜单 Add to Favorites start
        MenuItem menuDbContainerAddToFavorites = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerAddToFavorites.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addFavorite();
            }
        });
        menuDbContainerAddToFavorites.setText(i18nFile.getText(I18nFile.ADDFAVORITES));
        //db及其db子项 右键菜单 Add to Favorites end

        new MenuItem(menuDbContainer, SWT.SEPARATOR);//添加分隔符

        //db及其db子项 右键菜单 Cut start
        MenuItem menuDbContainerCut = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerCut.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                cut();
            }
        });
        menuDbContainerCut.setText(i18nFile.getText(I18nFile.CUT));
        //db及其db子项 右键菜单 Cut end

        //db及其db子项 右键菜单 Copy start
        MenuItem menuDbContainerCopy = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerCopy.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copy();
            }
        });
        menuDbContainerCopy.setText(i18nFile.getText(I18nFile.COPY));
        //db及其db子项 右键菜单 Copy end

        //db及其db子项 右键菜单 Paste start
        MenuItem menuDbContainerPaste = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerPaste.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                paste();
            }
        });
        menuDbContainerPaste.setEnabled(false);
        menuDbContainerPaste.setText(i18nFile.getText(I18nFile.PASTE));
        //db及其db子项 右键菜单 Paste end

        new MenuItem(menuDbContainer, SWT.SEPARATOR);//添加分隔符

        //db及其db子项 右键菜单 Import start
        MenuItem menuDbContainerImport = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerImport.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                importFile();
            }
        });
        menuDbContainerImport.setEnabled(false);
        menuDbContainerImport.setText(i18nFile.getText(I18nFile.IMPORT));
        //db及其db子项 右键菜单 Import end

        //db及其db子项 右键菜单 Export start
        MenuItem menuDbContainerExport = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerExport.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                export();
            }
        });
        menuDbContainerExport.setText(i18nFile.getText(I18nFile.EXPORT));
        //db及其db子项 右键菜单 Import start

        new MenuItem(menuDbContainer, SWT.SEPARATOR);//添加分隔符

        //db及其db子项 右键菜单 Find start
        MenuItem menuDbContainerFind = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerFind.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                find();
            }
        });
        menuDbContainerFind.setText(i18nFile.getText(I18nFile.FIND));
        //db及其db子项 右键菜单 Find start

        //db及其db子项 右键菜单 Find forward start
        MenuItem menuDbContainerFindForward = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerFindForward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                findForward();
            }
        });
        menuDbContainerFindForward.setText(i18nFile.getText(I18nFile.FINDFORWARD));
        //db及其db子项 右键菜单 Find forward start

        //db及其db子项 右键菜单 Find backward start
        MenuItem menuDbContainerFindBackward = new MenuItem(menuDbContainer, SWT.NONE);
        menuDbContainerFindBackward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                findBackward();
            }
        });
        menuDbContainerFindBackward.setText(i18nFile.getText(I18nFile.FINDBACKWARD));
        //db及其db子项 右键菜单 Find backward start

        return menuDbContainer;
    }

    private void initTable(CTabFolder tabFolder) {
        table = new Table(tabFolder, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        mainTabItem.setControl(table);
        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                tableItemSelected();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                TreeItem[] treeItems = tree.getSelection();
                Point point = new Point(e.x, e.y);
                TableItem selectedItem = table.getItem(point);

                if (selectedItem != null) {
                    NodeType type = (NodeType) selectedItem.getData(NODE_TYPE);

                    if (type == NodeType.CONTAINER || type == NodeType.DATABASE || type == NodeType.SERVER) {
                        boolean find = false;
                        for (TreeItem treeItem : treeItems[0].getItems()) {
                            String treeText = treeItem.getText();
                            String tableText = selectedItem.getText(0);

                            if (treeText.equals(tableText)) {
                                find = true;

                                if (type == NodeType.CONTAINER || type == NodeType.DATABASE) {
                                    dbContainerTreeItemSelected(treeItem, false);
                                } else if (type == NodeType.SERVER) {
                                    serverTreeItemSelected(treeItem, false);
                                }

                                history.add(treeItem);
                                btnBackward.setEnabled(true);
                                btnForward.setEnabled(false);
                                break;
                            }
                        }
                        if (!find) {
                            MessageDialog.openInformation(shell, i18nFile.getText(I18nFile.INFORMATION), i18nFile.getText(I18nFile.NEWKEYFOUND)  + text.getText());
                        }
                    } else {
                        dataProperties();
                    }
                }
            }

            @Override
            public void mouseDown(MouseEvent e) {
                Point point = new Point(e.x, e.y);
                TableItem selectedItem = table.getItem(point);
                if (e.button == 3) {
                    if (selectedItem == null) {
                        table.setMenu(menu_null);
                    } else {
                        NodeType type = (NodeType) selectedItem.getData(NODE_TYPE);

                        if (null != type) {
                            switch (type) {
                                case ROOT:
                                    table.setMenu(menu_null);
                                    break;
                                case SERVER:
                                    updateMenuServer(true, menuTableServer);
                                    table.setMenu(menuTableServer);
                                    break;
                                case CONTAINER: {
                                    TableItem[] items = table.getSelection();
                                    if (items.length == 1) {
                                        updateMenuDBContainer(type, menuTableDBContainer);
                                        table.setMenu(menuTableDBContainer);
                                    } else {
                                        table.setMenu(menu_Multi);
                                    }
                                    break;
                                }
                                case DATABASE:
                                    updateMenuDBContainer(type, menuTableDBContainer);
                                    table.setMenu(menuTableDBContainer);
                                    break;
                                default: {
                                    TableItem[] items = table.getSelection();
                                    if (items.length == 1) {
                                        table.setMenu(menuDataExplorer);
                                    } else {
                                        table.setMenu(menu_Multi);
                                    }
                                    break;
                                }
                            }
                        }

                    }
                }
            }
        });
        table.setHeaderVisible(true);

        tableColumnName = new TableColumn(table, SWT.NONE);
        tableColumnName.setWidth(150);
        tableColumnName.setText(i18nFile.getText(I18nFile.NAME));
        tableColumnName.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                orderColumnSelected(OrderBy.NAME);

            }
        });

        tablecolumnType = new TableColumn(table, SWT.NONE);
        tablecolumnType.setWidth(150);
        tablecolumnType.setText(i18nFile.getText(I18nFile.TYPE));
        tablecolumnType.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                orderColumnSelected(OrderBy.TYPE);
            }
        });

        tablecolumnSize = new TableColumn(table, SWT.NONE);
        tablecolumnSize.setWidth(100);
        tablecolumnSize.setText(i18nFile.getText(I18nFile.SIZE));
        tablecolumnSize.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                orderColumnSelected(OrderBy.SIZE);
            }
        });
    }

    /**
     * 更新Server菜单
     * @param isTable
     * @param menu 
     */
    protected void updateMenuServer(boolean isTable, Menu menu) {
        if (isTable && table.getSelectionCount() > 1) {
            menu.getItem(0).setEnabled(false);
            menu.getItem(1).setEnabled(true);
            menu.getItem(2).setEnabled(false);
            menu.getItem(4).setEnabled(false);
            menu.getItem(5).setEnabled(false);
            menu.getItem(6).setEnabled(false);
        } else {
            menu.getItem(0).setEnabled(true);
            menu.getItem(1).setEnabled(true);
            menu.getItem(2).setEnabled(true);
            menu.getItem(4).setEnabled(true);
            menu.getItem(5).setEnabled(true);
            menu.getItem(6).setEnabled(true);
        }
    }

    /**
     * 
     */
    protected void dbContainerProperties() {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, cinfo);

        NodeType type = (NodeType) treeItem.getData(NODE_TYPE);

        String str;
        String container;

        if (type == NodeType.DATABASE) {
            str = i18nFile.getText(I18nFile.TYPE) + ":\t"
                    + i18nFile.getText(I18nFile.DATABASE) + "\n"
                    + i18nFile.getText(I18nFile.LOCATION) + ":\t"
                    + getLocation(cinfo) + "\n"
                    + i18nFile.getText(I18nFile.KEY) + ":\t";
            container = "";
        } else {
            str = i18nFile.getText(I18nFile.TYPE) + ":\t"
                    + i18nFile.getText(I18nFile.CONTAINER) + "\n"
                    + i18nFile.getText(I18nFile.LOCATION) + ":\t"
                    + getLocation(cinfo) + "\n"
                    + i18nFile.getText(I18nFile.KEY) + ":\t";
            container = cinfo.getContainerStr();
        }

        Set<Node> nodes = nodeService.listContainerAllKeys(cinfo.getId(), cinfo.getDb(), container);
        str += nodes.size();

        String properties;

        if (type == NodeType.DATABASE) {
            properties = getLocation(cinfo) + " " + i18nFile.getText(I18nFile.DBPROPERTIES);
        } else {
            properties = getLocation(cinfo) + " " + i18nFile.getText(I18nFile.CONTAINERPROPERTIES);
        }
        MessageDialog.openInformation(shell, properties, str);

    }

    private void tableItemSelected() {
        TableItem[] items = table.getSelection();
        if (items.length == 1) {
            itemsSelected = items;
            TreeItem[] treeItems = tree.getSelection();
            treeItemSelected = treeItems[0];

            NodeType type = (NodeType) items[0].getData(NODE_TYPE);
            switch (type) {
                case SERVER:
                    serverItemSelected();
                    break;
                case DATABASE:
                case CONTAINER:
                    dbContainerItemSelected(items[0]);
                    break;
                default:
                    dataItemSelected();
                    break;
            }
        } else if (items.length > 1) {
            NodeType type = (NodeType) items[0].getData(NODE_TYPE);
            switch (type) {
                case SERVER:
                    itemsSelected = items;
                    TreeItem[] treeItems = tree.getSelection();
                    treeItemSelected = treeItems[0];
                    serverItemsSelected();
                    break;
                case DATABASE:
                    for (TableItem item : items) {
                        if (item != itemsSelected[0]) {
                            itemsSelected[0] = item;
                            table.setSelection(item);
                            break;
                        }
                    }
                    break;
                default:
                    itemsSelected = items;
                    TreeItem[] treeItems1 = tree.getSelection();
                    treeItemSelected = treeItems1[0];
                    containerItemsSelected();
                    break;

            }

        } else {
            TreeItem[] treeItems = tree.getSelection();
            itemsSelected = treeItems;

            NodeType type = (NodeType) itemsSelected[0].getData(NODE_TYPE);
            switch (type) {
                case SERVER:
                    serverItemSelected();
                    break;
                case DATABASE:
                case CONTAINER:
                    dbContainerItemSelected(itemsSelected[0]);
                    break;
                default:
                    dataItemSelected();
                    break;
            }
        }
    }

    private void containerItemsSelected() {
        menuServer.getItem(1).setEnabled(false);
        menuServer.getItem(2).setEnabled(false);
        menuServer.getItem(3).setEnabled(false);

        menuData.getItem(0).setEnabled(false);
        menuData.getItem(1).setEnabled(false);
        menuData.getItem(2).setEnabled(true);
        menuData.getItem(3).setEnabled(false);

        menuData.getItem(5).setEnabled(true);
        menuData.getItem(6).setEnabled(true);
        menuData.getItem(7).setEnabled(false);

        menuData.getItem(9).setEnabled(false);
        menuData.getItem(10).setEnabled(true);

        menuFavorite.getItem(0).setEnabled(false);
    }

    private void serverItemsSelected() {
        menuServer.getItem(1).setEnabled(false);
        menuServer.getItem(2).setEnabled(true);
        menuServer.getItem(3).setEnabled(false);

        menuData.getItem(0).setEnabled(false);
        menuData.getItem(1).setEnabled(false);
        menuData.getItem(2).setEnabled(false);
        menuData.getItem(3).setEnabled(false);

        menuData.getItem(5).setEnabled(false);
        menuData.getItem(6).setEnabled(false);
        menuData.getItem(7).setEnabled(false);

        menuData.getItem(9).setEnabled(false);
        menuData.getItem(10).setEnabled(false);

        menuFavorite.getItem(0).setEnabled(false);
    }

    private void dataProperties() {
        TableItem[] items = table.getSelection();
        NodeType type = (NodeType) items[0].getData(NODE_TYPE);

        TreeItem treeItem = getTreeItemByTableItem(items[0]);
        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        parseContainer(treeItem, cinfo);
        String key = cinfo.getContainerStr() + items[0].getText();

        if (!nodeService.isKeyExist(cinfo.getId(), cinfo.getDb(), key)) {
            throw new KeyNotExistException(cinfo.getId(), cinfo.getDb(), key);
        }

        if (!openDataContent.isOpen(cinfo.getId(), cinfo.getDb(), key)) {
            final CTabItem tbtmNewItem = new CTabItem(tabFolder_1, SWT.NONE);
            tbtmNewItem.setText("New Item");
            tbtmNewItem.setShowClose(true);
            Composite composite_2 = new Composite(tabFolder_1, SWT.NONE);
            tbtmNewItem.setControl(composite_2);
            composite_2.setLayout(new GridLayout(1, false));
            tbtmNewItem.setText(key);

            tbtmNewItem.addDisposeListener((DisposeEvent e) -> {
                DataContent dataContent = (DataContent) tbtmNewItem
                        .getData();

                openDataContent.remove(dataContent);
                tbtmNewItem.dispose();
            });

            DataContent content = null;
            if (null != type) {
                switch (type) {
                    case STRING:
                        tbtmNewItem.setImage(strImage);
                        content = new StringDataContent(tbtmNewItem, iconImage, cinfo.getId(),
                                cinfo.getServerName(), cinfo.getDb(), key,
                                I18nFile.STRING);
                        break;
                    case HASH:
                        tbtmNewItem.setImage(hashImage);
                        content = new HashDataContent(tbtmNewItem, iconImage, cinfo.getId(),
                                cinfo.getServerName(), cinfo.getDb(), key,
                                I18nFile.HASH);
                        break;
                    case LIST:
                        tbtmNewItem.setImage(listImage);
                        content = new ListDataContent(tbtmNewItem, iconImage, cinfo.getId(),
                                cinfo.getServerName(), cinfo.getDb(), key,
                                I18nFile.LIST);
                        break;
                    case SET:
                        tbtmNewItem.setImage(setImage);
                        content = new SetDataContent(tbtmNewItem, iconImage, cinfo.getId(),
                                cinfo.getServerName(), cinfo.getDb(), key, I18nFile.SET);
                        break;
                    case SORTEDSET:
                        tbtmNewItem.setImage(zsetImage);
                        content = new ZSetDataContent(tbtmNewItem, iconImage, cinfo.getId(),
                                cinfo.getServerName(), cinfo.getDb(), key,
                                I18nFile.ZSET);
                        break;
                    default:
                        break;
                }
            }
            content.setShell(composite_2);
            content.initContents();
            openDataContent.add(content);
            tabFolder_1.setSelection(tbtmNewItem);
        } else {
            tabFolder_1.setSelection(openDataContent.getTabItem(cinfo.getId(),
                    cinfo.getDb(), key));
        }

    }

    protected void dataItemSelected() {
        menuServer.getItem(1).setEnabled(false);
        menuServer.getItem(2).setEnabled(false);
        menuServer.getItem(3).setEnabled(false);

        menuData.getItem(0).setEnabled(false);
        menuData.getItem(1).setEnabled(true);
        menuData.getItem(2).setEnabled(true);
        menuData.getItem(3).setEnabled(true);

        menuData.getItem(5).setEnabled(true);
        menuData.getItem(6).setEnabled(true);
        menuData.getItem(7).setEnabled(false);

        menuData.getItem(9).setEnabled(false);
        menuData.getItem(10).setEnabled(false);

        menuFavorite.getItem(0).setEnabled(true);

    }

    private Menu initMenuTreeServer() {
        Menu menu_server = initMenuTableServer();

        new MenuItem(menu_server, SWT.SEPARATOR);

        MenuItem mntmRefresh_3 = new MenuItem(menu_server, SWT.NONE);
        mntmRefresh_3.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] items = tree.getSelection();
                serverTreeItemSelected(items[0], true);
            }
        });
        mntmRefresh_3.setText(i18nFile.getText(I18nFile.REFRESH));

        return menu_server;
    }

    private Menu initMenuTableServer() {
        Menu menu_server_1 = new Menu(shell);

        MenuItem mntmUpdate = new MenuItem(menu_server_1, SWT.NONE);
        mntmUpdate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateServer();
            }
        });
        mntmUpdate.setText(i18nFile.getText(I18nFile.UPDATE));

        MenuItem mntmDelete = new MenuItem(menu_server_1, SWT.NONE);
        mntmDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                removeServer();
            }
        });
        mntmDelete.setText(i18nFile.getText(I18nFile.REMOVE));

        MenuItem mntmProperties_4 = new MenuItem(menu_server_1, SWT.NONE);
        mntmProperties_4.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                serverProperties();
            }
        });
        mntmProperties_4.setText(i18nFile.getText(I18nFile.PROPERTIES));

        new MenuItem(menu_server_1, SWT.SEPARATOR);

        MenuItem mntmNewItem_1 = new MenuItem(menu_server_1, SWT.NONE);
        mntmNewItem_1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                console();
            }
        });
        mntmNewItem_1.setText(i18nFile.getText(I18nFile.CONSOLE));

        MenuItem mntmNewItem_2 = new MenuItem(menu_server_1, SWT.NONE);
        mntmNewItem_2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                publish();
            }
        });
        mntmNewItem_2.setText(i18nFile.getText(I18nFile.PUBLISH));

        MenuItem mntmNewItem_3 = new MenuItem(menu_server_1, SWT.NONE);
        mntmNewItem_3.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                subscribe();
            }
        });
        mntmNewItem_3.setText(i18nFile.getText(I18nFile.SUBSCRIBE));

        new MenuItem(menu_server_1, SWT.SEPARATOR);

        MenuItem menuItem_2 = new MenuItem(menu_server_1, SWT.NONE);
        menuItem_2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                find();
            }
        });
        menuItem_2.setText(i18nFile.getText(I18nFile.FIND));

        MenuItem mntmFindForward = new MenuItem(menu_server_1, SWT.NONE);
        mntmFindForward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                findForward();
            }
        });
        mntmFindForward.setText(i18nFile.getText(I18nFile.FINDFORWARD));

        MenuItem mntmFindBackward = new MenuItem(menu_server_1, SWT.NONE);
        mntmFindBackward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                findBackward();
            }
        });
        mntmFindBackward.setText(i18nFile.getText(I18nFile.FINDBACKWARD));

        return menu_server_1;
    }

    private void initMenuNull() {
        menu_null = new Menu(shell);

        MenuItem mntmNewConnection = new MenuItem(menu_null, SWT.NONE);
        mntmNewConnection.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                addServer();
            }
        });
        mntmNewConnection.setText(i18nFile.getText(I18nFile.ADDSERVER));

        new MenuItem(menu_null, SWT.SEPARATOR);

        MenuItem mntmFind = new MenuItem(menu_null, SWT.NONE);
        mntmFind.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                find();
            }
        });
        mntmFind.setText(i18nFile.getText(I18nFile.FIND));

        MenuItem mntmFindNext = new MenuItem(menu_null, SWT.NONE);
        mntmFindNext.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                findForward();
            }
        });
        mntmFindNext.setText(i18nFile.getText(I18nFile.FINDFORWARD));

        MenuItem mntmFindBackward_2 = new MenuItem(menu_null, SWT.NONE);
        mntmFindBackward_2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                findBackward();
            }
        });
        mntmFindBackward_2.setText(i18nFile.getText(I18nFile.FINDBACKWARD));

        new MenuItem(menu_null, SWT.SEPARATOR);

        MenuItem mntmRefresh = new MenuItem(menu_null, SWT.NONE);
        mntmRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                rootTreeItemSelected(true);
            }
        });
        mntmRefresh.setText(i18nFile.getText(I18nFile.REFRESH));
    }

    /**
     * 初始化 top menu 
     */
    private void initMenu() {
        //top menu 
        menu = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menu);

        //top menu => Server start
        MenuItem topMenuServer = new MenuItem(menu, SWT.CASCADE);
        topMenuServer.setText(i18nFile.getText(I18nFile.SERVER));

        menuServer = new Menu(topMenuServer);
        topMenuServer.setMenu(menuServer);

        //Server =》 Add start
        MenuItem menuServerAdd = new MenuItem(menuServer, SWT.NONE);
        menuServerAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                addServer();
            }
        });
        menuServerAdd.setText(i18nFile.getText(I18nFile.ADD));
        //Server =》 Add end

        //Server =》 Update start
        MenuItem menuServerUpdate = new MenuItem(menuServer, SWT.NONE);
        menuServerUpdate.setEnabled(false);
        menuServerUpdate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateServer();
            }
        });
        menuServerUpdate.setText(i18nFile.getText(I18nFile.UPDATE));
        //Server =》 Update end

        //Server =》 Remove start
        MenuItem menuServerRemove = new MenuItem(menuServer, SWT.NONE);
        menuServerRemove.setEnabled(false);
        menuServerRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                removeServer();
            }
        });
        menuServerRemove.setText(i18nFile.getText(I18nFile.REMOVE));
        //Server =》 Remove end

        //Server =》 Properties start
        MenuItem menuServerProperties = new MenuItem(menuServer, SWT.NONE);
        menuServerProperties.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                serverProperties();
            }
        });
        menuServerProperties.setEnabled(false);
        menuServerProperties.setText(i18nFile.getText(I18nFile.PROPERTIES));
        //Server =》 Properties end
        
        new MenuItem(menuServer, SWT.SEPARATOR); //Server 分隔符

        //Server =》 Exit start
        MenuItem menuServerExit = new MenuItem(menuServer, SWT.NONE);
        menuServerExit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                shell.close();
            }
        });
        menuServerExit.setText(i18nFile.getText(I18nFile.EXIT));
        //Server =》 Exit end
        //top menu => Server end
        
        //top menu => Data start
        MenuItem topMenuItemData = new MenuItem(menu, SWT.CASCADE);
        topMenuItemData.setText(i18nFile.getText(I18nFile.DATA));

        menuData = new Menu(topMenuItemData);
        topMenuItemData.setMenu(menuData);

        //Data =》 New start
        MenuItem menuItemDataNew = new MenuItem(menuData, SWT.CASCADE);
        menuItemDataNew.setEnabled(false);
        menuItemDataNew.setText(i18nFile.getText(I18nFile.NEW));

        Menu menuDataNew = new Menu(menuItemDataNew);
        menuItemDataNew.setMenu(menuDataNew);

        //Data =》 New =》 String start
        MenuItem menuDataNewString = new MenuItem(menuDataNew, SWT.NONE);
        menuDataNewString.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newString();
            }
        });
        menuDataNewString.setText(i18nFile.getText(I18nFile.STRING) + "\tAlt+1");
        menuDataNewString.setAccelerator(SWT.ALT + '1');
        menuDataNewString.setImage(strImage);
        //Data =》 New =》 String end
        
        //Data =》 New =》 List start
        MenuItem menuDataNewList = new MenuItem(menuDataNew, SWT.NONE);
        menuDataNewList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newList();
            }
        });
        menuDataNewList.setText(i18nFile.getText(I18nFile.LIST) + "\tAlt+2");
        menuDataNewList.setAccelerator(SWT.ALT + '2');
        menuDataNewList.setImage(listImage);
        //Data =》 New =》 List end
        
        //Data =》 New =》 Set start
        MenuItem menuDataNewSet = new MenuItem(menuDataNew, SWT.NONE);
        menuDataNewSet.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newSet();
            }
        });
        menuDataNewSet.setText(i18nFile.getText(I18nFile.SET) + "\tAlt+3");
        menuDataNewSet.setAccelerator(SWT.ALT + '3');
        menuDataNewSet.setImage(setImage);
        //Data =》 New =》 Set end
        
        //Data =》 New =》 Sorted set start
        MenuItem menuDataNewSortset = new MenuItem(menuDataNew, SWT.NONE);
        menuDataNewSortset.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newZSet();
            }
        });
        menuDataNewSortset.setText(i18nFile.getText(I18nFile.ZSET) + "\tAlt+4");
        menuDataNewSortset.setAccelerator(SWT.ALT + '4');
        menuDataNewSortset.setImage(zsetImage);
        //Data =》 New =》 Sorted set end
        
        //Data =》 New =》 hash start
        MenuItem menuDataNewHash = new MenuItem(menuDataNew, SWT.NONE);
        menuDataNewHash.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newHash();
            }
        });
        menuDataNewHash.setText(i18nFile.getText(I18nFile.HASH) + "\tAlt+5");
        menuDataNewHash.setAccelerator(SWT.ALT + '5');
        menuDataNewHash.setImage(hashImage);
        //Data =》 New =》 hash end
        
        //Data =》 Rename start
        MenuItem menuDataRename = new MenuItem(menuData, SWT.NONE);
        menuDataRename.setEnabled(false);
        menuDataRename.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (itemsSelected[0] instanceof TreeItem) {
                    renameContainer();
                } else {
                    renameKey();
                }
            }
        });
        menuDataRename.setText(i18nFile.getText(I18nFile.RENAME));
        //Data =》 Rename end

        //Data =》 Delete start
        MenuItem menuDataDelete = new MenuItem(menuData, SWT.NONE);
        menuDataDelete.setEnabled(false);
        menuDataDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (itemsSelected[0] instanceof TreeItem) {
                    deleteOneContainer();
                } else if (itemsSelected.length == 1) {
                    NodeType type = (NodeType) itemsSelected[0]
                            .getData(NODE_TYPE);
                    if (type == NodeType.CONTAINER) {
                        deleteOneContainer();
                    } else {
                        deleteOneKey();
                    }
                } else if (itemsSelected.length > 1) {
                    deleteKeys();
                }

            }
        });
        menuDataDelete.setText(i18nFile.getText(I18nFile.DELETE));
        //Data =》 Delete start
        
        //Data =》 Properties start
        MenuItem menuDataProperties = new MenuItem(menuData, SWT.NONE);
        menuDataProperties.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                NodeType type = (NodeType) itemsSelected[0].getData(NODE_TYPE);

                if (type == NodeType.CONTAINER || type == NodeType.DATABASE) {
                    dbContainerProperties();
                } else {
                    dataProperties();
                }

            }
        });
        menuDataProperties.setEnabled(false);
        menuDataProperties.setText(i18nFile.getText(I18nFile.PROPERTIES));
        //Data =》 Properties end
        
        new MenuItem(menuData, SWT.SEPARATOR);//分隔符

        //Data =》 Cut start
        MenuItem menuDataCut = new MenuItem(menuData, SWT.NONE);
        menuDataCut.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cut();
            }
        });
        menuDataCut.setEnabled(false);
        menuDataCut.setText(i18nFile.getText(I18nFile.CUT) + "\tCtrl+X");
        menuDataCut.setAccelerator(SWT.CTRL + 'X');
        //Data =》 Cut end
        
        //Data =》 Copy start
        MenuItem menuDataCopy = new MenuItem(menuData, SWT.NONE);
        menuDataCopy.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copy();
            }
        });
        menuDataCopy.setEnabled(false);
        menuDataCopy.setText(i18nFile.getText(I18nFile.COPY) + "\tCtrl+C");
        menuDataCopy.setAccelerator(SWT.CTRL + 'C');
        //Data =》 Copy end
        
        //Data =》 Paste start 
        MenuItem menuDataPaste = new MenuItem(menuData, SWT.NONE);
        menuDataPaste.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                paste();
            }
        });
        menuDataPaste.setEnabled(false);
        menuDataPaste.setText(i18nFile.getText(I18nFile.PASTE) + "\tCtrl+V");
        menuDataPaste.setAccelerator(SWT.CTRL + 'V');
        //Data =》 Paste end
        
        new MenuItem(menuData, SWT.SEPARATOR);//分隔符

        //Data =》 Import start 
        MenuItem menuDataImport = new MenuItem(menuData, SWT.NONE);
        menuDataImport.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                importFile();
            }
        });
        menuDataImport.setEnabled(false);
        menuDataImport.setText(i18nFile.getText(I18nFile.IMPORT));
        //Data =》 Import end 
        //Data =》 Export start 
        MenuItem menuDataExport = new MenuItem(menuData, SWT.NONE);
        menuDataExport.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                export();
            }
        });
        menuDataExport.setEnabled(false);
        menuDataExport.setText(i18nFile.getText(I18nFile.EXPORT));
        //Data =》 Export end
        
        new MenuItem(menuData, SWT.SEPARATOR); //分隔符

        //Data =》 Find start
        MenuItem menuDataFind = new MenuItem(menuData, SWT.NONE);
        menuDataFind.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                find();
            }
        });
        menuDataFind.setText(i18nFile.getText(I18nFile.FIND) + "\tCtrl+F");
        menuDataFind.setAccelerator(SWT.CTRL + 'F');
        //Data =》 Find end
        
        //Data =》 Find forward start
        MenuItem menuDataFindForward = new MenuItem(menuData, SWT.NONE);
        menuDataFindForward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                findForward();
            }
        });
        menuDataFindForward.setText(i18nFile.getText(I18nFile.FINDFORWARD) + "\tF3");
        menuDataFindForward.setAccelerator(SWT.F3);
        //Data =》 Find forward end
        
        //Data =》 Find backward start
        MenuItem menuDataFindBackward = new MenuItem(menuData, SWT.NONE);
        menuDataFindBackward.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                findBackward();
            }
        });
        menuDataFindBackward.setText(i18nFile.getText(I18nFile.FINDBACKWARD) + "\tCtrl+F3");
        menuDataFindBackward.setAccelerator(SWT.CTRL + SWT.F3);
        //Data =》 Find backward end
        //top menu => Data end
        
        //top menu => View start
        MenuItem topMenuItemView = new MenuItem(menu, SWT.CASCADE);
        topMenuItemView.setText(i18nFile.getText(I18nFile.VIEW));

        menuView = new Menu(topMenuItemView);
        topMenuItemView.setMenu(menuView);

        //View =》 Container start
        MenuItem menuItemViewContainer = new MenuItem(menuView, SWT.CASCADE);
        menuItemViewContainer.setText(i18nFile.getText(I18nFile.CONTAINER));

        Menu menuViewContainer = new Menu(menuItemViewContainer);
        menuItemViewContainer.setMenu(menuViewContainer);

        //View =》 Container => Hierarchy start
        MenuItem menuViewHierarchy = new MenuItem(menuViewContainer, SWT.RADIO);
        menuViewHierarchy.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (flatView == true) {
                    TreeItem[] treeItems = tree.getSelection();

                    ContainerKeyInfo cinfo = new ContainerKeyInfo();
                    parseContainer(treeItems[0], cinfo);

                    flatView = false;
                    updateView(cinfo);
                }
            }
        });
        menuViewHierarchy.setText(i18nFile.getText(I18nFile.HIERARCHY));
        //View =》 Container => Hierarchy end
        
        //View =》 Container => Flat start
        MenuItem menuViewFlat = new MenuItem(menuViewContainer, SWT.RADIO);
        menuViewFlat.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (flatView == false) {
                    TreeItem[] treeItems = tree.getSelection();

                    ContainerKeyInfo cinfo = new ContainerKeyInfo();
                    parseContainer(treeItems[0], cinfo);

                    flatView = true;
                    updateView(cinfo);
                }
            }
        });
        menuViewFlat.setText(i18nFile.getText(I18nFile.FLAT));
        //View =》 Container => Flat start
        
        if (!flatView) {
            menuViewHierarchy.setSelection(true);
        }
        if (flatView) {
            menuViewFlat.setSelection(true);
        }

        //View =》 Order by  start
        MenuItem menuItemViewOrderBy = new MenuItem(menuView, SWT.CASCADE);
        menuItemViewOrderBy.setText(i18nFile.getText(I18nFile.ORDERBY));

        Menu menuViewOrderBy = new Menu(menuItemViewOrderBy);
        menuItemViewOrderBy.setMenu(menuViewOrderBy);

        //View =》 Order by => Name start
        menuViewOrderbyName = new MenuItem(menuViewOrderBy, SWT.RADIO);
        menuViewOrderbyName.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                orderMenuSelected(OrderBy.NAME);
            }
        });
        menuViewOrderbyName.setText(i18nFile.getText(I18nFile.NAME));
        //View =》 Order by => Name end
        
        //View =》 Order by => Type start
        menuViewOrderbyType = new MenuItem(menuViewOrderBy, SWT.RADIO);
        menuViewOrderbyType.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                orderMenuSelected(OrderBy.TYPE);
            }
        });
        menuViewOrderbyType.setText(i18nFile.getText(I18nFile.TYPE));
        //View =》 Order by => Type end
        
        //View =》 Order by => Size start
        menuViewOrderbySize = new MenuItem(menuViewOrderBy, SWT.RADIO);
        menuViewOrderbySize.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                orderMenuSelected(OrderBy.SIZE);
            }
        });
        menuViewOrderbySize.setText(i18nFile.getText(I18nFile.SIZE));
        //View =》 Order by => Size end
        
        updateOrderby();
        //View =》 Order start
        MenuItem menuItemViewOrder = new MenuItem(menuView, SWT.CASCADE);
        menuItemViewOrder.setText(i18nFile.getText(I18nFile.ORDER));

        Menu menuViewOrder = new Menu(menuItemViewOrder);
        menuItemViewOrder.setMenu(menuViewOrder);

        //View =》 Order  => Ascend start
        menuViewOrderAscend = new MenuItem(menuViewOrder, SWT.RADIO);
        menuViewOrderAscend.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (clientOrder == Order.Descend) {
                    orderMenuSelected(Order.Ascend);
                }
            }
        });
        menuViewOrderAscend.setText(i18nFile.getText(I18nFile.ASCEND));
        //View =》 Order  => Ascend end
        
        //View =》 Order  => Descend start
        menuViewOrderDescend = new MenuItem(menuViewOrder, SWT.RADIO);
        menuViewOrderDescend.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (clientOrder == Order.Ascend) {
                    orderMenuSelected(Order.Descend);
                }
            }
        });
        menuViewOrderDescend.setText(i18nFile.getText(I18nFile.DESCEND));
        //View =》 Order  => Descend end
        
        updateOrder();

        //View =》Language start
        MenuItem menuItemViewLanguage = new MenuItem(menuView, SWT.CASCADE);
        menuItemViewLanguage.setText(i18nFile.getText(I18nFile.LANGUAGE));

        Menu menuViewLanguage = new Menu(menuItemViewLanguage);
        menuItemViewLanguage.setMenu(menuViewLanguage);

        //View =》Language => English start
        MenuItem menuViewLanguageEnglish = new MenuItem(menuViewLanguage, SWT.RADIO);
        menuViewLanguageEnglish.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (language == Language.Chinese) {
                    language = Language.English;
                    refreshLangUI();
                }
            }
        });
        menuViewLanguageEnglish.setText("English");
        //View =》Language => English start

        //View =》Language => 中文 start
        MenuItem menuViewLanguageChinese = new MenuItem(menuViewLanguage, SWT.RADIO);
        menuViewLanguageChinese.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (language == Language.English) {
                    language = Language.Chinese;
                    refreshLangUI();
                }
            }
        });
        menuViewLanguageChinese.setText("中文");
        //View =》Language => 中文 end
        
        if (language == Language.English) {
            menuViewLanguageEnglish.setSelection(true);
        }
        if (language == Language.Chinese) {
            menuViewLanguageChinese.setSelection(true);
        }

        new MenuItem(menuView, SWT.SEPARATOR);//分隔符

        //View =》 Refresh start
        MenuItem menuItemViewRefresh = new MenuItem(menuView, SWT.NONE);
        menuItemViewRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshOrder();

                treeItemSelected(true);
            }
        });
        menuItemViewRefresh.setText(i18nFile.getText(I18nFile.REFRESH) + "\tF5");
        menuItemViewRefresh.setAccelerator(SWT.F5);
        //View =》 Refresh end
        //top menu => View end
        
         //top menu => Tools start
        MenuItem topMenuItemTools = new MenuItem(menu, SWT.CASCADE);
        topMenuItemTools.setText(i18nFile.getText(I18nFile.TOOL));

        menuTools = new Menu(topMenuItemTools);
        topMenuItemTools.setMenu(menuTools);

        //Tools =》 Console start
        MenuItem menuToolsConsole = new MenuItem(menuTools, SWT.NONE);
        menuToolsConsole.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                console();
            }
        });
        menuToolsConsole.setText(i18nFile.getText(I18nFile.CONSOLE));
        //Tools =》 Console end
        
        //Tools =》 Publish start
        MenuItem menuToolsPublish = new MenuItem(menuTools, SWT.NONE);
        menuToolsPublish.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                publish();
            }
        });
        menuToolsPublish.setText(i18nFile.getText(I18nFile.PUBLISH));
        //Tools =》 Publish end
        
        //Tools =》 Subscribe start
        MenuItem menuToolsSubscribe = new MenuItem(menuTools, SWT.NONE);
        menuToolsSubscribe.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                subscribe();
            }
        });
        menuToolsSubscribe.setText(i18nFile.getText(I18nFile.SUBSCRIBE));
        //Tools =》 Subscribe end
        
        new MenuItem(menuTools, SWT.SEPARATOR);//分隔符

        //Tools =》 Options start
        MenuItem menuToolsOptions = new MenuItem(menuTools, SWT.NONE);
        menuToolsOptions.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                options();
            }
        });
        menuToolsOptions.setText(i18nFile.getText(I18nFile.OPTIONS));
        //Tools =》 Options end
        //top menu => Tools end
        
        //top menu => Favorites start
        MenuItem menuItemFavorites = new MenuItem(menu, SWT.CASCADE);
        menuItemFavorites.setText(i18nFile.getText(I18nFile.FAVORITES));

        menuFavorite = new Menu(menuItemFavorites);
        menuItemFavorites.setMenu(menuFavorite);

        //Favorite =》 Add start
        MenuItem menuFavoriteAdd = new MenuItem(menuFavorite, SWT.NONE);
        menuFavoriteAdd.setEnabled(false);
        menuFavoriteAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addFavorite();
            }
        });
        menuFavoriteAdd.setText(i18nFile.getText(I18nFile.ADD));
        //Favorite =》 Add end
        
        //Favorite =》 Organize start
        MenuItem menuFavoriteOrganize = new MenuItem(menuFavorite, SWT.NONE);
        menuFavoriteOrganize.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                OrganizeFavoriteDialog dialog = new OrganizeFavoriteDialog(shell, iconImage);

                @SuppressWarnings("unchecked")
                List<Favorite> favorites = (List<Favorite>) dialog.open();
                if (favorites != null) {
                    favoriteService.updateList(favorites);

                    removeFavoriteMenuItem();
                    addFavoriteMenuItem();
                }

            }
        });
        menuFavoriteOrganize.setText(i18nFile.getText(I18nFile.ORGANIZE));
        //Favorite =》 Organize end
        
        
        addFavoriteMenuItem();
        //top menu => Favorite end

        //top menu => Help start
        MenuItem menuItemHelp = new MenuItem(menu, SWT.CASCADE);
        menuItemHelp.setText(i18nFile.getText(I18nFile.HELP));

        Menu menuHelp = new Menu(menuItemHelp);
        menuItemHelp.setMenu(menuHelp);

        //Help => Donation start
        MenuItem menuHelpDonation = new MenuItem(menuHelp, SWT.NONE);
        menuHelpDonation.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                DonationDialog dialog = new DonationDialog(shell, iconImage, codeImage);
                dialog.open();
            }
        });
        menuHelpDonation.setText(i18nFile.getText(I18nFile.DONATION));
        //Help => Donation end
                
        new MenuItem(menuHelp, SWT.SEPARATOR);//分隔符

        //Help => About start
        MenuItem menuHelpAbout = new MenuItem(menuHelp, SWT.NONE);
        menuHelpAbout.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                AboutDialog dialog = new AboutDialog(shell, iconImage);
                dialog.open();
            }
        });
        menuHelpAbout.setText(i18nFile.getText(I18nFile.ABOUT));
        //Help => About end
        //top menu => Help end
    }

    private void options() {
        OptionsDialog dialog = new OptionsDialog(shell, iconImage);
        dialog.open();
    }

    private void updateOrder() {
        menuViewOrderAscend.setSelection(false);
        menuViewOrderDescend.setSelection(false);

        switch (clientOrder) {
            case Ascend:
                menuViewOrderAscend.setSelection(true);
                break;
            case Descend:
                menuViewOrderDescend.setSelection(true);
                break;
        }
    }

    private void updateOrderby() {
        menuViewOrderbyName.setSelection(false);
        menuViewOrderbyType.setSelection(false);
        menuViewOrderbySize.setSelection(false);

        switch (clientOrderBy) {
            case NAME:
                menuViewOrderbyName.setSelection(true);
                break;
            case TYPE:
                menuViewOrderbyType.setSelection(true);
                break;
            case SIZE:
                menuViewOrderbySize.setSelection(true);
                break;
        }
    }

    protected void findForward() {
        if (fBuffer == null) {
            find();
        } else {
            Node node = nodeService.findNext(fBuffer.getFindNode(), fBuffer.getSearchFrom(), fBuffer.getId(), fBuffer.getDb(), fBuffer.getContainer(), fBuffer.getSearchNodeType(), fBuffer.getPattern(), true);
            if (node != null) {
                TreeItem selected = gotoDBContainer(node.getId(), node.getDb(), node.getKey(), true, true);
                history.add(selected);
                btnBackward.setEnabled(true);
                btnForward.setEnabled(false);

                fBuffer.setFindNode(node);
            } else {
                boolean ok = MessageDialog.openConfirm(shell, i18nFile.getText(I18nFile.FINDFORWARD), i18nFile.getText(I18nFile.FINDAGAIN));
                if (ok) {
                    Set<Node> nodes = nodeService.find(fBuffer.getSearchFrom(), fBuffer.getId(), fBuffer.getDb(), fBuffer.getContainer(),  fBuffer.getSearchNodeType(), fBuffer.getPattern(), true);
                    if (!nodes.isEmpty()) {
                        Node node1 = nodes.iterator().next();
                        TreeItem selected = gotoDBContainer(node1.getId(), node1.getDb(), node1.getKey(), true, true);
                        history.add(selected);
                        btnBackward.setEnabled(true);
                        btnForward.setEnabled(false);

                        fBuffer.setFindNode(node1);
                    } else {
                        MessageDialog.openInformation(shell, i18nFile.getText(I18nFile.FINDRESULTS), i18nFile.getText(I18nFile.NOFOUND));
                    }
                }
            }
        }
    }

    protected void findBackward() {
        if (fBuffer == null) {
            find();
        } else {
            Node node = nodeService.findNext(fBuffer.getFindNode(), fBuffer.getSearchFrom(), fBuffer.getId(), fBuffer.getDb(), fBuffer.getContainer(), fBuffer.getSearchNodeType(), fBuffer.getPattern(), false);
            if (node != null) {
                TreeItem selected = gotoDBContainer(node.getId(), node.getDb(), node.getKey(), true, true);

                history.add(selected);
                btnBackward.setEnabled(true);
                btnForward.setEnabled(false);

                fBuffer.setFindNode(node);
            } else {
                boolean ok = MessageDialog.openConfirm(shell, i18nFile.getText(I18nFile.FINDBACKWARD), i18nFile.getText(I18nFile.FINDAGAIN));
                if (ok) {
                    Set<Node> nodes = nodeService.find(fBuffer.getSearchFrom(), fBuffer.getId(), fBuffer.getDb(), fBuffer.getContainer(), fBuffer.getSearchNodeType(), fBuffer.getPattern(), false);
                    if (!nodes.isEmpty()) {
                        Node node1 = nodes.iterator().next();
                        TreeItem selected = gotoDBContainer(node1.getId(), node1.getDb(), node1.getKey(), true, true);

                        history.add(selected);
                        btnBackward.setEnabled(true);
                        btnForward.setEnabled(false);

                        fBuffer.setFindNode(node1);
                    } else {
                        MessageDialog.openInformation(shell, i18nFile.getText(I18nFile.FINDRESULTS), i18nFile.getText(I18nFile.NOFOUND));
                    }
                }
            }
        }
    }

    protected void find() {
        FindKeyDialog dialog = new FindKeyDialog(shell, iconImage);
        FindInfo info = (FindInfo) dialog.open();
        if (info != null) {
            TreeItem treeItem;

            ContainerKeyInfo cinfo = new ContainerKeyInfo();
            if (itemsSelected[0] instanceof TreeItem) {
                treeItem = (TreeItem) itemsSelected[0];
            } else {
                treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
            }

            parseContainer(treeItem, cinfo);
            NodeType searchFrom = (NodeType) treeItem.getData(NODE_TYPE);

            Set<Node> nodes = nodeService.find(searchFrom, cinfo.getId(), cinfo.getDb(), cinfo.getContainerStr(), info.getSearchNodeType(), info.getPattern(), info.isForward());
            if (!nodes.isEmpty()) {
                Node node = nodes.iterator().next();
                TreeItem selected = gotoDBContainer(node.getId(), node.getDb(), node.getKey(), true, true);
                history.add(selected);
                btnBackward.setEnabled(true);
                btnForward.setEnabled(false);

                fBuffer = new FindBuffer(node, searchFrom, cinfo.getId(), cinfo.getDb(), cinfo.getContainerStr(), info.getSearchNodeType(), info.getPattern());
            } else {
                MessageDialog.openInformation(shell, i18nFile.getText(I18nFile.FINDRESULTS), i18nFile.getText(I18nFile.NOFOUND));
            }
        }
    }

    private void export() {
        FileDialog dialog = new FileDialog(shell, SWT.SAVE);
        dialog.setText(i18nFile.getText(I18nFile.EXPORTREDIS));
        String[] filterExt = {"*.*"};
        dialog.setFilterExtensions(filterExt);
        String file = dialog.open();
        if (file != null) {
            File exportFile = new File(file);

            boolean ok = false;
            boolean exist = exportFile.exists();
            if (exist) {
                ok = MessageDialog.openConfirm(shell, i18nFile.getText(I18nFile.FILEEXIST), i18nFile.getText(I18nFile.FILEREPLACE));
            }
            if (!exist || ok) {
                for (Item item : itemsSelected) {
                    TreeItem treeItem;

                    ContainerKeyInfo cinfo = new ContainerKeyInfo();
                    if (item instanceof TreeItem) {
                        treeItem = (TreeItem) item;
                    } else {
                        treeItem = getTreeItemByTableItem((TableItem) item);
                    }

                    parseContainer(treeItem, cinfo);

                    exportOne(cinfo, file, item);
                }
            }
        }
    }

    private void exportOne(ContainerKeyInfo cinfo, String file, Item item) {
        ContainerKey containerKey = cinfo.getContainer();

        if (item instanceof TableItem) {
            NodeType type = (NodeType) item.getData(NODE_TYPE);
            if (type != NodeType.CONTAINER && type != NodeType.DATABASE) {
                String con = containerKey == null ? "" : containerKey.getContainerKey();
                containerKey = new ContainerKey(con + item.getText());
            }
        }

        ExportService service = new ExportService(file, cinfo.getId(), cinfo.getDb(), containerKey);
        try {
            service.export();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void importFile() {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, cinfo);

        FileDialog dialog = new FileDialog(shell, SWT.OPEN);
        dialog.setText(i18nFile.getText(I18nFile.IMPORTREDIS));
        String[] filterExt = {"*.*"};
        dialog.setFilterExtensions(filterExt);
        String file = dialog.open();
        if (file != null) {
            ImportService service = new ImportService(file, cinfo.getId(), cinfo.getDb());
            try {
                service.importFile();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }

            dbContainerTreeItemSelected(treeItem, true);
        }
    }

    private void removeFavoriteMenuItem() {
        int num = menuFavorite.getItemCount();
        if (num > 2) {
            MenuItem[] items = menuFavorite.getItems();
            for (int i = 2; i < num; i++) {
                items[i].dispose();
            }
        }
    }

    private void addFavoriteMenuItem() {
        List<Favorite> favorites = favoriteService.listAll();
        if (favorites.size() > 0) {
            new MenuItem(menuFavorite, SWT.SEPARATOR);
            for (Favorite favorite : favorites) {
                final MenuItem menuItem = new MenuItem(menuFavorite, SWT.NONE);
                menuItem.setText(favorite.getName());
                menuItem.setData(FAVORITE, favorite);
                menuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Favorite favorite = (Favorite) menuItem.getData(FAVORITE);
                        int sid = favorite.getServerID();

                        String[] containers = favorite.getFavorite().split(":");

                        String container = "";
                        for (int i = 2; i < containers.length; i++) {
                            container += containers[i] + ":";
                        }
                        TreeItem selected = gotoDBContainer(sid, Integer.parseInt(containers[1].replaceFirst(DB_PREFIX, "")), container, favorite.isData(), false);
                        history.add(selected);
                        btnBackward.setEnabled(true);
                        btnForward.setEnabled(false);
                    }
                });
            }
        }
    }

    private void initServers() {
        java.util.List<Server> servers = serverService.listAll();

        servers.stream().forEach((server) -> {
            addServerTreeItem(server);
        });
        rootRedisServers.setExpanded(true);
        rootRedisServers.setData(ITEM_OPENED, true);
    }

    private TreeItem addServerTreeItem(Server server) {
        TreeItem treeItem = new TreeItem(rootRedisServers, 0);
        treeItem.setText(server.getName());
        treeItem.setData(NODE_ID, server.getId());
        treeItem.setData(NODE_TYPE, NodeType.SERVER);
        treeItem.setImage(redisImage);

        return treeItem;
    }

    private void addServer() {
        AddServerDialog dialog = new AddServerDialog(shell, iconImage);
        Server server = (Server) dialog.open();

        if (server != null) {
            server.setId(serverService.add(server.getName(), server.getHost(), server.getPort(), server.getPassword()));
            TreeItem item = addServerTreeItem(server);
            serverTreeItemSelected(item, false);
            history.add(item);
            btnBackward.setEnabled(true);
            btnForward.setEnabled(false);
        }

    }

    private void updateServer() {
        int id = (Integer) itemsSelected[0].getData(NODE_ID);

        Server server = serverService.listById(id);
        UpdateServerDialog dialog = new UpdateServerDialog(shell, iconImage, server);
        server = (Server) dialog.open();
        if (server != null) {
            serverService.update(id, server.getName(), server.getHost(), server.getPort(), server.getPassword());
            TreeItem treeItem;
            if (itemsSelected[0] instanceof TableItem) {
                treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
                itemsSelected[0].setText(server.getName());
            } else {
                treeItem = (TreeItem) itemsSelected[0];
            }

            treeItem.setText(server.getName());
            serverTreeItemSelected(treeItem, true);
        }
    }

    private void removeServer() {
        if (itemsSelected.length == 1) {
            boolean ok = MessageDialog.openConfirm(shell, i18nFile.getText(I18nFile.REMOVESERVER), i18nFile.getText(I18nFile.CONFIRMREMOVESERVER));
            if (ok) {
                removeOneServer(itemsSelected[0]);
            }
        } else if (itemsSelected.length > 1) {
            boolean ok = MessageDialog.openConfirm(shell, i18nFile.getText(I18nFile.REMOVESERVER), i18nFile.getText(I18nFile.CONFIRMREMOVESERVER));
            if (ok) {
                for (Item item : itemsSelected) {
                    removeOneServer(item);
                }
            }
        }
    }

    private void removeOneServer(Item item) {
        int id = ((Integer) (item.getData(NODE_ID)));
        serverService.delete(id);
        if (item instanceof TableItem) {
            getTreeItemByTableItem((TableItem) item).dispose();
        }
        item.dispose();
    }

    private TreeItem getTreeItemByTableItem(TableItem tableItem) {
        TreeItem[] treeItems = tree.getSelection();
        TreeItem treeItem = treeItems[0];
        NodeType tableItmeType = (NodeType) tableItem.getData(NODE_TYPE);

        if (tableItmeType != NodeType.DATABASE && tableItmeType != NodeType.SERVER && tableItmeType != NodeType.CONTAINER) {
            return treeItem;
        }
        TreeItem[] subTreeItems = treeItem.getItems();

        for (TreeItem item : subTreeItems) {
            NodeType type = (NodeType) item.getData(NODE_TYPE);

            if (type == NodeType.CONTAINER) {
                if (item.getText().equals(tableItem.getText())) {
                    return item;
                }
            } else {
                int treeid = (Integer) item.getData(NODE_ID);
                int tableid = (Integer) tableItem.getData(NODE_ID);
                if (treeid == tableid) {
                    return item;
                }
            }
        }
        return null;
    }

    private void renameContainer() {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, cinfo);

        RenameKeysDialog dialog = new RenameKeysDialog(shell, iconImage,
                cinfo.getServerName(), cinfo.getDb(), cinfo.getContainerStr());
        RenameInfo rinfo = (RenameInfo) dialog.open();

        if (rinfo != null) {
            Set<String> result = nodeService.renameContainer(cinfo.getId(), cinfo.getDb(), cinfo.getContainerStr(),  rinfo.getNewContainer(), rinfo.isOverwritten(), rinfo.isRenameSub());
            treeItem.getParentItem().setData(ITEM_OPENED, false);
            dbContainerTreeItemSelected(treeItem.getParentItem(), false);
            if (!rinfo.isOverwritten() && result.size() > 0) {
                String failString = i18nFile.getText(I18nFile.RENAMEFAIL);
                for (String container : result) {
                    failString += container + "\n";
                }
                MessageDialog.openError(shell, i18nFile.getText(I18nFile.RENAMERESULT), failString);
            }
        }
    }

    private void deleteCotainer(Item item, boolean deleteSub) {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (item instanceof TreeItem) {
            treeItem = (TreeItem) item;
        } else {
            treeItem = getTreeItemByTableItem((TableItem) item);
        }

        parseContainer(treeItem, cinfo);

        nodeService.deleteContainer(cinfo.getId(), cinfo.getDb(), cinfo.getContainerStr(), deleteSub);

        if (item instanceof TableItem) {
            treeItem.dispose();
        }
        item.dispose();
    }

    private void addFavorite() {
        TreeItem treeItem;
        String fullContainer;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
            fullContainer = text.getText();
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
            NodeType type = (NodeType) itemsSelected[0].getData(NODE_TYPE);
            if (type == NodeType.CONTAINER || type == NodeType.DATABASE) {
                fullContainer = text.getText() + itemsSelected[0].getText() + ":";
            } else {
                fullContainer = text.getText() + itemsSelected[0].getText();
            }
        }

        parseContainer(treeItem, cinfo);

        AddFavoriteDialog dialog = new AddFavoriteDialog(shell, iconImage, fullContainer);
        String name = (String) dialog.open();
        if (name != null) {
            favoriteService.add(cinfo.getId(), name, fullContainer);
        }

        removeFavoriteMenuItem();
        addFavoriteMenuItem();
    }

    private void treeItemSelected(boolean refresh) {
        TreeItem[] items = tree.getSelection();
        NodeType type = (NodeType) items[0].getData(NODE_TYPE);

        if (itemsSelected[0] == items[0] && !refresh) {
            return;
        }

        switch (type) {
            case ROOT:
                rootTreeItemSelected(refresh);
                break;
            case SERVER:
                serverTreeItemSelected(items[0], refresh);
                break;
            case DATABASE:
            case CONTAINER:
                dbContainerTreeItemSelected(items[0], refresh);
                break;
            default:
                break;
        }
    }

    private void dbContainerTreeItemSelected(TreeItem itemSelected, boolean refresh) {
        itemsSelected = new Item[]{itemSelected};
        tree.setSelection(itemSelected);
        ContainerKeyInfo info = new ContainerKeyInfo();
        parseContainer(itemSelected, info);
        text.setText(getLocation(info));

        dbContainerItemSelected(itemSelected);

        NodeType type = (NodeType) itemSelected.getData(NODE_TYPE);
        if (!(type == NodeType.CONTAINER && flatView)) {
            Set<Node> cnodes = nodeService.listContainers(info.getId(), info.getDb(), info.getContainerStr(), flatView);

            if (itemSelected.getData(ITEM_OPENED) == null || ((Boolean) (itemSelected.getData(ITEM_OPENED)) == false)) {
                itemSelected.removeAll();

                for (Node node : cnodes) {
                    TreeItem item = new TreeItem(itemSelected, SWT.NONE);
                    item.setText(node.getKey());
                    item.setData(NODE_TYPE, node.getType());
                    item.setImage(containerImage);
                }
                itemSelected.setExpanded(true);
                itemSelected.setData(ITEM_OPENED, true);
            } else if (refresh) {
                for (Node node : cnodes) {
                    if (!findItemByNode(itemSelected, node)) {
                        TreeItem item = new TreeItem(itemSelected, SWT.NONE);
                        item.setText(node.getKey());
                        item.setData(NODE_TYPE, node.getType());
                        item.setImage(containerImage);
                    }
                }

                TreeItem[] items = itemSelected.getItems();
                for (TreeItem item : items) {
                    if (!findNodeByItem(cnodes, item)) {
                        item.dispose();
                    }
                }
            }
        }

        tableItemOrderSelected(info);
        if (!(type == NodeType.CONTAINER)) {
            mainTabItem.setImage(dbImage);
        } else {
            mainTabItem.setImage(containerImage);
        }
    }

    private String getLocation(ContainerKeyInfo info) {
        return info.getServerName() + ":" + DB_PREFIX + info.getDb() + ":" + info.getContainerStr();
    }

    private boolean findItemByNode(TreeItem itemSelected, Node node) {
        TreeItem[] items = itemSelected.getItems();

        for (TreeItem item : items) {
            if (item.getText().equals(node.getKey())) {
                return true;
            }
        }
        return false;

    }

    private boolean findNodeByItem(Set<Node> nodes, TreeItem item) {
        return nodes.stream().anyMatch((node) -> (node.getKey().equals(item.getText())));
    }

    /**
     * 选中db container item 处理菜单enable属性
     * @param itemSelected 
     */
    private void dbContainerItemSelected(Item itemSelected) {
        menuServer.getItem(1).setEnabled(false);
        menuServer.getItem(2).setEnabled(false);
        menuServer.getItem(3).setEnabled(false);

        NodeType type = (NodeType) itemSelected.getData(NODE_TYPE);

        menuData.getItem(0).setEnabled(true);
        if (type == NodeType.CONTAINER) {
            menuData.getItem(1).setEnabled(true);
            menuData.getItem(2).setEnabled(true);
            menuData.getItem(3).setEnabled(true);
            menuData.getItem(5).setEnabled(true);
            menuData.getItem(6).setEnabled(true);
            if (pBuffer.canPaste()) {
                menuData.getItem(7).setEnabled(true);
            } else {
                menuData.getItem(7).setEnabled(false);
            }

            menuData.getItem(9).setEnabled(false);
            menuData.getItem(10).setEnabled(true);
        } else {
            menuData.getItem(1).setEnabled(false);
            menuData.getItem(2).setEnabled(false);
            menuData.getItem(3).setEnabled(true);
            menuData.getItem(5).setEnabled(false);
            menuData.getItem(6).setEnabled(true);
            if (pBuffer.canPaste()) {
                menuData.getItem(7).setEnabled(true);
            } else {
                menuData.getItem(7).setEnabled(false);
            }

            menuData.getItem(9).setEnabled(true);
            menuData.getItem(10).setEnabled(true);
        }

        menuView.getItem(1).setEnabled(true);
        menuView.getItem(2).setEnabled(true);

        menuFavorite.getItem(0).setEnabled(true);

        menuTools.getItem(0).setEnabled(false);
        menuTools.getItem(1).setEnabled(false);
        menuTools.getItem(2).setEnabled(false);
    }

    /**
     * 选中table item order 处理菜单enable属性
     * @param info 
     */
    private void tableItemOrderSelected(ContainerKeyInfo info) {
        table.removeAll();

        if (!flatView) {
            Set<Node> cnodes = nodeService.listContainers(info.getId(), info.getDb(), info.getContainerStr(), flatView, clientOrder);

            for (Node node : cnodes) {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(new String[]{node.getKey(), node.getType().toString()});
                item.setImage(containerImage);
                item.setData(NODE_TYPE, node.getType());
            }
        }

        Set<DataNode> knodes = nodeService.listContainerKeys(info.getId(), info.getDb(), info.getContainerStr(), false, clientOrder, clientOrderBy);

        for (DataNode node1 : knodes) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[]{node1.getKey(), node1.getType().toString(), String.valueOf(node1.getSize())});
            switch (node1.getType()) {
                case STRING:
                    if (node1.isPersist()) {
                        item.setImage(strImage);
                    } else {
                        item.setImage(strGrayImage);
                    }
                    break;
                case SET:
                    if (node1.isPersist()) {
                        item.setImage(setImage);
                    } else {
                        item.setImage(setGrayImage);
                    }
                    break;
                case LIST:
                    if (node1.isPersist()) {
                        item.setImage(listImage);
                    } else {
                        item.setImage(listGrayImage);
                    }
                    break;
                case HASH:
                    if (node1.isPersist()) {
                        item.setImage(hashImage);
                    } else {
                        item.setImage(hashGrayImage);
                    }
                    break;
                case SORTEDSET:
                    if (node1.isPersist()) {
                        item.setImage(zsetImage);
                    } else {
                        item.setImage(zsetGrayImage);
                    }
                    break;
                default:
                    break;
            }
            item.setData(NODE_TYPE, node1.getType());
        }
    }

    /**
     * 选中root tree item 处理菜单enable属性
     * @param refresh 
     */
    private void rootTreeItemSelected(boolean refresh) {
        itemsSelected = new Item[]{rootRedisServers};
        tree.setSelection(rootRedisServers);
        text.setText("");
        table.removeAll();

        rootItemSelected();

        if (rootRedisServers.getData(ITEM_OPENED) == null || ((Boolean) (rootRedisServers.getData(ITEM_OPENED)) == false)) {
            rootRedisServers.removeAll();
            initServers();
        } else if (refresh) {

        }

        java.util.List<Server> servers = serverService.listAll();

        for (Server server : servers) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[]{server.getName(), NodeType.SERVER.toString()});
            item.setImage(redisImage);
            item.setData(NODE_ID, server.getId());
            item.setData(NODE_TYPE, NodeType.SERVER);
        }

        mainTabItem.setImage(redisImage);
    }

    /**
     * 选中root项目 处理菜单enable属性
     */
    private void rootItemSelected() {
        
        menuServer.getItem(1).setEnabled(false);
        menuServer.getItem(2).setEnabled(false);
        menuServer.getItem(3).setEnabled(false);

        menuData.getItem(0).setEnabled(false);
        menuData.getItem(1).setEnabled(false);
        menuData.getItem(2).setEnabled(false);
        menuData.getItem(3).setEnabled(false);
        menuData.getItem(5).setEnabled(false);
        menuData.getItem(6).setEnabled(false);
        menuData.getItem(7).setEnabled(false);
        menuData.getItem(9).setEnabled(false);
        menuData.getItem(10).setEnabled(false);

        menuView.getItem(1).setEnabled(false);
        menuView.getItem(2).setEnabled(false);

        menuFavorite.getItem(0).setEnabled(false);

        menuTools.getItem(0).setEnabled(false);
        menuTools.getItem(1).setEnabled(false);
        menuTools.getItem(2).setEnabled(false);
    }

    /**
     * 选中server tree item  处理菜单enable属性
     * @param selectedItem
     * @param refresh 
     */
    private void serverTreeItemSelected(TreeItem selectedItem, boolean refresh) {
        itemsSelected = new Item[]{selectedItem};
        tree.setSelection(selectedItem);
        text.setText(selectedItem.getText() + ":");
        table.removeAll();

        serverItemSelected();

        int amount = serverService.listDBs((Integer) selectedItem.getData(NODE_ID));

        if (selectedItem.getData(ITEM_OPENED) == null
                || ((Boolean) (selectedItem.getData(ITEM_OPENED)) == false)) {
            selectedItem.removeAll();

            for (int i = 0; i < amount; i++) {
                TreeItem dbItem = new TreeItem(selectedItem, SWT.NONE);
                dbItem.setText(DB_PREFIX + i);
                dbItem.setData(NODE_ID, i);
                dbItem.setData(NODE_TYPE, NodeType.DATABASE);
                dbItem.setImage(dbImage);
            }
            selectedItem.setExpanded(true);
            selectedItem.setData(ITEM_OPENED, true);
        } else if (refresh) {

        }

        for (int i = 0; i < amount; i++) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[]{DB_PREFIX + i,
                NodeType.DATABASE.toString()});
            item.setData(NODE_ID, i);
            item.setImage(dbImage);
            item.setData(NODE_ID, i);
            item.setData(NODE_TYPE, NodeType.DATABASE);
        }
        mainTabItem.setImage(redisImage);
    }

    /**
     * 选中server item 处理菜单enable属性
     */
    private void serverItemSelected() {
        menuServer.getItem(1).setEnabled(true);
        menuServer.getItem(2).setEnabled(true);
        menuServer.getItem(3).setEnabled(true);

        menuData.getItem(0).setEnabled(false);
        menuData.getItem(1).setEnabled(false);
        menuData.getItem(2).setEnabled(false);
        menuData.getItem(3).setEnabled(false);
        menuData.getItem(5).setEnabled(false);
        menuData.getItem(6).setEnabled(false);
        menuData.getItem(7).setEnabled(false);
        menuData.getItem(9).setEnabled(false);
        menuData.getItem(10).setEnabled(false);

        menuView.getItem(1).setEnabled(false);
        menuView.getItem(2).setEnabled(false);

        menuFavorite.getItem(0).setEnabled(false);

        menuTools.getItem(0).setEnabled(true);
        menuTools.getItem(1).setEnabled(true);
        menuTools.getItem(2).setEnabled(true);
    }

    private void newString() {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, cinfo);

        NewStringDialog dialog = new NewStringDialog(shell, iconImage, cinfo.getId(), cinfo.getServerName(), cinfo.getDb(), cinfo.getContainerStr());
        StringInfo info = (StringInfo) dialog.open();
        if (info != null) {
            nodeService.addString(cinfo.getId(), cinfo.getDb(), info.getKey(), info.getValue(), info.getTtl());

            treeItem.setData(ITEM_OPENED, false);
            dbContainerTreeItemSelected(treeItem, false);
            gotoDBContainer(cinfo.getId(), cinfo.getDb(), info.getKey(), true, false);
        }
    }

    private void newList() {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, cinfo);

        NewListDialog dialog = new NewListDialog(shell, iconImage, cinfo.getId(), cinfo.getServerName(), cinfo.getDb(), cinfo.getContainerStr());
        ListInfo info = (ListInfo) dialog.open();
        if (info != null) {
            listService.add(cinfo.getId(), cinfo.getDb(), info.getKey(), info.getValues(), info.isHeadTail(), info.isExist(), info.getTtl());
            treeItem.setData(ITEM_OPENED, false);
            dbContainerTreeItemSelected(treeItem, false);
            gotoDBContainer(cinfo.getId(), cinfo.getDb(), info.getKey(), true, false);
        }
    }

    private void newSet() {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, cinfo);

        NewSetDialog dialog = new NewSetDialog(shell, iconImage, cinfo.getId(), cinfo.getServerName(), cinfo.getDb(), cinfo.getContainerStr());
        SetInfo info = (SetInfo) dialog.open();
        if (info != null) {
            setService.add(cinfo.getId(), cinfo.getDb(), info.getKey(), info.getValues(), info.getTtl());
            treeItem.setData(ITEM_OPENED, false);
            dbContainerTreeItemSelected(treeItem, false);
            gotoDBContainer(cinfo.getId(), cinfo.getDb(), info.getKey(), true, false);
        }
    }

    private void newZSet() {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, cinfo);

        NewZSetDialog dialog = new NewZSetDialog(shell, iconImage, cinfo.getId(), cinfo.getServerName(), cinfo.getDb(), cinfo.getContainerStr());
        ZSetInfo info = (ZSetInfo) dialog.open();
        if (info != null) {
            zsetService.add(cinfo.getId(), cinfo.getDb(), info.getKey(), info.getValues(), info.getTtl());
            treeItem.setData(ITEM_OPENED, false);
            dbContainerTreeItemSelected(treeItem, false);
            gotoDBContainer(cinfo.getId(), cinfo.getDb(), info.getKey(), true, false);
        }
    }

    private void newHash() {
        TreeItem treeItem;

        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, cinfo);

        NewHashDialog dialog = new NewHashDialog(shell, iconImage, cinfo.getId(), cinfo.getServerName(), cinfo.getDb(), cinfo.getContainerStr());
        HashInfo info = (HashInfo) dialog.open();
        if (info != null) {
            hashService.add(cinfo.getId(), cinfo.getDb(), info.getKey(), info.getValues(), info.getTtl());
            treeItem.setData(ITEM_OPENED, false);
            dbContainerTreeItemSelected(treeItem, false);
            gotoDBContainer(cinfo.getId(), cinfo.getDb(), info.getKey(), true, false);
        }
    }

    private void columnSelected() {
        TreeItem[] items = tree.getSelection();
        if (items.length > 0) {

            if (clientOrder == Order.Ascend) {
                table.setSortDirection(SWT.UP);
            } else {
                table.setSortDirection(SWT.DOWN);
            }
            if (null != clientOrderBy) {
                switch (clientOrderBy) {
                    case NAME:
                        table.setSortColumn(tableColumnName);
                        break;
                    case TYPE:
                        table.setSortColumn(tablecolumnType);
                        break;
                    default:
                        table.setSortColumn(tablecolumnSize);
                        break;
                }
            }

            ContainerKeyInfo info = new ContainerKeyInfo();
            parseContainer(items[0], info);

            updateOrder();
            updateOrderby();

            tableItemOrderSelected(info);

        }
    }

    private void renameKey() {
        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        TreeItem[] items = tree.getSelection();

        parseContainer(items[0], cinfo);

        String key = cinfo.getContainerStr();
        key += itemsSelected[0].getText();

        RenameKeysDialog dialog = new RenameKeysDialog(shell, iconImage, cinfo.getServerName(), cinfo.getDb(), key);
        RenameInfo rinfo = (RenameInfo) dialog.open();

        if (rinfo != null) {
            boolean result = nodeService.renameKey(cinfo.getId(), cinfo.getDb(),
                    key, rinfo.getNewContainer(), rinfo.isOverwritten());
            dbContainerTreeItemSelected(items[0], false);

            if (!rinfo.isOverwritten() && !result) {
                String failString = i18nFile.getText(I18nFile.RENAMEKEYFAIL);
                MessageDialog.openError(shell, i18nFile.getText(I18nFile.RENAMEKEYRESULT), failString);
            }
        }
    }

    private void deleteKey(Item item) {
        ContainerKeyInfo cinfo = new ContainerKeyInfo();
        TreeItem[] items = tree.getSelection();

        parseContainer(items[0], cinfo);

        String key = cinfo.getContainerStr();
        key += item.getText();

        nodeService.deleteKey(cinfo.getId(), cinfo.getDb(), key);
        item.dispose();
    }

    private void cut() {
        pBuffer = new PasteBuffer();

        for (Item item : itemsSelected) {
            TreeItem treeItem;

            ContainerKeyInfo cinfo = new ContainerKeyInfo();
            if (item instanceof TreeItem) {
                treeItem = (TreeItem) item;
            } else {
                treeItem = getTreeItemByTableItem((TableItem) item);
            }

            parseContainer(treeItem, cinfo);

            if (item instanceof TreeItem) {
                pBuffer.cut(cinfo, treeItem);
            } else {
                NodeType type = (NodeType) item.getData(NODE_TYPE);
                if (type == NodeType.CONTAINER || type == NodeType.DATABASE) {
                    pBuffer.cut(cinfo, treeItem);
                } else {
                    cinfo.setContainer(cinfo.getContainer(), item.getText());
                    pBuffer.cut(cinfo, treeItem);
                }
            }
        }
    }

    private void copy() {
        pBuffer = new PasteBuffer();
        for (Item item : itemsSelected) {
            TreeItem treeItem;

            ContainerKeyInfo cinfo = new ContainerKeyInfo();
            if (item instanceof TreeItem) {
                treeItem = (TreeItem) item;
            } else {
                treeItem = getTreeItemByTableItem((TableItem) item);
            }

            parseContainer(treeItem, cinfo);

            if (item instanceof TreeItem) {
                pBuffer.copy(cinfo);
            } else {
                NodeType type = (NodeType) item.getData(NODE_TYPE);
                if (type == NodeType.CONTAINER || type == NodeType.DATABASE) {
                    pBuffer.copy(cinfo);
                } else {
                    cinfo.setContainer(cinfo.getContainer(), item.getText());
                    pBuffer.copy(cinfo);
                }
            }
        }
    }

    private void paste() {
        TreeItem treeItem;

        ContainerKeyInfo target = new ContainerKeyInfo();
        if (itemsSelected[0] instanceof TreeItem) {
            treeItem = (TreeItem) itemsSelected[0];
        } else {
            treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]);
        }

        parseContainer(treeItem, target);

        do {
            ContainerKeyInfo source = pBuffer.paste();
            pasteOne(target, source);
        } while (pBuffer.hasNext());
    }

    private void pasteOne(ContainerKeyInfo target, ContainerKeyInfo source) {
        if (!pBuffer.isCopy() && !source.getContainer().isKey()) {
            pBuffer.getCutItem().dispose();
        }

        if (source.getContainer() != null && source.getContainer().isKey()) {
            String newKey = nodeService.pasteKey(source.getId(), source.getDb(), source.getContainerStr(), target.getId(), target.getDb(), target.getContainerStr() + source.getContainer().getKeyOnly(), pBuffer.isCopy(), true);
            if (newKey == null) {
                gotoDBContainer(target.getId(), target.getDb(), target.getContainerStr() + source.getContainer().getKeyOnly(), true, true);
            } else {
                gotoDBContainer(target.getId(), target.getDb(), newKey, true, true);
            }
        } else {
            nodeService.pasteContainer(source.getId(), source.getDb(), source.getContainerStr(), target.getId(), target.getDb(), target.getContainerStr(), pBuffer.isCopy(), true);
            gotoDBContainer(target.getId(), target.getDb(), target.getContainerStr(), false, true);
        }
    }

    private void updateMenuDBContainer(NodeType type, Menu menu) {
        if (type == NodeType.DATABASE) {
            menu.getItem(1).setEnabled(false);
            menu.getItem(2).setEnabled(false);
            menu.getItem(7).setEnabled(false);
            menu.getItem(8).setEnabled(true);
            if (pBuffer.canPaste()) {
                menu.getItem(9).setEnabled(true);
            } else {
                menu.getItem(9).setEnabled(false);
            }

            menu.getItem(11).setEnabled(true);
        } else {
            menu.getItem(1).setEnabled(true);
            menu.getItem(2).setEnabled(true);
            menu.getItem(7).setEnabled(true);
            menu.getItem(8).setEnabled(true);
            if (pBuffer.canPaste()) {
                menu.getItem(9).setEnabled(true);
            } else {
                menu.getItem(9).setEnabled(false);
            }

            menu.getItem(11).setEnabled(false);
        }
    }

    private TreeItem gotoDBContainer(int id, int db, String container, boolean isKey, boolean refresh) {
        rootTreeItemSelected(false);
        TreeItem dbItem = findDBTreeItem(id, db);
        TreeItem dataItemSelected = dbItem;

        dbContainerTreeItemSelected(dbItem, refresh);
        TreeItem[] dataItems = dbItem.getItems();

        if (!flatView) {
            String[] containers = container.split(":");
            if (!isKey) {
                for (String container1 : containers) {
                    for (TreeItem dataItem : dataItems) {
                        if (dataItem.getText().equals(container1)) {
                            tree.setSelection(dataItem);
                            tree.setFocus();
                            dbContainerTreeItemSelected(dataItem, refresh);
                            dataItems = dataItem.getItems();
                            dataItemSelected = dataItem;
                            break;
                        }
                    }
                }
            } else {
                for (int i = 0; i < containers.length - 1; i++) {
                    for (TreeItem dataItem : dataItems) {
                        if (dataItem.getText().equals(containers[i])) {
                            tree.setSelection(dataItem);
                            dbContainerTreeItemSelected(dataItem, false);
                            dataItems = dataItem.getItems();
                            dataItemSelected = dataItem;
                            break;
                        }
                    }
                }
                TableItem[] tableItems = table.getItems();
                for (TableItem tableItem : tableItems) {
                    NodeType type = (NodeType) tableItem.getData(NODE_TYPE);
                    if (type != NodeType.SERVER
                            && type != NodeType.DATABASE
                            && type != NodeType.CONTAINER
                            && tableItem.getText().equals(
                                    containers[containers.length - 1])) {
                        table.setSelection(tableItem);
                        table.setFocus();
                        tableItemSelected();
                        break;
                    }

                }

            }
        } else if (!isKey) {
            for (TreeItem dataItem : dataItems) {
                if (dataItem.getText().equals(container)) {
                    tree.setSelection(dataItem);
                    tree.setFocus();
                    dbContainerTreeItemSelected(dataItem, refresh);
                    dataItems = dataItem.getItems();
                    dataItemSelected = dataItem;
                    break;
                }
            }
        } else {
            for (TreeItem dataItem : dataItems) {
                String containerOnly = new ContainerKey(container)
                        .getContainerOnly();
                if (dataItem.getText().equals(containerOnly)) {
                    tree.setSelection(dataItem);
                    dbContainerTreeItemSelected(dataItem, false);
                    dataItems = dataItem.getItems();
                    dataItemSelected = dataItem;
                    break;
                }
            }

            TableItem[] tableItems = table.getItems();
            for (TableItem tableItem : tableItems) {
                NodeType type = (NodeType) tableItem.getData(NODE_TYPE);
                String keyOnly = new ContainerKey(container).getKeyOnly();
                if (type != NodeType.SERVER && type != NodeType.DATABASE
                        && type != NodeType.CONTAINER
                        && tableItem.getText().equals(keyOnly)) {
                    table.setSelection(tableItem);
                    table.setFocus();
                    tableItemSelected();
                    break;
                }

            }

        }

        return dataItemSelected;
    }

    private TreeItem findServerTreeItem(int id) {
        TreeItem[] treeItems = rootRedisServers.getItems();
        for (TreeItem treeItem : treeItems) {
            int serverId = (Integer) treeItem.getData(NODE_ID);
            if (serverId == id) {
                return treeItem;
            }
        }
        throw new RuntimeException(i18nFile.getText(I18nFile.FINDSERVER));
    }

    private TreeItem findDBTreeItem(int id, int db) {
        TreeItem server = findServerTreeItem(id);

        serverTreeItemSelected(server, false);
        TreeItem[] dbItems = server.getItems();
        for (TreeItem dbItem : dbItems) {
            if (dbItem.getText().equals(DB_PREFIX + db)) {
                return dbItem;
            }
        }
        throw new RuntimeException(i18nFile.getText(I18nFile.FINDDB));
    }

    private void selectTreeItem() {
        TreeItem[] items = tree.getSelection();

        if ((itemsSelected[0] instanceof TreeItem && items[0] != itemsSelected[0])) {
            history.add(items[0]);
            btnBackward.setEnabled(true);
            btnForward.setEnabled(false);
        } else if (itemsSelected[0] instanceof TableItem && items[0] != treeItemSelected) {
            history.add(items[0]);
            btnBackward.setEnabled(true);
            btnForward.setEnabled(false);
        }
        treeItemSelected(false);
    }

    private void serverProperties() {
        int id = (Integer) itemsSelected[0].getData(NODE_ID);
        Server info = serverService.listById(id);

        Map<String, String[]> values = serverService.listInfo(id);

        PropertiesDialog dialog = new PropertiesDialog(shell, iconImage, info, values);
        dialog.open();
    }

    private void deleteOneContainer() {
        DeleteContainerDialog dialog = new DeleteContainerDialog(shell, iconImage, questionImage, 1);
        Boolean deleteSub = (Boolean) dialog.open();
        if (deleteSub != null) {
            TreeItem treeItem;
            if (itemsSelected[0] instanceof TableItem) {
                treeItem = getTreeItemByTableItem((TableItem) itemsSelected[0]).getParentItem();
            } else {
                treeItem = ((TreeItem) itemsSelected[0]).getParentItem();
            }

            deleteCotainer(itemsSelected[0], deleteSub);

            treeItem.setData(ITEM_OPENED, false);
            dbContainerTreeItemSelected(treeItem, true);
        }
    }

    private void deleteOneKey() {
        boolean ok = MessageDialog.openConfirm(shell, i18nFile.getText(I18nFile.DELETEKEY), i18nFile.getText(I18nFile.CONFIRMDELETEKEY));
        if (ok) {
            deleteKey(itemsSelected[0]);
        }
    }

    private void deleteKeys() {
        TableItem containerItem = findContainerTableItem();

        DeleteContainerDialog dialog = new DeleteContainerDialog(shell, iconImage, questionImage, containerItem == null ? 0 : -1);
        Boolean deleteSub = (Boolean) dialog.open();
        if (deleteSub != null) {
            TreeItem treeItem = null;
            if (containerItem != null) {
                treeItem = getTreeItemByTableItem(containerItem).getParentItem();
            }

            for (Item item : itemsSelected) {
                NodeType type = (NodeType) item.getData(NODE_TYPE);
                if (type == NodeType.CONTAINER) {
                    deleteCotainer(item, deleteSub);
                } else {
                    deleteKey(item);
                }
            }
            if (containerItem != null) {
                treeItem.setData(ITEM_OPENED, false);
                dbContainerTreeItemSelected(treeItem, true);
            }

        }
    }

    private TableItem findContainerTableItem() {
        for (Item item : itemsSelected) {
            NodeType type = (NodeType) item.getData(NODE_TYPE);
            if (type == NodeType.CONTAINER) {
                return (TableItem) item;
            }
        }
        return null;
    }

    private void refreshDB() {
        TreeItem[] serverItems = rootRedisServers.getItems();
        for (TreeItem item : serverItems) {
            if (item.getData(ITEM_OPENED) != null && ((Boolean) (item.getData(ITEM_OPENED)) == true)) {
                TreeItem[] dbItems = item.getItems();
                for (TreeItem dbItem : dbItems) {
                    if (dbItem.getData(ITEM_OPENED) != null && ((Boolean) (dbItem.getData(ITEM_OPENED)) == true)) {
                        dbItem.setData(ITEM_OPENED, false);
                    }
                    dbItem.removeAll();
                    dbItem.setExpanded(false);
                }

            }
        }
        rootTreeItemSelected(true);
        history.clear();
        history.add(rootRedisServers);
        btnBackward.setEnabled(false);
        btnForward.setEnabled(false);
    }

    private void refreshLangUI() {
        menu.dispose();
        initMenu();
        rootRedisServers.setText(i18nFile.getText(I18nFile.REDISSERVERS));
        menuDataExplorer.dispose();
        initMenuData();
        menuTreeDBContainer.dispose();
        menuTreeDBContainer = initMenuTreeDB();
        menuTableDBContainer.dispose();
        menuTableDBContainer = initMenuTableDB();
        menu_Multi.dispose();
        initMenuMulti();
        menu_null.dispose();
        initMenuNull();
        menuTreeServer.dispose();
        menuTreeServer = initMenuTreeServer();
        menuTableServer.dispose();
        menuTableServer = initMenuTableServer();

        tableColumnName.setText(i18nFile.getText(I18nFile.NAME));
        tablecolumnType.setText(i18nFile.getText(I18nFile.TYPE));
        tablecolumnSize.setText(i18nFile.getText(I18nFile.SIZE));

        mainTabItem.setText(i18nFile.getText(I18nFile.REDISDATAEXPLORER));

        treeItemSelected(true);

        openDataContent.getList().stream().forEach((dataContent) -> {
            dataContent.refreshLangUI();
        });
        openConsole.getList().stream().forEach((console) -> {
            console.refreshLangUI();
        });
        openPublish.getList().stream().forEach((publish) -> {
            publish.refreshLangUI();
        });
        openSubscribe.getList().stream().forEach((subscribe) -> {
            subscribe.refreshLangUI();
        });
        ConfigFile.setLanguage(language);
    }

    private void updateView(ContainerKeyInfo cinfo) {
        refreshDB();
        if (cinfo.getId() != -1 && cinfo.getDb() != -1) {
            TreeItem item = gotoDBContainer(cinfo.getId(), cinfo.getDb(), cinfo.getContainerStr(), false, false);
            history.add(item);
            btnBackward.setEnabled(true);
        }
        ConfigFile.setFlatView(flatView);
    }

    private void refreshOrder() {
        clientOrder = Order.Ascend;
        clientOrderBy = OrderBy.NAME;
        updateOrder();
        updateOrderby();

        table.setSortColumn(null);
    }

    private void orderColumnSelected(OrderBy orderBy) {
        TreeItem[] items = tree.getSelection();
        if (items.length > 0) {
            NodeType type = (NodeType) items[0].getData(NODE_TYPE);
            if (type == NodeType.CONTAINER || type == NodeType.DATABASE) {
                if (clientOrder == Order.Ascend) {
                    clientOrder = Order.Descend;
                } else {
                    clientOrder = Order.Ascend;
                }

                clientOrderBy = orderBy;
                columnSelected();
            }

        }
    }

    private void orderMenuSelected(OrderBy orderBy) {
        TreeItem[] items = tree.getSelection();
        if (items.length > 0) {
            NodeType type = (NodeType) items[0].getData(NODE_TYPE);
            if (type == NodeType.CONTAINER || type == NodeType.DATABASE) {
                clientOrderBy = orderBy;
                columnSelected();
            }

        }
    }

    private void orderMenuSelected(Order order) {
        TreeItem[] items = tree.getSelection();
        if (items.length > 0) {
            NodeType type = (NodeType) items[0].getData(NODE_TYPE);
            if (type == NodeType.CONTAINER || type == NodeType.DATABASE) {
                clientOrder = order;
                columnSelected();
            }

        }
    }

    private void console() {
        int id = (Integer) itemsSelected[0].getData(NODE_ID);

        if (!openConsole.isOpen(id)) {
            final Console console = new Console(tabFolder_1, id);
            CTabItem tabItem = console.init();
            openConsole.add(console);

            tabItem.addDisposeListener((DisposeEvent e) -> {
                openConsole.remove(console);
            });
        } else {
            tabFolder_1.setSelection(openConsole.getTabItem(id));
        }

    }

    private void publish() {
        int id = (Integer) itemsSelected[0].getData(NODE_ID);

        if (!openPublish.isOpen(id)) {
            final Publish publish = new Publish(tabFolder_1, id);
            CTabItem tabItem = publish.init();
            openPublish.add(publish);
            tabItem.addDisposeListener((DisposeEvent e) -> {
                openPublish.remove(publish);
            });
        } else {
            tabFolder_1.setSelection(openPublish.getTabItem(id));
        }
    }

    private void subscribe() {
        int id = (Integer) itemsSelected[0].getData(NODE_ID);

        if (!openSubscribe.isOpen(id)) {
            final Subscribe subscribe = new Subscribe(tabFolder_1, id);
            CTabItem tabItem = subscribe.init();
            openSubscribe.add(subscribe);
            tabItem.addDisposeListener((DisposeEvent e) -> {
                openSubscribe.remove(subscribe);
            });
        } else {
            tabFolder_1.setSelection(openSubscribe.getTabItem(id));
        }

    }
}