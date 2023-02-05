package logbook.gui;

import logbook.constants.AppConstants;
import logbook.data.Data;
import logbook.data.DataType;
import logbook.gui.logic.CreateReportLogic;
import logbook.gui.logic.TableItemCreator;
import logbook.scripting.TableItemCreatorProxy;
import logbook.util.SwtUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

/**
 * 所有装備一覧
 *
 */
public final class ItemTable extends AbstractTableDialog {
    private Combo itemtext;
    private int SelectedEquipType = 0;
    String[] EQUIP_ITEMS = { "全て", 
    		"小口径主砲",
            "中口径主砲",
            "大口径主砲",
            "副砲",
            "魚雷",
            "艦上戦闘機",
            "艦上爆撃機",
            "艦上攻撃機",
            "艦上偵察機",
            "水上偵察機",
            "電波探信儀",
            "対空強化弾",
            "徹甲弾",
            "ダメコン",
            "機銃",
            "高角砲",
            "爆雷投射機",
            "ソナー",
            "機関部強化",
            "上陸用舟艇",
            "回転翼機",
            "対潜哨戒機",
            "追加装甲",
            "探照灯",
            "簡易輸送部材",
            "艦艇修理施設",
            "照明弾",
            "司令部施設",
            "航空要員",
            "高射装置",
            "対地装備",
            "水上艦要員",
            "大型飛行艇",
            "戦闘食料",
            "洋上補給",
            "特型内火艇",
            "陸上攻撃機",
            "局地戦闘機",
            "噴式戦闘爆撃機(噴式景雲改)",
            "噴式戦闘爆撃機(橘花改)",
            "輸送機材",
            "潜水艦装備",
            "水上戦闘機",
            "陸軍戦闘機",
            "夜間戦闘機",
            "夜間攻撃機",
            "陸上対潜哨戒機",
            "陸上攻撃機(襲撃機)",
            "大型陸上機",
            "夜間偵察機" };

    /**
     * @param parent
     */
    public ItemTable(Shell parent, MenuItem menuItem) {
        super(parent, menuItem);
    }

    @Override
    protected void createContentsBefore() {
        GridLayout shellLayout = new GridLayout(1, false);
        shellLayout.verticalSpacing = 1;
        shellLayout.marginWidth = 1;
        shellLayout.marginHeight = 1;
        shellLayout.marginBottom = 1;
        shellLayout.horizontalSpacing = 1;
        this.shell.setLayout(shellLayout);
        this.itemtext = new Combo(this.shell, SWT.DROP_DOWN);
        this.itemtext.setItems(this.EQUIP_ITEMS);
        this.itemtext.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ItemTable.this.SelectedEquipType = ItemTable.this.itemtext.getSelectionIndex();
                ItemTable.this.updateTableBody();
                ItemTable.this.reloadTable();
            }
        });
    }

    @Override
    protected void createContents() {
        this.table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        MenuItem itemCopy = new MenuItem(this.tablemenu, SWT.NONE);
        itemCopy.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StringBuilder sb = new StringBuilder();
                for (TableItem item : ItemTable.this.table.getSelection()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(item.getText(1));
                }
                Clipboard clipboard = new Clipboard(Display.getDefault());
                clipboard.setContents(new Object[] { sb.toString() }, new Transfer[] { TextTransfer.getInstance() });
            }
        });
        itemCopy.setText("装備名をコピー(&1)");
    }

    @Override
    protected String getTitleMain() {
        return "所有装備一覧";
    }

    @Override
    protected Point getSize() {
        return SwtUtils.DPIAwareSize(new Point(600, 350));
    }

    @Override
    protected String[] getTableHeader() {
        return CreateReportLogic.getItemListHeader();
    }

    @Override
    protected void updateTableBody() {
        this.body = CreateReportLogic.getItemListBody(this.SelectedEquipType);
    }

    @Override
    protected TableItemCreator getTableItemCreator() {
        return TableItemCreatorProxy.get(AppConstants.ITEMTABLE_PREFIX);
    }

    /**
     * 更新する必要のあるデータ
     */
    @SuppressWarnings("incomplete-switch")
    @Override
    public void update(DataType type, Data data) {
        switch (type) {
        case CHANGE:
        case PORT:
        case SHIP2:
        case SHIP3:
        case SLOTITEM_MEMBER:
        case GET_SHIP:
        case DESTROY_SHIP:
        case DESTROY_ITEM2:
        case POWERUP:
        case LOCK_SLOTITEM:
        case REMODEL_SLOT:
            this.needsUpdate = true;
        }
    }
}
