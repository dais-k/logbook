package logbook.gui.widgets;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.SWTResourceManager;

import logbook.config.AppConfig;
import logbook.constants.AppConstants;
import logbook.data.context.GlobalContext;
import logbook.data.context.TimerContext;
import logbook.dto.DeckMissionDto;
import logbook.dto.DockDto;
import logbook.dto.ItemDto;
import logbook.dto.ItemInfoDto;
import logbook.dto.ShipDto;
import logbook.gui.logic.AviationDetectionString;
import logbook.gui.logic.CalcAA;
import logbook.gui.logic.ColorManager;
import logbook.gui.logic.DaihatsuString;
import logbook.gui.logic.DamageRate;
import logbook.gui.logic.LayoutLogic;
import logbook.gui.logic.SakutekiString;
import logbook.gui.logic.SeikuString;
import logbook.gui.logic.TPString;
import logbook.gui.logic.TimeLogic;
import logbook.gui.logic.TimeString;
import logbook.internal.AkashiTimer;
import logbook.internal.CondTiming;
import logbook.internal.EvaluateExp;
import logbook.internal.LoggerHolder;
import logbook.internal.SeaExp;
import logbook.util.CalcExpUtils;
import logbook.util.SwtUtils;

/**
 * 艦隊タブのウィジェットです
 *
 */
public class FleetComposite extends Composite {
    /** ロガー */
    private static final LoggerHolder LOG = new LoggerHolder(FleetComposite.class);

    /** 警告 */
    public static final int WARN = 1;
    /** 致命的 */
    public static final int FATAL = 2;
    /** 1艦隊に編成できる艦娘の数 */
    private static final int MAXCHARA = AppConstants.MAXCHARA;
    /** フォント大きい */
    private final static int LARGE = 2;
    /** フォント小さい */
    private final static int SMALL = -1;

    /** HPゲージ幅 */
    private static final int GAUGE_WIDTH = 50;
    /** HPゲージ高さ */
    private static final int GAUGE_HEIGHT = 12;
    /** 経験値ゲージ高さ */
    private static final int EXP_GAUGE_HEIGHT = 4;
    /** HPゲージ最小色 */
    private static final RGB GAUGE_EMPTY = new RGB(0xff, 0, 0);
    /** HPゲージ中間色 */
    private static final RGB GAUGE_HALF = new RGB(0xff, 0xd7, 0);
    /** HPゲージ最大色 */
    private static final RGB GAUGE_FULL = new RGB(0, 0xd7, 0);
    /** 経験値ゲージ色 */
    private static final RGB EXP_GAUGE = new RGB(0, 0x80, 0xff);

    /** 艦隊 */
    private DockDto dock;

    private final Composite fleetGroup;

    /** タブアイコン表示 */
    private final BitSet state = new BitSet();
    /** コンディション最小値(メッセージ表示用) */
    private int cond;
    /** 疲労回復時間(メッセージ表示用) */
    private String clearDate;
    /** 大破している */
    private boolean badlyDamage;

    /** アイコンラベル */
    private final Label[] iconLabels = new Label[MAXCHARA];
    /** 名前ラベル */
    private final Label[] nameLabels = new Label[MAXCHARA];
    /** Lvラベル */
    private final Label[] lvLabels = new Label[MAXCHARA];
    /** HP */
    private final Label[] hpLabels = new Label[MAXCHARA];
    /** HPゲージ */
    private final Label[] hpgaugeLabels = new Label[MAXCHARA];
    /** HPゲージイメージ */
    private final Image[] hpgaugeImages = new Image[MAXCHARA];
    /** HPメッセージ */
    private final Label[] hpmsgLabels = new Label[MAXCHARA];
    /** コンディション */
    private final Label[] condLabels = new Label[MAXCHARA];
    /** コンディションステータス */
    private final Label[] condstLabels = new Label[MAXCHARA];
    /** 弾ステータス */
    private final Label[] bullstLabels = new Label[MAXCHARA];
    /** 燃料ステータス */
    private final Label[] fuelstLabels = new Label[MAXCHARA];
    /** ダメコン */
    private final Label[] dmgcLabels = new Label[MAXCHARA];
    /** 対空項目 */
    private final Label[] aaLabels = new Label[MAXCHARA];
    /** レベリング  */
    private final Label[] nextLabels = new Label[MAXCHARA];
    /** 泊地修理 or 疲労回復 */
    private final Label[] timeLabels = new Label[MAXCHARA];
    /** メッセージ */
    private final StyledText message;

    /** 時間表示系の更新タスク */
    private final List<Runnable> updators = new ArrayList<>();

    private static SimpleDateFormat format = new SimpleDateFormat(AppConstants.DATE_SHORT_FORMAT);

    private boolean showStrikingForceFleet;

    /**
     * @param parent 艦隊タブの親
     * @param tabItem 艦隊タブ
     */
    public FleetComposite(Composite parent) {
        super(parent, SWT.NONE);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        GridLayout glParent = new GridLayout(1, false);
        glParent.horizontalSpacing = 0;
        glParent.marginTop = 0;
        glParent.marginWidth = 0;
        glParent.marginHeight = 0;
        glParent.marginBottom = 0;
        glParent.verticalSpacing = 0;
        this.setLayout(glParent);

        this.fleetGroup = new Composite(this, SWT.NONE);
        this.fleetGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout glShipGroup = new GridLayout(2, false);
        glShipGroup.horizontalSpacing = 0;
        glShipGroup.marginTop = 0;
        glShipGroup.marginWidth = 1;
        glShipGroup.marginHeight = 0;
        glShipGroup.marginBottom = 4;
        glShipGroup.verticalSpacing = 0;
        this.fleetGroup.setLayout(glShipGroup);
        this.init();

        // セパレーター
        Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        // メッセージ
        this.message = new StyledText(this, SWT.READ_ONLY | SWT.WRAP);
        this.message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        this.message.setWordWrap(true);
        this.message.setBackground(this.getBackground());

        this.fleetGroup.layout();
    }

    /**
     * 初期化
     */
    private void init() {
        for (int i = 0; i < MAXCHARA; i++) {
            // アイコン
            Label iconlabel = new Label(this.fleetGroup, SWT.NONE);
            GridData gdIconlabel = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 2);
            gdIconlabel.widthHint = SwtUtils.DPIAwareWidth(16);
            iconlabel.setLayoutData(gdIconlabel);

            // 上段
            Composite upsideBase = new Composite(this.fleetGroup, SWT.NONE);
            upsideBase.setLayout(SwtUtils.makeGridLayout(2, 0, 0, 0, 0));
            upsideBase.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            // 名前
            Composite nameComposite = new Composite(upsideBase, SWT.NONE);
            nameComposite.setLayout(SwtUtils.makeGridLayout(2, 0, 0, 1, 0));
            nameComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
            Label namelabel = new Label(nameComposite, SWT.NONE);
            SwtUtils.initLabel(namelabel, "名前", LARGE, new GridData());
            Label lvlabel = new Label(nameComposite, SWT.NONE);
            SwtUtils.initLabel(lvlabel, "Lv.0", SMALL, 1.4, new GridData(SWT.CENTER, SWT.BOTTOM, false, false, 1, 1));

            // HP
            Composite hpComposite = new Composite(upsideBase, SWT.NONE);
            hpComposite.setLayout(SwtUtils.makeGridLayout(3, 0, 0, 1, 0));
            hpComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            Label hp = new Label(hpComposite, SWT.NONE);
            SwtUtils.initLabel(hp, "/", SMALL, new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
            Label hpgauge = new Label(hpComposite, SWT.NONE);
            hpgauge.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, false, false, 1, 1));
            Label hpmsg = new Label(hpComposite, SWT.NONE);
            SwtUtils.initLabel(hpmsg, "健在", new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));

            // 下段
            Composite downsideBase = new Composite(this.fleetGroup, SWT.NONE);
            downsideBase.setLayout(SwtUtils.makeGridLayout(2, 0, 0, 0, 0));
            downsideBase.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            // ステータス
            Composite stateComposite = new Composite(downsideBase, SWT.NONE);
            stateComposite.setLayout(SwtUtils.makeGridLayout(7, 0, 0, 0, 0));
            stateComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Label condst = new Label(stateComposite, SWT.NONE);
            SwtUtils.initLabel(condst, "疲", new GridData());
            Label fuelst = new Label(stateComposite, SWT.NONE);
            SwtUtils.initLabel(fuelst, "燃", new GridData());
            Label bullst = new Label(stateComposite, SWT.NONE);
            SwtUtils.initLabel(bullst, "弾", new GridData());
            Label dmgc = new Label(stateComposite, SWT.NONE);
            SwtUtils.initLabel(dmgc, "ダ", new GridData());
            Label aa = new Label(stateComposite, SWT.NONE);
            SwtUtils.initLabel(aa, "対空", new GridData());
            Label next = new Label(stateComposite, SWT.NONE);
            SwtUtils.initLabel(next, "next", new GridData());
            Label time = new Label(stateComposite, SWT.NONE);
            SwtUtils.initLabel(time, "time", new GridData());

            // 疲労
            Label cond = new Label(downsideBase, SWT.NONE);
            SwtUtils.initLabel(cond, "49 cond.", new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

            this.iconLabels[i] = iconlabel;
            this.nameLabels[i] = namelabel;
            this.lvLabels[i] = lvlabel;
            this.hpLabels[i] = hp;
            this.hpgaugeLabels[i] = hpgauge;
            this.hpmsgLabels[i] = hpmsg;
            this.condLabels[i] = cond;
            this.condstLabels[i] = condst;
            this.bullstLabels[i] = bullst;
            this.fuelstLabels[i] = fuelst;
            this.dmgcLabels[i] = dmgc;
            this.aaLabels[i] = aa;
            this.nextLabels[i] = next;
            this.timeLabels[i] = time;
        }
        this.hideStrikingForceFleet();
    }

    private void hideStrikingForceFleet() {
        this.showStrikingForceFleet = AppConfig.get().isShowStrikingForceFleet();
        this.setRedraw(false);
        Control[] child = this.fleetGroup.getChildren();
        for (int i = 1; i <= 3; i++) {
            LayoutLogic.hide(child[child.length - i], !this.showStrikingForceFleet);
        }
        this.layout();
        this.setRedraw(true);
    }

    /**
     * 艦隊を更新します
     *
     * @param dock
     * @param combinedFleetBadlyDamaed 連合艦隊の他の艦隊の艦が大破している
     */
    public void updateFleet(DockDto dock, boolean combinedFleetBadlyDamaed, List<ShipDto> badlyDamaged) {
        if ((this.dock == dock) && !this.dock.isUpdate()) {
            if (this.showStrikingForceFleet != AppConfig.get().isShowStrikingForceFleet()) {
                this.hideStrikingForceFleet();
            }
            // 時間表示だけ更新
            for (Runnable update : this.updators) {
                update.run();
            }
            return;
        }

        this.updators.clear();
        this.getShell().setRedraw(false);

        this.dock = dock;
        this.state.set(WARN, false);
        this.state.set(FATAL, false);
        this.cond = AppConfig.get().getOkCond();
        this.clearDate = null;
        this.badlyDamage = false;
        this.message.setText("");

        List<ShipDto> ships = dock.getShips();
        boolean[] escaped = dock.getEscaped();
        for (int i = ships.size(); i < MAXCHARA; i++) {
            this.iconLabels[i].setImage(null);
            this.nameLabels[i].setText("");
            this.lvLabels[i].setText("");
            this.hpLabels[i].setText("");
            this.hpgaugeLabels[i].setImage(null);
            this.hpmsgLabels[i].setText("");
            this.condLabels[i].setText("");
            this.condstLabels[i].setText("");
            this.bullstLabels[i].setText("");
            this.fuelstLabels[i].setText("");
            this.dmgcLabels[i].setText("");
            this.aaLabels[i].setText("");
            this.nextLabels[i].setText("");
            this.timeLabels[i].setText("");
        }
        // 艦隊合計Lv
        int totallv = 0;
        // 艦隊合計火力値(装備込)
        double totalFirepower = 0;
        // 艦隊合計索敵値(装備込)
        double totalLOS = 0;
        // 艦隊合計対潜値(装備込)
        double totalASW = 0;
        // 艦隊合計対空値(装備込)
        double totalAA = 0;

        int dockIndex = Integer.parseInt(dock.getId()) - 1;
        CondTiming condTiming = GlobalContext.getCondTiming();
        AkashiTimer.RepairState repairState = TimerContext.get().getAkashiRepairState(dockIndex);
        List<AkashiTimer.ShipState> repairShips = repairState.isRepairing() ? repairState.get() : null;
        boolean isSortie = GlobalContext.isSortie(dock.getId());
        Map<Integer, Date> ndockMap = GlobalContext.getNDockCompleteTimeMap();
        DeckMissionDto currentMission = (dockIndex == 0) ? null : GlobalContext.getDeckMissions()[dockIndex - 1];
        DeckMissionDto previousMission = (dockIndex == 0) ? null : GlobalContext.getPreviousMissions()[dockIndex - 1];
        boolean flagshipNeedSupply = false;
        boolean needSupply = false;
        boolean reqSupply = false;
        int lostPlanes = 0;
        boolean isBathwater = false;

        for (int i = 0; i < ships.size(); i++) {
            ShipDto ship = ships.get(i);
            // 艦娘のステータス
            BitSet shipstatus = new BitSet();
            // HP
            int nowhp = ship.getNowhp();
            // MaxHP
            int maxhp = ship.getMaxhp();
            // HP割合
            float hpratio = (float) nowhp / (float) maxhp;
            // 経験値ゲージの割合
            float expraito = ship.getExpraito();
            // 疲労
            int cond = ship.getCond();
            // 弾
            int bull = ship.getBull();
            // 弾Max
            int bullmax = ship.getBullMax();
            // 残弾比
            float bullraito = bullmax != 0 ? (float) bull / (float) bullmax : 1f;
            // 燃料
            int fuel = ship.getFuel();
            // 燃料Max
            int fuelmax = ship.getFuelMax();
            // 残燃料比
            float fuelraito = fuelmax != 0 ? (float) fuel / (float) fuelmax : 1f;
            // 艦隊合計Lv
            totallv += ship.getLv();
            List<ItemDto> _itemList = new ArrayList<>(ship.getItem2());
            _itemList.add(ship.getSlotExItem());
            List<ItemDto> itemList = _itemList.stream().filter(Objects::nonNull).collect(Collectors.toList());
            int[] onslot = ship.getOnSlot();
            // 艦隊合計火力値(装備込)
            totalFirepower += ship.getKaryoku() + itemList.stream()
                    .mapToDouble(item -> {
                        switch (item.getType2()) {
                        case 1: // 小口径主砲
                            return 0.5 * Math.sqrt(item.getLevel());
                        case 2: // 中口径主砲
                            return Math.sqrt(item.getLevel());
                        case 3: // 大口径主砲
                        case 38:
                            return Math.sqrt(item.getLevel());
                        case 4: // 副砲
                        case 95:
                            return 0.5 * Math.sqrt(item.getLevel());
                        case 12: // 小型電探
                            return 0.5 * Math.sqrt(item.getLevel());
                        case 13:
                        case 93: // 大型電探
                            return Math.sqrt(item.getLevel());
                        case 19: // 対艦強化弾
                            return 0.5 * Math.sqrt(item.getLevel());
                        case 21: // 機銃
                            return 0.5 * Math.sqrt(item.getLevel());
                        }
                        return 0;
                    }).sum();
            // 艦隊合計索敵値(装備込)
            totalLOS += ship.getSakuteki() + itemList.stream()
                    .mapToDouble(item -> {
                        switch (item.getType2()) {
                        case 10: // 水上偵察機
                            return Math.sqrt(item.getLevel());
                        case 12:
                        case 13:
                        case 93: // 電探
                            return Math.sqrt(item.getLevel());
                        }
                        return 0;
                    }).sum();
            // 艦隊合計対潜値(装備込)
            totalASW += ship.getTaisen()
                    + itemList.stream()
                            .mapToDouble(item -> {
                                switch (item.getType2()) {
                                case 10: // 水上偵察機
                                case 11: // 水上爆撃機
                                case 7: // 艦上爆撃機
                                case 8: // 艦上攻撃機
                                case 25: // オートジャイロ
                                    if (item.getParam().getTaisen() < 5) {
                                        return 0;
                                    }
                                    else if (item.getParam().getTaisen() < 7) { // 6かも
                                        return 0.5 * Math.sqrt(item.getLevel());
                                    }
                                    else {
                                        return Math.sqrt(item.getLevel());
                                    }
                                case 14: // ソナー
                                    return Math.sqrt(item.getLevel());
                                case 15: // 爆雷
                                    return Math.sqrt(item.getLevel());
                                }
                                return 0;
                            }).sum();
            // 艦隊合計対空値(装備込)
            totalAA += ship.getTaiku() + itemList.stream()
                    .mapToDouble(item -> {
                        switch (item.getType3()) {
                        case 15: // 機銃
                            return Math.sqrt(item.getLevel());
                        case 16: // 高角砲
                            return Math.sqrt(item.getLevel());
                        }
                        return 0;
                    }).sum();
            // 損失艦載機
            int[] maxeq = ship.getMaxeq();
            List<ItemInfoDto> items = ship.getItem();
            // 普通はnullにならないが、メンテで艦娘が追加されたあとリロードしていない場合に情報がなくてnullになることがある
            if (maxeq != null) {
                for (int c = 0; c < items.size(); ++c) {
                    if (items.get(c).isPlane()) {
                        lostPlanes += maxeq[c] - onslot[c];
                    }
                }
            }
            // 入渠中?
            isBathwater |= ndockMap.containsKey(ship.getId());

            // 疲労している艦娘がいる場合メッセージを表示
            final Date condClearDate = ship.getCondClearTime(condTiming, ndockMap.get(ship.getId()));
            if (this.cond > cond) {
                this.cond = cond;
                this.clearDate = new TimeString(condClearDate).toString();
            }

            // 体力メッセージ
            boolean isEscaped = ((escaped != null) && escaped[i]);
            if (isEscaped) {
                this.hpmsgLabels[i].setText("退避");
                this.hpmsgLabels[i].setBackground(ColorManager.getColor(AppConstants.ESCAPED_SHIP_COLOR));
                this.hpmsgLabels[i].setForeground(null);
            }
            else {
                DamageRate rate = DamageRate.fromHP(nowhp, maxhp);
                this.hpmsgLabels[i].setText(rate.toString());
                this.hpmsgLabels[i].setBackground(rate.getBackground());
                this.hpmsgLabels[i].setForeground(rate.getForeground());

                if (rate == DamageRate.TAIHA) {
                    if (AppConfig.get().isFatalBybadlyDamage()) {
                        // 大破で致命的アイコン
                        this.state.set(FATAL);
                        shipstatus.set(FATAL);
                    }
                    this.badlyDamage = true;
                    if (isSortie) {
                        badlyDamaged.add(ship);
                    }
                }
                else if (rate == DamageRate.TYUHA) {
                    if (AppConfig.get().isWarnByHalfDamage()) {
                        // 中破で警告アイコン
                        this.state.set(WARN);
                        shipstatus.set(WARN);
                    }
                }
            }

            // ステータス
            // ステータス.疲労
            this.condstLabels[i].setText("疲");
            if (cond >= 49) {
                this.condstLabels[i].setEnabled(false);
            }
            else {
                this.condstLabels[i].setEnabled(true);
            }
            // ステータス.燃料
            this.fuelstLabels[i].setText("燃");
            if (fuelraito >= 1f) {
                this.fuelstLabels[i].setEnabled(false);
                this.fuelstLabels[i].setForeground(null);
            }
            else {
                if (AppConfig.get().isWarnByNeedSupply()) {
                    // 補給不足で警告アイコン
                    this.state.set(WARN);
                    shipstatus.set(WARN);
                }
                this.fuelstLabels[i].setEnabled(true);
                if (fuelraito <= AppConstants.EMPTY_SUPPLY) {
                    // 補給赤
                    this.fuelstLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_RED_COLOR));
                }
                else if (fuelraito <= AppConstants.LOW_SUPPLY) {
                    // 補給橙
                    this.fuelstLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_ORANGE_COLOR));
                }
                needSupply = true;
                if (i == 0) {
                    flagshipNeedSupply = true;
                }
                if (fuelraito == 0f) {
                    reqSupply = true;
                }
            }
            // ステータス.弾
            this.bullstLabels[i].setText("弾");
            if (bullraito >= 1f) {
                this.bullstLabels[i].setEnabled(false);
                this.bullstLabels[i].setBackground(null);
                this.bullstLabels[i].setForeground(null);
            }
            else {
                if (AppConfig.get().isWarnByNeedSupply()) {
                    // 補給不足で警告アイコン
                    this.state.set(WARN);
                    shipstatus.set(WARN);
                }
                this.bullstLabels[i].setEnabled(true);
                if (bullraito <= AppConstants.EMPTY_SUPPLY) {
                    this.bullstLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_RED_COLOR));
                }
                else if (bullraito <= AppConstants.LOW_SUPPLY) {
                    this.bullstLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_ORANGE_COLOR));
                }
                needSupply = true;
                if (i == 0) {
                    flagshipNeedSupply = true;
                }
                if (bullraito == 0f) {
                    reqSupply = true;
                }
            }

            // ステータス.ダメコン, 補給物資, 対空機銃, バルジなど
            Map<String, Integer> exmap = new TreeMap<>();

            List<ItemDto> item = new ArrayList<ItemDto>(ship.getItem2());
            item.add(ship.getSlotExItem());
            for (ItemDto itemDto : item) {
                if (itemDto != null) {
                    if (itemDto.canEqipExslot()) {
                        String name = itemDto.getName();
                        Integer old = exmap.get(name);
                        exmap.put(name, (old != null) ? (old + 1) : 1);
                    }
                }
            }
            String dmgcstr = "";
            String[][] namemap = new String[][] {
                    new String[] { "応急修理要員", "要員" },
                    new String[] { "応急修理女神", "女神" },
                    new String[] { "洋上補給", "補給" },
                    new String[] { "戦闘糧食", "糧食" },
                    new String[] { "秋刀魚の缶詰", "缶詰" },
            };
            for (String[] map : namemap) {
                String key = map[0];
                if (exmap.containsKey(key)) {
                    dmgcstr += map[1] + "x" + exmap.get(key) + " ";
                    exmap.remove(key);
                }
            }
            if (AppConfig.get().isShowGunAndBulge()) {
                for (Map.Entry<String, Integer> entry : exmap.entrySet()) {
                    dmgcstr += entry.getKey() + "x" + entry.getValue() + " ";
                }
            }

            this.dmgcLabels[i].setText(dmgcstr);
            this.dmgcLabels[i].setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN));

            // 対空項目
            String aaString = "";
            if (AppConfig.get().isShowAA()) {
                CalcAA calcAA = new CalcAA();
                List<ShipDto> aaShips = new ArrayList<>();
                aaShips.addAll(ships);
                boolean isCombined = GlobalContext.isCombined();
                if (isCombined) {
                    switch (dockIndex) {
                    case 0:
                        aaShips.addAll(GlobalContext.getDock("2").getShips());
                        break;
                    case 1:
                        aaShips.addAll(GlobalContext.getDock("1").getShips());
                        break;
                    }
                }
                aaString += String.format("加:%.2f 割:%.2f%% 固:%d ",
                        calcAA.getFinalWeightedAirValue(ship, aaShips, true, 1),
                        calcAA.getPropShotDown(ship, true, isCombined, dockIndex == 1, false) * 100,
                        calcAA.getFixedShotDown(ship, aaShips, true, isCombined, dockIndex == 1, 1, 0, false));
            }
            this.aaLabels[i].setText(aaString);
            this.aaLabels[i].setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_RED));

            // ステータス.あと何回
            String statusstr = "";
            if (AppConfig.get().isDisplayCount()) {
                Integer nextcount = this.getNextCount(ship, i == 0);
                if (nextcount != null) {
                    statusstr = MessageFormat.format("あと{0}回", nextcount);
                }
            }
            this.nextLabels[i].setText(statusstr);

            // 残り修理時間/疲労回復までの時間/ダメコン表示
            Runnable updator = null;
            final Label timeLabel = this.timeLabels[i];
            boolean isRepairing = (repairShips != null) && (repairShips.get(i) != null);
            if (isRepairing && AppConfig.get().isShowAkashiTimer()) {
                // 泊地修理中
                updator = new AkashiTimerUpdator(timeLabel, dockIndex, i);
            }
            else if (!isSortie && (condClearDate != null) && AppConfig.get().isShowCondTimer()) {
                updator = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long rest = TimeLogic.getRest(new Date(), condClearDate);
                            String str;
                            String tip = null;
                            String reststr = TimeLogic.toDateRestString(rest);
                            if (reststr != null) {
                                str = "疲労あと" + reststr;
                                tip = format.format(condClearDate);
                            }
                            else {
                                str = "疲労まもなく回復";
                            }
                            timeLabel.setText(str);
                            timeLabel.setToolTipText(tip);
                            timeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN));
                            timeLabel.getParent().layout();
                        } catch (Exception e) {
                            LOG.get().warn("披露表示更新でエラー", e);
                        }
                    }
                };
            }
            if (updator != null) {
                updator.run();
                this.updators.add(updator);
            }
            else {
                timeLabel.setText("");
                timeLabel.setForeground(null);
                timeLabel.setToolTipText(null);
            }

            // コンディション
            if (cond < AppConstants.COND_RED) {
                // 疲労19以下
                if (AppConfig.get().isWarnByCondState()) {
                    // 疲労状態で警告アイコン
                    this.state.set(WARN);
                    shipstatus.set(WARN);
                }
                this.condLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_RED_COLOR));
                this.condstLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_RED_COLOR));
            }
            else if (cond < AppConstants.COND_ORANGE) {
                // 疲労29以下
                if (AppConfig.get().isWarnByCondState()) {
                    // 疲労状態で警告アイコン
                    this.state.set(WARN);
                    shipstatus.set(WARN);
                }
                this.condLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_ORANGE_COLOR));
                this.condstLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_ORANGE_COLOR));
            }
            else if ((cond >= AppConstants.COND_DARK_GREEN) && (cond < AppConstants.COND_GREEN)) {
                // 疲労50以上
                this.condLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_DARK_GREEN_COLOR));
                this.condstLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_DARK_GREEN_COLOR));
            }
            else if (cond >= AppConstants.COND_GREEN) {
                // 疲労53以上
                this.condLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_GREEN_COLOR));
                this.condstLabels[i].setForeground(ColorManager.getColor(AppConstants.COND_GREEN_COLOR));
            }
            else {
                this.condLabels[i].setForeground(null);
                this.condstLabels[i].setForeground(null);
            }

            // 艦娘の状態アイコンを更新
            if (shipstatus.get(FATAL)) {
                this.iconLabels[i].setImage(SWTResourceManager.getImage(FleetComposite.class,
                        AppConfig.get().isMonoIcon()
                                ? AppConstants.R_ICON_EXCLAMATION_MONO
                                : AppConstants.R_ICON_EXCLAMATION));
            }
            else if (shipstatus.get(WARN)) {
                this.iconLabels[i].setImage(SWTResourceManager.getImage(FleetComposite.class,
                        AppConfig.get().isMonoIcon()
                                ? AppConstants.R_ICON_ERROR_MONO
                                : AppConstants.R_ICON_ERROR));
            }
            else {
                this.iconLabels[i].setImage(null);
            }

            // ラベルを更新する
            // 名前
            this.nameLabels[i].setText(ship.getName());
            this.nameLabels[i].setToolTipText(MessageFormat.format(AppConstants.TOOLTIP_FLEETTAB_SHIP, nowhp, maxhp,
                    fuel, fuelmax, bull, bullmax, ship.getNext()));
            this.lvLabels[i].setText(MessageFormat.format("(Lv.{0})", ship.getLv()));
            // HP
            this.hpLabels[i].setText(MessageFormat.format("{0}/{1} ", nowhp, maxhp));
            // HPゲージ
            Image gauge = this.getHpGaugeImage(hpratio, expraito);
            this.hpgaugeLabels[i].setImage(gauge);
            if (this.hpgaugeImages[i] != null) {
                // 古いイメージを破棄
                this.hpgaugeImages[i].dispose();
            }
            this.hpgaugeImages[i] = gauge;
            // コンディション
            this.condLabels[i].setText(MessageFormat.format("{0} cond.", cond));

            //this.nameLabels[i].getParent().layout();
            //this.hpLabels[i].getParent().layout();
            //this.bullstLabels[i].getParent().layout();

        }

        // ドラム缶の合計
        int dram = 0;
        int dramKanmusu = 0;
        for (ShipDto shipDto : ships) {
            if (shipDto.getDram() > 0) {
                dramKanmusu++;
                dram += shipDto.getDram();
            }
        }

        //大発による遠征効率UP
        DaihatsuString daihatsu = new DaihatsuString(ships);

        // メッセージを更新する

        StyleRange messageStyle = new StyleRange();
        messageStyle.fontStyle = SWT.BOLD;
        messageStyle.foreground = SWTResourceManager.getColor(SWT.COLOR_DARK_BLUE);
        StyleRange taihaStyle = new StyleRange();
        taihaStyle.fontStyle = SWT.BOLD;
        taihaStyle.underline = true;
        taihaStyle.underlineStyle = SWT.UNDERLINE_SQUIGGLE;
        taihaStyle.underlineColor = SWTResourceManager.getColor(SWT.COLOR_RED);
        taihaStyle.foreground = SWTResourceManager.getColor(SWT.COLOR_RED);

        if ((currentMission != null) && (currentMission.getMission() != null)) {
            // 遠征中
            this.addStyledText(this.message, AppConstants.MESSAGE_MISSION, messageStyle);
        }
        else if (GlobalContext.isSortie(this.dock.getId())) {
            // 出撃中
            this.addStyledText(this.message, AppConstants.MESSAGE_SORTIE, messageStyle);
            if (this.badlyDamage) {
                // 大破
                this.addStyledText(this.message, AppConstants.MESSAGE_STOP_SORTIE, taihaStyle);
            }
            else if (combinedFleetBadlyDamaed) {
                // 連合艦隊の他の艦隊に大破艦がある
                this.addStyledText(this.message, AppConstants.MESSAGE_IN_COMBINED + AppConstants.MESSAGE_STOP_SORTIE,
                        taihaStyle);
            }
            else {
                // 進撃可能
                this.addStyledText(this.message, AppConstants.MESSAGE_GO_NEXT, messageStyle);
            }
        }
        else if (this.badlyDamage) {
            // 大破
            this.addStyledText(this.message,
                    MessageFormat.format(AppConstants.MESSAGE_BAD, AppConstants.MESSAGE_BADLY_DAMAGE), taihaStyle);
        }
        else if (combinedFleetBadlyDamaed) {
            // 連合艦隊の他の艦隊に大破艦がある
            this.addStyledText(this.message, AppConstants.MESSAGE_IN_COMBINED +
                    MessageFormat.format(AppConstants.MESSAGE_BAD, AppConstants.MESSAGE_BADLY_DAMAGE), taihaStyle);
        }
        else {
            if (isBathwater) {
                // 入渠中
                this.addStyledText(this.message,
                        MessageFormat.format(AppConstants.MESSAGE_BAD, AppConstants.MESSAGE_BATHWATER), messageStyle);
            }
            else if (flagshipNeedSupply) {
                // 未補給
                this.addStyledText(this.message, "未補給です。", messageStyle);
                if (reqSupply) { // 空
                    this.addStyledText(this.message, "出撃できません。", messageStyle);
                }
            }
            else {
                if (repairState.isRepairing()) {
                    // 泊地修理中
                    this.addStyledText(this.message, "泊地修理中。", messageStyle);
                }
                if (needSupply) {
                    // 一部未補給
                    this.addStyledText(this.message, "一部未補給。", messageStyle);
                }
                // 出撃可能
                this.addStyledText(this.message, AppConstants.MESSAGE_GOOD, messageStyle);
            }
        }
        if ((Integer.parseInt(this.dock.getId()) <= 2) && GlobalContext.isCombined()) {
            // 連合艦隊
            this.addStyledText(this.message, AppConstants.MESSAGE_COMBINED, messageStyle);
        }
        this.addStyledText(this.message, "\n", null);
        // 合計Lv
        this.addStyledText(this.message, MessageFormat.format(AppConstants.MESSAGE_TOTAL_LV, totallv), null);

        this.addStyledText(this.message, "\n", null);
        // 制空
        SeikuString seikuString = new SeikuString(ships);
        this.addStyledText(this.message, MessageFormat.format(AppConstants.MESSAGE_SEIKU, seikuString.toString()),
                null);
        if (lostPlanes > 0) {
            this.addStyledText(this.message,
                    MessageFormat.format("\n損失機:{0}(ボーキ:{1})", lostPlanes, lostPlanes * 5), null);
        }
        this.addStyledText(this.message, "\n", null);
        // 索敵
        SakutekiString sakutekiString = new SakutekiString(ships, GlobalContext.hqLevel());
        this.addStyledText(this.message,
                MessageFormat.format(AppConstants.MESSAGE_SAKUTEKI, sakutekiString.toString()), null);
        this.addStyledText(this.message, "\n", null);
        // 対空
        if (AppConfig.get().isShowAA()) {
            CalcAA calcAA = new CalcAA();
            List<ShipDto> aaShips = new ArrayList<>();
            aaShips.addAll(ships);
            boolean isCombined = GlobalContext.isCombined();
            if (isCombined) {
                switch (dockIndex) {
                case 0:
                    aaShips.addAll(GlobalContext.getDock("2").getShips());
                    break;
                case 1:
                    aaShips.addAll(GlobalContext.getDock("1").getShips());
                    break;
                }
            }
            this.addStyledText(this.message,
                    MessageFormat.format(AppConstants.MESSAGE_AA, calcAA.getFleetAirDefenseValue(aaShips, true, 1)),
                    null);
            this.addStyledText(this.message, "\n", null);
        }
        // 遠征
        this.addStyledText(this.message, MessageFormat.format(AppConstants.MESSAGE_EXPEDITION, totalFirepower,
                totalAA, totalASW, totalLOS), null);
        this.addStyledText(this.message, "\n", null);

        // 航空偵察スコア
        AviationDetectionString aviationDetectionString = new AviationDetectionString(ships);
        this.addStyledText(this.message,
                MessageFormat.format(AppConstants.MESSAGE_AVIATION_DETECTION, aviationDetectionString.toString()),
                null);

        this.addStyledText(this.message, "\n", null);

        if (dram > 0) {
            // ドラム缶合計数
            this.addStyledText(this.message, MessageFormat.format(AppConstants.MESSAGE_TOTAL_DRAM, dram, dramKanmusu),
                    null);
            if (daihatsu.getUp() > 0.0) {
                this.addStyledText(this.message, " / ", null);
            }
            else {
                this.addStyledText(this.message, "\n", null);
            }
        }
        if (daihatsu.getUp() > 0.0) {
            // 大発合計数
            this.addStyledText(this.message, daihatsu.toString(), null);
            this.addStyledText(this.message, "\n", null);
        }

        // TP獲得量
        this.addStyledText(this.message, new TPString(ships).toString(), null);

        this.addStyledText(this.message, "\n", null);

        // 前回の遠征
        if ((currentMission != null) && (currentMission.getMission() == null)
                && (previousMission.getMission() != null)) {
            String text = previousMission.getDisplayText("missioncheck_" + dock.getId() + "p");
            this.addStyledText(this.message,
                    MessageFormat.format(AppConstants.MESSAGE_PREVIOUS_MISSION, text), null);
            this.addStyledText(this.message, "\n", null);
        }
        if (this.clearDate != null) {
            this.addStyledText(this.message, MessageFormat.format(AppConstants.MESSAGE_COND, this.clearDate), null);
        }

        SwtUtils.layoutCompositeRecursively(this.fleetGroup);
        this.getShell().setRedraw(true);
    }

    /**
     * HPゲージのイメージを取得します
     * @param hpratio HP割合
     * @return HPゲージのイメージ
     */
    private Image getHpGaugeImage(float hpratio, float expraito) {
        Image image = new Image(Display.getDefault(), GAUGE_WIDTH, GAUGE_HEIGHT);
        GC gc = new GC(image);
        gc.setBackground(ColorManager.getColor(SWT.COLOR_WHITE));
        gc.fillRectangle(0, 0, GAUGE_WIDTH, GAUGE_HEIGHT);
        gc.setBackground(ColorManager.getColor(gradation(hpratio, GAUGE_EMPTY, GAUGE_HALF, GAUGE_FULL)));
        gc.fillRectangle(0, 0, (int) (GAUGE_WIDTH * hpratio), GAUGE_HEIGHT);
        gc.setBackground(ColorManager.getColor(EXP_GAUGE));
        gc.fillRectangle(0, GAUGE_HEIGHT - EXP_GAUGE_HEIGHT, (int) (GAUGE_WIDTH * expraito), EXP_GAUGE_HEIGHT);
        gc.drawImage(image, 0, 0);
        gc.dispose();
        return image;
    }

    /**
     * スタイル付きテキストを設定します
     *
     * @param text StyledText
     * @param str 文字
     * @param style スタイル
     */
    private void addStyledText(StyledText text, String str, StyleRange style) {
        StyleRange[] oldranges = text.getStyleRanges();
        String beforeText = text.getText();
        StyleRange addStyle = style;
        if (addStyle == null) {
            addStyle = new StyleRange();
        }
        addStyle.start = beforeText.length();
        addStyle.length = str.length();

        StyleRange[] ranges = new StyleRange[oldranges.length + 1];
        for (int i = 0; i < oldranges.length; i++) {
            ranges[i] = oldranges[i];
        }
        ranges[oldranges.length] = addStyle;

        text.setText(beforeText + str);
        text.setStyleRanges(ranges);
    }

    /**
     * あと何回戦闘すればよいかを取得します
     *
     * @param ship 艦娘
     * @param isFlagship 旗艦
     * @return 回数
     */
    @CheckForNull
    private Integer getNextCount(ShipDto ship, boolean isFlagship) {
        // 次のレベルに必要な経験値
        Integer nextexp = CalcExpUtils.getNextLvExp(ship.getLv());
        if (nextexp != null) {
            // 必要経験値
            int needexp = nextexp - ship.getExp();
            // 海域Exp
            Integer baseexp = SeaExp.get().get(AppConfig.get().getDefaultSea());
            // 評価倍率
            Double eval = EvaluateExp.get().get(AppConfig.get().getDefaultEvaluate());
            if ((baseexp != null) && (eval != null)) {
                // 得られる経験値
                int getexpd = CalcExpUtils.getExp(baseexp, eval, isFlagship, false);
                // 戦闘回数
                int count = CalcExpUtils.getCount(needexp, getexpd);
                return Integer.valueOf(count);
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Image image : this.hpgaugeImages) {
            if (image != null) {
                image.dispose();
            }
        }
    }

    /**
     * 複数の色の中間色を取得する
     *
     * @param raito 割合
     * @param rgbs 色たち
     * @return 色
     */
    private static RGB gradation(float raito, RGB... rgbs) {
        if (raito <= 0.0f) {
            return rgbs[0];
        }
        if (raito >= 1.0f) {
            return rgbs[rgbs.length - 1];
        }
        int length = rgbs.length - 1;

        // 開始色
        int start = (int) (length * raito);
        // 終了色
        int end = start + 1;
        // 開始色と終了色の割合を算出
        float startPer = (float) start / length;
        float endPer = (float) end / length;
        float subPer = (raito - startPer) / (endPer - startPer);
        return gradation(subPer, rgbs[start], rgbs[end]);
    }

    /**
     * 2つの色の中間色を取得する
     *
     * @param raito 割合
     * @param start 開始色
     * @param end 終了色
     * @return 色
     */
    private static RGB gradation(float raito, RGB start, RGB end) {
        int r = (int) (start.red + ((end.red - start.red) * raito));
        int g = (int) (start.green + ((end.green - start.green) * raito));
        int b = (int) (start.blue + ((end.blue - start.blue) * raito));
        return new RGB(r, g, b);
    }

    /**
     * @return state
     */
    public BitSet getState() {
        return this.state;
    }

    /**
     * 泊地修理タイマー表示を更新する
     * @author Nekopanda
     */
    private static class AkashiTimerUpdator implements Runnable {
        private final Label label;
        private final int dockIndex;
        private final int dockPosition;
        private int showCount = 0;

        public AkashiTimerUpdator(Label l, int i, int p) {
            this.label = l;
            this.dockIndex = i;
            this.dockPosition = p;
        }

        @Override
        public void run() {
            try {
                String str = "";
                String tip = null;

                Date now = TimerContext.get().getLastUpdated();
                AkashiTimer.RepairState repairState = TimerContext.get().getAkashiRepairState(this.dockIndex);
                if (repairState.isRepairing()) {
                    AkashiTimer.ShipState state = repairState.get().get(this.dockPosition);
                    if (state != null) {
                        if (now.before(state.getFinish())) {
                            String reststr = TimeLogic
                                    .toDateRestString(TimeLogic.getRest(now, state.getFinish()), true);
                            String nextstr = TimeLogic.toDateRestString(state.getNext() / 1000, true);
                            boolean showRemain;
                            switch (AppConfig.get().getAkashiTimerFormat()) {
                            case 1:
                                showRemain = false;
                                break;
                            case 2:
                                showRemain = ((this.showCount++ / 4) % 2) == 0;
                                break;
                            default:
                                showRemain = true;
                                break;
                            }
                            if (showRemain) {
                                str = "修理あと" + reststr;
                            }
                            else {
                                str = "次回復まで" + nextstr;
                            }
                            tip = "現在までに+" + state.getCurrentGain() + "回復\n" +
                                    "次の回復まで" + nextstr + "\n" +
                                    "全回復まで" + reststr +
                                    "(" + format.format(state.getFinish()) + ")";
                        }
                        else {
                            str = "修理まもなく完了";
                        }
                    }
                }

                this.label.setText(str);
                this.label.setToolTipText(tip);
                this.label.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_BLUE));
                this.label.getParent().layout();
            } catch (Exception e) {
                LOG.get().warn("泊地修理更新でエラー", e);
            }
        }
    }
}