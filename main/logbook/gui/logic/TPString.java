/**
 *
 */
package logbook.gui.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import logbook.dto.ItemDto;
import logbook.dto.ShipDto;

/**
 * @author Nishisonic
 *
 */
public class TPString implements Comparable<TPString> {

    private final Map<Integer, Integer> totalShipIdCountMap = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> totalShipTypeCountMap = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> totalItemCountMap = new HashMap<Integer, Integer>();
    private int TP = 0;

    private static class ShipParam {
        public Map<Integer, Long> itemCountMap;
        public int shipId;
        public int stype;

        ShipParam(ShipDto ship) {
            List<ItemDto> items = new ArrayList<ItemDto>(ship.getItem2());
            items.add(ship.getSlotExItem());

            this.itemCountMap = items.stream().filter(item -> item != null)
                    .collect(Collectors.groupingBy(ItemDto::getSlotitemId, Collectors.counting()));
            this.shipId = ship.getShipId();
            this.stype = ship.getStype();
        }
    }

    public TPString(List<ShipDto> ships) {
        ships.stream().filter(ship -> !ship.isBadlyDamage())
                .map(ship -> new ShipParam(ship)).forEach(param -> this.add(param));
        this.calc();
    }

    public TPString(ShipDto ship) {
        ShipParam param = new ShipParam(ship);
        this.add(param);
        this.calc();
    }

    private void add(ShipParam ship) {
        Map<Integer, Long> items = ship.itemCountMap;
        int shipId = ship.shipId;
        int stype = ship.stype;
        items.forEach((id, count) -> this.totalItemCountMap.put(id,
                (int) (this.totalItemCountMap.getOrDefault(id, 0) + count)));
        this.totalShipIdCountMap.put(shipId, this.totalShipIdCountMap.getOrDefault(shipId, 0) + 1);
        this.totalShipTypeCountMap.put(stype, this.totalShipTypeCountMap.getOrDefault(stype, 0) + 1);
    }

    private void calc() {
        this.TP += this.totalShipIdCountMap.entrySet().stream()
                .mapToInt(map -> this.toTPfromShipId(map.getKey()) * map.getValue()).sum();
        this.TP += this.totalShipTypeCountMap.entrySet().stream()
                .mapToInt(map -> this.toTPfromShipType(map.getKey()) * map.getValue()).sum();
        this.TP += this.totalItemCountMap.entrySet().stream()
                .mapToInt(map -> this.toTPfromItemId(map.getKey()) * map.getValue()).sum();
    }

    public int getValue() {
        return this.TP;
    }

    @Override
    public String toString() {
        int S = this.TP;
        int A = (int) (this.TP * 0.7);
        return String.format("TP獲得量: S %d / A %d", S, A);
    }

    private int toTPfromShipType(int type) {
        switch (type) {
        case 14: // 潜水空母
            return 1;
        case 2: // 駆逐艦
            return 5;
        case 3: // 軽巡洋艦
            return 2;
        case 6: // 航空巡洋艦
            return 4;
        case 10: // 航空戦艦
            return 7;
        case 22: // 補給艦
            return 15;
        case 17: // 揚陸艦
            return 12;
        case 16: // 水上機母艦
            return 9;
        case 20: // 潜水母艦
            return 7;
        case 21: // 練習巡洋艦
            return 6;
        default:
            return 0;
        }
    }

    private int toTPfromItemId(int id) {
        switch (id) {
        case 75: // ドラム缶(輸送用)
            return 5;
        case 68: // 大発動艇
        case 193: // 特大発動艇
        case 166: // 大発動艇(八九式中戦車＆陸戦隊)
        case 230: // 特大発動艇+戦車第11連隊
        case 355: // M4A1 DD
        case 408: // 装甲艇(AB艇)
        case 409: // 武装大発
        case 436: // 大発動艇(II号戦車/北アフリカ仕様)
        case 449: // 特大発動艇+一式砲戦車
        case 482: // 大発動艇(III号戦車/北アフリカ仕様)
        case 494: // 特大発動艇+チハ
        case 495: // 特大発動艇+チハ改
        case 514: // 特大発動艇＋III号戦車J型
            return 8;
        case 167: // 特二式内火艇
        case 525: // 特四式内火艇
        case 526: // 特四式内火艇改
            return 2;
        case 145: // 戦闘糧食
        case 150: // 秋刀魚の缶詰
        case 241: // 戦闘糧食(特別なおにぎり)
            return 1;
        default:
            return 0;
        }
    }

    private int toTPfromShipId(int id) {
        switch (id) {
        case 487: // 鬼怒改二
            return 8;
        default:
            return 0;
        }
    }

    @Override
    public int compareTo(TPString tp) {
        return Integer.compare(this.getValue(), tp.getValue());
    }
}
