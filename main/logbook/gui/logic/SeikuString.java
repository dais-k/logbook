/**
 *
 */
package logbook.gui.logic;

import java.util.List;

import logbook.config.AppConfig;
import logbook.dto.ItemDto;
import logbook.dto.ShipBaseDto;

/**
 * @author Nekopanda
 *
 */
public class SeikuString implements Comparable<SeikuString> {

    private static int[][] alevelBonusTable = new int[][] {
            { 0, 0, 2, 5, 9, 14, 14, 22 }, // 艦上戦闘機、水上戦闘機、夜間戦闘機
            { 0, 0, 0, 0, 0, 0, 0, 0 }, // 艦上爆撃機、艦上攻撃機、噴式戦闘爆撃機、陸上偵察機
            { 0, 0, 1, 1, 1, 3, 3, 6 }, // 水上爆撃機
            { 0, 0, 2, 5, 9, 14, 14, 22 }, // 一式戦 隼II型改(20戦隊)、一式戦 隼III型改(熟練/20戦隊)
    };

    private static int[] internalAlevelTable = new int[] {
            0, 10, 25, 40, 55, 70, 85, 100, 121
    };

    // 従来の数値
    private int seikuBase;
    // 熟練度ボーナス込み
    private int seikuToalMin;
    private int seikuToalMid;
    private int seikuTotalMax;

    // 情報が不足してて計算できなかった
    private boolean seikuFailed = false;

    public <SHIP extends ShipBaseDto> SeikuString(List<SHIP> ships) {
        for (SHIP ship : ships) {
            this.add(ship);
        }
    }

    public SeikuString(ShipBaseDto ship) {
        this.add(ship);
    }

    private static double calcBonusSeiku(int type, int alv, double internal) {
        return alevelBonusTable[type][alv] + Math.sqrt(internal / 10);
    }

    private void add(ShipBaseDto ship) {
        List<ItemDto> items = ship.getItem2();
        for (int i = 0; i < items.size(); i++) {
            ItemDto item = items.get(i);
            if (item != null) {
                int type;
                switch (item.getType2()) {
                case 6: // 艦上戦闘機、夜間戦闘機
                case 45: // 水上戦闘機
                    type = 0;
                    break;
                case 7: // 艦上爆撃機
                case 8: // 艦上攻撃機
                case 57: // 噴式戦闘爆撃機
                    type = 1;
                    break;
                case 11: // 水上爆撃機
                    type = 2;
                    break;
                default:
                    // 一式戦 隼II型改(20戦隊)、一式戦 隼III型改(熟練/20戦隊)
                    if (item.getSlotitemId() == 489 || item.getSlotitemId() == 491) {
                        type = 3;
                    } else {
                        type = -1;
                    }
                    break;
                }

                if (type != -1) {
                    // スロット数が分からないと計算できない
                    if (ship.getOnSlot() == null) {
                        this.seikuFailed = true;
                        return;
                    }

                    // 搭載機数
                    int onslot = ship.getOnSlot()[i];

                    // 搭載機数が0だと制空値はなくなる
                    if (onslot > 0) {
                        double tyku = item.getParam().getTyku();

                        // 改修効果 艦戦は★×0.2、爆戦は★×0.25
                        switch (type) {
                            case 0: // 艦上戦闘機 （水上戦闘機は不明だけど一応入れておく）
                                tyku += item.getLevel() * 0.2;
                                break;
                            case 1: // 爆戦（爆戦でない艦上爆撃機や艦上攻撃機、噴式戦闘爆撃機は不明だけど一応入れておく）
                                // 汎用的に(九七式艦攻(友永隊)改修実装に伴い変更)
                                if (item.getParam().getTyku() > 3) {
                                    tyku += item.getLevel() * 0.25;
                                }
                                break;
                        }

                        double basePart = tyku * Math.sqrt(onslot);

                        double ialvMin = internalAlevelTable[item.getAlv()];
                        double ialvMax = internalAlevelTable[item.getAlv() + 1] - 1;
                        double ialvMid = (ialvMin + ialvMax) / 2;

                        double totalMin = basePart + calcBonusSeiku(type, item.getAlv(), ialvMin);
                        double totalMid = basePart + calcBonusSeiku(type, item.getAlv(), ialvMid);
                        double totalMax = basePart + calcBonusSeiku(type, item.getAlv(), ialvMax);

                        this.seikuBase += (int) Math.floor(basePart);
                        this.seikuToalMin += (int) Math.floor(totalMin);
                        this.seikuToalMid += (int) Math.floor(totalMid);
                        this.seikuTotalMax += (int) Math.floor(totalMax);
                    }
                }
            }
        }
    }

    public int getValue() {
        int method = AppConfig.get().getSeikuMethod();
        switch (method) {
        case 0:
            return this.seikuBase;
        case 1:
        case 2:
        case 3:
        case 4:
            return this.seikuToalMid;
        default:
            return 0;
        }
    }

    private String seikuTotalString() {
        if (this.seikuToalMin == this.seikuTotalMax) {
            return Integer.toString(this.seikuToalMin);
        } else {
            return String.format("%d-%d", this.seikuToalMin, this.seikuTotalMax);
        }
    }

    @Override
    public String toString() {
        int method = AppConfig.get().getSeikuMethod();
        if (this.seikuFailed) {
            return "?";
        }
        switch (method) {
        case 0: // 艦載機素の制空値
            return Integer.toString(this.seikuBase);
        case 1: // 制空推定値範囲
            return this.seikuTotalString();
        case 2: // 制空推定値範囲(艦載機素の制空値 + 熟練度ボーナス推定値)
            return String.format("%s (%d+%d)",
                    this.seikuTotalString(), this.seikuBase, this.seikuToalMid - this.seikuBase);
        case 3: // 制空推定値中央
            return Integer.toString(this.seikuToalMid);
        case 4: // 制空推定値中央(艦載機素の制空値 + 熟練度ボーナス推定値)
            return String.format("%d (%d+%d)",
                    this.seikuToalMid, this.seikuBase, this.seikuToalMid - this.seikuBase);
        }
        return Integer.toString(this.seikuBase);
    }

    @Override
    public int compareTo(SeikuString o) {
        return Double.compare(this.getValue(), o.getValue());
    }
}
