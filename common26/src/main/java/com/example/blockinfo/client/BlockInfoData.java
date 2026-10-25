package com.example.blockinfo.client;

import net.minecraft.core.BlockPos;

import java.util.List;

public class BlockInfoData {
    public String blockName = "";
    public String modId = "";
    public boolean isVanilla = false;
    public BlockPos blockPos = BlockPos.ZERO;

    public boolean isPlant = false;
    public float plantGrowth = 0f;

    public boolean isFurnace = false;
    public int furnaceBurnTime = 0;
    public int furnaceTotalBurnTime = 0;
    public int furnaceCookTime = 0;
    public int furnaceTotalCookTime = 0;
    public float cookProgress = 0f;
    public float burnProgress = 0f;
    public String inputItemName = "";
    public int inputItemCount = 0;
    public String fuelItemName = "";
    public int fuelItemCount = 0;
    public String outputItemName = "";
    public int outputItemCount = 0;

    public boolean isContainer = false;
    public List<String> containerItems = null;

    public boolean canMine = false;

    /**
     * 玩家当前是否**正在**挖掘。
     *
     * <p>它决定 HUD 里那条“挖掘”进度条是否显示。用独立的布尔量而不是
     * {@code mineProgress > 0}，因为进度是 0..10 的整数档位：刚开始挖的那一档
     * （0%）和“挖破一块后归零”的那两 tick 都会是 0，用数值判断会让进度条
     * 一闪一闪的。
     */
    public boolean isMining = false;

    /** 挖掘进度 0..1，仅在 {@link #isMining} 为 true 时有意义。 */
    public float mineProgress = 0f;

    public void clear() {
        blockName = "";
        modId = "";
        isVanilla = false;
        blockPos = BlockPos.ZERO;
        isPlant = false;
        plantGrowth = 0f;
        isFurnace = false;
        furnaceBurnTime = 0;
        furnaceTotalBurnTime = 0;
        furnaceCookTime = 0;
        furnaceTotalCookTime = 0;
        cookProgress = 0f;
        burnProgress = 0f;
        inputItemName = "";
        inputItemCount = 0;
        fuelItemName = "";
        fuelItemCount = 0;
        outputItemName = "";
        outputItemCount = 0;
        isContainer = false;
        containerItems = null;
        canMine = false;
        isMining = false;
        mineProgress = 0f;
    }
}
