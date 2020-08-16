package logbook.gui.logic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import logbook.data.context.GlobalContext;
import logbook.dto.AirbaseDto;
import logbook.dto.ItemDto;
import logbook.dto.ShipDto;
import logbook.dto.AirbaseDto.AirCorpsDto;
import logbook.dto.AirbaseDto.AirCorpsDto.SquadronDto;
import logbook.util.JsonUtils;

/**
 * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを作成するクラス
 *
 * @author Nishisonic
 */
public class DeckBuilder {
    /**
     * 艦隊シミュレーター＆デッキビルダーのフォーマットバージョン
     */
    private static final int DECKBUILDER_FORMAT_VERSION = 4;

    /**
     * 制空権シミュレータのフォーマットバージョン
     */
    private static final double KC_TOOLS_FORMAT_VERSION = 4.2;

    /**
     * JervisORののフォーマットバージョン
     */
    private static final int JERVIS_OR_FORMAT_VERSION = 1;

    /**
     * 艦隊シミュレーター＆デッキビルダーのURL
     */
    private static final String DECKBUILDER_URL = "http://kancolle-calc.net/deckbuilder.html";

    /**
     * 制空権シミュレータのURL
     */
    private static final String KC_TOOLS_URL = "https://noro6.github.io/kcTools/";

    /**
     * JervisORのURL
     */
    private static final String JERVIS_OR_URL = "https://kcjervis.github.io/jervis/";

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを返します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param needsUsedDock どの艦隊のデータを用いるか[第一艦隊,第二艦隊,第三艦隊,第四艦隊]
     * @return format フォーマット
     */
    public static String toDeckBuilderFormat(boolean[] needsUsedDock) {
        JsonObjectBuilder deck = Json.createObjectBuilder();
        deck.add("version", DECKBUILDER_FORMAT_VERSION);
        try {
            IntStream.rangeClosed(1, GlobalContext.getBasicInfo().getDeckCount())
                    .filter(dockId -> needsUsedDock[dockId - 1])
                    .boxed()
                    .collect(Collectors.toMap(dockId -> dockId,
                            dockId -> GlobalContext.getDock(dockId.toString()).getShips()))
                    .forEach((dockId, ships) -> {
                        JsonObjectBuilder fleet = Json.createObjectBuilder();

                        IntStream.range(0, ships.size()).forEach(shipIdx -> {
                            JsonObjectBuilder ship = Json.createObjectBuilder();
                            ship.add("id", Integer.toString(ships.get(shipIdx).getShipInfo().getShipId()));
                            ship.add("lv", ships.get(shipIdx).getLv());
                            ship.add("luck", ships.get(shipIdx).getLucky());
                            JsonObjectBuilder items = Json.createObjectBuilder();
                            List<ItemDto> item2 = ships.get(shipIdx).getItem2();
                            int slotNum = ships.get(shipIdx).getSlotNum();

                            IntStream.range(0, slotNum)
                                    .filter(itemIdx -> Optional.ofNullable(item2.get(itemIdx)).isPresent())
                                    .boxed()
                                    .collect(Collectors.toMap(itemIdx -> itemIdx, itemIdx -> item2.get(itemIdx)))
                                    .forEach((itemIdx, itemDto) -> {
                                        JsonObjectBuilder item = Json.createObjectBuilder();
                                        item.add("id", item2.get(itemIdx).getSlotitemId());
                                        if (item2.get(itemIdx).getLevel() > 0) {
                                            item.add("rf", Integer.toString(item2.get(itemIdx).getLevel()));
                                        }
                                        else {
                                            item.add("rf", 0);
                                        }
                                        item.add("mas", Integer.toString(item2.get(itemIdx).getAlv()));
                                        items.add("i" + (itemIdx + 1), item);
                                    });

                            Optional.ofNullable(ships.get(shipIdx).getSlotExItem()).ifPresent(slotExItem -> {
                                JsonObjectBuilder item = Json.createObjectBuilder();
                                item.add("id", slotExItem.getSlotitemId());
                                item.add("rf", slotExItem.getLevel());
                                item.add("mas", slotExItem.getAlv());
                                if (slotNum < 5) {
                                    items.add("i" + (slotNum + 1), item);
                                }
                                else {
                                    items.add("ix", item);
                                }
                            });
                            ship.add("items", items);

                            fleet.add("s" + (shipIdx + 1), ship);
                        });
                        deck.add("f" + dockId, fleet);
                    });
            return deck.build().toString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを返します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param fleets 艦隊
     * @return format フォーマット
     */
    @SuppressWarnings("unchecked")
    public static String toDeckBuilderFormat(List<ShipDto>... fleets) {
        JsonObjectBuilder deck = Json.createObjectBuilder();
        deck.add("version", DECKBUILDER_FORMAT_VERSION);
        try {
            IntStream.rangeClosed(1, fleets.length)
                    .boxed()
                    .collect(Collectors.toMap(dockId -> dockId, dockId -> fleets[dockId - 1]))
                    .forEach((dockId, ships) -> {
                        JsonObjectBuilder fleet = Json.createObjectBuilder();

                        IntStream.range(0, ships.size()).forEach(shipIdx -> {
                            JsonObjectBuilder ship = Json.createObjectBuilder();
                            ship.add("id", Integer.toString(ships.get(shipIdx).getShipInfo().getShipId()));
                            ship.add("lv", ships.get(shipIdx).getLv());
                            ship.add("luck", ships.get(shipIdx).getLucky());
                            JsonObjectBuilder items = Json.createObjectBuilder();
                            List<ItemDto> item2 = ships.get(shipIdx).getItem2();

                            IntStream.range(0, item2.size())
                                    .filter(itemIdx -> Optional.ofNullable(item2.get(itemIdx)).isPresent())
                                    .boxed()
                                    .collect(Collectors.toMap(itemIdx -> itemIdx, itemIdx -> item2.get(itemIdx)))
                                    .forEach((itemIdx, itemDto) -> {
                                        JsonObjectBuilder item = Json.createObjectBuilder();
                                        item.add("id", item2.get(itemIdx).getSlotitemId());
                                        if (item2.get(itemIdx).getLevel() > 0) {
                                            item.add("rf", Integer.toString(item2.get(itemIdx).getLevel()));
                                        }
                                        else {
                                            item.add("rf", 0);
                                        }
                                        item.add("mas", Integer.toString(item2.get(itemIdx).getAlv()));
                                        items.add("i" + (itemIdx + 1), item);
                                    });

                            Optional.ofNullable(ships.get(shipIdx).getSlotExItem()).ifPresent(slotExItem -> {
                                JsonObjectBuilder item = Json.createObjectBuilder();
                                item.add("id", slotExItem.getSlotitemId());
                                if (slotExItem.getLevel() > 0) {
                                    item.add("rf", Integer.toString(slotExItem.getLevel()));
                                }
                                else {
                                    item.add("rf", 0);
                                }
                                item.add("mas", Integer.toString(slotExItem.getAlv()));
                                int slotNum = ships.get(shipIdx).getSlotNum();
                                if (slotNum < 5) {
                                    items.add("i" + (slotNum + 1), item);
                                }
                                else {
                                    items.add("ix", item);
                                }
                            });
                            ship.add("items", items);

                            fleet.add("s" + (shipIdx + 1), ship);
                        });
                        deck.add("f" + dockId, fleet);
                    });
            return deck.build().toString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを返します(全艦隊)
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @return format フォーマット
     */
    public static String toDeckBuilderFormat() {
        boolean[] b = { true, true, true, true };
        return toDeckBuilderFormat(b);
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのURLを作成します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param fleets 艦隊
     * @return url URL
     */
    @SuppressWarnings("unchecked")
    public static String toDeckBuilderURL(List<ShipDto>... fleets) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormat(fleets));
        if (formatOpt.isPresent()) {
            return DECKBUILDER_URL + "?predeck=" + formatOpt.get();
        }
        else {
            return null;
        }
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのURLを作成します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param needsUsedDock どの艦隊のデータを用いるか[第一艦隊,第二艦隊,第三艦隊,第四艦隊]
     * @return url URL
     */
    public static String toDeckBuilderURL(boolean[] needsUsedDock) {
        Optional<String> formatOpt = Optional.ofNullable(toDeckBuilderFormat(needsUsedDock));
        if (formatOpt.isPresent()) {
            return DECKBUILDER_URL + "?predeck=" + formatOpt.get();
        }
        else {
            return null;
        }
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのURLを作成します(全艦隊)
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @return url URL
     */
    public static String toDeckBuilderURL() {
        boolean[] b = { true, true, true, true };
        return toDeckBuilderURL(b);
    }

    private static String encodeURIComponent(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String toKcToolsBuilderURL(boolean[] needsUsedDock, boolean isEvent) {
        Optional<String> formatOpt = Optional.ofNullable(toKcToolsBuilderFormat(needsUsedDock, isEvent));
        if (formatOpt.isPresent()) {
            return KC_TOOLS_URL + "?predeck=" + formatOpt.get();
        }
        else {
            return null;
        }
    }

    public static String toKcToolsBuilderFormat(boolean[] needsUsedDock, boolean isEvent) {
        JsonObject deck = JsonUtils.fromString(toDeckBuilderFormat(needsUsedDock));
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("version", KC_TOOLS_FORMAT_VERSION);
        for (int i = 1; i <= 4; i++) {
            if (deck.containsKey("f" + i)) {
                json.add("f" + i, deck.getJsonObject("f" + i));
            }
        }
        Optional.ofNullable(GlobalContext.getAirbase()).ifPresent(airbases -> {
            Map<Integer, Map<Integer, AirCorpsDto>> airbase = airbases.get();
            int area = isEvent ? airbase.keySet().stream().filter(a -> a >= 22).findFirst().orElse(-1)
                    : airbase.containsKey(6) ? 6 : -1;
            if (area > 0) {
                airbase.get(area).forEach((id, aircorps) -> {
                    Map<Integer, SquadronDto> squadrons = aircorps.getSquadrons();
                    JsonObjectBuilder squadronJson = Json.createObjectBuilder();
                    squadrons.forEach((i, squadron) -> {
                        ItemDto item = GlobalContext.getItem(squadron.getSlotid());
                        if (Objects.nonNull(item)) {
                            squadronJson.add("i" + i, Json.createObjectBuilder()
                                    .add("id", item.getSlotitemId())
                                    .add("rf", item.getLevel())
                                    .add("mas", item.getAlv()));
                        }
                    });
                    json.add("a" + id, Json.createObjectBuilder()
                            .add("mode", 1)
                            .add("items", squadronJson));
                });
            }
        });
        return json.build().toString();
    }

    public static String toJervisORBuilderURL(boolean[] needsUsedDock, boolean isEvent) {
        Optional<String> formatOpt = Optional.ofNullable(toJervisORBuilderFormat(needsUsedDock, isEvent));
        if (formatOpt.isPresent()) {
            return JERVIS_OR_URL + "?operation-json=" + formatOpt.get();
        }
        else {
            return null;
        }
    }

    private static JsonArrayBuilder getEmptyLandBaseJervisORBuilderFormat() {
        JsonArrayBuilder landBaseJson = Json.createArrayBuilder();
        for (int aircorpId = 1; aircorpId <= 3; aircorpId++) {
            JsonObjectBuilder aircorpJson = Json.createObjectBuilder();
            JsonArrayBuilder squadronJson = Json.createArrayBuilder();
            for (int squadronId = 1; squadronId <= 4; squadronId++) {
                squadronJson.addNull();
            }
            aircorpJson.add("slots", JsonUtils.toJsonArray(new int[] { 18, 18, 18, 18 }))
                    .add("equipments", squadronJson);
            landBaseJson.add(aircorpJson);
        }
        return landBaseJson;
    }

    private static int toInternalProficiency(int alv) {
        switch (alv) {
            case 7: return 100;
            case 6: return 85;
            case 5: return 70;
            case 4: return 55;
            case 3: return 40;
            case 2: return 25;
            case 1: return 10;
        }
        return 0;
    }

    public static String toJervisORBuilderFormat(boolean[] needsUsedDock, boolean isEvent) {
        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("version", JERVIS_OR_FORMAT_VERSION)
                .add("name", "")
                .add("description", "")
                .add("hqLevel", GlobalContext.hqLevel())
                .add("side", "Player")
                .add("fleetType", "Single");
        JsonArrayBuilder fleetJson = Json.createArrayBuilder();
        for (int dockId = 1; dockId <= 4; dockId++) {
            List<ShipDto> ships = GlobalContext.getDock(String.valueOf(dockId)).getShips();
            if (dockId <= GlobalContext.getBasicInfo().getDeckCount() && Objects.nonNull(ships)
                    && needsUsedDock[dockId - 1]) {
                JsonArrayBuilder shipJson = Json.createArrayBuilder();
                for (int shipId = 0; shipId < Math.min(ships.size(), 7); shipId++) {
                    ShipDto ship = ships.get(shipId);
                    if (Objects.nonNull(ship)) {
                        List<ItemDto> items = ship.getItem2();
                        JsonArrayBuilder itemJson = Json.createArrayBuilder();
                        for (int itemId = 0; itemId < ship.getSlotNum(); itemId++) {
                            ItemDto item = items.get(itemId);
                            if (Objects.nonNull(item)) {
                                itemJson.add(Json.createObjectBuilder()
                                        .add("masterId", item.getSlotitemId())
                                        .add("improvement", item.getLevel())
                                        .add("proficiency", toInternalProficiency(item.getAlv())));
                            }
                            else {
                                itemJson.addNull();
                            }
                        }
                        ItemDto item = ship.getSlotExItem();
                        if (Objects.nonNull(item)) {
                            itemJson.add(Json.createObjectBuilder()
                                    .add("masterId", item.getSlotitemId())
                                    .add("improvement", item.getLevel())
                                    .add("proficiency", toInternalProficiency(item.getAlv())));
                        }
                        else {
                            itemJson.addNull();
                        }
                        JsonObjectBuilder increasedJson = Json.createObjectBuilder();
                        int hp = ship.getMaxhp() - ship.getShipInfo().getParam().getHP() - ship.getMarriedHpBonus();
                        if (hp > 0) {
                            increasedJson.add("hp", hp);
                        }
                        int lucky = ship.getLucky() - ship.getShipInfo().getParam().getLucky();
                        if (lucky > 0) {
                            increasedJson.add("luck", lucky);
                        }
                        // 対潜は装備ボーナスのせいで取れない
                        shipJson.add(Json.createObjectBuilder()
                                .add("masterId", ship.getShipId())
                                .add("level", ship.getLv())
                                .add("slots", JsonUtils.toJsonArray(ship.getMaxeq()))
                                .add("equipments", itemJson)
                                .add("increased", increasedJson));
                    }
                    else {
                        shipJson.addNull();
                    }
                }
                fleetJson.add(Json.createObjectBuilder()
                        .add("ships", shipJson));
            }
            else {
                JsonArrayBuilder array = Json.createArrayBuilder();
                for (int shipId = 0; shipId < 6; shipId++) {
                    array.addNull();
                }
                fleetJson.add(Json.createObjectBuilder()
                        .add("ships", array));
            }
        }
        json.add("fleets", fleetJson);

        AirbaseDto airbase = GlobalContext.getAirbase();
        int area = Objects.nonNull(airbase)
                ? (isEvent ? airbase.get().keySet().stream().filter(a -> a >= 22).findFirst().orElse(-1)
                        : airbase.get().containsKey(6) ? 6 : -1)
                : -1;
        if (area > 0) {
            JsonArrayBuilder landBaseJson = Json.createArrayBuilder();
            Map<Integer, AirCorpsDto> aircorps = airbase.get().get(area);
            for (int aircorpId = 1; aircorpId <= 3; aircorpId++) {
                Map<Integer, SquadronDto> squadrons = aircorps.get(aircorpId).getSquadrons();
                JsonObjectBuilder aircorpsJson = Json.createObjectBuilder();
                int[] slots = { 18, 18, 18, 18 };
                JsonArrayBuilder squadronJson = Json.createArrayBuilder();
                for (int squadronId = 1; squadronId <= 4; squadronId++) {
                    if (squadrons.containsKey(squadronId)) {
                        SquadronDto squadron = squadrons.get(squadronId);
                        slots[squadronId - 1] = squadron.getMaxCount();
                        ItemDto item = GlobalContext.getItem(squadron.getSlotid());
                        if (Objects.nonNull(item)) {
                            squadronJson.add(Json.createObjectBuilder()
                                    .add("masterId", item.getSlotitemId())
                                    .add("improvement", item.getLevel())
                                    .add("proficiency", toInternalProficiency(item.getAlv())));
                        }
                        else {
                            squadronJson.addNull();
                        }
                    }
                    else {
                        squadronJson.addNull();
                    }
                }
                aircorpsJson.add("slots", JsonUtils.toJsonArray(slots))
                        .add("equipments", squadronJson);
                landBaseJson.add(aircorpsJson);
            }
            json.add("landBase", landBaseJson);
        }
        else {
            json.add("landBase", getEmptyLandBaseJervisORBuilderFormat());
        }

        return json.build().toString();
    }
}
