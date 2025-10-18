package logbook.dto;

import java.util.Objects;

import javax.json.JsonObject;

import com.dyuproject.protostuff.Tag;

public class SquadronDto extends ItemDto {

    @Tag(101)
    private int squadronId;

    @Tag(102)
    private int state;

    @Tag(103)
    private int count;

    @Tag(104)
    private int maxCount;

    @Tag(105)
    private int cond;

    public SquadronDto(ItemDto item, JsonObject json) {
        super(Objects.nonNull(item) ? item.getInfo() : new ItemInfoDto(), json.getInt("api_slotid"));
        if (Objects.nonNull(item)) {
            super.setLocked(item.isLocked());
            super.setLevel(item.getLevel());
            super.setAlv(item.getAlv());
            this.count = (json.containsKey("api_count") && !json.isNull("api_count"))
                    ? json.getInt("api_count")
                    : item instanceof SquadronDto ? ((SquadronDto) item).getCount() : 0;

            this.maxCount = (json.containsKey("api_max_count") && !json.isNull("api_max_count"))
                    ? json.getInt("api_max_count")
                    : item instanceof SquadronDto ? ((SquadronDto) item).getMaxCount() : 0;

            this.cond = (json.containsKey("api_cond") && !json.isNull("api_cond"))
                    ? json.getInt("api_cond")
                    : item instanceof SquadronDto ? ((SquadronDto) item).getCond() : 0;
        }
        this.squadronId = json.getInt("api_squadron_id");
        this.state = json.getInt("api_state");
    }

    public SquadronDto(ItemDto item, int squadronId, int state, int count, int maxCount, int cond) {
        super(Objects.nonNull(item) ? item.getInfo() : new ItemInfoDto(), squadronId);
        if (Objects.nonNull(item)) {
            super.setLocked(item.isLocked());
            super.setLevel(item.getLevel());
            super.setAlv(item.getAlv());
        }
        this.squadronId = squadronId;
        this.state = state;
        this.count = count;
        this.maxCount = maxCount;
        this.cond = cond;
    }

    public int getSquadronId() {
        return this.squadronId;
    }

    public void setSquadronId(int squadronId) {
        this.squadronId = squadronId;
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getMaxCount() {
        return this.maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public int getCond() {
        return this.cond;
    }

    public void setCond(int cond) {
        this.cond = cond;
    }

    public boolean isNotEmptySlot() {
        return super.getSlotitemId() > 0;
    }
}
