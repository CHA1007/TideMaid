package com.chadate.tidemaid.fishing.rod;

/**
 * 鱼竿属性数据类，封装 luck 和 speed 加成
 */
public class RodStats {
    private int luckBonus;
    private int speedBonus;

    public RodStats() {
        this.luckBonus = 0;
        this.speedBonus = 0;
    }

    public RodStats(int luckBonus, int speedBonus) {
        this.luckBonus = luckBonus;
        this.speedBonus = speedBonus;
    }

    public int getLuckBonus() {
        return luckBonus;
    }

    public void setLuckBonus(int luckBonus) {
        this.luckBonus = luckBonus;
    }

    public void addLuckBonus(int amount) {
        this.luckBonus += amount;
    }

    public int getSpeedBonus() {
        return speedBonus;
    }

    public void setSpeedBonus(int speedBonus) {
        this.speedBonus = speedBonus;
    }

    public void addSpeedBonus(int amount) {
        this.speedBonus += amount;
    }
}
